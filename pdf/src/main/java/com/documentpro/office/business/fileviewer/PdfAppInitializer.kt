package com.documentpro.office.business.fileviewer

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.common.bill.ads.log.AdLogger
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ProcessUtils
import com.documentpro.office.business.fileviewer.ad.PdfAdInitializer
import com.documentpro.office.business.fileviewer.ad.HomePlacementManager
import com.documentpro.office.business.fileviewer.ui.language.LanguageActivity
import com.documentpro.office.business.fileviewer.ui.shortcut.UninstallOptionActivity
import com.documentpro.office.business.fileviewer.ui.shortcut.UninstallPromptActivity
import com.documentpro.office.business.fileviewer.ui.splash.BusinessSplashScreenActivity
import com.documentpro.office.business.fileviewer.ui.splash.GuideActivity
import com.documentpro.office.business.fileviewer.utils.BusinessSPConfig
import com.documentpro.office.business.fileviewer.utils.BusinessStorageUtils
import com.documentpro.office.business.fileviewer.utils.BusinessSplashForegroundController
import com.google.mlkit.common.sdkinternal.MlKitContext
import io.docview.push.utils.Logger
import net.corekit.core.ext.isDefaultLauncher
import net.corekit.metrics.log.MetricsLogger
import net.corekit.core.log.CoreLogger
import net.corekit.core.controller.ChannelUserController
import net.corekit.core.utils.RemoteConfigParams

/**
 * PDF 模块应用初始化工具类
 * 封装了原 BusinessApp 的初始化逻辑，供宿主应用（如 LawnchairApp）调用
 */
object PdfAppInitializer {
    private var isAppInForeground = false
    private var isFirstLaunch = true

    /**
     * Launcher 重启回调，由宿主应用（如 LawnchairApp）设置
     */
    private var launcherRestartCallback: ((Context) -> Unit)? = null

    /**
     * 检查 Launcher 首页是否存在的回调
     */
    private var isLauncherExistsCallback: (() -> Boolean)? = null

    /**
     * 设置 Launcher 重启回调
     * @param callback 重启回调函数，接收 Context 参数
     */
    fun setLauncherRestartCallback(callback: (Context) -> Unit) {
        launcherRestartCallback = callback
    }

    /**
     * 设置检查 Launcher 首页是否存在的回调
     * @param callback 检查回调函数，返回 Boolean
     */
    fun setLauncherExistsCallback(callback: () -> Boolean) {
        isLauncherExistsCallback = callback
    }

    /**
     * 检查 Launcher 首页是否存在
     * @return true 表示存在，false 表示不存在
     */
    fun isLauncherExists(): Boolean {
        return isLauncherExistsCallback?.invoke() ?: false
    }

    /**
     * 重启 Launcher
     * 通过回调调用宿主应用的重启逻辑
     * @param context Context 上下文
     */
    fun restartLauncher(context: Context) {
        launcherRestartCallback?.invoke(context)
    }


    /**
     * 在 attachBaseContext 中调用，用于设置日志启用状态
     * @param base Context
     * @param isLocalFlavor 是否为 local flavor（用于决定是否启用日志）
     */
    fun attachBaseContext(base: Context, isLocalFlavor: Boolean) {
        val appLogEnable = isLocalFlavor
        PdfAppRuntime.appLogEnable = appLogEnable
        AdLogger.setLogEnabled(appLogEnable)
        MetricsLogger.enableLog(appLogEnable)
        CoreLogger.setLogEnabled(appLogEnable)
        Logger.setLogEnabled(appLogEnable)

        ChannelUserController.setDefaultChannel(BuildConfig.DEFAULT_USER_CHANNEL)
    }

    /**
     * 在 onCreate 中调用，用于初始化 PDF 模块相关组件
     * @param application Application 实例
     */
    fun onCreate(application: Application) {
        // 设置夜间模式为关闭（强制日间模式）
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // 初始化存储工具类
        BusinessStorageUtils.init(application)

        // 初始化首次启动时间
        BusinessSPConfig.initFirstLaunchTimeIfNeeded()

        // 仅在主进程中初始化
        if (ProcessUtils.isMainProcess()) {
//            registerAppStatus(application)
            PdfAdInitializer.initialize(application)
        }

        // 初始化 ML Kit
        MlKitContext.initializeIfNeeded(application)

        // 初始化主页广告位配置
        HomePlacementManager.initialize(application)
    }

    /**
     * 注册应用生命周期观察者，用于处理前台拉起闪屏页的逻辑
     */
    private fun registerAppStatus(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                if (!isAppInForeground) {
                    isAppInForeground = true

                    if (!isFirstLaunch) {
                        if(application.isDefaultLauncher())
                            return
                        if(!RemoteConfigParams.isShowLoadingPageWhenBack)
                            return
                        // 检查是否已有相关 Activity 在栈中，如果有则不启动闪屏页
                        if (ActivityUtils.isActivityExistsInStack(BusinessSplashScreenActivity::class.java) ||
                            (ActivityUtils.isActivityExistsInStack(LanguageActivity::class.java) && !BusinessSPConfig.isInitLanguage()) ||
                            ActivityUtils.isActivityExistsInStack(GuideActivity::class.java) ||
                            ActivityUtils.isActivityExistsInStack(UninstallPromptActivity::class.java) ||
                            ActivityUtils.isActivityExistsInStack(UninstallOptionActivity::class.java)
                        ) {
                            return
                        }

                        // 闪屏页前台拉起控制器判断是否允许拉起
                        if (!BusinessSplashForegroundController.shouldLaunchSplash()) {
                            return
                        }

                        startSplashActivity(application)
                    } else {
                        isFirstLaunch = false
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                if (isAppInForeground) {
                    isAppInForeground = false
                }
            }
        })
    }

    /**
     * 启动闪屏页 Activity
     */
    private fun startSplashActivity(application: Application) {
        try {
            val intent = Intent(application, BusinessSplashScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            application.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
