package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Color
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


class MainActivity : AppCompatActivity() {

    // =====================================================
    // الألوان
    // =====================================================

    private val BLUE = Color.rgb(0, 102, 204)
    private val DARK_BLUE = Color.rgb(0, 74, 150)
    private val LIGHT_BLUE = Color.rgb(235, 245, 255)
    private val TEXT = Color.rgb(35, 45, 55)
    private val GREEN = Color.rgb(30, 140, 80)
    private val RED = Color.rgb(190, 40, 40)

    // =====================================================
    // Coroutine
    // =====================================================

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    // =====================================================
    // بيانات المستخدم
    // =====================================================

    private var phoneNumber = ""

    // =====================================================
    // onCreate
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showHome()
    }

    // =====================================================
    // onDestroy
    // =====================================================

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // =====================================================
    // التخطيط الأساسي
    // =====================================================

    private fun baseLayout(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setBackgroundColor(Color.WHITE)

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            setPadding(
                24,
                35,
                24,
                20
            )
        }
    }

    // =====================================================
    // إنشاء النصوص
    // =====================================================

    private fun text(
        value: String,
        size: Float,
        color: Int = TEXT
    ): TextView {

        return TextView(this).apply {

            text = value

            textSize = size

            setTextColor(color)

            gravity = Gravity.CENTER

            setPadding(
                10,
                12,
                10,
                12
            )

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL
        }
    }

    // =====================================================
    // إنشاء الأزرار
    // =====================================================

    private fun button(
        value: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            text = value

            textSize = 17f

            setTextColor(Color.WHITE)

            setBackgroundColor(BLUE)

            isAllCaps = false

            setPadding(
                10,
                10,
                10,
                10
            )

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            setOnClickListener {
                action()
            }
        }
    }

    // =====================================================
    // الصفحة الرئيسية
    // =====================================================

    private fun showHome() {

        val root = baseLayout()

        // -------------------------------------------------
        // Header
        // -------------------------------------------------

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    10,
                    10,
                    10,
                    25
                )

                setBackgroundColor(
                    LIGHT_BLUE
                )
            }

        header.addView(
            text(
                "التمريض المنزلي",
                32f,
                DARK_BLUE
            )
        )

        header.addView(
            text(
                "خدمات التمريض والرعاية الصحية المنزلية",
                20f,
                TEXT
            )
        )

        header.addView(
            text(
                "محافظة الأنبار - العراق",
                17f,
                Color.DKGRAY
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // -------------------------------------------------
        // الترحيب
        // -------------------------------------------------

        root.addView(
            text(
                "مرحباً بك 👋\nاحصل على خدمة تمريض منزلية بسهولة وأمان",
                20f,
                DARK_BLUE
            )
        )

        // -------------------------------------------------
        // تسجيل الدخول
        // -------------------------------------------------

        root.addView(
            button(
                "📱 تسجيل الدخول برقم الهاتف"
            ) {
                showPhoneLogin()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    15,
                    0,
                    10
                )
            }
        )

        root.addView(
            text(
                "التسجيل مطلوب لإنشاء الطلبات ومتابعتها",
                15f,
                Color.GRAY
            )
        )

        // -------------------------------------------------
        // طلب ممرض
        // -------------------------------------------------

        root.addView(
            button(
                "🩺 طلب ممرض منزلي"
            ) {
                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    15,
                    0,
                    10
                )
            }
        )

        // -------------------------------------------------
        // الخدمات
        // -------------------------------------------------

        root.addView(
            button(
                "🏥 الخدمات التمريضية"
            ) {
                showServices()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    10
                )
            }
        )

        // -------------------------------------------------
        // الموقع
        // -------------------------------------------------

        root.addView(
            button(
                "📍 تحديد موقعي"
            ) {

                Toast.makeText(
                    this,
                    "سيتم تفعيل الخريطة وتحديد الموقع في المرحلة التالية",
                    Toast.LENGTH_SHORT
                ).show()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    10
                )
            }
        )

        // -------------------------------------------------
        // الطلبات
        // -------------------------------------------------

        root.addView(
            button(
                "📋 طلباتي السابقة"
            ) {
                showBookings()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    10
                )
            }
        )

        // -------------------------------------------------
        // التواصل
        // -------------------------------------------------

        root.addView(
            button(
                "☎️ تواصل معنا"
            ) {
                contactUs()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    10
                )
            }
        )

        root.addView(
            text(
                "خدمة التمريض المنزلي على مدار الساعة",
                15f,
                Color.GRAY
            )
        )

        setContentView(root)
    }

    // =====================================================
    // تسجيل الدخول برقم الهاتف
    // =====================================================

    private fun showPhoneLogin() {

        val root = baseLayout()

        root.addView(
            text(
                "تسجيل الدخول",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل رقم هاتفك وسنرسل لك رمز تحقق OTP",
                17f,
                Color.GRAY
            )
        )

        val phone =
            EditText(this).apply {

                hint =
                    "رقم الهاتف مثال: 07701234567"

                textSize = 18f

                inputType =
                    InputType.TYPE_CLASS_PHONE

                gravity =
                    Gravity.RIGHT or
                            Gravity.CENTER_VERTICAL

                setPadding(
                    20,
                    10,
                    20,
                    10
                )

                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR
            }

        root.addView(
            phone,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            ).apply {

                setMargins(
                    0,
                    25,
                    0,
                    15
                )
            }
        )

        root.addView(
            text(
                "استخدم رقم الهاتف العراقي مع مفتاح الدولة",
                14f,
                Color.GRAY
            )
        )

        root.addView(
            button(
                "📨 إرسال رمز التحقق"
            ) {

                val input =
                    phone.text
                        .toString()
                        .trim()

                if (input.isEmpty()) {

                    phone.error =
                        "أدخل رقم الهاتف"

                    return@button
                }

                val normalized =
                    normalizeIraqPhone(input)

                if (normalized == null) {

                    phone.error =
                        "رقم عراقي غير صحيح"

                    return@button
                }

                phoneNumber =
                    normalized

                sendOtp()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    20,
                    0,
                    10
                )
            }
        )

        root.addView(
            button("رجوع") {
                showHome()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            )
        )

        setContentView(root)
    }

    // =====================================================
    // توحيد رقم الهاتف العراقي
    // =====================================================

    private fun normalizeIraqPhone(
        value: String
    ): String? {

        var phone =
            value
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")

        // +9647xxxxxxxxx

        if (phone.startsWith("+964")) {

            if (
                phone.length == 14 &&
                phone.substring(4, 5) == "7"
            ) {
                return phone
            }

            return null
        }

        // 009647xxxxxxxxx

        if (phone.startsWith("00964")) {

            phone =
                "+" +
                        phone.substring(2)

            if (
                phone.length == 14 &&
                phone.substring(4, 5) == "7"
            ) {
                return phone
            }

            return null
        }

        // 07xxxxxxxxx

        if (phone.startsWith("07")) {

            phone =
                "+964" +
                        phone.substring(1)

            if (phone.length == 14) {
                return phone
            }

            return null
        }

        return null
    }

    // =====================================================
    // إرسال OTP
    // =====================================================

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

                Toast.makeText(
                    this@MainActivity,
                    "تم إرسال رمز التحقق إلى $phoneNumber",
                    Toast.LENGTH_LONG
                ).show()

                showOtpScreen()

            } catch (e: Exception) {

                loading.dismiss()

                showError(
                    "تعذر إرسال رمز التحقق",
                    e.message
                        ?: "حدث خطأ غير معروف"
                )
            }
        }
    }

    // =====================================================
    // شاشة OTP
    // =====================================================

    private fun showOtpScreen() {

        val root = baseLayout()

        root.addView(
            text(
                "تأكيد رقم الهاتف",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل رمز التحقق المكون من 6 أرقام\nالذي وصلك عبر SMS",
                18f,
                Color.GRAY
            )
        )

        val otp =
            EditText(this).apply {

                hint = "000000"

                textSize = 28f

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                gravity =
                    Gravity.CENTER

                setPadding(
                    20,
                    10,
                    20,
                    10
                )

                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR

                maxLines = 1

                filters =
                    arrayOf(
                        InputFilter.LengthFilter(6)
                    )
            }

        root.addView(
            otp,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                75
            ).apply {

                setMargins(
                    0,
                    30,
                    0,
                    20
                )
            }
        )

        root.addView(
            button(
                "✅ تأكيد الرمز"
            ) {

                val code =
                    otp.text
                        .toString()
                        .trim()

                if (code.length != 6) {

                    otp.error =
                        "أدخل رمز التحقق المكون من 6 أرقام"

                    return@button
                }

                verifyOtp(code)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    10
                )
            }
        )

        root.addView(
            button(
                "🔄 إرسال الرمز مرة أخرى"
            ) {
                sendOtp()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    10
                )
            }
        )

        root.addView(
            button(
                "📱 تغيير رقم الهاتف"
            ) {
                showPhoneLogin()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    10
                )
            }
        )

        setContentView(root)
    }

    // =====================================================
    // التحقق من OTP
    // =====================================================

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
                        type = OtpType.Phone.SMS,
                        phone = phoneNumber,
                        token = code
                    )

                loading.dismiss()

                AlertDialog.Builder(
                    this@MainActivity
                )
                    .setTitle(
                        "تم تسجيل الدخول ✅"
                    )
                    .setMessage(
                        "تم التحقق من رقم هاتفك بنجاح.\n\n" +
                                "يمكنك الآن إنشاء طلب تمريض منزلي."
                    )
                    .setPositiveButton(
                        "متابعة"
                    ) { _, _ ->

                        showHome()
                    }
                    .setCancelable(false)
                    .show()

            } catch (e: Exception) {

                loading.dismiss()

                showError(
                    "رمز التحقق غير صحيح",
                    e.message
                        ?: "تأكد من الرمز وحاول مرة أخرى"
                )
            }
        }
    }

    // =====================================================
    // شاشة طلب ممرض
    // =====================================================

    private fun showRequestScreen() {

        val root = baseLayout()

        root.addView(
            text(
                "🩺 طلب ممرض منزلي",
                28f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "اختر نوع الخدمة التي تحتاجها",
                18f,
                Color.GRAY
            )
        )

        val service =
            Spinner(this)

        val services =
            arrayOf(
                "اختر الخدمة",
                "قياس ضغط الدم",
                "قياس السكر",
                "إعطاء حقنة",
                "تغيير الضماد",
                "تركيب المحلول",
                "رعاية كبار السن",
                "رعاية المرضى في المنزل",
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
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            ).apply {

                setMargins(
                    0,
                    20,
                    0,
                    15
                )
            }
        )

        val notes =
            EditText(this).apply {

                hint =
                    "ملاحظات إضافية"

                textSize = 17f

                gravity =
                    Gravity.TOP or Gravity.RIGHT

                minLines = 4

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE

                setPadding(
                    15,
                    15,
                    15,
                    15
                )
            }

        root.addView(
            notes,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                130
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    20
                )
            }
        )

        root.addView(
            button(
                "📍 تحديد موقع الطلب"
            ) {

                Toast.makeText(
                    this,
                    "سيتم تفعيل تحديد الموقع في المرحلة التالية",
                    Toast.LENGTH_SHORT
                ).show()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    10
                )
            }
        )

        root.addView(
            button(
                "📨 إرسال طلب التمريض"
            ) {

                if (service.selectedItemPosition == 0) {

                    Toast.makeText(
                        this,
                        "اختر نوع الخدمة أولاً",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@button
                }

                AlertDialog.Builder(this)
                    .setTitle(
                        "تأكيد الطلب"
                    )
                    .setMessage(
                        "هل تريد إرسال طلب التمريض المنزلي؟"
                    )
                    .setNegativeButton(
                        "إلغاء",
                        null
                    )
                    .setPositiveButton(
                        "إرسال"
                    ) { _, _ ->

                        Toast.makeText(
                            this,
                            "تم إنشاء الطلب بنجاح",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .show()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    10,
                    0,
                    15
                )
            }
        )

        root.addView(
            button("رجوع") {
                showHome()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            )
        )

        setContentView(root)
    }

    // =====================================================
    // الخدمات
    // =====================================================

    private fun showServices() {

        val root = baseLayout()

        root.addView(
            text(
                "🏥 الخدمات التمريضية",
                28f,
                DARK_BLUE
            )
        )

        val services =
            listOf(
                "💉 إعطاء الحقن",
                "🩹 تغيير الضمادات",
                "🩸 قياس ضغط الدم والسكر",
                "💧 تركيب المحاليل",
                "👴 رعاية كبار السن",
                "🏠 الرعاية الصحية المنزلية",
                "🛏️ رعاية المرضى طريحي الفراش",
                "📋 متابعة الحالات الصحية"
            )

        for (item in services) {

            root.addView(
                text(
                    item,
                    19f,
                    TEXT
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    60
                )
            )
        }

        root.addView(
            button("رجوع") {
                showHome()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            ).apply {

                setMargins(
                    0,
                    20,
                    0,
                    0
                )
            }
        )

        setContentView(root)
    }

    // =====================================================
    // طلباتي
    // =====================================================

    private fun showBookings() {

        val root = baseLayout()

        root.addView(
            text(
                "📋 طلباتي السابقة",
                28f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "لا توجد طلبات مسجلة حالياً",
                19f,
                Color.GRAY
            )
        )

        root.addView(
            button(
                "🩺 إنشاء طلب جديد"
            ) {
                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(
                    0,
                    25,
                    0,
                    15
                )
            }
        )

        root.addView(
            button("رجوع") {
                showHome()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            )
        )

        setContentView(root)
    }

    // =====================================================
    // تواصل معنا
    // =====================================================

    private fun contactUs() {

        AlertDialog.Builder(this)
            .setTitle(
                "☎️ تواصل معنا"
            )
            .setMessage(
                "التمريض المنزلي - محافظة الأنبار\n\n" +
                        "يمكنك التواصل مع إدارة الخدمة للحصول على المساعدة."
            )
            .setPositiveButton(
                "حسناً",
                null
            )
            .show()
    }

    // =====================================================
    // عرض الأخطاء
    // =====================================================

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
