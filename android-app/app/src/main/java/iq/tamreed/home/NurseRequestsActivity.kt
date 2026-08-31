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
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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

@Serializable
data class NurseBookingAssignment(
    val nurse_id: String,
    val status: String
)

@Serializable
data class NurseRecordForRequests(
    val id: String? = null,
    val user_id: String? = null
)

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

    // ID الممرض داخل جدول nurses
    private var nurseId: String? = null

    // ID حساب Supabase Auth
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadNurse()
    }

    override fun onResume() {
        super.onResume()

        if (!nurseId.isNullOrBlank()) {
            loadRequests()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
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
        strokeColor: Int = Color.rgb(215, 225, 232),
        radius: Int = 16
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
        }
    }

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

            gravity = Gravity.CENTER

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

        currentUserId = user.id

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
                        "معرف الممرض في جدول nurses فارغ",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@launch
                }

                nurseId = nurse.id

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
                 * الطلبات الظاهرة للممرض:
                 *
                 * 1- طلب جديد لم يتم تعيين ممرض له.
                 *
                 * 2- طلب مرتبط بـ nurses.id.
                 *
                 * 3- طلب مرتبط بـ auth.users.id.
                 *
                 * بهذه الطريقة لا تختفي الطلبات المقبولة
                 * بسبب اختلاف نوع الـ Foreign Key.
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
    // عرض الطلبات
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

                isFillViewport = true

                addView(root)
            }

        setContentView(scroll)

        // ========================================================
        // رأس الشاشة
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
            Button(this).apply {

                text = "رجوع"

                textSize = 15f

                isAllCaps = false

                setTextColor(NAVY)

                background =
                    bordered(
                        WHITE,
                        NAVY,
                        14
                    )

                setOnClickListener {
                    finish()
                }
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
            Button(this).apply {

                text = "↻"

                textSize = 22f

                isAllCaps = false

                setTextColor(NAVY)

                background =
                    bordered(
                        WHITE,
                        NAVY,
                        14
                    )

                setOnClickListener {
                    loadRequests()
                }
            }

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
        // عدد الطلبات
        // ========================================================

        root.addView(
            txt(
                "عدد الطلبات: ${requests.size}",
                18f,
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
        // عرض الطلبات
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

        val accepted =
            booking.nurse_id == nurseId ||
            booking.nurse_id == currentUserId ||
            booking.status
                ?.uppercase() == "ACCEPTED"

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    bordered(
                        WHITE,
                        Color.rgb(215, 225, 232),
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

        val status =
            booking.status
                ?.uppercase()
                ?: "PENDING"

        val statusText =
            when {

                accepted &&
                status == "ACCEPTED" ->
                    "✓ تم قبول الطلب"

                status == "PENDING" &&
                booking.nurse_id.isNullOrBlank() ->
                    "🟠 طلب جديد بانتظار الممرض"

                status == "COMPLETED" ->
                    "✓ تم إكمال الطلب"

                status == "CANCELLED" ->
                    "🔴 تم إلغاء الطلب"

                else ->
                    "حالة الطلب: $status"
            }

        val statusColor =
            when {

                accepted &&
                status == "ACCEPTED" ->
                    GREEN

                status == "CANCELLED" ->
                    RED

                else ->
                    ORANGE
            }

        card.addView(
            txt(
                statusText,
                15f,
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
            booking.service_id ?: "-"
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

        if (!booking.notes.isNullOrBlank()) {

            addRow(
                card,
                "الملاحظات",
                booking.notes ?: "-"
            )
        }

        // ========================================================
        // موقع المريض
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
                Button(this).apply {

                    text =
                        "📍 فتح موقع المريض على الخريطة"

                    textSize =
                        16f

                    isAllCaps =
                        false

                    setTextColor(
                        WHITE
                    )

                    background =
                        rounded(
                            NAVY,
                            16
                        )

                    setOnClickListener {

                        openPatientLocation(
                            booking.latitude,
                            booking.longitude
                        )
                    }
                }

            card.addView(
                mapButton,
                LinearLayout.LayoutParams(
                    -1,
                    dp(58)
                ).apply {
                    topMargin = dp(12)
                }
            )
        } else {

            card.addView(
                txt(
                    "⚠️ لم يتم تحديد موقع GPS للمريض",
                    14f,
                    RED,
                    true
                )
            )
        }

        addRow(
            card,
            "الحالة",
            status
        )

        // ========================================================
        // زر قبول الطلب
        // ========================================================

        if (
            !accepted &&
            booking.nurse_id.isNullOrBlank() &&
            status == "PENDING"
        ) {

            val accept =
                Button(this).apply {

                    text =
                        "✓ قبول طلب المريض"

                    textSize =
                        17f

                    isAllCaps =
                        false

                    setTextColor(
                        WHITE
                    )

                    background =
                        rounded(
                            GREEN,
                            16
                        )

                    setOnClickListener {

                        isEnabled =
                            false

                        acceptBooking(
                            booking
                        )
                    }
                }

            card.addView(
                accept,
                LinearLayout.LayoutParams(
                    -1,
                    dp(60)
                ).apply {

                    topMargin =
                        dp(12)
                }
            )
        }

        // ========================================================
        // إذا كان الطلب مقبولاً
        // ========================================================

        if (
            accepted &&
            status == "ACCEPTED"
        ) {

            card.addView(
                txt(
                    "✓ هذا الطلب مسند إليك",
                    16f,
                    GREEN,
                    true
                )
            )
        }

        return card
    }

    // ============================================================
    // صف بيانات
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
            }

        row.addView(
            txt(
                "$title:",
                14f,
                GRAY,
                true
            ),
            LinearLayout.LayoutParams(
                dp(105),
                dp(45)
            )
        )

        row.addView(
            txt(
                value,
                14f,
                TEXT
            ),
            LinearLayout.LayoutParams(
                0,
                dp(45),
                1f
            )
        )

        parent.addView(row)
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
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val uri =
                Uri.parse(
                    "google.navigation:q=$latitude,$longitude"
                )

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                ).apply {

                    setPackage(
                        "com.google.android.apps.maps"
                    )
                }

            startActivity(intent)

        } catch (e: Exception) {

            try {

                val uri =
                    Uri.parse(
                        "geo:$latitude,$longitude?q=$latitude,$longitude"
                    )

                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        uri
                    )

                startActivity(intent)

            } catch (e2: Exception) {

                Toast.makeText(
                    this,
                    "لا يوجد تطبيق خرائط مثبت على الجهاز",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ============================================================
    // قبول الطلب
    // ============================================================

    private fun acceptBooking(
        booking: NurseRequestsBooking
    ) {

        val bookingId =
            booking.id

        val databaseNurseId =
            nurseId

        val authUserId =
            currentUserId

        if (bookingId.isNullOrBlank()) {

            Toast.makeText(
                this,
                "رقم الطلب غير موجود",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (databaseNurseId.isNullOrBlank()) {

            Toast.makeText(
                this,
                "معرف الممرض غير موجود",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (authUserId.isNullOrBlank()) {

            Toast.makeText(
                this,
                "معرف حساب الممرض غير موجود",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        scope.launch {

            try {

                // =================================================
                // 1. قراءة الطلب الحالي
                // =================================================

                val currentBooking =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select {

                            filter {

                                eq(
                                    "id",
                                    bookingId
                                )
                            }
                        }
                        .decodeList<NurseRequestsBooking>()
                        .firstOrNull()

                if (currentBooking == null) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "الطلب غير موجود في قاعدة البيانات",
                        Toast.LENGTH_LONG
                    ).show()

                    loadRequests()
                    return@launch
                }

                // =================================================
                // 2. إذا كان مقبولاً بالفعل لهذا الممرض
                // =================================================

                if (
                    currentBooking.status
                        ?.uppercase() == "ACCEPTED" &&
                    (
                        currentBooking.nurse_id ==
                        databaseNurseId ||
                        currentBooking.nurse_id ==
                        authUserId
                    )
                ) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "هذا الطلب مقبول مسبقاً ✓",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadRequests()
                    return@launch
                }

                // =================================================
                // 3. إذا كان أخذه ممرض آخر
                // =================================================

                if (
                    !currentBooking.nurse_id.isNullOrBlank() &&
                    currentBooking.nurse_id != databaseNurseId &&
                    currentBooking.nurse_id != authUserId
                ) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "تم قبول هذا الطلب من ممرض آخر",
                        Toast.LENGTH_LONG
                    ).show()

                    loadRequests()
                    return@launch
                }

                // =================================================
                // 4. يجب أن يكون PENDING
                // =================================================

                if (
                    !currentBooking.status.isNullOrBlank() &&
                    currentBooking.status
                        ?.uppercase() != "PENDING"
                ) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "الطلب لم يعد بانتظار القبول",
                        Toast.LENGTH_LONG
                    ).show()

                    loadRequests()
                    return@launch
                }

                // =================================================
                // 5. محاولة استخدام nurses.id
                // =================================================

                var accepted =
                    false

                var firstError =
                    ""

                try {

                    SupabaseManager
                        .client
                        .from("bookings")
                        .update(
                            NurseBookingAssignment(
                                nurse_id =
                                    databaseNurseId,
                                status =
                                    "ACCEPTED"
                            )
                        ) {

                            filter {

                                eq(
                                    "id",
                                    bookingId
                                )

                                eq(
                                    "status",
                                    "PENDING"
                                )
                            }
                        }

                    // لا نكتفي بعدم وجود Exception.
                    // نقرأ الطلب مرة أخرى ونتحقق فعلياً.

                    val verified =
                        SupabaseManager
                            .client
                            .from("bookings")
                            .select {

                                filter {

                                    eq(
                                        "id",
                                        bookingId
                                    )
                                }
                            }
                            .decodeList<NurseRequestsBooking>()
                            .firstOrNull()

                    if (
                        verified != null &&
                        verified.status
                            ?.uppercase() == "ACCEPTED" &&
                        verified.nurse_id ==
                        databaseNurseId
                    ) {

                        accepted = true
                    }

                } catch (e: Exception) {

                    firstError =
                        e.message
                            ?: "خطأ غير معروف"
                }

                // =================================================
                // 6. إذا فشلت المحاولة الأولى
                // نجرب auth.users.id
                // =================================================

                if (!accepted) {

                    try {

                        SupabaseManager
                            .client
                            .from("bookings")
                            .update(
                                NurseBookingAssignment(
                                    nurse_id =
                                        authUserId,
                                    status =
                                        "ACCEPTED"
                                )
                            ) {

                                filter {

                                    eq(
                                        "id",
                                        bookingId
                                    )

                                    eq(
                                        "status",
                                        "PENDING"
                                    )
                                }
                            }

                        // التحقق الحقيقي من قاعدة البيانات

                        val verified =
                            SupabaseManager
                                .client
                                .from("bookings")
                                .select {

                                    filter {

                                        eq(
                                            "id",
                                            bookingId
                                        )
                                    }
                                }
                                .decodeList<NurseRequestsBooking>()
                                .firstOrNull()

                        if (
                            verified != null &&
                            verified.status
                                ?.uppercase() == "ACCEPTED" &&
                            verified.nurse_id ==
                            authUserId
                        ) {

                            accepted = true
                        }

                    } catch (e: Exception) {

                        val secondError =
                            e.message
                                ?: "خطأ غير معروف"

                        Toast.makeText(
                            this@NurseRequestsActivity,

                            """
                            تعذر قبول الطلب.

                            المحاولة الأولى:
                            $firstError

                            المحاولة الثانية:
                            $secondError
                            """.trimIndent(),

                            Toast.LENGTH_LONG
                        ).show()

                        loadRequests()
                        return@launch
                    }
                }

                // =================================================
                // 7. النتيجة النهائية
                // =================================================

                if (accepted) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "تم قبول طلب المريض بنجاح ✓",
                        Toast.LENGTH_SHORT
                    ).show()

                    // إعادة تحميل القائمة
                    // حتى يبقى الطلب المقبول ظاهراً
                    loadRequests()

                } else {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "لم يتم تثبيت قبول الطلب في قاعدة البيانات",
                        Toast.LENGTH_LONG
                    ).show()

                    loadRequests()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "حدث خطأ أثناء قبول الطلب:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                loadRequests()
            }
        }
    }
}
