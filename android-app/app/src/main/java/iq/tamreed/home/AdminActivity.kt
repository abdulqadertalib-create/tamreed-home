package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import java.time.Instant
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class AdminRecord(val user_id: String)

@Serializable
data class AdminNurseRecord(
    val id: String? = null,
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val specialty: String? = null,
    val experience_years: Int? = null,
    val city: String? = null,
    val address: String? = null,
    val is_available: Boolean? = false,
    val is_verified: Boolean? = false
)


@Serializable
data class AdminSubscriptionRequest(
    val id: String? = null,
    val nurse_id: String? = null,
    val plan_name: String? = null,
    val duration_days: Int? = null,
    val amount_iqd: Int? = null,
    val status: String? = null,
    val created_at: String? = null
)

class AdminActivity : AppCompatActivity() {
    private val sessionPrefs by lazy {
        getSharedPreferences("tamreed_session", MODE_PRIVATE)
    }
    private val navy = Color.rgb(5, 62, 105)
    private val blue = Color.rgb(31, 115, 176)
    private val green = Color.rgb(35, 145, 85)
    private val red = Color.rgb(180, 50, 50)
    private val gray = Color.rgb(110, 110, 110)
    private val light = Color.rgb(247, 248, 249)
    private val white = Color.WHITE
    private val border = Color.rgb(218, 224, 229)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAdmin()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun bg(color: Int, radius: Int = 18, stroke: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            if (stroke != null) setStroke(dp(1), stroke)
            cornerRadius = dp(radius).toFloat()
        }

    private fun text(value: String, size: Float, color: Int = navy, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

    private fun button(title: String, color: Int, action: () -> Unit) =
        Button(this).apply {
            text = title
            textSize = 16f
            isAllCaps = false
            setTextColor(white)
            background = bg(color, 15)
            setOnClickListener { action() }
        }

    private fun checkAdmin() {
        val user = SupabaseManager.client.auth.currentUserOrNull()
        if (user == null) {
            showLogin()
            return
        }

        scope.launch {
            try {
                val admins = SupabaseManager.client.from("admin_users").select {
                    filter { eq("user_id", user.id) }
                }.decodeList<AdminRecord>()

                if (admins.isEmpty()) {
                    showNotAdmin()
                } else {
                    sessionPrefs.edit().putString("role", "admin").apply()
                    showDashboard()
                }
            } catch (e: Exception) {
                showError("تعذر التحقق من صلاحيات الإدارة",
                    e.message ?: "تأكد من تنفيذ SQL الخاص بالإدارة.")
            }
        }
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(light)
            setPadding(dp(20), dp(30), dp(20), dp(30))
        }

        root.addView(text("🛡️", 55f))
        root.addView(text("دخول الإدارة", 28f, navy, true))
        root.addView(text("أدخل رقم هاتف حساب المدير", 16f, gray))

        val phone = EditText(this).apply {
            hint = "07810000000"
            textSize = 18f
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = bg(white, 15, border)
        }

        root.addView(phone, LinearLayout.LayoutParams(-1, dp(62)))

        root.addView(button("إرسال رمز التحقق", navy) {
            val p = normalizePhone(phone.text.toString())
            if (p == null) {
                phone.error = "رقم الهاتف العراقي غير صحيح"
                return@button
            }

            scope.launch {
                val loading = ProgressDialog.show(
                    this@AdminActivity, null, "جاري إرسال الرمز...", true, false
                )
                try {
                    SupabaseManager.client.auth.signInWith(OTP) { this.phone = p }
                    loading.dismiss()
                    showOtp(p)
                } catch (e: Exception) {
                    loading.dismiss()
                    showError("تعذر إرسال الرمز", e.message ?: "حاول مرة أخرى.")
                }
            }
        }, LinearLayout.LayoutParams(-1, dp(60)))

        root.addView(text("هذه الشاشة مخصصة للمشرف فقط.", 14f, gray))
        setContentView(root)
    }

    private fun showOtp(phone: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(light)
            setPadding(dp(20), dp(35), dp(20), dp(35))
        }

        root.addView(text("🔐", 55f))
        root.addView(text("تأكيد دخول الإدارة", 28f, navy, true))
        root.addView(text(phone, 17f, navy, true))

