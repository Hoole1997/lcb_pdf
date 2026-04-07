package com.documentpro.office.business.fileviewer.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.documentpro.office.business.fileviewer.R

class BusinessCircleProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Int = 0 // 0-100
    private var max: Int = 100
    private var strokeWidth: Float = 16f
    private var progressColor: Int = ContextCompat.getColor(context, R.color.progress_color)
    private var bgColor: Int = ContextCompat.getColor(context, R.color.white)
    private var roundCap: Boolean = true

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView)
        progress = ta.getInt(R.styleable.CircleProgressView_cpv_progress, progress)
        max = ta.getInt(R.styleable.CircleProgressView_cpv_max, max)
        progressColor = ta.getColor(R.styleable.CircleProgressView_cpv_progressColor, progressColor)
        bgColor = ta.getColor(R.styleable.CircleProgressView_cpv_bgColor, bgColor)
        strokeWidth = ta.getDimension(R.styleable.CircleProgressView_cpv_strokeWidth, strokeWidth)
        roundCap = ta.getBoolean(R.styleable.CircleProgressView_cpv_roundCap, roundCap)
        ta.recycle()
    }

    fun setProgress(progress: Int) {
        this.progress = progress.coerceIn(0, max)
        invalidate()
    }

    fun setProgressColor(color: Int) {
        this.progressColor = color
        invalidate()
    }

    fun setBgColor(color: Int) {
        this.bgColor = color
        invalidate()
    }

    fun setStrokeWidth(width: Float) {
        this.strokeWidth = width
        invalidate()
    }

    fun setRoundCap(round: Boolean) {
        this.roundCap = round
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = width.coerceAtMost(height)
        val halfStroke = strokeWidth / 2
        rectF.set(halfStroke, halfStroke, size - halfStroke, size - halfStroke)

        // 画背景圆环
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = bgColor
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(rectF, 0f, 360f, false, paint)

        // 画进度
        paint.color = progressColor
        paint.strokeCap = if (roundCap) Paint.Cap.ROUND else Paint.Cap.BUTT
        val sweepAngle = 360f * progress / max
        canvas.drawArc(rectF, -90f, sweepAngle, false, paint)
    }
} 