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
import io.github.jan.supabase.auth.OtpType
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
    val is_available: Boolean? = false,
    val is_verified: Boolean? = false
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
// شاشة تسجيل / إنشاء حساب الممرض
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

    private val GREEN =
        Color.rgb(35, 145, 85)

    private val RED =
        Color.rgb(180, 50, 50)


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
            SupabaseManager
                .client
                .auth
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


    private fun rootLayout(): LinearLayout {

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
                dp(18),
                dp(14),
                dp(60)
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


    private fun makeText(
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
                    16
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
                    16
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


    private fun field(
        hintText: String,
        inputTypeValue: Int =
            InputType.TYPE_CLASS_TEXT
    ): EditText {

        return EditText(this).apply {

            hint =
                hintText

            textSize =
                17f

            gravity =
                Gravity.CENTER

            inputType =
                inputTypeValue

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


    // ========================================================
    // شاشة الهاتف
    // ========================================================

    private fun showPhoneScreen() {

        val root =
            rootLayout()


        root.addView(
            makeText(
                "🩺",
                58f,
                NAVY
            )
        )


        root.addView(
            makeText(
                "دخول الممرض",
                30f,
                NAVY,
                true
            )
        )


        root.addView(
            makeText(
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
                        22
                    )

                setPadding(
                    dp(18),
                    dp(25),
                    dp(18),
                    dp(25)
                )
            }


        card.addView(
            makeText(
                "📱",
                45f,
                NAVY
            )
        )


        card.addView(
            makeText(
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
            makeText(
                "سيتم إرسال رمز تحقق SMS إلى الرقم",
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
                        "رقم الهاتف العراقي غير صحيح"

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

            makeText(
                "👨‍⚕️",
                30f,
                NAVY
            ),

            LinearLayout.LayoutParams(
                dp(55),
                dp(55)
            )
        )


        info.addView(

            makeText(
                "ممرض جديد؟ بعد تأكيد رقم الهاتف ستتمكن من إنشاء حسابك وإدخال بياناتك المهنية.",
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
            18
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
                phone.length == 14 &&
                phone.getOrNull(4) == '7'
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
                    "تعذر إرسال رمز التحقق",
                    e.message
                        ?: "تأكد من إعداد SMS في Supabase."
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
            makeText(
                "🔐",
                55f,
                NAVY
            )
        )


        root.addView(
            makeText(
                "تأكيد رقم الممرض",
                28f,
                NAVY,
                true
            )
        )


        root.addView(
            makeText(
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

                val token =
                    code.text
                        .toString()
                        .trim()

                if (
                    token.length != 6
                ) {

                    code.error =
                        "أدخل رمز التحقق المكون من 6 أرقام"

                    return@primaryButton
                }


                verifyOtp(
                    token
                )
            },

            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )


        addSpace(
            root,
            12
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


        addSpace(
            root,
            20
        )


        root.addView(
            makeText(
                "أدخل الرمز الذي وصلك برسالة SMS",
                14f,
                GRAY
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
        token: String
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
                        type = OtpType.Phone.SMS,
                        phoneNumber = phoneNumber,
                        token = token
                    )


                loading.dismiss()


                checkNurseAndContinue()

            } catch (e: Exception) {

                loading.dismiss()

                showError(
                    "رمز التحقق غير صحيح",
                    e.message
                        ?: "تأكد من الرمز ثم حاول مرة أخرى."
                )
            }
        }
    }


    // ========================================================
    // فحص حساب الممرض
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


        scope.launch {

            try {

                val nurses =
                    SupabaseManager
                        .client
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


                if (
                    nurses.isEmpty()
                ) {

                    phoneNumber =
                        user.phone
                            ?: phoneNumber

                    showCreateNurseScreen()

                    return@launch
                }


                val nurse =
                    nurses.first()


                if (
                    nurse.is_verified == true
                ) {

                    openNurseHome()

                } else {

                    showPendingVerification(
                        nurse
                    )
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


    // ========================================================
    // إنشاء حساب ممرض جديد
    // ========================================================

    private fun showCreateNurseScreen() {

        val root =
            rootLayout()


        root.addView(
            makeText(
                "👨‍⚕️",
                55f,
                NAVY
            )
        )


        root.addView(
            makeText(
                "إنشاء حساب ممرض",
                28f,
                NAVY,
                true
            )
        )


        root.addView(
            makeText(
                "أدخل بياناتك المهنية",
                16f,
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

                background =
                    rounded(
                        WHITE,
                        22
                    )

                setPadding(
                    dp(18),
                    dp(22),
                    dp(18),
                    dp(22)
                )
            }


        card.addView(
            makeText(
                "الاسم الكامل",
                17f,
                NAVY,
                true
            )
        )


        val fullName =
            field(
                "مثال: أحمد محمد علي"
            )


        card.addView(
            fullName,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )


        addSpace(
            card,
            12
        )


        card.addView(
            makeText(
                "التخصص",
                17f,
                NAVY,
                true
            )
        )


        val specialty =
            field(
                "ممرض عام / طوارئ / أطفال..."
            )


        card.addView(
            specialty,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )


        addSpace(
            card,
            12
        )


        card.addView(
            makeText(
                "سنوات الخبرة",
                17f,
                NAVY,
                true
            )
        )


        val experience =
            field(
                "مثال: 5",
                InputType.TYPE_CLASS_NUMBER
            )


        card.addView(
            experience,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )


        addSpace(
            card,
            12
        )


        card.addView(
            makeText(
                "المحافظة / المدينة",
                17f,
                NAVY,
                true
            )
        )


        val city =
            field(
                "الأنبار"
            )


        card.addView(
            city,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )


        addSpace(
            card,
            12
        )


        card.addView(
            makeText(
                "العنوان",
                17f,
                NAVY,
                true
            )
        )


        val address =
            field(
                "الحي / المنطقة / الشارع"
            )


        card.addView(
            address,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )


        addSpace(
            card,
            18
        )


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
                    nameValue.isBlank()
                ) {

                    fullName.error =
                        "أدخل الاسم الكامل"

                    return@primaryButton
                }


                if (
                    specialtyValue.isBlank()
                ) {

                    specialty.error =
                        "أدخل التخصص"

                    return@primaryButton
                }


                if (
                    experienceValue.isBlank()
                ) {

                    experience.error =
                        "أدخل سنوات الخبرة"

                    return@primaryButton
                }


                if (
                    cityValue.isBlank()
                ) {

                    city.error =
                        "أدخل المدينة"

                    return@primaryButton
                }


                if (
                    addressValue.isBlank()
                ) {

                    address.error =
                        "أدخل العنوان"

                    return@primaryButton
                }


                val experienceNumber =
                    experienceValue.toIntOrNull()


                if (
                    experienceNumber == null ||
                    experienceNumber < 0
                ) {

                    experience.error =
                        "أدخل عدد سنوات صحيح"

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


        addSpace(
            root,
            15
        )


        root.addView(

            outlineButton(
                "تسجيل الخروج"
            ) {

                signOutAndShowPhone()
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
    // إنشاء سجل الممرض في جدول nurses
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


        val phone =
            user.phone
                ?: phoneNumber


        if (
            phone.isBlank()
        ) {

            showError(
                "رقم الهاتف غير موجود",
                "تعذر الحصول على رقم الهاتف من حساب Supabase."
            )

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

                val record =
                    NurseCreateRecord(

                        user_id =
                            user.id,

                        full_name =
                            fullName,

                        phone =
                            phone,

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


                SupabaseManager
                    .client
                    .from("nurses")
                    .insert(record)


                loading.dismiss()


                showAccountCreatedDialog()

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
    // بعد إنشاء الحساب
    // ========================================================

    private fun showAccountCreatedDialog() {

        AlertDialog.Builder(this)
            .setTitle(
                "تم إنشاء الحساب"
            )
            .setMessage(
                "تم تسجيل بياناتك بنجاح.\n\n" +
                    "الحساب الآن بانتظار اعتماد الإدارة كممرض معتمد."
            )
            .setPositiveButton(
                "حسناً"
            ) { _, _ ->

                showPendingScreen()
            }
            .setCancelable(false)
            .show()
    }


    // ========================================================
    // حساب موجود ولكنه غير معتمد
    // ========================================================

    private fun showPendingVerification(
        nurse: NurseLoginRecord
    ) {

        showPendingScreen()
    }


    private fun showPendingScreen() {

        val root =
            rootLayout()


        root.addView(
            makeText(
                "⏳",
                60f,
                NAVY
            )
        )


        root.addView(
            makeText(
                "الحساب بانتظار الاعتماد",
                26f,
                NAVY,
                true
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
                    Gravity.CENTER

                background =
                    rounded(
                        WHITE,
                        22
                    )

                setPadding(
                    dp(20),
                    dp(25),
                    dp(20),
                    dp(25)
                )
            }


        card.addView(
            makeText(
                "تم إنشاء حسابك بنجاح.",
                20f,
                NAVY,
                true
            )
        )


        card.addView(
            makeText(
                "سيتمكن الممرض من استقبال الطلبات بعد اعتماد الحساب من الإدارة.",
                16f,
                TEXT,
                false
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
            20
        )


        root.addView(

            outlineButton(
                "إعادة فحص حالة الحساب"
            ) {

                checkNurseAndContinue()
            },

            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )


        addSpace(
            root,
            12
        )


        root.addView(

            outlineButton(
                "تسجيل الخروج"
            ) {

                signOutAndShowPhone()
            },

            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )


        setContentView(
            scroll(root)
        )
    }


    // ========================================================
    // فتح واجهة الممرض
    // ========================================================

    private fun openNurseHome() {

        val intent =
            Intent(
                this,
                NurseActivity::class.java
            )


        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK


        startActivity(intent)

        finish()
    }


    // ========================================================
    // تسجيل الخروج بطريقة صحيحة
    // ========================================================

    private fun signOutAndShowPhone() {

        scope.launch {

            try {

                SupabaseManager
                    .client
                    .auth
                    .signOut()

            } catch (_: Exception) {

                // نتجاهل الخطأ حتى نعيد المستخدم لشاشة الهاتف
            }


            phoneNumber =
                ""


            showPhoneScreen()
        }
    }


    // ========================================================
    // رسالة الخطأ
    // ========================================================

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
}
