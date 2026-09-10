package iq.tamreed.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.URL

@Serializable
data class ProfileRecord(
    val user_id: String,
    val full_name: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val address: String? = null,
    val bio: String? = null,
    val avatar_url: String? = null,
    val specialty: String? = null,
    val experience_years: Int? = null
)

class ProfileActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROLE = "role"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_READ_ONLY = "read_only"
    }

    private val navy = Color.rgb(5, 62, 105)
    private val blue = Color.rgb(31, 115, 176)
    private val green = Color.rgb(35, 145, 85)
    private val gray = Color.rgb(110, 110, 110)
    private val light = Color.rgb(247, 248, 249)
    private val white = Color.WHITE
    private val border = Color.rgb(218, 224, 229)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var role = "patient"
    private var targetUserId = ""
    private var readOnly = false
    private var selectedImageUri: Uri? = null
    private var avatarUrl: String? = null
    private var avatarView: ImageView? = null

    private var nameField: EditText? = null
    private var cityField: EditText? = null
    private var addressField: EditText? = null
    private var bioField: EditText? = null
    private var specialtyField: EditText? = null
    private var experienceField: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        role = intent.getStringExtra(EXTRA_ROLE)?.lowercase() ?: "patient"
        targetUserId = intent.getStringExtra(EXTRA_USER_ID)
            ?: SupabaseManager.client.auth.currentUserOrNull()?.id.orEmpty()
        readOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)

        if (targetUserId.isBlank()) {
            Toast.makeText(this, "تعذر تحديد الحساب", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        loadProfile()
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
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }

    private fun field(hint: String, value: String = "", multi: Boolean = false) =
        EditText(this).apply {
            this.hint = hint
            setText(value)
            textSize = 16f
            gravity = if (multi) Gravity.TOP or Gravity.RIGHT else Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(white, 15, border)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            if (multi) {
                minLines = 3
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
        }

    private fun loadProfile() {
        scope.launch {
            try {
                val rows = SupabaseManager.client.from(
                    if (role == "nurse") "nurses" else "patients"
                ).select {
                    filter { eq("user_id", targetUserId) }
                }.decodeList<ProfileRecord>()

                val profile = rows.firstOrNull()
                if (profile == null && !readOnly) {
                    createEmptyPatientIfNeeded()
                    renderProfile(ProfileRecord(targetUserId))
                } else if (profile == null) {
                    showError("الملف غير موجود", "لم يتم إنشاء ملف هذا المستخدم بعد.")
                } else {
                    renderProfile(profile)
                }
            } catch (e: Exception) {
                showError("تعذر تحميل الملف الشخصي", e.message ?: "تحقق من اتصال التطبيق.")
            }
        }
    }

    private suspend fun createEmptyPatientIfNeeded() {
        if (role != "patient") return
        val user = SupabaseManager.client.auth.currentUserOrNull() ?: return
        SupabaseManager.client.from("patients").insert(
            mapOf("user_id" to user.id, "phone" to user.phone)
        )
    }

    private fun renderProfile(profile: ProfileRecord) {
        avatarUrl = profile.avatar_url
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(light)
            setPadding(dp(14), dp(18), dp(14), dp(28))
        }

        root.addView(
            text(
                if (readOnly) {
                    if (role == "nurse") "👨‍⚕️ ملف الممرض" else "👤 ملف المريض"
                } else "👤 ملفي الشخصي",
                26f, navy, true
            ),
            LinearLayout.LayoutParams(-1, dp(58))
        )

        val avatarCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg(white, 22)
            setPadding(dp(12), dp(14), dp(12), dp(14))
        }

        avatarView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = bg(Color.rgb(235, 245, 251), 70)
            contentDescription = "الصورة الشخصية"
        }
        avatarCard.addView(avatarView, LinearLayout.LayoutParams(dp(130), dp(130)))

        if (!readOnly) {
            val choose = Button(this).apply {
                text = "📷 إضافة / تغيير الصورة"
                isAllCaps = false
                setTextColor(white)
                background = bg(blue, 15)
                setOnClickListener { chooseImage() }
            }
            avatarCard.addView(choose, LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(10)
            })
        }
        root.addView(avatarCard, LinearLayout.LayoutParams(-1, -2))
        loadAvatar(avatarUrl)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(white, 22)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        card.addView(text("المعلومات الشخصية", 19f, navy, true))
        nameField = field("الاسم الكامل", profile.full_name ?: "")
        card.addView(nameField, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })

        card.addView(text("رقم الهاتف: ${profile.phone ?: "غير محدد"}", 15f, gray).apply {
            gravity = Gravity.RIGHT
        }, LinearLayout.LayoutParams(-1, dp(42)))

        cityField = field("المدينة / القضاء", profile.city ?: "")
        card.addView(cityField, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })

        addressField = field("العنوان / المنطقة", profile.address ?: "")
        card.addView(addressField, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })

        if (role == "nurse") {
            specialtyField = field("التخصص", profile.specialty ?: "")
            card.addView(specialtyField, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })

            experienceField = field(
                "سنوات الخبرة",
                profile.experience_years?.toString() ?: "",
            )
            experienceField?.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            card.addView(experienceField, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })
        }

        bioField = field("نبذة عنك / معلومات إضافية", profile.bio ?: "", true)
        card.addView(bioField, LinearLayout.LayoutParams(-1, dp(110)).apply { topMargin = dp(8) })

        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        if (!readOnly) {
            root.addView(Button(this).apply {
                text = "💾 حفظ الملف الشخصي"
                isAllCaps = false
                textSize = 17f
                setTextColor(white)
                background = bg(green, 17)
                setOnClickListener { saveProfile() }
            }, LinearLayout.LayoutParams(-1, dp(60)).apply { topMargin = dp(12) })
        }

        root.addView(Button(this).apply {
            text = "رجوع"
            isAllCaps = false
            setTextColor(navy)
            background = bg(white, 15, navy)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(10) })

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, 5001)
    }

    @Deprecated("Use Activity Result API when refactoring UI.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5001 && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                avatarView?.setImageURI(uri)
                uploadAvatar(uri)
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val user = SupabaseManager.client.auth.currentUserOrNull() ?: return
        if (readOnly) return
        scope.launch {
            val loading = ProgressDialog.show(this@ProfileActivity, null, "جاري رفع الصورة...", true, false)
            try {
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("تعذر قراءة الصورة")

                val path = "${role}/${user.id}/avatar.jpg"
                val bucket = SupabaseManager.client.storage.from("avatars")
                bucket.upload(path, bytes) { upsert = true }
                avatarUrl = bucket.publicUrl(path)
                saveAvatarUrlOnly(avatarUrl!!)
                loading.dismiss()
                Toast.makeText(this@ProfileActivity, "تم رفع الصورة", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                loading.dismiss()
                showError("تعذر رفع الصورة", e.message ?: "تحقق من إعداد Storage.")
            }
        }
    }

    private suspend fun saveAvatarUrlOnly(url: String) {
        SupabaseManager.client.from(if (role == "nurse") "nurses" else "patients").update(
            mapOf("avatar_url" to url)
        ) {
            filter { eq("user_id", targetUserId) }
        }
    }

    private fun saveProfile() {
        val user = SupabaseManager.client.auth.currentUserOrNull()
        if (user == null || user.id != targetUserId) {
            showError("غير مسموح", "يمكنك تعديل ملفك الشخصي فقط.")
            return
        }

        val name = nameField?.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            nameField?.error = "أدخل الاسم"
            return
        }

        val values = mutableMapOf<String, Any?>(
            "full_name" to name,
            "city" to cityField?.text?.toString()?.trim().orEmpty(),
            "address" to addressField?.text?.toString()?.trim().orEmpty(),
            "bio" to bioField?.text?.toString()?.trim().orEmpty(),
            "avatar_url" to avatarUrl
        )
        if (role == "nurse") {
            values["specialty"] = specialtyField?.text?.toString()?.trim().orEmpty()
            values["experience_years"] = experienceField?.text?.toString()?.toIntOrNull() ?: 0
        }

        scope.launch {
            val loading = ProgressDialog.show(this@ProfileActivity, null, "جاري حفظ الملف...", true, false)
            try {
                SupabaseManager.client.from(if (role == "nurse") "nurses" else "patients").update(values) {
                    filter { eq("user_id", targetUserId) }
                }
                loading.dismiss()
                Toast.makeText(this@ProfileActivity, "تم حفظ الملف الشخصي", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                loading.dismiss()
                showError("تعذر حفظ الملف", e.message ?: "حاول مرة أخرى.")
            }
        }
    }

    private fun loadAvatar(url: String?) {
        if (url.isNullOrBlank()) {
            avatarView?.setImageResource(android.R.drawable.ic_menu_myplaces)
            return
        }
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                }
                if (bitmap != null) avatarView?.setImageBitmap(bitmap)
            } catch (_: Exception) {
                avatarView?.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
        }
    }

    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("حسنًا", null)
            .show()
    }
}
