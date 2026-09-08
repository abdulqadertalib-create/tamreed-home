package iq.tamreed.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


// ============================================================
// بيانات الطلب
// ============================================================

@Serializable
data class NurseRequestsBooking(
    val id: String? = null,
    val patient_id: String? = null,
    val nurse_id: String? = null,
    val service_id: String? = null,
    val address: String? = null,
    val city: String? = null,
    val landmark: String? = null,
    val patient_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String? = null,
    val notes: String? = null,
    val created_at: String? = null
)


// ============================================================
// بيانات الممرض
// ============================================================

@Serializable
data class NurseRecordForRequests(
    val id: String? = null,
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null
)


// ============================================================
// بيانات الخدمة
// ============================================================

@Serializable
data class NurseServiceForRequests(
    val id: String? = null,
    val name_ar: String? = null,
    val name: String? = null
)


// ============================================================
// بيانات تحديث الطلب
// ============================================================

@Serializable
data class NurseBookingAssignment(
    val nurse_id: String,
    val status: String
)

@Serializable
data class NurseBookingStatusUpdate(
    val status: String
)


// ============================================================
// الشاشة
// ============================================================

class NurseRequestsActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val BLUE = Color.rgb(235, 245, 251)
    private val GREEN = Color.rgb(35, 145, 85)
    private val ORANGE = Color.rgb(220, 145, 35)
    private val RED = Color.rgb(190, 55, 55)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val BG = Color.rgb(247, 248, 249)
    private val WHITE = Color.WHITE

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    // ID الممرض من جدول nurses
    private var nurseId: String? = null

    // ID المستخدم من Supabase Auth
    private var currentUserId: String? = null

    // أسماء الخدمات
    private val serviceNames =
        mutableMapOf<String, String>()


    // ============================================================
    // onCreate
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadNurse()
    }


    // ============================================================
    // عند العودة للشاشة
    // ============================================================

    override fun onResume() {
        super.onResume()

        if (!nurseId.isNullOrBlank()) {
            loadRequests()
        }
    }


    // ============================================================
    // إيقاف Coroutine
    // ============================================================

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }


    // ============================================================
    // dp
    // ============================================================

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }


    // ============================================================
    // خلفية مستديرة
    // ============================================================

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


    // ============================================================
    // خلفية بإطار
    // ============================================================

    private fun bordered(
        color: Int = WHITE,
        strokeColor: Int =
            Color.rgb(215, 225, 232),
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


    // ============================================================
    // TextView
    // ============================================================

    private fun txt(
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
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )
        }
    }


    // ============================================================
    // زر رئيسي
    // ============================================================

    private fun primaryButton(
        title: String,
        color: Int = GREEN,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            text = title

            textSize = 16f

            isAllCaps = false

            setTextColor(WHITE)

            gravity =
                Gravity.CENTER

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            background =
                rounded(
                    color,
                    15
                )

            setOnClickListener {
                action()
            }
        }
    }


    // ============================================================
    // زر إطار
    // ============================================================

    private fun outlineButton(
        title: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            text = title

            textSize = 15f

            isAllCaps = false

            setTextColor(NAVY)

            gravity =
                Gravity.CENTER

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

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


    // ============================================================
    // جلب الممرض الحالي
    // ============================================================

    private fun loadNurse() {

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()

        if (user == null) {

            Toast.makeText(
                this,
                "يجب تسجيل الدخول كممرض أولاً",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }

        currentUserId =
            user.id

        // ============================================================
        // FCM: الحصول على رمز جهاز الممرض
        // ============================================================
        // يتم الحصول على Token الجهاز هنا فقط.
        // حفظه في قاعدة البيانات سيتم بعد تجهيز جدول/سياسة RLS الخاصة
        // بتوكنات الإشعارات، حتى لا نغيّر بنية Supabase الحالية عشوائياً.
        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result

                    // الاحتفاظ بالتوكن محلياً مؤقتاً.
                    // يمكن استخدامه لاحقاً عند ربط جدول notification_tokens.
                    getSharedPreferences(
                        "tamreed_fcm",
                        MODE_PRIVATE
                    ).edit()
                        .putString("nurse_fcm_token", token)
                        .apply()
                }
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
                        .decodeList<NurseRecordForRequests>()

                val nurse =
                    nurses.firstOrNull()

                if (nurse == null) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "لم يتم العثور على سجل الممرض في جدول nurses",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()

                    return@launch
                }

                if (nurse.id.isNullOrBlank()) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "معرف الممرض فارغ",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()

                    return@launch
                }

                /*
                 * مهم جداً:
                 *
                 * nurseId = nurses.id
                 *
                 * وليس auth user id
                 */

                nurseId =
                    nurse.id

                loadServices()

                loadRequests()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "خطأ في تحميل بيانات الممرض:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ============================================================
    // FCM Token الحالي
    // ============================================================

    private fun currentNurseFcmToken(): String? {
        return getSharedPreferences(
            "tamreed_fcm",
            MODE_PRIVATE
        ).getString(
            "nurse_fcm_token",
            null
        )
    }


    // ============================================================
    // تحميل الخدمات
    // ============================================================

    private fun loadServices() {

        scope.launch {

            try {

                val services =
                    SupabaseManager
                        .client
                        .from("services")
                        .select()
                        .decodeList<NurseServiceForRequests>()

                serviceNames.clear()

                services.forEach { service ->

                    val id =
                        service.id

                    val name =
                        service.name_ar
                            ?: service.name

                    if (
                        !id.isNullOrBlank() &&
                        !name.isNullOrBlank()
                    ) {

                        serviceNames[id] =
                            name
                    }
                }

            } catch (e: Exception) {

                serviceNames.clear()
            }
        }
    }


    // ============================================================
    // اسم الخدمة
    // ============================================================

    private fun serviceName(
        serviceId: String?
    ): String {

        if (serviceId.isNullOrBlank()) {
            return "تمريض منزلي"
        }

        return serviceNames[serviceId]
            ?: serviceId
    }


    // ============================================================
    // تحميل الطلبات
    // ============================================================

    private fun loadRequests() {

        scope.launch {

            try {

                val bookings =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select()
                        .decodeList<NurseRequestsBooking>()

                val dbNurseId =
                    nurseId

                val authId =
                    currentUserId

                /*
                 * نعرض:
                 *
                 * 1 - الطلبات الجديدة التي لم يتم تعيين
                 *     ممرض لها.
                 *
                 * 2 - الطلبات التي nurse_id فيها يساوي
                 *     nurses.id.
                 *
                 * 3 - حماية إضافية إذا كان المشروع القديم
                 *     يخزن auth user id.
                 */

                val visibleBookings =
                    bookings
                        .filter { booking ->

                            val bookingNurseId =
                                booking.nurse_id

                            bookingNurseId.isNullOrBlank() ||
                            bookingNurseId == dbNurseId ||
                            bookingNurseId == authId
                        }
                        .sortedByDescending {

                            it.created_at ?: ""
                        }

                showRequests(
                    visibleBookings
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر تحميل طلبات المرضى:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                showRequests(
                    emptyList()
                )
            }
        }
    }


    // ============================================================
    // عرض الشاشة
    // ============================================================

    private fun showRequests(
        requests: List<NurseRequestsBooking>
    ) {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setBackgroundColor(BG)

                setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(30)
                )
            }

        val scroll =
            ScrollView(this).apply {

                isFillViewport =
                    true

                addView(root)
            }

        setContentView(scroll)


        // ========================================================
        // الرأس
        // ========================================================

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }


        val back =
            outlineButton("رجوع") {

                finish()
            }

        header.addView(
            back,
            LinearLayout.LayoutParams(
                dp(90),
                dp(52)
            )
        )


        header.addView(
            txt(
                "طلبات المرضى",
                25f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                dp(60),
                1f
            )
        )


        val refresh =
            outlineButton("↻") {

                loadRequests()
            }

        refresh.textSize =
            22f

        header.addView(
            refresh,
            LinearLayout.LayoutParams(
                dp(55),
                dp(52)
            )
        )


        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )


        // ========================================================
        // العدد
        // ========================================================

        root.addView(
            txt(
                "عدد الطلبات: ${requests.size}",
                19f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )


        // ========================================================
        // لا توجد طلبات
        // ========================================================

        if (requests.isEmpty()) {

            val empty =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    background =
                        rounded(
                            BLUE,
                            22
                        )

                    setPadding(
                        dp(20),
                        dp(30),
                        dp(20),
                        dp(30)
                    )
                }


            empty.addView(
                txt(
                    "📋",
                    50f,
                    NAVY
                )
            )


            empty.addView(
                txt(
                    "لا توجد طلبات حالياً",
                    22f,
                    NAVY,
                    true
                )
            )


            empty.addView(
                txt(
                    "ستظهر هنا طلبات المرضى الجديدة والمقبولة",
                    15f,
                    GRAY
                )
            )


            root.addView(
                empty,
                LinearLayout.LayoutParams(
                    -1,
                    dp(230)
                )
            )

            return
        }


        // ========================================================
        // البطاقات
        // ========================================================

        requests.forEach { booking ->

            root.addView(
                requestCard(booking),
                LinearLayout.LayoutParams(
                    -1,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    bottomMargin =
                        dp(16)
                }
            )
        }
    }


    // ============================================================
    // بطاقة الطلب
    // ============================================================

    private fun requestCard(
        booking: NurseRequestsBooking
    ): LinearLayout {

        val status =
            booking.status
                ?.uppercase()
                ?: "PENDING"


        val isAssignedToThisNurse =
            booking.nurse_id == nurseId ||
            booking.nurse_id == currentUserId


        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    bordered(
                        WHITE,
                        Color.rgb(
                            215,
                            225,
                            232
                        ),
                        20
                    )

                elevation =
                    dp(2).toFloat()

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(15)
                )
            }


        card.addView(
            txt(
                "🩺  طلب تمريض منزلي",
                21f,
                NAVY,
                true
            )
        )


        // ========================================================
        // الحالة
        // ========================================================

        val statusText =
            when {

                status == "ACCEPTED" &&
                isAssignedToThisNurse ->
                    "🟢 تم قبول الطلب"

                status == "ON_THE_WAY" &&
                isAssignedToThisNurse ->
                    "🚗 الممرض في الطريق إلى المريض"

                status == "IN_PROGRESS" &&
                isAssignedToThisNurse ->
                    "🔵 الزيارة جارية الآن"

                status == "PENDING" &&
                booking.nurse_id.isNullOrBlank() ->
                    "🟠 طلب جديد بانتظار الممرض"

                status == "COMPLETED" ->
                    "✓ تم إكمال الزيارة"

                status == "CANCELLED" ->
                    "🔴 تم إلغاء الطلب"

                else ->
                    "حالة الطلب: $status"
            }


        val statusColor =
            when {

                status == "ACCEPTED" ->
                    GREEN

                status == "IN_PROGRESS" ->
                    NAVY

                status == "COMPLETED" ->
                    GREEN

                status == "CANCELLED" ->
                    RED

                else ->
                    ORANGE
            }


        card.addView(
            txt(
                statusText,
                16f,
                statusColor,
                true
            )
        )


        addRow(
            card,
            "رقم الطلب",
            booking.id ?: "-"
        )


        addRow(
            card,
            "رقم المريض",
            booking.patient_phone ?: "-"
        )


        addRow(
            card,
            "الخدمة",
            serviceName(
                booking.service_id
            )
        )


        addRow(
            card,
            "المدينة",
            booking.city ?: "الأنبار"
        )


        addRow(
            card,
            "العنوان",
            booking.address ?: "-"
        )


        addRow(
            card,
            "النقطة الدالة",
            booking.landmark ?: "-"
        )


        if (
            !booking.notes.isNullOrBlank()
        ) {

            addRow(
                card,
                "الملاحظات",
                booking.notes ?: "-"
            )
        }


        // ========================================================
        // الموقع
        // ========================================================

        if (
            booking.latitude != null &&
            booking.longitude != null
        ) {

            addRow(
                card,
                "موقع المريض",
                "${booking.latitude}, ${booking.longitude}"
            )


            val mapButton =
                primaryButton(
                    "📍 فتح موقع المريض على الخريطة",
                    NAVY
                ) {

                    openPatientLocation(
                        booking.latitude,
                        booking.longitude
                    )
                }


            card.addView(
                mapButton,
                LinearLayout.LayoutParams(
                    -1,
                    dp(55)
                ).apply {

                    topMargin =
                        dp(8)
                }
            )
        }


        // ========================================================
        // زر القبول
        // ========================================================

        if (
            status == "PENDING" &&
            booking.nurse_id.isNullOrBlank()
        ) {

            val acceptButton =
                primaryButton(
                    "✓ قبول طلب المريض",
                    GREEN
                ) {

                    acceptBooking(
                        booking
                    )
                }


            card.addView(
                acceptButton,
                LinearLayout.LayoutParams(
                    -1,
                    dp(58)
                ).apply {

                    topMargin =
                        dp(10)
                }
            )
        }


        // ========================================================
        // الطلب المقبول ومراحل تنفيذ الزيارة
        // ========================================================

        if (
            isAssignedToThisNurse &&
            status != "CANCELLED" &&
            status != "COMPLETED"
        ) {

            val detailsButton =
                primaryButton(
                    "📍 فتح موقع المريض على الخريطة",
                    NAVY
                ) {

                    openPatientLocation(
                        booking.latitude,
                        booking.longitude
                    )
                }

            card.addView(
                detailsButton,
                LinearLayout.LayoutParams(
                    -1,
                    dp(56)
                ).apply {
                    topMargin = dp(10)
                }
            )

            when (status) {

                "ACCEPTED" -> {
                    val button =
                        primaryButton(
                            "🚗 أنا في الطريق إلى المريض",
                            ORANGE
                        ) {
                            updateBookingStatus(
                                booking,
                                "ON_THE_WAY"
                            )
                        }

                    card.addView(
                        button,
                        LinearLayout.LayoutParams(
                            -1,
                            dp(56)
                        ).apply {
                            topMargin = dp(8)
                        }
                    )
                }

                "ON_THE_WAY" -> {
                    val button =
                        primaryButton(
                            "▶ بدء الزيارة",
                            GREEN
                        ) {
                            updateBookingStatus(
                                booking,
                                "IN_PROGRESS"
                            )
                        }

                    card.addView(
                        button,
                        LinearLayout.LayoutParams(
                            -1,
                            dp(56)
                        ).apply {
                            topMargin = dp(8)
                        }
                    )
                }

                "IN_PROGRESS" -> {
                    val button =
                        primaryButton(
                            "✓ إكمال الزيارة",
                            GREEN
                        ) {
                            updateBookingStatus(
                                booking,
                                "COMPLETED"
                            )
                        }

                    card.addView(
                        button,
                        LinearLayout.LayoutParams(
                            -1,
                            dp(56)
                        ).apply {
                            topMargin = dp(8)
                        }
                    )
                }
            }
        }


        return card
    }


    // ============================================================
    // إضافة صف
    // ============================================================

    private fun addRow(
        parent: LinearLayout,
        title: String,
        value: String
    ) {

        val row =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setPadding(
                    0,
                    dp(3),
                    0,
                    dp(3)
                )
            }


        row.addView(
            txt(
                "$title:",
                15f,
                GRAY,
                true
            ),
            LinearLayout.LayoutParams(
                dp(125),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )


        row.addView(
            txt(
                value,
                15f,
                TEXT,
                false
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )


        parent.addView(
            row
        )
    }


    // ============================================================
    // قبول الطلب
    // ============================================================

    private fun acceptBooking(
        booking: NurseRequestsBooking
    ) {

        val dbNurseId = nurseId
        val bookingId = booking.id
        val serviceId = booking.service_id

        if (dbNurseId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "تعذر تحديد معرف الممرض",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (bookingId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "رقم الطلب غير صالح",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (serviceId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "الطلب لا يحتوي على service_id صالح",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        scope.launch {
            try {
                // ----------------------------------------------------
                // 1) التأكد أن nurses.id موجود فعلاً
                //    لأن bookings.nurse_id مرتبط بـ nurses.id
                // ----------------------------------------------------
                val nurseExists =
                    SupabaseManager
                        .client
                        .from("nurses")
                        .select {
                            filter {
                                eq("id", dbNurseId)
                            }
                        }
                        .decodeList<NurseRecordForRequests>()
                        .isNotEmpty()

                if (!nurseExists) {
                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "خطأ: معرف الممرض غير موجود في جدول nurses",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // ----------------------------------------------------
                // 2) التأكد أن service_id موجود في services
                //    لأن bookings.service_id مرتبط بـ services.id
                // ----------------------------------------------------
                val serviceExists =
                    SupabaseManager
                        .client
                        .from("services")
                        .select {
                            filter {
                                eq("id", serviceId)
                            }
                        }
                        .decodeList<NurseServiceForRequests>()
                        .isNotEmpty()

                if (!serviceExists) {
                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "تعذر قبول الطلب: الخدمة المرتبطة بهذا الطلب غير موجودة في جدول services",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // ----------------------------------------------------
                // 3) قبول الطلب باستخدام nurses.id وليس Auth user id
                // ----------------------------------------------------
                val assignment =
                    NurseBookingAssignment(
                        nurse_id = dbNurseId,
                        status = "ACCEPTED"
                    )

                SupabaseManager
                    .client
                    .from("bookings")
                    .update(assignment) {
                        filter {
                            eq("id", bookingId)
                            eq("status", "PENDING")
                        }
                    }

                // ----------------------------------------------------
                // 4) التحقق من أن التحديث تم فعلاً
                // ----------------------------------------------------
                val updated =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select {
                            filter {
                                eq("id", bookingId)
                            }
                        }
                        .decodeList<NurseRequestsBooking>()
                        .firstOrNull()

                if (updated?.nurse_id == dbNurseId &&
                    updated.status?.uppercase() == "ACCEPTED"
                ) {
                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "✓ تم قبول طلب المريض بنجاح",
                        Toast.LENGTH_LONG
                    ).show()

                    loadRequests()
                } else {
                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "لم يتم تحديث الطلب. تحقق من صلاحيات RLS في جدول bookings",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                val message = e.message ?: "خطأ غير معروف"

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر قبول الطلب:\n$message",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ============================================================
    // تحديث مرحلة الطلب
    // ============================================================

    private fun updateBookingStatus(
        booking: NurseRequestsBooking,
        newStatus: String
    ) {

        val dbNurseId = nurseId
        val bookingId = booking.id

        if (dbNurseId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "تعذر تحديد معرف الممرض",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (bookingId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "رقم الطلب غير صالح",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        scope.launch {

            try {

                // لا نسمح بتحديث طلب ممرض آخر.
                val current =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select {
                            filter {
                                eq("id", bookingId)
                                eq("nurse_id", dbNurseId)
                            }
                        }
                        .decodeList<NurseRequestsBooking>()
                        .firstOrNull()

                if (current == null) {
                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "لا يمكن تحديث هذا الطلب لأنه غير مرتبط بهذا الممرض",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val storedStatus =
                    current.status
                        ?.takeIf { it.isNotBlank() }
                        ?: "PENDING"

                val currentStatus =
                    storedStatus.uppercase()

                val allowed =
                    when (currentStatus) {
                        "ACCEPTED" ->
                            newStatus == "ON_THE_WAY"

                        "ON_THE_WAY" ->
                            newStatus == "IN_PROGRESS"

                        "IN_PROGRESS" ->
                            newStatus == "COMPLETED"

                        else ->
                            false
                    }

                if (!allowed) {
                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "لا يمكن الانتقال من ${statusTextForUpdate(currentStatus)} إلى ${statusTextForUpdate(newStatus)}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                SupabaseManager
                    .client
                    .from("bookings")
                    .update(
                        NurseBookingStatusUpdate(
                            status = newStatus
                        )
                    ) {
                        filter {
                            eq("id", bookingId)
                            eq("nurse_id", dbNurseId)
                            eq("status", storedStatus)
                        }
                    }

                val updated =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select {
                            filter {
                                eq("id", bookingId)
                                eq("nurse_id", dbNurseId)
                            }
                        }
                        .decodeList<NurseRequestsBooking>()
                        .firstOrNull()

                if (
                    updated?.status
                        ?.uppercase() == newStatus
                ) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "✓ تم تحديث حالة الطلب إلى ${statusTextForUpdate(newStatus)}",
                        Toast.LENGTH_LONG
                    ).show()

                    loadRequests()

                } else {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "لم يتم تحديث حالة الطلب. تحقق من صلاحيات RLS في جدول bookings",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر تحديث حالة الطلب:\n${e.message ?: "خطأ غير معروف"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun statusTextForUpdate(
        status: String
    ): String {

        return when (status.uppercase()) {

            "ACCEPTED" ->
                "تم القبول"

            "ON_THE_WAY" ->
                "في الطريق"

            "IN_PROGRESS" ->
                "الزيارة جارية"

            "COMPLETED" ->
                "مكتملة"

            "CANCELLED" ->
                "ملغاة"

            "PENDING" ->
                "قيد الانتظار"

            else ->
                status
        }
    }


    // ============================================================
    // فتح موقع المريض
    // ============================================================

    private fun openPatientLocation(
        latitude: Double?,
        longitude: Double?
    ) {

        if (
            latitude == null ||
            longitude == null
        ) {

            Toast.makeText(
                this,
                "موقع المريض غير متوفر",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val uri =
            Uri.parse(
                "google.navigation:q=$latitude,$longitude"
            )


        val navigationIntent =
            Intent(
                Intent.ACTION_VIEW,
                uri
            ).apply {

                setPackage(
                    "com.google.android.apps.maps"
                )
            }


        try {

            startActivity(
                navigationIntent
            )

        } catch (e: Exception) {

            val webUri =
                Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"
                )


            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    webUri
                )
            )
        }
    }
}
