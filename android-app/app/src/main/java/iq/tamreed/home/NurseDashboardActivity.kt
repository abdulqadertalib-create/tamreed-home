package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/*
 * ============================================================
 * المرحلة 12
 * لوحة الممرض — مزامنة وحماية انتقال حالات الطلب:
 * - تحديث تلقائي كل 15 ثانية.
 * - حماية انتقال الحالة من القفز أو التحديث المتزامن:
 *   ACCEPTED -> ON_THE_WAY -> IN_PROGRESS -> COMPLETED
 * - تحديث الحالة مرتبط بالممرض الحقيقي nurses.id.
 * - الحفاظ على الاتصال بالمريض وفتح الموقع.
 * ============================================================
 */

@Serializable
data class NurseDashboardProfile(
    val id: String,
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val is_active: Boolean? = null,
    val rating: Double? = null
)

@Serializable
data class NurseBooking(
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

class NurseDashboardActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val BLUE = Color.rgb(31, 115, 176)
    private val GREEN = Color.rgb(50, 150, 85)
    private val ORANGE = Color.rgb(230, 145, 45)
    private val RED = Color.rgb(205, 65, 65)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(125, 125, 125)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val WHITE = Color.WHITE

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val refreshHandler = Handler(Looper.getMainLooper())

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                showDashboard()
                refreshHandler.postDelayed(this, 15_000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDashboard()
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.removeCallbacks(refreshRunnable)
        refreshHandler.postDelayed(refreshRunnable, 15_000L)
    }

    override fun onPause() {
        refreshHandler.removeCallbacks(refreshRunnable)
        super.onPause()
    }

    override fun onDestroy() {
        refreshHandler.removeCallbacks(refreshRunnable)
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
        strokeColor: Int = NAVY,
        radius: Int = 14
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
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

    private fun button(
        value: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            text = value
            textSize = 16f
            isAllCaps = false
            setTextColor(WHITE)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(NAVY, 14)
            setOnClickListener { action() }
        }

    private fun outlineButton(
        value: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            text = value
            textSize = 15f
            isAllCaps = false
            setTextColor(NAVY)
            gravity = Gravity.CENTER
            background = bordered(WHITE, NAVY, 14)
            setOnClickListener { action() }
        }

    private fun addSpace(root: LinearLayout, height: Int) {
        root.addView(
            Space(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun scroll(root: View): ScrollView =
        ScrollView(this).apply {
            setBackgroundColor(LIGHT_GRAY)
            isFillViewport = true
            addView(root)
        }

    private fun baseLayout(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)
            setPadding(dp(14), dp(10), dp(14), dp(30))
        }

    private fun showDashboard() {

        val root = baseLayout()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(NAVY, 22)
            setPadding(dp(15), dp(18), dp(15), dp(18))
        }

        header.addView(text("👨‍⚕️", 45f, WHITE))
        header.addView(text("لوحة الممرض", 25f, WHITE, true))
        header.addView(
            text(
                "إدارة واستقبال طلبات التمريض المنزلي",
                14f,
                Color.rgb(225, 238, 247)
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, dp(145))
        )

        addSpace(root, 12)

        val profileText = text(
            "جاري تحميل بيانات الممرض...",
            15f,
            GRAY
        )

        val profileBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 18)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        profileBox.addView(profileText)
        root.addView(
            profileBox,
            LinearLayout.LayoutParams(-1, -2)
        )

        addSpace(root, 12)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        actions.addView(
            button("↻ تحديث") {
                showDashboard()
            },
            LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                marginEnd = dp(5)
            }
        )

        actions.addView(
            outlineButton("🚪 خروج") {
                finish()
            },
            LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                marginStart = dp(5)
            }
        )

        root.addView(actions)
        addSpace(root, 14)

        root.addView(
            text(
                "📥 الطلبات المتاحة",
                22f,
                NAVY,
                true
            )
        )

        root.addView(
            text(
                "هذه الطلبات لم يتم قبولها من ممرض بعد.",
                13f,
                GRAY
            )
        )

        addSpace(root, 8)

        val pendingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        root.addView(pendingContainer)

        addSpace(root, 18)

        root.addView(
            text(
                "📋 طلباتي المقبولة",
                22f,
                NAVY,
                true
            )
        )

        addSpace(root, 8)

        val myContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        root.addView(myContainer)

        setContentView(scroll(root))

        val user =
            SupabaseManager
                .client
                .auth
                .currentUserOrNull()

        if (user == null) {
            profileText.text =
                "لم يتم تسجيل دخول الممرض.\nارجع وسجل الدخول أولاً."
            return
        }

        scope.launch {

            try {

                val nurse =
                    SupabaseManager
                        .client
                        .from("nurses")
                        .select {
                            filter {
                                eq("user_id", user.id)
                            }
                        }
                        .decodeList<NurseDashboardProfile>()
                        .firstOrNull()

                if (nurse == null) {
                    profileText.text =
                        "⚠️ لم يتم العثور على ملف الممرض.\n" +
                        "تأكد أن جدول nurses يحتوي على سجل بنفس auth user id."
                } else {

                    val name =
                        nurse.full_name
                            ?.takeIf { it.isNotBlank() }
                            ?: "الممرض"

                    val active =
                        if (nurse.is_active == true)
                            "🟢 الحساب نشط"
                        else
                            "🔴 الحساب غير نشط"

                    val rating =
                        nurse.rating?.let {
                            "⭐ ${String.format("%.1f", it)}"
                        } ?: "⭐ لا يوجد تقييم"

                    profileText.text =
                        "👨‍⚕️ $name\n$active    $rating"

                    if (nurse.id.isBlank()) {
                        profileText.text =
                            "⚠️ سجل الممرض موجود لكن معرف nurses.id فارغ."
                        return@launch
                    }

                    loadPendingBookings(
                        pendingContainer,
                        myContainer,
                        nurse.id
                    )
                }

            } catch (e: Exception) {

                profileText.text =
                    "تعذر تحميل بيانات الممرض.\n${e.message ?: "خطأ غير معروف"}"
            }
        }
    }

    private fun loadPendingBookings(
        pendingContainer: LinearLayout,
        myContainer: LinearLayout,
        nurseId: String
    ) {

        scope.launch {

            try {

                val pending =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select {
                            filter {
                                eq("status", "PENDING")
                            }
                        }
                        .decodeList<NurseBooking>()
                        .sortedByDescending { it.created_at }

                val mine =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select {
                            filter {
                                eq("nurse_id", nurseId)
                            }
                        }
                        .decodeList<NurseBooking>()
                        .filter {
                            it.status.uppercase() != "CANCELLED"
                        }
                        .sortedByDescending { it.created_at }

                pendingContainer.removeAllViews()
                myContainer.removeAllViews()

                if (pending.isEmpty()) {
                    pendingContainer.addView(
                        emptyState(
                            "📭",
                            "لا توجد طلبات جديدة",
                            "عند وصول طلب جديد سيظهر هنا."
                        )
                    )
                } else {
                    pending.forEach {
                        pendingContainer.addView(
                            bookingCard(
                                it,
                                nurseId,
                                isAvailable = true
                            )
                        )
                        addSpace(pendingContainer, 10)
                    }
                }

                if (mine.isEmpty()) {
                    myContainer.addView(
                        emptyState(
                            "📋",
                            "لا توجد طلبات مقبولة",
                            "عندما تقبل طلباً سيظهر هنا لمتابعة حالته."
                        )
                    )
                } else {
                    mine.forEach {
                        myContainer.addView(
                            bookingCard(
                                it,
                                nurseId,
                                isAvailable = false
                            )
                        )
                        addSpace(myContainer, 10)
                    }
                }

            } catch (e: Exception) {

                pendingContainer.addView(
                    text(
                        "تعذر تحميل الطلبات.\n${e.message ?: "خطأ غير معروف"}",
                        14f,
                        RED
                    )
                )
            }
        }
    }

    private fun bookingCard(
        booking: NurseBooking,
        nurseId: String,
        isAvailable: Boolean
    ): LinearLayout {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 18)
            elevation = dp(2).toFloat()
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        card.addView(
            text(
                "🩺 طلب تمريض",
                18f,
                NAVY,
                true
            )
        )

        card.addView(
            text(
                "🔖 رقم الطلب: ${booking.id}",
                12f,
                GRAY
            )
        )

        card.addView(
            text(
                "📍 ${booking.city ?: ""} ${booking.landmark ?: ""}\n${booking.address}",
                14f,
                TEXT
            )
        )

        if (!booking.patient_phone.isNullOrBlank()) {
            card.addView(
                text(
                    "📞 هاتف المريض: ${booking.patient_phone}",
                    14f,
                    TEXT
                )
            )
        }

        card.addView(
            text(
                "الحالة: ${statusText(booking.status)}",
                15f,
                statusColor(booking.status),
                true
            )
        )

        if (!booking.notes.isNullOrBlank()) {
            card.addView(
                text(
                    "📝 ${booking.notes}",
                    13f,
                    GRAY
                )
            )
        }

        addSpace(card, 6)

        if (isAvailable) {

            card.addView(
                button("✅ قبول الطلب") {
                    confirmAccept(booking, nurseId)
                },
                LinearLayout.LayoutParams(-1, dp(55))
            )

            addSpace(card, 7)

            card.addView(
                outlineButton("❌ رفض / تجاهل") {
                    showInfo(
                        "الطلب",
                        "تم إبقاء الطلب متاحاً للممرضين الآخرين.\nيمكنك تجاهله دون تغيير حالة الطلب."
                    )
                },
                LinearLayout.LayoutParams(-1, dp(50))
            )

        } else {

            addStatusActions(card, booking, nurseId)
        }

        if (booking.patient_phone != null) {

            addSpace(card, 7)

            card.addView(
                outlineButton("📞 الاتصال بالمريض") {
                    callPhone(booking.patient_phone)
                },
                LinearLayout.LayoutParams(-1, dp(50))
            )
        }

        if (booking.latitude != null &&
            booking.longitude != null
        ) {

            addSpace(card, 7)

            card.addView(
                outlineButton("🗺️ فتح موقع المريض") {
                    openLocation(
                        booking.latitude,
                        booking.longitude
                    )
                },
                LinearLayout.LayoutParams(-1, dp(50))
            )
        }

        return card
    }

    private fun addStatusActions(
        card: LinearLayout,
        booking: NurseBooking,
        nurseId: String
    ) {

        when (booking.status.uppercase()) {

            "ACCEPTED" -> {

                card.addView(
                    button("🚗 أنا في الطريق") {
                        updateBookingStatus(
                            booking,
                            nurseId,
                            "ACCEPTED",
                            "ON_THE_WAY"
                        )
                    },
                    LinearLayout.LayoutParams(-1, dp(55))
                )
            }

            "ON_THE_WAY" -> {

                card.addView(
                    button("🩺 بدء الزيارة") {
                        updateBookingStatus(
                            booking,
                            nurseId,
                            "ON_THE_WAY",
                            "IN_PROGRESS"
                        )
                    },
                    LinearLayout.LayoutParams(-1, dp(55))
                )
            }

            "IN_PROGRESS" -> {

                card.addView(
                    button("✅ إكمال الزيارة") {
                        confirmComplete(
                            booking,
                            nurseId
                        )
                    },
                    LinearLayout.LayoutParams(-1, dp(55))
                )
            }

            "COMPLETED" -> {

                card.addView(
                    text(
                        "✅ اكتملت الزيارة",
                        16f,
                        GREEN,
                        true
                    )
                )
            }
        }
    }

    private fun confirmAccept(
        booking: NurseBooking,
        nurseId: String
    ) {

        AlertDialog.Builder(this)
            .setTitle("قبول الطلب")
            .setMessage(
                "هل تريد قبول طلب التمريض رقم:\n\n" +
                    booking.id +
                    "\n\nسيصبح الطلب مرتبطاً بحسابك."
            )
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("قبول") { _, _ ->
                acceptBooking(booking, nurseId)
            }
            .show()
    }

    private fun acceptBooking(
        booking: NurseBooking,
        nurseId: String
    ) {

        val loading =
            ProgressDialog(this).apply {
                setMessage("جاري قبول الطلب...")
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                SupabaseManager
                    .client
                    .from("bookings")
                    .update(
                        {
                            set("nurse_id", nurseId)
                            set("status", "ACCEPTED")
                        }
                    ) {
                        filter {
                            eq("id", booking.id)
                            eq("status", "PENDING")
                        }
                    }

                loading.dismiss()

                showInfo(
                    "تم قبول الطلب ✅",
                    "أصبح الطلب ضمن طلباتك.\nيمكنك الآن الانتقال إلى حالة «في الطريق»."
                )

                showDashboard()

            } catch (e: Exception) {

                loading.dismiss()

                showInfo(
                    "تعذر قبول الطلب",
                    e.message
                        ?: "تحقق من صلاحيات Supabase وسياسات RLS لجدول bookings."
                )
            }
        }
    }

    private fun updateBookingStatus(
        booking: NurseBooking,
        nurseId: String,
        expectedStatus: String,
        newStatus: String
    ) {

        val loading =
            ProgressDialog(this).apply {
                setMessage("جاري تحديث حالة الطلب...")
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                SupabaseManager
                    .client
                    .from("bookings")
                    .update(
                        {
                            set("status", newStatus)
                        }
                    ) {
                        filter {
                            eq("id", booking.id)
                            eq("nurse_id", nurseId)
                            eq("status", expectedStatus)
                        }
                    }

                loading.dismiss()

                showInfo(
                    "تم تحديث حالة الطلب",
                    "${statusText(newStatus)}\n\nسيتم تحديث شاشة المريض تلقائياً."
                )
                showDashboard()

            } catch (e: Exception) {

                loading.dismiss()

                showInfo(
                    "تعذر تحديث الحالة",
                    e.message
                        ?: "تحقق من صلاحيات تحديث bookings."
                )
            }
        }
    }

    private fun confirmComplete(
        booking: NurseBooking,
        nurseId: String
    ) {

        AlertDialog.Builder(this)
            .setTitle("إكمال الزيارة")
            .setMessage(
                "هل انتهت الزيارة التمريضية للطلب؟"
            )
            .setNegativeButton("ليس بعد", null)
            .setPositiveButton("نعم، مكتملة") { _, _ ->
                updateBookingStatus(
                    booking,
                    nurseId,
                    "IN_PROGRESS",
                    "COMPLETED"
                )
            }
            .show()
    }

    private fun callPhone(phone: String) {

        try {

            startActivity(
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:$phone")
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "تعذر فتح الاتصال",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openLocation(
        latitude: Double,
        longitude: Double
    ) {

        try {

            val uri =
                Uri.parse(
                    "geo:$latitude,$longitude?q=$latitude,$longitude"
                )

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "تعذر فتح الخرائط",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun statusText(status: String): String =
        when (status.uppercase()) {
            "PENDING" -> "بانتظار قبول الممرض"
            "ACCEPTED" -> "تم قبول الطلب"
            "ON_THE_WAY" -> "الممرض في الطريق"
            "IN_PROGRESS" -> "الزيارة جارية"
            "COMPLETED" -> "مكتمل"
            "CANCELLED" -> "ملغى"
            else -> status
        }

    private fun statusColor(status: String): Int =
        when (status.uppercase()) {
            "PENDING" -> ORANGE
            "ACCEPTED" -> BLUE
            "ON_THE_WAY" -> NAVY
            "IN_PROGRESS" -> ORANGE
            "COMPLETED" -> GREEN
            "CANCELLED" -> RED
            else -> GRAY
        }

    private fun emptyState(
        icon: String,
        title: String,
        description: String
    ): LinearLayout {

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(WHITE, 18)
            setPadding(dp(15), dp(25), dp(15), dp(25))
        }

        box.addView(text(icon, 45f, GRAY))
        box.addView(text(title, 19f, NAVY, true))
        box.addView(text(description, 14f, GRAY))

        return box
    }

    private fun showInfo(
        title: String,
        message: String
    ) {

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show()
    }
}
