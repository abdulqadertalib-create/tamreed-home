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
import kotlinx.coroutines.*
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
data class NurseRecordForRequests(
    val id: String? = null,
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null
)

@Serializable
data class NurseServiceForRequests(
    val id: String? = null,
    val name_ar: String? = null,
    val name: String? = null
)

@Serializable
data class NurseBookingAssignment(
    val nurse_id: String,
    val status: String
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var nurseId: String? = null
    private var currentUserId: String? = null
    private val serviceNames = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadNurse()
    }

    override fun onResume() {
        super.onResume()
        if (!nurseId.isNullOrBlank()) loadRequests()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 18) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun bordered(
        color: Int = WHITE,
        strokeColor: Int = Color.rgb(215, 225, 232),
        radius: Int = 16
    ) = GradientDrawable().apply {
        setColor(color)
        setStroke(dp(1), strokeColor)
        cornerRadius = dp(radius).toFloat()
    }

    private fun txt(
        value: String,
        size: Float = 16f,
        color: Int = TEXT,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    private fun primaryButton(
        title: String,
        color: Int = GREEN,
        action: () -> Unit
    ) = Button(this).apply {
        text = title
        textSize = 16f
        isAllCaps = false
        setTextColor(WHITE)
        gravity = Gravity.CENTER
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        background = rounded(color, 15)
        setOnClickListener { action() }
    }

    private fun outlineButton(
        title: String,
        action: () -> Unit
    ) = Button(this).apply {
        text = title
        textSize = 15f
        isAllCaps = false
        setTextColor(NAVY)
        gravity = Gravity.CENTER
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        background = bordered(WHITE, NAVY, 15)
        setOnClickListener { action() }
    }

    private fun loadNurse() {
        val user = SupabaseManager.client.auth.currentUserOrNull()

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
                val nurse = SupabaseManager.client
                    .from("nurses")
                    .select { filter { eq("user_id", user.id) } }
                    .decodeList<NurseRecordForRequests>()
                    .firstOrNull()

                if (nurse?.id.isNullOrBlank()) {
                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "لم يتم العثور على سجل الممرض في جدول nurses",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                // bookings.nurse_id مرتبط بـ nurses.id وليس auth user id
                nurseId = nurse!!.id

                // مهم: تحميل الخدمات أولاً ثم الطلبات حتى يظهر اسم الخدمة.
                loadServicesAndRequests()

            } catch (e: Exception) {
                Toast.makeText(
                    this@NurseRequestsActivity,
                    "خطأ في تحميل بيانات الممرض:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadServicesAndRequests() {
        scope.launch {
            try {
                val services = SupabaseManager.client
                    .from("services")
                    .select()
                    .decodeList<NurseServiceForRequests>()

                serviceNames.clear()

                services.forEach { service ->
                    val id = service.id
                    val name = service.name_ar?.takeIf { it.isNotBlank() }
                        ?: service.name?.takeIf { it.isNotBlank() }

                    if (!id.isNullOrBlank() && !name.isNullOrBlank()) {
                        serviceNames[id] = name
                    }
                }
            } catch (_: Exception) {
                serviceNames.clear()
            }

            loadRequests()
        }
    }

    private fun loadServices() = loadServicesAndRequests()

    private fun serviceName(serviceId: String?): String {
        if (serviceId.isNullOrBlank()) return "نوع الخدمة غير محدد"
        return serviceNames[serviceId] ?: "الخدمة غير متوفرة"
    }

    private fun loadRequests() {
        scope.launch {
            try {
                val bookings = SupabaseManager.client
                    .from("bookings")
                    .select()
                    .decodeList<NurseRequestsBooking>()

                val dbNurseId = nurseId
                val authId = currentUserId

                val visible = bookings
                    .filter {
                        it.nurse_id.isNullOrBlank() ||
                        it.nurse_id == dbNurseId ||
                        it.nurse_id == authId
                    }
                    .sortedByDescending { it.created_at ?: "" }

                showRequests(visible)

            } catch (e: Exception) {
                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر تحميل طلبات المرضى:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                showRequests(emptyList())
            }
        }
    }

    private fun showRequests(requests: List<NurseRequestsBooking>) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(BG)
            setPadding(dp(14), dp(14), dp(14), dp(30))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        header.addView(
            outlineButton("رجوع") { finish() },
            LinearLayout.LayoutParams(dp(90), dp(52))
        )

        header.addView(
            txt("طلبات المرضى", 25f, NAVY, true),
            LinearLayout.LayoutParams(0, dp(60), 1f)
        )

        header.addView(
            outlineButton("↻") { loadRequests() }.apply { textSize = 22f },
            LinearLayout.LayoutParams(dp(55), dp(52))
        )

        root.addView(header, LinearLayout.LayoutParams(-1, dp(70)))

        root.addView(
            txt("عدد الطلبات: ${requests.size}", 19f, NAVY, true),
            LinearLayout.LayoutParams(-1, dp(55))
        )

        if (requests.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(BLUE, 22)
                setPadding(dp(20), dp(30), dp(20), dp(30))
            }

            empty.addView(txt("📋", 50f, NAVY))
            empty.addView(txt("لا توجد طلبات حالياً", 22f, NAVY, true))
            empty.addView(
                txt(
                    "ستظهر هنا طلبات المرضى الجديدة والمقبولة",
                    15f,
                    GRAY
                )
            )

            root.addView(empty, LinearLayout.LayoutParams(-1, dp(230)))
            return
        }

        requests.forEach { booking ->
            root.addView(
                requestCard(booking),
                LinearLayout.LayoutParams(
                    -1,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(16) }
            )
        }
    }

    private fun requestCard(booking: NurseRequestsBooking): LinearLayout {
        val status = booking.status?.uppercase() ?: "PENDING"

        val assignedToThisNurse =
            booking.nurse_id == nurseId ||
            booking.nurse_id == currentUserId

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bordered(
                WHITE,
                Color.rgb(215, 225, 232),
                20
            )
            elevation = dp(2).toFloat()
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        // التعديل 1: طلب ممرض وليس طلب تمريض
        card.addView(txt("🩺  طلب ممرض", 21f, NAVY, true))

        val statusText = when {
            status == "ACCEPTED" && assignedToThisNurse ->
                "🟢 تم قبول الطلب"
            status == "PENDING" && booking.nurse_id.isNullOrBlank() ->
                "🟠 طلب جديد بانتظار الممرض"
            status == "COMPLETED" ->
                "✓ تم إكمال الطلب"
            status == "CANCELLED" ->
                "🔴 تم إلغاء الطلب"
            else ->
                "حالة الطلب: $status"
        }

        val statusColor = when (status) {
            "ACCEPTED" -> GREEN
            "CANCELLED" -> RED
            else -> ORANGE
        }

        card.addView(txt(statusText, 16f, statusColor, true))

        addRow(card, "رقم الطلب", booking.id ?: "-")
        addRow(card, "رقم المريض", booking.patient_phone ?: "-")

        // التعديل 2: اسم نوع الخدمة الحقيقي من جدول services
        addRow(card, "نوع الخدمة", serviceName(booking.service_id))

        addRow(card, "المدينة", booking.city ?: "الأنبار")
        addRow(card, "العنوان", booking.address ?: "-")
        addRow(card, "النقطة الدالة", booking.landmark ?: "-")

        if (!booking.notes.isNullOrBlank()) {
            addRow(card, "الملاحظات", booking.notes ?: "-")
        }

        // التعديل 3: زر الخريطة موجود في شاشة الممرض فقط.
        if (booking.latitude != null && booking.longitude != null) {
            addRow(
                card,
                "موقع المريض",
                "${booking.latitude}, ${booking.longitude}"
            )

            card.addView(
                primaryButton(
                    "📍 فتح موقع المريض على الخريطة",
                    NAVY
                ) {
                    openPatientLocation(
                        booking.latitude,
                        booking.longitude
                    )
                },
                LinearLayout.LayoutParams(-1, dp(55)).apply {
                    topMargin = dp(8)
                }
            )
        }

        if (status == "PENDING" && booking.nurse_id.isNullOrBlank()) {
            card.addView(
                primaryButton("✓ قبول طلب المريض", GREEN) {
                    acceptBooking(booking)
                },
                LinearLayout.LayoutParams(-1, dp(58)).apply {
                    topMargin = dp(10)
                }
            )
        }

        if (status == "ACCEPTED" && assignedToThisNurse) {
            card.addView(
                primaryButton("📍 الذهاب إلى موقع المريض", NAVY) {
                    openPatientLocation(
                        booking.latitude,
                        booking.longitude
                    )
                },
                LinearLayout.LayoutParams(-1, dp(58)).apply {
                    topMargin = dp(10)
                }
            )
        }

        return card
    }

    private fun addRow(
        parent: LinearLayout,
        title: String,
        value: String
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, dp(3), 0, dp(3))
        }

        row.addView(
            txt("$title:", 15f, GRAY, true),
            LinearLayout.LayoutParams(dp(125), -2)
        )

        row.addView(
            txt(value, 15f, TEXT, false),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        parent.addView(row)
    }

    private fun acceptBooking(booking: NurseRequestsBooking) {
        val dbNurseId = nurseId
        val bookingId = booking.id
        val serviceId = booking.service_id

        if (dbNurseId.isNullOrBlank()) {
            Toast.makeText(this, "تعذر تحديد معرف الممرض", Toast.LENGTH_LONG).show()
            return
        }

        if (bookingId.isNullOrBlank()) {
            Toast.makeText(this, "رقم الطلب غير صالح", Toast.LENGTH_LONG).show()
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
                val nurseExists = SupabaseManager.client
                    .from("nurses")
                    .select { filter { eq("id", dbNurseId) } }
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

                val serviceExists = SupabaseManager.client
                    .from("services")
                    .select { filter { eq("id", serviceId) } }
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

                SupabaseManager.client
                    .from("bookings")
                    .update(
                        NurseBookingAssignment(
                            nurse_id = dbNurseId,
                            status = "ACCEPTED"
                        )
                    ) {
                        filter {
                            eq("id", bookingId)
                            eq("status", "PENDING")
                        }
                    }

                val updated = SupabaseManager.client
                    .from("bookings")
                    .select { filter { eq("id", bookingId) } }
                    .decodeList<NurseRequestsBooking>()
                    .firstOrNull()

                if (
                    updated?.nurse_id == dbNurseId &&
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
                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر قبول الطلب:\n${e.message ?: "خطأ غير معروف"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openPatientLocation(latitude: Double?, longitude: Double?) {
        if (latitude == null || longitude == null) {
            Toast.makeText(
                this,
                "موقع المريض غير متوفر",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val navigationIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$latitude,$longitude")
        ).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            startActivity(navigationIntent)
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"
                    )
                )
            )
        }
    }
}
