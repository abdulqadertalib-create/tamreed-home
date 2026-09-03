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
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class AdminRecord(val user_id: String)

@Serializable
data class AdminNurseRecord(
    val id: String? = null,
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val specialty: String? = null,
    val experience_years: Int? = null,
    val city: String? = null,
    val address: String? = null,
    val is_available: Boolean? = false,
    val is_verified: Boolean? = false
)

class AdminActivity : AppCompatActivity() {
    private val navy = Color.rgb(5, 62, 105)
    private val DARK_NAVY = Color.rgb(3, 45, 78)
    private val blue = Color.rgb(31, 115, 176)
    private val green = Color.rgb(35, 145, 85)
    private val red = Color.rgb(180, 50, 50)
    private val gray = Color.rgb(110, 110, 110)
    private val light = Color.rgb(247, 248, 249)
    private val white = Color.WHITE
    private val border = Color.rgb(218, 224, 229)
    private val NAVY = navy
    private val GREEN = green
    private val WHITE = white
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = gray
    private val LIGHT_GRAY = light
    private val BORDER = border
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAdmin()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun bg(color: Int, radius: Int = 18, stroke: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            if (stroke != null) setStroke(dp(1), stroke)
            cornerRadius = dp(radius).toFloat()
        }

    // Helpers used by the professional login/OTP UI.
    private fun rounded(color: Int, radius: Int = 18): GradientDrawable =
        bg(color, radius)

    private fun bordered(color: Int, stroke: Int, radius: Int = 18): GradientDrawable =
        bg(color, radius, stroke)

