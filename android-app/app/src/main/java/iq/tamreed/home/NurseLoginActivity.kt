package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.from

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import kotlinx.serialization.Serializable


@Serializable
data class NurseLoginRecord(
    val id: String? = null,
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val specialty: String? = null,
    val experience_years: Int? = null,
    val city: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_available: Boolean? = false,
    val is_verified: Boolean? = false
)

@Serializable
data class NurseCreateRecord(
    val user_id: String,
    val full_name: String,
    val phone: String,
    val specialty: String,
    val experience_years: Int,
    val city: String,
    val address: String,
    val is_available: Boolean = true,
    val is_verified: Boolean = false
)


class NurseLoginActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val BLUE = Color.rgb(31, 115, 176)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val WHITE = Color.WHITE
    private val BORDER = Color.rgb(218, 224, 229)
    private val GREEN = Color.rgb(35, 145, 85)
    private val RED = Color.rgb(180, 50, 50)

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val sessionPrefs by lazy {
        getSharedPreferences("tamreed_session", MODE_PRIVATE)
    }

    private var phoneNumber = ""
    private var passwordResetFlow = false

    // بيانات تسجيل الممرض الجديد
    private var isNewNurseRegistration = false
    private var pendingFullName = ""
    private var pendingSpecialty = ""
    private var pendingExperienceYears = 0
    private var pendingCity = ""
    private var pendingAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // التعديل المهم:
        // إذا كان هناك جلسة Supabase محفوظة، لا نطلب رقم الهاتف ولا OTP مرة أخرى.
        continueFromSavedSession()
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
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)
            setPadding(dp(12), dp(10), dp(12), dp(20))
        }

    private fun scroll(view: View): ScrollView =
        ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(LIGHT_GRAY)
            addView(view)
        }

    private fun makeText(
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
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(dp(6), dp(6), dp(6), dp(6))
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
            background = rounded(NAVY, 16)
            setOnClickListener { action() }
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
            background = bordered(WHITE, NAVY, 16)
            setOnClickListener { action() }
        }

    private fun addSpace(
        parent: LinearLayout,
        height: Int
    ) {
        parent.addView(Space(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun field(
        hintText: String,
        inputTypeValue: Int = InputType.TYPE_CLASS_TEXT
    ): EditText =
        EditText(this).apply {
            hint = hintText
            textSize = 17f
            gravity = Gravity.CENTER
            inputType = inputTypeValue
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bordered(WHITE, BORDER, 15)
            setPadding(dp(15), dp(5), dp(15), dp(5))
        }

    // ========================================================
    // التعديل: استئناف الجلسة المحفوظة
    // ========================================================

    private fun continueFromSavedSession() {
        val user = SupabaseManager.client.auth.currentUserOrNull()

        if (user == null) {
            showPhoneScreen()
            return
        }

        phoneNumber = user.phone ?: ""

        // لا نطلب الهاتف أو OTP إذا كانت الجلسة موجودة.
        checkNurseAndContinue()
    }

    private fun showPhoneScreen() {
        isNewNurseRegistration = false

        val root = rootLayout()

        root.addView(makeText("✚", 46f, NAVY))
        root.addView(makeText("التمريض المنزلي", 26f, NAVY, true))
        root.addView(
            makeText(
                "دخول الممرضين وإدارة الطلبات",
                16f,
                GRAY
            )
        )

        addSpace(root, 10)

        val loginCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(WHITE, 22)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        loginCard.addView(makeText("📱", 32f, NAVY))
        loginCard.addView(
            makeText(
                "تسجيل دخول الممرض",
                18f,
                NAVY,
                true
            )
        )

        addSpace(loginCard, 8)

        val phone = EditText(this).apply {
            hint = "07810056006"
            textSize = 19f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_PHONE
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = bordered(WHITE, BORDER, 15)
            setPadding(dp(15), dp(5), dp(15), dp(5))
        }

        loginCard.addView(
            phone,
            LinearLayout.LayoutParams(-1, dp(54))
        )

        val password = EditText(this).apply {
            hint = "كلمة المرور"
            textSize = 17f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            maxLines = 1
            isSingleLine = true
            background = bordered(WHITE, BORDER, 15)
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }
        loginCard.addView(password, LinearLayout.LayoutParams(-1, dp(52)).apply {
            topMargin = dp(8)
        })

        loginCard.addView(
            primaryButton("🔐 دخول بكلمة المرور") {
                val normalized = normalizeIraqPhone(phone.text.toString())
                val pass = password.text.toString()
                if (normalized == null) {
                    phone.error = "رقم الهاتف العراقي غير صحيح"
                    return@primaryButton
                }
                if (pass.length < 6) {
                    password.error = "كلمة المرور 6 أحرف/أرقام على الأقل"
                    return@primaryButton
                }
                signInNurseWithPassword(normalized, pass)
            },
            LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) }
        )

        loginCard.addView(
            outlineButton("نسيت كلمة المرور") {
                val normalized = normalizeIraqPhone(phone.text.toString())
                if (normalized == null) {
                    phone.error = "أدخل رقم الهاتف أولاً"
                    return@outlineButton
                }
                phoneNumber = normalized
                passwordResetFlow = true
                sendOtp()
            },
            LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(6) }
        )

        loginCard.addView(
            makeText(
                "يمكنك أيضاً استخدام رمز SMS",
                13f,
                GRAY
            )
        )

        addSpace(loginCard, 7)

        loginCard.addView(
            primaryButton("دخول باستخدام رمز التحقق") {
                val normalized = normalizeIraqPhone(phone.text.toString())

                if (normalized == null) {
                    phone.error = "رقم الهاتف العراقي غير صحيح"
                    return@primaryButton
                }

                phoneNumber = normalized
                isNewNurseRegistration = false
                sendOtp()
            },
            LinearLayout.LayoutParams(-1, dp(54))
        )

        root.addView(loginCard, LinearLayout.LayoutParams(-1, -2))

        addSpace(root, 14)

        val newNurseCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(LIGHT_BLUE, 20)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        newNurseCard.addView(makeText("✚", 34f, NAVY))
        newNurseCard.addView(
            makeText(
                "ممرض جديد؟",
                18f,
                NAVY,
                true
            )
        )
        newNurseCard.addView(
            makeText(
                "أنشئ حسابك من هنا وأرسل بياناتك المهنية إلى الإدارة لاعتمادها.",
                14f,
                TEXT
            )
        )

        addSpace(newNurseCard, 8)

        newNurseCard.addView(
            primaryButton("➕ تسجيل ممرض جديد") {
                showNewNurseRegistrationScreen()
            },
            LinearLayout.LayoutParams(-1, dp(52))
        )

        root.addView(
            newNurseCard,
            LinearLayout.LayoutParams(-1, -2)
        )

        addSpace(root, 14)

        root.addView(
            outlineButton("العودة") { finish() },
            LinearLayout.LayoutParams(-1, dp(56))
        )

        setContentView(scroll(root))
    }

    private fun normalizeIraqPhone(value: String): String? {
        var phone = value.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (phone.startsWith("+964")) {
            return if (
                phone.length == 14 &&
                phone.getOrNull(4) == '7'
            ) phone else null
        }

        if (phone.startsWith("00964")) {
            phone = "+" + phone.substring(2)

            return if (
                phone.length == 14 &&
                phone.getOrNull(4) == '7'
            ) phone else null
        }

        if (phone.startsWith("07")) {
            phone = "+964" + phone.substring(1)

            return if (
                phone.length == 14 &&
                phone.getOrNull(4) == '7'
            ) phone else null
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
                showOtpScreen()

            } catch (e: Exception) {
                loading.dismiss()
                showError(
                    "تعذر إرسال رمز التحقق",
                    e.message ?: "تأكد من إعداد SMS في Supabase."
                )
            }
        }
    }

    private fun showOtpScreen() {
        val root = rootLayout()

        root.addView(makeText("🔐", 44f, NAVY))
        root.addView(
            makeText(
                "تأكيد رقم الممرض",
                28f,
                NAVY,
                true
            )
        )
        root.addView(makeText(phoneNumber, 18f, NAVY, true))

        addSpace(root, 12)

        val code = EditText(this).apply {
            hint = "000000"
            textSize = 24f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            maxLines = 1
            filters = arrayOf(InputFilter.LengthFilter(6))
            background = bordered(WHITE, BORDER, 15)
            setPadding(dp(15), dp(5), dp(15), dp(5))
        }

        root.addView(code, LinearLayout.LayoutParams(-1, dp(60)))

        addSpace(root, 10)

        root.addView(
            primaryButton("تأكيد الرمز") {
                val token = code.text.toString().trim()

                if (token.length != 6) {
                    code.error = "أدخل رمز التحقق المكون من 6 أرقام"
                    return@primaryButton
                }

                verifyOtp(token)
            },
            LinearLayout.LayoutParams(-1, dp(54))
        )

        addSpace(root, 12)

        root.addView(
            outlineButton("تغيير رقم الهاتف") {
                showPhoneScreen()
            },
            LinearLayout.LayoutParams(-1, dp(52))
        )

        addSpace(root, 10)

        root.addView(
            makeText(
                "أدخل الرمز الذي وصلك برسالة SMS",
                14f,
                GRAY
            )
        )

        setContentView(scroll(root))
    }

    private fun verifyOtp(token: String) {
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
                    token = token
                )

                loading.dismiss()

                // بعد التحقق بالـSMS نطلب إنشاء كلمة المرور مرة واحدة.
                // في حالة التسجيل الجديد سيتم إنشاء سجل الممرض بعد حفظ كلمة المرور.
                showSetPasswordScreen()

            } catch (e: Exception) {
                loading.dismiss()

                showError(
                    "رمز التحقق غير صحيح",
                    e.message ?: "تأكد من الرمز ثم حاول مرة أخرى."
                )
            }
        }
    }

    private fun signInNurseWithPassword(phone: String, password: String) {
        scope.launch {
            val loading = ProgressDialog.show(
                this@NurseLoginActivity, null, "جاري تسجيل الدخول...", true, false
            )
            try {
                SupabaseManager.client.auth.signInWith(Phone) {
                    this.phone = phone
                    this.password = password
                }
                loading.dismiss()
                phoneNumber = phone
                passwordResetFlow = false
                sessionPrefs.edit()
                    .putString("role", "nurse")
                    .putBoolean("password_configured", true)
                    .apply()
                FcmTokenManager.registerToken("nurse")
                checkNurseAndContinue()
            } catch (e: Exception) {
                loading.dismiss()
                showError(
                    "تعذر تسجيل الدخول",
                    "رقم الهاتف أو كلمة المرور غير صحيحة. استخدم «نسيت كلمة المرور» إذا لزم."
                )
            }
        }
    }

    private fun showSetPasswordScreen() {
        val root = rootLayout()
        root.addView(makeText("🔐", 48f, NAVY))
        root.addView(makeText("إنشاء كلمة المرور", 27f, NAVY, true))
        root.addView(
            makeText(
                if (passwordResetFlow)
                    "أنشئ كلمة مرور جديدة لحساب الممرض"
                else
                    "تم التحقق من رقم الهاتف. أنشئ كلمة مرور للدخول لاحقاً.",
                15f, GRAY
            )
        )
        root.addView(makeText(phoneNumber, 16f, NAVY, true))

        val pass = field("كلمة المرور الجديدة").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(pass, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(16) })

        val confirm = field("تأكيد كلمة المرور").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(confirm, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(10) })

        root.addView(
            primaryButton("💾 حفظ كلمة المرور") {
                val a = pass.text.toString()
                val b = confirm.text.toString()
                if (a.length < 6) {
                    pass.error = "استخدم 6 أحرف/أرقام على الأقل"
                    return@primaryButton
                }
                if (a != b) {
                    confirm.error = "كلمتا المرور غير متطابقتين"
                    return@primaryButton
                }

                scope.launch {
                    val loading = ProgressDialog.show(
                        this@NurseLoginActivity, null, "جاري حفظ كلمة المرور...", true, false
                    )
                    try {
                        SupabaseManager.client.auth.updateUser { password = a }
                        loading.dismiss()
                        passwordResetFlow = false
                        sessionPrefs.edit()
                            .putString("role", "nurse")
                            .putBoolean("password_configured", true)
                            .apply()
                        FcmTokenManager.registerToken("nurse")

                        if (isNewNurseRegistration) {
                            createNurseAccount(
                                pendingFullName,
                                pendingSpecialty,
                                pendingExperienceYears,
                                pendingCity,
                                pendingAddress
                            )
                        } else {
                            checkNurseAndContinue()
                        }
                    } catch (e: Exception) {
                        loading.dismiss()
                        showError("تعذر حفظ كلمة المرور", e.message ?: "حاول مرة أخرى.")
                    }
                }
            },
            LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(16) }
        )

        root.addView(
            outlineButton("نسيت كلمة المرور؟ أرسل رمزاً جديداً") {
                passwordResetFlow = true
                sendOtp()
            },
            LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) }
        )

        setContentView(scroll(root))
    }

    // ========================================================
    // فحص حساب الممرض
    // ========================================================

    private fun checkNurseAndContinue() {
        val user = SupabaseManager.client.auth.currentUserOrNull()

        if (user == null) {
            showPhoneScreen()
            return
        }

        phoneNumber = user.phone ?: phoneNumber

        scope.launch {
            try {
                val nurses = SupabaseManager.client
                    .from("nurses")
                    .select {
                        filter {
                            eq("user_id", user.id)
                        }
                    }
                    .decodeList<NurseLoginRecord>()

                if (nurses.isEmpty()) {
                    showCreateNurseScreen()
                    return@launch
                }

                val nurse = nurses.first()

                if (!sessionPrefs.getBoolean("password_configured", false)) {
                    showSetPasswordScreen()
                    return@launch
                }

                if (nurse.is_verified == true) {
                    openNurseHome()
                } else {
                    showPendingVerification(nurse)
                }

            } catch (e: Exception) {
                showError(
                    "تعذر قراءة بيانات الممرض",
                    e.message
                        ?: "حدث خطأ أثناء الاتصال بقاعدة البيانات."
                )
            }
        }
    }

    private fun showNewNurseRegistrationScreen() {
        isNewNurseRegistration = true

        val root = rootLayout()

        root.addView(makeText("✚", 44f, NAVY))
        root.addView(
            makeText(
                "تسجيل ممرض جديد",
                24f,
                NAVY,
                true
            )
        )
        root.addView(
            makeText(
                "أدخل بياناتك مرة واحدة، ثم أكد رقم الهاتف.",
                15f,
                GRAY
            )
        )

        addSpace(root, 14)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(WHITE, 22)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        card.addView(makeText("بيانات الممرض", 19f, NAVY, true))
        addSpace(card, 8)

        val fullName = field("الاسم الكامل")
        card.addView(fullName, LinearLayout.LayoutParams(-1, dp(52)))

        addSpace(card, 9)

        val specialty = field("التخصص: ممرض عام / طوارئ / أطفال...")
        card.addView(specialty, LinearLayout.LayoutParams(-1, dp(52)))

        addSpace(card, 9)

        val experience = field(
            "سنوات الخبرة",
            InputType.TYPE_CLASS_NUMBER
        )
        card.addView(experience, LinearLayout.LayoutParams(-1, dp(52)))

        addSpace(card, 9)

        val city = field("المدينة / القضاء")
        card.addView(city, LinearLayout.LayoutParams(-1, dp(52)))

        addSpace(card, 9)

        val address = field("العنوان / المنطقة / الشارع")
        card.addView(address, LinearLayout.LayoutParams(-1, dp(52)))

        addSpace(card, 9)

        val phone = field(
            "رقم الهاتف 07810056006",
            InputType.TYPE_CLASS_PHONE
        ).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        card.addView(phone, LinearLayout.LayoutParams(-1, dp(52)))

        addSpace(card, 14)

        card.addView(
            primaryButton("📲 متابعة وتأكيد رقم الهاتف") {
                val nameValue = fullName.text.toString().trim()
                val specialtyValue = specialty.text.toString().trim()
                val experienceValue = experience.text.toString().trim()
                val cityValue = city.text.toString().trim()
                val addressValue = address.text.toString().trim()
                val normalizedPhone = normalizeIraqPhone(phone.text.toString())

                if (nameValue.isBlank()) {
                    fullName.error = "أدخل الاسم الكامل"
                    return@primaryButton
                }

                if (specialtyValue.isBlank()) {
                    specialty.error = "أدخل التخصص"
                    return@primaryButton
                }

                val experienceNumber = experienceValue.toIntOrNull()
                if (experienceNumber == null || experienceNumber < 0) {
                    experience.error = "أدخل سنوات الخبرة بشكل صحيح"
                    return@primaryButton
                }

                if (cityValue.isBlank()) {
                    city.error = "أدخل المدينة / القضاء"
                    return@primaryButton
                }

                if (addressValue.isBlank()) {
                    address.error = "أدخل العنوان"
                    return@primaryButton
                }

                if (normalizedPhone == null) {
                    phone.error = "رقم الهاتف العراقي غير صحيح"
                    return@primaryButton
                }

                pendingFullName = nameValue
                pendingSpecialty = specialtyValue
                pendingExperienceYears = experienceNumber
                pendingCity = cityValue
                pendingAddress = addressValue
                phoneNumber = normalizedPhone
                isNewNurseRegistration = true

                sendOtp()
            },
            LinearLayout.LayoutParams(-1, dp(54))
        )

        root.addView(card, LinearLayout.LayoutParams(-1, -2))

        addSpace(root, 12)

        root.addView(
            outlineButton("العودة إلى دخول الممرضين") {
                isNewNurseRegistration = false
                showPhoneScreen()
            },
            LinearLayout.LayoutParams(-1, dp(56))
        )

        setContentView(scroll(root))
    }

    private fun showCreateNurseScreen() {
        val root = rootLayout()

        root.addView(makeText("✚", 44f, NAVY))
        root.addView(
            makeText(
                "استكمال بيانات الممرض",
                24f,
                NAVY,
                true
            )
        )
        root.addView(makeText("أدخل بياناتك المهنية", 16f, GRAY))

        addSpace(root, 10)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(WHITE, 22)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        card.addView(makeText("الاسم الكامل", 17f, NAVY, true))
        val fullName = field("مثال: أحمد محمد علي")
        card.addView(fullName, LinearLayout.LayoutParams(-1, dp(54)))

        addSpace(card, 12)

        card.addView(makeText("التخصص", 17f, NAVY, true))
        val specialty = field("ممرض عام / طوارئ / أطفال...")
        card.addView(specialty, LinearLayout.LayoutParams(-1, dp(54)))

        addSpace(card, 12)

        card.addView(makeText("سنوات الخبرة", 17f, NAVY, true))
        val experience = field(
            "مثال: 5",
            InputType.TYPE_CLASS_NUMBER
        )
        card.addView(experience, LinearLayout.LayoutParams(-1, dp(54)))

        addSpace(card, 12)

        card.addView(makeText("المحافظة / المدينة", 17f, NAVY, true))
        val city = field("الأنبار")
        card.addView(city, LinearLayout.LayoutParams(-1, dp(54)))

        addSpace(card, 12)

        card.addView(makeText("العنوان", 17f, NAVY, true))
        val address = field("الحي / المنطقة / الشارع")
        card.addView(address, LinearLayout.LayoutParams(-1, dp(54)))

        addSpace(card, 18)

        card.addView(
            primaryButton("إنشاء حساب الممرض") {
                val nameValue = fullName.text.toString().trim()
                val specialtyValue = specialty.text.toString().trim()
                val experienceValue = experience.text.toString().trim()
                val cityValue = city.text.toString().trim()
                val addressValue = address.text.toString().trim()

                if (nameValue.isBlank()) {
                    fullName.error = "أدخل الاسم الكامل"
                    return@primaryButton
                }

                if (specialtyValue.isBlank()) {
                    specialty.error = "أدخل التخصص"
                    return@primaryButton
                }

                if (experienceValue.isBlank()) {
                    experience.error = "أدخل سنوات الخبرة"
                    return@primaryButton
                }

                if (cityValue.isBlank()) {
                    city.error = "أدخل المدينة"
                    return@primaryButton
                }

                if (addressValue.isBlank()) {
                    address.error = "أدخل العنوان"
                    return@primaryButton
                }

                val experienceNumber = experienceValue.toIntOrNull()

                if (experienceNumber == null || experienceNumber < 0) {
                    experience.error = "أدخل عدد سنوات صحيح"
                    return@primaryButton
                }

                createNurseAccount(
                    nameValue,
                    specialtyValue,
                    experienceNumber,
                    cityValue,
                    addressValue
                )
            },
            LinearLayout.LayoutParams(-1, dp(56))
        )

        root.addView(card, LinearLayout.LayoutParams(-1, -2))

        addSpace(root, 15)

        root.addView(
            outlineButton("تسجيل الخروج") {
                signOutAndShowPhone()
            },
            LinearLayout.LayoutParams(-1, dp(52))
        )

        setContentView(scroll(root))
    }

    private fun createNurseAccount(
        fullName: String,
        specialty: String,
        experienceYears: Int,
        city: String,
        address: String
    ) {
        val user = SupabaseManager.client.auth.currentUserOrNull()

        if (user == null) {
            showPhoneScreen()
            return
        }

        val phone = user.phone ?: phoneNumber

        if (phone.isBlank()) {
            showError(
                "رقم الهاتف غير موجود",
                "تعذر الحصول على رقم الهاتف من حساب Supabase."
            )
            return
        }

        val loading = ProgressDialog(this).apply {
            setMessage("جاري إنشاء حساب الممرض...")
            setCancelable(false)
            show()
        }

        scope.launch {
            try {
                // قد يكون للمستخدم سجل ممرض موجود مسبقاً في nurses.
                // لا نحاول INSERT مرة ثانية لأن user_id عليه قيد UNIQUE.
                val existing = SupabaseManager.client
                    .from("nurses")
                    .select {
                        filter {
                            eq("user_id", user.id)
                        }
                    }
                    .decodeList<NurseLoginRecord>()

                if (existing.isNotEmpty()) {
                    val oldNurse = existing.first()

                    // إذا كان الحساب معتمداً بالفعل، لا نسمح بتسجيل حساب جديد فوقه.
                    if (oldNurse.is_verified == true) {
                        loading.dismiss()
                        isNewNurseRegistration = false
                        showError(
                            "الحساب موجود ومعتمد",
                            "هذا الرقم مرتبط بحساب ممرض معتمد بالفعل. استخدم «دخول الممرضين» بدلاً من إنشاء حساب جديد."
                        )
                        return@launch
                    }

                    // الحساب موجود لكنه غير معتمد:
                    // نحدّث بياناته بدلاً من إنشاء صف جديد.
                    SupabaseManager.client
                        .from("nurses")
                        .update(
                            mapOf(
                                "full_name" to fullName,
                                "phone" to phone,
                                "specialty" to specialty,
                                "experience_years" to experienceYears,
                                "city" to city,
                                "address" to address,
                                "is_available" to true,
                                "is_verified" to false
                            )
                        ) {
                            filter {
                                eq("user_id", user.id)
                            }
                        }

                    loading.dismiss()
                    showAccountCreatedDialog()
                    return@launch
                }

                // لا يوجد سجل سابق، لذلك ننشئ سجل الممرض الجديد.
                val record = NurseCreateRecord(
                    user_id = user.id,
                    full_name = fullName,
                    phone = phone,
                    specialty = specialty,
                    experience_years = experienceYears,
                    city = city,
                    address = address,
                    is_available = true,
                    is_verified = false
                )

                SupabaseManager.client
                    .from("nurses")
                    .insert(record)

                loading.dismiss()
                showAccountCreatedDialog()

            } catch (e: Exception) {
                loading.dismiss()

                showError(
                    "تعذر إنشاء الحساب",
                    e.message ?: "حدث خطأ أثناء حفظ بيانات الممرض."
                )
            }
        }
    }

    private fun showAccountCreatedDialog() {
        AlertDialog.Builder(this)
            .setTitle("تم إنشاء الحساب")
            .setMessage(
                "تم تسجيل بياناتك بنجاح.\n\n" +
                    "الحساب الآن بانتظار اعتماد الإدارة كممرض معتمد."
            )
            .setPositiveButton("حسناً") { _, _ ->
                showPendingScreen()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPendingVerification(
        nurse: NurseLoginRecord
    ) {
        showPendingScreen()
    }

    private fun showPendingScreen() {
        val root = rootLayout()

        root.addView(makeText("⏳", 46f, NAVY))
        root.addView(
            makeText(
                "الحساب بانتظار الاعتماد",
                26f,
                NAVY,
                true
            )
        )

        addSpace(root, 10)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(WHITE, 22)
            setPadding(dp(14), dp(16), dp(14), dp(16))
        }

        card.addView(
            makeText(
                "تم إنشاء حسابك بنجاح.",
                18f,
                NAVY,
                true
            )
        )

        card.addView(
            makeText(
                "سيتمكن الممرض من استقبال الطلبات بعد اعتماد الحساب من الإدارة.",
                14f,
                TEXT,
                false
            )
        )

        root.addView(card, LinearLayout.LayoutParams(-1, -2))

        addSpace(root, 10)

        root.addView(
            outlineButton("إعادة فحص حالة الحساب") {
                checkNurseAndContinue()
            },
            LinearLayout.LayoutParams(-1, dp(54))
        )

        addSpace(root, 12)

        root.addView(
            outlineButton("تسجيل الخروج") {
                signOutAndShowPhone()
            },
            LinearLayout.LayoutParams(-1, dp(54))
        )

        setContentView(scroll(root))
    }

    private fun openNurseHome() {
        sessionPrefs.edit().putString("role", "nurse").apply()
        FcmTokenManager.registerToken("nurse")
        val intent = Intent(this, NurseActivity::class.java)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun signOutAndShowPhone() {
        scope.launch {
            try {
                SupabaseManager.client.auth.signOut()
            } catch (_: Exception) {
            }

            phoneNumber = ""
            sessionPrefs.edit().remove("role").remove("password_configured").apply()
            showPhoneScreen()
        }
    }

    private fun showError(
        title: String,
        message: String
    ) {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show()
    }
}
