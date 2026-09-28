package com.eldevcreator.tracker

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import java.security.SecureRandom

/**
 * UI follows the exteraGram reference: light background #FFF6F7, red accent #F54142,
 * soft red cards, large radii, Onest-like system font, and a fixed bottom tab bar
 * with Home / Map / Profile.
 */
class MainActivity : Activity() {

    private val RED = Color.parseColor("#F54142")
    private val RED_SOFT = Color.parseColor("#FBEEEC")
    private val BG = Color.parseColor("#FFF6F7")
    private val INK = Color.parseColor("#1A1A1A")
    private val GREY = Color.parseColor("#A3A3A3")
    private val GREY2 = Color.parseColor("#737373")
    private val LINE = Color.parseColor("#EFEFEF")
    private val WHITE = Color.WHITE
    private val BLUE = Color.parseColor("#2F80ED")

    private lateinit var body: FrameLayout
    private var tab = 0 // 0 home, 1 map, 2 profile
    private var status: TextView? = null

    private val perms = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        if (Prefs.code.isBlank()) Prefs.code = newCode()
        build()
        requestPerms()
    }

    private fun newCode(): String {
        val a = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val r = SecureRandom()
        return (1..6).map { a[r.nextInt(a.length)] }.joinToString("")
    }

    // ---------------------------------------------------------------- layout helpers

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun txt(s: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
            setTextColor(color)
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

    private fun card(pad: Int = 18, radius: Int = 24): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
            background = rounded(WHITE, dp(radius))
            elevation = dp(2).toFloat()
        }

    private fun rounded(color: Int, r: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = r.toFloat()
        }

    private fun button(label: String, bg: Int, fg: Int, onClick: () -> Unit): View =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(fg)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(bg, dp(16))
            setOnClickListener { onClick() }
        }

    private fun input(hint: String): android.widget.EditText =
        android.widget.EditText(this).apply {
            this.hint = hint
            setHintTextColor(Color.parseColor("#C9C9C9"))
            setTextColor(INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(WHITE)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), LINE)
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        }

    // ---------------------------------------------------------------- shell

    private fun build() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(14))
            setBackgroundColor(BG)
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(txt("Найти телефон", 20f, INK, true))
        titles.addView(txt("ELDEVCREATOR RU", 13f, GREY))
        head.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(txt("EL", 15f, WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(RED, dp(12))
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
        })
        root.addView(head)
        root.addView(View(this).apply { setBackgroundColor(LINE); layoutParams = LinearLayout.LayoutParams(-1, dp(1)) })

        body = FrameLayout(this)
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bottomNav())
        setContentView(root)

        showHome()
    }

    private fun bottomNav(): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(6), dp(10), dp(10))
            setBackgroundColor(WHITE)
            elevation = dp(10).toFloat()
        }
        // real vector icons, generated from the same paths the Worker serves at /icons/*.svg
        val items = listOf(
            Triple(R.drawable.ic_home, "Главное", 0),
            Triple(R.drawable.ic_map, "Карта", 1),
            Triple(R.drawable.ic_user, "Профиль", 2)
        )
        val navBtns = mutableListOf<LinearLayout>()
        for ((ic, label, idx) in items) {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(4))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                tag = idx
            }
            val t1 = ImageView(this).apply {
                setImageResource(ic)
                setColorFilter(GREY)
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
            }
            val t2 = txt(label, 11f, GREY, true)
            col.addView(t1); col.addView(t2)
            col.setOnClickListener { when (idx) { 0 -> showHome(); 1 -> showMap(); else -> showProfile() } }
            nav.addView(col)
            navBtns.add(col)
        }
        tag = navBtns
        return nav
    }

    private fun paintNav(active: Int) {
        @Suppress("UNCHECKED_CAST")
        val btns = tag as List<LinearLayout>
        for (b in btns) {
            val on = (b.tag as Int) == active
            val c = if (on) BLUE else GREY
            (b.getChildAt(0) as ImageView).setColorFilter(c)
            (b.getChildAt(1) as TextView).setTextColor(c)
        }
    }

    private fun setBody(v: View) {
        body.removeAllViews()
        body.addView(v, FrameLayout.LayoutParams(-1, -1))
    }

    // ---------------------------------------------------------------- tabs

    private fun scroll(child: View): ScrollView = ScrollView(this).apply {
        setBackgroundColor(BG)
        isFillViewport = true
        addView(child)
    }

    private fun column(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(16), dp(20), dp(24))
    }

    private fun showHome() {
        tab = 0
        paintNav(0)
        val col = column()
        val wrap = scroll(col)

        // ---- 1. log in with the code the bot sent in Telegram
        val login = card()
        login.addView(txt("Вход по коду", 17f, INK, true))
        login.addView(txt("Напишите боту /findmy — он пришлёт код сюда, в вашу личку. Вставьте его.", 13f, GREY2).apply {
            setPadding(0, dp(6), 0, dp(12))
        })
        val userField = input("@username")
        val codeField = input("LINK-XXXX-XXXX").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        login.addView(userField)
        login.addView(codeField)
        login.addView(button("Войти по коду", RED, WHITE) {
            val c = codeField.text.toString().trim()
            if (c.isBlank()) { status?.text = "Вставьте код от бота"; return@button }
            status?.text = "Проверяю код…"
            Server.linkAccount(this, c, userField.text.toString()) { ok, msg ->
                if (ok) {
                    val rec = Prefs.recoveryCode
                    // showHome() rebuilds the views, so only set the status afterwards
                    showHome()
                    status?.text = if (rec.isBlank()) "Готово. Аккаунт: " + msg else "Готово. Сохраните код восстановления: " + rec
                } else status?.text = "Не получилось: " + msg
            }
        })
        col.addView(login, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        if (Prefs.deviceId.isBlank()) {
            val c = card()
            c.addView(txt("Аккаунт", 17f, INK, true))
            c.addView(txt("Код привязки создан на этом телефоне и действует только для вас.", 13f, GREY2).apply {
                setPadding(0, dp(6), 0, dp(14))
            })
            c.addView(button("Скопировать код", RED, WHITE) { copyCode() })
            val codeBox = card(16, 20).apply { setBackgroundColor(RED_SOFT) }
            codeBox.addView(txt(Prefs.code, 34f, RED, true).apply {
                gravity = Gravity.CENTER
                letterSpacing = 0.18f
            })
            c.addView(codeBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            c.addView(txt("Скопируйте код и откройте страницу с картой:\n" + Server.BASE + "/track?code=" + Uri.encode(Prefs.code) + "&auto=1", 12f, RED).apply {
                setPadding(0, dp(12), 0, dp(4))
            })
            c.addView(button("Привязать к серверу", RED_SOFT, RED) { pair() })
            c.addView(button("Включить слежение", RED, WHITE) { start() }.apply {
                (this as View).layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
            })
            col.addView(c)
        } else {
            val c = card()
            c.addView(txt("Привязано", 17f, INK, true))
            c.addView(txt("id: " + Prefs.deviceId, 12f, GREY).apply { setPadding(0, dp(4), 0, dp(12)) })
            c.addView(button("Открыть карту", RED, WHITE) { openMap() })
            c.addView(button("Включить слежение", RED_SOFT, RED) { start() }.apply {
                (this as View).layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
            })
            col.addView(c)
        }

        status = txt("", 13f, GREY2).apply { setPadding(dp(4), dp(12), dp(4), 0) }
        col.addView(status)

        val tools = card(16, 20)
        tools.addView(txt("Разрешения и доступ", 15f, INK, true))
        tools.addView(button("Разрешения приложения", Color.parseColor("#F3F3F3"), INK) { openAppSettings() })
        tools.addView(button("Разрешить блокировку экрана", Color.parseColor("#F3F3F3"), INK) { enableAdmin() }.apply {
            (this as View).layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        })
        tools.addView(button("Проверить звук", Color.parseColor("#F3F3F3"), INK) { testRing() }.apply {
            (this as View).layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        })
        col.addView(tools, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        setBody(wrap)
    }

    private fun showMap() {
        tab = 1
        paintNav(1)
        val col = column()
        val c = card(20, 24)
        c.gravity = Gravity.CENTER
        c.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_map)
            setColorFilter(GREY)
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        })
        c.addView(txt("Устройство не выбрано", 16f, INK, true).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(12), 0, 0)
        })
        c.addView(txt("Скопируйте код на телефоне и вставьте его в разделе «Главное», либо откройте страницу с картой в браузере.", 13f, GREY2).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0)
        })
        c.addView(button("Открыть страницу с картой", RED_SOFT, RED) { openMap() }.apply {
            (this as View).layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }
        })
        col.addView(c)
        setBody(scroll(col))
    }

    private fun showProfile() {
        tab = 2
        paintNav(2)
        val col = column()

        val head = card()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(txt("EL", 20f, WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(RED, dp(18))
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        })
        val t = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        t.addView(txt(if (Prefs.deviceId.isBlank()) "не привязано" else "привязано", 17f, INK, true))
        t.addView(txt("ELDEVCREATOR RU · v1.0", 13f, GREY))
        row.addView(t)
        head.addView(row)
        col.addView(head)

        val how = card()
        how.addView(txt("Как это работает", 15f, INK, true))
        how.addView(txt(
            "Телефон сам создаёт код привязки и отправляет его на сервер. Код не выдаётся по нику в Telegram, поэтому получить вашу геолокацию по имени аккаунта не может никто.",
            13f, GREY2
        ).apply { setPadding(0, dp(8), 0, 0) })
        col.addView(how, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        val acc = card()
        acc.addView(txt("Точность", 15f, INK, true))
        acc.addView(txt("GPS — метры. Bluetooth — только оценка: в помещении гуляет на 10–15 метров, поэтому это шкала, а не число.", 13f, GREY2).apply {
            setPadding(0, dp(8), 0, 0)
        })
        col.addView(acc, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        setBody(scroll(col))
    }

    // ---------------------------------------------------------------- actions

    private fun copyCode() {
        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("code", Prefs.code))
        status?.text = "Код скопирован: " + Prefs.code
    }

    private fun pair() {
        status?.text = "Привязываю…"
        Server.pair(this, Prefs.code, Build.MODEL ?: "Android") { ok, msg ->
            if (ok) {
                status?.text = "Готово. Откройте карту."
                showHome()
            } else status?.text = "Не получилось: " + msg
        }
    }

    private fun start() {
        if (!hasLocationPermission()) { requestPerms(); status?.text = "Нужны разрешения на геолокацию"; return }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, TrackService::class.java))
            } else startService(Intent(this, TrackService::class.java))
            status?.text = "Слежение запущено"
        } catch (e: Exception) { status?.text = "Ошибка: " + e.message }
    }

    private fun testRing() {
        try { startService(Intent(this, TrackService::class.java).setAction(TrackService.ACTION_RING)) }
        catch (e: Exception) { status?.text = e.message }
    }

    private fun openMap() {
        try {
            val url = if (Prefs.code.isBlank()) Server.BASE + "/track" else Server.BASE + "/track?code=" + Uri.encode(Prefs.code) + "&auto=1"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) { status?.text = e.message }
    }

    private fun enableAdmin() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            startActivity(Intent(dpm.setDeviceAdminActiveIntent(ComponentName(this, AdminReceiver::class.java))))
        } catch (e: Exception) { status?.text = e.message }
    }

    private fun openAppSettings() {
        try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        catch (e: Exception) { status?.text = e.message }
    }

    private fun requestPerms() {
        val need = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }.toTypedArray()
        if (need.isNotEmpty()) ActivityCompat.requestPermissions(this, need, 1)
    }

    private fun hasLocationPermission(): Boolean {
        val f = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val c = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return f || c
    }
}
