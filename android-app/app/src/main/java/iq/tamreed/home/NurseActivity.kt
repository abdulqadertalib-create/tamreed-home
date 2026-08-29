package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class NurseBookingRow(
    val id: String,
    val patient_id: String,
    val nurse_id: String? = null,
    val service_id: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val scheduled_at: String,
    val status: String,
    val notes: String? = null,
    val created_at: String
)

class NurseActivity : AppCompatActivity() {

    private val BLUE = Color.rgb(0, 105, 210)
    private val DARK_BLUE = Color.rgb(0, 67, 135)
    private val LIGHT_BLUE = Color.rgb(235, 246, 255)
    private val GREEN = Color.rgb(28, 145, 85)
    private val RED = Color.rgb(200, 50, 50)
    private val TEXT = Color.rgb(35, 45, 55)
    private val GRAY = Color.rgb(110, 110, 110)
    private val LIGHT_GRAY = Color.rgb(245, 247, 250)

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun background(
        color: Int,
        radius: Int = 18
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun root(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.TOP
            setBackgroundColor(Color.WHITE)
            setPadding(
                dp(16),
                dp(20),
                dp(16),
                dp(20)
            )
        }

    private fun scroll(
        view: View
    ): ScrollView =
        ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(view)
        }

    private fun label(
        value: String,
        size: Float = 16f,
        color: Int = TEXT
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(
                dp(6),
                dp(6),
                dp(6),
                dp(6)
            )
        }

    private fun addButton(
        container: LinearLayout,
        title: String,
        color: Int = BLUE,
        action: () -> Unit
    ) {

        val b =
            Button(this).apply {
                text = title
                textSize = 16f
                isAllCaps = false
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background =
                    background(color, 16)
                setOnClickListener {
                    action()
                }
            }

        container.addView(
            b,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {
                setMargins(
                    0,
                    dp(4),
                    0,
                    dp(6)
                )
            }
        )
    }

    private fun showHome() {

        val root = root()

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setPadding(
                    dp(15),
                    dp(25),
                    dp(15),
                    dp(25)
                )

                background =
                    background(
                        LIGHT_BLUE,
                        25
                    )
            }

        header.addView(
            label(
                "👨‍⚕️",
                48f,
                DARK_BLUE
            )
        )

        header.addView(
            label(
                "لوحة الممرض",
                28f,
                DARK_BLUE
            )
        )

