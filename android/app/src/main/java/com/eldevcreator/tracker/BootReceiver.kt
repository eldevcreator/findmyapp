package com.eldevcreator.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the service after a reboot so the phone is still trackable. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs.deviceId.isBlank()) return
        try {
            val i = Intent(context, TrackService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        } catch (e: Exception) {
            android.util.Log.w("Tracker", "boot start failed: ${e.message}")
        }
    }
}
