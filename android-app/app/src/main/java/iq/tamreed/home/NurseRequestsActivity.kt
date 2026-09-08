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
import io.github.jan.supabase.postgrest.from
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

/*
 * سجل الممرض من جدول nurses
 */
@Serializable
data class NurseRecordForRequests(
    val id: String? = null,
    val user_id: String? = null
)

@Serializable
data class NurseServiceRecord(
    val id: String? = null,
    val name_ar: String? = null
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

    /*
     * ID الخاص بالممرض داخل جدول nurses
     */
    private var nurseId: String? = null

    /*
     * ID الخاص بحساب Supabase Auth
     */
    private var currentUserId: String? = null

    private var serviceNames: Map<String, String> = emptyMap()

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

    /*
     * ============================================================
     * جلب الممرض الحالي
     * ============================================================
     */
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

                /*
                 * نبحث في جدول nurses
                 * عن الممرض الذي user_id الخاص به
                 * يساوي حساب Supabase الحالي.
                 */

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

    /*
     * ============================================================
     * تحميل الطلبات
     * ============================================================
     */
    private fun loadRequests() {

        scope.launch {

            try {

                val bookings =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select()
                        .decodeList<NurseRequestsBooking>()

                /*
                 * الطلبات الجديدة:
                 *
                 * nurse_id فارغ
                 *
                 * أو الطلبات المقبولة لهذا الممرض.
                 */

                val visibleBookings =
                    bookings
                        .filter { booking ->

                            booking.nurse_id.isNullOrBlank() ||
                            booking.nurse_id == nurseId ||
                            booking.nurse_id == currentUserId
                        }
                        .sortedByDescending {

                            it.created_at ?: ""
                        }

                serviceNames = try {
                    SupabaseManager
                        .client
                        .from("services")
                        .select()
                        .decodeList<NurseServiceRecord>()
                        .filter { !it.id.isNullOrBlank() && !it.name_ar.isNullOrBlank() }
                        .associate { it.id!! to it.name_ar!! }
                } catch (_: Exception) {
                    emptyMap()
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

    /*
     * ============================================================
     * عرض الطلبات
     * ============================================================
     */
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

        /*
         * رأس الشاشة
         */

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

        /*
         * عدد الطلبات
         */

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

        /*
         * لا توجد طلبات
         */

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
                    "ستظهر هنا طلبات المرضى الجديدة",
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

        /*
         * عرض الطلبات
         */

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

    /*
     * ============================================================
     * بطاقة الطلب
     * ============================================================
     */
    private fun requestCard(
        booking: NurseRequestsBooking
    ): LinearLayout {

        val status = booking.status?.uppercase() ?: "PENDING"
        val accepted =
            booking.nurse_id == nurseId ||
            booking.nurse_id == currentUserId

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bordered(WHITE, Color.rgb(215, 225, 232), 20)
            elevation = dp(2).toFloat()
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val title = serviceNames[booking.service_id] ?: "خدمة تمريض منزلي"
        card.addView(txt(title, 20f, NAVY, true))

        val statusLabel = when {
            status == "ACCEPTED" && accepted -> "تم قبول الطلب"
            status == "PENDING" && booking.nurse_id.isNullOrBlank() -> "طلب جديد بانتظار القبول"
            status == "ON_THE_WAY" && accepted -> "أنت في الطريق إلى المريض"
            status == "IN_PROGRESS" && accepted -> "الزيارة جارية"
            status == "COMPLETED" -> "تم إكمال الطلب"
            status == "CANCELLED" -> "تم إلغاء الطلب"
            else -> "حالة الطلب: $status"
        }

        val statusColor = when (status) {
            "ACCEPTED", "ON_THE_WAY" -> GREEN
            "IN_PROGRESS", "COMPLETED" -> GREEN
            "CANCELLED" -> RED
            else -> ORANGE
        }

        card.addView(txt(statusLabel, 14f, statusColor, true))

        addRow(card, "رقم الطلب", booking.id ?: "-")
        addRow(card, "رقم المريض", booking.patient_phone ?: "-")
        addRow(card, "المدينة", booking.city ?: "الأنبار")
        addRow(card, "العنوان", booking.address ?: "-")

        if (!booking.notes.isNullOrBlank()) {
            addRow(card, "الملاحظات", booking.notes ?: "-")
        }

        if (booking.latitude != null && booking.longitude != null) {
            addRow(
                card,
                "موقع المريض",
                "${booking.latitude}, ${booking.longitude}"
            )

            val mapButton = Button(this).apply {
                text = "فتح موقع المريض على الخريطة"
                textSize = 15f
                isAllCaps = false
                setTextColor(WHITE)
                background = rounded(NAVY, 15)
                setOnClickListener {
                    openPatientLocation(booking.latitude, booking.longitude)
                }
            }

            card.addView(
                mapButton,
                LinearLayout.LayoutParams(-1, dp(50)).apply {
                    topMargin = dp(7)
                }
            )
        }

        if (!booking.patient_phone.isNullOrBlank() && accepted) {
            val callButton = Button(this).apply {
                text = "اتصال بالمريض"
                textSize = 15f
                isAllCaps = false
                setTextColor(NAVY)
                background = bordered(WHITE, NAVY, 15)
                setOnClickListener {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${booking.patient_phone}")
                            )
                        )
                    } catch (_: Exception) {
                        Toast.makeText(
                            this@NurseRequestsActivity,
                            "تعذر فتح الاتصال",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            card.addView(
                callButton,
                LinearLayout.LayoutParams(-1, dp(50)).apply {
                    topMargin = dp(7)
                }
            )
        }

        if (status == "PENDING" && booking.nurse_id.isNullOrBlank()) {
            val accept = Button(this).apply {
                text = "قبول طلب المريض"
                textSize = 16f
                isAllCaps = false
                setTextColor(WHITE)
                background = rounded(GREEN, 15)
                setOnClickListener {
                    isEnabled = false
                    acceptBooking(booking)
                }
            }

            card.addView(
                accept,
                LinearLayout.LayoutParams(-1, dp(54)).apply {
                    topMargin = dp(9)
                }
            )
        }

        if (accepted && !booking.id.isNullOrBlank() && !booking.patient_id.isNullOrBlank()) {
            val chatButton = Button(this).apply {
                text = "المحادثة مع المريض"
                textSize = 16f
                isAllCaps = false
                setTextColor(WHITE)
                background = rounded(NAVY, 15)
                setOnClickListener { openPatientChat(booking) }
            }

            card.addView(
                chatButton,
                LinearLayout.LayoutParams(-1, dp(54)).apply {
                    topMargin = dp(8)
                }
            )
        }

        return card
    }

    private fun addStatusButton(
        card: LinearLayout,
        label: String,
        color: Int,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setTextColor(WHITE)
            background = rounded(color, 15)
            setOnClickListener { action() }
        }
        card.addView(
            button,
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(8)
            }
        )
    }

    private fun openPatientLocation(latitude: Double?, longitude: Double?) {
        if (latitude == null || longitude == null) {
            Toast.makeText(this, "موقع المريض غير متوفر", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح الخرائط", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPatientChat(booking: NurseRequestsBooking) {
        val bookingId = booking.id?.trim().orEmpty()
        val patientId = booking.patient_id?.trim().orEmpty()

        if (bookingId.isBlank() || patientId.isBlank()) {
            Toast.makeText(this, "بيانات المحادثة غير مكتملة", Toast.LENGTH_SHORT).show()
            return
        }

        startActivity(
            Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_BOOKING_ID, bookingId)
                putExtra(ChatActivity.EXTRA_RECEIVER_ID, patientId)
                putExtra(ChatActivity.EXTRA_RECEIVER_NAME, "المريض")
            }
        )
    }

    /*
     * ============================================================
     * صف بيانات
     * ============================================================
     */
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

    /*
     * ============================================================
     * قبول الطلب
     *
     * الإصلاح الأساسي هنا
     * ============================================================
     */
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

                /*
                 * ------------------------------------------------
                 * الخطوة 1:
                 * قراءة الطلب قبل القبول
                 * ------------------------------------------------
                 */

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

                /*
                 * ------------------------------------------------
                 * الطلب مقبول مسبقاً
                 * ------------------------------------------------
                 */

                if (
                    currentBooking.nurse_id ==
                    databaseNurseId &&
                    currentBooking.status
                        ?.uppercase() == "ACCEPTED"
                ) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "هذا الطلب مقبول مسبقاً ✓",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadRequests()

                    return@launch
                }

                /*
                 * ------------------------------------------------
                 * الطلب أخذه ممرض آخر
                 * ------------------------------------------------
                 */

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

                /*
                 * ------------------------------------------------
                 * يجب أن يكون الطلب PENDING
                 * ------------------------------------------------
                 */

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

                /*
                 * =================================================
                 * المحاولة الأولى
                 *
                 * bookings.nurse_id -> nurses.id
                 * =================================================
                 */

                var accepted = false

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

                    accepted = true

                } catch (e: Exception) {

                    firstError =
                        e.message
                            ?: "خطأ غير معروف"
                }

                /*
                 * =================================================
                 * المحاولة الثانية
                 *
                 * في حال كان الـ Foreign Key يشير إلى
                 * auth.users.id بدلاً من nurses.id
                 * =================================================
                 */

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

                        accepted = true

                    } catch (e: Exception) {

                        val secondError =
                            e.message
                                ?: "خطأ غير معروف"

                        /*
                         * ------------------------------------------------
                         * كلا الاحتمالين فشلا.
                         * نعرض الخطأ الحقيقي حتى نعرف نوع الـ FK.
                         * ------------------------------------------------
                         */

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

                        return@launch
                    }
                }

                /*
                 * =================================================
                 * نجح القبول
                 * =================================================
                 */

                if (accepted) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "تم قبول طلب المريض بنجاح ✓",
                        Toast.LENGTH_SHORT
                    ).show()

                    /*
                     * إعادة تحميل الطلبات
                     */

                    loadRequests()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,

                    "حدث خطأ أثناء قبول الطلب:\n${e.message}",

                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
