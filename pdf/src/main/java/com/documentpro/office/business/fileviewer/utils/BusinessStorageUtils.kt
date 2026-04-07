package com.documentpro.office.business.fileviewer.utils

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/**
 * 存储工具类，根据设备架构选择合适的存储方式
 */
object BusinessStorageUtils {
    private lateinit var defaultSP: SharedPreferences
    
    fun init(context: Context) {
        defaultSP = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }
    
    fun putString(key: String, value: String) {
        defaultSP.edit().putString(key, value).apply()
    }
    
    fun getString(key: String, defaultValue: String = ""): String {
        return defaultSP.getString(key, defaultValue) ?: defaultValue
    }
    
    fun putInt(key: String, value: Int) {
        defaultSP.edit().putInt(key, value).apply()
    }
    
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return defaultSP.getInt(key, defaultValue)
    }
    
    fun putBoolean(key: String, value: Boolean) {
        defaultSP.edit().putBoolean(key, value).apply()
    }
    
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return defaultSP.getBoolean(key, defaultValue)
    }
    
    // 其他类型方法可以根据需要添加...
} 