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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import kotlinx.serialization.Serializable


@Serializable
data class MyBooking(
    val id: String,
    val patient_id: String,
    val nurse_id: String? = null,
    val service_id: String,
    val address: String,
    val city: String? = null,
    val landmark: String? = null,
    val patient_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String? = null,
    val notes: String? = null,
    val created_at: String? = null
)


class MyBookingsActivity : AppCompatActivity() {

    private val NAVY = Color.rgb(5, 62, 105)
    private val BLUE = Color.rgb(31, 115, 176)
    private val LIGHT_BLUE = Color.rgb(235, 245, 251)

    private val GREEN = Color.rgb(45, 145, 80)
    private val ORANGE = Color.rgb(225, 145, 40)
    private val RED = Color.rgb(195, 60, 60)

    private val TEXT = Color.rgb(45, 45, 45)
    private val GRAY = Color.rgb(120, 120, 120)
    private val LIGHT_GRAY = Color.rgb(247, 248, 249)
    private val BORDER = Color.rgb(218, 224, 229)
    private val WHITE = Color.WHITE

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadBookings()
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
        strokeColor: Int = BORDER,
        radius: Int = 18
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


    private fun button(
        title: String,
        color: Int,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            text = title
            textSize = 16f

            isAllCaps = false

            setTextColor(WHITE)

            gravity = Gravity.CENTER

            background =
                rounded(
                    color,
                    15
                )

            setOnClickListener {
                action()
            }
        }
    }


    private fun loadBookings() {

        val user =
            SupabaseManager.client
                .auth
                .currentUserOrNull()

        if (user == null) {

            Toast.makeText(
                this,
                "يجب تسجيل الدخول أولاً",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        showLoading()


        scope.launch {

            try {

                val bookings =
                    SupabaseManager.client
                        .from("bookings")
                        .select {
                            filter {
                                eq(
                                    "patient_id",
                                    user.id
                                )
                            }
                        }
                        .decodeList<MyBooking>()


                showBookings(bookings)

            } catch (e: Exception) {

                Toast.makeText(
                    this@MyBookingsActivity,
                    "تعذر تحميل الطلبات",
                    Toast.LENGTH_LONG
                ).show()

                showBookings(
                    emptyList()
                )
            }
        }
    }


    private fun showLoading() {

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
            }


        root.addView(
            makeText(
                "جاري تحميل طلباتك...",
                20f,
                NAVY,
                true
            )
        )


        setContentView(root)
    }


    private fun showBookings(
        bookings: List<MyBooking>
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
                    dp(10),
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


        /*
         * =========================
         * الشريط العلوي
         * =========================
         */

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    rounded(
                        WHITE,
                        18
                    )

                setPadding(
                    dp(8),
                    dp(5),
                    dp(8),
                    dp(5)
                )
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
                dp(85),
                dp(52)
            )
        )


        header.addView(
            makeText(
                "طلباتي",
                24f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                dp(60),
                1f
            )
        )


        header.addView(
            makeText(
                "📋",
                28f,
                NAVY
            ),
            LinearLayout.LayoutParams(
                dp(55),
                dp(55)
            )
        )


        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            ).apply {
                bottomMargin = dp(12)
            }
        )


        /*
         * =========================
         * ملخص
         * =========================
         */

        val summary =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                background =
                    rounded(
                        LIGHT_BLUE,
                        18
                    )

                setPadding(
                    dp(12),
                    dp(8),
                    dp(12),
                    dp(8)
                )
            }


        summary.addView(
            makeText(
                "إجمالي الطلبات",
                16f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )


        summary.addView(
            makeText(
                bookings.size.toString(),
                25f,
                BLUE,
                true
            ),
            LinearLayout.LayoutParams(
                dp(65),
                dp(55)
            )
        )


        root.addView(
            summary,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            ).apply {
                bottomMargin = dp(15)
            }
        )


        /*
         * =========================
         * لا توجد طلبات
         * =========================
         */

        if (bookings.isEmpty()) {

            val empty =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    background =
                        rounded(
                            WHITE,
                            22
                        )

                    setPadding(
                        dp(20),
                        dp(35),
                        dp(20),
                        dp(35)
                    )
                }


            empty.addView(
                makeText(
                    "📋",
                    55f,
                    NAVY
                )
            )


            empty.addView(
                makeText(
                    "لا توجد طلبات حتى الآن",
                    21f,
                    NAVY,
                    true
                )
            )


            empty.addView(
                makeText(
                    "عند إرسال طلب تمريض سيظهر هنا",
                    15f,
                    GRAY
                )
            )


            root.addView(
                empty,
                LinearLayout.LayoutParams(
                    -1,
                    dp(240)
                )
            )


            return
        }


        /*
         * =========================
         * الطلبات
         * =========================
         */

        for (booking in bookings) {

            root.addView(
                createBookingCard(
                    booking
                ),
                LinearLayout.LayoutParams(
                    -1,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    bottomMargin =
                        dp(15)
                }
            )
        }
    }


    private fun createBookingCard(
        booking: MyBooking
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
                        BORDER,
                        20
                    )

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(15)
                )
            }


        /*
         * رأس البطاقة
         */

        val cardHeader =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }


        cardHeader.addView(
            makeText(
                "🩺  طلب تمريض منزلي",
                19f,
                NAVY,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )


        val statusView =
            makeText(
                statusArabic(
                    booking.status
                ),
                13f,
                WHITE,
                true
            )


        statusView.background =
            rounded(
                statusColor(
                    booking.status
                ),
                14
            )


        cardHeader.addView(
            statusView,
            LinearLayout.LayoutParams(
                dp(120),
                dp(42)
            )
        )


        card.addView(
            cardHeader,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )


        /*
         * رقم الطلب
         */

        addRow(
            card,
            "رقم الطلب",
            booking.id
        )


        /*
         * الخدمة
         */

        addRow(
            card,
            "الخدمة",
            booking.service_id
        )


        /*
         * المدينة
         */

        addRow(
            card,
            "المدينة",
            booking.city
                ?: "الأنبار"
        )


        /*
         * العنوان
         */

        addRow(
            card,
            "العنوان",
            booking.address
        )


        /*
         * أقرب نقطة
         */

        if (
            !booking.landmark
                .isNullOrBlank()
        ) {

            addRow(
                card,
                "أقرب نقطة",
                booking.landmark!!
            )
        }


        /*
         * رقم الهاتف
         */

        if (
            !booking.patient_phone
                .isNullOrBlank()
        ) {

            addRow(
                card,
                "الهاتف",
                booking.patient_phone!!
            )
        }


        /*
         * الممرض
         */

        addRow(
            card,
            "الممرض",
            if (
                booking.nurse_id
                    .isNullOrBlank()
            ) {
                "لم يتم تعيين ممرض بعد"
            } else {
                "تم تعيين ممرض"
            }
        )


        /*
         * الموقع
         */

        if (
            booking.latitude != null &&
            booking.longitude != null
        ) {

            addRow(
                card,
                "الموقع",
                "${booking.latitude}, ${booking.longitude}"
            )
        }


        /*
         * الملاحظات
         */

        if (
            !booking.notes
                .isNullOrBlank()
        ) {

            addRow(
                card,
                "ملاحظات",
                booking.notes!!
            )
        }


        /*
         * تاريخ الطلب
         */

        if (
            !booking.created_at
                .isNullOrBlank()
        ) {

            addRow(
                card,
                "تاريخ الطلب",
                formatDate(
                    booking.created_at!!
                )
            )
        }


        /*
         * أزرار البطاقة
         */

        val actions =
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


        /*
         * زر الموقع
         */

        if (
            booking.latitude != null &&
            booking.longitude != null
        ) {

            actions.addView(

                button(
                    "📍 الموقع",
                    BLUE
                ) {

                    openLocation(
                        booking.latitude!!,
                        booking.longitude!!
                    )
                },

                LinearLayout.LayoutParams(
                    0,
                    dp(55),
                    1f
                ).apply {

                    marginEnd =
                        dp(5)
                }
            )
        }


        /*
         * زر الاتصال
         */

        if (
            !booking.patient_phone
                .isNullOrBlank()
        ) {

            actions.addView(

                button(
                    "📞 اتصال",
                    GREEN
                ) {

                    callPhone(
                        booking.patient_phone!!
                    )
                },

                LinearLayout.LayoutParams(
                    0,
                    dp(55),
                    1f
                ).apply {

                    marginStart =
                        dp(5)
                }
            )
        }


        if (actions.childCount > 0) {

            card.addView(
                actions,
                LinearLayout.LayoutParams(
                    -1,
                    dp(70)
                )
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
            makeText(
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
            makeText(
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


    private fun statusArabic(
        status: String?
    ): String {

        return when (
            status?.lowercase()
        ) {

            "pending" ->
                "بانتظار الممرض"

            "accepted" ->
                "تم القبول"

            "assigned" ->
                "تم تعيين ممرض"

            "on_the_way" ->
                "الممرض في الطريق"

            "in_progress" ->
                "جاري تنفيذ الزيارة"

            "completed" ->
                "مكتمل"

            "rejected" ->
                "مرفوض"

            "cancelled" ->
                "ملغي"

            else ->
                status ?: "غير معروف"
        }
    }


    private fun statusColor(
        status: String?
    ): Int {

        return when (
            status?.lowercase()
        ) {

            "accepted",
            "assigned",
            "completed" ->
                GREEN

            "on_the_way",
            "in_progress" ->
                BLUE

            "rejected",
            "cancelled" ->
                RED

            else ->
                ORANGE
        }
    }


    private fun formatDate(
        value: String
    ): String {

        return try {

            value
                .replace("T", " ")
                .replace("Z", "")
                .take(19)

        } catch (
            e: Exception
        ) {

            value
        }
    }


    private fun openLocation(
        latitude: Double,
        longitude: Double
    ) {

        try {

            val uri =
                android.net.Uri.parse(
                    "geo:$latitude,$longitude?q=$latitude,$longitude"
                )

            val intent =
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    uri
                )

            startActivity(intent)

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "تعذر فتح الموقع",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun callPhone(
        phone: String
    ) {

        try {

            val uri =
                android.net.Uri.parse(
                    "tel:$phone"
                )

            val intent =
                android.content.Intent(
                    android.content.Intent.ACTION_DIAL,
                    uri
                )

            startActivity(intent)

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "تعذر فتح الاتصال",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
