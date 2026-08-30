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


// ============================================================
// بيانات الممرض الموجودة في جدول nurses
// ============================================================

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
    val is_available: Boolean? = true,
    val is_verified: Boolean? = false,
    val active: Boolean? = true
)


// ============================================================
// بيانات إنشاء ممرض جديد
// ============================================================

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


// ============================================================
// شاشة تسجيل الممرض
// ============================================================

class NurseLoginActivity : AppCompatActivity() {

    private val NAVY =
        Color.rgb(5, 62, 105)

    private val BLUE =
        Color.rgb(31, 115, 176)

    private val LIGHT_BLUE =
        Color.rgb(235, 245, 251)

    private val TEXT =
        Color.rgb(45, 45, 45)

    private val GRAY =
        Color.rgb(120, 120, 120)

    private val LIGHT_GRAY =
        Color.rgb(247, 248, 249)

    private val WHITE =
        Color.WHITE

    private val BORDER =
        Color.rgb(218, 224, 229)


    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )


    private var phoneNumber =
        ""


    // ========================================================
    // دورة حياة الشاشة
    // ========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val user =
            SupabaseManager.client.auth
                .currentUserOrNull()

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


    // ========================================================
    // أدوات الواجهة
    // ========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }


    private fun rounded(
        color: Int,
        radius: Int = 18
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                dp(radius).toFloat()
        }
    }


    private fun bordered(
        color: Int = WHITE,
        strokeColor: Int = BORDER,
        radius: Int = 16
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            setStroke(
                dp(1),
                strokeColor
            )

            cornerRadius =
                dp(radius).toFloat()
        }
    }


    private fun rootLayout():
        LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.TOP or
                    Gravity.CENTER_HORIZONTAL

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            setBackgroundColor(
                LIGHT_GRAY
            )

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

            setBackgroundColor(
                LIGHT_GRAY
            )

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

            gravity =
                Gravity.CENTER

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            if (bold) {

                setTypeface(
                    null,
                    Typeface.BOLD
                )
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

            gravity =
                Gravity.CENTER

            background =
                rounded(
                    NAVY,
                    15
                )

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

            gravity =
                Gravity.CENTER

            background =
                bordered(
                    WHITE,
                    NAVY,
                    15
                )

            setOnClickListener {

                action()
            }
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


    // ========================================================
    // شاشة رقم الهاتف
    // ========================================================

    private fun showPhoneScreen() {

        val root =
            rootLayout()


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


        addSpace(
            root,
            25
        )


        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                background =
                    rounded(
                        WHITE,
                        20
                    )

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


        addSpace(
            card,
            10
        )


        val phone =
            EditText(this).apply {

                hint =
                    "07810056006"

                textSize =
                    19f

                gravity =
                    Gravity.CENTER

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


        addSpace(
            card,
            12
        )


        card.addView(

            primaryButton(
                "إرسال رمز التحقق"
            ) {

                val normalized =
                    normalizeIraqPhone(
                        phone.text
                            .toString()
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


        addSpace(
            root,
            18
        )


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


        addSpace(
            root,
            15
        )


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


    // ========================================================
    // توحيد رقم الهاتف العراقي
    // ========================================================

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


        if (
            phone.startsWith("+964")
        ) {

            return if (
                phone.length == 14 &&
                phone.getOrNull(4) == '7'
            ) {

                phone

            } else {

                null
            }
        }


        if (
            phone.startsWith("00964")
        ) {

            phone =
                "+" +
                    phone.substring(2)


            return if (
                phone.length == 14 &&
                phone.getOrNull(4) == '7'
            ) {

                phone

            } else {

                null
            }
        }


        if (
            phone.startsWith("07")
        ) {

            phone =
                "+964" +
                    phone.substring(1)


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


    // ========================================================
    // إرسال OTP
    // ========================================================

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

                SupabaseManager
                    .client
                    .auth
                    .signInWith(OTP) {

                        phone =
                            phoneNumber
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


    // ========================================================
    // شاشة OTP
    // ========================================================

    private fun showOtpScreen() {

        val root =
            rootLayout()


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


        addSpace(
            root,
            25
        )


        val code =
            EditText(this).apply {

                hint =
                    "000000"

                textSize =
                    28f

                gravity =
                    Gravity.CENTER

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR

                maxLines =
                    1

                filters =
                    arrayOf(
                        InputFilter.LengthFilter(
                            6
                        )
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


        addSpace(
            root,
            18
        )


        root.addView(

            primaryButton(
                "تأكيد الرمز"
            ) {

                val value =
                    code.text
                        .toString()
                        .trim()


                if (
                    value.length != 6
                ) {

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


        addSpace(
            root,
            10
        )


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


        addSpace(
            root,
            10
        )


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


    // ========================================================
    // التحقق من OTP
    // ========================================================

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

                SupabaseManager
                    .client
                    .auth
                    .verifyPhoneOtp(

                        type =
                            OtpType.Phone.SMS,

                        phone =
                            phoneNumber,

                        token =
                            code
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


    // ========================================================
    // فحص حساب الممرض
    //
    // موجود ومعتمد
    //       ↓
    //     دخول
    //
    // موجود وغير معتمد
    //       ↓
    // انتظار اعتماد
    //
    // غير موجود
    //       ↓
    // إنشاء حساب جديد
    // ========================================================

    private fun checkNurseAndContinue() {

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()


        if (user == null) {

            showPhoneScreen()

            return
        }


        val loading =
            ProgressDialog(this).apply {

                setMessage(
                    "جاري التحقق من حساب الممرض..."
                )

                setCancelable(false)

                show()
            }


        scope.launch {

            try {

                /*
                 * مهم:
                 *
                 * لا نستخدم:
                 *
                 * select {
                 *     filter {
                 *         eq(...)
                 *     }
                 * }
                 *
                 * لأن هذه الصيغة سببت أخطاء
                 * Build في مشروعك.
                 *
                 * نقرأ البيانات ثم نبحث
                 * داخل Kotlin.
                 */

                val nurses =
                    SupabaseManager
                        .client
                        .from("nurses")
                        .select()
                        .decodeList<NurseLoginRecord>()


                val nurse =
                    nurses.firstOrNull {

                        it.user_id == user.id
                    }


                loading.dismiss()


                // =================================================
                // الممرض موجود
                // =================================================

                if (nurse != null) {

                    if (
                        nurse.is_verified == true
                    ) {

                        openNurseHome()

                    } else {

                        showPendingApproval()
                    }

                    return@launch
                }


                // =================================================
                // لا يوجد حساب ممرض
                // فتح إنشاء حساب جديد
                // =================================================

                showNurseRegistrationScreen()

            } catch (e: Exception) {

                loading.dismiss()


                /*
                 * إذا فشل SELECT بسبب RLS أو إعدادات الجدول،
                 * لا نغلق الحساب.
                 *
                 * نعرض شاشة إنشاء الحساب.
                 */

                showNurseRegistrationScreen()
            }
        }
    }


    // ========================================================
    // شاشة إنشاء حساب ممرض جديد
    // ========================================================

    private fun showNurseRegistrationScreen() {

        val root =
            rootLayout()


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
                28f,
                NAVY,
                true
            )
        )


        root.addView(
            text(
                "أكمل بياناتك لإرسال طلب اعتماد إلى الإدارة",
                15f,
                GRAY
            )
        )


        addSpace(
            root,
            20
        )


        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_HORIZONTAL

                background =
                    rounded(
                        WHITE,
                        20
                    )

                setPadding(
                    dp(18),
                    dp(20),
                    dp(18),
                    dp(25)
                )
            }


        // =====================================================
        // الاسم
        // =====================================================

        card.addView(
            text(
                "الاسم الكامل",
                16f,
                NAVY,
                true
            )
        )


        val fullName =
            createField(
                "اكتب الاسم الكامل"
            )


        card.addView(
            fullName,
            fieldParams()
        )


        addSpace(
            card,
            12
        )


        // =====================================================
        // الاختصاص
        // =====================================================

        card.addView(
            text(
                "الاختصاص",
                16f,
                NAVY,
                true
            )
        )


        val specialty =
            createField(
                "مثال: تمريض عام"
            )


        card.addView(
            specialty,
            fieldParams()
        )


        addSpace(
            card,
            12
        )


        // =====================================================
        // سنوات الخبرة
        // =====================================================

        card.addView(
            text(
                "سنوات الخبرة",
                16f,
                NAVY,
                true
            )
        )


        val experience =
            createField(
                "مثال: 5"
            )


        experience.inputType =
            InputType.TYPE_CLASS_NUMBER


        card.addView(
            experience,
            fieldParams()
        )


        addSpace(
            card,
            12
        )


        // =====================================================
        // المدينة
        // =====================================================

        card.addView(
            text(
                "المدينة",
                16f,
                NAVY,
                true
            )
        )


        val city =
            createField(
                "مثال: الفلوجة"
            )


        card.addView(
            city,
            fieldParams()
        )


        addSpace(
            card,
            12
        )


        // =====================================================
        // العنوان
        // =====================================================

        card.addView(
            text(
                "العنوان",
                16f,
                NAVY,
                true
            )
        )


        val address =
            createField(
                "الحي / المنطقة / الشارع"
            )


        card.addView(
            address,
            fieldParams()
        )


        addSpace(
            card,
            15
        )


        // =====================================================
        // رقم الهاتف
        // =====================================================

        card.addView(
            text(
                "رقم الهاتف المؤكد",
                16f,
                NAVY,
                true
            )
        )


        card.addView(
            text(
                phoneNumber,
                18f,
                BLUE,
                true
            )
        )


        addSpace(
            card,
            20
        )


        // =====================================================
        // إنشاء الحساب
        // =====================================================

        card.addView(

            primaryButton(
                "إنشاء حساب الممرض"
            ) {

                val nameValue =
                    fullName.text
                        .toString()
                        .trim()


                val specialtyValue =
                    specialty.text
                        .toString()
                        .trim()


                val experienceValue =
                    experience.text
                        .toString()
                        .trim()


                val cityValue =
                    city.text
                        .toString()
                        .trim()


                val addressValue =
                    address.text
                        .toString()
                        .trim()


                if (
                    nameValue.length < 3
                ) {

                    fullName.error =
                        "أدخل الاسم الكامل"

                    return@primaryButton
                }


                if (
                    specialtyValue.isEmpty()
                ) {

                    specialty.error =
                        "أدخل الاختصاص"

                    return@primaryButton
                }


                val years =
                    experienceValue
                        .toIntOrNull()


                if (
                    years == null ||
                    years < 0 ||
                    years > 60
                ) {

                    experience.error =
                        "أدخل سنوات خبرة صحيحة"

                    return@primaryButton
                }


                if (
                    cityValue.isEmpty()
                ) {

                    city.error =
                        "أدخل المدينة"

                    return@primaryButton
                }


                if (
                    addressValue.isEmpty()
                ) {

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
                dp(62)
            )
        )


        addSpace(
            card,
            10
        )


        card.addView(

            outlineButton(
                "رجوع"
            ) {

                showPhoneScreen()
            },

            LinearLayout.LayoutParams(
                -1,
                dp(58)
            )
        )


        root.addView(
            card,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )


        setContentView(
            scroll(root)
        )
    }


    // ========================================================
    // إنشاء EditText
    // ========================================================

    private fun createField(
        hintText: String
    ): EditText {

        return EditText(this).apply {

            hint =
                hintText

            textSize =
                16f

            setTextColor(
                TEXT
            )

            setHintTextColor(
                GRAY
            )

            gravity =
                Gravity.RIGHT or
                    Gravity.CENTER_VERTICAL

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

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

            maxLines =
                1
        }
    }


    private fun fieldParams():
        LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            -1,
            dp(60)
        )
    }


    // ========================================================
    // إنشاء حساب الممرض في Supabase
    // ========================================================

    private fun createNurseAccount(

        fullName: String,

        specialty: String,

        experienceYears: Int,

        city: String,

        address: String

    ) {

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()


        if (user == null) {

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

                /*
                 * أولاً نقرأ جدول nurses.
                 *
                 * لا نستخدم filter / eq.
                 */

                val allNurses =
                    SupabaseManager
                        .client
                        .from("nurses")
                        .select()
                        .decodeList<NurseLoginRecord>()


                val existing =
                    allNurses.firstOrNull {

                        it.user_id == user.id
                    }


                if (existing != null) {

                    loading.dismiss()


                    if (
                        existing.is_verified == true
                    ) {

                        openNurseHome()

                    } else {

                        showPendingApproval()
                    }


                    return@launch
                }


                // =================================================
                // بيانات الحساب الجديد
                // =================================================

                val record =
                    NurseCreateRecord(

                        user_id =
                            user.id,

                        full_name =
                            fullName,

                        phone =
                            phoneNumber,

                        specialty =
                            specialty,

                        experience_years =
                            experienceYears,

                        city =
                            city,

                        address =
                            address,

                        is_available =
                            true,

                        is_verified =
                            false
                    )


                // =================================================
                // إدخال الحساب
                // =================================================

                SupabaseManager
                    .client
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


    // ========================================================
    // نجاح إنشاء الحساب
    // ========================================================

    private fun showSuccessAndWait() {

        AlertDialog.Builder(this)

            .setTitle(
                "تم إرسال طلب التسجيل ✅"
            )

            .setMessage(

                "تم إنشاء حساب الممرض بنجاح.\n\n" +

                    "الحساب الآن بانتظار اعتماد الإدارة.\n\n" +

                    "بعد اعتماد الحساب يمكنك تسجيل الدخول واستخدام لوحة الممرض."
            )

            .setCancelable(false)

            .setPositiveButton(
                "حسناً"
            ) {

                _,
                _ ->

                try {

                    SupabaseManager
                        .client
                        .auth
                        .signOut()

                } catch (_: Exception) {
                }


                finish()
            }

            .show()
    }


    // ========================================================
    // حساب موجود لكنه بانتظار الاعتماد
    // ========================================================

    private fun showPendingApproval() {

        AlertDialog.Builder(this)

            .setTitle(
                "الحساب بانتظار الاعتماد"
            )

            .setMessage(

                "حساب الممرض موجود بالفعل، " +

                    "لكنه لم يعتمد من الإدارة بعد.\n\n" +

                    "يرجى الانتظار حتى تتم مراجعة بياناتك."
            )

            .setPositiveButton(
                "حسناً"
            ) {

                _,
                _ ->

                try {

                    SupabaseManager
                        .client
                        .auth
                        .signOut()

                } catch (_: Exception) {
                }


                finish()
            }

            .show()
    }


    // ========================================================
    // فتح لوحة الممرض
    // ========================================================

    private fun openNurseHome() {

        try {

            startActivity(

                Intent(
                    this,
                    NurseActivity::class.java
                )
            )

            finish()

        } catch (e: Exception) {

            showError(

                "تعذر فتح لوحة الممرض",

                e.message
                    ?: "تأكد من وجود NurseActivity في المشروع."
            )
        }
    }


    // ========================================================
    // عرض الأخطاء
    // ========================================================

    private fun showError(

        title: String,

        message: String

    ) {

        if (isFinishing) {

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
}
