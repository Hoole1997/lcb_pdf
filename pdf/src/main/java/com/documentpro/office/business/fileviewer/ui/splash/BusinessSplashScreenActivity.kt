package com.documentpro.office.business.fileviewer.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.SPUtils
import com.documentpro.office.business.fileviewer.databinding.ActivitySplashScreenBinding
import com.documentpro.office.business.fileviewer.utils.BusinessPermissionDialogUtils
import io.docview.push.service.KeepAliveServiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.leolin.shortcutbadger.ShortcutBadger
import com.android.common.bill.ads.util.GoogleMobileAdsConsentManager
import com.documentpro.office.business.fileviewer.R
import io.docview.push.controller.TriggerCtrl
import net.corekit.core.ext.canSendNotification
import kotlin.coroutines.resume

/**
 * 开屏页 Activity（重构版）
 *
 * 职责：
 * - 协调各个控制器完成开屏流程
 * - 处理页面跳转逻辑
 *
 * 控制器：
 * - SplashAnimationController: 动画控制
 * - SplashAdController: 广告控制
 * - DemoFileCopyController: Demo文件拷贝
 * - SplashReportController: 数据上报
 * - DefaultLauncherController: 桌面权限申请
 */
@SuppressLint("CustomSplashScreen")
class BusinessSplashScreenActivity : AppCompatActivity() {

    companion object {
        const val TAG = "开屏页"
        private const val PROGRESS_DURATION = 10000L
        private const val TIMEOUT_DURATION = 15000L
        private const val MIN_DISPLAY_TIME = 2000L

        // 启动时间（保持兼容）
        var launchTime = 0L

        var isFirstLaunch: Boolean
            get() = SPUtils.getInstance().getBoolean("is_first_launch", true)
            set(value) = SPUtils.getInstance().put("is_first_launch", value, true)

        fun launch(context: Context) {
            context.startActivity(Intent(context, BusinessSplashScreenActivity::class.java))
        }
    }

    private lateinit var binding: ActivitySplashScreenBinding

    // 控制器
    private lateinit var animationController: SplashAnimationController
    private lateinit var adController: SplashAdController
    private lateinit var demoFileCopyController: DemoFileCopyController
    private lateinit var reportController: SplashReportController
    private lateinit var launcherDialogController: SplashLauncherDialogController
    private lateinit var skipController: SplashSkipController
    private var splashFlowJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 必须在 super.onCreate 之前初始化 ActivityResultLauncher
        launcherDialogController = SplashLauncherDialogController(this, intent.extras)

        super.onCreate(savedInstanceState)

        initWindow()
        initBinding()
        initControllers()
        initBackPressedCallback()

        // 上报
        reportController.reportGrouping()
        reportController.reportLifecycle("onCreate")
        reportController.reportPageShow()
        reportController.reportAppOpen(intent)
        reportController.reportNotificationClick(intent)

        ShortcutBadger.removeCount(this)

