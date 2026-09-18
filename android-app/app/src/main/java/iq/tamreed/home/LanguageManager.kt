package iq.tamreed.home

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    private const val PREFS = "tamreed_language"
    private const val KEY = "language"

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?: context.resources.configuration.locales[0].language
                .takeIf { it == "en" } ?: "ar"

    fun isEnglish(context: Context): Boolean = current(context) == "en"

    fun applySaved(context: Context) {
        val language = current(context)
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val target = if (language == "en") "en" else "ar"
        if (current != target) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(target))
        }
    }

    fun set(context: Context, language: String) {
        val normalized = if (language == "en") "en" else "ar"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, normalized)
            .apply()
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalized)
        )
    }

    fun label(context: Context): String = if (isEnglish(context)) "العربية" else "English"

    fun tr(context: Context, value: String): String {
        if (!isEnglish(context)) return value
        return translations[value] ?: value
    }

    private val translations = mapOf(
        "حساب موثق" to "Verified account",
        "أدخل بيانات المريض وحدد موقع الوصول بدقة" to "Enter patient details and the exact arrival location",
        "أدخل رقم هاتفك للمتابعة" to "Enter your phone number to continue",
        "أدخل رمز التحقق" to "Enter the verification code",
        "أو اختر نوع الدخول" to "Or choose a login method",
        "إتمام طلب التمريض" to "Complete nursing request",
        "إنشاء كلمة المرور" to "Create password",
        "التمريض المنزلي" to "Home Nursing",
        "الخدمات الأكثر طلباً" to "Most requested services",
        "الخدمة المطلوبة" to "Requested service",
        "الدعم الفني" to "Technical support",
        "المعلومات الأساسية" to "Basic information",
        "الممرض المعين" to "Assigned nurse",
        "تأكيد رقم الهاتف" to "Confirm phone number",
        "تحقق آمن" to "Secure verification",
        "تسجيل الدخول" to "Log in",
        "تم إرسال رمز مكوّن من 6 أرقام إلى" to "A 6-digit code was sent to",
        "جاري تحميل الطلبات..." to "Loading requests...",
        "خدمات التمريض المنزلي" to "Home nursing services",
        "خدماتك" to "Your services",
        "خدمة تمريض منزلية موثوقة في الأنبار" to "Trusted home nursing service in Anbar",
        "رعاية أقرب إليك" to "Care closer to you",
        "رعاية مهنية موثوقة في منزلك" to "Trusted professional care at home",
        "صحة أفضل .. حياة أفضل" to "Better health .. Better life",
        "عرض الكل" to "View all",
        "عنوان الوصول" to "Arrival address",
        "لا تشارك رمز التحقق مع أي شخص" to "Do not share the verification code with anyone",
        "متابعة طلب التمريض" to "Track nursing request",
        "ملاحظات إضافية" to "Additional notes",
        "موقع المريض" to "Patient location",
        "📍 تحديد موقع المريض" to "📍 Set patient location",
        "🔐" to "🔐",
        "🔒  بياناتك محمية وآمنة" to "🔒  Your data is protected and secure",
        "إرسال رمز التحقق" to "Send verification code",
        "إرسال رمز جديد" to "Send new code",
        "إرسال طلب التمريض" to "Send nursing request",
        "إلغاء" to "Cancel",
        "إلغاء الطلب" to "Cancel request",
        "اتصال بالدعم" to "Contact support",
        "اتصال بالممرض" to "Call nurse",
        "المحادثة مع الممرض" to "Chat with nurse",
        "تأكيد الرمز" to "Confirm code",
        "تحديد موقعي الآن" to "Set my location now",
        "تسجيل الخروج" to "Log out",
        "طلب ممرض الآن" to "Request a nurse now",
        "نسيت كلمة المرور" to "Forgot password",
        "نسيت كلمة المرور؟ أرسل رمزاً جديداً" to "Forgot password? Send a new code",
        "واتساب الدعم" to "WhatsApp support",
        "↩️ رجوع" to "↩️ Back",
        "＋   إنشاء طلب جديد" to "＋   Create new request",
        "👤 ملف الممرض" to "👤 Nurse profile",
        "💾 حفظ كلمة المرور" to "💾 Save password",
        "📍 فتح موقع الطلب" to "📍 Open request location",
        "🔐 دخول بكلمة المرور" to "🔐 Log in with password",
        "🗺️ فتح الموقع في خرائط Google" to "🗺️ Open location in Google Maps",
        "🩺  إنشاء طلب تمريض" to "🩺  Create nursing request",
        "English" to "العربية"
    )
}
