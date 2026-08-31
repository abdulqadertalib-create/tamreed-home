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
data class NurseDashboardProfile(
    val id: String? = null,
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val is_active: Boolean? = null,
    val rating: Double? = null
)

@Serializable
data class NurseDashboardBooking(
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
data class NurseDashboardService(
    val id: String? = null,
    val name_ar: String? = null
)

@Serializable
data class NurseStatusUpdate(
    val status: String
)

class NurseDashboardActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val BLUE = Color.rgb(31, 115, 176)
    private val GREEN = Color.rgb(35, 145, 85)
    private val ORANGE = Color.rgb(220, 145, 35)
    private val RED = Color.rgb(190, 55, 55)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val BG = Color.rgb(247, 248, 249)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val WHITE = Color.WHITE

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    private var currentAuthUserId: String? = null
    private var currentNurseId: String? = null

    private val serviceNames =
        mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadNurseAndDashboard()
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
            setStroke(
                dp(1),
                strokeColor
            )
            cornerRadius =
                dp(radius).toFloat()
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
        color: Int = NAVY,
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

    private fun space(
        root: LinearLayout,
        height: Int
    ) {

        root.addView(
            Space(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    /*
     * ============================================================
     * جلب بيانات الممرض
     *
     * المهم هنا:
     *
     * auth.currentUser.id
     *       ↓
     * nurses.user_id
     *       ↓
     * nurses.id
     *
     * وليس nurses.id = auth user id
     * ============================================================
     */

    private fun loadNurseAndDashboard() {

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

        currentAuthUserId =
            user.id

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
                        .decodeList<NurseDashboardProfile>()

                val nurse =
                    nurses.firstOrNull()

                if (nurse == null) {

                    Toast.makeText(
                        this@NurseDashboardActivity,
                        "لم يتم العثور على سجل الممرض",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()

                    return@launch
                }

                if (
                    nurse.id.isNullOrBlank()
                ) {

                    Toast.makeText(
                        this@NurseDashboardActivity,
                        "معرف الممرض فارغ",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()

                    return@launch
                }

                currentNurseId =
                    nurse.id

                loadServices()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseDashboardActivity,
                    "تعذر تحميل بيانات الممرض:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /*
     * ============================================================
     * تحميل أسماء الخدمات
     * ============================================================
     */

    private fun loadServices() {

        scope.launch {

            try {

                val services =
                    SupabaseManager
                        .client
                        .from("services")
                        .select()
                        .decodeList<NurseDashboardService>()

                serviceNames.clear()

                services.forEach { service ->

                    if (
                        !service.id.isNullOrBlank() &&
                        !service.name_ar.isNullOrBlank()
                    ) {

                        serviceNames[
                            service.id!!
                        ] =
                            service.name_ar!!
                    }
                }

            } catch (e: Exception) {

                /*
                 * إذا فشل تحميل الخدمات
                 * لا نوقف الطلبات.
                 */

                serviceNames.clear()
            }

            showDashboard()
        }
    }

    /*
     * ============================================================
     * الشاشة الرئيسية للممرض
     * ============================================================
     */

    private fun showDashboard() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setBackgroundColor(BG)

                setPadding(
                    dp(14),
                    dp(12),
                    dp(14),
                    dp(35)
                )
            }

        val scroll =
            ScrollView(this).apply {

                isFillViewport = true

                addView(root)
            }

        setContentView(scroll)

        /*
         * الرأس
         */

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    rounded(
                        WHITE,
                        20
                    )

                setPadding(
                    dp(8),
                    dp(8),
                    dp(8),
                    dp(8)
                )
            }

        header.addView(
            primaryButton(
                "↻"
            ) {
                loadNurseAndDashboard()
            },
            LinearLayout.LayoutParams(
                dp(70),
                dp(55)
            )
        )

        header.addView(
            text(
                "طلبات المريض",
                25f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                dp(65),
                1f
            )
        )

        header.addView(
            outlineButton(
                "رجوع"
            ) {
                finish()
            },
            LinearLayout.LayoutParams(
                dp(90),
                dp(55)
            )
        )

        root.addView(
            header
        )

        space(
            root,
            12
        )

        /*
         * بيانات الممرض
         */

        val profile =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    rounded(
                        LIGHT_BLUE,
                        20
                    )

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(15)
                )
            }

        profile.addView(
            text(
                "👨‍⚕️ الممرض",
                22f,
                NAVY,
                true
            )
        )

        profile.addView(
            text(
                "نظام التمريض المنزلي - محافظة الأنبار",
                14f,
                GRAY
            )
        )

        root.addView(
            profile
        )

        space(
            root,
            15
        )

        /*
         * الطلبات الجديدة
         */

        root.addView(
            text(
                "📥 الطلبات الجديدة",
                22f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "طلبات المرضى التي لم يقبلها أي ممرض",
                13f,
                GRAY
            )
        )

        space(
            root,
            8
        )

        val pendingContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        root.addView(
            pendingContainer
        )

        /*
         * الطلبات المقبولة
         */

        space(
            root,
            22
        )

        root.addView(
            text(
                "📋 طلباتي المقبولة",
                22f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "هنا تظهر الطلبات بعد قبولها ويمكنك متابعة المريض",
                13f,
                GRAY
            )
        )

        space(
            root,
            8
        )

        val acceptedContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        root.addView(
            acceptedContainer
        )

        /*
         * تحميل الطلبات
         */

        loadBookings(
            pendingContainer,
            acceptedContainer
        )
    }

    /*
     * ============================================================
     * تحميل الطلبات
     * ============================================================
     */

    private fun loadBookings(
        pendingContainer: LinearLayout,
        acceptedContainer: LinearLayout
    ) {

        val nurseId =
            currentNurseId

        if (nurseId.isNullOrBlank()) {
            return
        }

        scope.launch {

            try {

                val bookings =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select()
                        .decodeList<NurseDashboardBooking>()

                pendingContainer.removeAllViews()
                acceptedContainer.removeAllViews()

                /*
                 * الطلبات الجديدة
                 */

                val pending =
                    bookings
                        .filter {

                            it.status
                                ?.uppercase() ==
                                "PENDING" &&
                            it.nurse_id
                                .isNullOrBlank()
                        }
                        .sortedByDescending {

                            it.created_at ?: ""
                        }

                /*
                 * الطلبات الخاصة بالممرض
                 */

                val accepted =
                    bookings
                        .filter {

                            it.nurse_id ==
                                nurseId
                        }
                        .filter {

                            it.status
                                ?.uppercase() !=
                                "CANCELLED"
                        }
                        .sortedByDescending {

                            it.created_at ?: ""
                        }

                if (pending.isEmpty()) {

                    pendingContainer.addView(
                        emptyCard(
                            "📭",
                            "لا توجد طلبات جديدة",
                            "ستظهر هنا طلبات المرضى الجديدة"
                        )
                    )

                } else {

                    pending.forEach { booking ->

                        pendingContainer.addView(
                            bookingCard(
                                booking,
                                true
                            )
                        )

                        space(
                            pendingContainer,
                            14
                        )
                    }
                }

                if (accepted.isEmpty()) {

                    acceptedContainer.addView(
                        emptyCard(
                            "📋",
                            "لا توجد طلبات مقبولة",
                            "بعد قبول أي طلب سيظهر هنا"
                        )
                    )

                } else {

                    accepted.forEach { booking ->

                        acceptedContainer.addView(
                            bookingCard(
                                booking,
                                false
                            )
                        )

                        space(
                            acceptedContainer,
                            14
                        )
                    }
                }

            } catch (e: Exception) {

                val error =
                    text(
                        "تعذر تحميل الطلبات:\n${e.message}",
                        15f,
                        RED,
                        true
                    )

                pendingContainer.addView(
                    error
                )
            }
        }
    }

    /*
     * ============================================================
     * بطاقة فارغة
     * ============================================================
     */

    private fun emptyCard(
        icon: String,
        title: String,
        description: String
    ): LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            background =
                rounded(
                    LIGHT_BLUE,
                    20
                )

            setPadding(
                dp(20),
                dp(25),
                dp(20),
                dp(25)
            )

            addView(
                text(
                    icon,
                    45f,
                    NAVY
                )
            )

            addView(
                text(
                    title,
                    20f,
                    NAVY,
                    true
                )
            )

            addView(
                text(
                    description,
                    14f,
                    GRAY
                )
            )
        }
    }

    /*
     * ============================================================
     * بطاقة الطلب الكاملة
     * ============================================================
     */

    private fun bookingCard(
        booking: NurseDashboardBooking,
        isPending: Boolean
    ): LinearLayout {

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
                    dp(3).toFloat()

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(15)
                )
            }

        /*
         * العنوان
         */

        card.addView(
            text(
                "🩺 طلب تمريض منزلي",
                22f,
                NAVY,
                true
            )
        )

        /*
         * الحالة
         */

        val status =
            booking.status
                ?: "PENDING"

        card.addView(
            text(
                statusLabel(status),
                16f,
                statusColor(status),
                true
            )
        )

        addRow(
            card,
            "رقم الطلب",
            booking.id ?: "-"
        )

        /*
         * ⭐ اسم الخدمة الحقيقي
         */

        val serviceId =
            booking.service_id

        val serviceName =
            if (
                !serviceId.isNullOrBlank()
            ) {

                serviceNames[
                    serviceId
                ] ?: serviceId

            } else {
                "-"
            }

        addRow(
            card,
            "الخدمة",
            serviceName
        )

        addRow(
            card,
            "رقم المريض",
            booking.patient_phone ?: "-"
        )

        addRow(
            card,
            "المدينة",
            booking.city ?: "-"
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
            !booking.notes
                .isNullOrBlank()
        ) {

            addRow(
                card,
                "ملاحظات المريض",
                booking.notes!!
            )
        }

        /*
         * موقع GPS
         */

        if (
            booking.latitude != null &&
            booking.longitude != null
        ) {

            addRow(
                card,
                "موقع المريض",
                "${booking.latitude}, ${booking.longitude}"
            )

            space(
                card,
                8
            )

            /*
             * زر فتح الخرائط
             */

            card.addView(
                primaryButton(
                    "📍 فتح موقع المريض في الخرائط",
                    BLUE
                ) {

                    openPatientLocation(
                        booking.latitude,
                        booking.longitude
                    )
                },
                LinearLayout.LayoutParams(
                    -1,
                    dp(58)
                )
            )
        }

        /*
         * زر الاتصال
         */

        if (
            !booking.patient_phone
                .isNullOrBlank()
        ) {

            space(
                card,
                8
            )

            card.addView(
                outlineButton(
                    "📞 الاتصال بالمريض"
                ) {

                    callPatient(
                        booking.patient_phone!!
                    )
                },
                LinearLayout.LayoutParams(
                    -1,
                    dp(52)
                )
            )
        }

        /*
         * طلب جديد
         */

        if (isPending) {

            space(
                card,
                10
            )

            card.addView(
                primaryButton(
                    "✓ قبول طلب المريض",
                    GREEN
                ) {

                    acceptBooking(
                        booking
                    )
                },
                LinearLayout.LayoutParams(
                    -1,
                    dp(60)
                )
            )
        }

        /*
         * الطلب المقبول
         */

        if (!isPending) {

            space(
                card,
                12
            )

            addStatusButtons(
                card,
                booking
            )
        }

        return card
    }

    /*
     * ============================================================
     * معلومات الصف
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
            text(
                "$title:",
                14f,
                GRAY,
                true
            ),
            LinearLayout.LayoutParams(
                dp(110),
                dp(48)
            )
        )

        row.addView(
            text(
                value,
                14f,
                TEXT
            ),
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        parent.addView(
            row
        )
    }

    /*
     * ============================================================
     * قبول الطلب
     * ============================================================
     */

    private fun acceptBooking(
        booking: NurseDashboardBooking
    ) {

        val bookingId =
            booking.id

        val nurseId =
            currentNurseId

        if (
            bookingId.isNullOrBlank() ||
            nurseId.isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                "بيانات الطلب أو الممرض غير مكتملة",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        scope.launch {

            try {

                /*
                 * تحديث الطلب:
                 *
                 * nurse_id = nurses.id
                 * status = ACCEPTED
                 */

                SupabaseManager
                    .client
                    .from("bookings")
                    .update(
                        mapOf(
                            "nurse_id" to nurseId,
                            "status" to "ACCEPTED"
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

                Toast.makeText(
                    this@NurseDashboardActivity,
                    "تم قبول الطلب بنجاح ✓",
                    Toast.LENGTH_SHORT
                ).show()

                loadNurseAndDashboard()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseDashboardActivity,
                    "تعذر قبول الطلب:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /*
     * ============================================================
     * أزرار متابعة الطلب
     * ============================================================
     */

    private fun addStatusButtons(
        card: LinearLayout,
        booking: NurseDashboardBooking
    ) {

        val status =
            booking.status
                ?.uppercase()
                ?: "PENDING"

        when (status) {

            "ACCEPTED" -> {

                card.addView(
                    primaryButton(
                        "🚗 أنا في الطريق",
                        BLUE
                    ) {

                        updateBookingStatus(
                            booking,
                            "ON_THE_WAY"
                        )
                    },
                    LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                    )
                )
            }

            "ON_THE_WAY" -> {

                card.addView(
                    primaryButton(
                        "🩺 بدأت التمريض",
                        GREEN
                    ) {

                        updateBookingStatus(
                            booking,
                            "IN_PROGRESS"
                        )
                    },
                    LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                    )
                )
            }

            "IN_PROGRESS" -> {

                card.addView(
                    primaryButton(
                        "✓ إكمال الطلب",
                        GREEN
                    ) {

                        updateBookingStatus(
                            booking,
                            "COMPLETED"
                        )
                    },
                    LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                    )
                )
            }

            "COMPLETED" -> {

                card.addView(
                    text(
                        "✓ تم إكمال خدمة المريض",
                        17f,
                        GREEN,
                        true
                    )
                )
            }
        }
    }

    /*
     * ============================================================
     * تحديث حالة الطلب
     * ============================================================
     */

    private fun updateBookingStatus(
        booking: NurseDashboardBooking,
        newStatus: String
    ) {

        val bookingId =
            booking.id

        val nurseId =
            currentNurseId

        if (
            bookingId.isNullOrBlank() ||
            nurseId.isNullOrBlank()
        ) {
            return
        }

        scope.launch {

            try {

                SupabaseManager
                    .client
                    .from("bookings")
                    .update(
                        NurseStatusUpdate(
                            status = newStatus
                        )
                    ) {

                        filter {

                            eq(
                                "id",
                                bookingId
                            )

                            eq(
                                "nurse_id",
                                nurseId
                            )
                        }
                    }

                Toast.makeText(
                    this@NurseDashboardActivity,
                    "تم تحديث حالة الطلب ✓",
                    Toast.LENGTH_SHORT
                ).show()

                loadNurseAndDashboard()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseDashboardActivity,
                    "تعذر تحديث الحالة:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /*
     * ============================================================
     * فتح موقع المريض
     * ============================================================
     */

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

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                uri
            ).apply {

                setPackage(
                    "com.google.android.apps.maps"
                )
            }

        try {

            startActivity(intent)

        } catch (e: Exception) {

            val fallback =
                Uri.parse(
                    "geo:$latitude,$longitude?q=$latitude,$longitude"
                )

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    fallback
                )
            )
        }
    }

    /*
     * ============================================================
     * الاتصال بالمريض
     * ============================================================
     */

    private fun callPatient(
        phone: String
    ) {

        val intent =
            Intent(
                Intent.ACTION_DIAL,
                Uri.parse(
                    "tel:$phone"
                )
            )

        startActivity(intent)
    }

    /*
     * ============================================================
     * أسماء الحالات
     * ============================================================
     */

    private fun statusLabel(
        status: String
    ): String {

        return when (
            status.uppercase()
        ) {

            "PENDING" ->
                "🟠 بانتظار الممرض"

            "ACCEPTED" ->
                "🟢 تم قبول الطلب"

            "ON_THE_WAY" ->
                "🚗 الممرض في الطريق"

            "IN_PROGRESS" ->
                "🩺 الخدمة قيد التنفيذ"

            "COMPLETED" ->
                "✓ تم إكمال الخدمة"

            "CANCELLED" ->
                "🔴 تم إلغاء الطلب"

            else ->
                status
        }
    }

    private fun statusColor(
        status: String
    ): Int {

        return when (
            status.uppercase()
        ) {

            "PENDING" ->
                ORANGE

            "ACCEPTED" ->
                GREEN

            "ON_THE_WAY" ->
                BLUE

            "IN_PROGRESS" ->
                GREEN

            "COMPLETED" ->
                GREEN

            "CANCELLED" ->
                RED

            else ->
                GRAY
        }
    }
}