        // 启动主流程
        startSplashFlow()
    }

    // ==================== 初始化 ====================

    private fun initWindow() {
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        BarUtils.setStatusBarVisibility(this, false)
        BarUtils.setNavBarVisibility(this, false)
    }

    private fun initBinding() {
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvDisplay.text = getString(com.android.common.bill.R.string.splash_ad_loading)
    }

    private fun initControllers() {
        animationController = SplashAnimationController(
            contentLayout = binding.llContent,
            progressBar = binding.progressBar,
            progressText = binding.tvDisplay
        )
        adController = SplashAdController(this)
        demoFileCopyController = DemoFileCopyController(this)
        reportController = SplashReportController()
        skipController = SplashSkipController(intent, reportController)

        // 设置初始状态
        animationController.ensureInitialState()

        // 广告加载回调
        adController.onAdLoaded = { isSuccess ->
            reportController.reportExitEvent()
            if (isSuccess) {
                animationController.completeProgress()
            }
        }
    }

    private fun initBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                reportController.reportUserBack()
            }
        })
    }

    // ==================== 主流程 ====================

    private fun startSplashFlow() {
        splashFlowJob?.cancel()
        splashFlowJob = lifecycleScope.launch {
            runCatching {
                // 1. 异步复制Demo文件（不阻塞）
                launch(Dispatchers.IO) {
                    demoFileCopyController.copyDemoFiles()
                }

                // 2. 延迟确保布局稳定
                delay(300)

                // 3. 播放波浪动画
                animationController.playWaveAnimation()

                // 4. 延迟
                delay(100)

                // 5. 显示进度条（保持在0%）
                animationController.showProgressBar()

                // 6. 收集UMP同意
                GoogleMobileAdsConsentManager.getInstance(this@BusinessSplashScreenActivity).gatherConsent(this@BusinessSplashScreenActivity)
                    .let {
                        if (!it) {
                            finish()
                            return@runCatching
                        }
                    }

                // 7. 申请通知权限
                requestNotificationPermission()

                if (canSendNotification()) {
                    KeepAliveServiceManager.startKeepAliveService(this@BusinessSplashScreenActivity)
                }

                // 9. 记录启动时间
                reportController.recordLaunchTime()
                launchTime = reportController.getLaunchTime()

                animationController.playProgressAnimation(PROGRESS_DURATION)

                // 11. 判断是否需要显示广告
                if (!skipController.shouldShowSplashAd()) {
                    Log.d(TAG, "跳过广告")
                    animationController.completeProgress()
                    onFlowComplete()
                    return@runCatching
                }

                awaitAdWithTimeout()

                // 13. 完成动画，跳转下一页
                onFlowComplete()

            }.onFailure { e ->
                Log.e(TAG, "Splash flow error", e)
                onFlowComplete()
            }
        }
    }

    /**
     * 等待广告加载完成或超时
     */
    private suspend fun awaitAdWithTimeout() {
        val adJob = lifecycleScope.async {
            adController.showAdWithBidding(onTick = {
                updateCountdownUI(it)
            })
        }

        val timeoutJob = lifecycleScope.async {
            delay(TIMEOUT_DURATION)
        }

        val isTimeout = select<Boolean> {
            adJob.onAwait { false }
            timeoutJob.onAwait { true }
        }

        if (isTimeout) {
            // 超时但有广告在显示，继续等待
            if (adController.isAdLoaded || adController.hasFullNativeShowing) {
                Log.d(TAG, "超时但有广告，继续等待")
                runCatching { adJob.await() }
            } else {
                Log.d(TAG, "超时且无广告，继续流程")
            }
        }
    }

    /**
     * 更新倒计时 UI
     * @param remaining 剩余秒数
     */
    private fun updateCountdownUI(remaining: Int) {
        val baseText = getString( com.android.common.bill.R.string.splash_ad_loading)
        binding.tvDisplay.text = "$baseText ${remaining}s"
    }

    /**
     * 流程完成，跳转下一页
     */
    private suspend fun onFlowComplete() {
        // 确保最少显示时间
        val elapsed = System.currentTimeMillis() - reportController.getLaunchTime()
        if (elapsed < MIN_DISPLAY_TIME) {
            delay(MIN_DISPLAY_TIME - elapsed)
        }

        withContext(Dispatchers.Main) {
            animationController.completeProgress()
            navigateToNextPage()
        }
    }

    // ==================== 权限申请 ====================

    private suspend fun requestNotificationPermission() {
        suspendCancellableCoroutine<Unit> { continuation ->
            val resumed = java.util.concurrent.atomic.AtomicBoolean(false)

            fun safeResume() {
                if (resumed.compareAndSet(false, true)) {
                    runCatching { continuation.resume(Unit) }
                }
            }

            runCatching {
                BusinessPermissionDialogUtils.showPostPermissionDialog(
                    context = this,
                    needDialog = true,
                    needStartPermissionPage = false,
                    pushRequestPosition = "Appstart",
                    onGranted = { _, _ -> safeResume() }
                )
                continuation.invokeOnCancellation { resumed.set(true) }
            }.onFailure { safeResume() }
        }
    }

    // ==================== 页面跳转 ====================

    /**
     * 跳转到下一页
     */
    private suspend fun navigateToNextPage() {
        if (isFinishing || isDestroyed) return

        runCatching {
            launcherDialogController.handleNavigation(this) {
                finish()
            }
        }.onFailure { e ->
            Log.e(TAG, "页面跳转异常", e)
            finish()
        }
    }


    // ==================== 生命周期 ====================

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: 通知重新打开，销毁当前实例并重建")
        reportController.reportLifecycle("onNewIntent")
        reportController.reportNotificationClick(intent)

        // 取消当前流程，销毁当前实例，启动新实例走完整 onCreate 流程
        splashFlowJob?.cancel()
        finish()
        startActivity(Intent(this, BusinessSplashScreenActivity::class.java).apply {
            intent.extras?.let { putExtras(it) }
        })
    }

    override fun onResume() {
        super.onResume()
        reportController.reportLifecycle("onResume")
    }

    override fun onPause() {
        super.onPause()
        reportController.reportLifecycle("onPause")
        // 广告加载中用户切后台埋点（仅在需要展示广告且广告未返回结果时，launchTime > 0 表示已过通知权限阶段）
        if (skipController.shouldShowAd && !adController.isAdLoaded && launchTime > 0) {
            reportController.reportAdLoadingUserBackground()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reportController.reportLifecycle("onDestroy")
        // 标记非首次启动
        if (isFirstLaunch) {
            isFirstLaunch = false
        }
        splashFlowJob?.cancel()
        animationController.release()
        adController.release()
    }
}
