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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

@Serializable
data class BookingInsert(
    val patient_id: String,
    val service_id: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
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
    val status: String,
    val notes: String? = null,
    val created_at: String
)

data class ServiceItem(
    val id: String,
    val name: String,
    val description: String
)

private val APP_SERVICES = listOf(
    ServiceItem(
        "11111111-1111-4111-8111-111111111111",
        "زيارة تمريض منزلية",
        "زيارة ممرض إلى منزل المريض"
    ),
    ServiceItem(
        "22222222-2222-4222-8222-222222222222",
        "قياس ضغط وسكر",
        "قياس ومتابعة ضغط الدم وسكر الدم"
    ),
    ServiceItem(
        "33333333-3333-4333-8333-333333333333",
        "تغيير الضماد",
        "العناية بالجروح وتغيير الضمادات"
    ),
    ServiceItem(
        "44444444-4444-4444-8444-444444444444",
        "إعطاء الحقن",
        "إعطاء الحقن حسب وصف الطبيب"
    ),
    ServiceItem(
        "55555555-5555-4555-8555-555555555555",
        "تركيب المحاليل",
        "تركيب ومتابعة المحاليل الوريدية"
    ),
    ServiceItem(
        "66666666-6666-4666-8666-666666666666",
        "رعاية كبار السن",
        "رعاية ومتابعة كبار السن في المنزل"
    )
)

class MainActivity : AppCompatActivity() {

    private val BLUE = Color.rgb(0, 105, 210)
    private val DARK_BLUE = Color.rgb(0, 67, 135)
    private val LIGHT_BLUE = Color.rgb(235, 246, 255)
    private val GREEN = Color.rgb(28, 145, 85)
    private val RED = Color.rgb(200, 50, 50)
    private val TEXT = Color.rgb(35, 45, 55)
    private val GRAY = Color.rgb(110, 110, 110)
    private val LIGHT_GRAY = Color.rgb(245, 247, 250)

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var phoneNumber = ""

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    private var currentLocationText =
        "لم يتم تحديد الموقع"

    private val LOCATION_REQUEST_CODE = 2001

    private val anbarCities = arrayOf(
        "اختر المدينة / المنطقة",
        "الرمادي",
        "الفلوجة",
        "الكرمة",
        "الصقلاوية",
        "الحبانية",
        "الخالدية",
        "عامرية الفلوجة",
        "هيت",
        "كبيسة",
        "حديثة",
        "البغدادي",
        "عانة",
        "راوة",
        "القائم",
        "الرطبة",
        "الوليد"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWelcome()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun baseLayout(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setBackgroundColor(Color.WHITE)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(
                dp(16),
                dp(20),
                dp(16),
                dp(20)
            )
        }

    private fun scroll(content: View): ScrollView =
        ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            addView(content)
        }

