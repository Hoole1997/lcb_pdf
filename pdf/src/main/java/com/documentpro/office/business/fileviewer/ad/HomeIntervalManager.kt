package com.documentpro.office.business.fileviewer.ad

import android.content.Context
import io.docview.push.utils.ResetCtrl
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 首页插页广告类型
 */
enum class HomeInterstitialType {
    DRAWER_CLOSE,
    SEARCH_EXIT
}

/**
 * 首页广告间隔管理器
 * 控制首页各场景插页广告的展示间隔
 */
object HomeIntervalManager {

    private object Keys {
        const val DRAWER_CLOSE_TODAY_SHOW_COUNT = "home_drawer_close_interstitial_today_show_count"
    }

    private val lastActionTimeMap = ConcurrentHashMap<String, Long>()
    private val resetController = ResetCtrl.getInstance()
    private val zeroIntervalBlockedTypes = listOf(
        HomeInterstitialType.DRAWER_CLOSE
    )

    /**
     * 检查是否可以执行操作（是否已过间隔时间）
     * @param tag 操作标识
     * @param intervalSeconds 间隔时间（秒）
     * @return true 表示放行，false 表示拦截
     */
    @JvmStatic
    fun shouldAllow(tag: String, intervalSeconds: Int): Boolean {
        if (intervalSeconds == 0 && zeroIntervalBlockedTypes.any { it.name == tag }) {
            return false
        }

        val currentTime = System.currentTimeMillis()
        val lastTime = lastActionTimeMap[tag] ?: 0L
        val intervalMillis = TimeUnit.SECONDS.toMillis(intervalSeconds.toLong())

        return if (currentTime - lastTime >= intervalMillis) {
            lastActionTimeMap[tag] = currentTime
            true
        } else {
            false
        }
    }

    /**
     * 检查是否可以执行操作（不自动记录时间）
     */
    @JvmStatic
    fun checkOnly(tag: String, intervalSeconds: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastActionTimeMap[tag] ?: 0L
        val intervalMillis = TimeUnit.SECONDS.toMillis(intervalSeconds.toLong())
        return currentTime - lastTime >= intervalMillis
    }

    /**
     * 手动记录操作时间
     */
    @JvmStatic
    fun record(tag: String) {
        lastActionTimeMap[tag] = System.currentTimeMillis()
    }

    /**
     * 重置指定标识的时间记录
     */
    @JvmStatic
    fun reset(tag: String) {
        lastActionTimeMap.remove(tag)
    }

    /**
     * 清除所有时间记录
     */
    @JvmStatic
    fun clearAll() {
        lastActionTimeMap.clear()
    }

    private fun ensureResetControllerInitialized(context: Context) {
        if (!resetController.isInitialized()) {
            resetController.initialize(context.applicationContext)
        }
    }

    @JvmStatic
    fun isSilentDurationSatisfied(context: Context, silentSeconds: Int): Boolean {
        if (silentSeconds <= 0) {
            return true
        }
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val installTimeMillis = packageInfo.firstInstallTime
            if (installTimeMillis <= 0L) {
                true
            } else {
                val silentMillis = TimeUnit.SECONDS.toMillis(silentSeconds.toLong())
                System.currentTimeMillis() - installTimeMillis >= silentMillis
            }
        } catch (_: Exception) {
            true
        }
    }

    @JvmStatic
    fun isDailyMaxCountSatisfied(context: Context, dailyMaxCount: Int): Boolean {
        if (dailyMaxCount <= 0) {
            return true
        }
        ensureResetControllerInitialized(context)
        val todayShowCount = resetController.getIntValue(
            Keys.DRAWER_CLOSE_TODAY_SHOW_COUNT,
            defaultValue = 0,
            enableMidnightReset = true
        )
        return todayShowCount < dailyMaxCount
    }

    @JvmStatic
    fun recordDrawerCloseInterstitialShow(context: Context) {
        ensureResetControllerInitialized(context)
        resetController.incrementIntValue(
            Keys.DRAWER_CLOSE_TODAY_SHOW_COUNT,
            increment = 1,
            enableMidnightReset = true
        )
    }

    /**
     * 根据首页广告类型和配置，判断是否展示插页广告
     */
    @JvmStatic
    fun executeWithCondition(
        type: HomeInterstitialType,
        context: Context,
        action: () -> Unit,
        noMatch: () -> Unit
    ) {
        val homeConfig = HomePlacementManager.getHomePlacement()
        val isEnabled = when (type) {
            HomeInterstitialType.DRAWER_CLOSE -> true
            HomeInterstitialType.SEARCH_EXIT -> true
        }
        val intervalSeconds = when (type) {
            HomeInterstitialType.DRAWER_CLOSE -> homeConfig.drawerCloseInterstitialIntervalSeconds
            HomeInterstitialType.SEARCH_EXIT -> 0
        }
        val silentSeconds = when (type) {
            HomeInterstitialType.DRAWER_CLOSE -> homeConfig.drawerCloseFirstInstallSilentSeconds
            HomeInterstitialType.SEARCH_EXIT -> 0
        }
        val dailyMaxCount = when (type) {
            HomeInterstitialType.DRAWER_CLOSE -> homeConfig.drawerCloseInterstitialDailyMaxCount
            HomeInterstitialType.SEARCH_EXIT -> 0
        }
        val isSilentDurationSatisfied = when (type) {
            HomeInterstitialType.DRAWER_CLOSE -> isSilentDurationSatisfied(context, silentSeconds)
            HomeInterstitialType.SEARCH_EXIT -> true
        }
        val isDailyMaxCountSatisfied = when (type) {
            HomeInterstitialType.DRAWER_CLOSE -> isDailyMaxCountSatisfied(context, dailyMaxCount)
            HomeInterstitialType.SEARCH_EXIT -> true
        }
        if (
            isEnabled &&
            isSilentDurationSatisfied &&
            isDailyMaxCountSatisfied &&
            shouldAllow(type.name, intervalSeconds)
        ) {
            if (type == HomeInterstitialType.DRAWER_CLOSE) {
                recordDrawerCloseInterstitialShow(context)
            }
            action.invoke()
        } else {
            noMatch.invoke()
        }
    }
}
