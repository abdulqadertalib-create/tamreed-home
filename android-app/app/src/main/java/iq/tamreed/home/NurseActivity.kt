package iq.tamreed.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
data class NurseHomeProfile(
    val id: String? = null,
    val user_id: String? = null,
    val full_name: String? = null,
    val phone: String? = null,
    val specialty: String? = null,
    val experience_years: Int? = null,
    val city: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_available: Boolean? = false,
    val is_verified: Boolean? = false
)

class NurseActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val BLUE = Color.rgb(31, 115, 176)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val WHITE = Color.WHITE
    private val GREEN = Color.rgb(35, 145, 85)

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    private var nurse: NurseHomeProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadNurseProfile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
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
        strokeColor: Int = Color.rgb(218, 224, 229),
        radius: Int = 16
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun makeText(
        value: String,
        size: Float = 16f,
        color: Int = TEXT,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {

            text = value
            textSize = size
            setTextColor(color)

            gravity = Gravity.CENTER

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            if (bold) {
                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )
        }
    }

    private fun loadNurseProfile() {

        val user =
            SupabaseManager.client
                .auth
                .currentUserOrNull()

        if (user == null) {

            goToLogin()

            return
        }

        scope.launch {

            try {

                val profiles =
                    SupabaseManager.client
                        .from("nurses")
                        .select {
                            filter {
                                eq(
                                    "user_id",
                                    user.id
                                )
                            }
                        }
                        .decodeList<NurseHomeProfile>()

                if (profiles.isEmpty()) {

                    Toast.makeText(
                        this@NurseActivity,
                        "لم يتم العثور على بيانات الممرض",
                        Toast.LENGTH_LONG
                    ).show()

                    goToLogin()

                    return@launch
                }

                nurse = profiles.first()

                showHome()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseActivity,
                    "تعذر تحميل بيانات الممرض",
                    Toast.LENGTH_LONG
                ).show()

                showHomeWithoutProfile()
            }
        }
    }

    private fun showHome() {

        val profile = nurse

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setBackgroundColor(
                    LIGHT_GRAY
                )

                setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(30)
                )
            }

        val scroll =
            ScrollView(this).apply {

                isFillViewport = true

                addView(root)
            }

        setContentView(scroll)

        // العنوان

        val title =
            makeText(
                "التمريض المنزلي",
                28f,
                NAVY,
                true
            )

        root.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        // بطاقة الترحيب

        val welcome =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                background =
                    rounded(
                        LIGHT_BLUE,
                        22
                    )

                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        val name =
            profile?.full_name
                ?: "الممرض"

        welcome.addView(
            makeText(
                "مرحباً بك 👨‍⚕️",
                20f,
                NAVY,
                true
            )
        )

        welcome.addView(
            makeText(
                name,
                24f,
                NAVY,
                true
            )
        )

        root.addView(
            welcome,
            LinearLayout.LayoutParams(
                -1,
                dp(130)
            ).apply {
                bottomMargin = dp(15)
            }
        )

        // حالة الممرض

        val statusCard =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    bordered()

                setPadding(
                    dp(14),
                    dp(12),
                    dp(14),
                    dp(12)
                )
            }

        val available =
            profile?.is_available == true

        val statusText =
            if (available) {
                "متاح لاستقبال الطلبات"
            } else {
                "غير متاح حالياً"
            }

        val status =
            makeText(
                if (available) {
                    "🟢  $statusText"
                } else {
                    "🔴  $statusText"
                },
                18f,
                if (available) GREEN else GRAY,
                true
            )

        statusCard.addView(
            status,
            LinearLayout.LayoutParams(
                0,
                dp(65),
                1f
            )
        )

        root.addView(
            statusCard,
            LinearLayout.LayoutParams(
                -1,
                dp(80)
            ).apply {
                bottomMargin = dp(15)
            }
        )

        // بيانات الممرض

        val infoTitle =
            makeText(
                "بيانات الممرض",
                21f,
                NAVY,
                true
            )

        root.addView(
            infoTitle,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        val info =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                background =
                    bordered()

                setPadding(
                    dp(15),
                    dp(12),
                    dp(15),
                    dp(12)
                )
            }

        addInfoRow(
            info,
            "الاسم",
            profile?.full_name ?: "-"
        )

        addInfoRow(
            info,
            "رقم الهاتف",
            profile?.phone ?: "-"
        )

        addInfoRow(
            info,
            "التخصص",
            profile?.specialty ?: "-"
        )

        addInfoRow(
            info,
            "سنوات الخبرة",
            profile?.experience_years
                ?.toString()
                ?: "-"
        )

        addInfoRow(
            info,
            "المحافظة",
            profile?.city ?: "الأنبار"
        )

        addInfoRow(
            info,
            "العنوان",
            profile?.address ?: "-"
        )

        root.addView(
            info,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(20)
            }
        )

        // زر الطلبات

        val requestsButton =
            Button(this).apply {

                text =
                    "📋  طلبات التمريض"

                textSize = 18f

                isAllCaps = false

                setTextColor(WHITE)

                background =
                    rounded(
                        NAVY,
                        18
                    )

                setOnClickListener {

                    Toast.makeText(
                        this@NurseActivity,
                        "سيتم فتح الطلبات في المرحلة التالية",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        root.addView(
            requestsButton,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            ).apply {
                bottomMargin = dp(12)
            }
        )

        // زر تغيير الحالة

        val availabilityButton =
            Button(this).apply {

                text =
                    if (available) {
                        "🔴  إيقاف استقبال الطلبات"
                    } else {
                        "🟢  تفعيل استقبال الطلبات"
                    }

                textSize = 17f

                isAllCaps = false

                setTextColor(
                    if (available) {
                        NAVY
                    } else {
                        GREEN
                    }
                )

                background =
                    bordered(
                        WHITE,
                        NAVY,
                        18
                    )

                setOnClickListener {

                    toggleAvailability()
                }
            }

        root.addView(
            availabilityButton,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            ).apply {
                bottomMargin = dp(12)
            }
        )

        // زر تسجيل الخروج

        val logoutButton =
            Button(this).apply {

                text =
                    "تسجيل الخروج"

                textSize = 17f

                isAllCaps = false

                setTextColor(
                    Color.rgb(170, 45, 45)
                )

                background =
                    bordered(
                        WHITE,
                        Color.rgb(200, 100, 100),
                        18
                    )

                setOnClickListener {

                    logout()
                }
            }

        root.addView(
            logoutButton,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )
    }

    private fun addInfoRow(
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

        val label =
            makeText(
                "$title:",
                16f,
                GRAY,
                true
            )

        val content =
            makeText(
                value,
                16f,
                TEXT,
                false
            )

        row.addView(
            label,
            LinearLayout.LayoutParams(
                dp(125),
                dp(48)
            )
        )

        row.addView(
            content,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        parent.addView(row)
    }

    private fun toggleAvailability() {

        val profile =
            nurse ?: return

        val id =
            profile.id

        if (id.isNullOrBlank()) {

            Toast.makeText(
                this,
                "معرف الممرض غير موجود",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val newValue =
            !(profile.is_available ?: false)

        scope.launch {

            try {

                SupabaseManager.client
                    .from("nurses")
                    .update(
                        {
                            set(
                                "is_available",
                                newValue
                            )
                        }
                    ) {
                        filter {
                            eq(
                                "id",
                                id
                            )
                        }
                    }

                nurse =
                    profile.copy(
                        is_available =
                            newValue
                    )

                Toast.makeText(
                    this@NurseActivity,
                    if (newValue) {
                        "تم تفعيل استقبال الطلبات"
                    } else {
                        "تم إيقاف استقبال الطلبات"
                    },
                    Toast.LENGTH_SHORT
                ).show()

                showHome()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseActivity,
                    "تعذر تغيير حالة الممرض",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun logout() {

        scope.launch {

            try {

                SupabaseManager.client
                    .auth
                    .signOut()

            } catch (_: Exception) {
            }

            goToLogin()
        }
    }

    private fun goToLogin() {

        val intent =
            Intent(
                this,
                NurseLoginActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }

    private fun showHomeWithoutProfile() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setBackgroundColor(
                    LIGHT_GRAY
                )

                setPadding(
                    dp(30),
                    dp(30),
                    dp(30),
                    dp(30)
                )
            }

        root.addView(
            makeText(
                "التمريض المنزلي",
                28f,
                NAVY,
                true
            )
        )

        root.addView(
            makeText(
                "مرحباً بك في لوحة الممرض",
                20f,
                TEXT,
                true
            )
        )

        root.addView(
            makeText(
                "تعذر تحميل بعض بيانات الحساب.",
                16f,
                GRAY
            )
        )

        val logout =
            Button(this).apply {

                text = "تسجيل الخروج"

                isAllCaps = false

                setOnClickListener {
                    logout()
                }
            }

        root.addView(
            logout,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                topMargin = dp(25)
            }
        )

        setContentView(root)
    }
}
