package iq.tamreed.home

import android.app.ProgressDialog
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

/** شاشة اشتراكات الممرضين. */
class NurseSubscriptionActivity : AppCompatActivity() {
    private val NAVY = Color.rgb(5, 62, 105)
    private val GREEN = Color.rgb(35, 145, 85)
    private val ORANGE = Color.rgb(225, 145, 45)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val LIGHT_GREEN = Color.rgb(235, 248, 240)
    private val WHITE = Color.WHITE
    private val BORDER = Color.rgb(218, 224, 229)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var selectedPlan: SubscriptionPlan? = null
    private var selectedButton: Button? = null
    private var summaryTextView: TextView? = null
    private var requestButton: Button? = null

    @Serializable
    data class SubscriptionRequestInsert(
        val nurse_id: String,
        val plan_name: String,
        val duration_days: Int,
        val amount_iqd: Int,
        val status: String = "PENDING"
    )

    data class SubscriptionPlan(
        val title: String,
        val duration: String,
        val durationDays: Int,
        val amountIqd: Int,
        val price: String,
        val description: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showSubscriptions()
    }

    override fun onDestroy() {
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
        radius: Int = 18
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
        }

    private fun text(
        value: String,
        size: Float = 16f,
        color: Int = TEXT,
        bold: Boolean = false
    ): TextView = TextView(this).apply {
        this.text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        includeFontPadding = true
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(dp(8), dp(5), dp(8), dp(5))
    }

