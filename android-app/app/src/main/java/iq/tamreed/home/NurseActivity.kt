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
import io.github.jan.supabase.auth.auth
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
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun background(color: Int, radius: Int = 18): GradientDrawable =
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
            setPadding(dp(16), dp(20), dp(16), dp(20))
        }

    private fun scroll(view: View): ScrollView =
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
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

    private fun addButton(
        container: LinearLayout,
        title: String,
        color: Int = BLUE,
        action: () -> Unit
    ) {
        val b = Button(this).apply {
            text = title
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = background(color, 16)
            setOnClickListener { action() }
        }

        container.addView(
            b,
            LinearLayout.LayoutParams(-1, dp(58)).apply {
                setMargins(0, dp(4), 0, dp(6))
            }
        )
    }

    private fun showHome() {
        val root = root()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(15), dp(25), dp(15), dp(25))
            background = background(LIGHT_BLUE, 25)
        }

        header.addView(label("ðŸ‘¨â€âš•ï¸", 48f, DARK_BLUE))
        header.addView(label("Ù„ÙˆØ­Ø© Ø§Ù„Ù…Ù…Ø±Ø¶", 28f, DARK_BLUE))
        header.addView(label("Ø¥Ø¯Ø§Ø±Ø© Ø·Ù„Ø¨Ø§Øª Ø§Ù„ØªÙ…Ø±ÙŠØ¶ Ø§Ù„Ù…Ù†Ø²Ù„ÙŠ", 16f, GRAY))

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 0, 0, dp(20))
            }
        )

        addButton(root, "ðŸ“‹ Ø¹Ø±Ø¶ Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ù…Ø±Ø¶Ù‰") { showBookings() }
        addButton(root, "ðŸ”„ ØªØ­Ø¯ÙŠØ« Ø§Ù„Ø·Ù„Ø¨Ø§Øª") { showBookings() }
        addButton(root, "ðŸ  Ø§Ù„Ø¹ÙˆØ¯Ø© Ù„Ù„ØªØ·Ø¨ÙŠÙ‚") { finish() }

        root.addView(
            label("ÙŠÙ…ÙƒÙ† Ù„Ù„Ù…Ù…Ø±Ø¶ Ù…ØªØ§Ø¨Ø¹Ø© Ø§Ù„Ø·Ù„Ø¨Ø§Øª ÙˆØªØ­Ø¯ÙŠØ« Ø­Ø§Ù„ØªÙ‡Ø§.", 14f, GRAY)
        )

        setContentView(scroll(root))
    }

    private fun showBookings() {
        val root = root()

        root.addView(label("ðŸ“‹ Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ù…Ø±Ø¶Ù‰", 29f, DARK_BLUE))
        root.addView(label("Ø§Ù„Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ù…ÙˆØ¬ÙˆØ¯Ø© ÙÙŠ Ø§Ù„Ù†Ø¸Ø§Ù…", 16f, GRAY))

        val loading = label("â³ Ø¬Ø§Ø±ÙŠ ØªØ­Ù…ÙŠÙ„ Ø§Ù„Ø·Ù„Ø¨Ø§Øª...", 16f, GRAY)

        root.addView(
            loading,
            LinearLayout.LayoutParams(-1, dp(80))
        )

        addButton(root, "â†©ï¸ Ø§Ù„Ø¹ÙˆØ¯Ø©") { showHome() }
        setContentView(scroll(root))

        scope.launch {
            try {
                val bookings = SupabaseManager.client
                    .from("bookings")
                    .select()
                    .decodeList<NurseBookingRow>()

                loading.visibility = View.GONE

                if (bookings.isEmpty()) {
                    root.addView(
                        label("ðŸ“­\nÙ„Ø§ ØªÙˆØ¬Ø¯ Ø·Ù„Ø¨Ø§Øª Ø­Ø§Ù„ÙŠØ§Ù‹", 20f, DARK_BLUE),
                        LinearLayout.LayoutParams(-1, dp(150))
                    )
                    return@launch
                }

                bookings
                    .sortedByDescending { it.created_at }
                    .forEach { addBooking(root, it) }

            } catch (e: Exception) {
                loading.text =
                    "âš ï¸ ØªØ¹Ø°Ø± ØªØ­Ù…ÙŠÙ„ Ø§Ù„Ø·Ù„Ø¨Ø§Øª\n\n" +
                        (e.message ?: "Ø®Ø·Ø£ ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ")
            }
        }
    }

    private fun addBooking(
        root: LinearLayout,
        booking: NurseBookingRow
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.RIGHT
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = background(LIGHT_GRAY, 20)
        }

        card.addView(
            label("ðŸ©º ${serviceName(booking.service_id)}", 19f, DARK_BLUE)
        )
        card.addView(
            label("ðŸ‘¤ Ø§Ù„Ù…Ø±ÙŠØ¶: ${booking.address}", 15f, TEXT)
        )
        card.addView(
            label("ðŸ“… Ø§Ù„Ù…ÙˆØ¹Ø¯: ${booking.scheduled_at}", 14f, GRAY)
        )
        card.addView(
            label(
                "ðŸ“Œ ${statusText(booking.status)}",
                16f,
                statusColor(booking.status)
            )
        )

        if (!booking.nurse_id.isNullOrBlank()) {
            card.addView(
                label(
                    "ðŸ‘¨â€âš•ï¸ ØªÙ… ØªØ¹ÙŠÙŠÙ† Ø§Ù„Ù…Ù…Ø±Ø¶ Ù„Ù‡Ø°Ø§ Ø§Ù„Ø·Ù„Ø¨",
                    14f,
                    GREEN
                )
            )
        }

        if (!booking.notes.isNullOrBlank()) {
            card.addView(label("ðŸ“ ${booking.notes}", 14f, GRAY))
        }

        root.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, dp(5), 0, dp(3))
            }
        )

        when (booking.status) {
            "PENDING" -> addButton(root, "âœ… Ù‚Ø¨ÙˆÙ„ Ø§Ù„Ø·Ù„Ø¨", GREEN) {
                acceptBooking(booking.id)
            }

            "ACCEPTED" -> addButton(root, "ðŸš— Ø§Ù„Ù…Ù…Ø±Ø¶ ÙÙŠ Ø§Ù„Ø·Ø±ÙŠÙ‚") {
                changeStatus(booking.id, "ON_THE_WAY")
            }

            "ON_THE_WAY" -> addButton(root, "ðŸ©º Ø¨Ø¯Ø£Øª Ø§Ù„Ø²ÙŠØ§Ø±Ø©") {
                changeStatus(booking.id, "IN_PROGRESS")
            }

            "IN_PROGRESS" -> addButton(root, "âœ… Ø¥Ù†Ù‡Ø§Ø¡ Ø§Ù„Ø²ÙŠØ§Ø±Ø©", GREEN) {
                changeStatus(booking.id, "COMPLETED")
            }
        }

        if (booking.latitude != null && booking.longitude != null) {
            addButton(root, "ðŸ—ºï¸ ÙØªØ­ Ù…ÙˆÙ‚Ø¹ Ø§Ù„Ù…Ø±ÙŠØ¶") {
                openMaps(booking.latitude, booking.longitude)
            }
        }
    }

    private fun acceptBooking(bookingId: String) {
        val currentUser =
            SupabaseManager.client.auth.currentUserOrNull()

        if (currentUser == null) {
            showError(
                "Ù„Ù… ÙŠØªÙ… ØªØ³Ø¬ÙŠÙ„ Ø¯Ø®ÙˆÙ„ Ø§Ù„Ù…Ù…Ø±Ø¶.\n\n" +
                    "ÙŠØ¬Ø¨ ØªØ³Ø¬ÙŠÙ„ Ø§Ù„Ø¯Ø®ÙˆÙ„ Ø£ÙˆÙ„Ø§Ù‹ Ø«Ù… Ù…Ø­Ø§ÙˆÙ„Ø© Ù‚Ø¨ÙˆÙ„ Ø§Ù„Ø·Ù„Ø¨."
            )
            return
        }

        val nurseId = currentUser.id

        val loading = ProgressDialog(this).apply {
            setMessage("Ø¬Ø§Ø±ÙŠ Ù‚Ø¨ÙˆÙ„ Ø§Ù„Ø·Ù„Ø¨...")
            setCancelable(false)
            show()
        }

        scope.launch {
            try {
                SupabaseManager.client
                    .from("bookings")
                    .update(
                        mapOf(
                            "nurse_id" to nurseId,
                            "status" to "ACCEPTED"
                        )
                    ) {
                        filter {
                            eq("id", bookingId)
                            eq("status", "PENDING")
                        }
                    }

                loading.dismiss()

                Toast.makeText(
                    this@NurseActivity,
                    "ØªÙ… Ù‚Ø¨ÙˆÙ„ Ø§Ù„Ø·Ù„Ø¨ Ø¨Ù†Ø¬Ø§Ø­ âœ…",
                    Toast.LENGTH_SHORT
                ).show()

                showBookings()

            } catch (e: Exception) {
                loading.dismiss()

                showError(
                    "ØªØ¹Ø°Ø± Ù‚Ø¨ÙˆÙ„ Ø§Ù„Ø·Ù„Ø¨\n\n" +
                        (e.message ?: "Ø®Ø·Ø£ ØºÙŠØ± Ù…Ø¹Ø±ÙˆÙ")
                )
            }
        }
    }

    private fun changeStatus(
        bookingId: String,
        status: String
    ) {
        val currentUser =
            SupabaseManager.client.auth.currentUserOrNull()

        if (currentUser == null) {
            showError("Ù„Ù… ÙŠØªÙ… ØªØ³Ø¬ÙŠÙ„ Ø¯Ø®ÙˆÙ„ Ø§Ù„Ù…Ù…Ø±Ø¶.")
            return
        }

        val loading = ProgressDialog(this).apply {
            setMessage("Ø¬Ø§Ø±ÙŠ ØªØ­Ø¯ÙŠØ« Ø­Ø§Ù„Ø© Ø§Ù„Ø·Ù„Ø¨...")
            setCancelable(false)
            show()
        }

        scope.launch {
            try {
                SupabaseManager.client
                    .from("bookings")
                    .update(mapOf("status" to status)) {
                        filter {
                            eq("id", bookingId)
                            eq("nurse_id", currentUser.id)
                        }
                    }

                loading.dismiss()

                Toast.makeText(
                    this@NurseActivity,
                    "ØªÙ… ØªØ­Ø¯ÙŠØ« Ø§Ù„Ø­Ø§Ù„Ø© Ø¨Ù†Ø¬Ø§Ø­ âœ…",
                    Toast.LENGTH_SHORT
                ).show()

                showBookings()

            } catch (e: Exception) {
                loading.dismiss()
                showError(e.message ?: "ØªØ¹Ø°Ø± ØªØ­Ø¯ÙŠØ« Ø§Ù„Ø­Ø§Ù„Ø©")
            }
        }
    }

    private fun showError(message: String) {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("ØªÙ†Ø¨ÙŠÙ‡")
            .setMessage(message)
            .setPositiveButton("Ø­Ø³Ù†Ù‹Ø§", null)
            .show()
    }

    private fun openMaps(
        latitude: Double,
        longitude: Double
    ) {
        try {
            val uri =
                Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")

            startActivity(
                Intent(Intent.ACTION_VIEW, uri)
            )

        } catch (_: Exception) {
            Toast.makeText(
                this,
                "ØªØ¹Ø°Ø± ÙØªØ­ Ø§Ù„Ø®Ø±Ø§Ø¦Ø·",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun serviceName(id: String): String =
        when (id) {
            "11111111-1111-4111-8111-111111111111" ->
                "Ø²ÙŠØ§Ø±Ø© ØªÙ…Ø±ÙŠØ¶ Ù…Ù†Ø²Ù„ÙŠØ©"

            "22222222-2222-4222-8222-222222222222" ->
                "Ù‚ÙŠØ§Ø³ Ø¶ØºØ· ÙˆØ³ÙƒØ±"

            "33333333-3333-4333-8333-333333333333" ->
                "ØªØºÙŠÙŠØ± Ø§Ù„Ø¶Ù…Ø§Ø¯"

            "44444444-4444-4444-8444-444444444444" ->
                "Ø¥Ø¹Ø·Ø§Ø¡ Ø§Ù„Ø­Ù‚Ù†"

            "55555555-5555-4555-8555-555555555555" ->
                "ØªØ±ÙƒÙŠØ¨ Ø§Ù„Ù…Ø­Ø§Ù„ÙŠÙ„"

            "66666666-6666-4666-8666-666666666666" ->
                "Ø±Ø¹Ø§ÙŠØ© ÙƒØ¨Ø§Ø± Ø§Ù„Ø³Ù†"

            else ->
                "Ø®Ø¯Ù…Ø© ØªÙ…Ø±ÙŠØ¶ÙŠØ©"
        }

    private fun statusText(status: String): String =
        when (status) {
            "PENDING" ->
                "ðŸŸ¡ Ø¨Ø§Ù†ØªØ¸Ø§Ø± Ù‚Ø¨ÙˆÙ„ Ø§Ù„Ù…Ù…Ø±Ø¶"

            "ACCEPTED" ->
                "ðŸ”µ ØªÙ… Ù‚Ø¨ÙˆÙ„ Ø§Ù„Ø·Ù„Ø¨"

            "ON_THE_WAY" ->
                "ðŸš— Ø§Ù„Ù…Ù…Ø±Ø¶ ÙÙŠ Ø§Ù„Ø·Ø±ÙŠÙ‚"

            "IN_PROGRESS" ->
                "ðŸ©º Ø¨Ø¯Ø£Øª Ø§Ù„Ø²ÙŠØ§Ø±Ø©"

            "COMPLETED" ->
                "ðŸŸ¢ Ø§ÙƒØªÙ…Ù„Øª Ø§Ù„Ø²ÙŠØ§Ø±Ø©"

            "CANCELLED" ->
                "ðŸ”´ ØªÙ… Ø¥Ù„ØºØ§Ø¡ Ø§Ù„Ø·Ù„Ø¨"

            else ->
                status
        }

    private fun statusColor(status: String): Int =
        when (status) {
            "COMPLETED" -> GREEN
            "CANCELLED" -> RED
            "ACCEPTED",
            "ON_THE_WAY",
            "IN_PROGRESS" -> BLUE
            else -> Color.rgb(185, 125, 0)
        }
}
