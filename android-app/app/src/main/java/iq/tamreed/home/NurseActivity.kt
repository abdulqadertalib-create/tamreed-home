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

import io.github.jan.supabase.auth.auth
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
    val full_name: String? = null,
    val phone: String? = null,
    val specialty: String? = null,
    val experience_years: Int? = null,
    val city: String? = null,
    val address: String? = null,
    val bio: String? = null,
    val avatar_url: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_available: Boolean? = false,
    val is_verified: Boolean? = false,
    val subscription_start: String? = null,
    val subscription_end: String? = null,
    val subscription_status: String? = "INACTIVE"
)


class NurseActivity : AppCompatActivity() {

    private val sessionPrefs by lazy {
        getSharedPreferences("tamreed_session", MODE_PRIVATE)
    }

    private val NAVY = Color.rgb(5, 62, 105)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val WHITE = Color.WHITE
    private val GREEN = Color.rgb(35, 145, 85)
    private val BLUE_COLOR = Color.rgb(31, 115, 176)

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    private var nurse: NurseHomeProfile? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        loadNurseProfile()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                3103
            )
        }
    }


    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }


    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }


    private fun rounded(
        color: Int,
        radius: Int = 18
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                dp(radius).toFloat()
        }
    }


    private fun bordered(
        color: Int = WHITE,
        strokeColor: Int =
            Color.rgb(218, 224, 229),
        radius: Int = 16
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            setStroke(
                dp(1),
                strokeColor
            )

            cornerRadius =
                dp(radius).toFloat()
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

        // تسجيل جهاز الممرض في FCM عند فتح لوحة الممرض.
        FcmTokenManager.registerToken("nurse")

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


                nurse =
                    profiles.first()

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


    private fun hasActiveSubscription(profile: NurseHomeProfile): Boolean {
        val status = profile.subscription_status?.uppercase()
        val end = profile.subscription_end

        if (status != "ACTIVE" || end.isNullOrBlank()) {
            return false
        }

        return try {
            java.time.Instant.parse(end).isAfter(java.time.Instant.now())
        } catch (_: Exception) {
            false
        }
    }

    private fun subscriptionStatusText(profile: NurseHomeProfile): String {
        val end = profile.subscription_end

        if (hasActiveSubscription(profile)) {
            return "🟢 الاشتراك فعال"
        }

        if (!end.isNullOrBlank()) {
            return "🔴 الاشتراك منتهي"
        }

        return "🟠 لا يوجد اشتراك فعال"
    }

    private fun formatSubscriptionEnd(value: String?): String {
        if (value.isNullOrBlank()) return "غير محدد"

        return try {
            val dateTime = java.time.OffsetDateTime.parse(value)
            val d = dateTime.toLocalDate()
            "${d.dayOfMonth}/${d.monthValue}/${d.year}"
        } catch (_: Exception) {
            value
        }
    }

    private fun openRequestsIfSubscribed() {
        val profile = nurse

        if (profile == null) {
            Toast.makeText(
                this,
                "تعذر تحميل بيانات الاشتراك",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!hasActiveSubscription(profile)) {
            Toast.makeText(
                this,
                "لا يمكنك استقبال طلبات المرضى لأن الاشتراك غير فعال",
                Toast.LENGTH_LONG
            ).show()

            startActivity(
                Intent(
                    this,
                    NurseSubscriptionActivity::class.java
                )
            )
            return
        }

        startActivity(
            Intent(
                this,
                NurseRequestsActivity::class.java
            )
        )
    }

    private fun updateAvailabilityIfAllowed() {
        val profile = nurse ?: return

        if (!hasActiveSubscription(profile)) {
            Toast.makeText(
                this,
                "لا يمكن تفعيل استقبال الطلبات قبل تفعيل الاشتراك",
                Toast.LENGTH_LONG
            ).show()

            startActivity(
                Intent(
                    this,
                    NurseSubscriptionActivity::class.java
                )
            )
            return
        }

        toggleAvailability()
    }

    private fun showHome() {
        val profile=nurse
        window.statusBarColor=WHITE;window.navigationBarColor=LIGHT_GRAY;window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(LIGHT_GRAY);setPadding(dp(16),dp(10),dp(16),dp(26))}
        val scroll=ScrollView(this).apply{isFillViewport=true;addView(root)}
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;background=WHITE;setPadding(dp(10),dp(8),dp(10),dp(8))}
        header.addView(makeText("مرحباً بك 👨‍⚕️",17f,NAVY,true),LinearLayout.LayoutParams(0,dp(48),1f))
        header.addView(makeText(profile?.full_name?:"الممرض",19f,NAVY,true),LinearLayout.LayoutParams(dp(150),dp(48)))
        root.addView(header,LinearLayout.LayoutParams(-1,dp(64)))
        val status=profile!=null&&hasActiveSubscription(profile)&&profile.is_available==true
        val statusCard=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;background=if(status) rounded(Color.rgb(232,248,239),20) else rounded(WHITE,20);setPadding(dp(14),dp(10),dp(14),dp(10))}
        statusCard.addView(makeText(if(status)"🟢 متاح لاستقبال الطلبات" else "⚪ غير متاح حالياً",16f,if(status)GREEN else GRAY,true),LinearLayout.LayoutParams(0,dp(58),1f))
        statusCard.addView(makeText("حالة الحساب",12f,GRAY,true),LinearLayout.LayoutParams(dp(90),dp(40)))
        root.addView(statusCard,LinearLayout.LayoutParams(-1,dp(78)).apply{topMargin=dp(10)})
        val action=Button(this).apply{text="🔔  عرض الطلبات الجديدة";textSize=17f;isAllCaps=false;setTextColor(WHITE);gravity=Gravity.CENTER;background=rounded(GREEN,18);setOnClickListener{openRequestsIfSubscribed()}}
        root.addView(action,LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(12)})
        val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;background=rounded(WHITE,22);setPadding(dp(14),dp(12),dp(14),dp(12))}
        info.addView(makeText("بياناتي المهنية",20f,NAVY,true),LinearLayout.LayoutParams(-1,dp(38)))
        addInfoRow(info,"الاسم",profile?.full_name?:"-");addInfoRow(info,"التخصص",profile?.specialty?:"-");addInfoRow(info,"الخبرة","${profile?.experience_years?:0} سنوات");addInfoRow(info,"المدينة",profile?.city?:"الأنبار")
        root.addView(info,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)})
        val sub=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;background=rounded(WHITE,22);setPadding(dp(14),dp(12),dp(14),dp(12))}
        sub.addView(makeText("💳 الاشتراك",19f,NAVY,true));sub.addView(makeText(subscriptionStatusText(profile?:NurseHomeProfile()),15f,if(profile!=null&&hasActiveSubscription(profile))GREEN else GRAY,true));sub.addView(makeText("ينتهي: ${formatSubscriptionEnd(profile?.subscription_end)}",13f,GRAY))
        root.addView(sub,LinearLayout.LayoutParams(-1,dp(100)).apply{topMargin=dp(12)})
        fun addBtn(title:String,color:Int,action:()->Unit){root.addView(Button(this).apply{text=title;textSize=16f;isAllCaps=false;setTextColor(WHITE);gravity=Gravity.CENTER;background=rounded(color,17);setOnClickListener{action()}},LinearLayout.LayoutParams(-1,dp(54)).apply{topMargin=dp(9)})}
        addBtn(if(status)"🔴  إيقاف استقبال الطلبات" else "🟢  تفعيل استقبال الطلبات",if(status)NAVY:GREEN){toggleAvailability()}
        addBtn("👤  ملفي الشخصي",BLUE_COLOR){val id=SupabaseManager.client.auth.currentUserOrNull()?.id;if(!id.isNullOrBlank())startActivity(Intent(this,ProfileActivity::class.java).apply{putExtra(ProfileActivity.EXTRA_ROLE,"nurse");putExtra(ProfileActivity.EXTRA_USER_ID,id);putExtra(ProfileActivity.EXTRA_READ_ONLY,false)})}
        addBtn("💳  الاشتراكات والباقات",BLUE_COLOR){startActivity(Intent(this,NurseSubscriptionActivity::class.java))}
        root.addView(outlineButtonLocal("تسجيل الخروج"){logout()},LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(9)})
        setContentView(scroll)
    }

    private fun outlineButtonLocal(title:String,action:()->Unit)=Button(this).apply{text=title;textSize=16f;isAllCaps=false;setTextColor(NAVY);gravity=Gravity.CENTER;background=bordered(WHITE,NAVY,17);setOnClickListener{action()}}

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

        if (!hasActiveSubscription(profile)) {
            Toast.makeText(
                this,
                "الاشتراك غير فعال، لا يمكن استقبال الطلبات",
                Toast.LENGTH_LONG
            ).show()
            return
        }


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
                    .update({

                        set(
                            "is_available",
                            newValue
                        )

                    }) {

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


            sessionPrefs.edit().remove("role").apply()
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
            ),
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )


        root.addView(
            makeText(
                "مرحباً بك في لوحة الممرض",
                20f,
                TEXT,
                true
            ),
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )


        root.addView(
            makeText(
                "تعذر تحميل بيانات الممرض حالياً",
                17f,
                GRAY,
                false
            ),
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )


        val loginButton =
            Button(this).apply {

                text =
                    "العودة إلى تسجيل دخول الممرض"

                textSize = 17f

                isAllCaps = false

                setTextColor(WHITE)

                background =
                    rounded(
                        NAVY,
                        18
                    )


                setOnClickListener {

                    goToLogin()
                }
            }


        root.addView(
            loginButton,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )


        setContentView(root)
    }
}
