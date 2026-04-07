package com.documentpro.office.business.fileviewer.utils

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.SPUtils
import com.documentpro.office.business.fileviewer.ui.launcher.LauncherGuideActivity
import io.docview.push.controller.BgNotiInterceptController
import io.docview.push.utils.ResetCtrl
import net.corekit.core.ext.isDefaultLauncher

/**
 * 默认桌面（Launcher）权限控制器
 *
 * 用于请求将应用设置为默认桌面应用
 * 只在首次启动时请求一次
 */
class DefaultLauncherController(
    private val activity: ComponentActivity,
    private val lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "DefaultLauncherCtrl"
        private const val SP_KEY_FIRST_REQUEST = "default_launcher_first_request"
        private const val SP_KEY_PERMANENTLY_DENIED = "default_launcher_permanently_denied"

        private const val KEY_DEFAULT_LAUNCHER_REQUEST_COUNT = "overlay_default_launcher_count"
        private const val MAX_DEFAULT_LAUNCHER_REQUESTS_PER_DAY = 2

        /**
         * 检查是否已被永久拒绝
         */
        fun isPermanentlyDenied(): Boolean {
            return SPUtils.getInstance().getBoolean(SP_KEY_PERMANENTLY_DENIED, false)
        }

        /**
         * 重置永久拒绝状态（如需要重新请求时调用）
         */
        fun resetPermanentlyDenied() {
            SPUtils.getInstance().put(SP_KEY_PERMANENTLY_DENIED, false, true)
        }

        fun isDailyLimitReached(): Boolean {
            val currentCount = ResetCtrl.getInstance()
                .getIntValue(KEY_DEFAULT_LAUNCHER_REQUEST_COUNT, 0)
            return currentCount >= MAX_DEFAULT_LAUNCHER_REQUESTS_PER_DAY
        }

        fun incrementDailyCount(): Int {
            return ResetCtrl.getInstance().incrementIntValue(KEY_DEFAULT_LAUNCHER_REQUEST_COUNT)
        }
    }

    private var resultLauncher: ActivityResultLauncher<Intent>? = null
    private var onResultCallback: ((Boolean) -> Unit)? = null

    // 是否正在等待系统设置页面返回
    private var isWaitingForSettings = false

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        registerResultLauncher()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // 检查是否从系统设置页面返回
        checkPendingSettingsResult()
    }

    /**
     * 检查待处理的系统设置结果
     * 在 onResume 时调用，检查用户是否从设置页面返回
     */
    private fun checkPendingSettingsResult() {
        if (!isWaitingForSettings || onResultCallback == null) {
            return
        }

        isWaitingForSettings = false

        val isSuccess = isDefaultLauncher()
        Log.d(TAG, "从系统设置返回，检查结果: isSuccess=$isSuccess")

        if (isSuccess) {
            // 用户在设置中选择了本应用，重置永久拒绝状态
            resetPermanentlyDenied()
        }

        onResultCallback?.invoke(isSuccess)
        onResultCallback = null
    }

    /**
     * 注册 Activity Result Launcher
     */
    private fun registerResultLauncher() {
        resultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val isSuccess = isDefaultLauncher()
            Log.d(TAG, "设置默认桌面结果: isSuccess=$isSuccess, resultCode=${result.resultCode}")

            if (isSuccess) {
                // 设置成功，重置永久拒绝状态
                resetPermanentlyDenied()
                onResultCallback?.invoke(true)
            } else {
                // 设置失败或取消，标记为永久拒绝
                markAsPermanentlyDenied()
                onResultCallback?.invoke(false)
            }

            onResultCallback = null
        }
    }

    /**
     * 检查是否是首次请求
     */
    fun isFirstRequest(): Boolean {
        val boolean = SPUtils.getInstance().getBoolean(SP_KEY_FIRST_REQUEST, true)
        markAsRequested()
        return boolean
    }

    /**
     * 标记已请求过
     */
    private fun markAsRequested() {
        SPUtils.getInstance().put(SP_KEY_FIRST_REQUEST, false, true)
    }

    /**
     * 标记为永久拒绝
     */
    private fun markAsPermanentlyDenied() {
        SPUtils.getInstance().put(SP_KEY_PERMANENTLY_DENIED, true, true)
        Log.d(TAG, "已标记为永久拒绝")
    }

    /**
     * 检查当前应用是否已经是默认桌面
     */
    fun isDefaultLauncher(): Boolean {
        return activity.isDefaultLauncher()
    }

    /**
     * 请求设置为默认桌面
     *
     * @param onResult 回调：true=设置成功，false=设置失败或取消（永久拒绝时会自动跳转系统设置）
     */
    fun requestDefaultLauncher(onResult: (Boolean) -> Unit) {
        // 这里不需要弹框式拉起，所以标记永久拒绝直接拉起页面式
        markAsPermanentlyDenied()

        // 如果已经是默认桌面，直接返回成功
        if (isDefaultLauncher()) {
            Log.d(TAG, "已经是默认桌面，直接返回成功")
            onResult(true)
            return
        }

        if (isDailyLimitReached()) {
            val currentCount = ResetCtrl.getInstance()
                .getIntValue(KEY_DEFAULT_LAUNCHER_REQUEST_COUNT, 0)
            Log.d(
                TAG,
                "Skip request default launcher: daily limit reached " +
                    "($currentCount/$MAX_DEFAULT_LAUNCHER_REQUESTS_PER_DAY)"
            )
            onResult(false)
            return
        }

        val newCount = incrementDailyCount()
        Log.d(
            TAG,
            "Default launcher request count: $newCount/$MAX_DEFAULT_LAUNCHER_REQUESTS_PER_DAY"
        )

        // 如果已被永久拒绝，跳转到系统设置页面，等待用户返回后检查结果
        if (isPermanentlyDenied()) {
            Log.d(TAG, "用户已永久拒绝，跳转系统设置页面")
            onResultCallback = onResult
            isWaitingForSettings = true
            BusinessSplashForegroundController.markNextIntercept()
            BgNotiInterceptController.markNextIntercept()
            openDefaultAppsSettings()
            return
        }

        onResultCallback = onResult

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 RoleManager
                val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                        resultLauncher?.launch(intent)
                        Log.d(TAG, "使用 RoleManager 请求默认桌面权限")
                    } else {
                        Log.d(TAG, "已持有 ROLE_HOME")
                        onResult(true)
                        onResultCallback = null
                    }
                } else {
                    // RoleManager 不可用，使用传统方式
                    requestDefaultLauncherLegacy(onResult)
                }
            } else {
                // Android 9 及以下使用传统方式
                requestDefaultLauncherLegacy(onResult)
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求默认桌面异常", e)
            onResult(false)
            onResultCallback = null
        }
    }

    /**
     * 传统方式请求默认桌面（Android 9 及以下）
     */
    private fun requestDefaultLauncherLegacy(onResult: (Boolean) -> Unit) {
        try {
            // 先清除当前默认桌面设置
            activity.packageManager.clearPackagePreferredActivities(activity.packageName)

            // 触发桌面选择器
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val chooserIntent = Intent.createChooser(intent, "选择默认桌面")
            resultLauncher?.launch(chooserIntent)
            Log.d(TAG, "使用传统方式请求默认桌面权限")
        } catch (e: Exception) {
            Log.e(TAG, "传统方式请求默认桌面异常", e)
            onResult(false)
            onResultCallback = null
        }
    }

    /**
     * 跳转到系统默认主屏幕应用设置页面
     * 用于永久拒绝后让用户手动设置默认桌面
     */
    fun openDefaultAppsSettings() {
        try {
            val intent = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    // Android 9.0+ 直接跳转到默认主屏幕应用设置
                    Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                    // Android 7.0-8.1 跳转到默认应用设置
                    Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                }
                else -> {
                    // 低版本跳转到应用设置
                    Intent(android.provider.Settings.ACTION_SETTINGS)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            LauncherGuideActivity.start(activity)
            Log.d(TAG, "跳转到系统默认主屏幕应用设置页面")
        } catch (e: Exception) {
            Log.e(TAG, "跳转系统设置异常，尝试降级跳转", e)
            tryFallbackSettings()
        }
    }

    /**
     * 降级跳转设置页面
     */
    private fun tryFallbackSettings() {
        try {
            // 先尝试跳转默认应用设置
            val fallbackIntent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(fallbackIntent)
        } catch (e: Exception) {
            try {
                // 最后降级到系统设置主页
                val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(settingsIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "所有设置页面跳转都失败", e2)
            }
        }
    }

    /**
     * 启动 Launcher Activity
     *
     * @param extras 可选的 extras，会透传给 Launcher
     */
    fun startLauncherActivity(extras: android.os.Bundle? = null) {
        try {
            val intent = if (isDefaultLauncher()) {
                // 已是默认桌面，使用隐式 Intent
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    extras?.let { putExtras(it) }
                }
            } else {
                // 非默认桌面，使用显式 Intent 直接启动 LawnchairLauncher
                Intent().apply {
                    setClassName(activity.packageName, "app.lawnchair.LawnchairLauncher")
                    // 使用 REORDER_TO_FRONT 避免重新创建，NO_ANIMATION 避免闪烁
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or 
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    extras?.let { putExtras(it) }
                }
            }
            activity.startActivity(intent)
            // 禁用当前 Activity 的退出动画
            activity.overridePendingTransition(0, 0)
            Log.d(TAG, "启动 Launcher, isDefault=${isDefaultLauncher()}, extras=$extras")
        } catch (e: Exception) {
            Log.e(TAG, "启动 Launcher 异常", e)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        resultLauncher = null
        onResultCallback = null
        isWaitingForSettings = false
        lifecycleOwner.lifecycle.removeObserver(this)
    }
}
