package com.documentpro.office.business.fileviewer.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.documentpro.office.business.fileviewer.R
import kotlin.math.min

/**
 * 支持多颜色的圆形进度条
 * 可以设置多个颜色段，每个段有自己的百分比和颜色
 */
class BusinessMultiColorCircleProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 进度段列表
    private var progressSegments: List<BusinessProgressSegment> = emptyList()
    private var targetSegments: List<BusinessProgressSegment> = emptyList()
    
    // 绘制属性
    private var strokeWidth: Float = 16f
    private var bgColor: Int = ContextCompat.getColor(context, R.color.white)
    private var roundCap: Boolean = true
    private var startAngle: Float = -90f // 开始角度，默认从顶部开始
    
    // 间隔角度（可选，用于在不同颜色段之间添加间隔）
    private var segmentGap: Float = 0f
    
    // 动画相关
    private var animationProgress: Float = 1f // 动画进度 0-1
    private var animator: ValueAnimator? = null
    private var animationDuration: Long = 800L // 动画时长
    private var isFirstTime: Boolean = true // 是否首次设置进度
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    
    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView)
        bgColor = ta.getColor(R.styleable.CircleProgressView_cpv_bgColor, bgColor)
        strokeWidth = ta.getDimension(R.styleable.CircleProgressView_cpv_strokeWidth, strokeWidth)
        roundCap = ta.getBoolean(R.styleable.CircleProgressView_cpv_roundCap, roundCap)
        ta.recycle()
    }
    
    /**
     * 设置进度段
     * @param segments 进度段列表，每个段包含百分比和颜色
     * @param animated 是否使用动画，默认为true（但只有首次才会真正动画）
     */
    fun setProgressSegments(segments: List<BusinessProgressSegment>, animated: Boolean = true) {
        targetSegments = segments
        
        // 只有首次且animated为true时才播放动画
        if (animated && isFirstTime) {
            isFirstTime = false
            startAnimation()
        } else {
            animationProgress = 1f
            progressSegments = segments
            invalidate()
        }
    }
    
    /**
     * 添加单个进度段
     */
    fun addProgressSegment(segment: BusinessProgressSegment) {
        progressSegments = progressSegments + segment
        invalidate()
    }
    
    /**
     * 清空所有进度段
     */
    fun clearProgressSegments() {
        progressSegments = emptyList()
        invalidate()
    }
    
    /**
     * 设置进度条宽度
     */
    fun setStrokeWidth(width: Float) {
        this.strokeWidth = width
        invalidate()
    }
    
    /**
     * 设置背景颜色
     */
    fun setBgColor(color: Int) {
        this.bgColor = color
        invalidate()
    }
    
    /**
     * 设置是否使用圆角
     */
    fun setRoundCap(round: Boolean) {
        this.roundCap = round
        invalidate()
    }
    
    /**
     * 设置开始角度
     * @param angle 开始角度，0为3点钟方向，-90为12点钟方向
     */
    fun setStartAngle(angle: Float) {
        this.startAngle = angle
        invalidate()
    }
    
    /**
     * 设置颜色段之间的间隔角度
     * @param gap 间隔角度
     */
    fun setSegmentGap(gap: Float) {
        this.segmentGap = gap
        invalidate()
    }
    
    /**
     * 便捷方法：设置单一颜色的进度
     * @param progress 进度值 0-100
     * @param color 进度颜色
     * @param animated 是否使用动画，默认为true
     */
    fun setProgress(progress: Float, color: Int, animated: Boolean = true) {
        setProgressSegments(listOf(BusinessProgressSegment(progress, color)), animated)
    }
    
    /**
     * 设置动画时长
     * @param duration 动画时长（毫秒）
     */
    fun setAnimationDuration(duration: Long) {
        this.animationDuration = duration
    }
    
    /**
     * 开始动画
     */
    private fun startAnimation() {
        // 取消之前的动画
        animator?.cancel()
        
        // 创建新动画
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                
                // 根据动画进度计算当前显示的进度段
                progressSegments = targetSegments.map { segment ->
                    BusinessProgressSegment(
                        percentage = segment.percentage * animationProgress,
                        color = segment.color
                    )
                }
                
                invalidate()
            }
            
            start()
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val size = min(width, height)
        val halfStroke = strokeWidth / 2
        rectF.set(halfStroke, halfStroke, size - halfStroke, size - halfStroke)
        
        // 绘制背景圆环
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = bgColor
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(rectF, 0f, 360f, false, paint)
        
        // 绘制进度段
        if (progressSegments.isNotEmpty()) {
            var currentAngle = startAngle
            val totalPercentage = progressSegments.sumOf { it.percentage.toDouble() }.toFloat()
            
            // 如果总百分比超过100，进行归一化
            val normalizedSegments = if (totalPercentage > 100) {
                progressSegments.map { 
                    BusinessProgressSegment(it.percentage * 100 / totalPercentage, it.color)
                }
            } else {
                progressSegments
            }
            
            // 绘制每个进度段
            normalizedSegments.forEach { segment ->
                if (segment.percentage > 0) {
                    paint.color = segment.color
                    paint.strokeCap = if (roundCap) Paint.Cap.ROUND else Paint.Cap.BUTT
                    
                    // 计算扫描角度
                    val sweepAngle = 360f * segment.percentage / 100f
                    
                    // 绘制弧形
                    canvas.drawArc(rectF, currentAngle, sweepAngle - segmentGap, false, paint)
                    
                    // 更新当前角度
                    currentAngle += sweepAngle
                }
            }
        }
    }
    
    /**
     * 获取当前所有进度段的总百分比
     */
    fun getTotalPercentage(): Float {
        return progressSegments.sumOf { it.percentage.toDouble() }.toFloat()
    }
    
    /**
     * 获取当前进度段列表
     */
    fun getProgressSegments(): List<BusinessProgressSegment> {
        return progressSegments.toList()
    }
    
    /**
     * 取消动画
     */
    fun cancelAnimation() {
        animator?.cancel()
        animator = null
    }
    
    /**
     * 重置首次标志，让下次设置时再次播放动画
     */
    fun resetFirstTime() {
        isFirstTime = true
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理动画资源
        cancelAnimation()
    }
}