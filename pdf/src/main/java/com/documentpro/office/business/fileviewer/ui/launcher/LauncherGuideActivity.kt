package com.documentpro.office.business.fileviewer.ui.launcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.ActivityLauncherGuideBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 默认桌面选择引导页
 *
 * 用于覆盖在系统默认桌面选择弹框上方，引导用户选择本应用
 * 配置为独立任务栈，确保显示在系统弹框之上
 */
class LauncherGuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherGuideBinding
    
    // 动画时长
    private val animDuration = 300L
    // 自动关闭延迟
    private val autoCloseDelay = 3000L
    // 是否正在关闭动画中
    private var isClosing = false

    companion object {
        private const val TAG = "LauncherGuideActivity"

        /**
         * 启动引导页
         */
        @JvmStatic
        fun start(context: Context) {
            MainScope().launch {
                delay(300L)
                val intent = Intent(context, LauncherGuideActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置窗口属性，使其显示在系统弹框之上
        setupWindow()

        binding = ActivityLauncherGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        
        // 播放入场动画
        playEnterAnimation()

        // 埋点：引导页显示
        BusinessPointLog.logEvent("Launcher_Guide_Show", mapOf())
        
        // 3秒后自动关闭
        scheduleAutoClose()
    }

    /**
     * 设置窗口属性
     */
    private fun setupWindow() {
        // 设置窗口背景透明
        window.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun initView() {
        // 设置标题文本，解析 HTML 标签
        // 注意：需要在代码中设置默认文本颜色，否则会覆盖 HTML 颜色
        binding.tvTitle.setTextColor(Color.parseColor("#333333"))
        val htmlText = getString(R.string.default_app_dialog_title)
        val spannedText = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.tvTitle.text = spannedText
        
        // 初始状态：内容区域在屏幕下方（不可见）
        binding.contentLL.post {
            binding.contentLL.translationY = binding.contentLL.height.toFloat()
        }

        // 点击取消图标关闭
        binding.ivCancel.setOnClickListener {
            closeWithAnimation()
        }

        // 点击卡片区域（可选：添加点击反馈）
        binding.cardApp.setOnClickListener {
            // 用户可能会点击这里，但实际选择需要在系统弹框中操作
            // 这里只是视觉引导
        }
    }
    
    /**
     * 播放入场动画：内容从下往上滑入
     */
    private fun playEnterAnimation() {
        binding.contentLL.post {
            // 内容区域从下往上滑入
            ObjectAnimator.ofFloat(binding.contentLL, View.TRANSLATION_Y, binding.contentLL.height.toFloat(), 0f).apply {
                duration = animDuration
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }
    
    /**
     * 播放退场动画：内容从上往下滑出
     */
    private fun playExitAnimation(onEnd: () -> Unit) {
        // 内容区域从上往下滑出
        ObjectAnimator.ofFloat(binding.contentLL, View.TRANSLATION_Y, 0f, binding.contentLL.height.toFloat()).apply {
            duration = animDuration
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            start()
        }
    }
    
    /**
     * 延迟自动关闭
     */
    private fun scheduleAutoClose() {
        MainScope().launch {
            delay(autoCloseDelay)
            if (!isFinishing && !isDestroyed && !isClosing) {
                closeWithAnimation()
            }
        }
    }
    
    /**
     * 带动画关闭页面
     */
    private fun closeWithAnimation() {
        if (isClosing) return
        isClosing = true
        
        playExitAnimation {
            finish()
            // 禁用默认的 Activity 转场动画
            overridePendingTransition(0, 0)
        }
    }

    override fun onBackPressed() {
        // 禁止返回键关闭，引导用户完成选择
        // 如果需要允许返回键关闭，取消下面的注释
        closeWithAnimation()
    }

    /**
     * 判断触摸点是否在内容区域内
     */
    private fun isTouchInsideContent(x: Float, y: Float): Boolean {
        val rect = Rect()
        binding.contentLL.getGlobalVisibleRect(rect)
        return rect.contains(x.toInt(), y.toInt())
    }

    /**
     * 处理触摸事件：空白处点击穿透并关闭页面
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // 如果点击的是空白区域（非内容区域）
            if (!isTouchInsideContent(ev.rawX, ev.rawY)) {
                // 带动画关闭页面，让事件穿透到下层
                closeWithAnimation()
                return false // 返回 false 让事件穿透
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

