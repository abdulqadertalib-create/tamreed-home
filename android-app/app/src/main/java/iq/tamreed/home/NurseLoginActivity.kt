package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
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
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class NurseLoginRecord(
    val id: String,
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val specialty: String? = null,
    val experience_years: Int? = null,
    val city: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_available: Boolean? = true,
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
        color: Int = WHITE,
        strokeColor: Int = BORDER,
        radius: Int = 16
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
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

            setPadding(
                dp(6),
                dp(6),
                dp(6),
                dp(6)
            )
        }
    }

    private fun primaryButton(
        title: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {
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
    }

    private fun outlineButton(
        title: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {
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
    }

    private fun rootLayout(): LinearLayout {

        return LinearLayout(this).apply {
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
    }

    private fun scroll(
        view: View
    ): ScrollView {

        return ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(LIGHT_GRAY)
            addView(view)
        }
    }

    private fun space(
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

    // ---------------------------------------------------------
    // شاشة رقم الهاتف
    // ---------------------------------------------------------

    private fun showPhoneScreen() {

        val root = rootLayout()

        root.addView(
            text(
                "🩺",
                55f,
                NAVY
            )
        )

        root.addView(
            text(
                "دخول الممرض",
                30f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "منصة التمريض المنزلي - الأنبار",
                16f,
                GRAY
            )
        )

        space(root, 25)

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                background =
                    rounded(WHITE, 20)

                setPadding(
                    dp(18),
                    dp(25),
                    dp(18),
                    dp(25)
                )
            }

        card.addView(
            text(
                "📱",
                45f,
                NAVY
            )
        )

        card.addView(
            text(
                "رقم هاتف الممرض",
                20f,
                NAVY,
                true
            )
        )

        space(card, 10)

        val phone =
            EditText(this).apply {

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

        card.addView(
            phone,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        card.addView(
            text(
                "سيتم إرسال رمز تحقق SMS",
                13f,
                GRAY
            )
        )

        space(card, 12)

        card.addView(
            primaryButton(
                "إرسال رمز التحقق"
            ) {

                val normalized =
                    normalizeIraqPhone(
                        phone.text.toString()
                    )

                if (normalized == null) {

                    phone.error =
                        "رقم هاتف عراقي غير صحيح"

                    return@primaryButton
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
            card,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        space(root, 18)

        val info =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    rounded(
                        LIGHT_BLUE,
                        18
                    )

                setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(12)
                )
            }

        info.addView(
            text(
                "🔐",
                32f,
                NAVY
            ),
            LinearLayout.LayoutParams(
                dp(55),
                dp(55)
            )
        )

        info.addView(
            text(
                "إذا كنت ممرضًا جديدًا يمكنك إنشاء حساب بعد تأكيد رقم الهاتف.",
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

        root.addView(info)

        space(root, 15)

        root.addView(
            outlineButton(
                "العودة"
            ) {
                finish()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            )
        )

        setContentView(
            scroll(root)
        )
    }

    // ---------------------------------------------------------
    // توحيد رقم الهاتف العراقي
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // إرسال OTP
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // شاشة OTP
    // ---------------------------------------------------------

    private fun showOtpScreen() {

        val root = rootLayout()

        root.addView(
            text(
                "🔐",
                55f,
                NAVY
            )
        )

        root.addView(
            text(
                "تأكيد رقم الممرض",
                28f,
                NAVY,
                true
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

        space(root, 25)

        val code =
            EditText(this).apply {

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
            code,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        space(root, 18)

        root.addView(
            primaryButton(
                "تأكيد الرمز"
            ) {

                val value =
                    code.text.toString().trim()

                if (value.length != 6) {

                    code.error =
                        "أدخل 6 أرقام"

                    return@primaryButton
                }

                verifyOtp(value)
            },
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        space(root, 10)

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

        space(root, 10)

        root.addView(
            outlineButton(
                "تغيير رقم الهاتف"
            ) {
                showPhoneScreen()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            )
        )

        setContentView(
            scroll(root)
        )
    }

    // ---------------------------------------------------------
    // التحقق من OTP
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // بعد OTP:
    // موجود ومعتمد -> دخول
    // غير موجود -> إنشاء حساب
    // موجود وغير معتمد -> انتظار اعتماد
    // ---------------------------------------------------------

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
                    "جاري التحقق من بيانات الممرض..."
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
                                    "user_id",
                                    user.id
                                )
                            }
                        }
                        .decodeList<NurseLoginRecord>()

                loading.dismiss()

                // لا يوجد حساب ممرض بهذا المستخدم
                if (nurses.isEmpty()) {

                    showNurseRegistrationScreen()

                    return@launch
                }

                val nurse =
                    nurses.first()

                // الحساب موجود لكنه غير معتمد
                if (nurse.is_verified != true) {

                    showPendingApproval()

                    return@launch
                }

                // الحساب موجود ومعتمد
                openNurseHome(
                    nurse,
                    user.id
                )

            } catch (e: Exception) {

                loading.dismiss()

                showError(
                    "خطأ في قاعدة البيانات",
                    e.message
                        ?: "تعذر قراءة بيانات الممرض."
                )
            }
        }
    }

    // ---------------------------------------------------------
    // شاشة إنشاء حساب ممرض جديد
    // ---------------------------------------------------------

    private fun showNurseRegistrationScreen() {

        val root = rootLayout()

        root.addView(
            text(
                "🩺",
                55f,
                NAVY
            )
        )

        root.addView(
            text(
                "إنشاء حساب ممرض",
                29f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "أكمل بياناتك للتسجيل في منصة التمريض المنزلي",
                15f,
                GRAY
            )
        )

        space(root, 18)

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                background =
                    rounded(WHITE, 20)

                setPadding(
                    dp(18),
                    dp(20),
                    dp(18),
                    dp(20)
                )
            }

        card.addView(
            text(
                "بيانات الممرض",
                20f,
                NAVY,
                true
            )
        )

        space(card, 12)

        // الاسم الكامل
        card.addView(
            text(
                "الاسم الكامل",
                15f,
                NAVY,
                true
            )
        )

        val fullName =
            createField(
                "مثال: أحمد محمد علي",
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            )

        card.addView(
            fullName,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        space(card, 10)

        // الاختصاص
        card.addView(
            text(
                "الاختصاص",
                15f,
                NAVY,
                true
            )
        )

        val specialty =
            createField(
                "تمريض عام / طوارئ / عناية مركزة...",
                InputType.TYPE_CLASS_TEXT
            )

        card.addView(
            specialty,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        space(card, 10)

        // سنوات الخبرة
        card.addView(
            text(
                "سنوات الخبرة",
                15f,
                NAVY,
                true
            )
        )

        val experience =
            createField(
                "مثال: 5",
                InputType.TYPE_CLASS_NUMBER
            )

        card.addView(
            experience,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        space(card, 10)

        // المدينة
        card.addView(
            text(
                "المدينة",
                15f,
                NAVY,
                true
            )
        )

        val city =
            createField(
                "الفلوجة",
                InputType.TYPE_CLASS_TEXT
            )

        card.addView(
            city,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        space(card, 10)

        // العنوان
        card.addView(
            text(
                "العنوان",
                15f,
                NAVY,
                true
            )
        )

        val address =
            createField(
                "الحي / المنطقة / الشارع",
                InputType.TYPE_CLASS_TEXT
            )

        card.addView(
            address,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        space(card, 15)

        card.addView(
            text(
                "رقم الهاتف المؤكد",
                14f,
                GRAY,
                true
            )
        )

        card.addView(
            text(
                phoneNumber,
                18f,
                NAVY,
                true
            )
        )

        space(card, 15)

        card.addView(
            primaryButton(
                "إنشاء حساب الممرض"
            ) {

                val nameValue =
                    fullName.text.toString().trim()

                val specialtyValue =
                    specialty.text.toString().trim()

                val experienceValue =
                    experience.text.toString().trim()

                val cityValue =
                    city.text.toString().trim()

                val addressValue =
                    address.text.toString().trim()

                if (nameValue.length < 3) {

                    fullName.error =
                        "أدخل الاسم الكامل"

                    return@primaryButton
                }

                if (specialtyValue.isEmpty()) {

                    specialty.error =
                        "أدخل الاختصاص"

                    return@primaryButton
                }

                val years =
                    experienceValue.toIntOrNull()

                if (years == null ||
                    years < 0 ||
                    years > 60
                ) {

                    experience.error =
                        "أدخل سنوات خبرة صحيحة"

                    return@primaryButton
                }

                if (cityValue.isEmpty()) {

                    city.error =
                        "أدخل المدينة"

                    return@primaryButton
                }

                if (addressValue.isEmpty()) {

                    address.error =
                        "أدخل العنوان"

                    return@primaryButton
                }

                createNurseAccount(
                    nameValue,
                    specialtyValue,
                    years,
                    cityValue,
                    addressValue
                )
            },
            LinearLayout.LayoutParams(
                -1,
                dp(64)
            )
        )

        root.addView(
            card,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        space(root, 15)

        root.addView(
            text(
                "بعد إنشاء الحساب سيتم مراجعته من الإدارة قبل السماح باستقبال طلبات المرضى.",
                14f,
                GRAY,
                true
            )
        )

        setContentView(
            scroll(root)
        )
    }

    // ---------------------------------------------------------
    // إنشاء EditText
    // ---------------------------------------------------------

    private fun createField(
        hintText: String,
        type: Int
    ): EditText {

        return EditText(this).apply {

            hint = hintText

            textSize = 17f

            gravity = Gravity.CENTER

            inputType = type

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

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
    }

    // ---------------------------------------------------------
    // إنشاء حساب الممرض في Supabase
    // ---------------------------------------------------------

    private fun createNurseAccount(
        fullName: String,
        specialty: String,
        experienceYears: Int,
        city: String,
        address: String
    ) {

        val user =
            SupabaseManager.client.auth
                .currentUserOrNull()

        if (user == null) {

            showError(
                "انتهت الجلسة",
                "يرجى تسجيل الدخول مرة أخرى."
            )

            showPhoneScreen()
            return
        }

        val loading =
            ProgressDialog(this).apply {

                setMessage(
                    "جاري إنشاء حساب الممرض..."
                )

                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                val existing =
                    SupabaseManager.client
                        .from("nurses")
                        .select {
                            filter {
                                eq(
                                    "user_id",
                                    user.id
                                )
                            }
                        }
                        .decodeList<NurseLoginRecord>()

                if (existing.isNotEmpty()) {

                    loading.dismiss()

                    showPendingApproval()

                    return@launch
                }

                val record =
                    NurseCreateRecord(
                        user_id = user.id,
                        full_name = fullName,
                        phone = phoneNumber,
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

                showSuccessAndWait()

            } catch (e: Exception) {

                loading.dismiss()

                showError(
                    "تعذر إنشاء الحساب",
                    e.message
                        ?: "حدث خطأ أثناء حفظ بيانات الممرض."
                )
            }
        }
    }

    // ---------------------------------------------------------
    // الحساب بانتظار الاعتماد
    // ---------------------------------------------------------

    private fun showPendingApproval() {

        AlertDialog.Builder(this)
            .setTitle("الحساب بانتظار الاعتماد")
            .setMessage(
                "حساب الممرض موجود، لكنه لم يعتمد من الإدارة بعد.\n\n" +
                        "سيتمكن الممرض من الدخول بعد اعتماد الحساب."
            )
            .setPositiveButton(
                "حسنًا"
            ) { _, _ ->

                SupabaseManager.client.auth.signOut()

                showPhoneScreen()
            }
            .setCancelable(false)
            .show()
    }

    // ---------------------------------------------------------
    // نجاح إنشاء الحساب
    // ---------------------------------------------------------

    private fun showSuccessAndWait() {

        AlertDialog.Builder(this)
            .setTitle("تم إنشاء الحساب بنجاح ✅")
            .setMessage(
                "تم تسجيل بياناتك كممرض بنجاح.\n\n" +
                        "الحساب الآن بانتظار مراجعة واعتماد الإدارة.\n\n" +
                        "بعد اعتماد الحساب يمكنك تسجيل الدخول باستخدام رقم هاتفك."
            )
            .setPositiveButton(
                "حسنًا"
            ) { _, _ ->

                SupabaseManager.client.auth.signOut()

                showPhoneScreen()
            }
            .setCancelable(false)
            .show()
    }

    // ---------------------------------------------------------
    // فتح الصفحة الرئيسية للممرض
    // ---------------------------------------------------------

    private fun openNurseHome(
        nurse: NurseLoginRecord,
        userId: String
    ) {

        val intent =
            android.content.Intent(
                this@NurseLoginActivity,
                NurseActivity::class.java
            )

        intent.putExtra(
            "nurse_id",
            nurse.id
        )

        intent.putExtra(
            "nurse_user_id",
            nurse.user_id ?: userId
        )

        intent.putExtra(
            "nurse_name",
            nurse.full_name ?: "الممرض"
        )

        startActivity(intent)

        finish()
    }

    // ---------------------------------------------------------
    // رسائل الخطأ
    // ---------------------------------------------------------

    private fun showError(
        title: String,
        message: String
    ) {

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                "حسنًا",
                null
            )
            .show()
    }
}
