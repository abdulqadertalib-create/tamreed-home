package iq.tamreed.home

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * سجل إشعارات بسيط داخل الجهاز لعرض الإشعارات داخل التطبيق.
 * الإشعارات المهمة القادمة من FCM أو تغيّر حالة الطلب تُحفظ هنا.
 */
object NotificationStore {

    private const val PREFS = "tamreed_notifications"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 50

    data class Item(
        val title: String,
        val message: String,
        val time: Long,
        val bookingId: String? = null
    )

    @Synchronized
    fun add(
        context: Context,
        title: String,
        message: String,
        bookingId: String? = null
    ) {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = read(prefs).toMutableList()
        current.add(
            0,
            Item(
                title = title,
                message = message,
                time = System.currentTimeMillis(),
                bookingId = bookingId
            )
        )

        val trimmed = current.take(MAX_ITEMS)
        val json = JSONArray()
        trimmed.forEach { item ->
            json.put(
                JSONObject().apply {
                    put("title", item.title)
                    put("message", item.message)
                    put("time", item.time)
                    put("booking_id", item.bookingId ?: "")
                }
            )
        }
        prefs.edit().putString(KEY_ITEMS, json.toString()).apply()
    }

    fun getAll(context: Context): List<Item> {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return read(prefs)
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ITEMS)
            .apply()
    }

    private fun read(
        prefs: android.content.SharedPreferences
    ): List<Item> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val json = JSONArray(raw)
            buildList {
                for (i in 0 until json.length()) {
                    val item = json.optJSONObject(i) ?: continue
                    add(
                        Item(
                            title = item.optString("title"),
                            message = item.optString("message"),
                            time = item.optLong("time", 0L),
                            bookingId = item.optString("booking_id")
                                .takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
