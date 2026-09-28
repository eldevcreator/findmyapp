package com.eldevcreator.tracker

import android.app.Application
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

class TrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}

object Prefs {
    private const val FILE = "tracker"
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_TOKEN = "token"
    const val KEY_CODE = "code"
    const val KEY_INTERVAL = "interval_seconds"
    const val KEY_BEACON_UUID = "beacon_uuid"
    const val KEY_BEACON_MAJOR = "beacon_major"
    const val KEY_ACCOUNT_ID = "account_id"
    const val KEY_ACCOUNT_TOKEN = "account_token"
    const val KEY_RECOVERY = "recovery_code"

    private lateinit var app: Context

    fun init(ctx: Context) {
        app = ctx.applicationContext
    }

    private fun prefs() = app.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getString(key: String, def: String = ""): String = prefs().getString(key, def) ?: def
    fun putString(key: String, value: String) { prefs().edit().putString(key, value).apply() }

    var deviceId: String
        get() = getString(KEY_DEVICE_ID)
        set(v) = putString(KEY_DEVICE_ID, v)

    var token: String
        get() = getString(KEY_TOKEN)
        set(v) = putString(KEY_TOKEN, v)

    /** the code shown on screen. generated on THIS device, never issued by the bot. */
    var code: String
        get() = getString(KEY_CODE)
        set(v) = putString(KEY_CODE, v)

    var intervalSeconds: Int
        get() = prefs().getInt(KEY_INTERVAL, 300)
        set(v) = prefs().edit().putInt(KEY_INTERVAL, v).apply()

    var beaconUuid: String
        get() = getString(KEY_BEACON_UUID)
        set(v) = putString(KEY_BEACON_UUID, v)

    var beaconMajor: Int
        get() = prefs().getInt(KEY_BEACON_MAJOR, 0)
        set(v) = prefs().edit().putInt(KEY_BEACON_MAJOR, v).apply()

    /** the Find My Phone account created from the code the bot sent in Telegram */
    var accountId: String
        get() = getString(KEY_ACCOUNT_ID)
        set(v) = putString(KEY_ACCOUNT_ID, v)

    var accountToken: String
        get() = getString(KEY_ACCOUNT_TOKEN)
        set(v) = putString(KEY_ACCOUNT_TOKEN, v)

    var recoveryCode: String
        get() = getString(KEY_RECOVERY)
        set(v) = putString(KEY_RECOVERY, v)
}

object Server {
    // set to your worker, e.g. https://cf-tg-spy.eldevcreatorru.workers.dev
    const val BASE = "https://cf-tg-spy.eldevcreatorru.workers.dev"

    fun pair(ctx: Context, code: String, label: String, onDone: (Boolean, String) -> Unit) {
        val body = """{"code":"$code","label":"$label"}"""
        post(ctx, "/api/track/pair", body) { ok, out ->
            if (!ok) { onDone(false, out); return@post }
            try {
                val o = org.json.JSONObject(out)
                if (o.optBoolean("ok")) {
                    Prefs.deviceId = o.getString("device_id")
                    Prefs.token = o.getString("token")
                    onDone(true, "Привязано")
                } else onDone(false, o.optString("error", "ошибка"))
            } catch (e: Exception) { onDone(false, e.message ?: "плохой ответ") }
        }
    }

    /**
     * Exchanges the LINK-XXXX-XXXX code the bot sent in the owner's Telegram chat
     * for a Find My Phone session. The bot only ever issues that code to the same
     * chat, which is what proves the phone belongs to that Telegram account.
     */
    fun linkAccount(ctx: Context, code: String, username: String, onDone: (Boolean, String) -> Unit) {
        val u = username.trim().removePrefix("@").replace("\"", "")
        val body = """{"code":"${code.trim().uppercase()}","username":"$u"}"""
        post(ctx, "/api/track/account/link", body) { ok, out ->
            if (!ok) { onDone(false, out); return@post }
            try {
                val o = org.json.JSONObject(out)
                if (o.optBoolean("ok")) {
                    Prefs.accountId = o.getString("account_id")
                    Prefs.accountToken = o.getString("token")
                    if (o.has("recovery_code") && !o.isNull("recovery_code")) {
                        Prefs.recoveryCode = o.getString("recovery_code")
                    }
                    onDone(true, Prefs.accountId)
                } else onDone(false, o.optString("error", "ошибка"))
            } catch (e: Exception) { onDone(false, e.message ?: "плохой ответ") }
        }
    }