    private fun text(value: String, size: Float, color: Int = navy, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

    private fun button(title: String, color: Int, action: () -> Unit) =
        Button(this).apply {
            text = title
            textSize = 16f
            isAllCaps = false
            setTextColor(white)
            background = bg(color, 15)
            setOnClickListener { action() }
        }

    private fun button(title: String, action: () -> Unit): Button =
        Button(this).apply {
            text = title
            textSize = 17f
            isAllCaps = false
            setTextColor(white)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(navy, 15)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setOnClickListener { action() }
        }

    private fun outlineButton(title: String, action: () -> Unit): Button =
        Button(this).apply {
            text = title
            textSize = 16f
            isAllCaps = false
            setTextColor(navy)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(white, 14, navy)
            setOnClickListener { action() }
        }

    private fun checkAdmin() {
        val user = SupabaseManager.client.auth.currentUserOrNull()
        if (user == null) {
            showLogin()
            return
        }

        scope.launch {
            try {
                val admins = SupabaseManager.client.from("admin_users").select {
                    filter { eq("user_id", user.id) }
                }.decodeList<AdminRecord>()

                if (admins.isEmpty()) showNotAdmin() else showDashboard()
            } catch (e: Exception) {
                showError("تعذر التحقق من صلاحيات الإدارة",
                    e.message ?: "تأكد من تنفيذ SQL الخاص بالإدارة.")
            }
        }
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

    private fun showLogin() {

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
            text("دخول الإدارة", 29f, NAVY, true).apply {
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(-1, dp(43))
        )

        root.addView(
            text("أدخل رقم هاتف حساب الإدارة للمتابعة", 14f, GRAY).apply {
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
            button("إرسال رمز التحقق") {
                val input = phone.text.toString().trim()
                val normalized = normalizePhone(input)

                if (normalized == null) {
                    phone.error = "أدخل رقم هاتف عراقي صحيح"
                    return@button
                }

                scope.launch {
                    val loading = ProgressDialog.show(
                        this@AdminActivity,
                        null,
                        "جاري إرسال رمز التحقق...",
                        true,
                        false
                    )
                    try {
                        SupabaseManager.client.auth.signInWith(OTP) {
                            phone = normalized
                        }
                        loading.dismiss()
                        showOtp(normalized)
                    } catch (e: Exception) {
                        loading.dismiss()
                        showError(
                            "تعذر إرسال الرمز",
                            e.message ?: "تأكد من إعداد Phone Auth في Supabase."
                        )
                    }
                }
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
            text("دخول إداري آمن • حسابات الإدارة المصرح لها فقط", 11f, NAVY, true).apply {
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            },
            LinearLayout.LayoutParams(0, dp(38), 1f)
        )
        root.addView(trust, LinearLayout.LayoutParams(-1, dp(40)).apply { topMargin = dp(4) })

        // لا يوجد ScrollView هنا؛ الواجهة مصممة لتظهر كاملة على شاشة الهاتف.
        setContentView(root)
    }

    private fun showOtp(phone: String) {

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
            setOnClickListener { showLogin() }
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
            text(phone, 17f, NAVY, true).apply {
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
            button("تأكيد الرمز") {

                val code = otp.text.toString().trim()

                if (code.length != 6) {
                    otp.error = "أدخل رمز التحقق المكوّن من 6 أرقام"
                    otp.requestFocus()
                    return@button
                }

                scope.launch {
                        val loading = ProgressDialog.show(
                            this@AdminActivity,
                            null,
                            "جاري التحقق...",
                            true,
                            false
                        )
                        try {
                            SupabaseManager.client.auth.verifyPhoneOtp(
                                type = OtpType.Phone.SMS,
                                phone = phone,
                                token = code
                            )
                            loading.dismiss()
                            checkAdmin()
                        } catch (e: Exception) {
                            loading.dismiss()
                            showError(
                                "رمز التحقق غير صحيح",
                                e.message ?: "حاول مرة أخرى."
                            )
                        }
                    }
            },
            LinearLayout.LayoutParams(-1, dp(54)).apply {
                topMargin = dp(9)
            }
        )

        root.addView(
            outlineButton("إرسال رمز جديد") {
                scope.launch {
                    try {
                        SupabaseManager.client.auth.signInWith(OTP) {
                            this.phone = phone
                        }
                        Toast.makeText(
                            this@AdminActivity,
                            "تم إرسال رمز جديد",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        showError(
                            "تعذر إرسال الرمز",
                            e.message ?: "حاول مرة أخرى."
                        )
                    }
                }
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

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(light)
            setPadding(dp(14), dp(22), dp(14), dp(30))
        }

        root.addView(text("🛡️", 52f))
        root.addView(text("لوحة إدارة الممرضين", 28f, navy, true))
        root.addView(text("اعتماد الممرضين الجدد", 16f, gray))

        root.addView(button("🔄 تحديث القائمة", blue) {
            loadNurses(root)
        }, LinearLayout.LayoutParams(-1, dp(58)))

        val logout = button("تسجيل الخروج", navy) { signOut() }
        logout.background = bg(white, 15, navy)
        logout.setTextColor(navy)
        root.addView(logout, LinearLayout.LayoutParams(-1, dp(55)))

        setContentView(ScrollView(this).apply { addView(root) })
        loadNurses(root)
    }

    private fun loadNurses(root: LinearLayout) {
        scope.launch {
            try {
                val nurses = SupabaseManager.client
                    .from("nurses")
                    .select()
                    .decodeList<AdminNurseRecord>()

                while (root.childCount > 4) root.removeViewAt(4)

                val pending = nurses.count { it.is_verified != true }
                val approved = nurses.count { it.is_verified == true }

                root.addView(text(
                    "بانتظار الاعتماد: $pending    |    معتمد: $approved",
                    17f, navy, true
                ))

                if (nurses.isEmpty()) {
                    root.addView(text("لا توجد حسابات ممرضين حاليًا.", 17f, gray))
                    return@launch
                }

                nurses.sortedBy { it.is_verified == true }
                    .forEach { nurse -> addNurseCard(root, nurse) }

            } catch (e: Exception) {
                showError("تعذر تحميل الممرضين",
                    e.message ?: "تحقق من صلاحيات الإدارة في Supabase.")
            }
        }
    }

    private fun addNurseCard(root: LinearLayout, nurse: AdminNurseRecord) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(white, 20, border)
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        card.addView(text(nurse.full_name ?: "بدون اسم", 20f, navy, true))
        card.addView(text("التخصص: ${nurse.specialty ?: "غير محدد"}", 15f, gray))
        card.addView(text("الخبرة: ${nurse.experience_years ?: 0} سنوات", 15f, gray))
        card.addView(text("المحافظة/المدينة: ${nurse.city ?: "غير محدد"}", 15f, gray))
        card.addView(text("العنوان: ${nurse.address ?: "غير محدد"}", 15f, gray))
        card.addView(text("الهاتف: ${nurse.phone ?: "غير محدد"}", 15f, gray))

        if (nurse.is_verified == true) {
            card.addView(text("✅ معتمد", 17f, green, true))
        } else {
            card.addView(text("⏳ بانتظار الاعتماد", 17f, Color.rgb(190, 120, 20), true))
            card.addView(button("✅ اعتماد الممرض", green) {
                confirmApproval(nurse, root)
            }, LinearLayout.LayoutParams(-1, dp(56)))
            card.addView(button("❌ رفض الطلب", red) {
                confirmReject(nurse, root)
            }, LinearLayout.LayoutParams(-1, dp(52)))
        }

        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, dp(10), 0, 0)
        root.addView(card, lp)
    }

    private fun confirmApproval(nurse: AdminNurseRecord, root: LinearLayout) {
        AlertDialog.Builder(this)
            .setTitle("اعتماد الممرض")
            .setMessage("هل تريد اعتماد ${nurse.full_name ?: "هذا الممرض"}؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("اعتماد") { _, _ ->
                setVerified(nurse, true, root)
            }
            .show()
    }

    private fun confirmReject(nurse: AdminNurseRecord, root: LinearLayout) {
        AlertDialog.Builder(this)
            .setTitle("رفض طلب الممرض")
            .setMessage("سيبقى الحساب غير معتمد. هل تريد المتابعة؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("رفض") { _, _ ->
                setVerified(nurse, false, root)
            }
            .show()
    }

    private fun setVerified(
        nurse: AdminNurseRecord,
        verified: Boolean,
        root: LinearLayout
    ) {
        val id = nurse.id ?: return

        scope.launch {
            val loading = ProgressDialog.show(
                this@AdminActivity, null, "جاري الحفظ...", true, false
            )
            try {
                SupabaseManager.client.from("nurses").update({
                    set("is_verified", verified)
                }) {
                    filter { eq("id", id) }
                }

                loading.dismiss()
                Toast.makeText(
                    this@AdminActivity,
                    if (verified) "تم اعتماد الممرض" else "تم رفض الطلب",
                    Toast.LENGTH_LONG
                ).show()

                loadNurses(root)
            } catch (e: Exception) {
                loading.dismiss()
                showError("تعذر حفظ القرار",
                    e.message ?: "تحقق من صلاحيات الإدارة.")
            }
        }
    }

    private fun showNotAdmin() {
        AlertDialog.Builder(this)
            .setTitle("غير مصرح")
            .setMessage(
                "هذا الحساب ليس حساب إدارة. أضف user_id الخاص بالمدير إلى جدول admin_users في Supabase."
            )
            .setPositiveButton("حسنًا") { _, _ -> finish() }
            .show()
    }

    private fun signOut() {
        scope.launch {
            try {
                SupabaseManager.client.auth.signOut()
            } catch (_: Exception) {}
            showLogin()
        }
    }

    private fun normalizePhone(value: String): String? {
        var p = value.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (p.startsWith("+964")) {
            return if (p.length == 14 && p.getOrNull(4) == '7') p else null
        }

        if (p.startsWith("00964")) p = "+" + p.substring(2)
        if (p.startsWith("07")) p = "+964" + p.substring(1)

        return if (p.length == 14 && p.startsWith("+9647")) p else null
    }

    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حسنًا", null)
            .show()
    }
}
