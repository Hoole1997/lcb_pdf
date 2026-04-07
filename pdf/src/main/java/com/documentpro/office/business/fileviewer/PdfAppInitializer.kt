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
import com.documentpro.office.business.fileviewer.ui.language.LanguageActivity
import com.documentpro.office.business.fileviewer.ui.shortcut.UninstallOptionActivity
import com.documentpro.office.business.fileviewer.ui.shortcut.UninstallPromptActivity
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

object PdfAppInitializer {

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
    }
}
