package iq.tamreed.home

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class NurseRequestsBooking(
    val id: String? = null,
    val patient_id: String? = null,
    val nurse_id: String? = null,
    val service_id: String? = null,
    val address: String? = null,
    val city: String? = null,
    val landmark: String? = null,
    val patient_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String? = null,
    val notes: String? = null,
    val created_at: String? = null
)

@Serializable
data class NurseBookingAssignment(
    val nurse_id: String,
    val status: String
)

class NurseRequestsActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val BLUE = Color.rgb(235, 245, 251)
    private val GREEN = Color.rgb(35, 145, 85)
    private val ORANGE = Color.rgb(220, 145, 35)
    private val RED = Color.rgb(190, 55, 55)
    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val BG = Color.rgb(247, 248, 249)
    private val WHITE = Color.WHITE

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    private var nurseId: String? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadNurse()
    }

    override fun onResume() {
        super.onResume()

        if (!nurseId.isNullOrBlank()) {
            loadRequests()
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
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun bordered(
        color: Int = WHITE,
        strokeColor: Int = Color.rgb(215, 225, 232),
        radius: Int = 16
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun txt(
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
            SupabaseManager
                .client
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

        currentUserId = user.id

        scope.launch {

            try {

                /*
                 * مهم جداً:
                 *
                 * نبحث عن سجل الممرض بواسطة user_id
                 * ثم نأخذ nurses.id.
                 *
                 * لأن bookings.nurse_id يجب أن يحتوي
                 * على معرف سجل الممرض في جدول nurses.
                 */

                val nurses =
                    SupabaseManager
                        .client
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

                val nurse =
                    nurses.firstOrNull()

                if (nurse == null) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "لم يتم العثور على سجل الممرض في قاعدة البيانات",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@launch
                }

                val databaseNurseId =
                    nurse.id

                if (databaseNurseId.isNullOrBlank()) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "معرف الممرض في جدول nurses فارغ",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@launch
                }

                nurseId =
                    databaseNurseId

                loadRequests()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "خطأ في بيانات الممرض:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadRequests() {

        val id =
            nurseId

        if (id.isNullOrBlank()) {
            return
        }

        scope.launch {

            try {

                val bookings =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select()
                        .decodeList<NurseRequestsBooking>()

                /*
                 * نعرض:
                 *
                 * 1- الطلبات الجديدة التي nurse_id فيها فارغ
                 * 2- الطلبات المقبولة لهذا الممرض فقط
                 *
                 * ولا نعرض طلبات ممرضين آخرين.
                 */

                val visibleBookings =
                    bookings
                        .filter { booking ->

                            booking.nurse_id.isNullOrBlank() ||
                            booking.nurse_id == id
                        }
                        .sortedByDescending {

                            it.created_at ?: ""
                        }

                showRequests(
                    visibleBookings
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر تحميل طلبات المرضى:\n${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                showRequests(
                    emptyList()
                )
            }
        }
    }

    private fun showRequests(
        requests: List<NurseRequestsBooking>
    ) {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setBackgroundColor(BG)

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

        val back =
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
            back,
            LinearLayout.LayoutParams(
                dp(90),
                dp(52)
            )
        )

        header.addView(
            txt(
                "طلبات المرضى",
                25f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                dp(60),
                1f
            )
        )

        val refresh =
            Button(this).apply {

                text = "↻"

                textSize = 22f

                isAllCaps = false

                setTextColor(NAVY)

                background =
                    bordered(
                        WHITE,
                        NAVY,
                        14
                    )

                setOnClickListener {
                    loadRequests()
                }
            }

        header.addView(
            refresh,
            LinearLayout.LayoutParams(
                dp(55),
                dp(52)
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        root.addView(
            txt(
                "عدد الطلبات: ${requests.size}",
                18f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                -1,
                dp(55)
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
                            BLUE,
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
                txt(
                    "📋",
                    50f,
                    NAVY
                )
            )

            empty.addView(
                txt(
                    "لا توجد طلبات حالياً",
                    22f,
                    NAVY,
                    true
                )
            )

            empty.addView(
                txt(
                    "ستظهر هنا طلبات المرضى الجديدة",
                    15f,
                    GRAY
                )
            )

            root.addView(
                empty,
                LinearLayout.LayoutParams(
                    -1,
                    dp(230)
                )
            )

            return
        }

        requests.forEach { booking ->

            root.addView(
                requestCard(booking),
                LinearLayout.LayoutParams(
                    -1,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    bottomMargin =
                        dp(16)
                }
            )
        }
    }

    private fun requestCard(
        booking: NurseRequestsBooking
    ): LinearLayout {

        val accepted =
            booking.nurse_id == nurseId

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

                elevation =
                    dp(2).toFloat()

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(15)
                )
            }

        card.addView(
            txt(
                "🩺  طلب تمريض منزلي",
                21f,
                NAVY,
                true
            )
        )

        card.addView(
            txt(
                if (accepted)
                    "✓ تم قبول الطلب"
                else
                    "🟠 طلب جديد بانتظار الممرض",

                15f,

                if (accepted)
                    GREEN
                else
                    ORANGE,

                true
            )
        )

        addRow(
            card,
            "رقم الطلب",
            booking.id ?: "-"
        )

        addRow(
            card,
            "رقم المريض",
            booking.patient_phone ?: "-"
        )

        addRow(
            card,
            "الخدمة",
            booking.service_id ?: "-"
        )

        addRow(
            card,
            "المدينة",
            booking.city ?: "الأنبار"
        )

        addRow(
            card,
            "العنوان",
            booking.address ?: "-"
        )

        addRow(
            card,
            "النقطة الدالة",
            booking.landmark ?: "-"
        )

        if (!booking.notes.isNullOrBlank()) {

            addRow(
                card,
                "الملاحظات",
                booking.notes ?: "-"
            )
        }

        if (
            booking.latitude != null &&
            booking.longitude != null
        ) {

            addRow(
                card,
                "موقع المريض",
                "${booking.latitude}, ${booking.longitude}"
            )
        }

        addRow(
            card,
            "الحالة",
            booking.status ?: "PENDING"
        )

        if (
            !accepted &&
            booking.nurse_id.isNullOrBlank()
        ) {

            val accept =
                Button(this).apply {

                    text =
                        "✓ قبول طلب المريض"

                    textSize =
                        17f

                    isAllCaps =
                        false

                    setTextColor(
                        WHITE
                    )

                    background =
                        rounded(
                            GREEN,
                            16
                        )

                    setOnClickListener {

                        acceptBooking(
                            booking
                        )
                    }
                }

            card.addView(
                accept,
                LinearLayout.LayoutParams(
                    -1,
                    dp(60)
                ).apply {

                    topMargin =
                        dp(12)
                }
            )
        }

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

        row.addView(
            txt(
                "$title:",
                14f,
                GRAY,
                true
            ),
            LinearLayout.LayoutParams(
                dp(105),
                dp(45)
            )
        )

        row.addView(
            txt(
                value,
                14f,
                TEXT
            ),
            LinearLayout.LayoutParams(
                0,
                dp(45),
                1f
            )
        )

        parent.addView(row)
    }

    private fun acceptBooking(
        booking: NurseRequestsBooking
    ) {

        val bookingId =
            booking.id

        val id =
            nurseId

        if (
            bookingId.isNullOrBlank() ||
            id.isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                "بيانات الطلب أو الممرض غير مكتملة",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        /*
         * منع الضغط المزدوج على نفس الطلب.
         */

        scope.launch {

            try {

                /*
                 * أولاً نتأكد أن الطلب ما زال PENDING
                 * ولم يقم ممرض آخر بقبوله.
                 */

                val current =
                    SupabaseManager
                        .client
                        .from("bookings")
                        .select {

                            filter {

                                eq(
                                    "id",
                                    bookingId
                                )
                            }
                        }
                        .decodeList<NurseRequestsBooking>()
                        .firstOrNull()

                if (current == null) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "الطلب غير موجود",
                        Toast.LENGTH_LONG
                    ).show()

                    loadRequests()
                    return@launch
                }

                if (
                    !current.nurse_id.isNullOrBlank() &&
                    current.nurse_id != id
                ) {

                    Toast.makeText(
                        this@NurseRequestsActivity,
                        "تم قبول هذا الطلب من ممرض آخر",
                        Toast.LENGTH_LONG
                    ).show()

                    loadRequests()
                    return@launch
                }

                if (
                    current.status != null &&
                    current.status.uppercase() != "PENDING"
                ) {

                    if (current.nurse_id == id) {

                        Toast.makeText(
                            this@NurseRequestsActivity,
                            "هذا الطلب مقبول مسبقاً",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this@NurseRequestsActivity,
                            "الطلب لم يعد متاحاً",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    loadRequests()
                    return@launch
                }

                /*
                 * هنا يتم الحفظ:
                 *
                 * nurse_id = nurses.id
                 * status   = ACCEPTED
                 */

                SupabaseManager
                    .client
                    .from("bookings")
                    .update(
                        NurseBookingAssignment(
                            nurse_id = id,
                            status = "ACCEPTED"
                        )
                    ) {

                        filter {

                            eq(
                                "id",
                                bookingId
                            )

                            eq(
                                "status",
                                "PENDING"
                            )
                        }
                    }

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تم قبول طلب المريض بنجاح ✓",
                    Toast.LENGTH_SHORT
                ).show()

                loadRequests()

            } catch (e: Exception) {

                val error =
                    e.message ?: "خطأ غير معروف"

                Toast.makeText(
                    this@NurseRequestsActivity,
                    "تعذر قبول الطلب:\n$error",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
