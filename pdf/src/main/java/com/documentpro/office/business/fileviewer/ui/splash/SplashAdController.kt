package com.documentpro.office.business.fileviewer.ui.splash

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.PreloadController
import com.android.common.bill.ads.ext.AdShowExt
import com.android.common.bill.ads.ext.CountdownConfig

/**
 * 开屏页广告控制器
 *
 * 负责管理开屏页的广告逻辑：
 * - AdMob SDK 初始化
 * - 开屏广告和插页广告的竞价
 * - 广告加载和显示
 */
class SplashAdController(private val activity: FragmentActivity) {

    companion object {
        private const val TAG = BusinessSplashScreenActivity.TAG
    }

    // 广告是否已加载
    var isAdLoaded = false
        private set


    // 广告加载回调
    var onAdLoaded: ((Boolean) -> Unit)? = null

    /**
     * 检查是否有全屏原生广告正在显示
     */
    val hasFullNativeShowing: Boolean
        get() = AdShowExt.isAnyInterstitialOrFullScreenNativeShowing()


    /**
     * 显示广告（带竞价逻辑）
     */
    suspend fun showAdWithBidding(onTick: ((Int) -> Unit)): Boolean {
        Log.d(TAG, "准备显示开屏广告")
        val adResult = AdShowExt.showAppOpenAd(
            activity = activity,
            onLoaded = { isSuccess ->
                PreloadController.preloadAll(activity)
                isAdLoaded = isSuccess
                onAdLoaded?.invoke(isSuccess)
            },
            countdown = CountdownConfig(
                seconds = 2,
                onTick = { remaining -> onTick(remaining) }
            ))

        return if (adResult is AdResult.Success) {
            Log.d(TAG, "广告显示成功")
            true
        } else {
            Log.d(TAG, "广告显示失败: ${(adResult as? AdResult.Failure)?.error?.message}")
            false
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        onAdLoaded = null
    }
}
