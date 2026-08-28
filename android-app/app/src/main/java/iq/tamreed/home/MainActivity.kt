package iq.tamreed.home

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.content.SharedPreferences
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
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class BookingInsert(
    val patient_id: String,
    val service_id: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val scheduled_at: String,
    val status: String = "PENDING",
    val notes: String? = null
)

@Serializable
data class BookingRow(
    val id: String,
    val patient_id: String,
    val nurse_id: String? = null,
    val service_id: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val scheduled_at: String,
    val status: String,
    val notes: String? = null,
    val created_at: String
)

// واجهة محسّنة مع الحفاظ على منطق Supabase وتسجيل الدخول والطلبات الحالي.

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
    private lateinit var prefs: SharedPreferences

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
        prefs = getSharedPreferences("tamreed_home", MODE_PRIVATE)
        phoneNumber = prefs.getString("phone", "") ?: ""

        // نبدأ من الشاشة المناسبة حسب جلسة Supabase الحالية.
        scope.launch {
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                if (user != null) {
                    showServicesHome()
                } else {
                    showWelcome()
                }
            } catch (_: Exception) {
                showWelcome()
            }
        }
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
        textSize = 16.5f
        setTextColor(Color.WHITE)
        isAllCaps = false
        gravity = Gravity.CENTER
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(BLUE, DARK_BLUE)
        ).apply {
            cornerRadius = dp(18).toFloat()
        }
        elevation = dp(4).toFloat()
        setPadding(dp(14), dp(8), dp(14), dp(8))
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        minHeight = dp(56)
        setOnClickListener { action() }
    }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun outlinedBackground(): GradientDrawable = GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dp(16).toFloat()
        setStroke(dp(1), Color.rgb(220, 228, 238))
    }

    private fun sectionTitle(title: String, subtitle: String? = null): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.RIGHT
            setPadding(dp(4), dp(4), dp(4), dp(4))
            addView(text(title, 21f, DARK_BLUE))
            if (!subtitle.isNullOrBlank()) addView(text(subtitle, 14f, GRAY))
        }

    private fun inputField(hintText: String, multiLine: Boolean = false): EditText =
        EditText(this).apply {
            hint = hintText
            textSize = 17f
            setTextColor(TEXT)
            setHintTextColor(GRAY)
            gravity = if (multiLine) Gravity.TOP or Gravity.RIGHT else Gravity.RIGHT
            inputType = if (multiLine) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            } else InputType.TYPE_CLASS_TEXT
            background = outlinedBackground()
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            if (multiLine) minLines = 3
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
        hero.addView(text("✚", 52f, BLUE))
        hero.addView(text("التمريض المنزلي", 31f, DARK_BLUE))
        hero.addView(text("رعاية صحية تصل إلى باب منزلك", 18f, TEXT))
        hero.addView(text("الأنبار • العراق", 15f, GRAY))
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

        val phone = inputField("07701234567").apply {
            textSize = 19f
            inputType = InputType.TYPE_CLASS_PHONE
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
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

        val otp = inputField("000000").apply {
            textSize = 28f
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            maxLines = 1
            filters = arrayOf(InputFilter.LengthFilter(6))
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

                prefs.edit().putString("phone", phoneNumber).apply()
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
        if (phoneNumber.isBlank()) {
            showPhoneLogin()
            return
        }

        val root = baseLayout().apply {
            setPadding(dp(14), dp(16), dp(14), dp(24))
        }

        // ==============================
        // رأس الصفحة الاحترافي
        // ==============================
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(DARK_BLUE, BLUE)
            ).apply { cornerRadius = dp(26).toFloat() }
        }

        val logo = text("🏥", 36f, Color.WHITE).apply {
            gravity = Gravity.CENTER
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(58), dp(58)))

        val headerText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        headerText.addView(text("التمريض المنزلي", 23f, Color.WHITE))
        headerText.addView(text("رعاية صحية تصل إلى باب منزلك", 13.5f, Color.WHITE))
        header.addView(headerText, LinearLayout.LayoutParams(0, dp(65), 1f))

        val profileButton = TextView(this).apply {
            text = "👤"
            textSize = 25f
            gravity = Gravity.CENTER
            background = roundedBackground(Color.WHITE, dp(18))
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("حسابي")
                    .setMessage("رقم الهاتف:\n$phoneNumber\n\nالحساب المسجل في تطبيق التمريض المنزلي")
                    .setPositiveButton("إغلاق", null)
                    .show()
            }
        }
        header.addView(profileButton, LinearLayout.LayoutParams(dp(58), dp(58)).apply {
            setMargins(dp(6), 0, 0, 0)
        })

        root.addView(header, LinearLayout.LayoutParams(-1, dp(86)).apply {
            setMargins(0, 0, 0, dp(16))
        })

        // ==============================
        // بطاقة الترحيب
        // ==============================
        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.RIGHT
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedBackground(LIGHT_BLUE, dp(22))
        }
        welcome.addView(text("مرحباً بك 👋", 24f, DARK_BLUE))
        welcome.addView(text("ماذا تحتاج اليوم؟", 19f, TEXT))
        welcome.addView(text("اختر الخدمة وسنساعدك في إرسال طلب تمريض منزلي بسهولة.", 14f, GRAY))
        root.addView(welcome, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, dp(14))
        })

        // ==============================
        // زر الطلب الرئيسي
        // ==============================
        val requestButton = button("🩺   اطلب ممرضاً الآن", {
            showRequestScreen()
        }).apply {
            textSize = 18f
            elevation = dp(6).toFloat()
        }
        root.addView(requestButton, LinearLayout.LayoutParams(-1, dp(70)).apply {
            setMargins(0, 0, 0, dp(18))
        })

        // ==============================
        // الخدمات السريعة
        // ==============================
        root.addView(sectionTitle("خدمات سريعة", "الخدمات الأكثر استخداماً"),
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) })

        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        quickRow.addView(
            serviceCard("🩺", "قياس الضغط", "متابعة الضغط", 2),
            LinearLayout.LayoutParams(0, dp(145), 1f).apply {
                setMargins(0, 0, dp(5), 0)
            }
        )
        quickRow.addView(
            serviceCard("🩸", "قياس السكر", "فحص السكر", 2),
            LinearLayout.LayoutParams(0, dp(145), 1f).apply {
                setMargins(dp(5), 0, 0, 0)
            }
        )
        root.addView(quickRow, LinearLayout.LayoutParams(-1, dp(145)).apply {
            setMargins(0, 0, 0, dp(18))
        })

        // ==============================
        // حالة الطلب
        // ==============================
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = roundedBackground(LIGHT_GRAY, dp(18))
            setOnClickListener { showBookings() }
        }
        statusCard.addView(text("📋", 28f, DARK_BLUE), LinearLayout.LayoutParams(dp(48), dp(55)))
        val statusInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        statusInfo.addView(text("طلباتك", 17f, DARK_BLUE))
        statusInfo.addView(text("اضغط لمتابعة حالة الطلبات السابقة", 13f, GRAY))
        statusCard.addView(statusInfo, LinearLayout.LayoutParams(0, dp(58), 1f))
        root.addView(statusCard, LinearLayout.LayoutParams(-1, dp(76)).apply {
            setMargins(0, 0, 0, dp(16))
        })

        // ==============================
        // الوصول السريع
        // ==============================
        root.addView(sectionTitle("الوصول السريع", "كل ما تحتاجه في مكان واحد"),
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) })

        addButton(root, "🏥   جميع الخدمات التمريضية", 62) {
            showServices()
        }
        addButton(root, "📋   طلباتي ومتابعة الطلب", 62) {
            showBookings()
        }
        addButton(root, "📍   تحديد موقع الطلب", 62) {
            showLocation()
        }
        addButton(root, "☎️   تواصل معنا", 62) {
            contactUs()
        }

        // ==============================
        // معلومات الحساب
        // ==============================
        val accountCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = outlinedBackground()
        }
        accountCard.addView(text("🔐", 25f, DARK_BLUE), LinearLayout.LayoutParams(dp(50), dp(50)))
        val accountInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        accountInfo.addView(text("الحساب المسجل", 13f, GRAY))
        accountInfo.addView(text(phoneNumber, 16f, DARK_BLUE))
        accountCard.addView(accountInfo, LinearLayout.LayoutParams(0, dp(55), 1f))
        root.addView(accountCard, LinearLayout.LayoutParams(-1, dp(72)).apply {
            setMargins(0, dp(12), 0, dp(10))
        })

        addButton(root, "🚪   تسجيل الخروج", 55) {
            logoutUser()
        }

        root.addView(
            text("التمريض المنزلي • محافظة الأنبار - العراق", 13f, GRAY),
            LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, dp(12), 0, 0)
            }
        )

        setContentView(scroll(root))
    }

    private fun serviceCard(icon: String, title: String, subtitle: String, serviceIndex: Int): LinearLayout = LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; layoutDirection=View.LAYOUT_DIRECTION_RTL
        setPadding(dp(8),dp(8),dp(8),dp(8)); background=roundedBackground(LIGHT_BLUE,dp(20)); setOnClickListener{showRequestScreen(serviceIndex)}
        addView(text(icon,34f,DARK_BLUE)); addView(text(title,18f,DARK_BLUE)); addView(text(subtitle,13f,GRAY))
    }

    // =====================================================
    // 5. طلب التمريض
    // =====================================================

    private fun showRequestScreen(preselectedServiceIndex: Int = 0) {
        if (phoneNumber.isBlank()) {
            showPhoneLogin()
            return
        }

        val root = baseLayout()

        root.addView(text("🩺 طلب ممرض منزلي", 30f, DARK_BLUE))
        root.addView(
            text(
                "أخبرنا بما تحتاج وسنجهز طلب الزيارة إلى منزلك",
                16f,
                GRAY
            ),
            LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, dp(3), 0, dp(18))
            }
        )

        // بطاقة معلومات المستخدم
        val accountCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundedBackground(LIGHT_BLUE, dp(18))
        }
        accountCard.addView(text("👤 الحساب المسجل", 14f, GRAY))
        accountCard.addView(text(phoneNumber, 17f, DARK_BLUE))
        root.addView(accountCard, LinearLayout.LayoutParams(-1, dp(75)).apply {
            setMargins(0, 0, 0, dp(18))
        })

        root.addView(text("1  •  اختر الخدمة", 19f, DARK_BLUE))
        root.addView(text("ما الخدمة التي تحتاجها؟", 14f, GRAY),
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(2), 0, dp(8)) })

        val services = arrayOf(
            "اختر الخدمة",
            "زيارة تمريض منزلية",
            "قياس ضغط وسكر",
            "تغيير الضماد"
        )
        val serviceIds = arrayOf(
            "",
            "11111111-1111-4111-8111-111111111111",
            "22222222-2222-4222-8222-222222222222",
            "33333333-3333-4333-8333-333333333333"
        )

        val service = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                services
            )
        }
        root.addView(service, LinearLayout.LayoutParams(-1, dp(58)).apply {
            setMargins(0, 0, 0, dp(16))
        })
        if (preselectedServiceIndex in services.indices && preselectedServiceIndex > 0) {
            service.setSelection(preselectedServiceIndex)
        }

        root.addView(text("2  •  موقع الزيارة", 19f, DARK_BLUE))
        root.addView(text("اختر المدينة ثم حدد موقع المنزل", 14f, GRAY),
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(2), 0, dp(8)) })

        val city = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                anbarCities
            )
        }
        root.addView(city, LinearLayout.LayoutParams(-1, dp(58)).apply {
            setMargins(0, 0, 0, dp(10))
        })

        val locationStatus = text(
            if (currentLatitude != null && currentLongitude != null)
                "📍 الموقع محفوظ ويمكن استخدامه للطلب"
            else currentLocationText,
            15f,
            GRAY
        )
        val locationCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.RIGHT
            setPadding(dp(15), dp(10), dp(15), dp(10))
            background = roundedBackground(LIGHT_GRAY, dp(16))
            addView(text("موقع GPS", 13f, GRAY))
            addView(locationStatus)
        }
        root.addView(locationCard, LinearLayout.LayoutParams(-1, dp(72)).apply {
            setMargins(0, 0, 0, dp(7))
        })

        addButton(root, "📍  تحديد موقعي الحالي", 58) {
            locationStatus.text = "جاري تحديد الموقع..."
            getCurrentLocation(locationStatus)
        }

        root.addView(text("3  •  بيانات المريض", 19f, DARK_BLUE),
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(10), 0, 0) })

        val patient = inputField("اسم المريض")
        root.addView(patient, LinearLayout.LayoutParams(-1, dp(60)).apply {
            setMargins(0, dp(7), 0, dp(8))
        })

        val address = inputField("المحلة، الشارع، أقرب نقطة دالة...", true).apply {
            textSize = 16f
        }
        root.addView(address, LinearLayout.LayoutParams(-1, dp(105)).apply {
            setMargins(0, 0, 0, dp(15))
        })

        root.addView(text("4  •  موعد الزيارة", 19f, DARK_BLUE))
        val schedule = text("لم يتم اختيار موعد الزيارة", 16f, DARK_BLUE)
        var scheduledAt = ""

        val scheduleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedBackground(LIGHT_BLUE, dp(17))
            addView(schedule)
        }
        root.addView(scheduleCard, LinearLayout.LayoutParams(-1, dp(68)).apply {
            setMargins(0, dp(7), 0, dp(7))
        })

        addButton(root, "📅  اختيار التاريخ والوقت", 58) {
            chooseSchedule(schedule) { iso -> scheduledAt = iso }
        }

        root.addView(text("5  •  ملاحظات للممرض", 19f, DARK_BLUE),
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(12), 0, 0) })

        val notes = inputField("أي ملاحظة مهمة عن المريض أو الزيارة...", true).apply {
            textSize = 16f
        }
        root.addView(notes, LinearLayout.LayoutParams(-1, dp(110)).apply {
            setMargins(0, dp(7), 0, dp(16))
        })

        val summary = text(
            "بعد الضغط على مراجعة الطلب ستظهر لك جميع التفاصيل قبل الإرسال.",
            14f,
            GRAY
        )
        root.addView(summary, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, dp(8))
        })

        addButton(root, "✅  مراجعة الطلب وإرساله", 68) {
            if (service.selectedItemPosition == 0) {
                Toast.makeText(this, "اختر الخدمة أولاً", Toast.LENGTH_SHORT).show()
                return@addButton
            }
            if (city.selectedItemPosition == 0) {
                Toast.makeText(this, "اختر المدينة أو المنطقة", Toast.LENGTH_SHORT).show()
                return@addButton
            }

            val patientName = patient.text.toString().trim()
            val addressText = address.text.toString().trim()

            if (patientName.isEmpty()) {
                patient.error = "أدخل اسم المريض"
                patient.requestFocus()
                return@addButton
            }
            if (addressText.isEmpty()) {
                address.error = "أدخل عنوان المنزل"
                address.requestFocus()
                return@addButton
            }
            if (scheduledAt.isBlank()) {
                Toast.makeText(this, "اختر موعد الزيارة", Toast.LENGTH_SHORT).show()
                return@addButton
            }

            confirmAndCreateBooking(
                serviceIds[service.selectedItemPosition],
                service.selectedItem.toString(),
                patientName,
                "${city.selectedItem} - $addressText",
                scheduledAt,
                notes.text.toString().trim()
            )
        }

        addButton(root, "↩️  العودة للرئيسية", 55) { showServicesHome() }
        setContentView(scroll(root))
    }

    private fun chooseSchedule(target: TextView, onSelected: (String) -> Unit) {
        val now = java.util.Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val selected = java.util.Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }

                        val display = String.format(
                            java.util.Locale("ar"),
                            "%02d/%02d/%04d - %02d:%02d",
                            day, month + 1, year, hour, minute
                        )

                        val iso = java.text.SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ssXXX",
                            java.util.Locale.US
                        ).apply {
                            timeZone = java.util.TimeZone.getDefault()
                        }.format(selected.time)

                        target.text = "📅 $display"
                        onSelected(iso)
                    },
                    now.get(java.util.Calendar.HOUR_OF_DAY),
                    now.get(java.util.Calendar.MINUTE),
                    true
                ).show()
            },
            now.get(java.util.Calendar.YEAR),
            now.get(java.util.Calendar.MONTH),
            now.get(java.util.Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    private fun confirmAndCreateBooking(
        serviceId: String,
        serviceName: String,
        patientName: String,
        address: String,
        scheduledAt: String,
        notes: String
    ) {
        val gps = if (currentLatitude != null && currentLongitude != null) {
            "%.6f, %.6f".format(currentLatitude, currentLongitude)
        } else {
            "غير محدد"
        }

        AlertDialog.Builder(this)
            .setTitle("تأكيد الطلب")
            .setMessage(
                "الخدمة: $serviceName\n\n" +
                    "المريض: $patientName\n\n" +
                    "العنوان: $address\n\n" +
                    "الموعد: $scheduledAt\n\n" +
                    "GPS: $gps\n\n" +
                    "هل تريد إرسال الطلب؟"
            )
            .setNegativeButton("تعديل", null)
            .setPositiveButton("إرسال الطلب") { _, _ ->
                createBooking(serviceId, patientName, address, scheduledAt, notes)
            }
            .show()
    }

    private fun createBooking(
        serviceId: String,
        patientName: String,
        address: String,
        scheduledAt: String,
        notes: String
    ) {
        val loading = ProgressDialog(this).apply {
            setMessage("جاري حفظ الطلب...")
            setCancelable(false)
            show()
        }

        scope.launch {
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                    ?: throw IllegalStateException("انتهت جلسة تسجيل الدخول. سجّل الدخول مرة أخرى.")

                val payload = BookingInsert(
                    patient_id = user.id,
                    service_id = serviceId,
                    address = "$patientName - $address",
                    latitude = currentLatitude,
                    longitude = currentLongitude,
                    scheduled_at = scheduledAt,
                    status = "PENDING",
                    notes = notes.ifBlank { null }
                )

                SupabaseManager.client.from("bookings").insert(payload)

                loading.dismiss()
                Toast.makeText(
                    this@MainActivity,
                    "تم إرسال الطلب بنجاح ✅",
                    Toast.LENGTH_LONG
                ).show()
                showBookings()
            } catch (e: Exception) {
                loading.dismiss()
                showError(
                    "تعذر إرسال الطلب",
                    e.message ?: "تأكد من اتصال الإنترنت وإعدادات Supabase."
                )
            }
        }
    }

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
        root.addView(
            text("طلباتك المحفوظة في النظام وحالتها الحالية", 16f, GRAY),
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(3), 0, dp(12)) }
        )

        val loading = text("جاري تحميل الطلبات...", 16f, GRAY)
        root.addView(loading, LinearLayout.LayoutParams(-1, dp(90)))

        addButton(root, "🩺 إنشاء طلب جديد", 65) { showRequestScreen() }
        addButton(root, "↩️ رجوع", 55) { showServicesHome() }

        setContentView(scroll(root))

        scope.launch {
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                    ?: throw IllegalStateException("انتهت جلسة تسجيل الدخول. سجّل الدخول مرة أخرى.")

                val bookings = SupabaseManager.client
                    .from("bookings")
                    .select {
                        filter {
                            eq("patient_id", user.id)
                        }
                    }
                    .decodeList<BookingRow>()

                root.removeView(loading)

                if (bookings.isEmpty()) {
                    root.addView(
                        text("📭\nلا توجد طلبات حالياً", 20f, DARK_BLUE),
                        1,
                        LinearLayout.LayoutParams(-1, dp(160)).apply {
                            setMargins(0, dp(15), 0, dp(10))
                        }
                    )
                    return@launch
                }

                for (booking in bookings.sortedByDescending { it.created_at }) {
                    val serviceName = when (booking.service_id) {
                        "11111111-1111-4111-8111-111111111111" -> "زيارة تمريض منزلية"
                        "22222222-2222-4222-8222-222222222222" -> "قياس ضغط وسكر"
                        "33333333-3333-4333-8333-333333333333" -> "تغيير الضماد"
                        else -> "خدمة تمريضية"
                    }

                    val statusArabic = when (booking.status) {
                        "PENDING" -> "بانتظار قبول الممرض"
                        "ACCEPTED" -> "تم قبول الطلب"
                        "ON_THE_WAY" -> "الممرض في الطريق"
                        "IN_PROGRESS" -> "الزيارة قيد التنفيذ"
                        "COMPLETED" -> "اكتملت الزيارة"
                        "CANCELLED" -> "تم إلغاء الطلب"
                        else -> booking.status
                    }

                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.RIGHT
                        layoutDirection = View.LAYOUT_DIRECTION_RTL
                        setPadding(dp(15), dp(12), dp(15), dp(12))
                        background = roundedBackground(LIGHT_BLUE, dp(18))
                    }

                    card.addView(text("🩺 $serviceName", 18f, DARK_BLUE))
                    card.addView(text("📍 ${booking.address}", 15f, TEXT))
                    card.addView(text("📅 ${booking.scheduled_at}", 14f, GRAY))
                    card.addView(text("الحالة: $statusArabic", 16f, GREEN))

                    root.addView(
                        card,
                        LinearLayout.LayoutParams(-1, dp(150)).apply {
                            setMargins(0, dp(5), 0, dp(8))
                        }
                    )
                }
            } catch (e: Exception) {
                loading.text = "تعذر تحميل الطلبات.\n${e.message ?: "خطأ غير معروف"}"
            }
        }
    }

    // 9. التواصل والأخطاء
    // =====================================================

    private fun logoutUser() {
        AlertDialog.Builder(this)
            .setTitle("تسجيل الخروج")
            .setMessage("هل تريد تسجيل الخروج من حسابك؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("تسجيل الخروج") { _, _ ->
                scope.launch {
                    try {
                        SupabaseManager.client.auth.signOut()
                    } catch (_: Exception) {
                        // حتى لو تعذر الاتصال، نمسح بيانات الدخول المحلية.
                    }
                    phoneNumber = ""
                    currentLatitude = null
                    currentLongitude = null
                    currentLocationText = "لم يتم تحديد الموقع"
                    prefs.edit().remove("phone").apply()
                    showWelcome()
                }
            }
            .show()
    }

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
