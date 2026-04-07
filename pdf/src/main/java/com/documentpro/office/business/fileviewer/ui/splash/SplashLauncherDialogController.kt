package com.documentpro.office.business.fileviewer.ui.splash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.documentpro.office.business.fileviewer.PdfAppInitializer
import com.documentpro.office.business.fileviewer.dialog.IntroductionDialog
import com.documentpro.office.business.fileviewer.dialog.IntroductionDialog2
import com.documentpro.office.business.fileviewer.utils.DefaultLauncherController
import com.documentpro.office.business.fileviewer.utils.LauncherApplyTrack
import io.docview.push.controller.BgNotiInterceptController
import kotlinx.coroutines.launch

/**
 * 开屏页弹框控制器
 *
 * 负责管理开屏页的弹框展示和默认桌面设置流程：
 * - IntroductionDialog（首次启动弹框）
 * - IntroductionDialog2（二次弹框）
 * - 默认桌面权限申请
 * - 启动 Launcher
 */
class SplashLauncherDialogController(
    private val activity: AppCompatActivity,
    private val intentExtras: Bundle?
) {
    companion object {
        private const val TAG = BusinessSplashScreenActivity.TAG
    }

    private val defaultLauncherController = DefaultLauncherController(activity, activity)

    /**
     * 是否首次请求默认桌面
     */
    fun isFirstRequest(): Boolean = defaultLauncherController.isFirstRequest()

    /**
     * 是否已是默认桌面
     */
    fun isDefaultLauncher(): Boolean = defaultLauncherController.isDefaultLauncher()

    /**
     * 处理页面跳转逻辑
     *
     * 流程：
     * 1. 首次启动 -> IntroductionDialog -> 同意则请求默认桌面 -> 拒绝则 IntroductionDialog2
     * 2. 非首次启动 -> 直接启动 Launcher
     *
     * @param activity 当前 Activity
     * @param onComplete 完成回调，参数为是否需要 finish Activity
     */
    suspend fun handleNavigation(
        activity: AppCompatActivity,
        onComplete: () -> Unit
    ) {
        if (isFirstRequest()) {
            handleFirstLaunch(activity, onComplete)
        } else {
            Log.d(TAG, "非首次启动，直接启动 Launcher")
            goLauncher(onComplete)
        }
    }

    /**
     * 处理首次启动
     * IntroductionDialog 同意 -> 请求默认桌面 -> 成功则跳转，拒绝则 IntroductionDialog2
     * IntroductionDialog 拒绝 -> IntroductionDialog2 -> 跳转 Launcher
     */
    private suspend fun handleFirstLaunch(
        activity: AppCompatActivity,
        onComplete: () -> Unit
    ) {
        val shouldContinue = IntroductionDialog.show(activity)
        if (shouldContinue) {
            Log.d(TAG, "首次启动，用户同意，请求设置默认桌面")
            LauncherApplyTrack.First_Launcher()
            requestDefaultLauncherWithFallback(activity, onComplete)
        } else {
            Log.d(TAG, "首次启动，用户拒绝，展示 IntroductionDialog2")
            showIntroductionDialog2AndGo(activity, onComplete)
        }
    }

    /**
     * 请求默认桌面权限，拒绝则展示 IntroductionDialog2
     */
    private fun requestDefaultLauncherWithFallback(
        activity: AppCompatActivity,
        onComplete: () -> Unit
    ) {
        defaultLauncherController.requestDefaultLauncher { isSuccess ->
            Log.d(TAG, "设置默认桌面结果: $isSuccess")
            if (isSuccess) {
                goLauncher(onComplete)
            } else {
                // 用户拒绝权限，展示 IntroductionDialog2
                activity.lifecycleScope.launch {
                    showIntroductionDialog2AndGo(activity, onComplete)
                }
            }
        }
    }

    /**
     * 展示 IntroductionDialog2 并处理结果
     */
    private suspend fun showIntroductionDialog2AndGo(
        activity: AppCompatActivity,
        onComplete: () -> Unit
    ) {
        val shouldContinue = IntroductionDialog2.show(activity)
        if (shouldContinue) {
            LauncherApplyTrack.Second_Launcher()
            requestDefaultLauncherAndGo(onComplete)
        } else {
            goLauncher(onComplete)
        }
    }

    /**
     * 请求默认桌面权限并跳转
     */
    private fun requestDefaultLauncherAndGo(onComplete: () -> Unit) {
        defaultLauncherController.requestDefaultLauncher { isSuccess ->
            Log.d(TAG, "设置默认桌面结果: $isSuccess")
            goLauncher(onComplete)
        }
    }

    /**
     * 跳转到 Launcher
     */
    private fun goLauncher(onComplete: () -> Unit) {
        BgNotiInterceptController.clearIntercept()

        // 非默认桌面时，先启动 Launcher，再启动 Loading Activity 覆盖白屏
        if (!isDefaultLauncher()) {
            // 1. 先启动 Launcher
            defaultLauncherController.startLauncherActivity(intentExtras)

            // 2. 立即启动 Loading Activity（会显示在 Launcher 上面，覆盖白屏）
            if (!PdfAppInitializer.isLauncherExists()) {
                val loadingIntent = Intent(activity, LauncherLoadingActivity::class.java)
                activity.startActivity(loadingIntent)
                activity.overridePendingTransition(0, 0)
            }

            onComplete()
        } else {
            defaultLauncherController.startLauncherActivity(intentExtras)
            onComplete()
        }
    }
}

