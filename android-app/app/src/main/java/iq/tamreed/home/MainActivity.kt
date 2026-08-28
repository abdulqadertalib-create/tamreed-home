package iq.tamreed.home

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val BLUE = Color.rgb(0, 105, 210)
    private val DARK_BLUE = Color.rgb(0, 67, 135)
    private val LIGHT_BLUE = Color.rgb(235, 246, 255)
    private val GREEN = Color.rgb(28, 145, 85)
    private val RED = Color.rgb(200, 50, 50)
    private val TEXT = Color.rgb(35, 45, 55)
    private val GRAY = Color.rgb(110, 110, 110)
    private val LIGHT_GRAY = Color.rgb(245, 247, 250)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var phoneNumber = ""
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentLocationText = "لم يتم تحديد الموقع"
    private val LOCATION_REQUEST_CODE = 2001

    private val anbarCities = arrayOf(
        "اختر المدينة / المنطقة",
        "الرمادي", "الفلوجة", "الكرمة", "الصقلاوية", "الحبانية",
        "الخالدية", "عامرية الفلوجة", "هيت", "كبيسة", "حديثة",
        "البغدادي", "عانة", "راوة", "القائم", "الرطبة", "الوليد"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWelcome()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun baseLayout(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.TOP
        setBackgroundColor(Color.WHITE)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(20), dp(16), dp(20))
    }

    private fun scroll(content: View): ScrollView = ScrollView(this).apply {
        setBackgroundColor(Color.WHITE)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        addView(content)
    }

    private fun text(value: String, size: Float, color: Int = TEXT): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(8), dp(8), dp(8))
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private fun button(value: String, action: () -> Unit): Button = Button(this).apply {
        text = value
        textSize = 17f
        setTextColor(Color.WHITE)
        isAllCaps = false
        gravity = Gravity.CENTER
        background = roundedBackground(BLUE, dp(14))
        setPadding(dp(10), dp(8), dp(10), dp(8))
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setOnClickListener { action() }
    }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun addButton(root: LinearLayout, label: String, height: Int = 62, action: () -> Unit) {
        root.addView(
            button(label, action),
            LinearLayout.LayoutParams(-1, dp(height)).apply {
                setMargins(0, dp(5), 0, dp(7))
            }
        )
    }

    // =====================================================
    // 1. شاشة الترحيب
    // =====================================================

    private fun showWelcome() {
        val root = baseLayout()

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(30), dp(18), dp(28))
            background = roundedBackground(LIGHT_BLUE, dp(28))
        }
        hero.addView(text("🏥", 54f, DARK_BLUE))
        hero.addView(text("التمريض المنزلي", 31f, DARK_BLUE))
        hero.addView(text("رعاية صحية تصل إلى باب منزلك", 18f, TEXT))
        hero.addView(text("محافظة الأنبار • العراق", 15f, GRAY))
        root.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0,0,0,dp(22)) })

        root.addView(text("مرحباً بك 👋", 27f, DARK_BLUE))
        root.addView(text("اطلب خدمة تمريض منزلية بسهولة وأمان، وتابع طلبك من داخل التطبيق.", 16f, GRAY),
            LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(3),0,dp(18)) })

        addButton(root, "📱  تسجيل الدخول / إنشاء حساب", 66) { showPhoneLogin() }

        root.addView(text("لماذا التمريض المنزلي؟", 21f, DARK_BLUE),
            LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(25),0,dp(10)) })

        val features = listOf(
            "🩺" to "ممرضون وخدمات منزلية",
            "📍" to "تحديد موقع المريض",
            "📋" to "متابعة الطلبات",
            "🔒" to "تسجيل آمن برقم الهاتف"
        )
        for ((icon,title) in features) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(12),dp(8),dp(12),dp(8))
                background = roundedBackground(LIGHT_GRAY, dp(16))
            }
            card.addView(text(icon,26f,DARK_BLUE), LinearLayout.LayoutParams(dp(55),dp(55)))
            card.addView(text(title,16f,TEXT), LinearLayout.LayoutParams(0,dp(55),1f))
            root.addView(card, LinearLayout.LayoutParams(-1,dp(65)).apply { setMargins(0,dp(4),0,dp(4)) })
        }
        root.addView(text("خدمة التمريض المنزلي على مدار الساعة", 14f, GRAY),
            LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(18),0,0) })
        setContentView(scroll(root))
    }

    // =====================================================
    // 2. تسجيل الدخول برقم الهاتف
    // =====================================================

    private fun showPhoneLogin() {
        val root = baseLayout()

        root.addView(text("📱 تسجيل الدخول", 30f, DARK_BLUE))
        root.addView(
            text("أدخل رقم هاتفك العراقي وسنرسل لك رمز التحقق عبر SMS", 17f, GRAY),
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(15)) }
        )

        val phone = EditText(this).apply {
            hint = "07701234567"
            textSize = 19f
            inputType = InputType.TYPE_CLASS_PHONE
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(15), dp(10), dp(15), dp(10))
        }

        root.addView(phone, LinearLayout.LayoutParams(-1, dp(65)).apply {
            setMargins(0, dp(10), 0, dp(8))
        })

        root.addView(text("الصيغة المقبولة: 07xxxxxxxxx أو +9647xxxxxxxxx", 14f, GRAY))

        addButton(root, "📨  إرسال رمز التحقق", 66) {
            val normalized = normalizeIraqPhone(phone.text.toString().trim())
            if (normalized == null) {
                phone.error = "أدخل رقم هاتف عراقي صحيح"
                return@addButton
            }
            phoneNumber = normalized
            sendOtp()
        }

        addButton(root, "↩️  رجوع", 55) {
            showWelcome()
        }

        setContentView(scroll(root))
    }

    private fun normalizeIraqPhone(value: String): String? {
        var phone = value.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")

        if (phone.startsWith("+964")) {
            return if (phone.length == 14 && phone.getOrNull(4) == '7') phone else null
        }

        if (phone.startsWith("00964")) {
            phone = "+" + phone.substring(2)
            return if (phone.length == 14 && phone.getOrNull(4) == '7') phone else null
        }

        if (phone.startsWith("07")) {
            phone = "+964" + phone.substring(1)
            return if (phone.length == 14 && phone.getOrNull(4) == '7') phone else null
        }

        return null
    }

    private fun sendOtp() {
        val loading = ProgressDialog(this).apply {
            setMessage("جاري إرسال رمز التحقق...")
            setCancelable(false)
            show()
        }

        scope.launch {
            try {
                SupabaseManager.client.auth.signInWith(OTP) {
                    phone = phoneNumber
                }

                loading.dismiss()
                Toast.makeText(this@MainActivity, "تم إرسال رمز التحقق", Toast.LENGTH_LONG).show()
                showOtpScreen()
            } catch (e: Exception) {
                loading.dismiss()
                showError("تعذر إرسال الرمز", e.message ?: "حدث خطأ غير معروف")
            }
        }
    }

    // =====================================================
    // 3. شاشة OTP
    // =====================================================

    private fun showOtpScreen() {
        val root = baseLayout()

        root.addView(text("🔐 تأكيد رقم الهاتف", 30f, DARK_BLUE))
        root.addView(text("أدخل رمز التحقق المكوّن من 6 أرقام", 17f, GRAY))
        root.addView(text(phoneNumber, 19f, DARK_BLUE))

        val otp = EditText(this).apply {
            hint = "000000"
            textSize = 28f
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            maxLines = 1
            filters = arrayOf(InputFilter.LengthFilter(6))
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }

        root.addView(otp, LinearLayout.LayoutParams(-1, dp(75)).apply {
            setMargins(0, dp(22), 0, dp(10))
        })

        addButton(root, "✅  تأكيد الرمز", 66) {
            val code = otp.text.toString().trim()
            if (code.length != 6) {
                otp.error = "أدخل 6 أرقام"
                return@addButton
            }
            verifyOtp(code)
        }

        addButton(root, "🔄  إرسال الرمز مرة أخرى", 60) {
            sendOtp()
        }

        addButton(root, "📱  تغيير رقم الهاتف", 55) {
            showPhoneLogin()
        }

        setContentView(scroll(root))
    }

    private fun verifyOtp(code: String) {
        val loading = ProgressDialog(this).apply {
            setMessage("جاري التحقق...")
            setCancelable(false)
            show()
        }

        scope.launch {
            try {
                SupabaseManager.client.auth.verifyPhoneOtp(
                    type = OtpType.Phone.SMS,
                    phone = phoneNumber,
                    token = code
                )

                loading.dismiss()
                Toast.makeText(this@MainActivity, "تم تسجيل الدخول بنجاح ✅", Toast.LENGTH_LONG).show()
                showServicesHome()
            } catch (e: Exception) {
                loading.dismiss()
                showError("رمز التحقق غير صحيح", e.message ?: "تأكد من الرمز وحاول مرة أخرى")
            }
        }
    }

    // =====================================================
    // 4. الخدمات بعد تسجيل الدخول
    // =====================================================

    private fun showServicesHome() {
        if (phoneNumber.isBlank()) { showPhoneLogin(); return }

        val root = baseLayout()

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14),dp(14),dp(14),dp(14))
            background = roundedBackground(LIGHT_BLUE, dp(24))
        }
        top.addView(text("🏥",42f,DARK_BLUE), LinearLayout.LayoutParams(dp(62),dp(62)))
        val titleBox=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_VERTICAL }
        titleBox.addView(text("التمريض المنزلي",24f,DARK_BLUE))
        titleBox.addView(text("الأنبار • العراق",14f,GRAY))
        top.addView(titleBox, LinearLayout.LayoutParams(0,dp(65),1f))
        root.addView(top, LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,dp(14)) })

        val account = LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12),dp(8),dp(12),dp(8)); background=roundedBackground(LIGHT_GRAY,dp(18))
        }
        account.addView(text("👤",28f,DARK_BLUE),LinearLayout.LayoutParams(dp(58),dp(58)))
        val accText=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_VERTICAL }
        accText.addView(text("مرحباً بك",15f,GRAY))
        accText.addView(text(phoneNumber,17f,DARK_BLUE))
        account.addView(accText,LinearLayout.LayoutParams(0,dp(62),1f))
        root.addView(account,LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,dp(18)) })

        root.addView(text("ماذا تحتاج اليوم؟",24f,DARK_BLUE))
        root.addView(text("اختر الخدمة المناسبة لك",14f,GRAY),LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,dp(10)) })
        addButton(root,"🩺  اطلب ممرضاً الآن",70){ showRequestScreen() }

        root.addView(text("خدمات سريعة",20f,DARK_BLUE),LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(18),0,dp(8)) })
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; layoutDirection=View.LAYOUT_DIRECTION_RTL }
        row.addView(serviceCard("🩺","قياس الضغط","متابعة الضغط"),LinearLayout.LayoutParams(0,dp(142),1f).apply{setMargins(0,dp(3),dp(4),dp(3))})
        row.addView(serviceCard("🩸","قياس السكر","فحص السكر"),LinearLayout.LayoutParams(0,dp(142),1f).apply{setMargins(dp(4),dp(3),0,dp(3))})
        root.addView(row)

        root.addView(text("الخدمات والمتابعة",20f,DARK_BLUE),LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(18),0,dp(8))})
        addButton(root,"🏥  جميع الخدمات التمريضية",60){showServices()}
        addButton(root,"📋  طلباتي",60){showBookings()}
        addButton(root,"📍  تحديد موقع الطلب",60){showLocation()}
        addButton(root,"☎️  تواصل معنا",60){contactUs()}
        addButton(root,"↩️  العودة إلى شاشة الترحيب",54){showWelcome()}

        root.addView(text("التمريض المنزلي • رعاية أقرب وأسهل",13f,GRAY),LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(12),0,dp(4))})
        setContentView(scroll(root))
    }

    private fun serviceCard(icon: String, title: String, subtitle: String): LinearLayout = LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; layoutDirection=View.LAYOUT_DIRECTION_RTL
        setPadding(dp(8),dp(8),dp(8),dp(8)); background=roundedBackground(LIGHT_BLUE,dp(20)); setOnClickListener{showRequestScreen()}
        addView(text(icon,34f,DARK_BLUE)); addView(text(title,18f,DARK_BLUE)); addView(text(subtitle,13f,GRAY))
    }

    // =====================================================
    // 5. طلب التمريض
    // =====================================================

    private fun showRequestScreen() {
        if (phoneNumber.isBlank()) {
            showPhoneLogin()
            return
        }

        val root = baseLayout()
        root.addView(text("🩺 طلب ممرض منزلي", 29f, DARK_BLUE))
        root.addView(text("حدد الخدمة ومكان المريض ثم أرسل الطلب", 17f, GRAY))

        val services = arrayOf(
            "اختر الخدمة",
            "قياس ضغط الدم",
            "قياس السكر",
            "إعطاء حقنة",
            "تغيير الضماد",
            "تركيب المحلول",
            "رعاية كبار السن",
            "رعاية المرضى في المنزل",
            "متابعة حالة صحية",
            "خدمة تمريض أخرى"
        )
        val service = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, services)
        }
        root.addView(service, LinearLayout.LayoutParams(-1, dp(60)).apply {
            setMargins(0, dp(18), 0, dp(12))
        })

        root.addView(text("📍 مدينة / منطقة المريض", 17f, DARK_BLUE))
        val city = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, anbarCities)
        }
        root.addView(city, LinearLayout.LayoutParams(-1, dp(60)).apply {
            setMargins(0, dp(5), 0, dp(12))
        })

        val patient = EditText(this).apply {
            hint = "اسم المريض"
            textSize = 17f
            gravity = Gravity.RIGHT
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(dp(15), dp(10), dp(15), dp(10))
        }
        root.addView(patient, LinearLayout.LayoutParams(-1, dp(60)).apply {
            setMargins(0, dp(5), 0, dp(10))
        })

        val notes = EditText(this).apply {
            hint = "ملاحظات إضافية عن الحالة"
            textSize = 17f
            gravity = Gravity.TOP or Gravity.RIGHT
            minLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }
        root.addView(notes, LinearLayout.LayoutParams(-1, dp(130)).apply {
            setMargins(0, dp(5), 0, dp(12))
        })

        addButton(root, "📍  تحديد موقع المريض", 62) {
            showLocation()
        }

        addButton(root, "📨  إرسال طلب التمريض", 68) {
            if (service.selectedItemPosition == 0) {
                Toast.makeText(this, "اختر الخدمة أولاً", Toast.LENGTH_SHORT).show()
                return@addButton
            }
            if (city.selectedItemPosition == 0) {
                Toast.makeText(this, "اختر المدينة أو المنطقة أولاً", Toast.LENGTH_SHORT).show()
                return@addButton
            }
            val patientName = patient.text.toString().trim()
            if (patientName.isEmpty()) {
                patient.error = "أدخل اسم المريض"
                return@addButton
            }

            confirmRequest(
                service.selectedItem.toString(),
                city.selectedItem.toString(),
                patientName,
                notes.text.toString()
            )
        }

        addButton(root, "↩️  رجوع", 55) { showServicesHome() }
        setContentView(scroll(root))
    }

    private fun confirmRequest(service: String, city: String, patient: String, notes: String) {
        val location = if (currentLatitude != null && currentLongitude != null) {
            "متوفر GPS: %.6f, %.6f".format(currentLatitude, currentLongitude)
        } else {
            "لم يتم تحديد GPS"
        }

        AlertDialog.Builder(this)
            .setTitle("تأكيد طلب التمريض")
            .setMessage(
                "الخدمة: $service\n\n" +
                        "المدينة / المنطقة: $city\n\n" +
                        "المريض: $patient\n\n" +
                        "الموقع: $location\n\n" +
                        "الملاحظات: ${if (notes.trim().isEmpty()) "لا توجد" else notes.trim()}\n\n" +
                        "هل تريد إرسال الطلب؟"
            )
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("إرسال") { _, _ ->
                Toast.makeText(this, "تم إنشاء طلب التمريض بنجاح ✅", Toast.LENGTH_LONG).show()
                showBookings()
            }
            .show()
    }

    // =====================================================
    // 6. جميع الخدمات
    // =====================================================

    private fun showServices() {
        val root = baseLayout()
        root.addView(text("🏥 الخدمات التمريضية", 29f, DARK_BLUE))
        root.addView(text("اختر الخدمة التي تحتاجها وسيتم تحويلك إلى نموذج الطلب", 16f, GRAY))

        val services = listOf(
            "💉 إعطاء الحقن" to "حسب وصف الطبيب",
            "🩹 تغيير الضمادات" to "العناية بالجروح",
            "🩸 قياس السكر" to "فحص مستوى السكر",
            "🩺 قياس الضغط" to "قياس ومتابعة الضغط",
            "💧 تركيب المحاليل" to "خدمة المحاليل الوريدية",
            "👴 رعاية كبار السن" to "رعاية ومتابعة كبار السن",
            "🛏️ رعاية المرضى" to "الرعاية المنزلية للمرضى",
            "📋 متابعة صحية" to "متابعة الحالة الصحية"
        )

        for ((title, description) in services) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = roundedBackground(LIGHT_GRAY, dp(15))
                setOnClickListener { showRequestScreen() }
            }
            box.addView(text(title, 19f, DARK_BLUE))
            box.addView(text(description, 14f, GRAY))
            root.addView(box, LinearLayout.LayoutParams(-1, dp(82)).apply {
                setMargins(0, dp(5), 0, dp(5))
            })
        }

        addButton(root, "🩺  طلب خدمة الآن", 65) { showRequestScreen() }
        addButton(root, "↩️  رجوع", 55) { showServicesHome() }
        setContentView(scroll(root))
    }

    // =====================================================
    // 7. الموقع و GPS
    // =====================================================

    private fun showLocation() {
        val root = baseLayout()
        root.addView(text("📍 تحديد موقع المريض", 29f, DARK_BLUE))
        root.addView(text("يساعد الموقع الممرض على الوصول إلى المكان الصحيح", 17f, GRAY))

        val status = text(currentLocationText, 17f, DARK_BLUE)
        root.addView(status, LinearLayout.LayoutParams(-1, dp(125)).apply {
            setMargins(0, dp(20), 0, dp(10))
        })

        addButton(root, "📍 الحصول على موقعي الحالي", 65) {
            status.text = "جاري تحديد موقعك..."
            getCurrentLocation(status)
        }

        addButton(root, "🗺️ فتح الموقع في خرائط Google", 60) {
            openCurrentLocationInMaps()
        }

        addButton(root, "↩️  رجوع", 55) { showServicesHome() }
        setContentView(scroll(root))
    }

    private fun getCurrentLocation(statusView: TextView) {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    currentLocationText = "تم تحديد الموقع بنجاح ✅\n\n" +
                            "خط العرض: %.6f\nخط الطول: %.6f".format(location.latitude, location.longitude)
                    statusView.text = currentLocationText
                    Toast.makeText(this, "تم تحديد موقعك بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    statusView.text = "تعذر الحصول على الموقع الحالي.\nتأكد من تشغيل GPS ثم حاول مرة أخرى."
                }
            }
            .addOnFailureListener { error ->
                statusView.text = "حدث خطأ أثناء تحديد الموقع:\n${error.message ?: "خطأ غير معروف"}"
            }
    }

    private fun openCurrentLocationInMaps() {
        val lat = currentLatitude
        val lon = currentLongitude

        if (lat == null || lon == null) {
            Toast.makeText(this, "حدد موقعك أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح خرائط Google", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "تم السماح بالموقع. اضغط تحديد الموقع مرة أخرى.", Toast.LENGTH_LONG).show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("صلاحية الموقع مطلوبة")
                    .setMessage("يحتاج التطبيق إلى موقعك لتسهيل وصول الممرض إلى المريض.")
                    .setPositiveButton("الإعدادات") { _, _ ->
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName")
                            )
                        )
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            }
        }
    }

    // =====================================================
    // 8. الطلبات
    // =====================================================

    private fun showBookings() {
        val root = baseLayout()
        root.addView(text("📋 طلباتي", 30f, DARK_BLUE))
        root.addView(text("يمكنك هنا متابعة طلبات التمريض الخاصة بك", 16f, GRAY))

        val empty = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(15), dp(30), dp(15), dp(30))
            background = roundedBackground(LIGHT_GRAY, dp(18))
        }
        empty.addView(text("📭", 48f))
        empty.addView(text("لا توجد طلبات حالياً", 21f, DARK_BLUE))
        empty.addView(text("عند إنشاء طلب تمريض سيظهر هنا", 15f, GRAY))

        root.addView(empty, LinearLayout.LayoutParams(-1, dp(185)).apply {
            setMargins(0, dp(25), 0, dp(15))
        })

        addButton(root, "🩺  إنشاء طلب جديد", 65) { showRequestScreen() }
        addButton(root, "↩️  رجوع", 55) { showServicesHome() }
        setContentView(scroll(root))
    }

    // =====================================================
    // 9. التواصل والأخطاء
    // =====================================================

    private fun contactUs() {
        AlertDialog.Builder(this)
            .setTitle("☎️ تواصل معنا")
            .setMessage(
                "التمريض المنزلي\n" +
                        "محافظة الأنبار - العراق\n\n" +
                        "للاستفسارات والمساعدة تواصل مع إدارة الخدمة."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showError(title: String, message: String) {
        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show()
    }
}
