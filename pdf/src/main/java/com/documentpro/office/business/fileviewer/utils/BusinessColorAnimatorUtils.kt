package com.documentpro.office.business.fileviewer.utils

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 颜色动画工具类
 * 提供背景色平滑过渡动画功能和圆形扩散动画
 */
object BusinessColorAnimatorUtils {
    
    /**
     * 创建背景色渐变动画
     * @param context 上下文
     * @param startColorRes 起始颜色资源ID
     * @param endColorRes 结束颜色资源ID
     * @param startDrawableRes 起始drawable资源ID（用于渐变背景）
     * @param endDrawableRes 结束drawable资源ID（用于渐变背景）
     * @param duration 动画时长（毫秒）
     * @param onUpdate 动画更新回调，返回当前的Drawable
     * @param onEnd 动画结束回调
     */
    fun createBackgroundColorAnimator(
        context: Context,
        startColorRes: Int? = null,
        endColorRes: Int? = null,
        startDrawableRes: Int? = null,
        endDrawableRes: Int? = null,
        duration: Long = 400L,
        onUpdate: (drawable: Drawable) -> Unit,
        onEnd: (() -> Unit)? = null
    ): ValueAnimator {
        
        return ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            
            // 获取起始和结束的背景
            val startDrawable = when {
                startColorRes != null -> ColorDrawable(ContextCompat.getColor(context, startColorRes))
                startDrawableRes != null -> ContextCompat.getDrawable(context, startDrawableRes)
                else -> null
            }
            
            val endDrawable = when {
                endColorRes != null -> ColorDrawable(ContextCompat.getColor(context, endColorRes))
                endDrawableRes != null -> ContextCompat.getDrawable(context, endDrawableRes)
                else -> null
            }
            
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                
                when {
                    // 纯色之间的渐变
                    startColorRes != null && endColorRes != null -> {
                        val startColor = ContextCompat.getColor(context, startColorRes)
                        val endColor = ContextCompat.getColor(context, endColorRes)
                        val currentColor = ArgbEvaluator().evaluate(fraction, startColor, endColor) as Int
                        onUpdate(ColorDrawable(currentColor))
                    }
                    // 从纯色到渐变
                    startColorRes != null && endDrawableRes != null -> {
                        if (fraction < 0.5f) {
                            // 前半段：纯色淡出
                            val startColor = ContextCompat.getColor(context, startColorRes)
                            val alpha = (255 * (1f - fraction * 2f)).toInt().coerceIn(0, 255)
                            val fadeColor = (startColor and 0x00FFFFFF) or (alpha shl 24)
                            onUpdate(ColorDrawable(fadeColor))
                        } else {
                            // 后半段：渐变淡入
                            endDrawable?.let { drawable ->
                                val alpha = ((fraction - 0.5f) * 2f * 255).toInt().coerceIn(0, 255)
                                drawable.alpha = alpha
                                onUpdate(drawable)
                            }
                        }
                    }
                    // 从渐变到纯色
                    startDrawableRes != null && endColorRes != null -> {
                        if (fraction < 0.5f) {
                            // 前半段：渐变淡出
                            startDrawable?.let { drawable ->
                                val alpha = (255 * (1f - fraction * 2f)).toInt().coerceIn(0, 255)
                                drawable.alpha = alpha
                                onUpdate(drawable)
                            }
                        } else {
                            // 后半段：纯色淡入
                            val endColor = ContextCompat.getColor(context, endColorRes)
                            val alpha = ((fraction - 0.5f) * 2f * 255).toInt().coerceIn(0, 255)
                            val fadeColor = (endColor and 0x00FFFFFF) or (alpha shl 24)
                            onUpdate(ColorDrawable(fadeColor))
                        }
                    }
                    // 渐变之间的切换（使用交叉淡入淡出）
                    startDrawableRes != null && endDrawableRes != null -> {
                        if (fraction < 0.5f) {
                            // 前半段：起始渐变淡出
                            startDrawable?.let { drawable ->
                                val alpha = (255 * (1f - fraction * 2f)).toInt().coerceIn(0, 255)
                                drawable.alpha = alpha
                                onUpdate(drawable)
                            }
                        } else {
                            // 后半段：结束渐变淡入
                            endDrawable?.let { drawable ->
                                val alpha = ((fraction - 0.5f) * 2f * 255).toInt().coerceIn(0, 255)
                                drawable.alpha = alpha
                                onUpdate(drawable)
                            }
                        }
                    }
                }
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    super.onAnimationEnd(animation)
                    onEnd?.invoke()
                }
            })
        }
    }
    
    /**
     * 创建文字颜色渐变动画
     * @param context 上下文
     * @param startColorRes 起始颜色资源ID
     * @param endColorRes 结束颜色资源ID
     * @param duration 动画时长（毫秒）
     * @param onUpdate 动画更新回调，返回当前颜色值
     * @param onEnd 动画结束回调
     */
    fun createTextColorAnimator(
        context: Context,
        startColorRes: Int,
        endColorRes: Int,
        duration: Long = 300L,
        onUpdate: (color: Int) -> Unit,
        onEnd: (() -> Unit)? = null
    ): ValueAnimator {
        
        val startColor = ContextCompat.getColor(context, startColorRes)
        val endColor = ContextCompat.getColor(context, endColorRes)
        
        return ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animator ->
                val currentColor = animator.animatedValue as Int
                onUpdate(currentColor)
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    super.onAnimationEnd(animation)
                    onEnd?.invoke()
                }
            })
        }
    }
    
    /**
     * 创建圆形扩散动画
     * @param context 上下文
     * @param targetView 目标View（用于获取尺寸）
     * @param centerX 扩散中心X坐标（相对于targetView）
     * @param centerY 扩散中心Y坐标（相对于targetView）
     * @param startColorRes 起始颜色资源ID
     * @param endColorRes 结束颜色资源ID
     * @param startDrawableRes 起始drawable资源ID
     * @param endDrawableRes 结束drawable资源ID
     * @param duration 动画时长（毫秒）
     * @param onUpdate 动画更新回调，返回当前的Drawable
     * @param onEnd 动画结束回调
     */
    fun createRippleAnimator(
        context: Context,
        targetView: View,
        centerX: Float,
        centerY: Float,
        startColorRes: Int? = null,
        endColorRes: Int? = null,
        startDrawableRes: Int? = null,
        endDrawableRes: Int? = null,
        duration: Long = 500L,
        onUpdate: (drawable: Drawable) -> Unit,
        onEnd: (() -> Unit)? = null
    ): ValueAnimator {
        
        return ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            
            // 获取目标View的尺寸
            val viewWidth = targetView.width.toFloat()
            val viewHeight = targetView.height.toFloat()
            
            // 计算扩散的最大半径（到最远角的距离）
            val maxRadius = sqrt(
                max(centerX, viewWidth - centerX) * max(centerX, viewWidth - centerX) +
                max(centerY, viewHeight - centerY) * max(centerY, viewHeight - centerY)
            )
            
            // 获取起始和结束背景
            val startColor = startColorRes?.let { ContextCompat.getColor(context, it) }
            val endColor = endColorRes?.let { ContextCompat.getColor(context, it) }
            val startDrawable = startDrawableRes?.let { ContextCompat.getDrawable(context, it) }
            val endDrawable = endDrawableRes?.let { ContextCompat.getDrawable(context, it) }
            
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                val currentRadius = maxRadius * fraction
                
                // 创建自定义Drawable实现扩散效果
                val rippleDrawable = object : Drawable() {
                    override fun draw(canvas: Canvas) {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        
                        // 绘制旧背景
                        when {
                            startColor != null -> {
                                paint.color = startColor
                                canvas.drawRect(bounds, paint)
                            }
                            startDrawable != null -> {
                                startDrawable.bounds = bounds
                                startDrawable.draw(canvas)
                            }
                        }
                        
                        // 绘制扩散的新背景
                        if (fraction > 0f) {
                            // 创建圆形裁剪区域
                            val clipPath = Path().apply {
                                addCircle(centerX, centerY, currentRadius, Path.Direction.CW)
                            }
                            
                            canvas.save()
                            canvas.clipPath(clipPath)
                            
                            when {
                                endColor != null -> {
                                    paint.color = endColor
                                    canvas.drawRect(bounds, paint)
                                }
                                endDrawable != null -> {
                                    endDrawable.bounds = bounds
                                    endDrawable.draw(canvas)
                                }
                            }
                            
                            canvas.restore()
                        }
                    }
                    
                    override fun setAlpha(alpha: Int) {}
                    override fun setColorFilter(colorFilter: ColorFilter?) {}
                    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
                }
                
                // 设置drawable的边界
                rippleDrawable.setBounds(0, 0, viewWidth.toInt(), viewHeight.toInt())
                onUpdate(rippleDrawable)
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    super.onAnimationEnd(animation)
                    // 动画结束后设置最终背景
                    val finalDrawable = when {
                        endColor != null -> ColorDrawable(endColor)
                        endDrawable != null -> endDrawable
                        else -> null
                    }
                    finalDrawable?.let { onUpdate(it) }
                    onEnd?.invoke()
                }
            })
        }
    }
    
    /**
     * 获取Tab的真实中心位置
     * @param tabLayout TabLayout
     * @param position tab位置
     * @return Pair<centerX, centerY> 相对于TabLayout的坐标
     */
    fun getTabCenterPosition(tabLayout: View, position: Int): Pair<Float, Float> {
        if (tabLayout is com.google.android.material.tabs.TabLayout) {
            val tabCount = tabLayout.tabCount
            if (position >= 0 && position < tabCount) {
                try {
                    // 通过反射获取TabLayout内部的SlidingTabIndicator
                    val slidingTabStripField = tabLayout.javaClass.getDeclaredField("slidingTabIndicator")
                    slidingTabStripField.isAccessible = true
                    val slidingTabStrip = slidingTabStripField.get(tabLayout) as android.view.ViewGroup
                    
                    // 获取指定位置的Tab View
                    if (position < slidingTabStrip.childCount) {
                        val tabView = slidingTabStrip.getChildAt(position)
                        
                        // 获取Tab View的位置和尺寸
                        val tabLeft = tabView.left.toFloat()
                        val tabRight = tabView.right.toFloat()
                        val tabTop = tabView.top.toFloat()
                        val tabBottom = tabView.bottom.toFloat()
                        
                        // 计算Tab的中心位置
                        val centerX = (tabLeft + tabRight) / 2
                        val centerY = (tabTop + tabBottom) / 2
                        
                        return Pair(centerX, centerY)
                    }
                } catch (e: Exception) {
                    // 反射失败时使用备用方案
                    android.util.Log.w("BusinessColorAnimatorUtils", "Failed to get real tab position, using fallback", e)
                }
                
                // 备用方案：平均分布计算
                val tabWidth = tabLayout.width.toFloat() / tabCount
                val centerX = tabWidth * position + tabWidth / 2
                val centerY = tabLayout.height.toFloat() / 2
                
                return Pair(centerX, centerY)
            }
        }
        
        // 默认返回TabLayout中心
        return Pair(tabLayout.width.toFloat() / 2, tabLayout.height.toFloat() / 2)
    }
    
    /**
     * 创建滑动擦除动画 - 从左到右或从右到左的滑动效果
     */
    fun createSlideAnimation(
        context: android.content.Context,
        targetView: View,
        fromLeft: Boolean = true, // true: 从左滑入, false: 从右滑入
        startColorRes: Int? = null,
        endColorRes: Int? = null,
        startDrawableRes: Int? = null,
        endDrawableRes: Int? = null,
        duration: Long = 400L,
        onUpdate: (android.graphics.drawable.Drawable) -> Unit,
        onEnd: (() -> Unit)? = null
    ): android.animation.ValueAnimator {
        
        val startDrawable = when {
            startColorRes != null -> android.graphics.drawable.ColorDrawable(
                androidx.core.content.ContextCompat.getColor(context, startColorRes)
            )
            startDrawableRes != null -> androidx.core.content.ContextCompat.getDrawable(context, startDrawableRes)
            else -> android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        }
        
        val endDrawable = when {
            endColorRes != null -> android.graphics.drawable.ColorDrawable(
                androidx.core.content.ContextCompat.getColor(context, endColorRes)
            )
            endDrawableRes != null -> androidx.core.content.ContextCompat.getDrawable(context, endDrawableRes)
            else -> android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        }
        
        return android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = android.view.animation.DecelerateInterpolator()
            
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val drawable = createSlideDrawable(targetView, startDrawable, endDrawable, progress, fromLeft)
                onUpdate(drawable)
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd?.invoke()
                }
            })
        }
    }
    
    /**
     * 创建淡入淡出动画 - 平滑的透明度过渡
     */
    fun createFadeAnimation(
        context: android.content.Context,
        startColorRes: Int? = null,
        endColorRes: Int? = null,
        startDrawableRes: Int? = null,
        endDrawableRes: Int? = null,
        duration: Long = 300L,
        onUpdate: (android.graphics.drawable.Drawable) -> Unit,
        onEnd: (() -> Unit)? = null
    ): android.animation.ValueAnimator {
        
        val startDrawable = when {
            startColorRes != null -> android.graphics.drawable.ColorDrawable(
                androidx.core.content.ContextCompat.getColor(context, startColorRes)
            )
            startDrawableRes != null -> androidx.core.content.ContextCompat.getDrawable(context, startDrawableRes)
            else -> android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        }
        
        val endDrawable = when {
            endColorRes != null -> android.graphics.drawable.ColorDrawable(
                androidx.core.content.ContextCompat.getColor(context, endColorRes)
            )
            endDrawableRes != null -> androidx.core.content.ContextCompat.getDrawable(context, endDrawableRes)
            else -> android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        }
        
        return android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val drawable = createFadeDrawable(startDrawable, endDrawable, progress)
                onUpdate(drawable)
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd?.invoke()
                }
            })
        }
    }
    
    /**
     * 创建弹性缩放动画 - 带有弹性效果的缩放过渡
     */
    fun createBounceScaleAnimation(
        context: android.content.Context,
        targetView: View,
        startColorRes: Int? = null,
        endColorRes: Int? = null,
        startDrawableRes: Int? = null,
        endDrawableRes: Int? = null,
        duration: Long = 600L,
        onUpdate: (android.graphics.drawable.Drawable) -> Unit,
        onEnd: (() -> Unit)? = null
    ): android.animation.ValueAnimator {
        
        val startDrawable = when {
            startColorRes != null -> android.graphics.drawable.ColorDrawable(
                androidx.core.content.ContextCompat.getColor(context, startColorRes)
            )
            startDrawableRes != null -> androidx.core.content.ContextCompat.getDrawable(context, startDrawableRes)
            else -> android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        }
        
        val endDrawable = when {
            endColorRes != null -> android.graphics.drawable.ColorDrawable(
                androidx.core.content.ContextCompat.getColor(context, endColorRes)
            )
            endDrawableRes != null -> androidx.core.content.ContextCompat.getDrawable(context, endDrawableRes)
            else -> android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        }
        
        return android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = android.view.animation.BounceInterpolator()
            
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val drawable = createScaleDrawable(targetView, startDrawable, endDrawable, progress)
                onUpdate(drawable)
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd?.invoke()
                }
            })
        }
    }
    
    /**
     * 创建滑动效果的Drawable
     */
    private fun createSlideDrawable(
        targetView: View,
        startDrawable: android.graphics.drawable.Drawable?,
        endDrawable: android.graphics.drawable.Drawable?,
        progress: Float,
        fromLeft: Boolean
    ): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            override fun draw(canvas: android.graphics.Canvas) {
                val width = bounds.width()
                val height = bounds.height()
                
                // 绘制开始背景
                startDrawable?.let {
                    it.setBounds(0, 0, width, height)
                    it.draw(canvas)
                }
                
                // 绘制滑入的背景
                endDrawable?.let {
                    canvas.save()
                    
                    val slideDistance = (width * progress).toInt()
                    if (fromLeft) {
                        // 从左滑入
                        canvas.clipRect(0, 0, slideDistance, height)
                    } else {
                        // 从右滑入
                        canvas.clipRect(width - slideDistance, 0, width, height)
                    }
                    
                    it.setBounds(0, 0, width, height)
                    it.draw(canvas)
                    canvas.restore()
                }
            }
            
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }
    
    /**
     * 创建淡入淡出效果的Drawable
     */
    private fun createFadeDrawable(
        startDrawable: android.graphics.drawable.Drawable?,
        endDrawable: android.graphics.drawable.Drawable?,
        progress: Float
    ): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            override fun draw(canvas: android.graphics.Canvas) {
                val width = bounds.width()
                val height = bounds.height()
                
                // 绘制开始背景（透明度递减）
                startDrawable?.let {
                    it.setBounds(0, 0, width, height)
                    it.alpha = ((1 - progress) * 255).toInt()
                    it.draw(canvas)
                }
                
                // 绘制结束背景（透明度递增）
                endDrawable?.let {
                    it.setBounds(0, 0, width, height)
                    it.alpha = (progress * 255).toInt()
                    it.draw(canvas)
                }
            }
            
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }
    
    /**
     * 创建缩放效果的Drawable
     */
    private fun createScaleDrawable(
        targetView: View,
        startDrawable: android.graphics.drawable.Drawable?,
        endDrawable: android.graphics.drawable.Drawable?,
        progress: Float
    ): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            override fun draw(canvas: android.graphics.Canvas) {
                val width = bounds.width()
                val height = bounds.height()
                val centerX = width / 2f
                val centerY = height / 2f
                
                // 绘制开始背景
                startDrawable?.let {
                    it.setBounds(0, 0, width, height)
                    it.draw(canvas)
                }
                
                // 绘制缩放的结束背景
                endDrawable?.let {
                    canvas.save()
                    
                    val scale = progress
                    canvas.scale(scale, scale, centerX, centerY)
                    
                    // 创建圆形裁剪区域
                    val radius = Math.min(width, height) * scale / 2f
                    val path = android.graphics.Path()
                    path.addCircle(centerX, centerY, radius, android.graphics.Path.Direction.CW)
                    canvas.clipPath(path)
                    
                    it.setBounds(0, 0, width, height)
                    it.draw(canvas)
                    canvas.restore()
                }
            }
            
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }
}
