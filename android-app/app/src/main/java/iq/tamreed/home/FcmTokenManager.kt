package iq.tamreed.home

import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    /**
     * تسجيل FCM Token للمستخدم الحالي.
     *
     * role:
     * patient = مريض
     * nurse   = ممرض
     */
    fun registerToken(role: String) {

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    android.util.Log.e(
                        TAG,
                        "Failed to get FCM token",
                        task.exception
                    )
                    return@addOnCompleteListener
                }

                val token = task.result

                if (token.isNullOrBlank()) {
                    android.util.Log.e(
                        TAG,
                        "FCM token is empty"
                    )
                    return@addOnCompleteListener
                }

                CoroutineScope(Dispatchers.IO).launch {

                    try {

                        val user =
                            SupabaseManager
                                .client
                                .auth
                                .currentUserOrNull()

                        if (user == null) {

                            android.util.Log.w(
                                TAG,
                                "No authenticated user"
                            )

                            return@launch
                        }

                        val record =
                            NotificationTokenRecord(
                                user_id = user.id,
                                token = token,
                                role = role
                            )

                        SupabaseManager
                            .client
                            .from("notification_tokens")
                            .upsert(
                                record
                            )

                        android.util.Log.d(
                            TAG,
                            "FCM token saved successfully"
                        )

                    } catch (e: Exception) {

                        android.util.Log.e(
                            TAG,
                            "Error saving FCM token",
                            e
                        )
                    }
                }
            }
    }

    /**
     * تحديث الـ Token عند تغيّره.
     */
    fun updateToken(
        token: String,
        role: String
    ) {

        if (token.isBlank()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val user =
                    SupabaseManager
                        .client
                        .auth
                        .currentUserOrNull()

                if (user == null) {
                    return@launch
                }

                val record =
                    NotificationTokenRecord(
                        user_id = user.id,
                        token = token,
                        role = role
                    )

                SupabaseManager
                    .client
                    .from("notification_tokens")
                    .upsert(
                        record
                    )

                android.util.Log.d(
                    TAG,
                    "FCM token updated successfully"
                )

            } catch (e: Exception) {

                android.util.Log.e(
                    TAG,
                    "Error updating FCM token",
                    e
                )
            }
        }
    }
}

/**
 * البيانات التي يتم حفظها في Supabase.
 */
@Serializable
data class NotificationTokenRecord(
    val user_id: String,
    val token: String,
    val role: String
)