        header.addView(
            label(
                "إدارة طلبات التمريض المنزلي",
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
                    dp(20)
                )
            }
        )

        addButton(
            root,
            "📋 عرض طلبات المرضى"
        ) {
            showBookings()
        }

        addButton(
            root,
            "🔄 تحديث الطلبات"
        ) {
            showBookings()
        }

        addButton(
            root,
            "🏠 العودة للتطبيق"
        ) {
            finish()
        }

        root.addView(
            label(
                "يمكن للممرض متابعة الطلبات وتحديث حالتها.",
                14f,
                GRAY
            )
        )

        setContentView(
            scroll(root)
        )
    }

    private fun showBookings() {

        val root = root()

        root.addView(
            label(
                "📋 طلبات المرضى",
                29f,
                DARK_BLUE
            )
        )

        root.addView(
            label(
                "الطلبات الموجودة في النظام",
                16f,
                GRAY
            )
        )

        val loading =
            label(
                "⏳ جاري تحميل الطلبات...",
                16f,
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
            "↩️ العودة"
        ) {
            showHome()
        }

        setContentView(
            scroll(root)
        )

        scope.launch {

            try {

                val bookings =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select()
                        .decodeList<NurseBookingRow>()

                loading.visibility =
                    View.GONE

                if (bookings.isEmpty()) {

                    root.addView(
                        label(
                            "📭\nلا توجد طلبات حالياً",
                            20f,
                            DARK_BLUE
                        ),
                        LinearLayout.LayoutParams(
                            -1,
                            dp(150)
                        )
                    )

                    return@launch
                }

                bookings
                    .sortedByDescending {
                        it.created_at
                    }
                    .forEach {
                        addBooking(
                            root,
                            it
                        )
                    }

            } catch (e: Exception) {

                loading.text =
                    "⚠️ تعذر تحميل الطلبات\n\n" +
                        (
                            e.message
                                ?: "خطأ غير معروف"
                        )
            }
        }
    }

    private fun addBooking(
        root: LinearLayout,
        booking: NurseBookingRow
    ) {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                gravity =
                    Gravity.RIGHT

                setPadding(
                    dp(14),
                    dp(12),
                    dp(14),
                    dp(12)
                )

                background =
                    background(
                        LIGHT_GRAY,
                        20
                    )
            }

        card.addView(
            label(
                "🩺 ${serviceName(booking.service_id)}",
                19f,
                DARK_BLUE
            )
        )

        card.addView(
            label(
                "👤 المريض: ${booking.address}",
                15f,
                TEXT
            )
        )

        card.addView(
            label(
                "📅 الموعد: ${booking.scheduled_at}",
                14f,
                GRAY
            )
        )

        card.addView(
            label(
                "📌 ${statusText(booking.status)}",
                16f,
                statusColor(
                    booking.status
                )
            )
        )

        if (
            !booking.notes.isNullOrBlank()
        ) {

            card.addView(
                label(
                    "📝 ${booking.notes}",
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
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(3)
                )
            }
        )

        when (booking.status) {

            "PENDING" -> {

                addButton(
                    root,
                    "✅ قبول الطلب",
                    GREEN
                ) {
                    changeStatus(
                        booking.id,
                        "ACCEPTED"
                    )
                }
            }

            "ACCEPTED" -> {

                addButton(
                    root,
                    "🚗 الممرض في الطريق"
                ) {
                    changeStatus(
                        booking.id,
                        "ON_THE_WAY"
                    )
                }
            }

            "ON_THE_WAY" -> {

                addButton(
                    root,
                    "🩺 بدأت الزيارة"
                ) {
                    changeStatus(
                        booking.id,
                        "IN_PROGRESS"
                    )
                }
            }

            "IN_PROGRESS" -> {

                addButton(
                    root,
                    "✅ إنهاء الزيارة",
                    GREEN
                ) {
                    changeStatus(
                        booking.id,
                        "COMPLETED"
                    )
                }
            }
        }

        if (
            booking.latitude != null &&
            booking.longitude != null
        ) {

            addButton(
                root,
                "🗺️ فتح موقع المريض"
            ) {

                openMaps(
                    booking.latitude,
                    booking.longitude
                )
            }
        }
    }

    private fun changeStatus(
        bookingId: String,
        status: String
    ) {

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

                SupabaseManager
                    .client
                    .from("bookings")
                    .update(
                        mapOf(
                            "status" to status
                        )
                    ) {
                        filter {
                            eq(
                                "id",
                                bookingId
                            )
                        }
                    }

                loading.dismiss()

                Toast.makeText(
                    this@NurseActivity,
                    "تم تحديث الحالة بنجاح ✅",
                    Toast.LENGTH_SHORT
                ).show()

                showBookings()

            } catch (e: Exception) {

                loading.dismiss()

                AlertDialog.Builder(
                    this@NurseActivity
                )
                    .setTitle(
                        "خطأ"
                    )
                    .setMessage(
                        e.message
                            ?: "تعذر تحديث الحالة"
                    )
                    .setPositiveButton(
                        "حسنًا",
                        null
                    )
                    .show()
            }
        }
    }

    private fun openMaps(
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

    private fun serviceName(
        id: String
    ): String =
        when (id) {

            "11111111-1111-4111-8111-111111111111" ->
                "زيارة تمريض منزلية"

            "22222222-2222-4222-8222-222222222222" ->
                "قياس ضغط وسكر"

            "33333333-3333-4333-8333-333333333333" ->
                "تغيير الضماد"

            "44444444-4444-4444-8444-444444444444" ->
                "إعطاء الحقن"

            "55555555-5555-4555-8555-555555555555" ->
                "تركيب المحاليل"

            "66666666-6666-4666-8666-666666666666" ->
                "رعاية كبار السن"

            else ->
                "خدمة تمريضية"
        }

    private fun statusText(
        status: String
    ): String =
        when (status) {

            "PENDING" ->
                "🟡 بانتظار قبول الممرض"

            "ACCEPTED" ->
                "🔵 تم قبول الطلب"

            "ON_THE_WAY" ->
                "🚗 الممرض في الطريق"

            "IN_PROGRESS" ->
                "🩺 بدأت الزيارة"

            "COMPLETED" ->
                "🟢 اكتملت الزيارة"

            "CANCELLED" ->
                "🔴 تم إلغاء الطلب"

            else ->
                status
        }

    private fun statusColor(
        status: String
    ): Int =
        when (status) {

            "COMPLETED" ->
                GREEN

            "CANCELLED" ->
                RED

            "ACCEPTED",
            "ON_THE_WAY",
            "IN_PROGRESS" ->
                BLUE

            else ->
                Color.rgb(
                    185,
                    125,
                    0
                )
        }
}
