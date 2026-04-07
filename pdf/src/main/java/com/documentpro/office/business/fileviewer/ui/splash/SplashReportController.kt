package com.documentpro.office.business.fileviewer.ui.splash

import android.content.Intent
import android.util.Log
import com.documentpro.office.business.fileviewer.PdfAppInitializer
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.CustomSignals
import com.google.firebase.remoteconfig.remoteConfig
import io.docview.push.builder.LANDING_NOTIFICATION_CONTENT
import io.docview.push.builder.LANDING_NOTIFICATION_FROM
import io.docview.push.builder.LANDING_NOTIFICATION_TITLE
import io.docview.push.check.CheckCtrl
import io.docview.push.controller.TriggerCtrl
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager
import net.corekit.core.utils.ConfigRemoteManager

/**
 * 开屏页数据上报控制器
 *
 * 负责管理开屏页的所有埋点和数据上报
 */
class SplashReportController {

    companion object {
        private const val TAG = BusinessSplashScreenActivity.TAG
    }

    // 启动时间
    private var launchTime = 0L

    /**
     * 记录启动时间
     */
    fun recordLaunchTime() {
        BusinessPointLog.logEvent("loading_page_show")
        launchTime = System.currentTimeMillis()
    }

    /**
     * 获取启动时间
     */
    fun getLaunchTime(): Long = launchTime

    /**
     * 上报生命周期事件
     */
    fun reportLifecycle(lifecycle: String) {
        ReportDataManager.reportData(
            "BusinessSplashScreenActivity",
            mapOf("lifecycle" to lifecycle)
        )
    }

    /**
     * 上报页面显示
     */
    fun reportPageShow() {
        ReportDataManager.reportData("Loading_Page_Show", mapOf())
    }

    /**
     * 上报用户返回
     */
    fun reportUserBack() {
        ReportDataManager.reportData("Activity_User_Back", mapOf())
    }

    /**
     * 上报退出事件
     */
    fun reportExitEvent() {
        val passTime = if (launchTime > 0) {
            ceil((System.currentTimeMillis() - launchTime) / 1000.0).toInt()
        } else {
            0
        }
        BusinessPointLog.logEvent(
            "loading_page_end",
            mapOf("pass_time" to passTime)
        )
    }

    /**
     * 上报广告加载中用户切后台事件
     * 当广告正在加载但未返回结果时，用户切到后台
     */
    fun reportAdLoadingUserBackground() {
        val passTime = if (launchTime > 0) {
            ceil((System.currentTimeMillis() - launchTime) / 1000.0).toInt()
        } else {
            0
        }
        BusinessPointLog.logEvent(
            "splash_ad_loading_background",
            mapOf("pass_time" to passTime)
        )
        Log.d(TAG, "上报广告加载中用户切后台，已等待 ${passTime}s")
    }

    /**
     * 上报应用打开事件
     */
    fun reportAppOpen(intent: Intent) {
        val isHotOpen = PdfAppInitializer.isLauncherExists()
        val position = if (intent.hasExtra(LANDING_NOTIFICATION_FROM)) {
            intent.getStringExtra(LANDING_NOTIFICATION_FROM).orEmpty().ifBlank { "other" }
        } else {
            "other"
        }

        ReportDataManager.reportData(
            "app_open",
            mapOf(
                "type" to if (isHotOpen) "hot_open" else "cold_open",
                "position" to position
            )
        )
    }

    /**
     * 上报通知点击事件
     */
    fun reportNotificationClick(intent: Intent) {
        if (!intent.hasExtra(LANDING_NOTIFICATION_FROM)) return
        TriggerCtrl.stopRepeatNotification()
        val notificationType = getNotificationType(intent)
        val notificationPosition = getNotificationPosition(intent)
        val notificationPriority = getNotificationPriority(intent)
        val eventId = getEventId(intent)
        val title = intent.getStringExtra(LANDING_NOTIFICATION_TITLE).orEmpty()
        val text = intent.getStringExtra(LANDING_NOTIFICATION_CONTENT).orEmpty()
        val fromBackground = PdfAppInitializer.isLauncherExists()

        // 上报 Notific_Click
        ReportDataManager.reportData(
            "Notific_Click",
            mapOf(
                "Notific_Type" to notificationType,
                "Notific_Position" to notificationPosition,
                "Notific_Priority" to notificationPriority,
                "event_id" to eventId,
                "title" to title,
                "text" to text,
                "from_background" to fromBackground
            )
        )

        // 上报 Notific_Enter
        ReportDataManager.reportData(
            "Notific_Enter",
            mapOf(
                "Notific_Type" to notificationType,
                "Notific_Position" to notificationPosition,
                "Notific_Priority" to notificationPriority,
                "event_id" to eventId,
                "title" to title,
                "text" to text
            )
        )
    }

    /**
     * 上报分组参数（从远程获取 Grouping 拼接）
     * @param extraParams 额外参数，如 isShowSplash
     */
    fun reportGrouping(extraParams: Map<String, String> = emptyMap()) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val key = "Grouping"
                ConfigRemoteManager.getString(key, "")?.takeIf {
                    it.isNotEmpty()
                }?.let {
                    val value = "${key}_${it}"
                    ReportDataManager.reportData(value, extraParams)
                    Firebase.remoteConfig.setCustomSignals(
                        CustomSignals.Builder()
                            .put(key, value)
                            .build()
                    )
                    Log.d(TAG, "上报分组参数: $value, params=$extraParams")
                }
            } catch (e: Exception) {
                Log.e(TAG, "上报分组参数异常", e)
            }
        }
    }

    // ==================== 辅助方法 ====================

    private fun getNotificationType(intent: Intent): Int {
        return when (intent.getStringExtra(LANDING_NOTIFICATION_FROM).orEmpty()) {
            CheckCtrl.NotificationType.UNLOCK.string -> 1
            CheckCtrl.NotificationType.BACKGROUND.string -> 1
            CheckCtrl.NotificationType.KEEPALIVE.string -> 1
            CheckCtrl.NotificationType.FCM.string -> 3
            CheckCtrl.NotificationType.RESIDENT.string -> 4
            CheckCtrl.NotificationType.EARTHQUAKE.string -> 5
            else -> 4
        }
    }

    private fun getNotificationPosition(intent: Intent): Int {
        return when (intent.getStringExtra(LANDING_NOTIFICATION_FROM).orEmpty()) {
            CheckCtrl.NotificationType.RESIDENT.string -> 2
            else -> 1
        }
    }

    private fun getNotificationPriority(intent: Intent): String {
        return when (intent.getStringExtra(LANDING_NOTIFICATION_FROM).orEmpty()) {
            CheckCtrl.NotificationType.RESIDENT.string -> "PRIORITY_DEFAULT"
            else -> "PRIORITY_MAX"
        }
    }

    private fun getEventId(intent: Intent): String {
        return when (intent.getStringExtra(LANDING_NOTIFICATION_FROM).orEmpty()) {
            CheckCtrl.NotificationType.RESIDENT.string -> "permanent"
            else -> "customer_general_style"
        }
    }
}
