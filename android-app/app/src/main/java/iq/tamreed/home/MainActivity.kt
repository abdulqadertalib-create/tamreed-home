package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    // =====================================================
    // الألوان
    // =====================================================

    private val BLUE = Color.rgb(0, 105, 210)
    private val DARK_BLUE = Color.rgb(0, 67, 135)
    private val LIGHT_BLUE = Color.rgb(235, 246, 255)
    private val GREEN = Color.rgb(28, 145, 85)
    private val RED = Color.rgb(200, 50, 50)
    private val ORANGE = Color.rgb(230, 130, 30)
    private val TEXT = Color.rgb(35, 45, 55)
    private val GRAY = Color.rgb(110, 110, 110)
    private val LIGHT_GRAY = Color.rgb(245, 247, 250)

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
    // مدن ومناطق محافظة الأنبار
    // =====================================================

    private val anbarCities = arrayOf(
        "اختر المدينة / المنطقة",
        "الرمادي",
        "الفلوجة",
        "الكرمة",
        "الصقلاوية",
        "الحبانية",
        "الخالدية",
        "عامرية الفلوجة",
        "هيت",
        "كبيسة",
        "حديثة",
        "البغدادي",
        "عانة",
        "راوة",
        "القائم",
        "الرطبة",
        "الوليد"
    )

    // =====================================================
    // إنشاء التطبيق
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showHome()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // =====================================================
    // تحويل dp
    // =====================================================

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    // =====================================================
    // التخطيط الأساسي
    // =====================================================

    private fun baseLayout(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.TOP

            setBackgroundColor(Color.WHITE)

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            setPadding(
                dp(16),
                dp(20),
                dp(16),
                dp(20)
            )
        }
    }

    // =====================================================
    // ScrollView
    // =====================================================

    private fun scroll(content: View): ScrollView {

        return ScrollView(this).apply {

            setBackgroundColor(Color.WHITE)

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            addView(content)
        }
    }

    // =====================================================
    // النصوص
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
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL
        }
    }

    // =====================================================
    // زر احترافي
    // =====================================================

    private fun button(
        value: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            text = value

            textSize = 17f

            setTextColor(Color.WHITE)

            isAllCaps = false

            gravity = Gravity.CENTER

            background =
                roundedBackground(
                    BLUE,
                    dp(12)
                )

            setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
            )

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            setOnClickListener {
                action()
            }
        }
    }

    // =====================================================
    // خلفية مستديرة
    // =====================================================

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                radius.toFloat()
        }
    }

    // =====================================================
    // بطاقة
    // =====================================================

    private fun card(
        title: String,
        description: String,
        icon: String,
        action: () -> Unit
    ): LinearLayout {

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setPadding(
                    dp(12),
                    dp(14),
                    dp(12),
                    dp(14)
                )

                background =
                    roundedBackground(
                        LIGHT_BLUE,
                        dp(16)
                    )

                setOnClickListener {
                    action()
                }
            }

        box.addView(
            text(
                icon,
                30f,
                DARK_BLUE
            )
        )

        box.addView(
            text(
                title,
                19f,
                DARK_BLUE
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

    // =====================================================
    // الصفحة الرئيسية
    // =====================================================

    private fun showHome() {

        val root = baseLayout()

        // -------------------------------------------------
        // الهيدر
        // -------------------------------------------------

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                setPadding(
                    dp(15),
                    dp(20),
                    dp(15),
                    dp(20)
                )

                background =
                    roundedBackground(
                        LIGHT_BLUE,
                        dp(20)
                    )
            }

        header.addView(
            text(
                "🏥",
                42f,
                DARK_BLUE
            )
        )

        header.addView(
            text(
                "التمريض المنزلي",
                31f,
                DARK_BLUE
            )
        )

        header.addView(
            text(
                "خدمات التمريض والرعاية الصحية المنزلية",
                18f,
                TEXT
            )
        )

        header.addView(
            text(
                "محافظة الأنبار - العراق",
                16f,
                GRAY
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    dp(15)
                )
            }
        )

        // -------------------------------------------------
        // الترحيب
        // -------------------------------------------------

        root.addView(
            text(
                "مرحباً بك 👋",
                25f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "احصل على خدمة تمريض منزلية بسهولة وأمان",
                17f,
                GRAY
            )
        )

        // -------------------------------------------------
        // زر الطلب الرئيسي
        // -------------------------------------------------

        root.addView(
            button(
                "🩺  اطلب ممرضاً الآن"
            ) {
                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(15),
                    0,
                    dp(12)
                )
            }
        )

        // -------------------------------------------------
        // تسجيل الدخول
        // -------------------------------------------------

        root.addView(
            button(
                "📱  تسجيل الدخول / إنشاء حساب"
            ) {
                showPhoneLogin()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    dp(10)
                )
            }
        )

        // -------------------------------------------------
        // الخدمات
        // -------------------------------------------------

        root.addView(
            text(
                "خدماتنا",
                23f,
                DARK_BLUE
            )
        )

        val serviceRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity = Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        serviceRow.addView(
            card(
                "قياس الضغط",
                "ضغط الدم",
                "🩺"
            ) {
                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                0,
                dp(145),
                1f
            ).apply {
                setMargins(
                    0,
                    dp(8),
                    dp(5),
                    dp(8)
                )
            }
        )

        serviceRow.addView(
            card(
                "قياس السكر",
                "فحص السكر",
                "🩸"
            ) {
                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                0,
                dp(145),
                1f
            ).apply {
                setMargins(
                    dp(5),
                    dp(8),
                    0,
                    dp(8)
                )
            }
        )

        root.addView(serviceRow)

        // -------------------------------------------------
        // أزرار إضافية
        // -------------------------------------------------

        root.addView(
            button(
                "🏥  جميع الخدمات التمريضية"
            ) {
                showServices()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {
                setMargins(
                    0,
                    dp(8),
                    0,
                    dp(8)
                )
            }
        )

        val bookingsButton = button("📋  طلباتي") {
            showBookings()
        }

        val bookingsParams =
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(8)
                )
            }

        root.addView(bookingsButton, bookingsParams)

        root.addView(
            button(
                "📍  تحديد موقع الطلب"
            ) {
                showLocation()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(8)
                )
            }
        )

        root.addView(
            button(
                "☎️  تواصل معنا"
            ) {
                contactUs()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            text(
                "خدمة التمريض المنزلي على مدار الساعة",
                14f,
                GRAY
            )
        )

        setContentView(
            scroll(root)
        )
    }

    // =====================================================
    // تسجيل الدخول
    // =====================================================

    private fun showPhoneLogin() {

        val root = baseLayout()

        root.addView(
            text(
                "📱 تسجيل الدخول",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل رقم هاتفك العراقي وسنرسل لك رمز التحقق",
                17f,
                GRAY
            )
        )

        val phone =
            EditText(this).apply {

                hint =
                    "07701234567"

                textSize = 18f

                inputType =
                    InputType.TYPE_CLASS_PHONE

                gravity =
                    Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR

                setPadding(
                    dp(15),
                    dp(10),
                    dp(15),
                    dp(10)
                )
            }

        root.addView(
            phone,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(25),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            text(
                "يمكنك إدخال الرقم بصيغة 07xxxxxxxxx أو +9647xxxxxxxxx",
                14f,
                GRAY
            )
        )

        root.addView(
            button(
                "📨  إرسال رمز التحقق"
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
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(25),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button("رجوع") {
                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        setContentView(
            scroll(root)
        )
    }

    // =====================================================
    // توحيد رقم العراق
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
                    "تم إرسال رمز التحقق",
                    Toast.LENGTH_LONG
                ).show()

                showOtpScreen()

            } catch (e: Exception) {

                loading.dismiss()

                showError(
                    "تعذر إرسال الرمز",
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
                "🔐 تأكيد رقم الهاتف",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل رمز التحقق الذي وصلك عبر SMS",
                17f,
                GRAY
            )
        )

        root.addView(
            text(
                phoneNumber,
                18f,
                DARK_BLUE
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

                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR

                maxLines = 1

                filters =
                    arrayOf(
                        InputFilter.LengthFilter(6)
                    )

                setPadding(
                    dp(20),
                    dp(10),
                    dp(20),
                    dp(10)
                )
            }

        root.addView(
            otp,
            LinearLayout.LayoutParams(
                -1,
                dp(75)
            ).apply {
                setMargins(
                    0,
                    dp(25),
                    0,
                    dp(15)
                )
            }
        )

        root.addView(
            button(
                "✅  تأكيد الرمز"
            ) {

                val code =
                    otp.text
                        .toString()
                        .trim()

                if (code.length != 6) {

                    otp.error =
                        "أدخل 6 أرقام"

                    return@button
                }

                verifyOtp(code)
            },
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(10),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button(
                "🔄  إرسال الرمز مرة أخرى"
            ) {
                sendOtp()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button(
                "📱  تغيير رقم الهاتف"
            ) {
                showPhoneLogin()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        setContentView(
            scroll(root)
        )
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

                showProfileAfterLogin()

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
    // بيانات المستخدم
    // =====================================================

    private fun showProfileAfterLogin() {

        val root = baseLayout()

        root.addView(
            text(
                "🎉 تم تسجيل الدخول بنجاح",
                28f,
                GREEN
            )
        )

        root.addView(
            text(
                "رقم الهاتف",
                16f,
                GRAY
            )
        )

        root.addView(
            text(
                phoneNumber,
                21f,
                DARK_BLUE
            )
        )

        root.addView(
            button(
                "🩺  إنشاء طلب تمريض"
            ) {
                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(25),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button(
                "📋  طلباتي"
            ) {
                showBookings()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button(
                "🏠  الصفحة الرئيسية"
            ) {
                showHome()
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

    // =====================================================
    // طلب تمريض
    // =====================================================

    private fun showRequestScreen() {

        val root = baseLayout()

        root.addView(
            text(
                "🩺 طلب ممرض منزلي",
                29f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "حدد الخدمة التي تحتاجها",
                17f,
                GRAY
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
            ).apply {
                setMargins(
                    0,
                    dp(20),
                    0,
                    dp(15)
                )
            }
        )

        // -------------------------------------------------
        // اختيار مدينة / منطقة الطلب
        // -------------------------------------------------

        root.addView(
            text(
                "📍 اختر مدينة أو منطقة المريض",
                17f,
                DARK_BLUE
            )
        )

        val city = Spinner(this)

        city.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                anbarCities
            )

        root.addView(
            city,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(15)
                )
            }
        )

        val patient =
            EditText(this).apply {

                hint = "اسم المريض"

                textSize = 17f

                gravity =
                    Gravity.RIGHT

                inputType =
                    InputType.TYPE_CLASS_TEXT

                setPadding(
                    dp(15),
                    dp(10),
                    dp(15),
                    dp(10)
                )
            }

        root.addView(
            patient,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(10)
                )
            }
        )

        val notes =
            EditText(this).apply {

                hint =
                    "ملاحظات إضافية عن الحالة"

                textSize = 17f

                gravity =
                    Gravity.TOP or Gravity.RIGHT

                minLines = 4

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE

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
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(15)
                )
            }
        )

        root.addView(
            button(
                "📍  تحديد موقع المريض"
            ) {
                showLocation()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button(
                "📨  إرسال طلب التمريض"
            ) {

                if (
                    service.selectedItemPosition == 0
                ) {

                    Toast.makeText(
                        this,
                        "اختر الخدمة أولاً",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@button
                }

                if (city.selectedItemPosition == 0) {

                    Toast.makeText(
                        this,
                        "اختر المدينة أو المنطقة أولاً",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@button
                }

                if (
                    patient.text
                        .toString()
                        .trim()
                        .isEmpty()
                ) {

                    patient.error =
                        "أدخل اسم المريض"

                    return@button
                }

                confirmRequest(
                    service.selectedItem.toString(),
                    city.selectedItem.toString(),
                    patient.text.toString(),
                    notes.text.toString()
                )
            },
            LinearLayout.LayoutParams(
                -1,
                dp(68)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button("رجوع") {
                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        setContentView(
            scroll(root)
        )
    }

    // =====================================================
    // تأكيد الطلب
    // =====================================================

    private fun confirmRequest(
        service: String,
        city: String,
        patient: String,
        notes: String
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "تأكيد طلب التمريض"
            )
            .setMessage(
                "الخدمة: $service\n\n" +
                        "المدينة / المنطقة: $city\n\n" +
                        "المريض: $patient\n\n" +
                        "الملاحظات: ${if (notes.trim().isEmpty()) "لا توجد" else notes.trim()}\n\n" +
                        "هل تريد إرسال الطلب؟"
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
                    "تم إرسال طلب التمريض بنجاح ✅",
                    Toast.LENGTH_LONG
                ).show()

                showBookings()
            }
            .show()
    }

    // =====================================================
    // الخدمات
    // =====================================================

    private fun showServices() {

        val root = baseLayout()

        root.addView(
            text(
                "🏥 الخدمات التمريضية",
                29f,
                DARK_BLUE
            )
        )

        val services =
            listOf(
                "💉 إعطاء الحقن" to
                        "إعطاء الحقن حسب وصف الطبيب",

                "🩹 تغيير الضمادات" to
                        "العناية بالجروح وتغيير الضماد",

                "🩸 قياس السكر" to
                        "قياس مستوى سكر الدم",

                "🩺 قياس الضغط" to
                        "قياس ومتابعة ضغط الدم",

                "💧 تركيب المحاليل" to
                        "خدمة تركيب المحاليل الوريدية",

                "👴 رعاية كبار السن" to
                        "رعاية ومتابعة كبار السن",

                "🛏️ رعاية المرضى" to
                        "الرعاية المنزلية للمرضى",

                "📋 متابعة صحية" to
                        "متابعة الحالة الصحية في المنزل"
            )

        for (
            item in services
        ) {

            val box =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    background =
                        roundedBackground(
                            LIGHT_GRAY,
                            dp(14)
                        )

                    setPadding(
                        dp(12),
                        dp(10),
                        dp(12),
                        dp(10)
                    )
                }

            box.addView(
                text(
                    item.first,
                    20f,
                    DARK_BLUE
                )
            )

            box.addView(
                text(
                    item.second,
                    14f,
                    GRAY
                )
            )

            root.addView(
                box,
                LinearLayout.LayoutParams(
                    -1,
                    dp(90)
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

        root.addView(
            button(
                "🩺  طلب خدمة الآن"
            ) {
                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(15),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button("رجوع") {
                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        setContentView(
            scroll(root)
        )
    }

    // =====================================================
    // الموقع
    // =====================================================

    private fun showLocation() {

        val root = baseLayout()

        root.addView(
            text(
                "📍 موقع الطلب",
                29f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "حدد موقع المريض ليسهل وصول الممرض",
                17f,
                GRAY
            )
        )

        root.addView(
            text(
                "🗺️",
                65f,
                BLUE
            )
        )

        root.addView(
            text(
                "سيتم ربط الخريطة وتحديد الموقع الجغرافي GPS في الخطوة القادمة.",
                17f,
                TEXT
            )
        )

        root.addView(
            button(
                "📍  فتح خرائط Google"
            ) {

                try {

                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "geo:33.3500,43.7833?q=33.3500,43.7833"
                            )
                        )

                    startActivity(intent)

                } catch (
                    e: Exception
                ) {

                    Toast.makeText(
                        this,
                        "تعذر فتح الخرائط",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(25),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button(
                "📌 استخدام موقعي"
            ) {

                Toast.makeText(
                    this,
                    "سيتم تفعيل GPS الحقيقي في المرحلة القادمة",
                    Toast.LENGTH_LONG
                ).show()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button("رجوع") {
                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        setContentView(
            scroll(root)
        )
    }

    // =====================================================
    // الطلبات
    // =====================================================

    private fun showBookings() {

        val root = baseLayout()

        root.addView(
            text(
                "📋 طلباتي",
                30f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "يمكنك هنا متابعة جميع طلبات التمريض الخاصة بك",
                16f,
                GRAY
            )
        )

        val empty =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(15),
                    dp(35),
                    dp(15),
                    dp(35)
                )

                background =
                    roundedBackground(
                        LIGHT_GRAY,
                        dp(18)
                    )
            }

        empty.addView(
            text(
                "📭",
                50f
            )
        )

        empty.addView(
            text(
                "لا توجد طلبات حالياً",
                21f,
                DARK_BLUE
            )
        )

        empty.addView(
            text(
                "عند إنشاء طلب تمريض سيظهر هنا",
                15f,
                GRAY
            )
        )

        root.addView(
            empty,
            LinearLayout.LayoutParams(
                -1,
                dp(190)
            ).apply {
                setMargins(
                    0,
                    dp(25),
                    0,
                    dp(20)
                )
            }
        )

        root.addView(
            button(
                "🩺  إنشاء طلب جديد"
            ) {
                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(10)
                )
            }
        )

        root.addView(
            button("رجوع") {
                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        setContentView(
            scroll(root)
        )
    }

    // =====================================================
    // التواصل
    // =====================================================

    private fun contactUs() {

        AlertDialog.Builder(this)
            .setTitle(
                "☎️ تواصل معنا"
            )
            .setMessage(
                "التمريض المنزلي\n" +
                        "محافظة الأنبار - العراق\n\n" +
                        "للاستفسارات والمساعدة تواصل مع إدارة الخدمة."
            )
            .setPositiveButton(
                "حسناً",
                null
            )
            .show()
    }

    // =====================================================
    // الأخطاء
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
