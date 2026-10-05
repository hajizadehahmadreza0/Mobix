package ir.mobix.repair

import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val repairs = mutableListOf<JSONObject>()
    private lateinit var list: LinearLayout
    private lateinit var autoSms: Switch

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Toast.makeText(this, if (granted) "مجوز پیامک فعال شد" else "مجوز پیامک داده نشد", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        showHome()
    }

    private fun base(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            layoutDirection = LinearLayout.DIRECTION_RTL
        }
    }

    private fun text(s: String, size: Float = 16f) = TextView(this).apply {
        text = s; textSize = size; setPadding(8, 10, 8, 10)
    }

    private fun edit(hint: String) = EditText(this).apply {
        this.hint = hint; textSize = 16f; setPadding(16, 12, 16, 12)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; setOnClickListener { action() }
    }

    private fun showHome() {
        val v = base()
        v.addView(text("موبیکس", 26f))
        v.addView(text("مدیریت تعمیرات + ارسال پیامک با سیم‌کارت خود گوشی"))
        v.addView(button("➕ ثبت تعمیر جدید") { showNewRepair() })
        v.addView(button("📋 لیست تعمیرات") { showRepairs() })
        v.addView(button("⚙️ تنظیمات پیامک") { showSettings() })
        v.addView(text("تعداد تعمیرات: ${repairs.size}"))
        setContentView(v)
    }

    private fun showNewRepair() {
        val v = base()
        v.addView(text("ثبت تعمیر جدید", 24f))
        val name = edit("نام مشتری")
        val phone = edit("شماره موبایل مشتری")
        val model = edit("مدل گوشی")
        val imei = edit("IMEI (اختیاری)")
        val problem = edit("شرح مشکل")
        val parts = edit("قطعات مصرفی")
        val cost = edit("هزینه تعمیر")
        val status = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                arrayOf("در انتظار بررسی", "در حال تعمیر", "آماده تحویل", "تحویل شد"))
        }
        v.addView(name); v.addView(phone); v.addView(model); v.addView(imei)
        v.addView(problem); v.addView(parts); v.addView(cost)
        v.addView(text("وضعیت"))
        v.addView(status)
        v.addView(button("ثبت") {
            val code = (100000..999999).random().toString()
            val r = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("code", code)
                put("name", name.text.toString())
                put("phone", phone.text.toString())
                put("model", model.text.toString())
                put("imei", imei.text.toString())
                put("problem", problem.text.toString())
                put("parts", parts.text.toString())
                put("cost", cost.text.toString())
                put("status", status.selectedItem.toString())
            }
            repairs.add(r); save()
            if (autoSms.isChecked && r.getString("phone").isNotBlank()) {
                sendStatusSms(r)
            }
            Toast.makeText(this, "ثبت شد؛ کد پذیرش: $code", Toast.LENGTH_LONG).show()
            showHome()
        })
        v.addView(button("بازگشت") { showHome() })
        setContentView(v)
    }

    private fun showRepairs() {
        val v = base()
        v.addView(text("لیست تعمیرات", 24f))
        list = v
        repairs.forEach { r ->
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 12, 0, 12) }
            box.addView(text("کد ${r.getString("code")} — ${r.getString("name")} — ${r.getString("model")}"))
            box.addView(text("وضعیت: ${r.getString("status")}"))
            val sp = Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("در انتظار بررسی", "در حال تعمیر", "آماده تحویل", "تحویل شد"))
                val current = arrayOf("در انتظار بررسی", "در حال تعمیر", "آماده تحویل", "تحویل شد").indexOf(r.getString("status"))
                setSelection(if (current >= 0) current else 0)
            }
            box.addView(sp)
            box.addView(button("ذخیره وضعیت") {
                val old = r.getString("status")
                val newStatus = sp.selectedItem.toString()
                r.put("status", newStatus); save()
                if (old != newStatus && autoSms.isChecked && r.getString("phone").isNotBlank()) sendStatusSms(r)
                Toast.makeText(this, "وضعیت ذخیره شد", Toast.LENGTH_SHORT).show()
                showRepairs()
            })
            box.addView(button("ارسال پیامک همین تعمیر") {
                if (r.getString("phone").isBlank()) Toast.makeText(this, "شماره مشتری ثبت نشده", Toast.LENGTH_SHORT).show()
                else sendStatusSms(r)
            })
            v.addView(box)
        }
        v.addView(button("بازگشت") { showHome() })
        setContentView(v)
    }

    private fun showSettings() {
        val v = base()
        v.addView(text("تنظیمات پیامک", 24f))
        autoSms = Switch(this).apply {
            text = "ارسال خودکار هنگام تغییر وضعیت"
            isChecked = getPreferences(0).getBoolean("autoSms", false)
            setOnCheckedChangeListener { _, b -> getPreferences(0).edit().putBoolean("autoSms", b).apply() }
        }
        v.addView(autoSms)
        v.addView(text("پیامک‌ها با سیم‌کارت خود گوشی ارسال می‌شوند؛ هزینه طبق تعرفه اپراتور شماست."))
        v.addView(button("فعال‌کردن مجوز پیامک") {
            if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
                permissionLauncher.launch(Manifest.permission.SEND_SMS)
            else Toast.makeText(this, "مجوز از قبل فعال است", Toast.LENGTH_SHORT).show()
        })
        v.addView(text("قالب پیامک:\nسلام {name}، دستگاه شما با کد پذیرش {code} اکنون {status} است."))
        v.addView(button("بازگشت") { showHome() })
        setContentView(v)
    }

    private fun sendStatusSms(r: JSONObject) {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.SEND_SMS)
            Toast.makeText(this, "اول مجوز پیامک را فعال کن و دوباره ارسال را بزن.", Toast.LENGTH_LONG).show()
            return
        }
        val msg = "سلام ${r.getString("name")}، دستگاه شما با کد پذیرش ${r.getString("code")} اکنون ${r.getString("status")} است."
        try {
            val sms = SmsManager.getDefault()
            val parts = sms.divideMessage(msg)
            val sent = PendingIntent.getBroadcast(this, 0, android.content.Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE)
            val delivered = PendingIntent.getBroadcast(this, 1, android.content.Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE)
            sms.sendMultipartTextMessage(r.getString("phone"), null, parts, arrayListOf(sent), arrayListOf(delivered))
            Toast.makeText(this, "پیامک ارسال شد", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در ارسال پیامک: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun save() {
        val a = JSONArray()
        repairs.forEach { a.put(it) }
        getPreferences(0).edit().putString("repairs", a.toString()).apply()
    }

    private fun load() {
        val s = getPreferences(0).getString("repairs", "[]") ?: "[]"
        val a = JSONArray(s)
        for (i in 0 until a.length()) repairs.add(a.getJSONObject(i))
        autoSms = Switch(this)
        autoSms.isChecked = getPreferences(0).getBoolean("autoSms", false)
    }
}
