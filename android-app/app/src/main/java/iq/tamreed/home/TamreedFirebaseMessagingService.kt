package iq.tamreed.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * استقبال إشعارات Firebase Cloud Messaging.
 *
 * يدعم:
 * - رسائل البيانات Data في المقدمة والخلفية.
 * - إظهار إشعار محلي عند وصول رسالة بيانات.
 * - حفظ/معالجة FCM token لاحقًا لربطه بحساب المريض أو الممرض.
 */
class TamreedFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "tamreed_fcm"
        const val CHANNEL_NAME = "إشعارات التمريض المنزلي"
        const val CHANNEL_DESCRIPTION =
            "إشعارات الطلبات وتغيّر حالة طلب التمريض"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // سيتم ربط هذا الـ token بحساب المريض/الممرض
        // في Supabase في المرحلة التالية.
        android.util.Log.d(
            "TamreedFCM",
            "FCM token updated: $token"
        )
    }

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data

        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "التمريض المنزلي"

        val body = data["body"]
            ?: remoteMessage.notification?.body
            ?: "لديك إشعار جديد"

        val target = data["target"] ?: "main"

        val bookingId = data["booking_id"]

        showNotification(
            title = title,
            body = body,
            target = target,
            bookingId = bookingId
        )
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description = CHANNEL_DESCRIPTION
            }

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        target: String,
        bookingId: String?
    ) {

        // Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val granted =
                checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!granted) {
                return
            }
        }

        val intent = when (target.lowercase()) {

            "nurse",
            "nurse_requests",
            "nursedashboard" -> {

                Intent(
                    this,
                    NurseRequestsActivity::class.java
                )
            }

            "booking",
            "my_bookings",
            "patient" -> {

                Intent(
                    this,
                    MainActivity::class.java
                ).apply {

                    putExtra(
                        "open_bookings",
                        true
                    )
                }
            }

            else -> {

                Intent(
                    this,
                    MainActivity::class.java
                )
            }

        }.apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP

            if (!bookingId.isNullOrBlank()) {

                putExtra(
                    "booking_id",
                    bookingId
                )
            }
        }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                bookingId?.hashCode()
                    ?: System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setContentIntent(
                    pendingIntent
                )
                .build()

        NotificationManagerCompat
            .from(this)
            .notify(
                bookingId?.hashCode()
                    ?: System.currentTimeMillis().toInt(),
                notification
            )
    }
}
