# إعداد إشعارات الممرضين

1. شغّل `supabase/notification_push_setup.sql` في Supabase SQL Editor.
2. انشر Edge Function الموجودة في `supabase/functions/notify-new-booking/index.ts` باسم `notify-new-booking`.
3. أضف Secrets للـ Edge Function:
   - `FIREBASE_PROJECT_ID`
   - `FIREBASE_CLIENT_EMAIL`
   - `FIREBASE_PRIVATE_KEY`
   - `SUPABASE_URL`
   - `SUPABASE_SERVICE_ROLE_KEY` (يُحفظ في Secrets فقط، ولا يوضع داخل APK).
4. بعد تسجيل دخول الممرض، يسجل التطبيق جهازه تلقائياً في `notification_tokens`.
5. عند إنشاء طلب جديد، يستدعي تطبيق المريض الـ Edge Function؛ وهي ترسل Push للممرضين المعتمدين والمتاحين ولديهم اشتراك فعال.