        val code = EditText(this).apply {
            hint = "123456"
            textSize = 22f
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        root.addView(code, LinearLayout.LayoutParams(-1, dp(65)))

        root.addView(button("تأكيد الرمز", navy) {
            val token = code.text.toString().trim()
            if (token.length != 6) {
                code.error = "أدخل 6 أرقام"
                return@button
            }

            scope.launch {
                val loading = ProgressDialog.show(
                    this@AdminActivity, null, "جاري التحقق...", true, false
                )
                try {
                    SupabaseManager.client.auth.verifyPhoneOtp(
                        type = OtpType.Phone.SMS,
                        phone = phone,
                        token = token
                    )
                    loading.dismiss()
                    checkAdmin()
                } catch (e: Exception) {
                    loading.dismiss()
                    showError("رمز التحقق غير صحيح", e.message ?: "حاول مرة أخرى.")
                }
            }
        }, LinearLayout.LayoutParams(-1, dp(60)))

        setContentView(root)
    }

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(light)
            setPadding(dp(14), dp(22), dp(14), dp(30))
        }

        root.addView(text("🛡️", 52f))
        root.addView(text("لوحة الإدارة", 28f, navy, true))
        root.addView(text("إدارة الممرضين وطلبات الاشتراك", 16f, gray))

        root.addView(button("🔄 تحديث جميع الطلبات", blue) {
            loadDashboardData(root)
        }, LinearLayout.LayoutParams(-1, dp(58)))

        val logout = button("تسجيل الخروج", navy) { signOut() }
        logout.background = bg(white, 15, navy)
        logout.setTextColor(navy)
        root.addView(logout, LinearLayout.LayoutParams(-1, dp(55)))

        setContentView(ScrollView(this).apply { addView(root) })
        loadDashboardData(root)
    }

    private fun loadDashboardData(root: LinearLayout) {
        scope.launch {
            try {
                while (root.childCount > 5) root.removeViewAt(5)

                val nurses = SupabaseManager.client
                    .from("nurses")
                    .select()
                    .decodeList<AdminNurseRecord>()

                val requests = SupabaseManager.client
                    .from("nurse_subscription_requests")
                    .select()
                    .decodeList<AdminSubscriptionRequest>()
                    .sortedByDescending { it.created_at ?: "" }

                root.addView(text("👨‍⚕️ الممرضون", 22f, navy, true))
                root.addView(text(
                    "بانتظار الاعتماد: ${nurses.count { it.is_verified != true }}    |    معتمد: ${nurses.count { it.is_verified == true }}",
                    16f, navy, true
                ))

                if (nurses.isEmpty()) {
                    root.addView(text("لا توجد حسابات ممرضين حاليًا.", 16f, gray))
                } else {
                    nurses.sortedBy { it.is_verified == true }
                        .forEach { nurse -> addNurseCard(root, nurse) }
                }

                root.addView(text("💳 طلبات اشتراك الممرضين", 22f, navy, true).apply {
                    setPadding(dp(8), dp(20), dp(8), dp(8))
                })

                val pendingRequests = requests.count { it.status.equals("PENDING", true) }
                root.addView(text("طلبات بانتظار المراجعة: $pendingRequests", 16f, navy, true))

                if (requests.isEmpty()) {
                    root.addView(text("لا توجد طلبات اشتراك.", 16f, gray))
                } else {
                    val nursesByUserId = nurses.mapNotNull { n ->
                        n.user_id?.let { it to n }
                    }.toMap()
                    requests.forEach { request ->
                        addSubscriptionRequestCard(root, request, nursesByUserId[request.nurse_id])
                    }
                }
            } catch (e: Exception) {
                showError(
                    "تعذر تحميل لوحة الإدارة",
                    e.message ?: "تحقق من صلاحيات الإدارة ووجود جدول nurse_subscription_requests."
                )
            }
        }
    }

    private fun addNurseCard(root: LinearLayout, nurse: AdminNurseRecord) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(white, 20, border)
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        card.addView(text(nurse.full_name ?: "بدون اسم", 20f, navy, true))
        card.addView(text("التخصص: ${nurse.specialty ?: "غير محدد"}", 15f, gray))
        card.addView(text("الخبرة: ${nurse.experience_years ?: 0} سنوات", 15f, gray))
        card.addView(text("المحافظة/المدينة: ${nurse.city ?: "غير محدد"}", 15f, gray))
        card.addView(text("العنوان: ${nurse.address ?: "غير محدد"}", 15f, gray))
        card.addView(text("الهاتف: ${nurse.phone ?: "غير محدد"}", 15f, gray))

        if (nurse.is_verified == true) {
            card.addView(text("✅ معتمد", 17f, green, true))
        } else {
            card.addView(text("⏳ بانتظار الاعتماد", 17f, Color.rgb(190, 120, 20), true))
            card.addView(button("✅ اعتماد الممرض", green) {
                confirmApproval(nurse, root)
            }, LinearLayout.LayoutParams(-1, dp(56)))
            card.addView(button("❌ رفض الطلب", red) {
                confirmReject(nurse, root)
            }, LinearLayout.LayoutParams(-1, dp(52)))
        }

        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, dp(10), 0, 0)
        root.addView(card, lp)
    }

    private fun addSubscriptionRequestCard(
        root: LinearLayout,
        request: AdminSubscriptionRequest,
        nurse: AdminNurseRecord?
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(white, 20, border)
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        val nurseName = nurse?.full_name ?: "ممرض غير معروف"
        val nursePhone = nurse?.phone ?: "غير محدد"
        val status = request.status ?: "PENDING"
        val statusText = when (status.uppercase()) {
            "APPROVED" -> "✅ معتمد"
            "REJECTED" -> "❌ مرفوض"
            "EXPIRED" -> "⌛ منتهي"
            else -> "⏳ بانتظار المراجعة"
        }

        card.addView(text("${request.plan_name ?: "اشتراك"}", 20f, navy, true))
        card.addView(text("👤 اسم الممرض: $nurseName", 16f, gray, true))
        card.addView(text("📱 رقم الهاتف: $nursePhone", 16f, gray, true))
        card.addView(text("المدة: ${request.duration_days ?: 0} يومًا", 15f, gray))
        card.addView(text("المبلغ: ${(request.amount_iqd ?: 0)} د.ع", 15f, gray))
        card.addView(text(statusText, 16f, if (status.equals("APPROVED", true)) green else gray, true))

        if (status.equals("PENDING", true)) {
            card.addView(button("✅ اعتماد الاشتراك", green) {
                confirmSubscriptionApproval(request, nurse, root)
            }, LinearLayout.LayoutParams(-1, dp(56)))
            card.addView(button("❌ رفض طلب الاشتراك", red) {
                confirmSubscriptionReject(request, root)
            }, LinearLayout.LayoutParams(-1, dp(52)))
        }

        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, dp(8), 0, 0)
        root.addView(card, lp)
    }

    private fun confirmSubscriptionApproval(
        request: AdminSubscriptionRequest,
        nurse: AdminNurseRecord?,
        root: LinearLayout
    ) {
        if (request.id.isNullOrBlank() || request.nurse_id.isNullOrBlank()) {
            showError("طلب غير صالح", "لا يمكن تحديد الممرض لهذا الطلب.")
            return
        }
        if (nurse?.user_id.isNullOrBlank() || nurse.id.isNullOrBlank()) {
            showError("بيانات الممرض ناقصة", "لم يتم العثور على ملف الممرض المرتبط بهذا الطلب.")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("اعتماد الاشتراك")
            .setMessage(
                "الممرض: ${nurse.full_name ?: "غير معروف"}\n" +
                    "الهاتف: ${nurse.phone ?: "غير محدد"}\n" +
                    "الباقة: ${request.plan_name ?: "غير محددة"}\n" +
                    "المدة: ${request.duration_days ?: 0} يومًا\n\n" +
                    "هل تريد اعتماد الاشتراك؟"
            )
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("اعتماد") { _, _ ->
                approveSubscription(request, nurse, root)
            }
            .show()
    }

    private fun approveSubscription(
        request: AdminSubscriptionRequest,
        nurse: AdminNurseRecord,
        root: LinearLayout
    ) {
        val requestId = request.id ?: return
        val userId = request.nurse_id ?: return
        val duration = request.duration_days ?: return
        val start = Instant.now()
        val end = start.plusSeconds(duration.toLong() * 24L * 60L * 60L)

        scope.launch {
            val loading = ProgressDialog.show(this@AdminActivity, null, "جاري اعتماد الاشتراك...", true, false)
            try {
                SupabaseManager.client.from("nurses").update({
                    set("subscription_start", start.toString())
                    set("subscription_end", end.toString())
                    set("subscription_status", "ACTIVE")
                }) {
                    filter { eq("user_id", userId) }
                }

                SupabaseManager.client.from("nurse_subscription_requests").update({
                    set("status", "APPROVED")
                    set("reviewed_at", start.toString())
                }) {
                    filter { eq("id", requestId) }
                }

                loading.dismiss()
                Toast.makeText(
                    this@AdminActivity,
                    "تم اعتماد اشتراك ${nurse.full_name ?: "الممرض"}",
                    Toast.LENGTH_LONG
                ).show()
                loadDashboardData(root)
            } catch (e: Exception) {
                loading.dismiss()
                showError("تعذر اعتماد الاشتراك", e.message ?: "حاول مرة أخرى.")
            }
        }
    }

    private fun confirmSubscriptionReject(
        request: AdminSubscriptionRequest,
        root: LinearLayout
    ) {
        AlertDialog.Builder(this)
            .setTitle("رفض طلب الاشتراك")
            .setMessage("هل تريد رفض طلب الاشتراك هذا؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("رفض") { _, _ ->
                rejectSubscription(request, root)
            }
            .show()
    }

    private fun rejectSubscription(
        request: AdminSubscriptionRequest,
        root: LinearLayout
    ) {
        val id = request.id ?: return
        scope.launch {
            val loading = ProgressDialog.show(this@AdminActivity, null, "جاري رفض الطلب...", true, false)
            try {
                SupabaseManager.client.from("nurse_subscription_requests").update({
                    set("status", "REJECTED")
                    set("reviewed_at", Instant.now().toString())
                }) {
                    filter { eq("id", id) }
                }
                loading.dismiss()
                Toast.makeText(this@AdminActivity, "تم رفض طلب الاشتراك", Toast.LENGTH_LONG).show()
                loadDashboardData(root)
            } catch (e: Exception) {
                loading.dismiss()
                showError("تعذر رفض الطلب", e.message ?: "حاول مرة أخرى.")
            }
        }
    }

    private fun confirmApproval(nurse: AdminNurseRecord, root: LinearLayout) {
        AlertDialog.Builder(this)
            .setTitle("اعتماد الممرض")
            .setMessage("هل تريد اعتماد ${nurse.full_name ?: "هذا الممرض"}؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("اعتماد") { _, _ ->
                setVerified(nurse, true, root)
            }
            .show()
    }

    private fun confirmReject(nurse: AdminNurseRecord, root: LinearLayout) {
        AlertDialog.Builder(this)
            .setTitle("رفض طلب الممرض")
            .setMessage("سيبقى الحساب غير معتمد. هل تريد المتابعة؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("رفض") { _, _ ->
                setVerified(nurse, false, root)
            }
            .show()
    }

    private fun setVerified(
        nurse: AdminNurseRecord,
        verified: Boolean,
        root: LinearLayout
    ) {
        val id = nurse.id ?: return

        scope.launch {
            val loading = ProgressDialog.show(
                this@AdminActivity, null, "جاري الحفظ...", true, false
            )
            try {
                SupabaseManager.client.from("nurses").update({
                    set("is_verified", verified)
                }) {
                    filter { eq("id", id) }
                }

                loading.dismiss()
                Toast.makeText(
                    this@AdminActivity,
                    if (verified) "تم اعتماد الممرض" else "تم رفض الطلب",
                    Toast.LENGTH_LONG
                ).show()

                loadDashboardData(root)
            } catch (e: Exception) {
                loading.dismiss()
                showError("تعذر حفظ القرار",
                    e.message ?: "تحقق من صلاحيات الإدارة.")
            }
        }
    }

    private fun showNotAdmin() {
        AlertDialog.Builder(this)
            .setTitle("غير مصرح")
            .setMessage(
                "هذا الحساب ليس حساب إدارة. أضف user_id الخاص بالمدير إلى جدول admin_users في Supabase."
            )
            .setPositiveButton("حسنًا") { _, _ -> finish() }
            .show()
    }

    private fun signOut() {
        scope.launch {
            try {
                SupabaseManager.client.auth.signOut()
            } catch (_: Exception) {}
            sessionPrefs.edit().remove("role").apply()
            showLogin()
        }
    }

    private fun normalizePhone(value: String): String? {
        var p = value.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (p.startsWith("+964")) {
            return if (p.length == 14 && p.getOrNull(4) == '7') p else null
        }

        if (p.startsWith("00964")) p = "+" + p.substring(2)
        if (p.startsWith("07")) p = "+964" + p.substring(1)

        return if (p.length == 14 && p.startsWith("+9647")) p else null
    }

    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حسنًا", null)
            .show()
    }
}
