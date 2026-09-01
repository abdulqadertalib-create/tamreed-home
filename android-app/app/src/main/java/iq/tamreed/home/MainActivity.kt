package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.provider.Settings
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
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
    val city: String,
    val landmark: String,
    val patient_phone: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = "PENDING",
    val notes: String? = null
)


@Serializable
data class ServiceRecord(
    val id: String,
    val name_ar: String
)


@Serializable
data class PatientBooking(
    val id: String,
    val patient_id: String,
    val nurse_id: String? = null,
    val service_id: String,
    val address: String,
    val city: String? = null,
    val landmark: String? = null,
    val patient_phone: String? = null,
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
    private var patientPhone = ""
    private var selectedCity = ""
    private var landmark = ""
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedAddress = ""
    private var currentLocationText = "لم يتم تحديد الموقع"
    private val LOCATION_REQUEST_CODE = 2001

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

    private fun medicalVisualCard(
        icon: String,
        title: String,
        description: String
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 20)
            elevation = dp(2).toFloat()
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        card.addView(
            text(
                icon,
                38f,
                NAVY
            ),
            LinearLayout.LayoutParams(
                dp(65),
                dp(70)
            )
        )

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.RIGHT
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        info.addView(
            text(
                title,
                17f,
                NAVY,
                true
            )
        )

        info.addView(
            text(
                description,
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

        return card
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

    private fun accountSummaryCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 20)
            elevation = dp(2).toFloat()
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val avatar = TextView(this).apply {
            text = "👤"
            textSize = 30f
            gravity = Gravity.CENTER
            background = rounded(LIGHT_BLUE, 18)
        }

        card.addView(
            avatar,
            LinearLayout.LayoutParams(dp(60), dp(60)).apply {
                marginStart = dp(10)
            }
        )

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.RIGHT
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val title = if (phoneNumber.isBlank()) "مرحباً بك" else "مرحباً بك"
        info.addView(text(title, 18f, NAVY, true))
        info.addView(
            text(
                if (phoneNumber.isBlank()) "حساب المريض" else phoneNumber,
                13f,
                GRAY
            )
        )

        val verified = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        verified.addView(text("✓", 15f, GREEN, true))
        verified.addView(text(" حساب موثق", 12f, GREEN, true))
        info.addView(verified)

        card.addView(
            info,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        return card
    }

    private fun quickActionCard(
        icon: String,
        title: String,
        subtitle: String,
        action: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bordered(WHITE, BORDER, 18)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            elevation = dp(1).toFloat()
            setOnClickListener { action() }

            addView(text(icon, 28f, NAVY, true))
            addView(text(title, 15f, NAVY, true))
            addView(text(subtitle, 11f, GRAY))
        }
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
    /**
     * شعار تمريضي أصلي مرسوم بالكود:
     * - لا توجد سماعة طبية.
     * - دائرة احترافية + علامة صحية + نبض ECG.
     * - لا يحتاج إلى أي صورة داخل drawable.
     */
    private fun nursingLogo(): View {
        return NursingLogoView(this)
    }

    private inner class NursingLogoView(context: android.content.Context) :
        View(context) {

        private val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = NAVY
            style = Paint.Style.FILL
        }

        private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GREEN
            style = Paint.Style.STROKE
            strokeWidth = dp(5).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WHITE
            style = Paint.Style.FILL
        }

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WHITE
            style = Paint.Style.STROKE
            strokeWidth = dp(4).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val w = width.toFloat()
            val h = height.toFloat()
            val size = minOf(w, h)
            val cx = w / 2f
            val cy = h / 2f
            val radius = size * 0.39f

            // ظل خفيف
            canvas.drawCircle(
                cx + dp(1),
                cy + dp(2),
                radius + dp(2),
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(22, 0, 0, 0)
                    style = Paint.Style.FILL
                }
            )

            // الدائرة الأساسية
            canvas.drawCircle(cx, cy, radius, navyPaint)

            // حلقة خضراء رفيعة
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = GREEN
                style = Paint.Style.STROKE
                strokeWidth = dp(3).toFloat()
            }
            canvas.drawCircle(cx, cy, radius - dp(2), ringPaint)

            // علامة + الصحية
            val crossWidth = radius * 0.48f
            val crossHeight = radius * 0.48f
            val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                cx - dp(5),
                cy - crossHeight / 2f,
                cx + dp(5),
                cy + crossHeight / 2f,
                dp(4).toFloat(),
                dp(4).toFloat(),
                crossPaint
            )
            canvas.drawRoundRect(
                cx - crossWidth / 2f,
                cy - dp(5),
                cx + crossWidth / 2f,
                cy + dp(5),
                dp(4).toFloat(),
                dp(4).toFloat(),
                crossPaint
            )

            // خط نبض ECG أسفل العلامة الصحية
            val ecg = Path()
            val y = cy + radius * 0.42f
            ecg.moveTo(cx - radius * 0.62f, y)
            ecg.lineTo(cx - radius * 0.38f, y)
            ecg.lineTo(cx - radius * 0.25f, y - radius * 0.20f)
            ecg.lineTo(cx - radius * 0.08f, y + radius * 0.28f)
            ecg.lineTo(cx + radius * 0.10f, y - radius * 0.34f)
            ecg.lineTo(cx + radius * 0.24f, y)
            ecg.lineTo(cx + radius * 0.62f, y)
            canvas.drawPath(ecg, greenPaint)
        }
    }

    /*
     * =========================================================
     * الشاشة الأولى: تسجيل الدخول برقم الهاتف
     * شاشة مضغوطة ومتجاوبة، بدون تمرير طويل.
     * =========================================================
     */
    private fun showPhoneLogin() {

        window.statusBarColor = DARK_NAVY
        window.navigationBarColor = LIGHT_GRAY

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)
            setPadding(dp(14), dp(5), dp(14), dp(4))
        }

        // =========================================================
        // الهوية البصرية - شعار تمريضي حديث بدون سماعة طبية
        // =========================================================
        val logoArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 22)
            elevation = dp(2).toFloat()
            setPadding(dp(8), dp(3), dp(8), dp(3))
        }

        logoArea.addView(
            nursingLogo(),
            LinearLayout.LayoutParams(dp(68), dp(68))
        )

        logoArea.addView(
            text("التمريض المنزلي", 21f, NAVY, true).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(28))
        )

        logoArea.addView(
            text("رعاية تمريضية منزلية موثوقة في الأنبار", 10f, GRAY, false).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(18))
        )

        root.addView(
            logoArea,
            LinearLayout.LayoutParams(-1, dp(121))
        )

        // =========================================================
        // العنوان
        // =========================================================
        root.addView(
            text("تسجيل الدخول", 24f, NAVY, true).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(38)).apply {
                topMargin = dp(3)
            }
        )

        root.addView(
            text("أدخل رقم هاتفك للمتابعة", 12f, GRAY, false).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(21))
        )

        // =========================================================
        // بطاقة تسجيل الدخول
        // =========================================================
        val loginCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 21)
            elevation = dp(2).toFloat()
            setPadding(dp(13), dp(7), dp(13), dp(7))
        }

        val phoneLabel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // رمز هاتف بسيط وواضح بدون Emoji كبير يغيّر حجمه حسب الجهاز.
        val phoneIcon = TextView(this).apply {
            text = "☎︎"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(NAVY)
            background = rounded(LIGHT_BLUE, 11)
        }

        phoneLabel.addView(
            phoneIcon,
            LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                marginStart = dp(8)
            }
        )

        phoneLabel.addView(
            text("رقم الهاتف", 16f, NAVY, true).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            },
            LinearLayout.LayoutParams(0, dp(36), 1f)
        )

        loginCard.addView(phoneLabel)

        val phone = EditText(this).apply {
            hint = "07701234567"
            textSize = 17f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_PHONE
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            maxLines = 1
            isSingleLine = true
            background = bordered(WHITE, BORDER, 15)
            setPadding(dp(10), 0, dp(10), 0)
        }

        loginCard.addView(
            phone,
            LinearLayout.LayoutParams(-1, dp(49)).apply {
                topMargin = dp(5)
            }
        )

        loginCard.addView(
            text("مثال: 07701234567", 10f, GRAY, false).apply {
                includeFontPadding = false
                gravity = Gravity.RIGHT
            },
            LinearLayout.LayoutParams(-1, dp(16))
        )

        loginCard.addView(
            button("إرسال رمز التحقق") {
                val input = phone.text.toString().trim()
                val normalized = normalizeIraqPhone(input)

                if (normalized == null) {
                    phone.error = "أدخل رقم هاتف عراقي صحيح"
                    return@button
                }

                phoneNumber = normalized
                sendOtp()
            },
            LinearLayout.LayoutParams(-1, dp(46)).apply {
                topMargin = dp(4)
            }
        )

        // ارتفاع البطاقة محسوب حسب محتوياتها حتى لا تختفي الكتابة.
        root.addView(
            loginCard,
            LinearLayout.LayoutParams(-1, dp(171)).apply {
                topMargin = dp(4)
            }
        )

        // =========================================================
        // رسالة الأمان
        // =========================================================
        root.addView(
            text("تسجيل آمن برمز OTP • بياناتك محمية", 10f, GRAY, false).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(19)).apply {
                topMargin = dp(2)
            }
        )

        // =========================================================
        // بطاقة الثقة
        // =========================================================
        val trust = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(LIGHT_BLUE, 14)
            setPadding(dp(7), dp(1), dp(7), dp(1))
        }

        trust.addView(
            text("✓", 21f, GREEN, true).apply {
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(dp(32), dp(32))
        )

        trust.addView(
            text("رعاية المريض أولاً • خدمة محلية في الأنبار", 11f, NAVY, true).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            },
            LinearLayout.LayoutParams(0, dp(36), 1f)
        )

        root.addView(
            trust,
            LinearLayout.LayoutParams(-1, dp(42)).apply {
                topMargin = dp(3)
            }
        )

        // =========================================================
        // دخول الممرضين والإدارة
        // =========================================================
        val staffRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        staffRow.addView(
            outlineButton("دخول الممرضين") {
                startActivity(Intent(this, NurseLoginActivity::class.java))
            },
            LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                marginEnd = dp(4)
            }
        )

        staffRow.addView(
            outlineButton("دخول الإدارة") {
                startActivity(Intent(this, AdminActivity::class.java))
            },
            LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                marginStart = dp(4)
            }
        )

        root.addView(
            staffRow,
            LinearLayout.LayoutParams(-1, dp(43)).apply {
                topMargin = dp(4)
            }
        )

        root.addView(
            text("شروط الاستخدام وسياسة الخصوصية تنطبق عند تسجيل الدخول.", 9f, GRAY, false).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(18)).apply {
                topMargin = dp(1)
            }
        )

        // بدون ScrollView: الشاشة الأولى مضغوطة ومناسبة لشاشة الهاتف.
        setContentView(root)
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

        root.addView(accountSummaryCard())

        addSpace(root, 12)

        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        quickRow.addView(
            quickActionCard("📋", "طلباتي", "متابعة الطلبات") {
                showBookings()
            },
            LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                marginEnd = dp(5)
            }
        )

        quickRow.addView(
            quickActionCard("🩺", "الخدمات", "اختر خدمة") {
                showServices()
            },
            LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                marginStart = dp(5)
            }
        )

        root.addView(quickRow)

        addSpace(root, 14)

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
                "رعاية تمريضية منزلية منظمة",
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

        addSpace(root, 16)

        root.addView(
            text(
                "الرعاية المنزلية باحتراف",
                22f,
                NAVY,
                true
            )
        )

        addSpace(root, 8)

        root.addView(
            medicalVisualCard(
                "👩‍⚕️",
                "ممرضون وممرضات",
                "خدمة تمريض منزلية منظمة مع متابعة حالة الطلب."
            )
        )

        addSpace(root, 8)

        root.addView(
            medicalVisualCard(
                "🩺",
                "عناية صحية منزلية",
                "حقن، جروح، قياسات حيوية، ورعاية كبار السن."
            )
        )

        addSpace(root, 8)

        root.addView(
            medicalVisualCard(
                "📍",
                "وصول أسهل للمريض",
                "حدد موقع المريض والنقطة الدالة لمساعدة الممرض على الوصول."
            )
        )

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
                "بيانات طلب التمريض",
                25f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "أدخل معلومات المريض والموقع بالتفصيل",
                15f,
                GRAY
            )
        )

        addSpace(root, 15)

        // ----------------------------------------------------
        // الخدمة
        // ----------------------------------------------------

        val service = Spinner(this)

        val serviceNames = mutableListOf("جاري تحميل الخدمات...")
        val serviceItems = mutableListOf<ServiceRecord>()

        service.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                serviceNames
            )

        root.addView(
            service,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        // تحميل الخدمات الحقيقية من جدول services.
        scope.launch {
            try {
                val loadedServices =
                    SupabaseManager.client
                        .from("services")
                        .select {
                            filter {
                                eq("is_active", true)
                            }
                        }
                        .decodeList<ServiceRecord>()

                serviceItems.clear()
                serviceNames.clear()
                serviceNames.add("اختر الخدمة")

                serviceItems.addAll(loadedServices)
                serviceNames.addAll(loadedServices.map { it.name_ar })

                service.adapter =
                    ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        serviceNames
                    )
            } catch (e: Exception) {
                serviceNames.clear()
                serviceNames.add("تعذر تحميل الخدمات")
                service.adapter =
                    ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        serviceNames
                    )

                Toast.makeText(
                    this@MainActivity,
                    "تعذر تحميل الخدمات: ${e.message ?: "خطأ غير معروف"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        addSpace(root, 12)

        // ----------------------------------------------------
        // اسم المريض
        // ----------------------------------------------------

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

        // ----------------------------------------------------
        // رقم هاتف المريض
        // ----------------------------------------------------

        val patientPhoneInput =
            EditText(this).apply {

                hint = "رقم هاتف المريض للتواصل"

                textSize = 17f

                gravity = Gravity.CENTER

                inputType =
                    InputType.TYPE_CLASS_PHONE

                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR

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

                /*
                 * نضع رقم تسجيل الدخول افتراضياً.
                 * يستطيع المستخدم تغييره إذا كان رقم المريض مختلفاً.
                 */
                setText(
                    phoneNumber
                )
            }

        root.addView(
            patientPhoneInput,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        addSpace(root, 6)

        root.addView(
            text(
                "سيستخدم الممرض هذا الرقم للتواصل مع المريض عند الحاجة.",
                13f,
                GRAY
            )
        )

        addSpace(root, 12)

        // ----------------------------------------------------
        // مدينة / قضاء الأنبار
        // ----------------------------------------------------

        root.addView(
            text(
                "المدينة / القضاء",
                16f,
                NAVY,
                true
            )
        )

        addSpace(root, 4)

        val citySpinner =
            Spinner(this)

        val anbarCities =
            arrayOf(
                "اختر المدينة / القضاء",
                "الرمادي",
                "الفلوجة",
                "الكرمة",
                "الحبانية",
                "الخالدية",
                "هيت",
                "حديثة",
                "عانة",
                "راوة",
                "القائم",
                "الرطبة",
                "البغدادي",
                "عامرية الصمود"
            )

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
            )
        )

        addSpace(root, 12)

        // ----------------------------------------------------
        // أقرب نقطة دالة
        // ----------------------------------------------------

        val landmarkInput =
            EditText(this).apply {

                hint = "أقرب نقطة دالة (جامع، مدرسة، مستشفى، شارع...)"

                textSize = 16f

                gravity =
                    Gravity.RIGHT

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
                    dp(12),
                    dp(15),
                    dp(12)
                )
            }

        root.addView(
            landmarkInput,
            LinearLayout.LayoutParams(
                -1,
                dp(75)
            )
        )

        addSpace(root, 12)

        // ----------------------------------------------------
        // الموقع
        // ----------------------------------------------------

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

        // ----------------------------------------------------
        // الملاحظات
        // ----------------------------------------------------

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

        addSpace(root, 12)

        root.addView(
            medicalVisualCard(
                "🛡️",
                "تنبيه طبي",
                "الخدمة التمريضية لا تستبدل الطبيب أو الطوارئ. في الحالات الحرجة اتصل بالإسعاف فوراً. لا تشارك بيانات حساسة غير ضرورية في الملاحظات."
            )
        )

        addSpace(root, 18)

        // ----------------------------------------------------
        // إرسال الطلب
        // ----------------------------------------------------

        root.addView(
            medicalVisualCard(
                "📋",
                "راجع بيانات الطلب قبل الإرسال",
                "تأكد من اسم المريض والهاتف والمدينة والموقع والخدمة المختارة."
            )
        )

        addSpace(root, 12)

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

                val enteredPhone =
                    normalizeIraqPhone(
                        patientPhoneInput.text
                            .toString()
                            .trim()
                    )

                if (enteredPhone == null) {

                    patientPhoneInput.error =
                        "أدخل رقم هاتف عراقي صحيح"

                    return@button
                }

                if (citySpinner.selectedItemPosition == 0) {

                    Toast.makeText(
                        this,
                        "اختر المدينة / القضاء",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@button
                }

                val selectedCityValue =
                    citySpinner.selectedItem
                        .toString()
                        .trim()

                val enteredLandmark =
                    landmarkInput.text
                        .toString()
                        .trim()

                if (enteredLandmark.isEmpty()) {

                    landmarkInput.error =
                        "أدخل أقرب نقطة دالة"

                    return@button
                }

                patientPhone =
                    enteredPhone

                selectedCity =
                    selectedCityValue

                landmark =
                    enteredLandmark

                if (serviceItems.isEmpty() ||
                    service.selectedItemPosition <= 0 ||
                    service.selectedItemPosition > serviceItems.size
                ) {
                    Toast.makeText(
                        this,
                        "اختر خدمة متاحة أولاً",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@button
                }

                val selectedService =
                    serviceItems[service.selectedItemPosition - 1]

                createBooking(
                    selectedService,
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
        service: ServiceRecord,
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

        val phoneForBooking =
            if (patientPhone.isBlank())
                phoneNumber
            else
                patientPhone

        AlertDialog.Builder(this)
            .setTitle("تأكيد الطلب")
            .setMessage(
                "الخدمة: ${service.name_ar}\n\n" +
                    "المريض: $patient\n\n" +
                    "الهاتف: $phoneForBooking\n\n" +
                    "المدينة: $selectedCity\n\n" +
                    "أقرب نقطة دالة: $landmark\n\n" +
                    "سيتم إرسال الطلب فوراً إلى الممرضين المتاحين."
            )
            .setNegativeButton("تعديل", null)
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
                                // مهم: service_id في قاعدة البيانات UUID،
                                // لذلك نرسل id الحقيقي للخدمة وليس الاسم العربي.
                                service_id = service.id,
                                address =
                                    if (selectedAddress.isBlank())
                                        "$selectedCity - $landmark"
                                    else
                                        selectedAddress,
                                city = selectedCity,
                                landmark = landmark,
                                patient_phone = phoneForBooking,
                                latitude = selectedLatitude,
                                longitude = selectedLongitude,
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

                        selectedLatitude = null
                        selectedLongitude = null
                        selectedAddress = ""
                        selectedCity = ""
                        landmark = ""
                        patientPhone = ""

                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("تم إرسال الطلب ✅")
                            .setMessage(
                                "تم إرسال طلب التمريض بنجاح.\n\n" +
                                    "رقم الطلب محفوظ في حسابك ويمكنك متابعة حالته من قسم طلباتي.\n\n" +
                                    "سيتمكن الممرض المقبول من رؤية رقم هاتف المريض وبيانات الموقع للتواصل والوصول."
                            )
                            .setNegativeButton("العودة للرئيسية") { _, _ ->
                                showHome()
                            }
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
                                ?: "تأكد من إضافة أعمدة المدينة والنقطة الدالة ورقم الهاتف إلى جدول bookings."
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
                "الخدمات الطبية",
                ::showHome
            )
        )

        addSpace(root, 10)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(NAVY, 22)
            setPadding(dp(18), dp(20), dp(18), dp(20))
        }

        hero.addView(text("🩺", 42f, WHITE, true))
        hero.addView(text("خدمات التمريض المنزلي", 25f, WHITE, true))
        hero.addView(
            text(
                "اختر الخدمة المناسبة ثم اطلع على تفاصيلها قبل إرسال الطلب.",
                15f,
                Color.rgb(225, 238, 247)
            )
        )

        root.addView(
            hero,
            LinearLayout.LayoutParams(-1, dp(155))
        )

        addSpace(root, 12)

        val search = EditText(this).apply {
            hint = "ابحث عن خدمة..."
            textSize = 16f
            gravity = Gravity.RIGHT
            setSingleLine(true)
            background = bordered(WHITE, BORDER, 15)
            setPadding(dp(15), dp(5), dp(15), dp(5))
        }

        root.addView(
            search,
            LinearLayout.LayoutParams(-1, dp(58))
        )

        addSpace(root, 10)

        val services = listOf(
            Triple("💉", "إعطاء الحقن", "إعطاء الحقن حسب وصف الطبيب"),
            Triple("🩹", "تغيير الضماد", "العناية بالجروح والضمادات"),
            Triple("🩸", "قياس السكر", "فحص مستوى سكر الدم"),
            Triple("🩺", "قياس الضغط", "قياس ومتابعة ضغط الدم"),
            Triple("💧", "تركيب المحلول", "تركيب المحاليل حسب الحاجة"),
            Triple("👴", "رعاية كبار السن", "رعاية ومتابعة كبار السن"),
            Triple("🛏️", "رعاية المرضى", "رعاية المرضى داخل المنزل"),
            Triple("📋", "متابعة صحية", "متابعة الحالة الصحية")
        )

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        root.addView(container)

        fun render(filter: String) {
            container.removeAllViews()

            val query = filter.trim()
            val filtered = if (query.isBlank()) {
                services
            } else {
                services.filter {
                    it.second.contains(query, true) ||
                        it.third.contains(query, true)
                }
            }

            if (filtered.isEmpty()) {
                container.addView(
                    emptyState(
                        "🔎",
                        "لم نجد الخدمة المطلوبة",
                        "جرّب كتابة اسم خدمة آخر."
                    )
                )
                return
            }

            filtered.forEach { service ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    background = bordered(WHITE, BORDER, 18)
                    elevation = dp(2).toFloat()
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    setOnClickListener {
                        showServiceDetails(
                            service.first,
                            service.second,
                            service.third
                        )
                    }
                }

                val icon = text(service.first, 34f, NAVY, true).apply {
                    gravity = Gravity.CENTER
                    background = rounded(LIGHT_BLUE, 16)
                }

                card.addView(
                    icon,
                    LinearLayout.LayoutParams(dp(62), dp(62)).apply {
                        marginStart = dp(8)
                    }
                )

                val info = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                }

                info.addView(text(service.second, 18f, NAVY, true))
                info.addView(text(service.third, 13f, GRAY))

                card.addView(
                    info,
                    LinearLayout.LayoutParams(0, -2, 1f)
                )

                card.addView(
                    text("›", 30f, NAVY, true),
                    LinearLayout.LayoutParams(dp(35), dp(60))
                )

                container.addView(
                    card,
                    LinearLayout.LayoutParams(-1, dp(86)).apply {
                        bottomMargin = dp(10)
                    }
                )
            }
        }

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                render(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })

        render("")

        addSpace(root, 5)

        root.addView(
            button("🩺  إنشاء طلب تمريض") {
                checkLoginBeforeRequest()
            },
            LinearLayout.LayoutParams(-1, dp(60))
        )

        addSpace(root, 8)
        root.addView(bottomNavigation("services"))

        setContentView(scroll(root))
    }

    private fun showServiceDetails(
        icon: String,
        title: String,
        description: String
    ) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(10), dp(20), dp(5))
        }

        content.addView(
            text(icon, 52f, NAVY, true),
            LinearLayout.LayoutParams(-1, dp(70))
        )

        content.addView(
            text(title, 22f, NAVY, true)
        )

        content.addView(
            text(
                description,
                16f,
                TEXT
            )
        )

        content.addView(
            text(
                "يتم إرسال الطلب إلى النظام ليتم التعامل معه من خلال الممرضين المتاحين.",
                14f,
                GRAY
            )
        )

        AlertDialog.Builder(this)
            .setTitle("تفاصيل الخدمة")
            .setView(content)
            .setNegativeButton("إغلاق", null)
            .setPositiveButton("اطلب هذه الخدمة") { _, _ ->
                checkLoginBeforeRequest()
            }
            .show()
    }

    // =====================================================
    // تحديد موقع المريض و GPS
    // =====================================================

    private fun showLocation() {

        val root = baseLayout()

        root.addView(
            text("📍 تحديد موقع المريض", 29f, DARK_NAVY)
        )

        root.addView(
            text(
                "يساعد الموقع الممرض على الوصول إلى المكان الصحيح",
                17f,
                GRAY
            )
        )

        val status = text(currentLocationText, 17f, DARK_NAVY)
        root.addView(
            status,
            LinearLayout.LayoutParams(-1, dp(125)).apply {
                setMargins(0, dp(20), 0, dp(10))
            }
        )

        root.addView(
            button("📍 الحصول على موقعي الحالي") {
                status.text = "جاري تحديد موقعك..."
                getCurrentLocation(status)
            },
            LinearLayout.LayoutParams(-1, dp(65)).apply {
                bottomMargin = dp(8)
            }
        )

        root.addView(
            button("🗺️ فتح الموقع في خرائط Google") {
                openCurrentLocationInMaps()
            },
            LinearLayout.LayoutParams(-1, dp(60)).apply {
                bottomMargin = dp(8)
            }
        )

        root.addView(
            button("↩️ رجوع") {
                showServices()
            },
            LinearLayout.LayoutParams(-1, dp(55))
        )

        setContentView(scroll(root))
    }

    private fun getCurrentLocation(statusView: TextView) {

        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
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

        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    selectedLatitude = location.latitude
                    selectedLongitude = location.longitude
                    selectedAddress = "موقع GPS: %.6f, %.6f".format(
                        location.latitude,
                        location.longitude
                    )
                    currentLocationText =
                        "تم تحديد الموقع بنجاح ✅\n\n" +
                        "خط العرض: %.6f\n".format(location.latitude) +
                        "خط الطول: %.6f".format(location.longitude)
                    statusView.text = currentLocationText
                    Toast.makeText(
                        this,
                        "تم تحديد موقعك بنجاح",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    statusView.text =
                        "تعذر الحصول على الموقع الحالي.\nتأكد من تشغيل GPS ثم حاول مرة أخرى."
                }
            }
            .addOnFailureListener { error ->
                statusView.text =
                    "حدث خطأ أثناء تحديد الموقع:\n${error.message ?: "خطأ غير معروف"}"
            }
    }

    private fun openCurrentLocationInMaps() {
        val lat = selectedLatitude
        val lon = selectedLongitude

        if (lat == null || lon == null) {
            Toast.makeText(this, "حدد موقعك أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            try {
                val webUri = Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
                )
                startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "تعذر فتح خرائط Google",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
                Toast.makeText(
                    this,
                    "تم السماح بالموقع. اضغط تحديد الموقع مرة أخرى.",
                    Toast.LENGTH_LONG
                ).show()
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

        if (!booking.city.isNullOrBlank()) {
            card.addView(
                text(
                    "🏙️  المدينة: ${booking.city}",
                    14f,
                    TEXT
                )
            )
        }

        if (!booking.landmark.isNullOrBlank()) {
            card.addView(
                text(
                    "📌  أقرب نقطة دالة: ${booking.landmark}",
                    14f,
                    TEXT
                )
            )
        }

        if (!booking.patient_phone.isNullOrBlank()) {

            card.addView(
                text(
                    "📞  هاتف المريض: ${booking.patient_phone}",
                    14f,
                    NAVY,
                    true
                )
            )

            card.addView(
                outlineButton(
                    "اتصال بالمريض"
                ) {

                    try {

                        val intent =
                            Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse(
                                    "tel:${booking.patient_phone}"
                                )
                            )

                        startActivity(intent)

                    } catch (_: Exception) {

                        Toast.makeText(
                            this,
                            "تعذر فتح الاتصال",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                LinearLayout.LayoutParams(
                    -1,
                    dp(50)
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
                    patientPhone = ""
                    selectedCity = ""
                    landmark = ""
                    selectedLatitude = null
                    selectedLongitude = null
                    selectedAddress = ""

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
                    "منصة رقمية لخدمات التمريض والرعاية الصحية المنزلية في محافظة الأنبار - العراق.\n\n" +
                    "تساعدك على تسجيل الدخول برقم الهاتف، اختيار الخدمة، إدخال بيانات المريض، تحديد الموقع، إرسال الطلب ومتابعة حالته."
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
                "نستخدم بيانات الحساب والطلب والموقع فقط لتقديم خدمة التمريض ومتابعة الطلب ومساعدة الممرض على الوصول إلى موقع المريض. لا تكتب في الملاحظات معلومات حساسة غير ضرورية."
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
