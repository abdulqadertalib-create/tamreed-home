package iq.tamreed.home

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // الصفحة الرئيسية
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 60, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        // عنوان التطبيق
        val title = TextView(this).apply {
            text = "التمريض المنزلي"
            textSize = 30f
            setTextColor(Color.rgb(20, 100, 90))
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        // الوصف
        val subtitle = TextView(this).apply {
            text = "خدمات التمريض المنزلي\nلمحافظة الأنبار - العراق"
            textSize = 18f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 40)
        }

        // زر طلب ممرض منزلي
        val requestButton = Button(this).apply {
            text = "طلب ممرض منزلي"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(20, 130, 115))

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "سيتم فتح نموذج طلب الممرض المنزلي",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // زر الخدمات الطبية
        val servicesButton = Button(this).apply {
            text = "الخدمات الطبية"
            textSize = 18f

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "الخدمات الطبية",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // زر التواصل
        val contactButton = Button(this).apply {
            text = "التواصل"
            textSize = 18f

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "التواصل مع التمريض المنزلي",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // إضافة العناصر
        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            requestButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            ).apply {
                setMargins(0, 10, 0, 15)
            }
        )

        root.addView(
            servicesButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            ).apply {
                setMargins(0, 10, 0, 15)
            }
        )

        root.addView(
            contactButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            ).apply {
                setMargins(0, 10, 0, 15)
            }
        )

        setContentView(root)
    }
}
