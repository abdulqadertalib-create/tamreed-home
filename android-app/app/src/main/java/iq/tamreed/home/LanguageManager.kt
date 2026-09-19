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
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(target)
            )
        }
    }

    fun set(context: Context, language: String) {
        val normalized = if (language == "en") "en" else "ar"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, normalized).apply()
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalized)
        )
    }

    fun label(context: Context): String =
        if (isEnglish(context)) "العربية" else "English"

    fun tr(context: Context, value: String): String {
        if (!isEnglish(context)) return value
        return translations[value] ?: value
    }

    private val translations = mapOf(
        "حساب موثق" to "Verified account",
        "مرحباً بك" to "Welcome",
        "حساب المريض" to "Patient account",
        "المزيد" to "More",
        "المحادثات" to "Chats",
        "الطلبات" to "Requests",
        "الرئيسية" to "Home",
        "الأنبار - الفلوجة" to "Anbar - Fallujah",
        "تمريض منزلي في محافظة الأنبار" to "Home nursing in Anbar Governorate",
        "العربية" to "Arabic",
        "اللغة" to "Language",

        "رعاية تمريضية منزلية" to "Home nursing care",
        "رعاية أقرب إليك" to "Care closer to you",
        "التمريض المنزلي" to "Home Nursing",
        "رعاية مهنية موثوقة في منزلك" to "Trusted professional care at home",
        "كوادر مهنية" to "Professional staff",
        "مؤهلة" to "Qualified",
        "رعاية" to "Care",
        "في منزلك" to "In your home",
        "آمن" to "Safe",
        "وموثوق" to "and trusted",
        "صحة أفضل .. حياة أفضل" to "Better health .. Better life",
        "خدمة تمريض منزلية موثوقة في الأنبار" to "Trusted home nursing service in Anbar",

        "تسجيل الدخول" to "Log in",
        "أدخل رقم هاتفك للمتابعة" to "Enter your phone number to continue",
        "كلمة المرور" to "Password",
        "🔐 دخول بكلمة المرور" to "🔐 Log in with password",
        "أدخل رقم هاتف عراقي صحيح" to "Enter a valid Iraqi phone number",
        "كلمة المرور 6 أحرف/أرقام على الأقل" to "Password must contain at least 6 letters/numbers",
        "نسيت كلمة المرور" to "Forgot password",
        "أدخل رقم الهاتف أولاً" to "Enter the phone number first",
        "إرسال رمز التحقق" to "Send verification code",
        "سيصلك رمز تحقق SMS مكوّن من 6 أرقام" to "You will receive a 6-digit verification code by SMS",
        "أو اختر نوع الدخول" to "Or choose a login method",
        "دخول الممرضين" to "Nurse login",
        "للكوادر المعتمدة" to "For authorized staff",
        "دخول الإدارة" to "Admin login",
        "لإدارة المنصة" to "For platform management",
        "🔒  بياناتك محمية وآمنة" to "🔒  Your data is protected and secure",

        "جاري إرسال رمز التحقق..." to "Sending verification code...",
        "تعذر إرسال الرمز" to "Unable to send the code",
        "تأكد من إعداد Phone Auth في Supabase." to "Make sure Phone Auth is configured in Supabase.",
        "تأكيد رقم الهاتف" to "Confirm phone number",
        "تحقق آمن" to "Secure verification",
        "أدخل رمز التحقق" to "Enter the verification code",
        "تم إرسال رمز مكوّن من 6 أرقام إلى" to "A 6-digit code was sent to",
        "رمز التحقق" to "Verification code",
        "تأكيد الرمز" to "Confirm code",
        "أدخل رمز التحقق المكوّن من 6 أرقام" to "Enter the 6-digit verification code",
        "إرسال رمز جديد" to "Send new code",
        "لا تشارك رمز التحقق مع أي شخص" to "Do not share the verification code with anyone",
        "جاري التحقق..." to "Verifying...",
        "تم التحقق من رقم الهاتف" to "Phone number verified",
        "فشل التحقق" to "Verification failed",
        "رمز التحقق غير صحيح." to "The verification code is incorrect.",
        "جاري تسجيل الدخول..." to "Logging in...",
        "تعذر تسجيل الدخول" to "Unable to log in",

        "إنشاء كلمة المرور" to "Create password",
        "أنشئ كلمة مرور جديدة لحسابك" to "Create a new password for your account",
        "كلمة المرور الجديدة" to "New password",
        "تأكيد كلمة المرور" to "Confirm password",
        "💾 حفظ كلمة المرور" to "💾 Save password",
        "استخدم 6 أحرف/أرقام على الأقل" to "Use at least 6 letters/numbers",
        "كلمتا المرور غير متطابقتين" to "The passwords do not match",
        "جاري حفظ كلمة المرور..." to "Saving password...",
        "تعذر حفظ كلمة المرور" to "Unable to save password",
        "حاول مرة أخرى." to "Please try again.",
        "نسيت كلمة المرور؟ أرسل رمزاً جديداً" to "Forgot password? Send a new code",

        "الإشعارات" to "Notifications",
        "لا توجد إشعارات جديدة" to "No new notifications",
        "طلب ممرض الآن" to "Request a nurse now",
        "خدماتك" to "Your services",
        "عرض الكل" to "View all",
        "طلباتي" to "My requests",
        "متابعة الطلبات" to "Track requests",
        "الخدمات" to "Services",
        "اختر خدمة" to "Choose a service",
        "تواصل معنا" to "Contact us",
        "الخدمات الأكثر طلباً" to "Most requested services",
        "إعطاء الحقن" to "Injections",
        "خدمة منزلية" to "Home service",
        "تغيير الضماد" to "Dressing change",
        "العناية بالجروح" to "Wound care",
        "قياس السكر" to "Blood sugar check",
        "فحص منزلي" to "Home check",
        "موثوقة" to "Trusted",
        "رعاية آمنة" to "Safe care",
        "وصول للموقع" to "On-site arrival",
        "سريعة" to "Fast",
        "أقرب وقت" to "Earliest available time",

        "طلب ممرض" to "Nurse request",
        "إتمام طلب التمريض" to "Complete nursing request",
        "أدخل بيانات المريض وحدد موقع الوصول بدقة" to "Enter patient details and the exact arrival location",
        "المعلومات الأساسية" to "Basic information",
        "اسم المريض" to "Patient name",
        "الخدمة المطلوبة" to "Requested service",
        "جاري تحميل الخدمات..." to "Loading services...",
        "تعذر تحميل الخدمات" to "Unable to load services",
        "موقع المريض" to "Patient location",
        "لم يتم تحديد الموقع بعد" to "Location has not been set yet",
        "تم تحديد الموقع بنجاح" to "Location set successfully",
        "تحديد موقعي الآن" to "Set my location now",
        "جاري تحديد موقع المريض..." to "Locating the patient...",
        "عنوان الوصول" to "Arrival address",
        "اختر المدينة / القضاء" to "Choose city / district",
        "الرمادي" to "Ramadi",
        "الفلوجة" to "Fallujah",
        "الكرمة" to "Al-Karma",
        "الحبانية" to "Habaniyah",
        "الخالدية" to "Khalidiya",
        "هيت" to "Hit",
        "حديثة" to "Haditha",
        "عانة" to "Ana",
        "راوة" to "Rawa",
        "القائم" to "Al-Qaim",
        "الرطبة" to "Rutba",
        "البغدادي" to "Al-Baghdadi",
        "عامرية الصمود" to "Amiriyah Al-Sumoud",
        "أقرب نقطة دالة: جامع، مدرسة، مستشفى، شارع..." to "Nearest landmark: mosque, school, hospital, street...",
        "ملاحظات إضافية" to "Additional notes",
        "معلومة تساعد الممرض على فهم الحالة (اختياري)..." to "Information that helps the nurse understand the case (optional)...",
        "إرسال طلب التمريض" to "Send nursing request",
        "اختر الخدمة أولاً" to "Choose a service first",
        "أدخل اسم المريض" to "Enter the patient name",
        "أدخل أقرب نقطة دالة" to "Enter the nearest landmark",
        "إلغاء" to "Cancel",
        "تأكيد الطلب" to "Confirm request",
        "تعديل" to "Edit",
        "إرسال" to "Send",
        "جاري إرسال الطلب..." to "Sending request...",
        "تم إرسال الطلب ✅" to "Request sent ✅",
        "العودة للرئيسية" to "Back to home",
        "متابعة الطلب" to "Track request",
        "تعذر إرسال الطلب" to "Unable to send request",

        "الخدمات الطبية" to "Medical services",
        "خدمات التمريض المنزلي" to "Home nursing services",
        "اختر الخدمة المناسبة ثم اطلع على تفاصيلها قبل إرسال الطلب." to "Choose the appropriate service and review its details before sending the request.",
        "ابحث عن خدمة..." to "Search for a service...",
        "إعطاء الحقن حسب وصف الطبيب" to "Administer injections as prescribed by the doctor",
        "العناية بالجروح والضمادات" to "Wound and dressing care",
        "فحص مستوى سكر الدم" to "Blood glucose check",
        "قياس الضغط" to "Blood pressure measurement",
        "قياس ومتابعة ضغط الدم" to "Blood pressure measurement and monitoring",
        "تركيب المحلول" to "IV fluid administration",
        "تركيب المحاليل حسب الحاجة" to "IV fluids as needed",
        "رعاية كبار السن" to "Elderly care",
        "رعاية ومتابعة كبار السن" to "Elderly care and follow-up",
        "وضع القسطرة البولية" to "Urinary catheter insertion",
        "تركيب القسطرة البولية في المنزل" to "Urinary catheter insertion at home",
        "تركيب الكانيولا" to "IV cannula insertion",
        "تركيب الكانيولا والعناية بمكانها" to "IV cannula insertion and site care",
        "لم نجد الخدمة المطلوبة" to "The requested service was not found",
        "جرّب كتابة اسم خدمة آخر." to "Try entering another service name.",
        "🩺  إنشاء طلب تمريض" to "🩺  Create nursing request",
        "تفاصيل الخدمة" to "Service details",
        "إغلاق" to "Close",
        "اطلب هذه الخدمة" to "Request this service",

        "📍 تحديد موقع المريض" to "📍 Set patient location",
        "يساعد الموقع الممرض على الوصول إلى المكان الصحيح" to "The location helps the nurse reach the correct place",
        "📍 تحديد موقعي الآن" to "📍 Set my location now",
        "جاري التحقق من إعدادات الموقع..." to "Checking location settings...",
        "🗺️ فتح الموقع في خرائط Google" to "🗺️ Open location in Google Maps",
        "↩️ رجوع" to "↩️ Back",
        "الموقع متوقف. سيتم فتح إعدادات GPS لتشغيله." to "Location is turned off. GPS settings will be opened.",
        "تشغيل الموقع GPS" to "Turn on GPS location",
        "فتح إعدادات GPS" to "Open GPS settings",
        "تم تحديد موقعك بنجاح" to "Your location was set successfully",
        "حدد موقعك أولاً" to "Set your location first",
        "تعذر فتح خرائط Google" to "Unable to open Google Maps",
        "تم السماح بالموقع." to "Location permission granted.",
        "صلاحية الموقع مطلوبة" to "Location permission is required",

        "English" to "العربية"
    )
}
