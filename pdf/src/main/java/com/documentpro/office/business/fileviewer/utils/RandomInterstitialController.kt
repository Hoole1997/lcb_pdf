package com.documentpro.office.business.fileviewer.utils

import androidx.fragment.app.FragmentActivity

/**
 * 插页随机广告位控制器
 * 控制随机插页广告的展示时间间隔
 */
object RandomInterstitialController {

    private const val TAG = "RandomInterstitialController"
    private const val PREFS_NAME = "random_interstitial_prefs"
    private const val KEY_LAST_SHOW_TIME = "last_show_time"

    /**
     * 显示随机插页广告（带时间间隔控制）
     * @param activity FragmentActivity
     * @param onAdShown 广告展示成功回调（可选）
     * @param onAdDismissed 广告关闭回调（可选）
     */
    fun showRandomInterstitial(
        activity: FragmentActivity,
        onAdShown: (() -> Unit)? = null,
        onAdDismissed: (() -> Unit)? = null
    ) {
        // 检查是否满足时间间隔要求
        if (!isIntervalPassed(activity)) {
            android.util.Log.d(TAG, "未达到时间间隔要求，跳过广告展示")
            onAdDismissed?.invoke()
            return
        }

        // 执行广告加载和展示
        activity.loadInterstitial {
            // 广告展示成功，更新最后展示时间
            updateLastShowTime(activity)
            android.util.Log.d(TAG, "随机插页广告展示成功")
            onAdShown?.invoke()

            // 广告关闭回调
            onAdDismissed?.invoke()
        }
    }

    /**
     * 检查是否已经超过最小时间间隔
     * @param activity Activity上下文
     * @return true表示已超过间隔，可以展示广告；false表示未达到间隔
     */
    private fun isIntervalPassed(activity: FragmentActivity): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastShowTime = getLastShowTime(activity)

        // 如果从未展示过（lastShowTime = 0），允许展示
        if (lastShowTime == 0L) {
            android.util.Log.d(TAG, "首次展示，允许显示广告")
            return true
        }

        // 获取主页广告位配置中的插页时间间隔（秒）
        val intervalSeconds = 120
        val intervalMillis = intervalSeconds * 1000L

        // 计算已经过的时间
        val elapsedTime = currentTime - lastShowTime

        android.util.Log.d(TAG, "时间间隔检查: 要求=${intervalSeconds}秒, 已过=${elapsedTime/1000}秒, 是否满足=${elapsedTime >= intervalMillis}")

        return elapsedTime >= intervalMillis
    }

    /**
     * 获取上次展示时间
     * @param activity Activity上下文
     * @return 上次展示的时间戳（毫秒）
     */
    private fun getLastShowTime(activity: FragmentActivity): Long {
        val prefs = activity.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SHOW_TIME, 0L)
    }

    /**
     * 更新最后展示时间为当前时间
     * @param activity Activity上下文
     */
    private fun updateLastShowTime(activity: FragmentActivity) {
        val currentTime = System.currentTimeMillis()
        val prefs = activity.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_SHOW_TIME, currentTime)
            .apply()

        android.util.Log.d(TAG, "更新最后展示时间: $currentTime")
    }

    /**
     * 重置展示时间（用于测试或特殊情况）
     * @param activity Activity上下文
     */
    fun resetShowTime(activity: FragmentActivity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_LAST_SHOW_TIME)
            .apply()

        android.util.Log.d(TAG, "重置展示时间")
    }

    /**
     * 获取距离下次可展示的剩余时间（秒）
     * @param activity Activity上下文
     * @return 剩余秒数，如果已可展示则返回0
     */
    fun getRemainingSeconds(activity: FragmentActivity): Int {
        val currentTime = System.currentTimeMillis()
        val lastShowTime = getLastShowTime(activity)

        if (lastShowTime == 0L) {
            return 0
        }

        val intervalSeconds = 120
        val intervalMillis = intervalSeconds * 1000L
        val elapsedTime = currentTime - lastShowTime

        if (elapsedTime >= intervalMillis) {
            return 0
        }

        val remainingMillis = intervalMillis - elapsedTime
        return (remainingMillis / 1000).toInt()
    }
}
