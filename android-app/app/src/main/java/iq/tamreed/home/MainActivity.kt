package iq.tamreed.home

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Supabase
    private val SUPABASE_URL =
        "https://pmjmfeymnahpockjjafn.supabase.co"

    private val SUPABASE_KEY =
        "ضع_هنا_PUBLISHABLE_KEY"

    // ألوان التطبيق
    private val BLUE = Color.rgb(0, 91, 170)
    private val DARK_BLUE = Color.rgb(0, 63, 125)
    private val LIGHT_BLUE = Color.rgb(238, 247, 255)
    private val TEXT = Color.rgb(35, 45, 55)
    private val GREEN = Color.rgb(20, 150, 100)
    private val GRAY = Color.rgb(110, 120, 130)

    private val LOCATION_REQUEST = 1001

    private var currentPhone = ""
    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showWelcome()
    }

    // ---------------------------------------------------------
    // أدوات الواجهة
    // ---------------------------------------------------------

    private fun baseLayout(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

            setBackgroundColor(Color.WHITE)

            layoutDirection = View.LAYOUT_DIRECTION_RTL

            setPadding(24, 30, 24, 24)
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = TEXT
    ): TextView {

        return TextView(this).apply {

            text = value

            textSize = size

            setTextColor(color)

            gravity = Gravity.CENTER

            setPadding(10, 10, 10, 10)

            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
    }

    private fun button(
        value: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            text = value

            textSize = 17f

            setTextColor(Color.WHITE)

            setBackgroundColor(BLUE)

            isAllCaps = false

            setPadding(10, 5, 10, 5)

            layoutDirection = View.LAYOUT_DIRECTION_RTL

            setOnClickListener {
                action()
            }
        }
    }

    private fun input(
        hintText: String,
        phone: Boolean = false
    ): EditText {

        return EditText(this).apply {

            hint = hintText

            textSize = 17f

            setTextColor(TEXT)

            setHintTextColor(GRAY)

            setPadding(20, 5, 20, 5)

            layoutDirection = View.LAYOUT_DIRECTION_RTL

            if (phone) {
                inputType = InputType.TYPE_CLASS_PHONE
            }
        }
    }

    // ---------------------------------------------------------
    // شاشة البداية
    // ---------------------------------------------------------

    private fun showWelcome() {

        val root = baseLayout()

        val logo = TextView(this).apply {

            text = "⚕"

            textSize = 55f

            setTextColor(BLUE)

            gravity = Gravity.CENTER

            setPadding(0, 15, 0, 5)
        }

        root.addView(
            logo,
            LinearLayout.LayoutParams(
                -1,
                90
            )
        )

        root.addView(
            text(
                "التمريض المنزلي",
                34f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "خدمات التمريض والرعاية الصحية المنزلية",
                18f,
                TEXT
            )
        )

        root.addView(
            text(
                "محافظة الأنبار - العراق",
                17f,
                GRAY
            )
        )

        val line = View(this).apply {
            setBackgroundColor(BLUE)
        }

        root.addView(
            line,
            LinearLayout.LayoutParams(
                -1,
                5
            ).apply {
                setMargins(0, 25, 0, 25)
            }
        )

        root.addView(
            text(
                "مرحباً بك 👋\nاحصل على خدمة تمريض منزلية بسهولة وأمان",
                19f,
                DARK_BLUE
            )
        )

        root.addView(
            button("📱 تسجيل الدخول برقم الهاتف") {

                showPhoneLogin()
            },
            LinearLayout.LayoutParams(
                -1,
                65
            ).apply {
                setMargins(0, 25, 0, 10)
            }
        )

        root.addView(
            text(
                "التسجيل مطلوب لإنشاء الطلبات ومتابعتها",
                14f,
                GRAY
            )
        )

        setContentView(root)
    }

    // ---------------------------------------------------------
    // تسجيل الهاتف
    // ---------------------------------------------------------

    private fun showPhoneLogin() {

        val root = baseLayout()

        root.addView(
            text(
                "تسجيل الدخول",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل رقم هاتفك لإرسال رمز التحقق",
                17f,
                GRAY
            )
        )

        val phone = input(
            "رقم الهاتف مثال: +9647701234567",
            true
        )

        root.addView(
            phone,
            LinearLayout.LayoutParams(
                -1,
                65
            ).apply {
                setMargins(0, 30, 0, 15)
            }
        )

        root.addView(
            button("إرسال رمز التحقق") {

                val value = phone.text.toString().trim()

                if (value.isEmpty()) {

                    phone.error = "أدخل رقم الهاتف"

                    return@button
                }

                if (!value.startsWith("+")) {

                    phone.error =
                        "اكتب الرقم بالصيغة الدولية +964..."

                    return@button
                }

                currentPhone = value

                sendOtp(value)
            },
            LinearLayout.LayoutParams(
                -1,
                65
            )
        )

        root.addView(
            button("رجوع") {

                showWelcome()

            },
            LinearLayout.LayoutParams(
                -1,
                60
            ).apply {
                setMargins(0, 15, 0, 0)
            }
        )

        setContentView(root)
    }

    // ---------------------------------------------------------
    // إرسال OTP
    // ---------------------------------------------------------

    private fun sendOtp(phone: String) {

        Toast.makeText(
            this,
            "جاري إرسال رمز التحقق...",
            Toast.LENGTH_SHORT
        ).show()

        executor.execute {

            try {

                val url =
                    URL("$SUPABASE_URL/auth/v1/otp")

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"

                connection.setRequestProperty(
                    "apikey",
                    SUPABASE_KEY
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.doOutput = true

                val body =
                    JSONObject()
                        .put("phone", phone)
                        .toString()

                connection.outputStream.use {

                    it.write(
                        body.toByteArray(Charsets.UTF_8)
                    )
                }

                val code =
                    connection.responseCode

                handler.post {

                    if (code in 200..299) {

                        showOtpScreen()

                    } else {

                        Toast.makeText(
                            this,
                            "تعذر إرسال رمز التحقق. تأكد من إعدادات Supabase.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {

                handler.post {

                    Toast.makeText(
                        this,
                        "خطأ في الاتصال بالإنترنت",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------------------------------------------------
    // شاشة OTP
    // ---------------------------------------------------------

    private fun showOtpScreen() {

        val root = baseLayout()

        root.addView(
            text(
                "رمز التحقق",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل الرمز المرسل إلى:\n$currentPhone",
                17f,
                GRAY
            )
        )

        val otp = input(
            "أدخل رمز التحقق المكون من 6 أرقام"
        )

        otp.inputType =
            InputType.TYPE_CLASS_NUMBER

        otp.gravity = Gravity.CENTER

        root.addView(
            otp,
            LinearLayout.LayoutParams(
                -1,
                70
            ).apply {
                setMargins(0, 30, 0, 15)
            }
        )

        root.addView(
            button("تأكيد الرمز") {

                val code =
                    otp.text.toString().trim()

                if (code.length != 6) {

                    otp.error =
                        "أدخل الرمز المكون من 6 أرقام"

                    return@button
                }

                verifyOtp(
                    currentPhone,
                    code
                )
            },
            LinearLayout.LayoutParams(
                -1,
                65
            )
        )

        root.addView(
            button("إعادة إرسال الرمز") {

                sendOtp(currentPhone)
            },
            LinearLayout.LayoutParams(
                -1,
                60
            ).apply {
                setMargins(0, 12, 0, 0)
            }
        )

        setContentView(root)
    }

    // ---------------------------------------------------------
    // التحقق من OTP
    // ---------------------------------------------------------

    private fun verifyOtp(
        phone: String,
        token: String
    ) {

        Toast.makeText(
            this,
            "جاري التحقق...",
            Toast.LENGTH_SHORT
        ).show()

        executor.execute {

            try {

                val url =
                    URL("$SUPABASE_URL/auth/v1/verify")

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"

                connection.setRequestProperty(
                    "apikey",
                    SUPABASE_KEY
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.doOutput = true

                val body =
                    JSONObject()
                        .put("phone", phone)
                        .put("token", token)
                        .put("type", "sms")
                        .toString()

                connection.outputStream.use {

                    it.write(
                        body.toByteArray(Charsets.UTF_8)
                    )
                }

                val code =
                    connection.responseCode

                handler.post {

                    if (code in 200..299) {

                        Toast.makeText(
                            this,
                            "تم تسجيل الدخول بنجاح",
                            Toast.LENGTH_SHORT
                        ).show()

                        showHome()

                    } else {

                        Toast.makeText(
                            this,
                            "رمز التحقق غير صحيح أو منتهي",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {

                handler.post {

                    Toast.makeText(
                        this,
                        "حدث خطأ أثناء التحقق",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------------------------------------------------
    // الصفحة الرئيسية
    // ---------------------------------------------------------

    private fun showHome() {

        val root = baseLayout()

        root.addView(
            text(
                "التمريض المنزلي",
                32f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "خدمات الرعاية المنزلية - الأنبار",
                18f,
                GRAY
            )
        )

        val line = View(this).apply {
            setBackgroundColor(BLUE)
        }

        root.addView(
            line,
            LinearLayout.LayoutParams(
                -1,
                4
            ).apply {
                setMargins(0, 15, 0, 20)
            }
        )

        root.addView(
            button("🩺 طلب ممرض منزلي") {

                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                -1,
                70
            ).apply {
                setMargins(0, 5, 0, 10)
            }
        )

        root.addView(
            button("🏥 الخدمات التمريضية") {

                showServices()
            },
            LinearLayout.LayoutParams(
                -1,
                70
            ).apply {
                setMargins(0, 5, 0, 10)
            }
        )

        root.addView(
            button("📍 تحديد موقعي") {

                requestLocation()
            },
            LinearLayout.LayoutParams(
                -1,
                70
            ).apply {
                setMargins(0, 5, 0, 10)
            }
        )

        root.addView(
            button("📋 طلباتي") {

                showBookings()
            },
            LinearLayout.LayoutParams(
                -1,
                70
            ).apply {
                setMargins(0, 5, 0, 10)
            }
        )

        root.addView(
            button("☎️ التواصل معنا") {

                contactUs()
            },
            LinearLayout.LayoutParams(
                -1,
                70
            ).apply {
                setMargins(0, 5, 0, 10)
            }
        )

        root.addView(
            text(
                "خدمة التمريض المنزلي في محافظة الأنبار",
                14f,
                GRAY
            )
        )

        setContentView(root)
    }

    // ---------------------------------------------------------
    // الخدمات
    // ---------------------------------------------------------

    private fun showServices() {

        val root = baseLayout()

        root.addView(
            text(
                "الخدمات التمريضية",
                29f,
                DARK_BLUE
            )
        )

        val services = arrayOf(
            "💉 إعطاء الإبر والحقن",
            "🩹 تغيير الضمادات",
            "🩺 قياس ضغط الدم والسكر",
            "💊 إعطاء الأدوية حسب وصف الطبيب",
            "👴 رعاية كبار السن",
            "🏥 رعاية ما بعد العمليات",
            "🛏️ رعاية المرضى طريحي الفراش",
            "❤️ متابعة الحالات المنزلية"
        )

        for (service in services) {

            val item = TextView(this).apply {

                text = service

                textSize = 17f

                setTextColor(TEXT)

                gravity =
                    Gravity.RIGHT or
                    Gravity.CENTER_VERTICAL

                setPadding(20, 10, 20, 10)

                setBackgroundColor(LIGHT_BLUE)

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

            root.addView(
                item,
                LinearLayout.LayoutParams(
                    -1,
                    65
                ).apply {
                    setMargins(0, 6, 0, 6)
                }
            )
        }

        root.addView(
            button("رجوع") {

                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                65
            ).apply {
                setMargins(0, 20, 0, 0)
            }
        )

        setContentView(root)
    }

    // ---------------------------------------------------------
    // طلب ممرض
    // ---------------------------------------------------------

    private fun showRequestScreen() {

        val root = baseLayout()

        root.addView(
            text(
                "طلب ممرض منزلي",
                29f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل معلومات المريض ومكان الخدمة",
                16f,
                GRAY
            )
        )

        val name =
            input("اسم المريض")

        root.addView(
            name,
            LinearLayout.LayoutParams(
                -1,
                60
            ).apply {
                setMargins(0, 20, 0, 8)
            }
        )

        val phone =
            input("رقم الهاتف", true)

        phone.setText(currentPhone)

        root.addView(
            phone,
            LinearLayout.LayoutParams(
                -1,
                60
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        )

        val address =
            input("العنوان / المنطقة")

        root.addView(
            address,
            LinearLayout.LayoutParams(
                -1,
                60
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        )

        val notes =
            input("ملاحظات إضافية")

        notes.minLines = 4

        notes.gravity =
            Gravity.TOP or Gravity.RIGHT

        root.addView(
            notes,
            LinearLayout.LayoutParams(
                -1,
                120
            ).apply {
                setMargins(0, 8, 0, 12)
            }
        )

        val locationButton =
            button("📍 تحديد موقعي الآن") {

                requestLocation()
            }

        root.addView(
            locationButton,
            LinearLayout.LayoutParams(
                -1,
                60
            ).apply {
                setMargins(0, 5, 0, 10)
            }
        )

        root.addView(
            button("✅ إرسال طلب الممرض") {

                val patient =
                    name.text.toString().trim()

                val phoneValue =
                    phone.text.toString().trim()

                val addressValue =
                    address.text.toString().trim()

                val notesValue =
                    notes.text.toString().trim()

                if (patient.isEmpty()) {

                    name.error =
                        "أدخل اسم المريض"

                    return@button
                }

                if (phoneValue.isEmpty()) {

                    phone.error =
                        "أدخل رقم الهاتف"

                    return@button
                }

                if (addressValue.isEmpty()) {

                    address.error =
                        "أدخل العنوان"

                    return@button
                }

                createBooking(
                    patient,
                    phoneValue,
                    addressValue,
                    notesValue
                )
            },
            LinearLayout.LayoutParams(
                -1,
                70
            ).apply {
                setMargins(0, 10, 0, 10)
            }
        )

        root.addView(
            button("رجوع") {

                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                60
            )
        )

        setContentView(root)
    }

    // ---------------------------------------------------------
    // إنشاء الطلب في Supabase
    // ---------------------------------------------------------

    private fun createBooking(
        patient: String,
        phone: String,
        address: String,
        notes: String
    ) {

        Toast.makeText(
            this,
            "جاري إرسال الطلب...",
            Toast.LENGTH_SHORT
        ).show()

        executor.execute {

            try {

                val url =
                    URL(
                        "$SUPABASE_URL/rest/v1/bookings"
                    )

                val connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod = "POST"

                connection.setRequestProperty(
                    "apikey",
                    SUPABASE_KEY
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Prefer",
                    "return=minimal"
                )

                connection.doOutput = true

                val json =
                    JSONObject().apply {

                        put(
                            "patient_name",
                            patient
                        )

                        put(
                            "phone",
                            phone
                        )

                        put(
                            "address",
                            address
                        )

                        put(
                            "notes",
                            notes
                        )

                        put(
                            "status",
                            "pending"
                        )

                        if (currentLat != null) {
                            put(
                                "latitude",
                                currentLat
                            )
                        }

                        if (currentLng != null) {
                            put(
                                "longitude",
                                currentLng
                            )
                        }
                    }

                connection.outputStream.use {

                    it.write(
                        json.toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }

                val code =
                    connection.responseCode

                handler.post {

                    if (code in 200..299) {

                        AlertDialog.Builder(this)
                            .setTitle("تم إرسال الطلب ✅")
                            .setMessage(
                                "تم استلام طلب التمريض بنجاح.\n\n" +
                                "سيتم التواصل معك لتأكيد الطلب."
                            )
                            .setPositiveButton(
                                "حسناً"
                            ) { _, _ ->

                                showHome()
                            }
                            .show()

                    } else {

                        AlertDialog.Builder(this)
                            .setTitle("تعذر إرسال الطلب")
                            .setMessage(
                                "تأكد من إنشاء جدول bookings " +
                                "وإعداد صلاحيات Supabase."
                            )
                            .setPositiveButton(
                                "حسناً",
                                null
                            )
                            .show()
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {

                handler.post {

                    Toast.makeText(
                        this,
                        "خطأ في الاتصال بـ Supabase",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------------------------------------------------
    // الطلبات
    // ---------------------------------------------------------

    private fun showBookings() {

        val root = baseLayout()

        root.addView(
            text(
                "طلباتي",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "طلبات التمريض الخاصة بك",
                17f,
                GRAY
            )
        )

        root.addView(
            button("🩺 طلب ممرض جديد") {

                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                -1,
                65
            ).apply {
                setMargins(0, 25, 0, 10)
            }
        )

        root.addView(
            button("رجوع للرئيسية") {

                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                60
            )
        )

        setContentView(root)
    }

    // ---------------------------------------------------------
    // الموقع
    // ---------------------------------------------------------

    private fun requestLocation() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST
            )

            return
        }

        val manager =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager

        val gpsEnabled =
            manager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )

        if (!gpsEnabled) {

            AlertDialog.Builder(this)
                .setTitle("تفعيل الموقع")
                .setMessage(
                    "يجب تشغيل خدمة الموقع أولاً."
                )
                .setPositiveButton(
                    "حسناً",
                    null
                )
                .show()

            return
        }

        try {

            val location: Location? =
                manager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )

            if (location != null) {

                currentLat =
                    location.latitude

                currentLng =
                    location.longitude

                Toast.makeText(
                    this,
                    "تم تحديد موقعك بنجاح 📍",
                    Toast.LENGTH_LONG
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "لم يتم الحصول على الموقع بعد، حاول مرة أخرى.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: SecurityException) {

            Toast.makeText(
                this,
                "لا يوجد إذن للوصول إلى الموقع",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------------------------------------------------------
    // نتيجة إذن الموقع
    // ---------------------------------------------------------

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == LOCATION_REQUEST) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                requestLocation()

            } else {

                Toast.makeText(
                    this,
                    "لم يتم السماح بالوصول إلى الموقع",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---------------------------------------------------------
    // التواصل
    // ---------------------------------------------------------

    private fun contactUs() {

        AlertDialog.Builder(this)
            .setTitle("التواصل معنا")
            .setMessage(
                "التمريض المنزلي - محافظة الأنبار\n\n" +
                "سيتم إضافة الاتصال وواتساب في المرحلة القادمة."
            )
            .setPositiveButton(
                "حسناً",
                null
            )
            .show()
    }

    override fun onDestroy() {

        executor.shutdown()

        super.onDestroy()
    }
}
