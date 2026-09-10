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

    fun registerToken(role: String) {
        saveToken(role)
    }

    fun updateToken(token: String, role: String) {
        if (token.isBlank()) return
        saveToken(role, token)
    }

    private fun saveToken(role: String, knownToken: String? = null) {
        val normalizedRole = role.lowercase().let {
            if (it == "nurse" || it == "admin" || it == "patient") it else "patient"
        }

        val save: (String) -> Unit = { token ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val user = SupabaseManager.client.auth.currentUserOrNull() ?: return@launch
                    val record = NotificationTokenRecord(
                        user_id = user.id,
                        token = token,
                        role = normalizedRole
                    )

                    SupabaseManager.client
                        .from("notification_tokens")
                        .upsert(record) {
                            onConflict = "user_id,token"
                        }

                    android.util.Log.d(TAG, "FCM token saved: role=$normalizedRole")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error saving FCM token", e)
                }
            }
        }

        if (!knownToken.isNullOrBlank()) {
            save(knownToken)
        } else {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isNullOrBlank()) {
                    save(task.result)
                } else {
                    android.util.Log.e(TAG, "Failed to get FCM token", task.exception)
                }
            }
        }
    }
}

@Serializable
data class NotificationTokenRecord(
    val user_id: String,
    val token: String,
    val role: String
)
