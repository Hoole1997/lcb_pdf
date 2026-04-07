package com.documentpro.office.business.fileviewer

//import com.game.hachisdk.common.HCApplication
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ProcessUtils
import com.documentpro.office.business.fileviewer.ui.language.LanguageActivity
import com.documentpro.office.business.fileviewer.ui.shortcut.UninstallOptionActivity
import com.documentpro.office.business.fileviewer.ui.shortcut.UninstallPromptActivity
import com.documentpro.office.business.fileviewer.ui.splash.BusinessSplashScreenActivity
import com.documentpro.office.business.fileviewer.ui.splash.GuideActivity
import com.documentpro.office.business.fileviewer.utils.BusinessSPConfig
import com.documentpro.office.business.fileviewer.utils.BusinessStorageUtils
import com.documentpro.office.business.fileviewer.utils.BusinessSplashForegroundController
import com.documentpro.office.business.fileviewer.utils.BusinessShortcutManager
import com.google.mlkit.common.sdkinternal.MlKitContext
import io.docview.push.utils.Logger
import net.corekit.metrics.log.MetricsLogger
import net.corekit.core.log.CoreLogger

/**
 * @deprecated 请使用 [PdfAppInitializer] 来初始化 PDF 模块。
 * 此类保留仅用于向后兼容，新代码应使用 [PdfAppInitializer]。
 */
@Deprecated(
    message = "请使用 PdfAppInitializer 来初始化 PDF 模块",
    replaceWith = ReplaceWith("PdfAppInitializer", "com.documentpro.office.business.fileviewer.PdfAppInitializer")
)
class BusinessApp : Application() {

    companion object {

        var appLogEnable: Boolean
            get() = PdfAppRuntime.appLogEnable
            set(value) {
                PdfAppRuntime.appLogEnable = value
            }

    }
    private var isAppInForeground = false
    private var isFirstLaunch = true

    override fun attachBaseContext(base: Context?) {
        MetricsLogger.enableLog(appLogEnable)
        CoreLogger.setLogEnabled(appLogEnable)
        Logger.setLogEnabled(appLogEnable)
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // 初始化存储工具类
        BusinessStorageUtils.init(this)
        // 初始化首次启动时间
        BusinessSPConfig.initFirstLaunchTimeIfNeeded()
        // 初始化 Firebase Remote Config
        if (ProcessUtils.isMainProcess()) {
            registerAppStatus()
            BusinessShortcutManager.setAppShortcuts(this)
        }
        MlKitContext.initializeIfNeeded(this)
    }


    private fun registerAppStatus() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    private inner class AppLifecycleObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            if (!isAppInForeground) {
                isAppInForeground = true

                if (!isFirstLaunch ) {
                    if(ActivityUtils.isActivityExistsInStack(BusinessSplashScreenActivity::class.java) ||
                        (ActivityUtils.isActivityExistsInStack(LanguageActivity::class.java) && !BusinessSPConfig.isInitLanguage()) ||
                        ActivityUtils.isActivityExistsInStack(GuideActivity::class.java) ||
                        ActivityUtils.isActivityExistsInStack(UninstallPromptActivity::class.java) ||
                        ActivityUtils.isActivityExistsInStack(UninstallOptionActivity::class.java)
                        ){
                        return
                    }
                    // 闪屏页前台拉起控制器判断是否允许拉起
                    if (!BusinessSplashForegroundController.shouldLaunchSplash()) {
                        return
                    }
                    startSplashActivity()
                } else {
                    isFirstLaunch = false
                }
            }
        }

        fun startSplashActivity() {
            try {
                val intent = Intent(this@BusinessApp, BusinessSplashScreenActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            if (isAppInForeground) {
                isAppInForeground = false
            }
        }
    }

}
