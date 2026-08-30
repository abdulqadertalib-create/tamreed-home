package iq.tamreed.home

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import java.util.Locale

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
    private val BLUE = Color.rgb(31, 115, 176)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val GREEN = Color.rgb(50, 150, 85)
    private val RED = Color.rgb(205, 65, 65)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(125, 125, 125)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val BORDER = Color.rgb(220, 225, 230)
    private val WHITE = Color.WHITE

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val LOCATION_PERMISSION_REQUEST = 501

    private var phoneNumber = ""
    private var patientPhone = ""
    private var selectedCity = ""
    private var landmark = ""
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedAddress = ""

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = SupabaseManager.client.auth.currentUserOrNull()

        if (user == null) {
            showPhoneLogin()
        } else {
            showHome()
        }
    }

    override fun onDestroy() {
        stopLocationUpdates()
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun bordered(
        color: Int,
        strokeColor: Int = BORDER,
        radius: Int = 18
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
        }

    private fun baseLayout(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)
            setPadding(dp(14), dp(10), dp(14), dp(80))
        }

    private fun scroll(view: View): ScrollView =
        ScrollView(this).apply {
            setBackgroundColor(LIGHT_GRAY)
            isFillViewport = true
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
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

    private fun button(value: String, action: () -> Unit): Button =
        Button(this).apply {
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

    private fun outlineButton(value: String, action: () -> Unit): Button =
        Button(this).apply {
            text = value
            textSize = 16f
            isAllCaps = false
            setTextColor(NAVY)
            gravity = Gravity.CENTER
            background = bordered(WHITE, NAVY, 14)
            setOnClickListener { action() }
        }

    private fun addSpace(root: LinearLayout, height: Int) {
        root.addView(Space(this), LinearLayout.LayoutParams(1, dp(height)))
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
            bar.addView(
                TextView(this).apply {
                    text = "‹"
                    textSize = 40f
                    setTextColor(NAVY)
                    gravity = Gravity.CENTER
                    setOnClickListener { backAction() }
                },
                LinearLayout.LayoutParams(dp(55), dp(55))
            )
        }

        bar.addView(
            text(title, 22f, NAVY, true),
            LinearLayout.LayoutParams(0, dp(55), 1f)
        )

        bar.addView(
            text("🔔", 23f, NAVY),
            LinearLayout.LayoutParams(dp(55), dp(55))
        )

        return bar
    }

    private fun bottomNavigation(selected: String): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(248, 251, 253))
            elevation = dp(8).toFloat()
        }

        fun item(icon: String, title: String, key: String, action: () -> Unit) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { action() }
            }

            box.addView(
                text(icon, 24f, if (selected == key) NAVY else GRAY)
            )
            box.addView(
                text(title, 13f, if (selected == key) NAVY else GRAY)
            )

            nav.addView(
                box,
                LinearLayout.LayoutParams(0, dp(70), 1f)
            )
        }

        item("⋯", "المزيد", "more") { showMore() }
        item("💬", "المحادثات", "chat") { showChats() }
        item("☷", "الطلبات", "orders") { showBookings() }
        item("⌂", "الرئيسية", "home") { showHome() }

        return nav
    }

    private fun showPhoneLogin() {
        val root = baseLayout()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(WHITE, 22)
            setPadding(dp(15), dp(18), dp(15), dp(18))
        }

        header.addView(text("🩺", 52f, NAVY))
        header.addView(text("التمريض المنزلي", 27f, NAVY, true))
        header.addView(text("محافظة الأنبار - العراق", 14f, GRAY))

        root.addView(header, LinearLayout.LayoutParams(-1, dp(170)))
        addSpace(root, 22)
        root.addView(text("تسجيل الدخول", 29f, NAVY, true))
        root.addView(text("أدخل رقم هاتفك للمتابعة", 16f, GRAY))
        addSpace(root, 20)

        val loginCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(WHITE, 20)
            setPadding(dp(18), dp(20), dp(18), dp(20))
        }

        loginCard.addView(text("📱", 42f, NAVY))
        loginCard.addView(text("رقم الهاتف", 18f, NAVY, true))
        addSpace(loginCard, 8)

        val phone = EditText(this).apply {
            hint = "07701234567"
            textSize = 19f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_PHONE
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = bordered(WHITE, BORDER, 15)
            setPadding(dp(15), dp(5), dp(15), dp(5))
        }

        loginCard.addView(phone, LinearLayout.LayoutParams(-1, dp(65)))
        loginCard.addView(text("مثال: 07701234567", 13f, GRAY))
        addSpace(loginCard, 15)

        loginCard.addView(
            button("إرسال رمز التحقق") {
                val normalized = normalizeIraqPhone(phone.text.toString().trim())

                if (normalized == null) {
                    phone.error = "رقم الهاتف العراقي غير صحيح"
                    return@button
                }

                phoneNumber = normalized
                sendOtp()
            },
            LinearLayout.LayoutParams(-1, dp(62))
        )

        root.addView(loginCard, LinearLayout.LayoutParams(-1, -2))
        addSpace(root, 18)
        root.addView(
            text(
                "بتسجيل الدخول أنت توافق على شروط استخدام الخدمة وسياسة الخصوصية.",
                12f,
                GRAY
            )
        )
        addSpace(root, 15)

        root.addView(
            outlineButton("🩺 دخول الممرضين") {
                startActivity(Intent(this, NurseLoginActivity::class.java))
            },
            LinearLayout.LayoutParams(-1, dp(58))
        )

        setContentView(scroll(root))
    }

    private fun normalizeIraqPhone(value: String): String? {
        var phone = value
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (phone.startsWith("+964")) {
            return if (phone.length == 14 && phone.getOrNull(4) == '7') phone else null
        }

        if (phone.startsWith("00964")) {
            phone = "+" + phone.substring(2)
            return if (phone.length == 14 && phone.getOrNull(4) == '7') phone else null
        }

        if (phone.startsWith("07")) {
            phone = "+964" + phone.substring(1)
            return if (phone.length == 14) phone else null
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
                    "تعذر إرسال الرمز",
                    e.message ?: "تأكد من إعداد Phone Auth في Supabase."
                )
            }
        }
    }

    private fun showOtpScreen() {
        val root = baseLayout()

        root.addView(topBar("تأكيد رقم الهاتف", ::showPhoneLogin))
        addSpace(root, 25)
        root.addView(text("🔐", 58f, NAVY))
        root.addView(text("أدخل رمز التحقق", 28f, NAVY, true))
        root.addView(text("تم إرسال الرمز إلى", 15f, GRAY))
        root.addView(text(phoneNumber, 18f, NAVY, true))
        addSpace(root, 20)

        val otp = EditText(this).apply {
            hint = "000000"
            textSize = 28f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            maxLines = 1
            filters = arrayOf(InputFilter.LengthFilter(6))
            background = bordered(WHITE, BORDER, 15)
            setPadding(dp(15), dp(5), dp(15), dp(5))
        }

        root.addView(otp, LinearLayout.LayoutParams(-1, dp(70)))
        addSpace(root, 18)

        root.addView(
            button("تأكيد الرمز") {
                val code = otp.text.toString().trim()

                if (code.length != 6) {
                    otp.error = "أدخل 6 أرقام"
                    return@button
                }

                verifyOtp(code)
            },
            LinearLayout.LayoutParams(-1, dp(62))
        )

        addSpace(root, 10)

        root.addView(
            outlineButton("إرسال الرمز مرة أخرى") { sendOtp() },
            LinearLayout.LayoutParams(-1, dp(58))
        )

        setContentView(scroll(root))
    }

    private fun verifyOtp(code: String) {
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
                    token = code
                )

                loading.dismiss()
                Toast.makeText(this@MainActivity, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                showHome()
            } catch (e: Exception) {
                loading.dismiss()
                showError(
                    "فشل التحقق",
                    e.message ?: "رمز التحقق غير صحيح."
                )
            }
        }
    }

    private fun showHome() {
        val root = baseLayout()

        root.addView(topBar("الرئيسية"))
        addSpace(root, 12)

        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(NAVY, 22)
            setPadding(dp(15), dp(20), dp(15), dp(20))
        }

        welcome.addView(text("التمريض المنزلي", 29f, WHITE, true))
        welcome.addView(text("خدمة تمريض تصل إليك أينما كنت", 16f, WHITE))
        welcome.addView(text("محافظة الأنبار - العراق", 14f, Color.rgb(220, 235, 245)))

        root.addView(welcome, LinearLayout.LayoutParams(-1, dp(150)))
        addSpace(root, 15)

        root.addView(
            button("🩺   إنشاء طلب تمريض الآن") {
                checkLoginBeforeRequest()
            },
            LinearLayout.LayoutParams(-1, dp(65))
        )

        addSpace(root, 17)
        root.addView(text("الخدمات الأكثر طلباً", 23f, NAVY, true))
        addSpace(root, 8)

        val services = listOf(
            Triple("💉", "إعطاء الحقن", "خدمة منزلية"),
            Triple("🩹", "تغيير الضماد", "العناية بالجروح"),
            Triple("🩸", "قياس السكر", "فحص منزلي"),
            Triple("🩺", "قياس الضغط", "متابعة الضغط")
        )

        for (i in services.indices step 2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }

            row.addView(
                serviceCard(services[i].first, services[i].second, services[i].third) {
                    checkLoginBeforeRequest()
                },
                LinearLayout.LayoutParams(0, dp(145), 1f).apply {
                    setMargins(0, 0, dp(5), 0)
                }
            )

            if (i + 1 < services.size) {
                row.addView(
                    serviceCard(services[i + 1].first, services[i + 1].second, services[i + 1].third) {
                        checkLoginBeforeRequest()
                    },
                    LinearLayout.LayoutParams(0, dp(145), 1f).apply {
                        setMargins(dp(5), 0, 0, 0)
                    }
                )
            }

            root.addView(row)
            addSpace(root, 10)
        }

        root.addView(
            outlineButton("عرض جميع الخدمات") { showServices() },
            LinearLayout.LayoutParams(-1, dp(58))
        )

        addSpace(root, 15)

        root.addView(
            text(
                "📍 الموقع الجغرافي أصبح متاحاً لتحديد موقع المريض بدقة عند إنشاء الطلب.",
                14f,
                NAVY,
                true
            )
        )

        addSpace(root, 15)
        root.addView(bottomNavigation("home"))

        setContentView(scroll(root))
    }

    private fun serviceCard(
        icon: String,
        title: String,
        description: String,
        action: () -> Unit
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 18)
            elevation = dp(2).toFloat()
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { action() }

            addView(text(icon, 34f, NAVY))
            addView(text(title, 17f, NAVY, true))
            addView(text(description, 12f, GRAY))
        }

    private fun checkLoginBeforeRequest() {
        if (SupabaseManager.client.auth.currentUserOrNull() == null) {
            showPhoneLogin()
        } else {
            showRequestScreen()
        }
    }

    private fun showRequestScreen() {
        val root = baseLayout()

        root.addView(topBar("إنشاء طلب", ::showHome))
        addSpace(root, 12)
        root.addView(text("بيانات طلب التمريض", 25f, NAVY, true))
        root.addView(text("أدخل معلومات المريض والموقع بالتفصيل", 15f, GRAY))
        addSpace(root, 15)

        val services = arrayOf(
            "اختر الخدمة",
            "إعطاء حقنة",
            "تغيير الضماد",
            "قياس السكر",
            "قياس ضغط الدم",
            "تركيب المحلول",
            "رعاية كبار السن",
            "رعاية المرضى في المنزل",
            "متابعة حالة صحية",
            "خدمة تمريض أخرى"
        )

        val service = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                services
            )
        }

        root.addView(service, LinearLayout.LayoutParams(-1, dp(60)))
        addSpace(root, 12)

        val patient = EditText(this).apply {
            hint = "اسم المريض"
            textSize = 17f
            gravity = Gravity.RIGHT
            inputType = InputType.TYPE_CLASS_TEXT
            background = bordered(WHITE, BORDER, 14)
            setPadding(dp(15), dp(5), dp(15), dp(5))
        }

        root.addView(patient, LinearLayout.LayoutParams(-1, dp(62)))
        addSpace(root, 12)

        val patientPhoneInput = EditText(this).apply {
            hint = "رقم هاتف المريض للتواصل"
            textSize = 17f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_PHONE
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = bordered(WHITE, BORDER, 14)
            setPadding(dp(15), dp(5), dp(15), dp(5))
            setText(phoneNumber)
        }

        root.addView(patientPhoneInput, LinearLayout.LayoutParams(-1, dp(62)))
        addSpace(root, 12)

        root.addView(text("المدينة / القضاء", 16f, NAVY, true))
        addSpace(root, 4)

        val cities = arrayOf(
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

        val citySpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                cities
            )
        }

        root.addView(citySpinner, LinearLayout.LayoutParams(-1, dp(60)))
        addSpace(root, 12)

        val landmarkInput = EditText(this).apply {
            hint = "أقرب نقطة دالة (جامع، مدرسة، مستشفى، شارع...)"
            textSize = 16f
            gravity = Gravity.RIGHT
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            background = bordered(WHITE, BORDER, 14)
            setPadding(dp(15), dp(12), dp(15), dp(12))
        }

        root.addView(landmarkInput, LinearLayout.LayoutParams(-1, dp(75)))
        addSpace(root, 12)

        root.addView(
            button("📍  تحديد موقع المريض باستخدام GPS") {
                showLocation()
            },
            LinearLayout.LayoutParams(-1, dp(62))
        )

        addSpace(root, 8)

        val locationStatus = text(
            if (selectedAddress.isBlank())
                "لم يتم تحديد الموقع بعد"
            else
                selectedAddress,
            14f,
            if (selectedAddress.isBlank()) GRAY else GREEN,
            selectedAddress.isNotBlank()
        )

        root.addView(locationStatus)

        addSpace(root, 12)

        val notes = EditText(this).apply {
            hint = "ملاحظات إضافية عن الحالة"
            textSize = 16f
            gravity = Gravity.TOP or Gravity.RIGHT
            minLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            background = bordered(WHITE, BORDER, 14)
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        root.addView(notes, LinearLayout.LayoutParams(-1, dp(130)))
        addSpace(root, 18)

        root.addView(
            button("📨  إرسال طلب التمريض الآن") {
                if (service.selectedItemPosition == 0) {
                    Toast.makeText(this, "اختر الخدمة أولاً", Toast.LENGTH_SHORT).show()
                    return@button
                }

                val patientName = patient.text.toString().trim()
                if (patientName.isEmpty()) {
                    patient.error = "أدخل اسم المريض"
                    return@button
                }

                val enteredPhone = normalizeIraqPhone(patientPhoneInput.text.toString().trim())
                if (enteredPhone == null) {
                    patientPhoneInput.error = "أدخل رقم هاتف عراقي صحيح"
                    return@button
                }

                if (citySpinner.selectedItemPosition == 0) {
                    Toast.makeText(this, "اختر المدينة / القضاء", Toast.LENGTH_SHORT).show()
                    return@button
                }

                val enteredLandmark = landmarkInput.text.toString().trim()
                if (enteredLandmark.isEmpty()) {
                    landmarkInput.error = "أدخل أقرب نقطة دالة"
                    return@button
                }

                if (selectedLatitude == null || selectedLongitude == null) {
                    Toast.makeText(
                        this,
                        "حدد موقع المريض باستخدام GPS أولاً",
                        Toast.LENGTH_LONG
                    ).show()
                    showLocation()
                    return@button
                }

                patientPhone = enteredPhone
                selectedCity = citySpinner.selectedItem.toString().trim()
                landmark = enteredLandmark

                createBooking(
                    service.selectedItem.toString(),
                    patientName,
                    notes.text.toString().trim()
                )
            },
            LinearLayout.LayoutParams(-1, dp(65))
        )

        addSpace(root, 10)

        root.addView(
            outlineButton("إلغاء") { showHome() },
            LinearLayout.LayoutParams(-1, dp(55))
        )

        setContentView(scroll(root))
    }

    private fun createBooking(
        service: String,
        patient: String,
        notes: String
    ) {
        val user = SupabaseManager.client.auth.currentUserOrNull()

        if (user == null) {
            showPhoneLogin()
            return
        }

        val phoneForBooking =
            if (patientPhone.isBlank()) phoneNumber else patientPhone

        AlertDialog.Builder(this)
            .setTitle("تأكيد الطلب")
            .setMessage(
                "الخدمة: $service\n\n" +
                    "المريض: $patient\n\n" +
                    "الهاتف: $phoneForBooking\n\n" +
                    "المدينة: $selectedCity\n\n" +
                    "أقرب نقطة دالة: $landmark\n\n" +
                    "الموقع: ${selectedLatitude ?: "-"}, ${selectedLongitude ?: "-"}"
            )
            .setNegativeButton("تعديل", null)
            .setPositiveButton("إرسال") { _, _ ->
                val loading = ProgressDialog(this).apply {
                    setMessage("جاري إرسال الطلب...")
                    setCancelable(false)
                    show()
                }

                scope.launch {
                    try {
                        val booking = BookingInsert(
                            patient_id = user.id,
                            service_id = service,
                            address = if (selectedAddress.isBlank()) {
                                "$selectedCity - $landmark"
                            } else {
                                selectedAddress
                            },
                            city = selectedCity,
                            landmark = landmark,
                            patient_phone = phoneForBooking,
                            latitude = selectedLatitude,
                            longitude = selectedLongitude,
                            status = "PENDING",
                            notes = if (notes.isBlank()) {
                                "المريض: $patient"
                            } else {
                                "المريض: $patient\n$notes"
                            }
                        )

                        SupabaseManager.client
                            .from("bookings")
                            .insert(booking)

                        loading.dismiss()

                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("تم إرسال الطلب ✅")
                            .setMessage(
                                "تم إرسال طلب التمريض مع موقع المريض بنجاح."
                            )
                            .setPositiveButton("متابعة الطلب") { _, _ ->
                                clearBookingLocation()
                                showBookings()
                            }
                            .show()

                    } catch (e: Exception) {
                        loading.dismiss()
                        showError(
                            "تعذر إرسال الطلب",
                            e.message ?: "حدث خطأ أثناء حفظ الطلب."
                        )
                    }
                }
            }
            .show()
    }

    /*
     * =========================================================
     * GPS الحقيقي
     * =========================================================
     */

    private fun showLocation() {
        val root = baseLayout()

        root.addView(topBar("موقع المريض", ::showRequestScreen))
        addSpace(root, 20)

        root.addView(text("📍", 65f, NAVY))
        root.addView(text("حدد موقع المريض", 26f, NAVY, true))
        root.addView(
            text(
                "سيتم استخدام GPS الحقيقي للهاتف وإرسال الإحداثيات مع طلب التمريض.",
                15f,
                GRAY
            )
        )

        addSpace(root, 25)

        val currentLocationText = text(
            if (selectedLatitude != null && selectedLongitude != null)
                formatCoordinates()
            else
                "لم يتم تحديد الموقع بعد",
            16f,
            if (selectedLatitude != null) GREEN else GRAY,
            selectedLatitude != null
        )

        root.addView(currentLocationText)
        addSpace(root, 15)

        root.addView(
            button("📍  تحديد موقعي الحالي GPS") {
                requestCurrentLocation()
            },
            LinearLayout.LayoutParams(-1, dp(62))
        )

        addSpace(root, 10)

        root.addView(
            outlineButton("🗺️ فتح الموقع في خرائط Google") {
                openCurrentLocationOnMaps()
            },
            LinearLayout.LayoutParams(-1, dp(60))
        )

        addSpace(root, 20)

        root.addView(
            text(
                "تنبيه: يجب تفعيل خدمة الموقع GPS ومنح التطبيق إذن الوصول إلى الموقع.",
                14f,
                NAVY,
                true
            )
        )

        addSpace(root, 20)

        root.addView(
            emptyState(
                "📍",
                if (selectedAddress.isBlank())
                    "لم يتم تحديد الموقع"
                else
                    selectedAddress,
                if (selectedLatitude != null)
                    formatCoordinates()
                else
                    "اضغط على زر تحديد موقعي الحالي GPS"
            )
        )

        addSpace(root, 20)

        root.addView(
            button("حفظ الموقع والعودة للطلب") {
                if (selectedLatitude == null || selectedLongitude == null) {
                    Toast.makeText(
                        this,
                        "حدد الموقع أولاً",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showRequestScreen()
                }
            },
            LinearLayout.LayoutParams(-1, dp(62))
        )

        setContentView(scroll(root))
    }

    private fun requestCurrentLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager = manager

        val provider =
            when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER

                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER

                else -> null
            }

        if (provider == null) {
            AlertDialog.Builder(this)
                .setTitle("خدمة الموقع غير مفعلة")
                .setMessage("فعّل GPS أو خدمة الموقع من إعدادات الهاتف ثم حاول مرة أخرى.")
                .setPositiveButton("فتح الإعدادات") { _, _ ->
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    } catch (_: Exception) {
                    }
                }
                .setNegativeButton("إلغاء", null)
                .show()
            return
        }

        Toast.makeText(
            this,
            "جاري تحديد موقعك الحالي...",
            Toast.LENGTH_SHORT
        ).show()

        try {
            val last = manager.getLastKnownLocation(provider)

            if (last != null) {
                acceptLocation(last)
                Toast.makeText(
                    this,
                    "تم تحديد الموقع الحالي",
                    Toast.LENGTH_SHORT
                ).show()
                showLocation()
                return
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    acceptLocation(location)
                    stopLocationUpdates()

                    Toast.makeText(
                        this@MainActivity,
                        "تم تحديد الموقع بنجاح",
                        Toast.LENGTH_SHORT
                    ).show()

                    showLocation()
                }
            }

            locationListener = listener

            manager.requestLocationUpdates(
                provider,
                1000L,
                1f,
                listener,
                mainLooper
            )

        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "لم يتم منح إذن الموقع للتطبيق.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            showError(
                "تعذر تحديد الموقع",
                e.message ?: "تعذر الوصول إلى GPS."
            )
        }
    }

    private fun acceptLocation(location: Location) {
        selectedLatitude = location.latitude
        selectedLongitude = location.longitude

        selectedAddress = String.format(
            Locale.US,
            "الموقع الحالي: %.6f, %.6f",
            location.latitude,
            location.longitude
        )
    }

    private fun stopLocationUpdates() {
        try {
            val manager = locationManager
            val listener = locationListener

            if (manager != null && listener != null) {
                manager.removeUpdates(listener)
            }
        } catch (_: Exception) {
        }

        locationListener = null
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != LOCATION_PERMISSION_REQUEST) return

        if (hasLocationPermission()) {
            requestCurrentLocation()
        } else {
            AlertDialog.Builder(this)
                .setTitle("إذن الموقع مطلوب")
                .setMessage(
                    "لا يمكن تحديد موقع المريض بدون السماح للتطبيق بالوصول إلى موقع الهاتف."
                )
                .setPositiveButton("حاول مرة أخرى") { _, _ ->
                    requestCurrentLocation()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }
    }

    private fun formatCoordinates(): String {
        val lat = selectedLatitude ?: return "لا يوجد موقع"
        val lon = selectedLongitude ?: return "لا يوجد موقع"

        return String.format(
            Locale.US,
            "خط العرض: %.6f\nخط الطول: %.6f",
            lat,
            lon
        )
    }

    private fun openCurrentLocationOnMaps() {
        val lat = selectedLatitude
        val lon = selectedLongitude

        if (lat == null || lon == null) {
            Toast.makeText(
                this,
                "حدد الموقع أولاً",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val uri = Uri.parse(
                "geo:$lat,$lon?q=$lat,$lon"
            )

            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "تعذر فتح الخرائط",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun clearBookingLocation() {
        selectedLatitude = null
        selectedLongitude = null
        selectedAddress = ""
        selectedCity = ""
        landmark = ""
        patientPhone = ""
    }

    private fun showServices() {
        val root = baseLayout()

        root.addView(topBar("الخدمات", ::showHome))
        addSpace(root, 12)

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

        for (service in services) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                background = rounded(WHITE, 17)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setOnClickListener { checkLoginBeforeRequest() }
            }

            card.addView(
                text(service.first, 32f, NAVY),
                LinearLayout.LayoutParams(dp(60), dp(70))
            )

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.RIGHT
            }

            info.addView(text(service.second, 18f, NAVY, true))
            info.addView(text(service.third, 13f, GRAY))

            card.addView(
                info,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            root.addView(
                card,
                LinearLayout.LayoutParams(-1, dp(82)).apply {
                    setMargins(0, dp(5), 0, dp(5))
                }
            )
        }

        addSpace(root, 15)

        root.addView(
            button("🩺 إنشاء طلب") { checkLoginBeforeRequest() },
            LinearLayout.LayoutParams(-1, dp(62))
        )

        setContentView(scroll(root))
    }

    private fun showBookings() {
        try {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        } catch (_: Exception) {
            showBookingsFallback()
        }
    }

    private fun showBookingsFallback() {
        val root = baseLayout()

        root.addView(topBar("الطلبات", ::showHome))
        addSpace(root, 10)

        root.addView(
            button("＋   إنشاء طلب") { checkLoginBeforeRequest() },
            LinearLayout.LayoutParams(-1, dp(62))
        )

        addSpace(root, 15)

        val loading = text("جاري تحميل الطلبات...", 16f, GRAY)
        root.addView(loading)
        setContentView(scroll(root))

        val user = SupabaseManager.client.auth.currentUserOrNull()

        if (user == null) {
            loading.text = "سجل الدخول لعرض طلباتك"
            return
        }

        scope.launch {
            try {
                val bookings = SupabaseManager.client
                    .from("bookings")
                    .select {
                        filter {
                            eq("patient_id", user.id)
                        }
                    }
                    .decodeList<PatientBooking>()

                loading.visibility = View.GONE

                if (bookings.isEmpty()) {
                    root.addView(
                        emptyState(
                            "📭",
                            "لا توجد طلبات بعد",
                            "عند إنشاء طلب تمريض سيظهر هنا"
                        )
                    )
                } else {
                    bookings
                        .sortedByDescending { it.created_at }
                        .forEach { addBookingCard(root, it) }
                }
            } catch (e: Exception) {
                loading.text =
                    "تعذر تحميل الطلبات\n\n${e.message ?: "خطأ غير معروف"}"
            }
        }
    }

    private fun addBookingCard(
        root: LinearLayout,
        booking: PatientBooking
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 20)
            setPadding(dp(15), dp(15), dp(15), dp(15))
            elevation = dp(2).toFloat()
        }

        card.addView(
            text("🩺  ${booking.service_id}", 19f, NAVY, true)
        )

        card.addView(
            text("📍  ${booking.address}", 15f, TEXT)
        )

        booking.city?.takeIf { it.isNotBlank() }?.let {
            card.addView(text("🏙️  المدينة: $it", 14f, TEXT))
        }

        booking.landmark?.takeIf { it.isNotBlank() }?.let {
            card.addView(text("📌  أقرب نقطة دالة: $it", 14f, TEXT))
        }

        if (booking.latitude != null && booking.longitude != null) {
            card.addView(
                text(
                    String.format(
                        Locale.US,
                        "📍  الإحداثيات: %.6f, %.6f",
                        booking.latitude,
                        booking.longitude
                    ),
                    14f,
                    NAVY,
                    true
                )
            )

            card.addView(
                outlineButton("🗺️ فتح موقع المريض") {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "geo:${booking.latitude},${booking.longitude}?q=${booking.latitude},${booking.longitude}"
                                )
                            )
                        )
                    } catch (_: Exception) {
                    }
                },
                LinearLayout.LayoutParams(-1, dp(50))
            )
        }

        booking.patient_phone?.takeIf { it.isNotBlank() }?.let { phone ->
            card.addView(text("📞  هاتف المريض: $phone", 14f, NAVY, true))

            card.addView(
                outlineButton("اتصال بالمريض") {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:$phone")
                            )
                        )
                    } catch (_: Exception) {
                    }
                },
                LinearLayout.LayoutParams(-1, dp(50))
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

        booking.notes?.takeIf { it.isNotBlank() }?.let {
            card.addView(text("📝 $it", 14f, GRAY))
        }

        root.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, dp(5), 0, dp(10))
            }
        )
    }

    private fun statusText(status: String): String =
        when (status.uppercase()) {
            "PENDING" -> "بانتظار قبول الممرض"
            "ACCEPTED" -> "تم قبول الطلب"
            "ON_THE_WAY" -> "الممرض في الطريق"
            "IN_PROGRESS" -> "الزيارة جارية"
            "COMPLETED" -> "اكتملت الزيارة"
            "CANCELLED" -> "تم إلغاء الطلب"
            else -> status
        }

    private fun statusColor(status: String): Int =
        when (status.uppercase()) {
            "PENDING" -> Color.rgb(230, 145, 45)
            "ACCEPTED", "ON_THE_WAY", "IN_PROGRESS" -> BLUE
            "COMPLETED" -> GREEN
            "CANCELLED" -> RED
            else -> GRAY
        }

    private fun emptyState(
        icon: String,
        title: String,
        description: String
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 20)
            setPadding(dp(20), dp(25), dp(20), dp(25))

            addView(text(icon, 48f, NAVY))
            addView(text(title, 19f, NAVY, true))
            addView(text(description, 14f, GRAY))
        }

    private fun showMore() {
        AlertDialog.Builder(this)
            .setTitle("المزيد")
            .setItems(
                arrayOf(
                    "الرئيسية",
                    "الخدمات",
                    "طلباتي",
                    "دخول الممرضين"
                )
            ) { _, which ->
                when (which) {
                    0 -> showHome()
                    1 -> showServices()
                    2 -> showBookings()
                    3 -> startActivity(Intent(this, NurseLoginActivity::class.java))
                }
            }
            .show()
    }

    private fun showChats() {
        AlertDialog.Builder(this)
            .setTitle("المحادثات")
            .setMessage(
                "المحادثات مع الممرضين ستكون متاحة بعد قبول الطلب."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show()
    }
}
