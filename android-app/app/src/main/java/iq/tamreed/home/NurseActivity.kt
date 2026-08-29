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

        header.addView(label("\uD83D\uDC68\u200D\u2695\uFE0F", 48f, DARK_BLUE))
        header.addView(label("\u0644\u0648\u062D\u0629 \u0627\u0644\u0645\u0645\u0631\u0636", 28f, DARK_BLUE))
        header.addView(label("\u0625\u062F\u0627\u0631\u0629 \u0637\u0644\u0628\u0627\u062A \u0627\u0644\u062A\u0645\u0631\u064A\u0636 \u0627\u0644\u0645\u0646\u0632\u0644\u064A", 16f, GRAY))

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 0, 0, dp(20))
            }
        )

        addButton(root, "\uD83D\uDCCB \u0639\u0631\u0636 \u0637\u0644\u0628\u0627\u062A \u0627\u0644\u0645\u0631\u0636\u0649") { showBookings() }
        addButton(root, "\uD83D\uDD04 \u062A\u062D\u062F\u064A\u062B \u0627\u0644\u0637\u0644\u0628\u0627\u062A") { showBookings() }
        addButton(root, "\uD83C\uDFE0 \u0627\u0644\u0639\u0648\u062F\u0629 \u0644\u0644\u062A\u0637\u0628\u064A\u0642") { finish() }

        root.addView(
            label("\u064A\u0645\u0643\u0646 \u0644\u0644\u0645\u0645\u0631\u0636 \u0645\u062A\u0627\u0628\u0639\u0629 \u0627\u0644\u0637\u0644\u0628\u0627\u062A \u0648\u062A\u062D\u062F\u064A\u062B \u062D\u0627\u0644\u062A\u0647\u0627.", 14f, GRAY)
        )

        setContentView(scroll(root))
    }

    private fun showBookings() {
        val root = root()

        root.addView(label("\uD83D\uDCCB \u0637\u0644\u0628\u0627\u062A \u0627\u0644\u0645\u0631\u0636\u0649", 29f, DARK_BLUE))
        root.addView(label("\u0627\u0644\u0637\u0644\u0628\u0627\u062A \u0627\u0644\u0645\u0648\u062C\u0648\u062F\u0629 \u0641\u064A \u0627\u0644\u0646\u0638\u0627\u0645", 16f, GRAY))

        val loading = label("\u23F3 \u062C\u0627\u0631\u064A \u062A\u062D\u0645\u064A\u0644 \u0627\u0644\u0637\u0644\u0628\u0627\u062A...", 16f, GRAY)

        root.addView(
            loading,
            LinearLayout.LayoutParams(-1, dp(80))
        )

        addButton(root, "\u21A9\uFE0F \u0627\u0644\u0639\u0648\u062F\u0629") { showHome() }
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
                        label("\uD83D\uDCED\n\u0644\u0627 \u062A\u0648\u062C\u062F \u0637\u0644\u0628\u0627\u062A \u062D\u0627\u0644\u064A\u0627\u064B", 20f, DARK_BLUE),
                        LinearLayout.LayoutParams(-1, dp(150))
                    )
                    return@launch
                }

                bookings
                    .sortedByDescending { it.created_at }
                    .forEach { addBooking(root, it) }

            } catch (e: Exception) {
                loading.text =
                    "\u26A0\uFE0F \u062A\u0639\u0630\u0631 \u062A\u062D\u0645\u064A\u0644 \u0627\u0644\u0637\u0644\u0628\u0627\u062A\n\n" +
                        (e.message ?: "\u062E\u0637\u0623 \u063A\u064A\u0631 \u0645\u0639\u0631\u0648\u0641")
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
            label("\uD83E\uDE7A ${serviceName(booking.service_id)}", 19f, DARK_BLUE)
        )
        card.addView(
            label("\uD83D\uDC64 \u0627\u0644\u0645\u0631\u064A\u0636: ${booking.address}", 15f, TEXT)
        )
        card.addView(
            label("\uD83D\uDCC5 \u0627\u0644\u0645\u0648\u0639\u062F: ${booking.scheduled_at}", 14f, GRAY)
        )
        card.addView(
            label(
                "\uD83D\uDCCC ${statusText(booking.status)}",
                16f,
                statusColor(booking.status)
            )
        )

        if (!booking.nurse_id.isNullOrBlank()) {
            card.addView(
                label(
                    "\uD83D\uDC68\u200D\u2695\uFE0F \u062A\u0645 \u062A\u0639\u064A\u064A\u0646 \u0627\u0644\u0645\u0645\u0631\u0636 \u0644\u0647\u0630\u0627 \u0627\u0644\u0637\u0644\u0628",
                    14f,
                    GREEN
                )
            )
        }

        if (!booking.notes.isNullOrBlank()) {
            card.addView(label("\uD83D\uDCDD ${booking.notes}", 14f, GRAY))
        }

        root.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, dp(5), 0, dp(3))
            }
        )

        when (booking.status) {
            "PENDING" -> addButton(root, "\u2705 \u0642\u0628\u0648\u0644 \u0627\u0644\u0637\u0644\u0628", GREEN) {
                acceptBooking(booking.id)
            }

            "ACCEPTED" -> addButton(root, "\uD83D\uDE97 \u0627\u0644\u0645\u0645\u0631\u0636 \u0641\u064A \u0627\u0644\u0637\u0631\u064A\u0642") {
                changeStatus(booking.id, "ON_THE_WAY")
            }

            "ON_THE_WAY" -> addButton(root, "\uD83E\uDE7A \u0628\u062F\u0623\u062A \u0627\u0644\u0632\u064A\u0627\u0631\u0629") {
                changeStatus(booking.id, "IN_PROGRESS")
            }

            "IN_PROGRESS" -> addButton(root, "\u2705 \u0625\u0646\u0647\u0627\u0621 \u0627\u0644\u0632\u064A\u0627\u0631\u0629", GREEN) {
                changeStatus(booking.id, "COMPLETED")
            }
        }

        if (booking.latitude != null && booking.longitude != null) {
            addButton(root, "\uD83D\uDDFA\uFE0F \u0641\u062A\u062D \u0645\u0648\u0642\u0639 \u0627\u0644\u0645\u0631\u064A\u0636") {
                openMaps(booking.latitude, booking.longitude)
            }
        }
    }

    private fun acceptBooking(bookingId: String) {
        val currentUser =
            SupabaseManager.client.auth.currentUserOrNull()

        if (currentUser == null) {
            showError(
                "\u0644\u0645 \u064A\u062A\u0645 \u062A\u0633\u062C\u064A\u0644 \u062F\u062E\u0648\u0644 \u0627\u0644\u0645\u0645\u0631\u0636.\n\n" +
                    "\u064A\u062C\u0628 \u062A\u0633\u062C\u064A\u0644 \u0627\u0644\u062F\u062E\u0648\u0644 \u0623\u0648\u0644\u0627\u064B \u062B\u0645 \u0645\u062D\u0627\u0648\u0644\u0629 \u0642\u0628\u0648\u0644 \u0627\u0644\u0637\u0644\u0628."
            )
            return
        }

        val nurseId = currentUser.id

        val loading = ProgressDialog(this).apply {
            setMessage("\u062C\u0627\u0631\u064A \u0642\u0628\u0648\u0644 \u0627\u0644\u0637\u0644\u0628...")
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
                    "\u062A\u0645 \u0642\u0628\u0648\u0644 \u0627\u0644\u0637\u0644\u0628 \u0628\u0646\u062C\u0627\u062D \u2705",
                    Toast.LENGTH_SHORT
                ).show()

                showBookings()

            } catch (e: Exception) {
                loading.dismiss()

                showError(
                    "\u062A\u0639\u0630\u0631 \u0642\u0628\u0648\u0644 \u0627\u0644\u0637\u0644\u0628\n\n" +
                        (e.message ?: "\u062E\u0637\u0623 \u063A\u064A\u0631 \u0645\u0639\u0631\u0648\u0641")
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
            showError("\u0644\u0645 \u064A\u062A\u0645 \u062A\u0633\u062C\u064A\u0644 \u062F\u062E\u0648\u0644 \u0627\u0644\u0645\u0645\u0631\u0636.")
            return
        }

        val loading = ProgressDialog(this).apply {
            setMessage("\u062C\u0627\u0631\u064A \u062A\u062D\u062F\u064A\u062B \u062D\u0627\u0644\u0629 \u0627\u0644\u0637\u0644\u0628...")
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
                    "\u062A\u0645 \u062A\u062D\u062F\u064A\u062B \u0627\u0644\u062D\u0627\u0644\u0629 \u0628\u0646\u062C\u0627\u062D \u2705",
                    Toast.LENGTH_SHORT
                ).show()

                showBookings()

            } catch (e: Exception) {
                loading.dismiss()
                showError(e.message ?: "\u062A\u0639\u0630\u0631 \u062A\u062D\u062F\u064A\u062B \u0627\u0644\u062D\u0627\u0644\u0629")
            }
        }
    }

    private fun showError(message: String) {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("\u062A\u0646\u0628\u064A\u0647")
            .setMessage(message)
            .setPositiveButton("\u062D\u0633\u0646\u064B\u0627", null)
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
                "\u062A\u0639\u0630\u0631 \u0641\u062A\u062D \u0627\u0644\u062E\u0631\u0627\u0626\u0637",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun serviceName(id: String): String =
        when (id) {
            "11111111-1111-4111-8111-111111111111" ->
                "\u0632\u064A\u0627\u0631\u0629 \u062A\u0645\u0631\u064A\u0636 \u0645\u0646\u0632\u0644\u064A\u0629"

            "22222222-2222-4222-8222-222222222222" ->
                "\u0642\u064A\u0627\u0633 \u0636\u063A\u0637 \u0648\u0633\u0643\u0631"

            "33333333-3333-4333-8333-333333333333" ->
                "\u062A\u063A\u064A\u064A\u0631 \u0627\u0644\u0636\u0645\u0627\u062F"

            "44444444-4444-4444-8444-444444444444" ->
                "\u0625\u0639\u0637\u0627\u0621 \u0627\u0644\u062D\u0642\u0646"

            "55555555-5555-4555-8555-555555555555" ->
                "\u062A\u0631\u0643\u064A\u0628 \u0627\u0644\u0645\u062D\u0627\u0644\u064A\u0644"

            "66666666-6666-4666-8666-666666666666" ->
                "\u0631\u0639\u0627\u064A\u0629 \u0643\u0628\u0627\u0631 \u0627\u0644\u0633\u0646"

            else ->
                "\u062E\u062F\u0645\u0629 \u062A\u0645\u0631\u064A\u0636\u064A\u0629"
        }

    private fun statusText(status: String): String =
        when (status) {
            "PENDING" ->
                "\uD83D\uDFE1 \u0628\u0627\u0646\u062A\u0638\u0627\u0631 \u0642\u0628\u0648\u0644 \u0627\u0644\u0645\u0645\u0631\u0636"

            "ACCEPTED" ->
                "\uD83D\uDD35 \u062A\u0645 \u0642\u0628\u0648\u0644 \u0627\u0644\u0637\u0644\u0628"

            "ON_THE_WAY" ->
                "\uD83D\uDE97 \u0627\u0644\u0645\u0645\u0631\u0636 \u0641\u064A \u0627\u0644\u0637\u0631\u064A\u0642"

            "IN_PROGRESS" ->
                "\uD83E\uDE7A \u0628\u062F\u0623\u062A \u0627\u0644\u0632\u064A\u0627\u0631\u0629"

            "COMPLETED" ->
                "\uD83D\uDFE2 \u0627\u0643\u062A\u0645\u0644\u062A \u0627\u0644\u0632\u064A\u0627\u0631\u0629"

            "CANCELLED" ->
                "\uD83D\uDD34 \u062A\u0645 \u0625\u0644\u063A\u0627\u0621 \u0627\u0644\u0637\u0644\u0628"

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