    private fun showSubscriptions() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(248, 250, 252))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(12), dp(16), dp(28))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(NAVY, 20)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val headerTitle = text("اشتراكات الممرضين", 20f, WHITE, true).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        header.addView(headerTitle, LinearLayout.LayoutParams(0, dp(56), 1f))
        val back = Button(this).apply {
            text = "رجوع"
            textSize = 14f
            isAllCaps = false
            setTextColor(NAVY)
            background = rounded(WHITE, 14)
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(82), dp(44)))
        root.addView(header, LinearLayout.LayoutParams(-1, dp(72)))

        root.addView(
            text("اختر باقة الاشتراك المناسبة", 23f, NAVY, true).apply {
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(18), dp(6), dp(2))
            }, LinearLayout.LayoutParams(-1, dp(58))
        )
        root.addView(
            text("الاشتراك يتيح للممرض استقبال طلبات التمريض المنزلية خلال مدة الباقة.", 14f, GRAY),
            LinearLayout.LayoutParams(-1, dp(48))
        )

        // أسعار مخفضة
        val plans = listOf(
            SubscriptionPlan("الباقة الشهرية", "30 يومًا", 30, 5000, "5,000 د.ع", "اشتراك شهري اقتصادي للبدء"),
            SubscriptionPlan("باقة 3 أشهر", "90 يومًا", 90, 12000, "12,000 د.ع", "توفير أفضل مع مدة أطول"),
            SubscriptionPlan("باقة 6 أشهر", "180 يومًا", 180, 20000, "20,000 د.ع", "خيار اقتصادي للاستمرار"),
            SubscriptionPlan("الباقة السنوية", "365 يومًا", 365, 35000, "35,000 د.ع", "أفضل قيمة للاشتراك الطويل")
        )
        plans.forEach { addPlanCard(root, it) }

        val paymentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bordered(WHITE, BORDER, 20)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            elevation = dp(2).toFloat()
        }
        paymentCard.addView(text("💳 طريقة الدفع", 18f, NAVY, true), LinearLayout.LayoutParams(-1, dp(42)))
        paymentCard.addView(text("في هذه المرحلة يتم اعتماد التحويل اليدوي، ثم تقوم الإدارة بمراجعة العملية وتفعيل الاشتراك.", 14f, TEXT), LinearLayout.LayoutParams(-1, dp(62)))
        paymentCard.addView(text("⚠️ لا تدخل رقم البطاقة أو رمزها السري داخل التطبيق.", 13f, ORANGE, true), LinearLayout.LayoutParams(-1, dp(38)))
        root.addView(paymentCard, LinearLayout.LayoutParams(-1, dp(160)).apply { topMargin = dp(10); bottomMargin = dp(12) })

        val summary = text("لم يتم اختيار باقة بعد", 15f, GRAY, true).apply {
            background = rounded(LIGHT_BLUE, 16)
            gravity = Gravity.CENTER
        }
        summaryTextView = summary
        root.addView(summary, LinearLayout.LayoutParams(-1, dp(58)).apply { bottomMargin = dp(10) })

        requestButton = Button(this).apply {
            text = "إرسال طلب تفعيل الاشتراك"
            textSize = 17f
            isAllCaps = false
            setTextColor(WHITE)
            background = rounded(NAVY, 18)
            setOnClickListener { submitSubscriptionRequest() }
        }
        root.addView(requestButton, LinearLayout.LayoutParams(-1, dp(62)))

        root.addView(
            text("بعد الإرسال يظهر الطلب لدى الإدارة، وبعد اعتماد التحويل يتم تفعيل الاشتراك للممرض تلقائيًا.", 12f, GRAY).apply {
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, dp(62))
        )

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun addPlanCard(parent: LinearLayout, plan: SubscriptionPlan) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bordered(WHITE, BORDER, 20)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            isClickable = true
            isFocusable = true
        }
        card.addView(text(plan.title, 18f, NAVY, true).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(-1, dp(34)))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        row.addView(text(plan.duration, 13f, GRAY).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, dp(34), 1f))
        row.addView(text(plan.price, 18f, GREEN, true).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(125), dp(40)))
        card.addView(row, LinearLayout.LayoutParams(-1, dp(42)))
        card.addView(text(plan.description, 12f, GRAY).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(-1, dp(30)))

        val choose = Button(this).apply {
            text = "اختيار الباقة"
            textSize = 14f
            isAllCaps = false
            setTextColor(NAVY)
            background = bordered(WHITE, Color.rgb(150, 190, 215), 14)
            setOnClickListener { selectPlan(plan, this) }
        }
        card.addView(choose, LinearLayout.LayoutParams(-1, dp(44)))
        card.setOnClickListener { selectPlan(plan, choose) }
        parent.addView(card, LinearLayout.LayoutParams(-1, dp(154)).apply { bottomMargin = dp(10) })
    }

    private fun selectPlan(plan: SubscriptionPlan, button: Button) {
        selectedPlan = plan
        selectedButton?.apply {
            setTextColor(NAVY)
            background = bordered(WHITE, Color.rgb(150, 190, 215), 14)
        }
        selectedButton = button
        button.setTextColor(WHITE)
        button.background = rounded(GREEN, 14)
        summaryTextView?.apply {
            text = "الباقة المختارة: ${plan.title}  •  ${plan.price}  •  ${plan.duration}"
            setTextColor(NAVY)
            background = rounded(LIGHT_GREEN, 16)
        }
    }

    private fun submitSubscriptionRequest() {
        val plan = selectedPlan
        if (plan == null) {
            Toast.makeText(this, "اختر باقة الاشتراك أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        val user = SupabaseManager.client.auth.currentUserOrNull()
        if (user == null) {
            Toast.makeText(this, "انتهت جلسة الدخول. سجل الدخول كممرض ثم حاول مرة أخرى.", Toast.LENGTH_LONG).show()
            return
        }

        scope.launch {
            val loading = ProgressDialog.show(this@NurseSubscriptionActivity, null, "جاري إرسال طلب الاشتراك...", true, false)
            requestButton?.isEnabled = false
            try {
                SupabaseManager.client.from("nurse_subscription_requests").insert(
                    SubscriptionRequestInsert(
                        nurse_id = user.id,
                        plan_name = plan.title,
                        duration_days = plan.durationDays,
                        amount_iqd = plan.amountIqd
                    )
                )
                loading.dismiss()
                Toast.makeText(this@NurseSubscriptionActivity, "تم إرسال طلب الاشتراك إلى الإدارة ✓", Toast.LENGTH_LONG).show()
                requestButton?.text = "تم إرسال الطلب ✓"
                requestButton?.background = rounded(GREEN, 18)
            } catch (e: Exception) {
                loading.dismiss()
                requestButton?.isEnabled = true
                Toast.makeText(this@NurseSubscriptionActivity, "تعذر إرسال الطلب: ${e.message ?: "تحقق من Supabase"}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
