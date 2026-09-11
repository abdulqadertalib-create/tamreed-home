package iq.tamreed.home

import android.content.Intent
import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import java.time.Instant
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.builtin.Phone
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
data class AdminPatientRecord(
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val address: String? = null,
    val bio: String? = null,
    val avatar_url: String? = null
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
    private var adminPhone = ""
    private var passwordResetFlow = false
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

    private fun rounded(color: Int, radius: Int = 18): GradientDrawable =
        bg(color, radius)

    private fun bordered(color: Int, stroke: Int, radius: Int = 18): GradientDrawable =
        bg(color, radius, stroke)

    private fun outlineButton(title: String, action: () -> Unit): Button =
        Button(this).apply {
            text = title
            textSize = 16f
            isAllCaps = false
            setTextColor(navy)
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(white, 14, navy)
            setOnClickListener { action() }
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
        if (user == null) { showLogin(); return }
        scope.launch {
            try {
                val admins = SupabaseManager.client.from("admin_users").select {
                    filter { eq("user_id", user.id) }
                }.decodeList<AdminRecord>()
                if (admins.isEmpty()) {
                    showNotAdmin()
                } else if (!sessionPrefs.getBoolean("admin_password_configured", false)) {
                    adminPhone = user.phone ?: adminPhone
                    passwordResetFlow = false
                    showSetPasswordScreen()
                } else {
                    sessionPrefs.edit().putString("role", "admin").apply()
                    showDashboard()
                }
            } catch (e: Exception) {
                showError("تعذر التحقق من صلاحيات الإدارة", e.message ?: "تأكد من إعداد الإدارة في Supabase.")
            }
        }
    }

    private fun showLogin() {
        window.statusBarColor = white; window.navigationBarColor = white
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(white);setPadding(dp(18),dp(14),dp(18),dp(24))}
        root.addView(ImageView(this).apply{setImageResource(R.drawable.app_logo);scaleType=ImageView.ScaleType.CENTER_INSIDE;contentDescription="شعار التمريض المنزلي"},LinearLayout.LayoutParams(dp(112),dp(112)).apply{gravity=Gravity.CENTER_HORIZONTAL})
        root.addView(text("دخول الإدارة",29f,Color.rgb(79,48,170),true),LinearLayout.LayoutParams(-1,dp(46)))
        root.addView(text("لوحة التحكم والإدارة",14f,Color.rgb(79,48,170),true),LinearLayout.LayoutParams(-1,dp(30)))
        val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;background=rounded(white,24);elevation=dp(3).toFloat();setPadding(dp(14),dp(14),dp(14),dp(14))}
        card.addView(text("📱 رقم الهاتف",15f,navy,true),LinearLayout.LayoutParams(-1,dp(32)))
        val phone=EditText(this).apply{hint="07XXXXXXXXX";textSize=17f;gravity=Gravity.CENTER;inputType=InputType.TYPE_CLASS_PHONE;layoutDirection=View.LAYOUT_DIRECTION_LTR;maxLines=1;isSingleLine=true;background=bordered(white,border,16);setPadding(dp(12),0,dp(12),0)}
        card.addView(phone,LinearLayout.LayoutParams(-1,dp(52)))
        card.addView(text("🔐 كلمة المرور",15f,navy,true),LinearLayout.LayoutParams(-1,dp(32)).apply{topMargin=dp(7)})
        val password=EditText(this).apply{hint="كلمة المرور";textSize=17f;gravity=Gravity.CENTER;inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;maxLines=1;isSingleLine=true;background=bordered(white,border,16);setPadding(dp(12),0,dp(12),0)}
        card.addView(password,LinearLayout.LayoutParams(-1,dp(52)))
        card.addView(button("🔐  دخول",Color.rgb(91,63,190)){val p=normalizePhone(phone.text.toString());val pass=password.text.toString();if(p==null){phone.error="رقم الهاتف العراقي غير صحيح";return@button};if(pass.length<6){password.error="كلمة المرور 6 أحرف/أرقام على الأقل";return@button};signInAdminWithPassword(p,pass)},LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(9)})
        card.addView(outlineButton("نسيت كلمة المرور"){val p=normalizePhone(phone.text.toString());if(p==null){phone.error="أدخل رقم الهاتف أولاً";return@outlineButton};adminPhone=p;passwordResetFlow=true;sendAdminOtp(p)},LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(7)})
        card.addView(text("أو",12f,gray,true),LinearLayout.LayoutParams(-1,dp(28)))
        card.addView(outlineButton("📱  دخول بالرمز (OTP)"){val p=normalizePhone(phone.text.toString());if(p==null){phone.error="رقم الهاتف العراقي غير صحيح";return@outlineButton};adminPhone=p;passwordResetFlow=false;sendAdminOtp(p)},LinearLayout.LayoutParams(-1,dp(52)))
        root.addView(card,LinearLayout.LayoutParams(-1,-2))
        root.addView(text("🔒 حسابات الإدارة المصرح لها فقط",12f,gray,true),LinearLayout.LayoutParams(-1,dp(36)).apply{topMargin=dp(10)})
        root.addView(outlineButton("‹  العودة"){finish()},LinearLayout.LayoutParams(-1,dp(50)))
        setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)})
    }

    private fun sendAdminOtp(phone: String) {
        scope.launch {
            val loading = ProgressDialog.show(this@AdminActivity, null, "جاري إرسال رمز التحقق...", true, false)
            try {
                SupabaseManager.client.auth.signInWith(OTP) { this.phone = phone }
                loading.dismiss(); showOtp(phone)
            } catch (e: Exception) {
                loading.dismiss(); showError("تعذر إرسال الرمز", e.message ?: "تأكد من إعداد Phone Auth في Supabase.")
            }
        }
    }

    private fun signInAdminWithPassword(phone: String, password: String) {
        scope.launch {
            val loading = ProgressDialog.show(this@AdminActivity, null, "جاري تسجيل الدخول...", true, false)
            try {
                SupabaseManager.client.auth.signInWith(Phone) { this.phone = phone; this.password = password }
                val user = SupabaseManager.client.auth.currentUserOrNull() ?: throw IllegalStateException("تعذر إنشاء جلسة الدخول")
                val admins = SupabaseManager.client.from("admin_users").select { filter { eq("user_id", user.id) } }.decodeList<AdminRecord>()
                if (admins.isEmpty()) { SupabaseManager.client.auth.signOut(); loading.dismiss(); showNotAdmin(); return@launch }
                adminPhone = phone
                sessionPrefs.edit().putString("role", "admin").putBoolean("admin_password_configured", true).apply()
                loading.dismiss(); showDashboard()
            } catch (e: Exception) {
                loading.dismiss(); showError("تعذر تسجيل الدخول", "رقم الهاتف أو كلمة المرور غير صحيحة. استخدم «نسيت كلمة المرور» إذا لزم.")
            }
        }
    }

    private fun showSetPasswordScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setBackgroundColor(light); setPadding(dp(20), dp(28), dp(20), dp(20)) }
        root.addView(text("🔐", 50f, navy, true))
        root.addView(text(if (passwordResetFlow) "تغيير كلمة المرور" else "إنشاء كلمة المرور", 27f, navy, true))
        root.addView(text(if (passwordResetFlow) "أنشئ كلمة مرور جديدة لحساب الإدارة" else "تم التحقق من رقم الهاتف. أنشئ كلمة مرور للدخول لاحقاً.", 15f, gray))
        root.addView(text(adminPhone, 16f, navy, true), LinearLayout.LayoutParams(-1, dp(35)))
        val pass = EditText(this).apply { hint = "كلمة المرور الجديدة"; textSize = 17f; gravity = Gravity.CENTER; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; background = bordered(white, border, 15) }
        val confirm = EditText(this).apply { hint = "تأكيد كلمة المرور"; textSize = 17f; gravity = Gravity.CENTER; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; background = bordered(white, border, 15) }
        root.addView(pass, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(14) })
        root.addView(confirm, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(10) })
        root.addView(button("💾 حفظ كلمة المرور", green) {
            val a = pass.text.toString(); val b = confirm.text.toString()
            if (a.length < 6) { pass.error = "استخدم 6 أحرف/أرقام على الأقل"; return@button }
            if (a != b) { confirm.error = "كلمتا المرور غير متطابقتين"; return@button }
            saveAdminPassword(a)
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })
        root.addView(outlineButton("‹ العودة") { showLogin() }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        setContentView(root)
    }

    private fun saveAdminPassword(password: String) {
        scope.launch {
            val loading = ProgressDialog.show(this@AdminActivity, null, "جاري حفظ كلمة المرور...", true, false)
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull() ?: throw IllegalStateException("انتهت جلسة التحقق")
                val admins = SupabaseManager.client.from("admin_users").select { filter { eq("user_id", user.id) } }.decodeList<AdminRecord>()
                if (admins.isEmpty()) { SupabaseManager.client.auth.signOut(); loading.dismiss(); showNotAdmin(); return@launch }
                SupabaseManager.client.auth.updateUser { this.password = password }
                sessionPrefs.edit().putString("role", "admin").putBoolean("admin_password_configured", true).apply()
                passwordResetFlow = false; loading.dismiss()
                Toast.makeText(this@AdminActivity, "تم حفظ كلمة المرور بنجاح", Toast.LENGTH_LONG).show(); showDashboard()
            } catch (e: Exception) {
                loading.dismiss()
                val msg = if (e.message?.contains("same_password", true) == true) "كلمة المرور الجديدة يجب أن تكون مختلفة عن السابقة." else (e.message ?: "حاول مرة أخرى.")
                showError("تعذر حفظ كلمة المرور", msg)
            }
        }
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
        window.statusBarColor = white
        window.navigationBarColor = light
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(light)
            setPadding(dp(16), dp(12), dp(16), dp(28))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(Color.rgb(244,239,255), 24)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        header.addView(ImageView(this).apply { setImageResource(R.drawable.app_logo); scaleType=ImageView.ScaleType.CENTER_INSIDE; contentDescription="شعار التطبيق" }, LinearLayout.LayoutParams(dp(66),dp(66)))
        header.addView(text("لوحة الإدارة",24f,Color.rgb(79,48,170),true),LinearLayout.LayoutParams(0,dp(66),1f))
        root.addView(header,LinearLayout.LayoutParams(-1,dp(92)))
        root.addView(text("إدارة المرضى والممرضين والطلبات والاشتراكات",13f,gray),LinearLayout.LayoutParams(-1,dp(34)).apply{topMargin=dp(7)})
        val stats=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
        fun stat(title:String,icon:String,color:Int)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_RTL;background=rounded(white,20);setPadding(dp(4),dp(8),dp(4),dp(8));addView(text(icon,24f,color,true),LinearLayout.LayoutParams(-1,dp(30)));addView(text(title,11f,gray,true),LinearLayout.LayoutParams(-1,dp(28)))}
        stats.addView(stat("المرضى","👥",green),LinearLayout.LayoutParams(0,dp(78),1f).apply{marginEnd=dp(4)})
        stats.addView(stat("الممرضون","👨‍⚕️",blue),LinearLayout.LayoutParams(0,dp(78),1f).apply{marginStart=dp(4);marginEnd=dp(4)})
        stats.addView(stat("الاشتراكات","💳",Color.rgb(125,82,195)),LinearLayout.LayoutParams(0,dp(78),1f).apply{marginStart=dp(4)})
        root.addView(stats,LinearLayout.LayoutParams(-1,dp(78)).apply{topMargin=dp(8)})
        root.addView(button("🔄  تحديث البيانات",blue){loadDashboardData(root)},LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(10)})
        val logout=button("تسجيل الخروج",white){signOut()};logout.setTextColor(navy);logout.background=bg(white,16,navy);root.addView(logout,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(8)})
        setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)})
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

                val patients = try {
                    SupabaseManager.client
                        .from("patients")
                        .select()
                        .decodeList<AdminPatientRecord>()
                        .sortedBy { it.full_name ?: "" }
                } catch (_: Exception) {
                    emptyList()
                }

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

                root.addView(text("👥 المرضى المسجلون", 22f, navy, true).apply {
                    setPadding(dp(8), dp(20), dp(8), dp(8))
                })
                root.addView(text("إجمالي المرضى: ${patients.size}", 16f, navy, true))

                if (patients.isEmpty()) {
                    root.addView(text("لا توجد حسابات مرضى مسجلة حاليًا.", 16f, gray))
                } else {
                    patients.forEach { patient ->
                        addPatientCard(root, patient)
                    }
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

    private fun addPatientCard(root: LinearLayout, patient: AdminPatientRecord) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(white, 20, border)
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        card.addView(text("👤 ${patient.full_name ?: "بدون اسم"}", 19f, navy, true))
        card.addView(text("📱 الهاتف: ${patient.phone ?: "غير محدد"}", 15f, gray))
        card.addView(text("📍 المدينة: ${patient.city ?: "غير محددة"}", 15f, gray))
        card.addView(text("🏠 العنوان: ${patient.address ?: "غير محدد"}", 15f, gray))
        if (!patient.bio.isNullOrBlank()) {
            card.addView(text("نبذة: ${patient.bio}", 14f, gray))
        }

        if (!patient.user_id.isNullOrBlank()) {
            card.addView(button("👤 فتح الملف الشخصي", blue) {
                startActivity(Intent(this, ProfileActivity::class.java).apply {
                    putExtra(ProfileActivity.EXTRA_ROLE, "patient")
                    putExtra(ProfileActivity.EXTRA_USER_ID, patient.user_id)
                    putExtra(ProfileActivity.EXTRA_READ_ONLY, true)
                })
            }, LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(8)
            })
        }

        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, dp(8), 0, 0)
        })
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
            sessionPrefs.edit().remove("role").remove("admin_password_configured").apply()
            adminPhone = ""
            passwordResetFlow = false
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
