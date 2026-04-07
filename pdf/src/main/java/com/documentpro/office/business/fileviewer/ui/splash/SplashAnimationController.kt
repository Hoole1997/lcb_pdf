package com.documentpro.office.business.fileviewer.ui.splash

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar

/**
 * 开屏页动画控制器
 *
 * 负责管理开屏页的所有动画效果：
 * - 子View波浪入场动画
 * - 进度条动画
 * - 进度文字更新
 */
class SplashAnimationController(
    private val contentLayout: LinearLayout,
    private val progressBar: RoundCornerProgressBar,
    private val progressText: TextView
) {
    companion object {
        private const val TAG = BusinessSplashScreenActivity.TAG
        private const val DEFAULT_PROGRESS_DURATION = 10000L
    }

    private var progressAnimator: ValueAnimator? = null
    private var onProgressUpdate: ((Int) -> Unit)? = null

    /**
     * 设置初始状态，防止任何闪现
     */
    fun ensureInitialState() {
        try {
            // 设置子view的初始状态
            for (i in 0 until contentLayout.childCount) {
                val child = contentLayout.getChildAt(i)
                child.alpha = 0f
                child.translationY = 50f
                child.scaleX = 0.9f
                child.scaleY = 0.9f
            }

            // 设置进度条的初始状态
            progressBar.alpha = 0f
            progressText.alpha = 0f
            progressBar.translationY = 30f
            progressText.translationY = 30f
            progressBar.scaleX = 0.95f
            progressText.scaleX = 0.95f
            progressText.scaleY = 0.95f
            progressText.text = "0%"
        } catch (e: Exception) {
            Log.e(TAG, "设置初始状态异常", e)
        }
    }

    /**
     * 执行子view波浪推进动画
     */
    fun playWaveAnimation() {
        try {
            val childViews = mutableListOf<View>()
            for (i in 0 until contentLayout.childCount) {
                childViews.add(contentLayout.getChildAt(i))
            }

            if (childViews.isEmpty()) return

            val animators = mutableListOf<Animator>()

            // 波浪推进效果：每个元素延迟200ms开始
            childViews.forEachIndexed { index, child ->
                val baseDelay = index * 200L

                val fadeIn = ObjectAnimator.ofFloat(child, "alpha", 0f, 1f).apply {
                    duration = 700
                    startDelay = baseDelay
                    interpolator = AccelerateDecelerateInterpolator()
                }

                val translateY = ObjectAnimator.ofFloat(child, "translationY", 50f, 0f).apply {
                    duration = 700
                    startDelay = baseDelay
                    interpolator = AccelerateDecelerateInterpolator()
                }

                val scaleX = ObjectAnimator.ofFloat(child, "scaleX", 0.9f, 1f).apply {
                    duration = 700
                    startDelay = baseDelay
                    interpolator = AccelerateDecelerateInterpolator()
                }

                val scaleY = ObjectAnimator.ofFloat(child, "scaleY", 0.9f, 1f).apply {
                    duration = 700
                    startDelay = baseDelay
                    interpolator = AccelerateDecelerateInterpolator()
                }

                animators.addAll(listOf(fadeIn, translateY, scaleX, scaleY))
            }

            AnimatorSet().apply {
                playTogether(animators)
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "波浪动画执行异常", e)
        }
    }

    /**
     * 显示进度条（保持在0%位置，不启动进度动画）
     */
    fun showProgressBar() {
        try {
            // 进度条优雅动画
            val progressBarFadeIn = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
            }

            val progressBarTranslateY = ObjectAnimator.ofFloat(progressBar, "translationY", 30f, 0f).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
            }

            val progressBarScaleX = ObjectAnimator.ofFloat(progressBar, "scaleX", 0.95f, 1f).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
            }

            // 文字优雅动画
            val textFadeIn = ObjectAnimator.ofFloat(progressText, "alpha", 0f, 1f).apply {
                duration = 350
                interpolator = AccelerateDecelerateInterpolator()
            }

            val textTranslateY = ObjectAnimator.ofFloat(progressText, "translationY", 30f, 0f).apply {
                duration = 350
                interpolator = AccelerateDecelerateInterpolator()
            }

            val textScaleX = ObjectAnimator.ofFloat(progressText, "scaleX", 0.95f, 1f).apply {
                duration = 350
                interpolator = AccelerateDecelerateInterpolator()
            }

            val textScaleY = ObjectAnimator.ofFloat(progressText, "scaleY", 0.95f, 1f).apply {
                duration = 350
                interpolator = AccelerateDecelerateInterpolator()
            }

            // 确保进度条在0%
            progressBar.setProgress(0)
            progressText.text = "0%"

            // 启动进度条显示动画
            AnimatorSet().apply {
                playTogether(
                    progressBarFadeIn, progressBarTranslateY, progressBarScaleX,
                    textFadeIn, textTranslateY, textScaleX, textScaleY
                )
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "显示进度条异常", e)
        }
    }

    /**
     * 执行进度条进度动画（仅进度改变，不含位移动画）
     * @param duration 动画时长，默认10秒
     * @param onUpdate 进度更新回调
     */
    fun playProgressAnimation(
        duration: Long = DEFAULT_PROGRESS_DURATION,
        onUpdate: ((Int) -> Unit)? = null
    ) {
        this.onProgressUpdate = onUpdate

        try {
            // 只启动进度条0-100动画，不含位移动画（进度条已通过showProgressBar显示）
            progressAnimator = ValueAnimator.ofInt(0, 100).apply {
                this.duration = duration
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    try {
                        val progress = animation.animatedValue as Int
                        updateProgress(progress)
                        onProgressUpdate?.invoke(progress)
                    } catch (e: Exception) {
                        Log.e(TAG, "进度更新异常", e)
                        animation.cancel()
                    }
                }
            }

            progressAnimator?.start()
        } catch (e: Exception) {
            Log.e(TAG, "进度条动画异常", e)
        }
    }

    /**
     * 更新进度
     */
    private fun updateProgress(progress: Int) {
        try {
            progressBar.setProgress(progress)
            progressText.text = "$progress%"
        } catch (e: Exception) {
            Log.e(TAG, "更新进度异常", e)
        }
    }

    /**
     * 完成进度（设置到100%）
     */
    fun completeProgress() {
        try {
            progressAnimator?.cancel()
            progressBar.setProgress(100)
            progressText.text = "100%"
        } catch (e: Exception) {
            Log.e(TAG, "完成进度异常", e)
        }
    }

    /**
     * 取消所有动画
     */
    fun cancelAnimations() {
        try {
            progressAnimator?.cancel()
            progressAnimator = null
        } catch (e: Exception) {
            Log.e(TAG, "取消动画异常", e)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        cancelAnimations()
        onProgressUpdate = null
    }
}
