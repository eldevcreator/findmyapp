package com.eldevcreator.tracker

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Foreground service. Keeps the beacon on, pings location on a timer,
 * polls for remote commands and scans for our own beacon (proximity).
 *
 * Android kills background location aggressively, so this MUST be a foreground
 * service with a permanent notification. That is also visible to the user,
 * which is deliberate: a tracker you cannot see is a tracker you cannot stop.
 */
class TrackService : android.app.Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pingJob: Job? = null
    private var pollJob: Job? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: android.bluetooth.le.BluetoothLeScanner? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        startBeacon()
        schedulePings()
        schedulePolls()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> ringLoud()
            ACTION_LOCK -> lockScreen()
            ACTION_WIPE -> wipeData()
            ACTION_STOP_ALL -> {
                stopTracking()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, TrackService::class.java).setAction(ACTION_STOP_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.tracking_active))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.stop), stop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // ---------------------------------------------------------------- location

    private fun schedulePings() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                val loc = lastKnown()
                if (loc != null) {
                    val battery = batteryLevel()
                    Server.ping(this@TrackService, loc.latitude, loc.longitude, loc.accuracy, loc.speed, battery, isCharging()) {}
                }
                delay(Prefs.intervalSeconds.coerceIn(60, 3600) * 1000L)
            }
        }
    }

    private fun lastKnown(): Location? {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (e: Exception) { null }
    }

    private fun batteryLevel(): Float {
        val i = Intent(Intent.ACTION_BATTERY_CHANGED)
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) level * 100f / scale else -1f
    }

    private fun isCharging(): Boolean {
        val i = Intent(Intent.ACTION_BATTERY_CHANGED)
        val p = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        return p != 0
    }

    // ---------------------------------------------------------------- beacon (BLE)

    private fun startBeacon() {
        if (!hasBle()) return
        val mgr = getSystemService(Context.BLUETOOTH_LE_SERVICE) as? android.bluetooth.le.BluetoothLeManager ?: return
        if (!mgr.isAdvertisingSupported) { Log.w("Tracker", "BLE advertising unsupported on this phone"); return }
        val adv = mgr.bluetoothLeAdvertiser ?: return
        val uuid = if (Prefs.beaconUuid.isBlank()) UUID.randomUUID().toString() else Prefs.beaconUuid
        Prefs.beaconUuid = uuid
        val data = AdvertiseData.Builder()
            .addServiceUuid(android.os.ParcelUuid(UUID.fromString(uuid)))
            .setIncludeDeviceName(false)
            .build()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .build()
        adv.startAdvertising(settings, data, object : BluetoothLeAdvertiser.AdvertisingCallback() {
            override fun onStartFailure(errorCode: Int) { Log.e("Tracker", "advertise failed: $errorCode") }
        })
        advertiser = adv
    }

    private fun hasBle(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        }
    }

    /**
     * Scans for our own beacon. RSSI gives a coarse distance only.
     * We report the raw signal; the server turns it into a range.
     */
    private fun startScan() {
        if (Prefs.beaconUuid.isBlank()) return
        val mgr = getSystemService(Context.BLUETOOTH_LE_SERVICE) as? android.bluetooth.le.BluetoothLeManager ?: return
        val sc = mgr.bluetoothLeScanner ?: return
        scanner = sc
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val filter = android.bluetooth.le.ScanFilter.Builder()
            .setServiceUuid(android.os.ParcelUuid(UUID.fromString(Prefs.beaconUuid)))
            .build()
        sc.startScan(listOf(filter), settings, object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val rssi = result?.rssi ?: return
                if (rssi > -100) Server.ble(this@TrackService, rssi) {}
            }
        })
    }

    // ---------------------------------------------------------------- remote commands

    private fun schedulePolls() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                Server.poll(this@TrackService) { cmds ->
                    for (c in cmds) {
                        when (c) {
                            "ring" -> ringLoud()
                            "lock" -> lockScreen()
                            "wipe" -> wipeData()
                        }
                    }
                }
                delay(20_000)
            }
        }
        // proximity scan runs alongside, slower to save battery
        scope.launch {
            delay(5000)
            while (isActive) {
                startScan()
                delay(60_000)
            }
        }
    }

    private fun ringLoud() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val r = RingtoneManager.getRingtone(this, uri)
            if (!r.isPlaying) r.play()
            // also try the alarm stream, which is louder and survives ringer mode
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.mode = AudioManager.MODE_RING
            val amr = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            if (!amr.isPlaying) amr.play()
            scope.launch {
                delay(20_000)
                r.stop(); amr.stop()
                am.mode = AudioManager.MODE_NORMAL
            }
        } catch (e: Exception) { Log.e("Tracker", "ring failed", e) }
    }

    private fun lockScreen() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (dpm.isAdminActive(AdminReceiver::class.java)) dpm.lockNow()
        } catch (e: Exception) { Log.e("Tracker", "lock failed", e) }
    }

    private fun wipeData() {
        // Factory reset needs Device Owner, which only a provisioning flow can grant.
        // Without it we do the next best thing: ask the user to wipe from Settings.
        try {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) { Log.e("Tracker", "wipe handoff failed", e) }
    }

    private fun stopTracking() {
        pingJob?.cancel()
        pollJob?.cancel()
        advertiser?.stopAdvertising(android.bluetooth.le.BluetoothLeAdvertiser.AdvertisingCallback())
        try { scanner?.stopScan(android.bluetooth.le.ScanCallback()) } catch (e: Exception) {}
        Server.setTracking(this, false) {}
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "track"
        const val NOTIF_ID = 42
        const val ACTION_RING = "ring"
        const val ACTION_LOCK = "lock"
        const val ACTION_WIPE = "wipe"
        const val ACTION_STOP_ALL = "stop"
    }
}

private typealias BatteryManager = android.os.BatteryManager
