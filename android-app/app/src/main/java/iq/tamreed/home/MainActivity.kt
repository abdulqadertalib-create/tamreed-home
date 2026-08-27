package iq.tamreed.home

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text = "التمريض المنزلي"
            textSize = 30f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }

        val subtitle = TextView(this).apply {
            text = "خدمة التمريض المنزلي لمحافظة الأنبار - العراق"
            textSize = 18f
            setPadding(0, 24, 0, 40)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }

        val requestButton = Button(this).apply {
            text = "طلب ممرض منزلي"
            textSize = 18f
        }

        val servicesButton = Button(this).apply {
            text = "الخدمات الطبية"
            textSize = 18f
        }

        val contactButton = Button(this).apply {
            text = "التواصل معنا"
            textSize = 18f
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(requestButton)
        root.addView(servicesButton)
        root.addView(contactButton)

        setContentView(root)
    }
}
