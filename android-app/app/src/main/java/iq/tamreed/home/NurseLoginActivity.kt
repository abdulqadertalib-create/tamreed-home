package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
data class NurseProfile(
    val id: String,
    val full_name: String? = null,
    val phone: String? = null,
    val active: Boolean = true
)

class NurseLoginActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val DARK_NAVY = Color.rgb(3, 45, 78)
    private val BLUE = Color.rgb(31, 115, 176)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val WHITE = Color.WHITE
    private val BORDER = Color.rgb(218, 224, 229)

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var phoneNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user =
            SupabaseManager.client.auth.currentUserOrNull()

        if (user != null) {
            checkNurseAndContinue()
        } else {
            showPhoneScreen()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(
        color: Int,
        radius: Int = 18
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun bordered(
        color: Int = WHITE,
        strokeColor: Int = BORDER,
        radius: Int = 16
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
        }

    private fun rootLayout(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)
            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(80)
            )
        }

    private fun scroll(view: View): ScrollView =
        ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(LIGHT_GRAY)
            addView(view)
        }

    private fun text(
        value: String,
        size: Float = 16f,
        color: Int = TEXT,
        bold: Boolean = false
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
            setPadding(
                dp(6),
                dp(6),
                dp(6),
                dp(6)
            )
        }

    private fun primaryButton(
        title: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            text = title
            textSize = 17f
            isAllCaps = false
            setTextColor(WHITE)
            gravity = Gravity.CENTER
            background = rounded(NAVY, 15)
            setOnClickListener {
                action()
            }
        }

    private fun outlineButton(
        title: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            text = title
            textSize = 16f
            isAllCaps = false
            setTextColor(NAVY)
            gravity = Gravity.CENTER
            background = bordered(
                WHITE,
                NAVY,
                15
            )
            setOnClickListener {
                action()
            }
        }

    private fun addSpace(
        parent: LinearLayout,
        height: Int
    ) {
        parent.addView(
            Space(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

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

    private fun showPhoneScreen() {

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = LIGHT_GRAY
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // شاشة دخول قصيرة ومتجاوبة، مستوحاة من واجهة التسجيل الحديثة.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)
            setPadding(dp(16), dp(4), dp(16), dp(4))
        }

        // شريط علوي بسيط: اللغة + سهم الرجوع/الخروج.
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(WHITE)
            setPadding(dp(2), 0, dp(2), 0)
        }

        val language = TextView(this).apply {
            text = "文  English"
            textSize = 14f
            setTextColor(TEXT)
            gravity = Gravity.CENTER
            background = bordered(WHITE, BORDER, 12)
            setTypeface(null, Typeface.BOLD)
        }
        header.addView(language, LinearLayout.LayoutParams(dp(112), dp(46)))

        header.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))

        val headerArrow = TextView(this).apply {
            text = "→"
            textSize = 34f
            setTextColor(TEXT)
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        header.addView(headerArrow, LinearLayout.LayoutParams(dp(52), dp(46)))

        root.addView(header, LinearLayout.LayoutParams(-1, dp(52)))

        // مساحة قصيرة قبل الشعار حتى لا تصبح الشاشة طويلة.
        addSpace(root, 18)

        root.addView(
            nursingLogo(),
            LinearLayout.LayoutParams(dp(104), dp(104))
        )

        root.addView(
            text("التمريض المنزلي", 27f, NAVY, true).apply {
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(-1, dp(40)).apply {
                topMargin = dp(5)
            }
        )

        root.addView(
            text("رعاية تمريضية منزلية موثوقة في الأنبار", 12f, GRAY).apply {
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(-1, dp(24))
        )

        addSpace(root, 12)

        root.addView(
            text("دخول الممرض", 29f, NAVY, true).apply {
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(-1, dp(43))
        )

        root.addView(
            text("أدخل رقم هاتفك للمتابعة", 14f, GRAY).apply {
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(-1, dp(25))
        )

        addSpace(root, 7)

        // بطاقة الهاتف: مرتبة ومضغوطة حتى لا تختفي الكتابة.
        val loginCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 22)
            elevation = dp(2).toFloat()
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }

        val phoneLabel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val phoneIcon = TextView(this).apply {
            text = "☎"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(NAVY)
            background = rounded(LIGHT_BLUE, 11)
        }

        phoneLabel.addView(
            phoneIcon,
            LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                marginStart = dp(8)
            }
        )

        phoneLabel.addView(
            text("رقم الهاتف", 17f, NAVY, true).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            },
            LinearLayout.LayoutParams(0, dp(38), 1f)
        )

        loginCard.addView(phoneLabel)

        val phone = EditText(this).apply {
            hint = "07701234567"
            textSize = 18f
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
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(5)
            }
        )

        loginCard.addView(
            text("مثال: 07701234567", 10f, GRAY).apply {
                includeFontPadding = false
                gravity = Gravity.RIGHT
            },
            LinearLayout.LayoutParams(-1, dp(16))
        )

        loginCard.addView(
            primaryButton("إرسال رمز التحقق") {
                val input = phone.text.toString().trim()
                val normalized = normalizeIraqPhone(input)

                if (normalized == null) {
                    phone.error = "أدخل رقم هاتف عراقي صحيح"
                    return@primaryButton
                }

                phoneNumber = normalized
                sendOtp()
            },
            LinearLayout.LayoutParams(-1, dp(47)).apply {
                topMargin = dp(4)
            }
        )

        root.addView(
            loginCard,
            LinearLayout.LayoutParams(-1, dp(170))
        )

        root.addView(
            text("تسجيل آمن برمز OTP • بياناتك محمية", 10f, GRAY).apply {
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(-1, dp(21)).apply {
                topMargin = dp(3)
            }
        )

        // بطاقة ثقة قصيرة.
        val trust = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(LIGHT_BLUE, 14)
            setPadding(dp(8), 0, dp(8), 0)
        }
        trust.addView(
            text("✓", 22f, GREEN, true).apply { includeFontPadding = false },
            LinearLayout.LayoutParams(dp(34), dp(36))
        )
        trust.addView(
            text("خدمة تمريض منزلية آمنة • للممرضين المعتمدين", 11f, NAVY, true).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            },
            LinearLayout.LayoutParams(0, dp(38), 1f)
        )
        root.addView(trust, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(4) })

        // لا يوجد ScrollView هنا؛ الواجهة مصممة لتظهر كاملة على شاشة الهاتف.
        setContentView(root)
    }

    private fun normalizeIraqPhone(
        value: String
    ): String? {

        var phone =
            value
                .trim()
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
            phone =
                "+" + phone.substring(2)

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
            phone =
                "+964" + phone.substring(1)

            return if (
                phone.length == 14
            ) {
                phone
            } else {
                null
            }
        }

        return null
    }

    private fun sendOtp() {

        val loading =
            ProgressDialog(this).apply {
                setMessage(
                    "جاري إرسال رمز التحقق..."
                )
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                SupabaseManager.client.auth
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

    private fun showOtpScreen() {

        window.statusBarColor = DARK_NAVY
        window.navigationBarColor = LIGHT_GRAY

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)

            // إنزال الشاشة قليلاً عن شريط الحالة.
            setPadding(dp(18), dp(14), dp(18), dp(6))

            clipChildren = false
            clipToPadding = false
        }

        // -----------------------------------------------------
        // الشريط العلوي
        // -----------------------------------------------------
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 16)
            elevation = dp(1).toFloat()
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }

        val back = TextView(this).apply {
            text = "‹"
            textSize = 34f
            setTextColor(NAVY)
            gravity = Gravity.CENTER
            includeFontPadding = true
            setOnClickListener { showPhoneScreen() }
        }

        header.addView(
            back,
            LinearLayout.LayoutParams(dp(42), dp(46))
        )

        header.addView(
            text("تأكيد رقم الهاتف", 20f, NAVY, true).apply {
                gravity = Gravity.CENTER
                includeFontPadding = true
                setPadding(dp(4), dp(2), dp(4), dp(2))
            },
            LinearLayout.LayoutParams(0, dp(46), 1f)
        )

        val secure = TextView(this).apply {
            text = "آمن"
            textSize = 11f
            setTextColor(GREEN)
            gravity = Gravity.CENTER
            includeFontPadding = true
            background = rounded(LIGHT_BLUE, 10)
        }

        header.addView(
            secure,
            LinearLayout.LayoutParams(dp(48), dp(28))
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, dp(50))
        )

        // -----------------------------------------------------
        // رمز الأمان
        // -----------------------------------------------------
        val securityBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val securityIcon = TextView(this).apply {
            text = "✓"
            textSize = 30f
            setTextColor(WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = true
            background = rounded(NAVY, 45)
        }

        securityBox.addView(
            securityIcon,
            LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                topMargin = dp(10)
            }
        )

        securityBox.addView(
            text("تحقق آمن", 12f, GREEN, true).apply {
                gravity = Gravity.CENTER
                includeFontPadding = true
                setPadding(dp(4), dp(1), dp(4), dp(1))
            },
            LinearLayout.LayoutParams(-1, dp(24)).apply {
                topMargin = dp(2)
            }
        )

        root.addView(
            securityBox,
            LinearLayout.LayoutParams(-1, dp(108))
        )

        // -----------------------------------------------------
        // العناوين — ارتفاع كافٍ حتى لا تختفي الحروف العربية
        // -----------------------------------------------------
        root.addView(
            text("أدخل رمز التحقق", 27f, NAVY, true).apply {
                gravity = Gravity.CENTER
                includeFontPadding = true
                setPadding(dp(6), dp(1), dp(6), dp(1))
            },
            LinearLayout.LayoutParams(-1, dp(48))
        )

        root.addView(
            text("تم إرسال رمز مكوّن من 6 أرقام إلى", 13f, GRAY).apply {
                gravity = Gravity.CENTER
                includeFontPadding = true
                setPadding(dp(6), 0, dp(6), 0)
            },
            LinearLayout.LayoutParams(-1, dp(25))
        )

        root.addView(
            text(phoneNumber, 17f, NAVY, true).apply {
                gravity = Gravity.CENTER
                includeFontPadding = true
                setPadding(dp(6), 0, dp(6), 0)
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                textDirection = View.TEXT_DIRECTION_LTR
            },
            LinearLayout.LayoutParams(-1, dp(30))
        )

        // -----------------------------------------------------
        // بطاقة رمز التحقق
        // -----------------------------------------------------
        val otpCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 22)
            elevation = dp(2).toFloat()
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        otpCard.addView(
            text("رمز التحقق", 14f, NAVY, true).apply {
                gravity = Gravity.CENTER
                includeFontPadding = true
                setPadding(dp(4), 0, dp(4), 0)
            },
            LinearLayout.LayoutParams(-1, dp(25))
        )

        val otp = EditText(this).apply {
            hint = "000000"
            textSize = 28f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textDirection = View.TEXT_DIRECTION_LTR
            maxLines = 1
            isSingleLine = true
            includeFontPadding = true
            filters = arrayOf(InputFilter.LengthFilter(6))
            background = bordered(WHITE, BORDER, 16)
            setPadding(dp(10), dp(2), dp(10), dp(2))
        }

        otpCard.addView(
            otp,
            LinearLayout.LayoutParams(-1, dp(58)).apply {
                topMargin = dp(4)
            }
        )

        root.addView(
            otpCard,
            LinearLayout.LayoutParams(-1, dp(101)).apply {
                topMargin = dp(7)
            }
        )

        // -----------------------------------------------------
        // الأزرار
        // -----------------------------------------------------
        root.addView(
            primaryButton("تأكيد الرمز") {

                val code = otp.text.toString().trim()

                if (code.length != 6) {
                    otp.error = "أدخل رمز التحقق المكوّن من 6 أرقام"
                    otp.requestFocus()
                    return@button
                }

                verifyOtp(code)
            },
            LinearLayout.LayoutParams(-1, dp(54)).apply {
                topMargin = dp(9)
            }
        )

        root.addView(
            outlineButton("إرسال رمز جديد") {
                sendOtp()
            },
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                topMargin = dp(7)
            }
        )

        root.addView(
            text("لا تشارك رمز التحقق مع أي شخص", 11f, GRAY).apply {
                gravity = Gravity.CENTER
                includeFontPadding = true
                setPadding(dp(4), 0, dp(4), 0)
            },
            LinearLayout.LayoutParams(-1, dp(22)).apply {
                topMargin = dp(4)
            }
        )

        // شاشة ثابتة بلا تمرير؛ مناسبة للهاتف ولا تصبح طويلة.
        setContentView(root)
    }

    private fun verifyOtp(
        code: String
    ) {

        val loading =
            ProgressDialog(this).apply {
                setMessage(
                    "جاري التحقق..."
                )
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                SupabaseManager.client.auth
                    .verifyPhoneOtp(
                        type = OtpType.Phone.SMS,
                        phone = phoneNumber,
                        token = code
                    )

                loading.dismiss()

                checkNurseAndContinue()

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

    private fun checkNurseAndContinue() {

        val user =
            SupabaseManager.client.auth
                .currentUserOrNull()

        if (user == null) {
            showPhoneScreen()
            return
        }

        val loading =
            ProgressDialog(this).apply {
                setMessage(
                    "جاري التحقق من اعتماد الممرض..."
                )
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                val nurses =
                    SupabaseManager.client
                        .from("nurses")
                        .select {
                            filter {
                                eq(
                                    "id",
                                    user.id
                                )
                                eq(
                                    "active",
                                    true
                                )
                            }
                        }
                        .decodeList<NurseProfile>()

                loading.dismiss()

                if (nurses.isEmpty()) {

                    /*
                     * المستخدم موثق برقم الهاتف لكنه
                     * ليس ضمن جدول الممرضين المعتمدين.
                     */
                    try {
                        SupabaseManager.client.auth
                            .signOut()
                    } catch (_: Exception) {
                    }

                    showError(
                        "الدخول غير مصرح",
                        "هذا الرقم غير مسجل كممرض معتمد في النظام.\n\n" +
                            "يرجى التواصل مع إدارة التمريض لإضافة الحساب."
                    )

                    return@launch
                }

                startActivity(
                    Intent(
                        this@NurseLoginActivity,
                        NurseActivity::class.java
                    )
                )

                finish()

            } catch (e: Exception) {

                loading.dismiss()

                showError(
                    "تعذر التحقق من الممرض",
                    "تأكد من إنشاء جدول nurses في Supabase.\n\n" +
                        (e.message ?: "")
                )
            }
        }
    }

    private fun showError(
        title: String,
        message: String
    ) {

        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                "حسناً",
                null
            )
            .show()
    }
}
