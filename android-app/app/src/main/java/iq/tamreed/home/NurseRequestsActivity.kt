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

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import kotlinx.serialization.Serializable


@Serializable
data class NursingRequest(
    val id: String? = null,
    val customer_id: String? = null,
    val nurse_id: String? = null,
    val service_type: String? = null,
    val patient_name: String? = null,
    val patient_phone: String? = null,
    val patient_age: Int? = null,
    val notes: String? = null,
    val city: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)


@Serializable
data class NurseAssignment(
    val nurse_id: String
)


class NurseRequestsActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
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

    private var nurseId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadNurse()
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


    private fun loadNurse() {

        val user =
            SupabaseManager.client
                .auth
                .currentUserOrNull()

        if (user == null) {

            Toast.makeText(
                this,
                "يجب تسجيل الدخول كممرض أولاً",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        scope.launch {

            try {

                val nurses =
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

                if (nurses.isEmpty()) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "لم يتم العثور على بيانات الممرض",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@launch
                }

                nurseId =
                    nurses.first().id

                if (nurseId.isNullOrBlank()) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "معرف الممرض غير موجود",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@launch
                }

                loadRequests()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر تحميل بيانات الممرض: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }


    private fun loadRequests() {

        scope.launch {

            try {

                /*
                 * جدول nursing_requests يحتوي على 12 عموداً.
                 *
                 * id
                 * customer_id
                 * nurse_id
                 * service_type
                 * patient_name
                 * patient_phone
                 * patient_age
                 * notes
                 * city
                 * address
                 * latitude
                 * longitude
                 */

                val allRequests =
                    SupabaseManager.client
                        .from("nursing_requests")
                        .select()
                        .decodeList<NursingRequest>()


                /*
                 * نعرض:
                 *
                 * 1- الطلبات التي لم يتم تعيين ممرض لها.
                 *
                 * 2- الطلبات التي تم تعيينها لهذا الممرض.
                 */

                val requests =
                    allRequests.filter { request ->

                        request.nurse_id.isNullOrBlank() ||
                        request.nurse_id == nurseId
                    }


                showRequests(requests)

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر تحميل الطلبات: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                showRequests(emptyList())
            }
        }
    }


    private fun showRequests(
        requests: List<NursingRequest>
    ) {

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


        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }


        val backButton =
            Button(this).apply {

                text = "رجوع"

                textSize = 15f

                isAllCaps = false

                setTextColor(NAVY)

                background =
                    bordered(
                        WHITE,
                        NAVY,
                        14
                    )

                setOnClickListener {
                    finish()
                }
            }


        header.addView(
            backButton,
            LinearLayout.LayoutParams(
                dp(90),
                dp(52)
            )
        )


        val title =
            makeText(
                "طلبات التمريض",
                25f,
                NAVY,
                true
            )


        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(65),
                1f
            )
        )


        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            ).apply {
                bottomMargin = dp(10)
            }
        )


        val countText =
            makeText(
                "عدد الطلبات: ${requests.size}",
                17f,
                GRAY,
                true
            )


        root.addView(
            countText,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            )
        )


        if (requests.isEmpty()) {

            val empty =
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
                        dp(20),
                        dp(30),
                        dp(20),
                        dp(30)
                    )
                }


            empty.addView(
                makeText(
                    "📋",
                    45f,
                    NAVY,
                    false
                )
            )


            empty.addView(
                makeText(
                    "لا توجد طلبات حالياً",
                    21f,
                    NAVY,
                    true
                )
            )


            empty.addView(
                makeText(
                    "ستظهر هنا طلبات التمريض الجديدة",
                    16f,
                    GRAY,
                    false
                )
            )


            root.addView(
                empty,
                LinearLayout.LayoutParams(
                    -1,
                    dp(220)
                ).apply {
                    topMargin = dp(20)
                }
            )

            return
        }


        for (request in requests) {

            root.addView(
                createRequestCard(request),
                LinearLayout.LayoutParams(
                    -1,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(16)
                }
            )
        }
    }


    private fun createRequestCard(
        request: NursingRequest
    ): LinearLayout {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    bordered(
                        WHITE,
                        Color.rgb(215, 225, 232),
                        20
                    )

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(15)
                )
            }


        val title =
            makeText(
                "🩺 طلب تمريض منزلي",
                20f,
                NAVY,
                true
            )


        card.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )


        addRow(
            card,
            "الخدمة",
            request.service_type ?: "-"
        )


        addRow(
            card,
            "اسم المريض",
            request.patient_name ?: "-"
        )


        addRow(
            card,
            "العمر",
            request.patient_age?.toString() ?: "-"
        )


        addRow(
            card,
            "رقم الهاتف",
            request.patient_phone ?: "-"
        )


        addRow(
            card,
            "المحافظة",
            request.city ?: "الأنبار"
        )


        addRow(
            card,
            "العنوان",
            request.address ?: "-"
        )


        addRow(
            card,
            "الملاحظات",
            request.notes ?: "لا توجد ملاحظات"
        )


        if (
            request.latitude != null &&
            request.longitude != null
        ) {

            addRow(
                card,
                "الموقع",
                "${request.latitude}, ${request.longitude}"
            )
        }


        val buttons =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setPadding(
                    0,
                    dp(12),
                    0,
                    0
                )
            }


        val acceptButton =
            Button(this).apply {

                text = "✓ قبول الطلب"

                textSize = 16f

                isAllCaps = false

                setTextColor(WHITE)

                background =
                    rounded(
                        GREEN,
                        16
                    )

                setOnClickListener {
                    acceptRequest(request)
                }
            }


        buttons.addView(
            acceptButton,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            )
        )


        card.addView(
            buttons,
            LinearLayout.LayoutParams(
                -1,
                dp(75)
            )
        )


        return card
    }


    private fun addRow(
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
                15f,
                GRAY,
                true
            )


        val content =
            makeText(
                value,
                15f,
                TEXT,
                false
            )


        row.addView(
            label,
            LinearLayout.LayoutParams(
                dp(105),
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


    private fun acceptRequest(
        request: NursingRequest
    ) {

        val requestId =
            request.id

        val id =
            nurseId


        if (
            requestId.isNullOrBlank() ||
            id.isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                "بيانات الطلب غير مكتملة",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        scope.launch {

            try {

                /*
                 * لا يوجد status في جدولك.
                 *
                 * قبول الطلب = تسجيل nurse_id
                 */

                SupabaseManager.client
                    .from("nursing_requests")
                    .update(
                        NurseAssignment(
                            nurse_id = id
                        )
                    ) {

                        filter {

                            eq(
                                "id",
                                requestId
                            )
                        }
                    }


                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تم قبول الطلب بنجاح ✓",
                    Toast.LENGTH_SHORT
                ).show()


                loadRequests()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر قبول الطلب: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
