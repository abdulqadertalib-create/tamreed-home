package iq.tamreed.home

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

/**
 * شاشة اشتراكات الممرضين.
 *
 * المرحلة الأولى: واجهة اختيار الباقة وشرح الدفع اليدوي.
 * لا يتم حفظ أو معالجة بيانات البطاقة داخل التطبيق.
 * سيتم ربط زر طلب التفعيل بجدول الاشتراكات في Supabase في المرحلة التالية.
 */
class NurseSubscriptionActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val BLUE = Color.rgb(31, 115, 176)
    private val GREEN = Color.rgb(35, 145, 85)
    private val ORANGE = Color.rgb(225, 145, 45)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)
    private val LIGHT_GREEN = Color.rgb(235, 248, 240)
    private val WHITE = Color.WHITE
    private val BORDER = Color.rgb(218, 224, 229)

    private var selectedPlan: SubscriptionPlan? = null
    private var selectedButton: Button? = null
    private var summaryTextView: TextView? = null

    data class SubscriptionPlan(
        val title: String,
        val duration: String,
        val price: String,
        val description: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showSubscriptions()
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

        // الشريط العلوي
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
            },
            LinearLayout.LayoutParams(-1, dp(58))
        )

        root.addView(
            text(
                "الاشتراك يتيح للممرض استقبال طلبات التمريض المنزلية خلال مدة الباقة.",
                14f,
                GRAY
            ).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(48))
        )

        val plans = listOf(
            SubscriptionPlan(
                "الباقة الشهرية",
                "30 يومًا",
                "10,000 د.ع",
                "اشتراك شهري مناسب للبدء"
            ),
            SubscriptionPlan(
                "باقة 3 أشهر",
                "90 يومًا",
                "25,000 د.ع",
                "توفير مقارنة بالدفع الشهري"
            ),
            SubscriptionPlan(
                "باقة 6 أشهر",
                "180 يومًا",
                "45,000 د.ع",
                "خيار مناسب للاستمرار"
            ),
            SubscriptionPlan(
                "الباقة السنوية",
                "365 يومًا",
                "80,000 د.ع",
                "أفضل خيار للاشتراك الطويل"
            )
        )

        plans.forEach { plan ->
            addPlanCard(root, plan)
        }

        // طريقة الدفع
        val paymentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bordered(WHITE, BORDER, 20)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            elevation = dp(2).toFloat()
        }

        paymentCard.addView(
            text("💳 طريقة الدفع", 18f, NAVY, true),
            LinearLayout.LayoutParams(-1, dp(42))
        )
        paymentCard.addView(
            text(
                "في هذه المرحلة يتم اعتماد التحويل اليدوي، ثم تقوم الإدارة بمراجعة العملية وتفعيل الاشتراك.",
                14f,
                TEXT
            ),
            LinearLayout.LayoutParams(-1, dp(62))
        )
        paymentCard.addView(
            text(
                "⚠️ لا تدخل رقم البطاقة أو رمزها السري داخل التطبيق.",
                13f,
                ORANGE,
                true
            ),
            LinearLayout.LayoutParams(-1, dp(38))
        )

        root.addView(
            paymentCard,
            LinearLayout.LayoutParams(-1, dp(160)).apply {
                topMargin = dp(10)
                bottomMargin = dp(12)
            }
        )

        val summary = text(
            "لم يتم اختيار باقة بعد",
            15f,
            GRAY,
            true
        ).apply {
            background = rounded(LIGHT_BLUE, 16)
            gravity = Gravity.CENTER
        }
        summaryTextView = summary
        root.addView(summary, LinearLayout.LayoutParams(-1, dp(58)).apply {
            bottomMargin = dp(10)
        })

        val requestButton = Button(this).apply {
            text = "إرسال طلب تفعيل الاشتراك"
            textSize = 17f
            isAllCaps = false
            setTextColor(WHITE)
            background = rounded(NAVY, 18)
            setOnClickListener {
                val plan = selectedPlan
                if (plan == null) {
                    Toast.makeText(
                        this@NurseSubscriptionActivity,
                        "اختر باقة الاشتراك أولاً",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                Toast.makeText(
                    this@NurseSubscriptionActivity,
                    "تم اختيار ${plan.title}. سيتم ربط طلب التفعيل بالدفع والإدارة في الخطوة التالية.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        root.addView(requestButton, LinearLayout.LayoutParams(-1, dp(62)))

        root.addView(
            text(
                "بعد ربط Supabase سيتم إنشاء طلب دفع للممرض، ثم تعتمد الإدارة العملية وتحدد تاريخ بداية ونهاية الاشتراك.",
                12f,
                GRAY
            ).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(62))
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

        val title = text(plan.title, 18f, NAVY, true).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        card.addView(title, LinearLayout.LayoutParams(-1, dp(34)))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val duration = text(plan.duration, 13f, GRAY).apply {
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        row.addView(duration, LinearLayout.LayoutParams(0, dp(34), 1f))

        val price = text(plan.price, 18f, GREEN, true).apply {
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        row.addView(price, LinearLayout.LayoutParams(dp(125), dp(40)))
        card.addView(row, LinearLayout.LayoutParams(-1, dp(42)))

        card.addView(
            text(plan.description, 12f, GRAY).apply {
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            },
            LinearLayout.LayoutParams(-1, dp(30))
        )

        val choose = Button(this).apply {
            text = "اختيار الباقة"
            textSize = 14f
            isAllCaps = false
            setTextColor(NAVY)
            background = bordered(WHITE, Color.rgb(150, 190, 215), 14)
            setOnClickListener {
                selectPlan(plan, this)
            }
        }
        card.addView(choose, LinearLayout.LayoutParams(-1, dp(44)))

        card.setOnClickListener {
            selectPlan(plan, choose)
        }

        parent.addView(
            card,
            LinearLayout.LayoutParams(-1, dp(154)).apply {
                bottomMargin = dp(10)
            }
        )
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

}
