package iq.tamreed.home

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val BLUE = Color.rgb(0, 102, 204)
    private val DARK_BLUE = Color.rgb(0, 74, 150)
    private val LIGHT_BLUE = Color.rgb(235, 245, 255)
    private val TEXT = Color.rgb(35, 45, 55)

    private val LOCATION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showHome()
    }

    private fun baseLayout(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setBackgroundColor(Color.WHITE)

            layoutDirection = View.LAYOUT_DIRECTION_RTL

            setPadding(24, 35, 24, 20)
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = TEXT
    ): TextView {

        return TextView(this).apply {

            text = value

            textSize = size

            setTextColor(color)

            gravity = Gravity.CENTER

            setPadding(10, 12, 10, 12)

            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
    }

    private fun button(
        value: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            text = value

            textSize = 17f

            setTextColor(Color.WHITE)

            setBackgroundColor(BLUE)

            isAllCaps = false

            setPadding(10, 10, 10, 10)

            layoutDirection = View.LAYOUT_DIRECTION_RTL

            setOnClickListener {
                action()
            }
        }
    }

    private fun showHome() {

        val root = baseLayout()

        val header = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER

            setPadding(10, 10, 10, 25)

            setBackgroundColor(LIGHT_BLUE)
        }

        header.addView(
            text(
                "التمريض المنزلي",
                32f,
                DARK_BLUE
            )
        )

        header.addView(
            text(
                "خدمات التمريض المنزلي",
                20f,
                TEXT
            )
        )

        header.addView(
            text(
                "لمحافظة الأنبار - العراق",
                17f,
                Color.DKGRAY
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            text(
                "مرحباً بك 👋\nكيف يمكننا مساعدتك اليوم؟",
                20f,
                DARK_BLUE
            )
        )

        val request = button("🩺  طلب ممرض منزلي") {

            showRequestScreen()
        }

        root.addView(
            request,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(0, 15, 0, 10)
            }
        )

        val services = button("🏥  الخدمات التمريضية") {

            showServices()
        }

        root.addView(
            services,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(0, 10, 0, 10)
            }
        )

        val location = button("📍  تحديد موقعي") {

            requestLocation()
        }

        root.addView(
            location,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(0, 10, 0, 10)
            }
        )

        val bookings = button("📋  طلباتي السابقة") {

            showBookings()
        }

        root.addView(
            bookings,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(0, 10, 0, 10)
            }
        )

        val contact = button("☎️  تواصل معنا") {

            contactUs()
        }

        root.addView(
            contact,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(0, 10, 0, 10)
            }
        )

        root.addView(
            text(
                "خدمة التمريض المنزلي على مدار الساعة",
                15f,
                Color.GRAY
            )
        )

        setContentView(root)
    }

    private fun showServices() {

        val root = baseLayout()

        root.addView(
            text(
                "الخدمات التمريضية",
                28f,
                DARK_BLUE
            )
        )

        val services = arrayOf(
            "💉 إعطاء الإبر والحقن",
            "🩹 تغيير الضمادات",
            "🩺 قياس ضغط الدم والسكر",
            "💊 إعطاء الأدوية حسب وصف الطبيب",
            "👴 رعاية كبار السن",
            "🏥 رعاية المرضى بعد العمليات",
            "🛏️ رعاية المرضى طريحي الفراش"
        )

        for (service in services) {

            val item = TextView(this).apply {

                text = service

                textSize = 18f

                setTextColor(TEXT)

                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

                setPadding(20, 20, 20, 20)

                setBackgroundColor(LIGHT_BLUE)
            }

            root.addView(
                item,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    65
                ).apply {

                    setMargins(0, 7, 0, 7)
                }
            )
        }

        val back = button("رجوع للرئيسية") {

            showHome()
        }

        root.addView(
            back,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            ).apply {

                setMargins(0, 20, 0, 10)
            }
        )

        setContentView(root)
    }

    private fun showRequestScreen() {

        val root = baseLayout()

        root.addView(
            text(
                "طلب ممرض منزلي",
                28f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "أدخل تفاصيل الطلب",
                17f,
                Color.GRAY
            )
        )

        val name = EditText(this).apply {

            hint = "اسم المريض"

            textSize = 17f

            setPadding(20, 10, 20, 10)

            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        root.addView(
            name,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            ).apply {

                setMargins(0, 15, 0, 10)
            }
        )

        val phone = EditText(this).apply {

            hint = "رقم الهاتف"

            inputType = android.text.InputType.TYPE_CLASS_PHONE

            textSize = 17f

            setPadding(20, 10, 20, 10)
        }

        root.addView(
            phone,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            ).apply {

                setMargins(0, 10, 0, 10)
            }
        )

        val address = EditText(this).apply {

            hint = "العنوان / المنطقة"

            textSize = 17f

            setPadding(20, 10, 20, 10)

            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        root.addView(
            address,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            ).apply {

                setMargins(0, 10, 0, 10)
            }
        )

        val notes = EditText(this).apply {

            hint = "ملاحظات إضافية"

            textSize = 17f

            gravity = Gravity.TOP or Gravity.RIGHT

            setPadding(20, 15, 20, 15)

            minLines = 4
        }

        root.addView(
            notes,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                120
            ).apply {

                setMargins(0, 10, 0, 15)
            }
        )

        val location = button("📍 تحديد الموقع") {

            requestLocation()
        }

        root.addView(
            location,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            ).apply {

                setMargins(0, 10, 0, 10)
            }
        )

        val submit = button("تأكيد طلب الممرض") {

            if (name.text.toString().trim().isEmpty()) {

                name.error = "أدخل اسم المريض"

                return@button
            }

            if (phone.text.toString().trim().isEmpty()) {

                phone.error = "أدخل رقم الهاتف"

                return@button
            }

            AlertDialog.Builder(this)
                .setTitle("تم استلام الطلب")
                .setMessage(
                    "تم تسجيل طلبك بنجاح.\n\n" +
                    "سيتم التواصل معك لتأكيد الطلب وتحديد الممرض المناسب."
                )
                .setPositiveButton("حسناً") { _, _ ->

                    showHome()
                }
                .show()
        }

        root.addView(
            submit,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            ).apply {

                setMargins(0, 15, 0, 10)
            }
        )

        val back = button("رجوع") {

            showHome()
        }

        root.addView(
            back,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            )
        )

        setContentView(root)
    }

    private fun showBookings() {

        val root = baseLayout()

        root.addView(
            text(
                "طلباتي",
                28f,
                DARK_BLUE
            )
        )

        root.addView(
            text(
                "لا توجد طلبات مسجلة حالياً",
                18f,
                Color.GRAY
            )
        )

        root.addView(
            button("طلب ممرض منزلي") {

                showRequestScreen()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            ).apply {

                setMargins(0, 25, 0, 10)
            }
        )

        root.addView(
            button("رجوع للرئيسية") {

                showHome()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            )
        )

        setContentView(root)
    }

    private fun contactUs() {

        AlertDialog.Builder(this)
            .setTitle("التواصل معنا")
            .setMessage(
                "للتواصل مع فريق التمريض المنزلي، " +
                "سيتم إضافة الاتصال وواتساب في المرحلة التالية."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun requestLocation() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST
            )

            return
        }

        val manager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        val gpsEnabled =
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!gpsEnabled) {

            AlertDialog.Builder(this)
                .setTitle("تفعيل الموقع")
                .setMessage(
                    "يجب تشغيل خدمة الموقع حتى نستطيع تحديد موقعك."
                )
                .setPositiveButton("الإعدادات") { _, _ ->

                    startActivity(
                        Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        )
                    )
                }
                .setNegativeButton("إلغاء", null)
                .show()

            return
        }

        Toast.makeText(
            this,
            "تم السماح بالوصول إلى الموقع",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == LOCATION_REQUEST) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(
                    this,
                    "تم السماح بتحديد موقعك",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "لم يتم السماح بتحديد الموقع",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
