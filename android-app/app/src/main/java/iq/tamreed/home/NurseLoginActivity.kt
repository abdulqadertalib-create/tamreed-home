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
import io.github.jan.supabase.postgrest.from
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
                "الدخول مقصور على الممرضين المسجلين والمعتمدين.",
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

        setContentView(
            scroll(root)
        )
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

                /*
                 * مهم جداً:
                 * جدول nurses عندك يحتوي user_id
                 * لذلك نبحث باستخدام user_id
                 * وليس id.
                 */

                val nurses =
                    SupabaseManager.client
                        .from("nurses")
                        .select {
                            filter {
                                eq(
                                    "user_id",
                                    user.id
                                )
                                eq(
                                    "is_verified",
                                    true
                                )
                            }
                        }
                        .decodeList<NurseLoginRecord>()

                loading.dismiss()

                if (nurses.isEmpty()) {

                    showError(
                        "الدخول غير مصرح",
                        "هذا الرقم غير مسجل كممرض معتمد في النظام."
                    )

                    return@launch
                }

                val nurse =
                    nurses.first()

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
                    nurse.user_id ?: user.id
                )

                intent.putExtra(
                    "nurse_name",
                    nurse.full_name ?: "الممرض"
                )

                startActivity(intent)

                finish()

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

    private fun showError(
        title: String,
        message: String
    ) {

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
