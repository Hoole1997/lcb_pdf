package com.documentpro.office.business.fileviewer.utils

import com.blankj.utilcode.util.LanguageUtils
import com.blankj.utilcode.util.SPUtils
import java.util.Calendar
import java.util.Date

object BusinessSPConfig {

    private const val KEY_FIRST_LAUNCH_TIME = "first_launch_time"
    private const val KEY_FIRST_LAUNCH_DATE = "first_launch_date"

    fun isInitLanguage(): Boolean {
        return LanguageUtils.getAppliedLanguage() != null
    }

    /**
     * 初始化首次启动时间，需要在Application中调用
     */
    fun initFirstLaunchTimeIfNeeded() {
        val sp = SPUtils.getInstance()
        val firstLaunchTime = sp.getLong(KEY_FIRST_LAUNCH_TIME, 0L)
        
        if (firstLaunchTime == 0L) {
            val currentTime = System.currentTimeMillis()
            sp.put(KEY_FIRST_LAUNCH_TIME, currentTime, true)
            sp.put(KEY_FIRST_LAUNCH_DATE, formatDateString(currentTime), true)
        }
    }

    /**
     * 判断是否是新用户，新用户标准为是否是同一天
     * @return true: 新用户(当天), false: 老用户(非当天)
     */
    fun isNewUser(): Boolean {
        val sp = SPUtils.getInstance()
        val firstLaunchDate = sp.getString(KEY_FIRST_LAUNCH_DATE, "")
        
        // 如果没有首次启动日期记录，说明是首次启动
        if (firstLaunchDate.isEmpty()) {
            return true
        }
        
        val currentDate = formatDateString(System.currentTimeMillis())
        return firstLaunchDate == currentDate
    }

    /**
     * 获取用户第几天打开应用
     * @return 从首次打开应用算起的天数，0表示当天，1表示第二天，以此类推
     */
    fun loginDay(): Int {
        val sp = SPUtils.getInstance()
        val firstLaunchTime = sp.getLong(KEY_FIRST_LAUNCH_TIME, 0L)
        
        if (firstLaunchTime == 0L) {
            return 0
        }
        
        val firstLaunchCalendar = Calendar.getInstance().apply {
            timeInMillis = firstLaunchTime
        }
        val currentCalendar = Calendar.getInstance()
        
        // 计算两个日期之间的天数差
        val firstDay = getDayOfYear(firstLaunchCalendar)
        val currentDay = getDayOfYear(currentCalendar)
        val yearDiff = currentCalendar.get(Calendar.YEAR) - firstLaunchCalendar.get(Calendar.YEAR)
        
        return if (yearDiff == 0) {
            currentDay - firstDay
        } else {
            // 跨年计算
            var days = 0
            val tempCalendar = Calendar.getInstance().apply {
                timeInMillis = firstLaunchTime
            }
            
            while (tempCalendar.get(Calendar.YEAR) < currentCalendar.get(Calendar.YEAR)) {
                days += tempCalendar.getActualMaximum(Calendar.DAY_OF_YEAR) - tempCalendar.get(Calendar.DAY_OF_YEAR)
                tempCalendar.add(Calendar.YEAR, 1)
                tempCalendar.set(Calendar.DAY_OF_YEAR, 1)
            }
            
            days + currentCalendar.get(Calendar.DAY_OF_YEAR) - 1
        }
    }
    
    /**
     * 格式化日期为字符串 (yyyy-MM-dd)
     */
    private fun formatDateString(timeMillis: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeMillis
        }
        return "${calendar.get(Calendar.YEAR)}-${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}-${String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))}"
    }
    
    /**
     * 获取一年中的第几天
     */
    private fun getDayOfYear(calendar: Calendar): Int {
        return calendar.get(Calendar.DAY_OF_YEAR)
    }
}