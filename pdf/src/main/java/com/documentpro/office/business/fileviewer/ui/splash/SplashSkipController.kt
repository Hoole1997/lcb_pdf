package com.documentpro.office.business.fileviewer.ui.splash

import android.content.Intent
import android.util.Log
import io.docview.push.controller.LandingCtrl
import kotlinx.coroutines.withTimeoutOrNull
import net.corekit.core.utils.ConfigRemoteManager
import net.corekit.core.utils.RemoteConfigParams

/**
 * 开屏跳过控制器
 *
 * 负责判断是否需要跳过开屏广告
 */
class SplashSkipController(
    private val intent: Intent?,
    private val reportController: SplashReportController
) {

    companion object {
        private const val TAG = BusinessSplashScreenActivity.TAG
        private const val CONFIG_KEY = "isShowSplash"
        private const val CONFIG_TIMEOUT = 1500L
    }

    // 是否需要展示广告（用于判断切后台埋点）
    var shouldShowAd: Boolean = false
        private set

    /**
     * 是否需要显示开屏广告
     *
     * 逻辑：
     * - 非首次启动 → 必须显示广告
     * - 首次启动 + 从通知进入 → 显示广告
     * - 首次启动 + 非通知进入 → 从在线参数获取 "isShowSplash"，超时后默认不显示
     *
     * @return true: 显示广告; false: 跳过广告
     */
    suspend fun shouldShowSplashAd(): Boolean {
        // 非首次启动，必须显示广告
        if (!BusinessSplashScreenActivity.isFirstLaunch) {
            Log.d(TAG, "非首次启动，必须显示广告")
            shouldShowAd = true
            return true
        }

        // 首次启动，从通知进入，显示广告
        if (isFromNotification()) {
            Log.d(TAG, "首次启动，从通知进入，显示广告")
            shouldShowAd = true
            return true
        }

        // 首次启动，非通知进入，从在线参数获取
        return try {
            val result = RemoteConfigParams.isShowSplash
            val showAd = result

            Log.d(TAG, "首次启动，在线参数 $CONFIG_KEY = $result, showAd = $showAd")
            // 上报是否展示开屏广告
            shouldShowAd = showAd
            showAd
        } catch (e: Exception) {
            Log.e(TAG, "首次启动，获取在线参数异常，默认显示广告", e)
            // 上报默认展示广告
            shouldShowAd = true
            true
        }
    }

    /**
     * 是否从通知进入
     */
    fun isFromNotification(): Boolean {
        return intent != null && LandingCtrl.isFromNotification(intent)
    }
}