    /**
     * Restores access with either the LINK code the bot sent, or the account id
     * plus the recovery code. Pass an empty accountId when using the LINK code.
     */
    fun recoverAccount(ctx: Context, accountId: String, code: String, onDone: (Boolean, String) -> Unit) {
        val acct = accountId.trim().uppercase().replace("\"", "")
        val payload = if (acct.isBlank()) {
            """{"code":"${code.trim().uppercase()}"}"""
        } else {
            """{"account_id":"$acct","recovery_code":"${code.trim().lowercase()}"}"""
        }
        post(ctx, "/api/track/account/recover", payload) { ok, out ->
            if (!ok) { onDone(false, out); return@post }
            try {
                val o = org.json.JSONObject(out)
                if (o.optBoolean("ok")) {
                    Prefs.accountId = o.getString("account_id")
                    Prefs.accountToken = o.getString("token")
                    onDone(true, Prefs.accountId)
                } else onDone(false, o.optString("error", "ошибка"))
            } catch (e: Exception) { onDone(false, e.message ?: "плохой ответ") }
        }
    }

    fun ping(ctx: Context, lat: Double, lon: Double, acc: Float, speed: Float, battery: Float, charging: Boolean, onDone: (Boolean) -> Unit) {
        val b = """{"device_id":"${Prefs.deviceId}","token":"${Prefs.token}","lat":$lat,"lon":$lon,"acc":$acc,"speed":$speed,"battery":$battery,"charging":$charging}"""
        post(ctx, "/api/track/ping", b) { ok, _ -> onDone(ok) }
    }

    fun ble(ctx: Context, rssi: Int, onDone: () -> Unit) {
        val b = """{"device_id":"${Prefs.deviceId}","token":"${Prefs.token}","rssi":$rssi}"""
        post(ctx, "/api/track/ble", b) { _, _ -> onDone() }
    }

    fun poll(ctx: Context, onDone: (List<String>) -> Unit) {
        val b = """{"device_id":"${Prefs.deviceId}","token":"${Prefs.token}"}"""
        post(ctx, "/api/track/poll", b) { ok, out ->
            if (!ok) { onDone(emptyList()); return@post }
            try {
                val arr = org.json.JSONObject(out).optJSONArray("commands")
                val list = ArrayList<String>()
                if (arr != null) for (i in 0 until arr.length()) list.add(arr.getString(i))
                onDone(list)
            } catch (e: Exception) { onDone(emptyList()) }
        }
    }

    fun setTracking(ctx: Context, on: Boolean, onDone: () -> Unit) {
        val b = """{"device_id":"${Prefs.deviceId}","token":"${Prefs.token}","on":$on}"""
        post(ctx, "/api/track/tracking", b) { _, _ -> onDone() }
    }

    private fun post(ctx: Context, path: String, body: String, cb: (Boolean, String) -> Unit) {
        Thread {
            var ok = false
            var out = ""
            try {
                val conn = (java.net.URL(Server.BASE + path).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 12000
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(body.toByteArray()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                out = (if (stream != null) stream.bufferedReader().readText() else "")
                ok = code in 200..299
            } catch (e: Exception) {
                out = e.message ?: "network"
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post { cb(ok, out) }
        }.start()
    }
}

/** Screen lock + wipe. Requires the user to grant Device Admin once. */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Log.i("Tracker", "device admin enabled")
    }
}
