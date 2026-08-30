package iq.tamreed.home

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
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
data class NurseBooking(
    val id: String,
    val patient_id: String,
    val nurse_id: String? = null,
    val service_id: String,
    val address: String? = null,
    val city: String? = null,
    val landmark: String? = null,
    val patient_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = "PENDING",
    val notes: String? = null,
    val created_at: String? = null
)

class NurseActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val DARK_NAVY = Color.rgb(3, 45, 78)
    private val BLUE = Color.rgb(31, 115, 176)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val GREEN = Color.rgb(45, 145, 80)
    private val ORANGE = Color.rgb(225, 145, 35)
    private val RED = Color.rgb(200, 60, 60)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val WHITE = Color.WHITE
    private val BORDER = Color.rgb(218, 224, 229)

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = SupabaseManager.client.auth.currentUserOrNull()

        if (user == null) {
            Toast.makeText(
                this,
                "يجب تسجيل دخول الممرض أولاً",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        showBookings()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(
        color: Int,
        radius: Int = 18
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun bordered(
        color: Int = WHITE,
        strokeColor: Int = BORDER,
        radius: Int = 16
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
        }

    private fun rootLayout(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.TOP
            setBackgroundColor(LIGHT_GRAY)
            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(90)
            )
        }

    private fun scroll(view: View): ScrollView =
        ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(LIGHT_GRAY)
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
            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
            setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
            )
        }

    private fun primaryButton(
        title: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            text = title
            textSize = 16f
            isAllCaps = false
            setTextColor(WHITE)
            gravity = Gravity.CENTER
            background = rounded(NAVY, 14)
            setOnClickListener {
                action()
            }
        }

    private fun greenButton(
        title: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            text = title
            textSize = 16f
            isAllCaps = false
            setTextColor(WHITE)
            gravity = Gravity.CENTER
            background = rounded(GREEN, 14)
            setOnClickListener {
                action()
            }
        }

    private fun outlineButton(
        title: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            text = title
            textSize = 15f
            isAllCaps = false
            setTextColor(NAVY)
            gravity = Gravity.CENTER
            background = bordered(
                WHITE,
                NAVY,
                14
            )
            setOnClickListener {
                action()
            }
        }

    private fun addSpace(
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

    private fun addButton(
        parent: LinearLayout,
        button: Button,
        height: Int = 56
    ) {
        parent.addView(
            button,
            LinearLayout.LayoutParams(
                -1,
                dp(height)
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

    private fun showBookings() {

        val root = rootLayout()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(
                NAVY,
                24
            )
            setPadding(
                dp(16),
                dp(18),
                dp(16),
                dp(18)
            )
        }

        header.addView(
            text(
                "🩺",
                42f,
                WHITE
            )
        )

        header.addView(
            text(
                "لوحة الممرض",
                27f,
                WHITE,
                true
            )
        )

        header.addView(
            text(
                "طلبات التمريض المنزلي - محافظة الأنبار",
                14f,
                WHITE
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(165)
            )
        )

        addSpace(root, 14)

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(
                LIGHT_BLUE,
                18
            )
            setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
            )
        }

        info.addView(
            text(
                "الطلبات الجديدة",
                20f,
                NAVY,
                true
            )
        )

        info.addView(
            text(
                "اقبل الطلب ثم تحرك إلى موقع المريض",
                14f,
                GRAY
            )
        )

        root.addView(
            info,
            LinearLayout.LayoutParams(
                -1,
                dp(92)
            )
        )

        addSpace(root, 10)

        val loading = text(
            "⏳ جاري تحميل الطلبات...",
            17f,
            GRAY
        )

        root.addView(
            loading,
            LinearLayout.LayoutParams(
                -1,
                dp(80)
            )
        )

        addButton(
            root,
            primaryButton(
                "🔄 تحديث الطلبات"
            ) {
                showBookings()
            }
        )

        addButton(
            root,
            outlineButton(
                "🏠 العودة إلى التطبيق"
            ) {
                finish()
            }
        )

        setContentView(scroll(root))

        loadBookings(
            root,
            loading
        )
    }

    private fun loadBookings(
        root: LinearLayout,
        loading: TextView
    ) {
        scope.launch {

            try {

                val bookings =
                    SupabaseManager.client
                        .from("bookings")
                        .select()
                        .decodeList<NurseBooking>()

                loading.visibility = View.GONE

                if (bookings.isEmpty()) {

                    root.addView(
                        text(
                            "📭\n\nلا توجد طلبات حالياً",
                            20f,
                            NAVY,
                            true
                        ),
                        LinearLayout.LayoutParams(
                            -1,
                            dp(180)
                        )
                    )

                    return@launch
                }

                val sorted =
                    bookings.sortedByDescending {
                        it.created_at ?: ""
                    }

                sorted.forEach { booking ->
                    addBookingCard(
                        root,
                        booking
                    )
                }

            } catch (e: Exception) {

                loading.text =
                    "⚠️ تعذر تحميل الطلبات\n\n" +
                    (e.message ?: "خطأ غير معروف")

                loading.setTextColor(RED)
            }
        }
    }

    private fun addBookingCard(
        root: LinearLayout,
        booking: NurseBooking
    ) {

        addSpace(root, 8)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.RIGHT
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(
                WHITE,
                20
            )
            setPadding(
                dp(15),
                dp(14),
                dp(15),
                dp(14)
            )
            elevation = dp(2).toFloat()
        }

        card.addView(
            text(
                "🩺 ${serviceName(booking.service_id)}",
                21f,
                NAVY,
                true
            )
        )

        card.addView(
            text(
                "👤 رقم الطلب: ${booking.id}",
                12f,
                GRAY
            )
        )

        card.addView(
            text(
                "🏙️ المدينة: ${booking.city?.ifBlank { "غير محددة" } ?: "غير محددة"}",
                16f,
                TEXT,
                true
            )
        )

        card.addView(
            text(
                "📌 أقرب نقطة دالة: " +
                    (booking.landmark?.ifBlank {
                        "غير محددة"
                    } ?: "غير محددة"),
                15f,
                TEXT
            )
        )

        card.addView(
            text(
                "📍 العنوان: " +
                    (booking.address?.ifBlank {
                        "الموقع محدد بالخريطة"
                    } ?: "الموقع محدد بالخريطة"),
                15f,
                TEXT
            )
        )

        val phone =
            booking.patient_phone
                ?.trim()
                .orEmpty()

        card.addView(
            text(
                if (phone.isNotBlank()) {
                    "📞 هاتف المريض: $phone"
                } else {
                    "📞 هاتف المريض: غير مسجل"
                },
                16f,
                if (phone.isNotBlank()) {
                    NAVY
                } else {
                    GRAY
                },
                true
            )
        )

        card.addView(
            text(
                "📌 الحالة: ${statusText(booking.status)}",
                16f,
                statusColor(booking.status),
                true
            )
        )

        if (!booking.notes.isNullOrBlank()) {
            card.addView(
                text(
                    "📝 ملاحظات المريض: ${booking.notes}",
                    14f,
                    GRAY
                )
            )
        }

        root.addView(
            card,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        if (
            booking.latitude != null &&
            booking.longitude != null
        ) {

            addButton(
                root,
                primaryButton(
                    "🗺️ فتح موقع المريض في الخرائط"
                ) {
                    openMaps(
                        booking.latitude,
                        booking.longitude
                    )
                }
            )
        }

        if (phone.isNotBlank()) {

            addButton(
                root,
                greenButton(
                    "📞 الاتصال بالمريض"
                ) {
                    callPatient(phone)
                }
            )
        }

        addStatusButton(
            root,
            booking
        )
    }

    private fun addStatusButton(
        root: LinearLayout,
        booking: NurseBooking
    ) {

        when (
            booking.status
                .uppercase()
                .trim()
        ) {

            "PENDING" -> {

                addButton(
                    root,
                    greenButton(
                        "✅ قبول الطلب"
                    ) {
                        acceptBooking(
                            booking.id
                        )
                    }
                )
            }

            "ACCEPTED" -> {

                addButton(
                    root,
                    primaryButton(
                        "🚗 الممرض في الطريق"
                    ) {
                        updateStatus(
                            booking.id,
                            "ON_THE_WAY"
                        )
                    }
                )
            }

            "ON_THE_WAY" -> {

                addButton(
                    root,
                    primaryButton(
                        "📍 وصلت إلى موقع المريض"
                    ) {
                        updateStatus(
                            booking.id,
                            "IN_PROGRESS"
                        )
                    }
                )
            }

            "IN_PROGRESS" -> {

                addButton(
                    root,
                    greenButton(
                        "✅ إنهاء الزيارة"
                    ) {
                        updateStatus(
                            booking.id,
                            "COMPLETED"
                        )
                    }
                )
            }

            "COMPLETED" -> {

                addButton(
                    root,
                    outlineButton(
                        "✔️ الطلب مكتمل"
                    ) {
                        Toast.makeText(
                            this,
                            "هذا الطلب مكتمل",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun acceptBooking(
        bookingId: String
    ) {

        val user =
            SupabaseManager.client
                .auth
                .currentUserOrNull()

        if (user == null) {
            Toast.makeText(
                this,
                "يجب تسجيل الدخول أولاً",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val loading =
            ProgressDialog(this).apply {
                setMessage(
                    "جاري قبول الطلب..."
                )
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                SupabaseManager.client
                    .from("bookings")
                    .update(
                        mapOf(
                            "nurse_id" to user.id,
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

                loading.dismiss()

                Toast.makeText(
                    this@NurseActivity,
                    "تم قبول الطلب بنجاح ✅",
                    Toast.LENGTH_SHORT
                ).show()

                showBookings()

            } catch (e: Exception) {

                loading.dismiss()

                Toast.makeText(
                    this@NurseActivity,
                    "تعذر قبول الطلب:\n" +
                        (e.message ?: "خطأ غير معروف"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateStatus(
        bookingId: String,
        newStatus: String
    ) {

        val user =
            SupabaseManager.client
                .auth
                .currentUserOrNull()

        if (user == null) {
            Toast.makeText(
                this,
                "يجب تسجيل الدخول أولاً",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val loading =
            ProgressDialog(this).apply {
                setMessage(
                    "جاري تحديث حالة الطلب..."
                )
                setCancelable(false)
                show()
            }

        scope.launch {

            try {

                SupabaseManager.client
                    .from("bookings")
                    .update(
                        mapOf(
                            "status" to newStatus
                        )
                    ) {
                        filter {
                            eq(
                                "id",
                                bookingId
                            )
                            eq(
                                "nurse_id",
                                user.id
                            )
                        }
                    }

                loading.dismiss()

                Toast.makeText(
                    this@NurseActivity,
                    "تم تحديث حالة الطلب ✅",
                    Toast.LENGTH_SHORT
                ).show()

                showBookings()

            } catch (e: Exception) {

                loading.dismiss()

                Toast.makeText(
                    this@NurseActivity,
                    "تعذر تحديث الحالة:\n" +
                        (e.message ?: "خطأ غير معروف"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

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

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "تعذر فتح تطبيق الاتصال",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openMaps(
        latitude: Double,
        longitude: Double
    ) {

        val geoUri =
            Uri.parse(
                "geo:$latitude,$longitude?q=$latitude,$longitude"
            )

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                geoUri
            )

        try {
            startActivity(intent)
        } catch (e: Exception) {

            val webUri =
                Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                )

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    webUri
                )
            )
        }
    }

    private fun serviceName(
        serviceId: String
    ): String {

        val key =
            serviceId
                .trim()
                .lowercase()

        return when {

            key.contains("حقن") ||
                key.contains("injection") ->
                "إعطاء الحقن"

            key.contains("ضماد") ||
                key.contains("dressing") ->
                "تغيير الضماد"

            key.contains("سكر") ||
                key.contains("glucose") ||
                key.contains("sugar") ->
                "قياس السكر"

            key.contains("ضغط") ||
                key.contains("pressure") ->
                "قياس ضغط الدم"

            key.contains("محلول") ||
                key.contains("iv fluid") ||
                key.contains("infusion") ->
                "تركيب المحلول"

            key.contains("كانيولا") ||
                key.contains("cannula") ||
                key.contains("iv cannula") ->
                "تركيب الكانيولا"

            key.contains("قسطرة") ||
                key.contains("urinary") ||
                key.contains("catheter") ->
                "وضع القسطرة البولية"

            key.contains("كبار") ||
                key.contains("elderly") ->
                "رعاية كبار السن"

            key.contains("مرضى") ||
                key.contains("patient care") ->
                "رعاية المرضى في المنزل"

            key.contains("متابعة") ||
                key.contains("follow") ->
                "متابعة حالة صحية"

            else ->
                serviceId
        }
    }

    private fun statusText(
        status: String
    ): String {

        return when (
            status.uppercase().trim()
        ) {

            "PENDING" ->
                "بانتظار قبول الممرض"

            "ACCEPTED" ->
                "تم قبول الطلب"

            "ON_THE_WAY" ->
                "الممرض في الطريق"

            "IN_PROGRESS" ->
                "الزيارة جارية"

            "COMPLETED" ->
                "تم إكمال الزيارة"

            "CANCELLED" ->
                "تم إلغاء الطلب"

            else ->
                status
        }
    }

    private fun statusColor(
        status: String
    ): Int {

        return when (
            status.uppercase().trim()
        ) {

            "PENDING" ->
                ORANGE

            "ACCEPTED",
            "ON_THE_WAY",
            "IN_PROGRESS" ->
                BLUE

            "COMPLETED" ->
                GREEN

            "CANCELLED" ->
                RED

            else ->
                GRAY
        }
    }
}
