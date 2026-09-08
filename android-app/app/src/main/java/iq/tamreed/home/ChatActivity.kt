package iq.tamreed.home

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
data class ChatMessage(
    val id: String? = null,
    val booking_id: String,
    val sender_id: String,
    val receiver_id: String,
    val message: String,
    val created_at: String? = null
)

@Serializable
data class ChatMessageInsert(
    val booking_id: String,
    val sender_id: String,
    val receiver_id: String,
    val message: String
)

class ChatActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val GREEN = Color.rgb(35, 145, 85)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val WHITE = Color.WHITE
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val BORDER = Color.rgb(218, 224, 229)
    private val SENT_BG = Color.rgb(5, 62, 105)
    private val RECEIVED_BG = Color.WHITE

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private var bookingId = ""
    private var receiverId = ""
    private var receiverName = "الممرض"
    private var currentUserId = ""

    private lateinit var messagesContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private lateinit var loadingText: TextView

    private var lastRenderedSignature = ""
    private var sending = false

    // رسائل محلية مؤقتة تمنع اختفاء الرسالة من الشاشة إذا تأخر
    // تحديث Supabase أو تأخر ظهورها في نتيجة SELECT.
    private val pendingLocalMessages = mutableListOf<ChatMessage>()

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                loadMessages(silent = true)
                handler.postDelayed(this, 3000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookingId = intent.getStringExtra(EXTRA_BOOKING_ID)?.trim().orEmpty()
        receiverId = intent.getStringExtra(EXTRA_RECEIVER_ID)?.trim().orEmpty()
        receiverName =
            intent.getStringExtra(EXTRA_RECEIVER_NAME)?.trim().takeUnless { it.isNullOrBlank() }
                ?: "الممرض"

        val user = SupabaseManager.client.auth.currentUserOrNull()
        currentUserId = user?.id.orEmpty()

        if (bookingId.isBlank() || receiverId.isBlank() || currentUserId.isBlank()) {
            Toast.makeText(
                this,
                "تعذر فتح المحادثة: بيانات الطلب أو المستخدم غير مكتملة.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        buildUi()
        loadMessages(silent = false)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, 3000L)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshRunnable)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
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
        color: Int = WHITE,
        strokeColor: Int = BORDER,
        radius: Int = 16
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
        }

    private fun buildUi() {
        window.statusBarColor = NAVY
        window.navigationBarColor = LIGHT_GRAY

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)
        }

        // شريط المحادثة
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 0)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            elevation = dp(2).toFloat()
        }

        val back = TextView(this).apply {
            text = "‹"
            textSize = 38f
            setTextColor(NAVY)
            gravity = Gravity.CENTER
            includeFontPadding = true
            setOnClickListener { finish() }
        }

        header.addView(
            back,
            LinearLayout.LayoutParams(dp(48), dp(52))
        )

        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        identity.addView(
            TextView(this).apply {
                text = receiverName
                textSize = 18f
                setTextColor(NAVY)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.RIGHT
                includeFontPadding = true
            },
            LinearLayout.LayoutParams(-1, dp(29))
        )

        identity.addView(
            TextView(this).apply {
                text = "محادثة مرتبطة بالطلب • آمنة"
                textSize = 11f
                setTextColor(GREEN)
                gravity = Gravity.RIGHT
                includeFontPadding = true
            },
            LinearLayout.LayoutParams(-1, dp(22))
        )

        header.addView(
            identity,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )

        val medicalIcon = TextView(this).apply {
            text = "✚"
            textSize = 25f
            setTextColor(WHITE)
            gravity = Gravity.CENTER
            background = rounded(NAVY, 50)
        }

        header.addView(
            medicalIcon,
            LinearLayout.LayoutParams(dp(48), dp(48))
        )

        root.addView(header, LinearLayout.LayoutParams(-1, dp(68)))

        // معلومات الطلب
        val bookingInfo = TextView(this).apply {
            text = "رقم الطلب: ${bookingId.take(8)}…"
            textSize = 11f
            setTextColor(GRAY)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(Color.rgb(235, 245, 251), 0)
            setPadding(dp(8), 0, dp(8), 0)
        }

        root.addView(bookingInfo, LinearLayout.LayoutParams(-1, dp(32)))

        // منطقة الرسائل
        scrollView = ScrollView(this).apply {
            isFillViewport = true
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LIGHT_GRAY)
            clipToPadding = false
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        messagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.TOP
        }

        scrollView.addView(
            messagesContainer,
            FrameLayout.LayoutParams(-1, -2)
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(0, 0, 1f)
        )

        loadingText = TextView(this).apply {
            text = "جاري تحميل المحادثة..."
            textSize = 13f
            setTextColor(GRAY)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        root.addView(loadingText, LinearLayout.LayoutParams(-1, dp(30)))

        // شريط إرسال الرسالة
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(WHITE, 0)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            elevation = dp(3).toFloat()
        }

        input = EditText(this).apply {
            hint = "اكتب رسالتك..."
            textSize = 16f
            setTextColor(TEXT)
            setHintTextColor(GRAY)
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bordered(WHITE, BORDER, 22)
            setPadding(dp(16), 0, dp(16), 0)
            maxLines = 4
            minLines = 1
        }

        composer.addView(
            input,
            LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                marginEnd = dp(7)
            }
        )

        sendButton = Button(this).apply {
            text = "إرسال"
            textSize = 15f
            isAllCaps = false
            setTextColor(WHITE)
            gravity = Gravity.CENTER
            background = rounded(NAVY, 20)
            setOnClickListener { sendMessage() }
        }

        composer.addView(
            sendButton,
            LinearLayout.LayoutParams(dp(86), dp(52))
        )

        root.addView(composer, LinearLayout.LayoutParams(-1, dp(68)))

        setContentView(root)
    }

    private fun loadMessages(silent: Boolean) {
        if (!silent) {
            loadingText.visibility = View.VISIBLE
        }

        scope.launch {
            try {
                val serverMessages = SupabaseManager.client
                    .from("chat_messages")
                    .select {
                        filter {
                            eq("booking_id", bookingId)
                        }
                    }
                    .decodeList<ChatMessage>()
                    .sortedBy { it.created_at ?: "" }

                // إذا وصلت الرسالة إلى الخادم، نحذف النسخة المحلية المؤقتة
                // المطابقة لها حتى لا تتكرر.
                if (serverMessages.isNotEmpty()) {
                    pendingLocalMessages.removeAll { local ->
                        serverMessages.any { server ->
                            server.sender_id == local.sender_id &&
                            server.receiver_id == local.receiver_id &&
                            server.message == local.message
                        }
                    }
                }

                val result = (serverMessages + pendingLocalMessages)
                    .distinctBy {
                        it.id ?: "local:${it.sender_id}:${it.message}:${it.created_at}"
                    }
                    .sortedBy { it.created_at ?: "" }

                val signature = result.joinToString("|") {
                    "${it.id}:${it.sender_id}:${it.receiver_id}:${it.message}:${it.created_at}"
                }

                if (signature != lastRenderedSignature) {
                    val wasAtBottom =
                        scrollView.getChildAt(0)?.let { child ->
                            scrollView.scrollY + scrollView.height >= child.height - dp(80)
                        } ?: true

                    lastRenderedSignature = signature
                    renderMessages(result)

                    if (wasAtBottom || result.isNotEmpty()) {
                        scrollView.post {
                            scrollView.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                }

                loadingText.visibility = View.GONE
            } catch (e: Exception) {
                loadingText.text =
                    if (silent) "تعذر تحديث المحادثة" else
                        "تعذر تحميل المحادثة\n${e.message ?: ""}"
                loadingText.visibility = View.VISIBLE
            }
        }
    }

    private fun renderMessages(messages: List<ChatMessage>) {
        messagesContainer.removeAllViews()

        if (messages.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }

            empty.addView(
                TextView(this).apply {
                    text = "✉"
                    textSize = 42f
                    setTextColor(NAVY)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(-1, dp(70))
            )

            empty.addView(
                TextView(this).apply {
                    text = "ابدأ المحادثة"
                    textSize = 19f
                    setTextColor(NAVY)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(-1, dp(38))
            )

            empty.addView(
                TextView(this).apply {
                    text = "يمكنك التواصل مع الطرف الآخر بخصوص طلب التمريض."
                    textSize = 13f
                    setTextColor(GRAY)
                    gravity = Gravity.CENTER
                },
                LinearLayout.LayoutParams(-1, dp(34))
            )

            messagesContainer.addView(
                empty,
                LinearLayout.LayoutParams(-1, dp(150))
            )
            return
        }

        messages.forEach { message ->
            addMessageBubble(message)
        }
    }

    private fun addMessageBubble(message: ChatMessage) {
        val mine = message.sender_id == currentUserId

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (mine) Gravity.START else Gravity.END
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, dp(3), 0, dp(3))
        }

        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = if (mine) Gravity.RIGHT else Gravity.LEFT
            background = rounded(
                if (mine) SENT_BG else RECEIVED_BG,
                18
            )
            setPadding(dp(13), dp(8), dp(13), dp(7))
            if (!mine) {
                elevation = dp(1).toFloat()
            }
        }

        val messageText = TextView(this).apply {
            text = message.message
            textSize = 16f
            setTextColor(if (mine) WHITE else TEXT)
            gravity = if (mine) Gravity.RIGHT else Gravity.LEFT
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            includeFontPadding = true
        }

        bubble.addView(
            messageText,
            LinearLayout.LayoutParams(
                dp(230),
                -2
            )
        )

        val timeText = TextView(this).apply {
            text = formatTime(message.created_at)
            textSize = 9f
            setTextColor(if (mine) Color.rgb(215, 230, 240) else GRAY)
            gravity = if (mine) Gravity.RIGHT else Gravity.LEFT
            includeFontPadding = true
        }

        bubble.addView(
            timeText,
            LinearLayout.LayoutParams(
                dp(230),
                dp(17)
            )
        )

        row.addView(
            bubble,
            LinearLayout.LayoutParams(
                dp(250),
                -2
            )
        )

        messagesContainer.addView(row)
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()

        if (message.isBlank() || sending) return

        sending = true
        sendButton.isEnabled = false
        sendButton.text = "..."

        scope.launch {
            try {
                SupabaseManager.client
                    .from("chat_messages")
                    .insert(
                        ChatMessageInsert(
                            booking_id = bookingId,
                            sender_id = currentUserId,
                            receiver_id = receiverId,
                            message = message
                        )
                    )

                // عرض الرسالة فوراً وعدم حذفها من الواجهة إذا تأخر SELECT.
                pendingLocalMessages.add(
                    ChatMessage(
                        id = "local-${System.nanoTime()}",
                        booking_id = bookingId,
                        sender_id = currentUserId,
                        receiver_id = receiverId,
                        message = message,
                        created_at = ""
                    )
                )

                input.setText("")
                loadMessages(silent = true)
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChatActivity,
                    "تعذر إرسال الرسالة\n${e.message ?: "حاول مرة أخرى"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                sending = false
                sendButton.isEnabled = true
                sendButton.text = "إرسال"
            }
        }
    }

    private fun formatTime(value: String?): String {
        if (value.isNullOrBlank()) return ""

        return try {
            val timePart = value.substringAfter("T").substringBefore(".")
            if (timePart.length >= 5) timePart.substring(0, 5) else timePart
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        const val EXTRA_BOOKING_ID = "booking_id"
        const val EXTRA_RECEIVER_ID = "receiver_id"
        const val EXTRA_RECEIVER_NAME = "receiver_name"
    }
}