    private fun text(
        value: String,
        size: Float,
        color: Int = TEXT
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

    private fun button(
        value: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            text = value
            textSize = 17f
            setTextColor(Color.WHITE)
            isAllCaps = false
            gravity = Gravity.CENTER
            background = roundedBackground(
                BLUE,
                dp(18)
            )
            elevation = dp(3).toFloat()
            setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )
            layoutDirection =
                View.LAYOUT_DIRECTION_RTL
            minHeight = dp(56)
            setOnClickListener {
                action()
            }
        }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun outlinedBackground(): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(16).toFloat()
            setStroke(
                dp(1),
                Color.rgb(220, 228, 238)
            )
        }

    private fun sectionTitle(
        title: String,
        subtitle: String? = null
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection =
                View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.RIGHT
            setPadding(
                dp(4),
                dp(4),
                dp(4),
                dp(4)
            )

            addView(
                text(
                    title,
                    21f,
                    DARK_BLUE
                )
            )

            if (!subtitle.isNullOrBlank()) {
                addView(
                    text(
                        subtitle,
                        14f,
                        GRAY
                    )
                )
            }
        }

    private fun inputField(
        hintText: String,
        multiLine: Boolean = false
    ): EditText =
        EditText(this).apply {
            hint = hintText
            textSize = 17f
            setTextColor(TEXT)
            setHintTextColor(GRAY)

            gravity =
                if (multiLine) {
                    Gravity.TOP or Gravity.RIGHT
                } else {
                    Gravity.RIGHT
                }

            inputType =
                if (multiLine) {
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE
                } else {
                    InputType.TYPE_CLASS_TEXT
                }

            background = outlinedBackground()

            setPadding(
                dp(16),
                dp(10),
                dp(16),
                dp(10)
            )

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            if (multiLine) {
                minLines = 3
            }
        }

    private fun addButton(
        root: LinearLayout,
        label: String,
        height: Int = 62,
        action: () -> Unit
    ) {
        root.addView(
            button(label, action),
            LinearLayout.LayoutParams(
                -1,
                dp(height)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(7)
                )
            }
        )
    }

    // =====================================================
    // شاشة الترحيب
    // =====================================================

    private fun showWelcome() {

        val root = baseLayout()

        val hero =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setPadding(
                    dp(18),
                    dp(30),
                    dp(18),
                    dp(28)
                )

                background =
                    roundedBackground(
                        LIGHT_BLUE,
                        dp(28)
                    )
            }

        hero.addView(
            text("✚", 52f, BLUE)
        )

        hero.addView(
            text(
                "التمريض المنزلي",
                31f,
                DARK_BLUE
            )
        )

        hero.addView(
            text(
                "رعاية صحية تصل إلى باب منزلك",
                18f
            )
        )

        hero.addView(
            text(
                "الأنبار • العراق",
                15f,
                GRAY
            )
        )

        root.addView(
            hero,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    dp(22)
                )
            }
        )

        root.addView(
            text(
                "مرحباً بك 👋",
                27f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "اطلب خدمة تمريض منزلية بسهولة وأمان، وتابع طلبك من داخل التطبيق.",
                16f,
                GRAY
            )
        )

        addButton(
            root,
            "📱 تسجيل الدخول / إنشاء حساب",
            66
        ) {
            showPhoneLogin()
        }

        addButton(
            root,
            "👨‍⚕️ لوحة الممرض",
            60
        ) {
            try {
                startActivity(
                    Intent(
                        this,
                        NurseActivity::class.java
                    )
                )
            } catch (e: Exception) {
                showError(
                    "لوحة الممرض",
                    "تعذر فتح لوحة الممرض"
                )
            }
        }

        root.addView(
            text(
                "لماذا التمريض المنزلي؟",
                21f,
                DARK_BLUE
            ),
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                setMargins(
                    0,
                    dp(25),
                    0,
                    dp(10)
                )
            }
        )

        val features = listOf(
            "🩺" to "ممرضون وخدمات منزلية",
            "📍" to "تحديد موقع المريض",
            "📋" to "متابعة الطلبات",
            "🔒" to "تسجيل آمن برقم الهاتف"
        )

        for ((icon, title) in features) {

            val card =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                    layoutDirection =
                        View.LAYOUT_DIRECTION_RTL

                    setPadding(
                        dp(12),
                        dp(8),
                        dp(12),
                        dp(8)
                    )

                    background =
                        roundedBackground(
                            LIGHT_GRAY,
                            dp(16)
                        )
                }

            card.addView(
                text(
                    icon,
                    26f,
                    DARK_BLUE
                ),
                LinearLayout.LayoutParams(
                    dp(55),
                    dp(55)
                )
            )

            card.addView(
                text(
                    title,
                    16f
                ),
                LinearLayout.LayoutParams(
                    0,
                    dp(55),
                    1f
                )
            )

            root.addView(
                card,
                LinearLayout.LayoutParams(
                    -1,
                    dp(65)
                ).apply {
                    setMargins(
                        0,
                        dp(4),
                        0,
                        dp(4)
                    )
                }
            )
        }

        root.addView(
            text(
                "خدمة التمريض المنزلي على مدار الساعة",
                14f,
                GRAY
            )
        )

        setContentView(scroll(root))
    }

    // =====================================================
    // تسجيل الهاتف
    // =====================================================

    private fun showPhoneLogin() {

        val root = baseLayout()

        root.addView(
            text(
                "📱 تسجيل الدخول",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل رقم هاتفك العراقي وسنرسل لك رمز التحقق عبر SMS",
                17f,
                GRAY
            )
        )

        val phone =
            inputField("07701234567").apply {
                textSize = 19f
                inputType =
                    InputType.TYPE_CLASS_PHONE
                gravity = Gravity.CENTER
                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR
            }

        root.addView(
            phone,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(15),
                    0,
                    dp(8)
                )
            }
        )
           private fun showPhoneLogin() {

        val root = baseLayout()

        root.addView(
            text(
                "📱 تسجيل الدخول",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل رقم هاتفك العراقي وسنرسل لك رمز التحقق عبر SMS",
                17f,
                GRAY
            )
        )

        val phone =
            inputField("07701234567").apply {
                textSize = 19f
                inputType =
                    InputType.TYPE_CLASS_PHONE
                gravity = Gravity.CENTER
                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR
            }

        root.addView(
            phone,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(15),
                    0,
                    dp(8)
                )
            }
        )

        addButton(
            root,
            "إرسال رمز التحقق 📲",
            64
        ) {

            val raw =
                phone.text.toString().trim()

            if (raw.isBlank()) {
                Toast.makeText(
                    this,
                    "أدخل رقم الهاتف أولاً",
                    Toast.LENGTH_SHORT
                ).show()
                return@addButton
            }

            val normalized =
                normalizeIraqiPhone(raw)

            if (normalized == null) {
                Toast.makeText(
                    this,
                    "رقم الهاتف العراقي غير صحيح",
                    Toast.LENGTH_SHORT
                ).show()
                return@addButton
            }

            phoneNumber = normalized

            sendOtp(normalized)
        }

        addButton(
            root,
            "رجوع ↩️",
            58
        ) {
            showWelcome()
        }

        root.addView(
            text(
                "مثال: 07701234567",
                14f,
                GRAY
            )
        )

        setContentView(scroll(root))
    }

    private fun normalizeIraqiPhone(
        value: String
    ): String? {

        var phone =
            value
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")

        if (phone.startsWith("+964")) {
            phone =
                "0" + phone.substring(4)
        } else if (phone.startsWith("00964")) {
            phone =
                "0" + phone.substring(5)
        }

        if (
            phone.length != 11 ||
            !phone.startsWith("07")
        ) {
            return null
        }

        return phone
    }

    // =====================================================
    // إرسال OTP
    // =====================================================

    private fun sendOtp(
        phone: String
    ) {

        val progress =
            ProgressDialog(this).apply {
                setMessage("جاري إرسال رمز التحقق...")
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                withContext(Dispatchers.IO) {

                    SupabaseManager.client.auth
                        .signInWith(OTP) {
                            this.phone = phone
                        }
                }

                progress.dismiss()

                showOtpScreen(phone)

            } catch (e: Exception) {

                progress.dismiss()

                showError(
                    "تعذر إرسال الرمز",
                    e.message
                        ?: "حدث خطأ أثناء إرسال رمز التحقق"
                )
            }
        }
    }

    // =====================================================
    // شاشة OTP
    // =====================================================

    private fun showOtpScreen(
        phone: String
    ) {

        val root = baseLayout()

        root.addView(
            text(
                "🔐 رمز التحقق",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل الرمز المرسل إلى:",
                17f,
                GRAY
            )
        )

        root.addView(
            text(
                phone,
                20f,
                DARK_BLUE
            )
        )

        val otp =
            inputField("أدخل رمز التحقق").apply {

                textSize = 23f

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                gravity =
                    Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR

                filters = arrayOf(
                    InputFilter.LengthFilter(6)
                )
            }

        root.addView(
            otp,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            ).apply {
                setMargins(
                    0,
                    dp(18),
                    0,
                    dp(10)
                )
            }
        )

        addButton(
            root,
            "تأكيد الرمز ✅",
            64
        ) {

            val code =
                otp.text.toString().trim()

            if (code.length < 4) {

                Toast.makeText(
                    this,
                    "أدخل رمز التحقق",
                    Toast.LENGTH_SHORT
                ).show()

                return@addButton
            }

            verifyOtp(
                phone,
                code
            )
        }

        addButton(
            root,
            "إعادة إرسال الرمز 🔄",
            58
        ) {
            sendOtp(phone)
        }

        addButton(
            root,
            "تغيير رقم الهاتف",
            58
        ) {
            showPhoneLogin()
        }

        setContentView(scroll(root))
    }

    private fun verifyOtp(
        phone: String,
        code: String
    ) {

        val progress =
            ProgressDialog(this).apply {
                setMessage("جاري التحقق...")
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                withContext(Dispatchers.IO) {

                    SupabaseManager.client.auth
                        .verifyPhoneOtp(
                            type = OtpType.SMS,
                            phone = phone,
                            token = code
                        )
                }

                progress.dismiss()

                savePatientProfile()

            } catch (e: Exception) {

                progress.dismiss()

                showError(
                    "رمز التحقق غير صحيح",
                    e.message
                        ?: "تأكد من الرمز وحاول مرة أخرى"
                )
            }
        }
    }

    // =====================================================
    // حفظ ملف المريض
    // =====================================================

    @Serializable
    data class ProfileInsert(
        val id: String,
        val phone: String,
        val role: String = "patient"
    )

    private fun savePatientProfile() {

        val user =
            SupabaseManager.client.auth.currentUserOrNull()

        if (user == null) {
            showError(
                "خطأ",
                "لم يتم العثور على حساب المستخدم"
            )
            return
        }

        val userId =
            user.id.toString()

        val progress =
            ProgressDialog(this).apply {
                setMessage("جاري تجهيز الحساب...")
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                withContext(Dispatchers.IO) {

                    SupabaseManager.client
                        .from("profiles")
                        .upsert(
                            ProfileInsert(
                                id = userId,
                                phone = phoneNumber,
                                role = "patient"
                            )
                        )
                }

                progress.dismiss()

                showPatientHome()

            } catch (e: Exception) {

                progress.dismiss()

                /*
                 * إذا كان الملف الشخصي موجوداً مسبقاً
                 * يمكن الانتقال إلى الصفحة الرئيسية.
                 */
                showPatientHome()
            }
        }
    }

    // =====================================================
    // الصفحة الرئيسية للمريض
    // =====================================================

    private fun showPatientHome() {

        val root = baseLayout()

        root.addView(
            text(
                "التمريض المنزلي 🩺",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "اختر الخدمة التي تحتاجها",
                18f,
                GRAY
            )
        )

        val currentUser =
            SupabaseManager.client.auth
                .currentUserOrNull()

        if (currentUser != null) {

            root.addView(
                text(
                    "الحساب: ${phoneNumber.ifBlank { "مستخدم مسجل" }}",
                    14f,
                    GRAY
                )
            )
        }

        addButton(
            root,
            "📝 إنشاء طلب جديد",
            66
        ) {
            showCreateBooking()
        }

        addButton(
            root,
            "📋 طلباتي",
            66
        ) {
            showMyBookings()
        }

        addButton(
            root,
            "📍 تحديد موقعي",
            60
        ) {
            requestLocation()
        }

        addButton(
            root,
            "🚪 تسجيل الخروج",
            58
        ) {

            scope.launch {

                try {

                    withContext(Dispatchers.IO) {

                        SupabaseManager.client.auth
                            .signOut()
                    }

                } catch (_: Exception) {
                }

                phoneNumber = ""

                showWelcome()
            }
        }

        root.addView(
            text(
                "خدماتنا",
                22f,
                DARK_BLUE
            ),
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                setMargins(
                    0,
                    dp(20),
                    0,
                    dp(8)
                )
            }
        )

        for (service in APP_SERVICES) {

            val card =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity = Gravity.CENTER

                    layoutDirection =
                        View.LAYOUT_DIRECTION_RTL

                    setPadding(
                        dp(14),
                        dp(10),
                        dp(14),
                        dp(10)
                    )

                    background =
                        roundedBackground(
                            LIGHT_BLUE,
                            dp(18)
                        )
                }

            card.addView(
                text(
                    service.name,
                    19f,
                    DARK_BLUE
                )
            )

            card.addView(
                text(
                    service.description,
                    14f,
                    GRAY
                )
            )

            root.addView(
                card,
                LinearLayout.LayoutParams(
                    -1,
                    dp(86)
                ).apply {
                    setMargins(
                        0,
                        dp(4),
                        0,
                        dp(4)
                    )
                }
            )
        }

        setContentView(scroll(root))
    }

    // =====================================================
    // إنشاء طلب جديد — بدون موعد
    // =====================================================

    private fun showCreateBooking() {

        val root = baseLayout()

        root.addView(
            text(
                "📝 إنشاء طلب جديد",
                29f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "الطلب فوري ولا يحتاج إلى اختيار تاريخ أو وقت",
                16f,
                GRAY
            )
        )

        root.addView(
            sectionTitle(
                "1️⃣ اختر الخدمة",
                "حدد الخدمة المطلوبة"
            )
        )

        val serviceSpinner =
            Spinner(this).apply {
                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        val serviceNames =
            mutableListOf("اختر الخدمة")

        serviceNames.addAll(
            APP_SERVICES.map {
                it.name
            }
        )

        serviceSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                serviceNames
            )

        root.addView(
            serviceSpinner,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                setMargins(
                    0,
                    dp(6),
                    0,
                    dp(12)
                )
            }
        )

        root.addView(
            sectionTitle(
                "2️⃣ اختر المدينة / المنطقة",
                "حدد موقع الخدمة"
            )
        )

        val citySpinner =
            Spinner(this).apply {
                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        citySpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                anbarCities
            )

        root.addView(
            citySpinner,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                setMargins(
                    0,
                    dp(6),
                    0,
                    dp(12)
                )
            }
        )

        root.addView(
            sectionTitle(
                "3️⃣ اسم المريض"
            )
        )

        val patientName =
            inputField("اسم المريض")

        root.addView(
            patientName,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(6),
                    0,
                    dp(12)
                )
            }
        )

        root.addView(
            sectionTitle(
                "4️⃣ عنوان المنزل",
                "المحلة، الشارع، أقرب نقطة..."
            )
        )

        val address =
            inputField(
                "اكتب عنوان المنزل بالتفصيل",
                true
            )

        root.addView(
            address,
            LinearLayout.LayoutParams(
                -1,
                dp(115)
            ).apply {
                setMargins(
                    0,
                    dp(6),
                    0,
                    dp(12)
                )
            }
        )

        root.addView(
            sectionTitle(
                "5️⃣ موقع المريض",
                "يمكنك استخدام موقعك الحالي"
            )
        )

        val locationText =
            text(
                currentLocationText,
                16f,
                GRAY
            )

        root.addView(
            locationText,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        addButton(
            root,
            "📍 تحديد موقعي الحالي",
            62
        ) {

            requestLocation {

                locationText.text =
                    currentLocationText
            }
        }

        root.addView(
            sectionTitle(
                "6️⃣ ملاحظات",
                "اختياري"
            )
        )

        val notes =
            inputField(
                "اكتب أي ملاحظات مهمة...",
                true
            )

        root.addView(
            notes,
            LinearLayout.LayoutParams(
                -1,
                dp(110)
            ).apply {
                setMargins(
                    0,
                    dp(6),
                    0,
                    dp(12)
                )
            }
        )

        addButton(
            root,
            "إرسال الطلب الآن 🚑",
            68
        ) {

            if (serviceSpinner.selectedItemPosition <= 0) {

                Toast.makeText(
                    this,
                    "اختر الخدمة أولاً",
                    Toast.LENGTH_SHORT
                ).show()

                return@addButton
            }

            if (citySpinner.selectedItemPosition <= 0) {

                Toast.makeText(
                    this,
                    "اختر المدينة / المنطقة",
                    Toast.LENGTH_SHORT
                ).show()

                return@addButton
            }

            val name =
                patientName.text.toString().trim()

            if (name.isBlank()) {

                Toast.makeText(
                    this,
                    "أدخل اسم المريض",
                    Toast.LENGTH_SHORT
                ).show()

                return@addButton
            }

            val addressText =
                address.text.toString().trim()

            if (addressText.isBlank()) {

                Toast.makeText(
                    this,
                    "أدخل عنوان المنزل",
                    Toast.LENGTH_SHORT
                ).show()

                return@addButton
            }

            val selectedCity =
                citySpinner.selectedItem.toString()

            val fullAddress =
                "$selectedCity - $addressText"

            val selectedService =
                APP_SERVICES[
                    serviceSpinner.selectedItemPosition - 1
                ]

            createBooking(
                service = selectedService,
                address = fullAddress,
                notes =
                    notes.text.toString().trim()
                        .ifBlank { null }
            )
        }

        addButton(
            root,
            "رجوع ↩️",
            58
        ) {
            showPatientHome()
        }

        setContentView(scroll(root))
           // =====================================================
    // الموقع الجغرافي
    // =====================================================

    private fun requestLocation(
        onSuccess: (() -> Unit)? = null
    ) {

        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (
            fine != PackageManager.PERMISSION_GRANTED &&
            coarse != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )

            return
        }

        val progress =
            ProgressDialog(this).apply {
                setMessage("جاري تحديد موقعك...")
                setCancelable(false)
                show()
            }

        val locationClient =
            LocationServices
                .getFusedLocationProviderClient(this)

        locationClient.lastLocation
            .addOnSuccessListener { location: Location? ->

                progress.dismiss()

                if (location == null) {

                    Toast.makeText(
                        this,
                        "تعذر الحصول على موقعك الحالي. تأكد من تشغيل GPS.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                currentLatitude =
                    location.latitude

                currentLongitude =
                    location.longitude

                currentLocationText =
                    "تم تحديد الموقع بنجاح ✅\n" +
                    "خط العرض: %.6f\n".format(
                        location.latitude
                    ) +
                    "خط الطول: %.6f".format(
                        location.longitude
                    )

                Toast.makeText(
                    this,
                    "تم تحديد موقعك بنجاح 📍",
                    Toast.LENGTH_SHORT
                ).show()

                onSuccess?.invoke()
            }
            .addOnFailureListener { error ->

                progress.dismiss()

                Toast.makeText(
                    this,
                    error.message
                        ?: "تعذر تحديد الموقع",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =====================================================
    // إنشاء الطلب في Supabase
    // =====================================================

    private fun createBooking(
        service: ServiceItem,
        address: String,
        notes: String?
    ) {

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()

        if (user == null) {

            showError(
                "تسجيل الدخول مطلوب",
                "يجب تسجيل الدخول أولاً قبل إرسال الطلب."
            )

            showPhoneLogin()

            return
        }

        val progress =
            ProgressDialog(this).apply {
                setMessage("جاري إرسال الطلب...")
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                val payload =
                    BookingInsert(
                        patient_id =
                            user.id.toString(),

                        service_id =
                            service.id,

                        address =
                            address,

                        latitude =
                            currentLatitude,

                        longitude =
                            currentLongitude,

                        status =
                            "PENDING",

                        notes =
                            notes
                    )

                withContext(Dispatchers.IO) {

                    SupabaseManager
                        .client
                        .from("bookings")
                        .insert(payload)
                }

                progress.dismiss()

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("تم إرسال الطلب ✅")
                    .setMessage(
                        "تم تسجيل طلب التمريض بنجاح.\n\n" +
                        "سيظهر الطلب لدى إدارة التمريض والممرض."
                    )
                    .setPositiveButton(
                        "عرض طلباتي"
                    ) { _, _ ->
                        showMyBookings()
                    }
                    .setNegativeButton(
                        "الرئيسية"
                    ) { _, _ ->
                        showPatientHome()
                    }
                    .show()

            } catch (e: Exception) {

                progress.dismiss()

                showError(
                    "تعذر إرسال الطلب",
                    e.message
                        ?: "حدث خطأ أثناء الاتصال بقاعدة البيانات."
                )
            }
        }
    }

    // =====================================================
    // طلباتي
    // =====================================================

    private fun showMyBookings() {

        val root =
            baseLayout()

        root.addView(
            text(
                "📋 طلباتي",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "طلبات التمريض الخاصة بك",
                17f,
                GRAY
            ),
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                setMargins(
                    0,
                    dp(3),
                    0,
                    dp(15)
                )
            }
        )

        val loadingText =
            text(
                "جاري تحميل الطلبات...",
                16f,
                GRAY
            )

        root.addView(
            loadingText,
            LinearLayout.LayoutParams(
                -1,
                dp(90)
            )
        )

        addButton(
            root,
            "📝 إنشاء طلب جديد",
            64
        ) {
            showCreateBooking()
        }

        addButton(
            root,
            "رجوع إلى الرئيسية ↩️",
            58
        ) {
            showPatientHome()
        }

        setContentView(
            scroll(root)
        )

        scope.launch {

            try {

                val user =
                    SupabaseManager
                        .client
                        .auth
                        .currentUserOrNull()
                        ?: throw IllegalStateException(
                            "يجب تسجيل الدخول أولاً."
                        )

                val bookings =
                    withContext(Dispatchers.IO) {

                        SupabaseManager
                            .client
                            .from("bookings")
                            .select {
                                filter {
                                    eq(
                                        "patient_id",
                                        user.id
                                    )
                                }
                            }
                            .decodeList<BookingRow>()
                    }

                root.removeView(
                    loadingText
                )

                if (bookings.isEmpty()) {

                    root.addView(
                        text(
                            "📭\n\nلا توجد طلبات حالياً",
                            21f,
                            DARK_BLUE
                        ),
                        LinearLayout.LayoutParams(
                            -1,
                            dp(180)
                        ).apply {
                            setMargins(
                                0,
                                dp(10),
                                0,
                                dp(10)
                            )
                        }
                    )

                    return@launch
                }

                val sortedBookings =
                    bookings.sortedByDescending {
                        it.created_at
                    }

                for (booking in sortedBookings) {

                    val serviceName =
                        getServiceName(
                            booking.service_id
                        )

                    val statusInfo =
                        getStatusInfo(
                            booking.status
                        )

                    val card =
                        LinearLayout(
                            this@MainActivity
                        ).apply {

                            orientation =
                                LinearLayout.VERTICAL

                            gravity =
                                Gravity.RIGHT

                            layoutDirection =
                                View.LAYOUT_DIRECTION_RTL

                            setPadding(
                                dp(15),
                                dp(13),
                                dp(15),
                                dp(13)
                            )

                            background =
                                roundedBackground(
                                    LIGHT_BLUE,
                                    dp(20)
                                )
                        }

                    card.addView(
                        text(
                            "🩺 $serviceName",
                            19f,
                            DARK_BLUE
                        )
                    )

                    card.addView(
                        text(
                            "👤 ${extractPatientName(booking.address)}",
                            15f,
                            TEXT
                        )
                    )

                    card.addView(
                        text(
                            "📍 ${extractAddress(booking.address)}",
                            15f,
                            TEXT
                        )
                    )

                    card.addView(
                        text(
                            "📌 الحالة: ${statusInfo.first}",
                            16f,
                            statusInfo.second
                        )
                    )

                    if (!booking.notes.isNullOrBlank()) {

                        card.addView(
                            text(
                                "📝 ${booking.notes}",
                                14f,
                                GRAY
                            )
                        )
                    }

                    card.addView(
                        text(
                            "🕒 تم إنشاء الطلب: ${formatCreatedAt(booking.created_at)}",
                            13f,
                            GRAY
                        )
                    )

                    if (
                        booking.latitude != null &&
                        booking.longitude != null
                    ) {

                        val mapButton =
                            Button(
                                this@MainActivity
                            ).apply {

                                text =
                                    "🗺️ فتح موقع الطلب"

                                isAllCaps =
                                    false

                                setTextColor(
                                    Color.WHITE
                                )

                                background =
                                    roundedBackground(
                                        BLUE,
                                        dp(14)
                                    )

                                setOnClickListener {

                                    openLocationInMaps(
                                        booking.latitude,
                                        booking.longitude
                                    )
                                }
                            }

                        card.addView(
                            mapButton,
                            LinearLayout.LayoutParams(
                                -1,
                                dp(52)
                            ).apply {
                                setMargins(
                                    0,
                                    dp(8),
                                    0,
                                    0
                                )
                            }
                        )
                    }

                    root.addView(
                        card,
                        LinearLayout.LayoutParams(
                            -1,
                            -2
                        ).apply {
                            setMargins(
                                0,
                                dp(5),
                                0,
                                dp(9)
                            )
                        }
                    )
                }

            } catch (e: Exception) {

                loadingText.text =
                    "تعذر تحميل الطلبات.\n\n" +
                    (
                        e.message
                            ?: "خطأ غير معروف"
                    )

            }
        }
    }

    // =====================================================
    // اسم الخدمة
    // =====================================================

    private fun getServiceName(
        serviceId: String
    ): String {

        return APP_SERVICES
            .firstOrNull {
                it.id == serviceId
            }
            ?.name
            ?: "خدمة تمريضية"
    }

    // =====================================================
    // حالة الطلب
    // =====================================================

    private fun getStatusInfo(
        status: String
    ): Pair<String, Int> {

        return when (
            status.uppercase()
        ) {

            "PENDING" ->
                Pair(
                    "بانتظار قبول الممرض",
                    Color.rgb(220, 140, 20)
                )

            "ACCEPTED" ->
                Pair(
                    "تم قبول الطلب",
                    BLUE
                )

            "ON_THE_WAY" ->
                Pair(
                    "الممرض في الطريق 🚑",
                    BLUE
                )

            "IN_PROGRESS" ->
                Pair(
                    "الزيارة قيد التنفيذ",
                    Color.rgb(150, 80, 190)
                )

            "COMPLETED" ->
                Pair(
                    "اكتملت الزيارة ✅",
                    GREEN
                )

            "CANCELLED" ->
                Pair(
                    "تم إلغاء الطلب",
                    RED
                )

            else ->
                Pair(
                    status,
                    GRAY
                )
        }
    }

    // =====================================================
    // استخراج اسم المريض من العنوان
    // =====================================================

    private fun extractPatientName(
        value: String
    ): String {

        val separator =
            " - "

        return if (
            value.contains(separator)
        ) {
            value.substringBefore(
                separator
            )
        } else {
            "المريض"
        }
    }

    private fun extractAddress(
        value: String
    ): String {

        val separator =
            " - "

        return if (
            value.contains(separator)
        ) {
            value.substringAfter(
                separator
            )
        } else {
            value
        }
    }

    // =====================================================
    // تنسيق تاريخ إنشاء الطلب فقط
    // =====================================================
    // ملاحظة:
    // هذا ليس موعد زيارة.
    // هو فقط وقت إنشاء الطلب الموجود في created_at.
    // =====================================================

    private fun formatCreatedAt(
        value: String
    ): String {

        return try {

            val input =
                java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                    java.util.Locale.US
                )

            val date =
                input.parse(value)

            if (date != null) {

                java.text.SimpleDateFormat(
                    "yyyy/MM/dd HH:mm",
                    java.util.Locale.getDefault()
                ).format(date)

            } else {

                value
            }

        } catch (_: Exception) {

            value
                .replace("T", " ")
                .take(16)
        }
    }

    // =====================================================
    // فتح موقع الطلب في Google Maps
    // =====================================================

    private fun openLocationInMaps(
        latitude: Double?,
        longitude: Double?
    ) {

        if (
            latitude == null ||
            longitude == null
        ) {

            Toast.makeText(
                this,
                "لا يوجد موقع لهذا الطلب",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val uri =
            Uri.parse(
                "geo:$latitude,$longitude?q=$latitude,$longitude"
            )

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                )
            )

        } catch (_: Exception) {

            val webUri =
                Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                )

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    webUri
                )
            )
        }
    }     // =====================================================
    // صلاحيات الموقع
    // =====================================================

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

        if (
            requestCode != LOCATION_REQUEST_CODE
        ) {
            return
        }

        val granted =
            grantResults.any {
                it ==
                    PackageManager.PERMISSION_GRANTED
            }

        if (granted) {

            Toast.makeText(
                this,
                "تم السماح بالموقع. اضغط تحديد الموقع مرة أخرى.",
                Toast.LENGTH_LONG
            ).show()

        } else {

            AlertDialog.Builder(this)
                .setTitle(
                    "صلاحية الموقع مطلوبة"
                )
                .setMessage(
                    "يحتاج التطبيق إلى صلاحية الموقع حتى يستطيع الممرض الوصول إلى المريض."
                )
                .setPositiveButton(
                    "فتح الإعدادات"
                ) { _, _ ->

                    try {

                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse(
                                    "package:$packageName"
                                )
                            )
                        )

                    } catch (_: Exception) {
                    }
                }
                .setNegativeButton(
                    "إلغاء",
                    null
                )
                .show()
        }
    }

    // =====================================================
    // شاشة الموقع
    // =====================================================

    private fun showLocationScreen() {

        val root =
            baseLayout()

        root.addView(
            text(
                "📍 موقع المريض",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "حدد موقعك الحالي ليسهل على الممرض الوصول إليك.",
                17f,
                GRAY
            )
        )

        val status =
            text(
                currentLocationText,
                16f,
                DARK_BLUE
            )

        root.addView(
            status,
            LinearLayout.LayoutParams(
                -1,
                dp(130)
            ).apply {

                setMargins(
                    0,
                    dp(18),
                    0,
                    dp(10)
                )
            }
        )

        addButton(
            root,
            "📍 تحديد موقعي الحالي",
            65
        ) {

            status.text =
                "جاري تحديد الموقع..."

            requestLocation {

                status.text =
                    currentLocationText
            }
        }

        addButton(
            root,
            "🗺️ فتح الموقع في الخرائط",
            60
        ) {

            if (
                currentLatitude == null ||
                currentLongitude == null
            ) {

                Toast.makeText(
                    this,
                    "حدد موقعك أولاً",
                    Toast.LENGTH_SHORT
                ).show()

                return@addButton
            }

            openLocationInMaps(
                currentLatitude,
                currentLongitude
            )
        }

        addButton(
            root,
            "رجوع إلى الرئيسية ↩️",
            58
        ) {
            showPatientHome()
        }

        setContentView(
            scroll(root)
        )
    }

    // =====================================================
    // اختصار لعرض الموقع
    // =====================================================

    private fun showLocation() {
        showLocationScreen()
    }

    // =====================================================
    // الاتصال بالإدارة
    // =====================================================

    private fun contactUs() {

        AlertDialog.Builder(this)
            .setTitle(
                "☎️ تواصل معنا"
            )
            .setMessage(
                "التمريض المنزلي\n\n" +
                "محافظة الأنبار - العراق\n\n" +
                "يمكنك التواصل مع إدارة الخدمة للاستفسارات والمساعدة."
            )
            .setPositiveButton(
                "حسناً",
                null
            )
            .show()
    }

    // =====================================================
    // تسجيل الخروج
    // =====================================================

    private fun logout() {

        AlertDialog.Builder(this)
            .setTitle(
                "تسجيل الخروج"
            )
            .setMessage(
                "هل تريد تسجيل الخروج من الحساب؟"
            )
            .setNegativeButton(
                "إلغاء",
                null
            )
            .setPositiveButton(
                "تسجيل الخروج"
            ) { _, _ ->

                scope.launch {

                    try {

                        withContext(
                            Dispatchers.IO
                        ) {

                            SupabaseManager
                                .client
                                .auth
                                .signOut()
                        }

                    } catch (_: Exception) {
                    }

                    phoneNumber = ""

                    currentLatitude = null
                    currentLongitude = null

                    currentLocationText =
                        "لم يتم تحديد الموقع"

                    showWelcome()
                }
            }
            .show()
    }

    // =====================================================
    // إعادة تحميل الصفحة الرئيسية
    // =====================================================

    private fun refreshHome() {

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()

        if (user == null) {

            phoneNumber = ""

            showWelcome()

            return
        }

        showPatientHome()
    }

    // =====================================================
    // التحقق من وجود جلسة
    // =====================================================

    private fun hasActiveSession(): Boolean {

        return try {

            SupabaseManager
                .client
                .auth
                .currentUserOrNull() != null

        } catch (_: Exception) {

            false
        }
    }

    // =====================================================
    // عند العودة للتطبيق
    // =====================================================

    override fun onResume() {

        super.onResume()

        if (
            phoneNumber.isBlank() &&
            hasActiveSession()
        ) {

            val user =
                SupabaseManager
                    .client
                    .auth
                    .currentUserOrNull()

            if (user != null) {

                phoneNumber =
                    user.phone
                        ?: ""

                if (
                    phoneNumber.isBlank()
                ) {

                    phoneNumber =
                        "مستخدم مسجل"
                }
            }
        }
    }

    // =====================================================
    // أخطاء التطبيق
    // =====================================================

    private fun showError(
        title: String,
        message: String
    ) {

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                "حسناً",
                null
            )
            .show()
    }

    // =====================================================
    // فحص الإنترنت
    // =====================================================

    private fun isInternetAvailable(): Boolean {

        return try {

            val manager =
                getSystemService(
                    CONNECTIVITY_SERVICE
                ) as android.net.ConnectivityManager

            val network =
                manager.activeNetwork
                    ?: return false

            val capabilities =
                manager.getNetworkCapabilities(
                    network
                )
                    ?: return false

            capabilities.hasCapability(
                android.net.NetworkCapabilities
                    .NET_CAPABILITY_INTERNET
            )

        } catch (_: Exception) {

            false
        }
    }

    // =====================================================
    // مساعد رسالة الاتصال
    // =====================================================

    private fun checkInternetBefore(
        action: () -> Unit
    ) {

        if (!isInternetAvailable()) {

            Toast.makeText(
                this,
                "لا يوجد اتصال بالإنترنت",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        action()
    }

    // =====================================================
    // تنظيف رقم الهاتف
    // =====================================================

    private fun cleanPhone(
        value: String
    ): String {

        return value
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .trim()
    }

    // =====================================================
    // تحويل الأرقام العربية إلى إنجليزية
    // =====================================================

    private fun normalizeArabicDigits(
        value: String
    ): String {

        return value.map {

            when (it) {

                '٠' -> '0'
                '١' -> '1'
                '٢' -> '2'
                '٣' -> '3'
                '٤' -> '4'
                '٥' -> '5'
                '٦' -> '6'
                '٧' -> '7'
                '٨' -> '8'
                '٩' -> '9'

                else -> it
            }

        }.joinToString("")
    }

    // =====================================================
    // تجهيز رقم الهاتف
    // =====================================================

    private fun normalizePhoneInput(
        value: String
    ): String? {

        var phone =
            normalizeArabicDigits(
                cleanPhone(value)
            )

        if (
            phone.startsWith("+964")
        ) {

            phone =
                "0" +
                    phone.substring(4)
        }

        if (
            phone.startsWith("00964")
        ) {

            phone =
                "0" +
                    phone.substring(5)
        }

        if (
            !phone.startsWith("07")
        ) {
            return null
        }

        if (
            phone.length != 11
        ) {
            return null
        }

        return phone
    }

    // =====================================================
    // تحويل الرقم العراقي إلى صيغة Supabase
    // =====================================================

    private fun toSupabasePhone(
        value: String
    ): String? {

        val normalized =
            normalizePhoneInput(value)
                ?: return null

        return "+964" +
            normalized.substring(1)
    }

    // =====================================================
    // معلومات المستخدم
    // =====================================================

    private fun currentUserId(): String? {

        return try {

            SupabaseManager
                .client
                .auth
                .currentUserOrNull()
                ?.id
                ?.toString()

        } catch (_: Exception) {

            null
        }
    }

    // =====================================================
    // التحقق قبل إنشاء الطلب
    // =====================================================

    private fun validateBookingData(
        service: ServiceItem?,
        address: String
    ): Boolean {

        if (service == null) {

            Toast.makeText(
                this,
                "اختر الخدمة أولاً",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        if (address.isBlank()) {

            Toast.makeText(
                this,
                "أدخل عنوان المنزل",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        return true
    }    // =====================================================
    // التحقق من حالة تسجيل الدخول
    // =====================================================

    private fun requireLogin(
        action: () -> Unit
    ) {

        if (!hasActiveSession()) {

            AlertDialog.Builder(this)
                .setTitle(
                    "تسجيل الدخول مطلوب"
                )
                .setMessage(
                    "يرجى تسجيل الدخول برقم الهاتف أولاً."
                )
                .setPositiveButton(
                    "تسجيل الدخول"
                ) { _, _ ->
                    showPhoneLogin()
                }
                .setNegativeButton(
                    "إلغاء",
                    null
                )
                .show()

            return
        }

        action()
    }

    // =====================================================
    // عرض تفاصيل الخدمة
    // =====================================================

    private fun showServiceDetails(
        service: ServiceItem
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                service.name
            )
            .setMessage(
                service.description
            )
            .setPositiveButton(
                "طلب الخدمة"
            ) { _, _ ->
                showCreateBooking()
            }
            .setNegativeButton(
                "إغلاق",
                null
            )
            .show()
    }

    // =====================================================
    // شاشة تفاصيل الطلب
    // =====================================================

    private fun showBookingDetails(
        booking: BookingRow
    ) {

        val serviceName =
            getServiceName(
                booking.service_id
            )

        val status =
            getStatusInfo(
                booking.status
            )

        val message =
            "الخدمة:\n$serviceName\n\n" +
            "العنوان:\n${booking.address}\n\n" +
            "الحالة:\n${status.first}\n\n" +
            "ملاحظات:\n${
                booking.notes
                    ?: "لا توجد ملاحظات"
            }\n\n" +
            "وقت إنشاء الطلب:\n" +
            formatCreatedAt(
                booking.created_at
            )

        AlertDialog.Builder(this)
            .setTitle(
                "تفاصيل الطلب"
            )
            .setMessage(
                message
            )
            .setPositiveButton(
                "إغلاق",
                null
            )
            .show()
    }

    // =====================================================
    // إلغاء طلب
    // =====================================================

    private fun cancelBooking(
        booking: BookingRow
    ) {

        if (
            booking.status.uppercase() !=
            "PENDING"
        ) {

            Toast.makeText(
                this,
                "لا يمكن إلغاء هذا الطلب حالياً",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "إلغاء الطلب"
            )
            .setMessage(
                "هل أنت متأكد من إلغاء طلب التمريض؟"
            )
            .setNegativeButton(
                "لا",
                null
            )
            .setPositiveButton(
                "نعم، إلغاء"
            ) { _, _ ->

                updateBookingStatus(
                    booking.id,
                    "CANCELLED"
                )
            }
            .show()
    }

    // =====================================================
    // تحديث حالة الطلب
    // =====================================================

    private fun updateBookingStatus(
        bookingId: String,
        status: String
    ) {

        val progress =
            ProgressDialog(this).apply {
                setMessage(
                    "جاري تحديث الطلب..."
                )
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                @Serializable
                data class StatusUpdate(
                    val status: String
                )

                withContext(
                    Dispatchers.IO
                ) {

                    SupabaseManager
                        .client
                        .from("bookings")
                        .update(
                            StatusUpdate(status)
                        ) {
                            filter {
                                eq(
                                    "id",
                                    bookingId
                                )
                            }
                        }
                }

                progress.dismiss()

                Toast.makeText(
                    this@MainActivity,
                    "تم تحديث حالة الطلب",
                    Toast.LENGTH_SHORT
                ).show()

                showMyBookings()

            } catch (e: Exception) {

                progress.dismiss()

                showError(
                    "تعذر تحديث الطلب",
                    e.message
                        ?: "حدث خطأ أثناء تحديث الحالة."
                )
            }
        }
    }

    // =====================================================
    // التأكد من وجود الموقع
    // =====================================================

    private fun ensureLocation(
        onReady: () -> Unit
    ) {

        if (
            currentLatitude != null &&
            currentLongitude != null
        ) {

            onReady()

            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "الموقع غير محدد"
            )
            .setMessage(
                "يفضل تحديد موقع المنزل حتى يستطيع الممرض الوصول إلى المريض بسهولة."
            )
            .setNegativeButton(
                "متابعة بدون موقع"
            ) { _, _ ->
                onReady()
            }
            .setPositiveButton(
                "تحديد الموقع"
            ) { _, _ ->

                requestLocation {
                    onReady()
                }
            }
            .show()
    }

    // =====================================================
    // معلومات التطبيق
    // =====================================================

    private fun showAbout() {

        AlertDialog.Builder(this)
            .setTitle(
                "عن التطبيق"
            )
            .setMessage(
                "التمريض المنزلي\n\n" +
                "خدمة لطلب الرعاية والتمريض المنزلي " +
                "في محافظة الأنبار - العراق.\n\n" +
                "الإصدار 1.0"
            )
            .setPositiveButton(
                "حسناً",
                null
            )
            .show()
    }

    // =====================================================
    // سياسة الخصوصية
    // =====================================================

    private fun showPrivacy() {

        AlertDialog.Builder(this)
            .setTitle(
                "الخصوصية"
            )
            .setMessage(
                "يستخدم التطبيق رقم الهاتف لتسجيل الدخول، " +
                "ويستخدم موقع الجهاز عند موافقة المستخدم " +
                "لتحديد موقع تقديم خدمة التمريض."
            )
            .setPositiveButton(
                "حسناً",
                null
            )
            .show()
    }

    // =====================================================
    // التأكد من تشغيل الموقع
    // =====================================================

    private fun isLocationEnabled(): Boolean {

        return try {

            val manager =
                getSystemService(
                    LOCATION_SERVICE
                ) as android.location.LocationManager

            manager.isProviderEnabled(
                android.location.LocationManager.GPS_PROVIDER
            ) ||
            manager.isProviderEnabled(
                android.location.LocationManager.NETWORK_PROVIDER
            )

        } catch (_: Exception) {

            false
        }
    }

    // =====================================================
    // فتح إعدادات الموقع
    // =====================================================

    private fun openLocationSettings() {

        try {

            startActivity(
                Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "تعذر فتح إعدادات الموقع",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =====================================================
    // التأكد من GPS قبل تحديد الموقع
    // =====================================================

    private fun checkLocationAndRequest(
        onSuccess: (() -> Unit)? = null
    ) {

        if (!isLocationEnabled()) {

            AlertDialog.Builder(this)
                .setTitle(
                    "الموقع غير مفعّل"
                )
                .setMessage(
                    "قم بتشغيل GPS أو خدمة الموقع ثم حاول مرة أخرى."
                )
                .setPositiveButton(
                    "فتح الإعدادات"
                ) { _, _ ->
                    openLocationSettings()
                }
                .setNegativeButton(
                    "إلغاء",
                    null
                )
                .show()

            return
        }

        requestLocation(
            onSuccess
        )
    }

    // =====================================================
    // صفحة الخدمات المختصرة
    // =====================================================

    private fun showAllServices() {

        val root =
            baseLayout()

        root.addView(
            text(
                "🏥 الخدمات",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "اختر الخدمة المناسبة لك",
                17f,
                GRAY
            )
        )

        for (
            service in APP_SERVICES
        ) {

            val card =
                LinearLayout(
                    this
                ).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    layoutDirection =
                        View.LAYOUT_DIRECTION_RTL

                    setPadding(
                        dp(15),
                        dp(10),
                        dp(15),
                        dp(10)
                    )

                    background =
                        roundedBackground(
                            LIGHT_BLUE,
                            dp(18)
                        )

                    setOnClickListener {

                        showServiceDetails(
                            service
                        )
                    }
                }

            card.addView(
                text(
                    "🩺 ${service.name}",
                    19f,
                    DARK_BLUE
                )
            )

            card.addView(
                text(
                    service.description,
                    14f,
                    GRAY
                )
            )

            root.addView(
                card,
                LinearLayout.LayoutParams(
                    -1,
                    dp(92)
                ).apply {

                    setMargins(
                        0,
                        dp(5),
                        0,
                        dp(5)
                    )
                }
            )
        }

        addButton(
            root,
            "📝 إنشاء طلب",
            65
        ) {

            showCreateBooking()
        }

        addButton(
            root,
            "رجوع ↩️",
            58
        ) {

            showPatientHome()
        }

        setContentView(
            scroll(root)
        )
    }

    // =====================================================
    // فتح جميع الخدمات
    // =====================================================

    private fun openServices() {

        requireLogin {

            showAllServices()
        }
    }

    // =====================================================
    // التحقق من صحة بيانات الطلب
    // =====================================================

    private fun validateRequest(
        service: ServiceItem?,
        city: String,
        patient: String,
        address: String
    ): String? {

        if (service == null) {

            return "اختر الخدمة"
        }

        if (
            city.isBlank() ||
            city == "اختر المدينة / المنطقة"
        ) {

            return "اختر المدينة أو المنطقة"
        }

        if (
            patient.trim().length < 2
        ) {

            return "أدخل اسم المريض"
        }

        if (
            address.trim().length < 3
        ) {

            return "أدخل عنوان المنزل"
        }

        return null
    }    // =====================================================
    // إنهاء الطلب
    // =====================================================

    private fun finishBooking(
        service: ServiceItem,
        patientName: String,
        city: String,
        address: String,
        notes: String
    ) {

        val fullAddress =
            "$city - $address"

        if (
            !validateRequest(
                service,
                city,
                patientName,
                address
            ).isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                validateRequest(
                    service,
                    city,
                    patientName,
                    address
                ),
                Toast.LENGTH_LONG
            ).show()

            return
        }

        ensureLocation {

            confirmBooking(
                service,
                patientName,
                fullAddress,
                notes
            )
        }
    }

    // =====================================================
    // تأكيد الطلب قبل الإرسال
    // =====================================================

    private fun confirmBooking(
        service: ServiceItem,
        patientName: String,
        address: String,
        notes: String
    ) {

        val locationInfo =
            if (
                currentLatitude != null &&
                currentLongitude != null
            ) {

                "تم تحديد الموقع 📍\n" +
                "خط العرض: %.6f\n".format(
                    currentLatitude
                ) +
                "خط الطول: %.6f".format(
                    currentLongitude
                )

            } else {

                "لم يتم تحديد الموقع"
            }

        AlertDialog.Builder(this)
            .setTitle(
                "تأكيد طلب التمريض"
            )
            .setMessage(
                "الخدمة:\n" +
                "${service.name}\n\n" +

                "المريض:\n" +
                "$patientName\n\n" +

                "العنوان:\n" +
                "$address\n\n" +

                "الموقع:\n" +
                "$locationInfo\n\n" +

                "ملاحظات:\n" +
                if (notes.isBlank()) {
                    "لا توجد"
                } else {
                    notes
                } +

                "\n\n" +
                "سيتم إرسال الطلب الآن بدون تحديد موعد."
            )
            .setNegativeButton(
                "تعديل",
                null
            )
            .setPositiveButton(
                "إرسال الطلب"
            ) { _, _ ->

                sendBookingToSupabase(
                    service,
                    patientName,
                    address,
                    notes
                )
            }
            .show()
    }

    // =====================================================
    // إرسال الطلب إلى Supabase
    // =====================================================

    private fun sendBookingToSupabase(
        service: ServiceItem,
        patientName: String,
        address: String,
        notes: String
    ) {

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()

        if (user == null) {

            showError(
                "تسجيل الدخول مطلوب",
                "انتهت جلسة تسجيل الدخول. يرجى تسجيل الدخول مرة أخرى."
            )

            showPhoneLogin()

            return
        }

        val progress =
            ProgressDialog(this).apply {

                setMessage(
                    "جاري إرسال الطلب..."
                )

                setCancelable(false)

                show()
            }

        scope.launch {

            try {

                val booking =
                    BookingInsert(

                        patient_id =
                            user.id.toString(),

                        service_id =
                            service.id,

                        address =
                            "$patientName - $address",

                        latitude =
                            currentLatitude,

                        longitude =
                            currentLongitude,

                        status =
                            "PENDING",

                        notes =
                            notes.ifBlank {
                                null
                            }
                    )

                withContext(
                    Dispatchers.IO
                ) {

                    SupabaseManager
                        .client
                        .from("bookings")
                        .insert(
                            booking
                        )
                }

                progress.dismiss()

                showBookingSuccess()

            } catch (e: Exception) {

                progress.dismiss()

                showError(
                    "تعذر إرسال الطلب",
                    e.message
                        ?: "حدث خطأ أثناء حفظ الطلب في قاعدة البيانات."
                )
            }
        }
    }

    // =====================================================
    // نجاح إرسال الطلب
    // =====================================================

    private fun showBookingSuccess() {

        AlertDialog.Builder(this)
            .setTitle(
                "تم إرسال الطلب بنجاح ✅"
            )
            .setMessage(
                "تم استلام طلب التمريض الخاص بك.\n\n" +
                "الطلب الآن بانتظار قبول الممرض."
            )
            .setPositiveButton(
                "عرض طلباتي"
            ) { _, _ ->

                showMyBookings()
            }
            .setNegativeButton(
                "الرئيسية"
            ) { _, _ ->

                showPatientHome()
            }
            .setCancelable(false)
            .show()
    }

    // =====================================================
    // التأكد من بيانات التطبيق عند البداية
    // =====================================================

    private fun checkExistingSession() {

        try {

            val user =
                SupabaseManager
                    .client
                    .auth
                    .currentUserOrNull()

            if (user != null) {

                phoneNumber =
                    user.phone
                        ?: ""

                if (
                    phoneNumber.isBlank()
                ) {
                    phoneNumber =
                        "مستخدم مسجل"
                }

                showPatientHome()

            } else {

                showWelcome()
            }

        } catch (_: Exception) {

            showWelcome()
        }
    }

    // =====================================================
    // فتح خرائط Google
    // =====================================================

    private fun openGoogleMaps() {

        if (
            currentLatitude == null ||
            currentLongitude == null
        ) {

            Toast.makeText(
                this,
                "حدد موقعك أولاً",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        openLocationInMaps(
            currentLatitude,
            currentLongitude
        )
    }

    // =====================================================
    // تنظيف الذاكرة عند إغلاق الشاشة
    // =====================================================

    private fun clearTemporaryData() {

        currentLatitude = null
        currentLongitude = null

        currentLocationText =
            "لم يتم تحديد الموقع"
    }

    // =====================================================
    // معالجة زر الرجوع
    // =====================================================

    @Deprecated(
        "Deprecated in Java"
    )
    override fun onBackPressed() {

        if (
            hasActiveSession()
        ) {

            AlertDialog.Builder(this)
                .setTitle(
                    "الخروج"
                )
                .setMessage(
                    "هل تريد العودة إلى شاشة الترحيب؟"
                )
                .setNegativeButton(
                    "إلغاء",
                    null
                )
                .setPositiveButton(
                    "نعم"
                ) { _, _ ->

                    showPatientHome()
                }
                .show()

        } else {

            showWelcome()
        }
    }
    }
    } 
