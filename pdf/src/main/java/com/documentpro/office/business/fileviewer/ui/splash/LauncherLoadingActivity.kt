package com.documentpro.office.business.fileviewer.ui.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.documentpro.office.business.fileviewer.R
import kotlin.math.hypot

/**
 * Launcher 加载遮罩 Activity
 * 
 * 用于在非默认桌面场景下，覆盖 Launcher 的白屏问题。
 * 当 Launcher 初始化完成后，会发送广播通知本 Activity 关闭。
 */
class LauncherLoadingActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "LauncherLoading"
        const val ACTION_LAUNCHER_READY = "com.launcher.LAUNCHER_READY"
        private const val TIMEOUT_MS = 5000L
    }
    
    private var isFinishing = false
    
    private val launcherReadyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Received LAUNCHER_READY broadcast, finishing loading activity")
            finishLoading()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher_loading)
        
        // 注册广播接收器
        registerReceiver(
            launcherReadyReceiver,
            IntentFilter(ACTION_LAUNCHER_READY),
            RECEIVER_NOT_EXPORTED
        )
        
        // 超时保护：5秒后强制关闭
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Timeout, force finishing loading activity")
            finishLoading()
        }, TIMEOUT_MS)
    }
    
    private fun finishLoading() {
        if (isFinishing) return
        isFinishing = true
        
        try {
            unregisterReceiver(launcherReadyReceiver)
        } catch (e: Exception) {
            // ignore
        }
        
        // 波纹缩小动画关闭
        playCircularRevealExit()
    }
    
    private fun playCircularRevealExit() {
        val rootView = findViewById<View>(R.id.root_container)
        if (rootView == null || !rootView.isAttachedToWindow) {
            finish()
            overridePendingTransition(0, 0)
            return
        }
        
        // 从屏幕中心开始缩小
        val centerX = rootView.width / 2
        val centerY = rootView.height / 2
        val startRadius = hypot(centerX.toDouble(), centerY.toDouble()).toFloat()
        
        try {
            val animator = ViewAnimationUtils.createCircularReveal(
                rootView,
                centerX,
                centerY,
                startRadius,
                0f
            )
            animator.duration = 350
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    rootView.visibility = View.INVISIBLE
                    finish()
                    overridePendingTransition(0, 0)
                }
            })
            animator.start()
        } catch (e: Exception) {
            Log.e(TAG, "CircularReveal animation failed", e)
            finish()
            overridePendingTransition(0, 0)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(launcherReadyReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 禁止返回键
    }
}
