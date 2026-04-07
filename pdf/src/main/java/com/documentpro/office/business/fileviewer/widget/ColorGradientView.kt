package com.documentpro.office.business.fileviewer.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils

/**
 * 颜色渐变 View
 * 实现从一个颜色渐变到另一个颜色的动画效果
 */
class ColorGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentColor: Int = Color.parseColor("#1868FF")
    private var animator: ValueAnimator? = null

    init {
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = currentColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    /**
     * 开始颜色渐变动画（支持多个颜色）
     * @param duration 动画时长（毫秒）
     * @param colors 颜色数组，支持多个颜色渐变
     */
    fun startGradientAnimation(
        duration: Long,
        colors: Array<String> = arrayOf("#1868FF", "#FF8018", "#FF5919")
    ) {
        stopGradientAnimation()

        if (colors.size < 2) {
            return
        }

        val parsedColors = colors.map { Color.parseColor(it) }.toIntArray()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            this.interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                currentColor = getColorAtFraction(parsedColors, fraction)
                invalidate()
            }
            start()
        }
    }

    /**
     * 根据进度获取对应的颜色
     * @param colors 颜色数组
     * @param fraction 进度 0-1
     */
    private fun getColorAtFraction(colors: IntArray, fraction: Float): Int {
        if (colors.size == 1) return colors[0]
        
        // 计算当前在哪两个颜色之间
        val segmentCount = colors.size - 1
        val scaledFraction = fraction * segmentCount
        val segmentIndex = scaledFraction.toInt().coerceIn(0, segmentCount - 1)
        val segmentFraction = scaledFraction - segmentIndex
        
        val colorStart = colors[segmentIndex]
        val colorEnd = colors[segmentIndex + 1]
        
        return ColorUtils.blendARGB(colorStart, colorEnd, segmentFraction)
    }

    /**
     * 停止颜色渐变动画
     */
    fun stopGradientAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopGradientAnimation()
    }
}

