package io.docview.push.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    @JvmStatic
    fun setLogEnabled(enabled: Boolean) = Unit
}

class ResetCtrl private constructor() {
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences("push_reset_ctrl", Context.MODE_PRIVATE)
        }
    }

    fun isInitialized(): Boolean = prefs != null

    fun getIntValue(
        key: String,
        defaultValue: Int = 0,
        enableMidnightReset: Boolean = false
    ): Int {
        val preferences = prefs ?: return defaultValue
        val normalizedKey = normalizeKey(key, enableMidnightReset)
        return preferences.getInt(normalizedKey, defaultValue)
    }

    fun incrementIntValue(
        key: String,
        increment: Int = 1,
        enableMidnightReset: Boolean = false
    ): Int {
        val preferences = prefs ?: return increment
        val normalizedKey = normalizeKey(key, enableMidnightReset)
        val updated = preferences.getInt(normalizedKey, 0) + increment
        preferences.edit().putInt(normalizedKey, updated).apply()
        return updated
    }

    private fun normalizeKey(key: String, enableMidnightReset: Boolean): String {
        if (!enableMidnightReset) {
            return key
        }
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return "${key}_$date"
    }

    companion object {
        private val INSTANCE = ResetCtrl()

        @JvmStatic
        fun getInstance(): ResetCtrl = INSTANCE
    }
}
