package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
data class PatientBooking(
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


class MainActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val DARK_NAVY = Color.rgb(3, 45, 78)
    private val BLUE = Color.rgb(31, 115, 176)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val GREEN = Color.rgb(50, 150, 85)
    private val ORANGE = Color.rgb(230, 145, 45)
    private val RED = Color.rgb(205, 65, 65)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(125, 125, 125)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val BORDER = Color.rgb(220, 225, 230)
    private val WHITE = Color.WHITE

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var phoneNumber = ""
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * أول شاشة للمستخدم هي تسجيل الدخول.
         * إذا كان المستخدم مسجلاً مسبقاً في Supabase
         * ننتقل مباشرة إلى الرئيسية.
         */
        val user = SupabaseManager.client.auth.currentUserOrNull()

        if (user == null) {
            showPhoneLogin()
        } else {
            showHome()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun rounded(
        color: Int,
        radius: Int = 18
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun bordered(
        color: Int,
        strokeColor: Int = BORDER,
        radius: Int = 18
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun baseLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)
            setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(80)
            )
        }
    }

    private fun scroll(view: View): ScrollView {
        return ScrollView(this).apply {
            setBackgroundColor(LIGHT_GRAY)
            isFillViewport = true
            addView(view)
        }
    }

    private fun text(
        value: String,
        size: Float = 16f,
        color: Int = TEXT,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
    }

    private fun button(
        value: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 17f
            isAllCaps = false
            setTextColor(WHITE)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(NAVY, 15)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setOnClickListener { action() }
        }
    }

    private fun outlineButton(
        value: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 16f
            isAllCaps = false
            setTextColor(NAVY)
            gravity = Gravity.CENTER
            background = bordered(WHITE, NAVY, 14)
            setOnClickListener { action() }
        }
    }

    private fun addSpace(
        root: LinearLayout,
        height: Int
    ) {
        root.addView(
            Space(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun topBar(
        title: String,
        backAction: (() -> Unit)? = null
    ): LinearLayout {

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(WHITE)
        }

        if (backAction != null) {
            val back = TextView(this).apply {
                text = "‹"
                textSize = 40f
                setTextColor(NAVY)
                gravity = Gravity.CENTER
                setOnClickListener { backAction() }
            }

            bar.addView(
                back,
                LinearLayout.LayoutParams(dp(55), dp(55))
            )
        }

        bar.addView(
            text(title, 22f, NAVY, true),
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        bar.addView(
            text("🔔", 23f, NAVY),
            LinearLayout.LayoutParams(dp(55), dp(55))
        )

        bar.addView(
            text("🛒", 22f, NAVY),
            LinearLayout.LayoutParams(dp(55), dp(55))
        )

        return bar
    }

    private fun bottomNavigation(
        selected: String
    ): LinearLayout {

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(248, 251, 253))
            elevation = dp(8).toFloat()
        }

        fun item(
            icon: String,
            title: String,
            key: String,
            action: () -> Unit
        ) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { action() }
            }

            box.addView(
                text(
                    icon,
                    24f,
                    if (selected == key) NAVY else GRAY
                )
            )

            box.addView(
                text(
                    title,
                    13f,
                    if (selected == key) NAVY else GRAY
                )
            )

            nav.addView(
                box,
                LinearLayout.LayoutParams(
                    0,
                    dp(70),
                    1f
                )
            )
        }

        item("⋯", "المزيد", "more") { showMore() }
        item("💬", "المحادثات", "chat") { showChats() }
        item("☷", "الطلبات", "orders") { showBookings() }
        item("⌂", "الرئيسية", "home") { showHome() }

        return nav
    }

    /*
     * =========================================================
     * الشاشة الأولى: تسجيل الدخول برقم الهاتف
     * =========================================================
     */
    private fun showPhoneLogin() {

        val root = baseLayout()

        /*
         * هيدر بسيط يشبه تصميم التطبيق:
         * شعار + اسم التطبيق
         */
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(WHITE, 22)
            setPadding(dp(15), dp(18), dp(15), dp(18))
        }

        header.addView(
            text("🩺", 52f, NAVY)
        )

        header.addView(
            text(
                "التمريض المنزلي",
                27f,
                NAVY,
                true
            )
        )

        header.addView(
            text(
                "محافظة الأنبار - العراق",
                14f,
                GRAY
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(170)
            )
        )

        addSpace(root, 22)

        root.addView(
            text(
                "تسجيل الدخول",
                29f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "أدخل رقم هاتفك للمتابعة",
                16f,
                GRAY
            )
        )

        addSpace(root, 20)

        /*
         * بطاقة تسجيل الدخول
         */
        val loginCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(WHITE, 20)
            setPadding(
                dp(18),
                dp(20),
                dp(18),
                dp(20)
            )
        }

        loginCard.addView(
            text(
                "📱",
                42f,
                NAVY
            )
        )

        loginCard.addView(
            text(
                "رقم الهاتف",
                18f,
                NAVY,
                true
            )
        )

        addSpace(loginCard, 8)

        val phone = EditText(this).apply {

            hint = "07701234567"

            textSize = 19f

            gravity = Gravity.CENTER

            inputType =
                InputType.TYPE_CLASS_PHONE

            layoutDirection =
                View.LAYOUT_DIRECTION_LTR

            background =
                bordered(
                    WHITE,
                    BORDER,
                    15
                )

            setPadding(
                dp(15),
                dp(5),
                dp(15),
                dp(5)
            )
        }

        loginCard.addView(
            phone,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        loginCard.addView(
            text(
                "مثال: 07701234567",
                13f,
                GRAY
            )
        )

        addSpace(loginCard, 15)

        loginCard.addView(
            button(
                "إرسال رمز التحقق"
            ) {

                val input =
                    phone.text
                        .toString()
                        .trim()

                val normalized =
                    normalizeIraqPhone(input)

                if (normalized == null) {

                    phone.error =
                        "رقم الهاتف العراقي غير صحيح"

                    return@button
                }

                phoneNumber =
                    normalized

                sendOtp()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        root.addView(
            loginCard,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        addSpace(root, 18)

        root.addView(
            text(
                "بتسجيل الدخول أنت توافق على شروط استخدام الخدمة وسياسة الخصوصية.",
                12f,
                GRAY
            )
        )

        addSpace(root, 15)

        val emergency = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(LIGHT_BLUE, 18)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        emergency.addView(
            text("🛡️", 30f, NAVY),
            LinearLayout.LayoutParams(
                dp(55),
                dp(55)
            )
        )

        emergency.addView(
            text(
                "خدمة تمريض منزلية في محافظة الأنبار",
                14f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        root.addView(emergency)

        setContentView(scroll(root))
    }

    private fun normalizeIraqPhone(
        value: String
    ): String? {

        var phone =
            value
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")

        if (phone.startsWith("+964")) {
            return if (
                phone.length == 14 &&
                phone.getOrNull(4) == '7'
            ) {
                phone
            } else {
                null
            }
        }

        if (phone.startsWith("00964")) {
            phone = "+" + phone.substring(2)

            return if (
                phone.length == 14 &&
                phone.getOrNull(4) == '7'
            ) {
                phone
            } else {
                null
            }
        }

        if (phone.startsWith("07")) {
            phone = "+964" + phone.substring(1)

            return if (phone.length == 14) {
                phone
            } else {
                null
            }
        }

        return null
    }

    /*
     * =========================================================
     * إرسال OTP
     * =========================================================
     */
    private fun sendOtp() {

        val loading =
            ProgressDialog(this).apply {
                setMessage("جاري إرسال رمز التحقق...")
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                SupabaseManager
                    .client
                    .auth
                    .signInWith(OTP) {
                        phone = phoneNumber
                    }

                loading.dismiss()

                showOtpScreen()

            } catch (e: Exception) {

                loading.dismiss()

                showError(
                    "تعذر إرسال الرمز",
                    e.message
                        ?: "تأكد من إعداد Phone Auth في Supabase."
                )
            }
        }
    }

    /*
     * =========================================================
     * شاشة OTP
     * =========================================================
     */
    private fun showOtpScreen() {

        val root = baseLayout()

        root.addView(
            topBar(
                "تأكيد رقم الهاتف",
                ::showPhoneLogin
            )
        )

        addSpace(root, 25)

        root.addView(
            text(
                "🔐",
                58f,
                NAVY
            )
        )

        root.addView(
            text(
                "أدخل رمز التحقق",
                28f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "تم إرسال الرمز إلى",
                15f,
                GRAY
            )
        )

        root.addView(
            text(
                phoneNumber,
                18f,
                NAVY,
                true
            )
        )

        addSpace(root, 20)

        val otp = EditText(this).apply {

            hint = "000000"

            textSize = 28f

            gravity = Gravity.CENTER

            inputType =
                InputType.TYPE_CLASS_NUMBER

            layoutDirection =
                View.LAYOUT_DIRECTION_LTR

            maxLines = 1

            filters =
                arrayOf(
                    InputFilter.LengthFilter(6)
                )

            background =
                bordered(
                    WHITE,
                    BORDER,
                    15
                )

            setPadding(
                dp(15),
                dp(5),
                dp(15),
                dp(5)
            )
        }

        root.addView(
            otp,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        addSpace(root, 18)

        root.addView(
            button(
                "تأكيد الرمز"
            ) {

                val code =
                    otp.text
                        .toString()
                        .trim()

                if (code.length != 6) {
                    otp.error = "أدخل 6 أرقام"
                    return@button
                }

                verifyOtp(code)
            },
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        addSpace(root, 10)

        root.addView(
            outlineButton(
                "إرسال الرمز مرة أخرى"
            ) {
                sendOtp()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            )
        )

        setContentView(scroll(root))
    }

    private fun verifyOtp(code: String) {

        val loading =
            ProgressDialog(this).apply {
                setMessage("جاري التحقق...")
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                SupabaseManager
                    .client
                    .auth
                    .verifyPhoneOtp(
                        type = OtpType.Phone.SMS,
                        phone = phoneNumber,
                        token = code
                    )

                loading.dismiss()

                Toast.makeText(
                    this@MainActivity,
                    "تم تسجيل الدخول بنجاح",
                    Toast.LENGTH_SHORT
                ).show()

                showHome()

            } catch (e: Exception) {

                loading.dismiss()

                showError(
                    "فشل التحقق",
                    e.message
                        ?: "رمز التحقق غير صحيح."
                )
            }
        }
    }

    /*
     * =========================================================
     * الرئيسية
     * =========================================================
     */
    private fun showHome() {

        val root = baseLayout()

        root.addView(topBar("الرئيسية"))

        addSpace(root, 12)

        val welcome =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    rounded(NAVY, 22)

                setPadding(
                    dp(15),
                    dp(20),
                    dp(15),
                    dp(20)
                )
            }

        welcome.addView(
            text(
                "التمريض المنزلي",
                29f,
                WHITE,
                true
            )
        )

        welcome.addView(
            text(
                "خدمة تمريض تصل إليك أينما كنت",
                16f,
                WHITE
            )
        )

        welcome.addView(
            text(
                "محافظة الأنبار - العراق",
                14f,
                Color.rgb(220, 235, 245)
            )
        )

        root.addView(
            welcome,
            LinearLayout.LayoutParams(
                -1,
                dp(150)
            )
        )

        addSpace(root, 15)

        root.addView(
            button(
                "🩺   إنشاء طلب تمريض الآن"
            ) {
                checkLoginBeforeRequest()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        addSpace(root, 17)

        root.addView(
            text(
                "الخدمات الأكثر طلباً",
                23f,
                NAVY,
                true
            )
        )

        addSpace(root, 8)

        val row1 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        row1.addView(
            serviceCard(
                "💉",
                "إعطاء الحقن",
                "خدمة منزلية"
            ) {
                checkLoginBeforeRequest()
            },
            LinearLayout.LayoutParams(
                0,
                dp(145),
                1f
            ).apply {
                setMargins(0, 0, dp(5), 0)
            }
        )

        row1.addView(
            serviceCard(
                "🩹",
                "تغيير الضماد",
                "العناية بالجروح"
            ) {
                checkLoginBeforeRequest()
            },
            LinearLayout.LayoutParams(
                0,
                dp(145),
                1f
            ).apply {
                setMargins(dp(5), 0, 0, 0)
            }
        )

        root.addView(row1)

        addSpace(root, 10)

        val row2 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        row2.addView(
            serviceCard(
                "🩸",
                "قياس السكر",
                "فحص منزلي"
            ) {
                checkLoginBeforeRequest()
            },
            LinearLayout.LayoutParams(
                0,
                dp(145),
                1f
            ).apply {
                setMargins(0, 0, dp(5), 0)
            }
        )

        row2.addView(
            serviceCard(
                "🩺",
                "قياس الضغط",
                "متابعة الضغط"
            ) {
                checkLoginBeforeRequest()
            },
            LinearLayout.LayoutParams(
                0,
                dp(145),
                1f
            ).apply {
                setMargins(dp(5), 0, 0, 0)
            }
        )

        root.addView(row2)

        addSpace(root, 14)

        root.addView(
            outlineButton("عرض جميع الخدمات") {
                showServices()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            )
        )

        addSpace(root, 15)

        val safe =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    rounded(LIGHT_BLUE, 18)

                setPadding(
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
                )
            }

        safe.addView(
            text("🛡️", 32f, NAVY),
            LinearLayout.LayoutParams(
                dp(60),
                dp(65)
            )
        )

        safe.addView(
            text(
                "خدمة آمنة وموثوقة\nساعدنا في الحصول على الرعاية المنزلية بسهولة",
                15f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        root.addView(safe)

        addSpace(root, 14)

        root.addView(bottomNavigation("home"))

        setContentView(scroll(root))
    }

    private fun serviceCard(
        icon: String,
        title: String,
        description: String,
        action: () -> Unit
    ): LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            background =
                rounded(WHITE, 18)

            elevation =
                dp(2).toFloat()

            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )

            setOnClickListener {
                action()
            }

            addView(
                text(icon, 34f, NAVY)
            )

            addView(
                text(
                    title,
                    17f,
                    NAVY,
                    true
                )
            )

            addView(
                text(
                    description,
                    12f,
                    GRAY
                )
            )
        }
    }

    private fun checkLoginBeforeRequest() {

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()

        if (user == null) {

            showPhoneLogin()

            return
        }

        showRequestScreen()
    }

    /*
     * =========================================================
     * إنشاء الطلب
     * =========================================================
     */
    private fun showRequestScreen() {

        val root = baseLayout()

        root.addView(
            topBar(
                "إنشاء طلب",
                ::showHome
            )
        )

        addSpace(root, 12)

        root.addView(
            text(
                "خدمة التمريض",
                25f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "اختر الخدمة التي تحتاجها الآن",
                15f,
                GRAY
            )
        )

        addSpace(root, 15)

        val service = Spinner(this)

        val services =
            arrayOf(
                "اختر الخدمة",
                "إعطاء حقنة",
                "تغيير الضماد",
                "قياس السكر",
                "قياس ضغط الدم",
                "تركيب المحلول",
                "رعاية كبار السن",
                "رعاية المرضى في المنزل",
                "متابعة حالة صحية",
                "خدمة تمريض أخرى"
            )

        service.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                services
            )

        root.addView(
            service,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        addSpace(root, 12)

        val patient =
            EditText(this).apply {

                hint = "اسم المريض"

                textSize = 17f

                gravity = Gravity.RIGHT

                inputType =
                    InputType.TYPE_CLASS_TEXT

                background =
                    bordered(
                        WHITE,
                        BORDER,
                        14
                    )

                setPadding(
                    dp(15),
                    dp(5),
                    dp(15),
                    dp(5)
                )
            }

        root.addView(
            patient,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        addSpace(root, 12)

        root.addView(
            button(
                "📍  تحديد موقع المريض"
            ) {
                showLocation()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        addSpace(root, 8)

        root.addView(
            text(
                if (selectedAddress.isBlank())
                    "لم يتم تحديد الموقع بعد"
                else
                    selectedAddress,
                14f,
                GRAY
            )
        )

        addSpace(root, 10)

        val notes =
            EditText(this).apply {

                hint = "ملاحظات إضافية عن الحالة"

                textSize = 16f

                gravity =
                    Gravity.TOP or Gravity.RIGHT

                minLines = 4

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE

                background =
                    bordered(
                        WHITE,
                        BORDER,
                        14
                    )

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(15)
                )
            }

        root.addView(
            notes,
            LinearLayout.LayoutParams(
                -1,
                dp(130)
            )
        )

        addSpace(root, 18)

        root.addView(
            button(
                "📨  إرسال طلب التمريض الآن"
            ) {

                if (service.selectedItemPosition == 0) {

                    Toast.makeText(
                        this,
                        "اختر الخدمة أولاً",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@button
                }

                val patientName =
                    patient.text
                        .toString()
                        .trim()

                if (patientName.isEmpty()) {

                    patient.error =
                        "أدخل اسم المريض"

                    return@button
                }

                createBooking(
                    service.selectedItem.toString(),
                    patientName,
                    notes.text.toString().trim()
                )
            },
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        addSpace(root, 10)

        root.addView(
            outlineButton("إلغاء") {
                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        setContentView(scroll(root))
    }

    private fun createBooking(
        service: String,
        patient: String,
        notes: String
    ) {

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()

        if (user == null) {
            showPhoneLogin()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("تأكيد الطلب")
            .setMessage(
                "الخدمة: $service\n\n" +
                    "المريض: $patient\n\n" +
                    "سيتم إرسال الطلب فوراً إلى الممرضين المتاحين."
            )
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("إرسال") { _, _ ->

                val loading =
                    ProgressDialog(this).apply {
                        setMessage("جاري إرسال الطلب...")
                        setCancelable(false)
                        show()
                    }

                scope.launch {

                    try {

                        val booking =
                            BookingInsert(
                                patient_id = user.id,
                                service_id = service,
                                address =
                                    if (selectedAddress.isBlank())
                                        patient
                                    else
                                        selectedAddress,
                                latitude =
                                    selectedLatitude,
                                longitude =
                                    selectedLongitude,
                                status = "PENDING",
                                notes =
                                    if (notes.isBlank())
                                        "المريض: $patient"
                                    else
                                        "المريض: $patient\n$notes"
                            )

                        SupabaseManager
                            .client
                            .from("bookings")
                            .insert(booking)

                        loading.dismiss()

                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("تم إرسال الطلب ✅")
                            .setMessage(
                                "تم إرسال طلب التمريض بنجاح.\n\n" +
                                    "يمكنك متابعة حالته من صفحة الطلبات."
                            )
                            .setPositiveButton(
                                "متابعة الطلب"
                            ) { _, _ ->
                                showBookings()
                            }
                            .show()

                    } catch (e: Exception) {

                        loading.dismiss()

                        showError(
                            "تعذر إرسال الطلب",
                            e.message
                                ?: "حدث خطأ أثناء حفظ الطلب."
                        )
                    }
                }
            }
            .show()
    }

    /*
     * =========================================================
     * الخدمات
     * =========================================================
     */
    private fun showServices() {

        val root = baseLayout()

        root.addView(
            topBar(
                "الخدمات",
                ::showHome
            )
        )

        addSpace(root, 12)

        val services =
            listOf(
                Triple("💉", "إعطاء الحقن", "إعطاء الحقن حسب وصف الطبيب"),
                Triple("🩹", "تغيير الضماد", "العناية بالجروح والضمادات"),
                Triple("🩸", "قياس السكر", "فحص مستوى سكر الدم"),
                Triple("🩺", "قياس الضغط", "قياس ومتابعة ضغط الدم"),
                Triple("💧", "تركيب المحلول", "تركيب المحاليل حسب الحاجة"),
                Triple("👴", "رعاية كبار السن", "رعاية ومتابعة كبار السن"),
                Triple("🛏️", "رعاية المرضى", "رعاية المرضى داخل المنزل"),
                Triple("📋", "متابعة صحية", "متابعة الحالة الصحية")
            )

        for (service in services) {

            val card =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    layoutDirection =
                        View.LAYOUT_DIRECTION_RTL

                    background =
                        rounded(WHITE, 17)

                    setPadding(
                        dp(12),
                        dp(10),
                        dp(12),
                        dp(10)
                    )

                    setOnClickListener {
                        checkLoginBeforeRequest()
                    }
                }

            card.addView(
                text(
                    service.first,
                    32f,
                    NAVY
                ),
                LinearLayout.LayoutParams(
                    dp(60),
                    dp(70)
                )
            )

            val info =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.RIGHT
                }

            info.addView(
                text(
                    service.second,
                    18f,
                    NAVY,
                    true
                )
            )

            info.addView(
                text(
                    service.third,
                    13f,
                    GRAY
                )
            )

            card.addView(
                info,
                LinearLayout.LayoutParams(
                    0,
                    -2,
                    1f
                )
            )

            root.addView(
                card,
                LinearLayout.LayoutParams(
                    -1,
                    dp(82)
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

        addSpace(root, 15)

        root.addView(
            button("🩺 إنشاء طلب") {
                checkLoginBeforeRequest()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        setContentView(scroll(root))
    }

    /*
     * =========================================================
     * الموقع
     * =========================================================
     */
    private fun showLocation() {

        val root = baseLayout()

        root.addView(
            topBar(
                "موقع المريض",
                ::showRequestScreen
            )
        )

        addSpace(root, 20)

        root.addView(
            text("📍", 65f, NAVY)
        )

        root.addView(
            text(
                "حدد موقع المريض",
                26f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "يساعد الموقع الممرض على الوصول إليك بسهولة",
                15f,
                GRAY
            )
        )

        addSpace(root, 25)

        root.addView(
            button(
                "📍  فتح خرائط Google"
            ) {

                try {

                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("geo:33.3500,43.7833")
                        )

                    startActivity(intent)

                } catch (_: Exception) {

                    Toast.makeText(
                        this,
                        "تعذر فتح الخرائط",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        addSpace(root, 10)

        root.addView(
            outlineButton(
                "📌 استخدام موقعي الحالي"
            ) {

                /*
                 * مؤقتاً: موقع الفلوجة.
                 * في الخطوة التالية سنستبدله بـ GPS الحقيقي.
                 */
                selectedLatitude = 33.3500
                selectedLongitude = 43.7833
                selectedAddress = "الفلوجة - محافظة الأنبار"

                Toast.makeText(
                    this,
                    "تم تحديد الموقع",
                    Toast.LENGTH_SHORT
                ).show()

                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        addSpace(root, 20)

        root.addView(
            emptyState(
                "📍",
                if (selectedAddress.isBlank())
                    "لم يتم تحديد الموقع"
                else
                    selectedAddress,
                "يمكنك تغيير الموقع قبل إرسال الطلب"
            )
        )

        setContentView(scroll(root))
    }

    /*
     * =========================================================
     * الطلبات
     * =========================================================
     */
    private fun showBookings() {

        val root = baseLayout()

        root.addView(
            topBar("الطلبات")
        )

        addSpace(root, 10)

        root.addView(
            button("＋   إنشاء طلب") {
                checkLoginBeforeRequest()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        addSpace(root, 15)

        val loading =
            text(
                "جاري تحميل الطلبات...",
                16f,
                GRAY
            )

        root.addView(loading)

        addSpace(root, 15)

        root.addView(
            bottomNavigation("orders")
        )

        setContentView(scroll(root))

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()

        if (user == null) {

            loading.text =
                "سجل الدخول لعرض طلباتك"

            return
        }

        scope.launch {

            try {

                val bookings =
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
                        .decodeList<PatientBooking>()

                loading.visibility =
                    View.GONE

                if (bookings.isEmpty()) {

                    root.addView(
                        emptyState(
                            "📭",
                            "لا توجد طلبات بعد",
                            "عند إنشاء طلب تمريض سيظهر هنا"
                        ),
                        root.indexOfChild(loading) + 1
                    )

                } else {

                    bookings
                        .sortedByDescending {
                            it.created_at
                        }
                        .forEach {
                            addBookingCard(
                                root,
                                it
                            )
                        }
                }

            } catch (e: Exception) {

                loading.text =
                    "تعذر تحميل الطلبات\n\n" +
                        (
                            e.message
                                ?: "خطأ غير معروف"
                            )
            }
        }
    }

    private fun addBookingCard(
        root: LinearLayout,
        booking: PatientBooking
    ) {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    rounded(WHITE, 20)

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(15)
                )

                elevation =
                    dp(2).toFloat()
            }

        card.addView(
            text(
                "🩺  ${booking.service_id}",
                19f,
                NAVY,
                true
            )
        )

        card.addView(
            text(
                "👤  ${booking.address}",
                15f,
                TEXT
            )
        )

        card.addView(
            text(
                "الحالة: ${statusText(booking.status)}",
                16f,
                statusColor(booking.status),
                true
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
                    dp(10)
                )
            }
        )
    }

    private fun statusText(
        status: String
    ): String {

        return when (status.uppercase()) {
            "PENDING" -> "بانتظار قبول الممرض"
            "ACCEPTED" -> "تم قبول الطلب"
            "ON_THE_WAY" -> "الممرض في الطريق"
            "IN_PROGRESS" -> "الزيارة جارية"
            "COMPLETED" -> "اكتملت الزيارة"
            "CANCELLED" -> "تم إلغاء الطلب"
            else -> status
        }
    }

    private fun statusColor(
        status: String
    ): Int {

        return when (status.uppercase()) {
            "PENDING" -> ORANGE
            "ACCEPTED" -> BLUE
            "ON_THE_WAY" -> BLUE
            "IN_PROGRESS" -> GREEN
            "COMPLETED" -> GREEN
            "CANCELLED" -> RED
            else -> GRAY
        }
    }

    /*
     * =========================================================
     * المحادثات
     * =========================================================
     */
    private fun showChats() {

        val root = baseLayout()

        root.addView(topBar("المحادثات"))

        addSpace(root, 10)

        val search =
            EditText(this).apply {

                hint = "ابحث..."

                textSize = 16f

                gravity = Gravity.RIGHT

                background =
                    bordered(
                        WHITE,
                        BORDER,
                        15
                    )

                setPadding(
                    dp(15),
                    dp(5),
                    dp(15),
                    dp(5)
                )
            }

        root.addView(
            search,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        addSpace(root, 20)

        root.addView(
            chatCard(
                "🛡️",
                "دعم التمريض",
                "تواصل مع فريق التمريض",
                true
            ) {

                Toast.makeText(
                    this,
                    "سيتم فتح المحادثة في المرحلة التالية",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        addSpace(root, 20)

        root.addView(
            emptyState(
                "💬",
                "لا توجد محادثات بعد",
                "ستظهر محادثاتك هنا عند بدء التواصل"
            )
        )

        addSpace(root, 15)

        root.addView(
            bottomNavigation("chat")
        )

        setContentView(scroll(root))
    }

    private fun chatCard(
        icon: String,
        title: String,
        description: String,
        verified: Boolean,
        action: () -> Unit
    ): LinearLayout {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    rounded(WHITE, 18)

                setPadding(
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
                )

                setOnClickListener {
                    action()
                }
            }

        card.addView(
            text(icon, 35f, NAVY),
            LinearLayout.LayoutParams(
                dp(60),
                dp(70)
            )
        )

        card.addView(
            text(
                "$title\n$description",
                16f,
                TEXT,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        if (verified) {
            card.addView(
                text(
                    "✓",
                    25f,
                    WHITE,
                    true
                ).apply {
                    background = rounded(NAVY, 50)
                },
                LinearLayout.LayoutParams(
                    dp(45),
                    dp(45)
                )
            )
        }

        return card
    }

    /*
     * =========================================================
     * المزيد
     * =========================================================
     */
    private fun showMore() {

        val root = baseLayout()

        root.addView(topBar("المزيد"))

        addSpace(root, 10)

        root.addView(
            menuCard(
                "👤",
                "حسابي"
            ) {
                showAccount()
            }
        )

        root.addView(
            menuCard(
                "✉️",
                "تواصل معنا"
            ) {
                contactUs()
            }
        )

        root.addView(
            menuCard(
                "ⓘ",
                "عن التطبيق"
            ) {
                showAbout()
            }
        )

        root.addView(
            menuCard(
                "?",
                "الأسئلة الشائعة"
            ) {
                showFaq()
            }
        )

        root.addView(
            menuCard(
                "▤",
                "الشروط والأحكام"
            ) {
                showTerms()
            }
        )

        root.addView(
            menuCard(
                "▤",
                "سياسة الخصوصية"
            ) {
                showPrivacy()
            }
        )

        root.addView(
            menuCard(
                "⇥",
                "تسجيل الخروج"
            ) {
                logout()
            }
        )

        addSpace(root, 15)

        root.addView(
            bottomNavigation("more")
        )

        setContentView(scroll(root))
    }

    private fun menuCard(
        icon: String,
        title: String,
        action: () -> Unit
    ): LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            background =
                rounded(WHITE, 12)

            setPadding(
                dp(12),
                dp(6),
                dp(12),
                dp(6)
            )

            setOnClickListener {
                action()
            }

            addView(
                text(icon, 24f, TEXT),
                LinearLayout.LayoutParams(
                    dp(55),
                    dp(55)
                )
            )

            addView(
                text(title, 17f, TEXT),
                LinearLayout.LayoutParams(
                    0,
                    dp(55),
                    1f
                )
            )

            addView(
                text("‹", 28f, GRAY)
            )
        }
    }

    private fun showAccount() {

        val root = baseLayout()

        root.addView(
            topBar(
                "حسابي",
                ::showMore
            )
        )

        addSpace(root, 20)

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()

        root.addView(
            text("👤", 65f, NAVY)
        )

        root.addView(
            text(
                "حسابي",
                27f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                user?.phone ?: "غير مسجل",
                18f,
                GRAY
            )
        )

        addSpace(root, 25)

        root.addView(
            button("تسجيل الخروج") {
                logout()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        setContentView(scroll(root))
    }

    private fun logout() {

        AlertDialog.Builder(this)
            .setTitle("تسجيل الخروج")
            .setMessage("هل تريد تسجيل الخروج؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("خروج") { _, _ ->

                scope.launch {

                    try {
                        SupabaseManager
                            .client
                            .auth
                            .signOut()
                    } catch (_: Exception) {
                    }

                    phoneNumber = ""

                    showPhoneLogin()
                }
            }
            .show()
    }

    private fun emptyState(
        icon: String,
        title: String,
        description: String
    ): LinearLayout {

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                background =
                    rounded(WHITE, 18)

                setPadding(
                    dp(15),
                    dp(25),
                    dp(15),
                    dp(25)
                )
            }

        box.addView(
            text(icon, 48f, GRAY)
        )

        box.addView(
            text(
                title,
                20f,
                NAVY,
                true
            )
        )

        box.addView(
            text(
                description,
                14f,
                GRAY
            )
        )

        return box
    }

    private fun contactUs() {

        AlertDialog.Builder(this)
            .setTitle("☎️ تواصل معنا")
            .setMessage(
                "التمريض المنزلي\n\n" +
                    "محافظة الأنبار - العراق\n\n" +
                    "يمكنك التواصل مع إدارة الخدمة للاستفسارات والمساعدة."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showAbout() {

        AlertDialog.Builder(this)
            .setTitle("عن التطبيق")
            .setMessage(
                "التمريض المنزلي\n\n" +
                    "منصة لخدمات التمريض والرعاية الصحية المنزلية في محافظة الأنبار - العراق."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showFaq() {

        AlertDialog.Builder(this)
            .setTitle("الأسئلة الشائعة")
            .setMessage(
                "كيف أطلب ممرضاً؟\n\n" +
                    "بعد تسجيل الدخول اضغط إنشاء طلب، اختر الخدمة وأدخل بيانات المريض والموقع ثم أرسل الطلب.\n\n" +
                    "هل أحتاج إلى حجز موعد؟\n\n" +
                    "لا، الطلب الحالي فوري بدون تاريخ أو وقت.\n\n" +
                    "أين أتابع الطلب؟\n\n" +
                    "من صفحة الطلبات."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showTerms() {

        AlertDialog.Builder(this)
            .setTitle("الشروط والأحكام")
            .setMessage(
                "يجب إدخال معلومات صحيحة عن المريض.\n\n" +
                    "الخدمات التمريضية لا تغني عن مراجعة الطبيب في الحالات الطارئة."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showPrivacy() {

        AlertDialog.Builder(this)
            .setTitle("سياسة الخصوصية")
            .setMessage(
                "نستخدم بيانات الحساب والطلب لتقديم خدمة التمريض ومتابعة الطلب."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showError(
        title: String,
        message: String
    ) {

        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show()
    }
}
