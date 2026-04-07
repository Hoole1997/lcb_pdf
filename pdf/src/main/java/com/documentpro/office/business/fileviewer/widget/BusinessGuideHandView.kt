package com.documentpro.office.business.fileviewer.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import com.documentpro.office.business.fileviewer.R
import kotlin.math.abs

/**
 * 手势方向枚举
 */
enum class SwipeDirection {
    LEFT, RIGHT
}

/**
 * 手势回调接口
 */
interface OnSwipeGestureListener {
    /**
     * 滑动手势回调
     * @param direction 滑动方向 LEFT 或 RIGHT
     */
    fun onSwipe(direction: SwipeDirection)
}

/**
 * 自定义View，用于显示手指左滑的引导动画
 */
class BusinessGuideHandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var handBitmap: Bitmap? = null
    
    // 动画相关参数
    private var animator: ValueAnimator? = null
    private var translationX = 0f
    private var startX = 0f
    private var endX = 0f
    private var isAnimating = false
    
    // 动画配置
    private val animationDuration = 1200L
    private val animationDelay = 300L
    
    // 手势相关
    private var swipeListener: OnSwipeGestureListener? = null
    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var initialX = 0f
    
    // 最小滑动距离阈值，以密度为基础
    private val minSwipeDistance = (context.resources.displayMetrics.density * 50).toInt() // 50dp
    
    // 手势检测器
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            e1 ?: return false
            
            val distanceX = e2.x - e1.x
            val distanceY = e2.y - e1.y
            
            // 只有当水平滑动距离大于垂直滑动距离，并且水平滑动距离大于最小滑动距离阈值时才触发
            if (abs(distanceX) > abs(distanceY) && abs(distanceX) > minSwipeDistance) {
                if (distanceX > 0) {
                    // 向右滑动
                    swipeListener?.onSwipe(SwipeDirection.RIGHT)
                } else {
                    // 向左滑动
                    swipeListener?.onSwipe(SwipeDirection.LEFT)
                }
                return true
            }
            return false
        }
    })
    
    init {
        // 初始化手指图片
        handBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_hand_gesture)
        
        // 设置可点击以接收触摸事件
        isClickable = true
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // 设置动画的开始和结束位置
        // 调整为从屏幕右侧70%位置开始，到30%位置结束
        // 这样图标不会出现在过于左侧的位置
        startX = w * 0.7f
        endX = w * 0.3f
        
        // 初始设置为开始位置
        translationX = startX
        
        // 初始化时开始动画
        startAnimation()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        handBitmap?.let {
            // 计算手指图片位置，使其垂直居中
            val centerY = height / 2f - it.height / 2f
            
            // 为了让手指图标更居中地指向滑动区域，需要考虑图标的宽度
            // 将绘制位置向左偏移图标宽度的一半，使手指部分位于计算的X坐标
            val adjustedX = translationX - (it.width / 2f)
            
            canvas.drawBitmap(it, adjustedX, centerY, handPaint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 首先让手势检测器处理事件
        val gestureResult = gestureDetector.onTouchEvent(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                return true
            }
            MotionEvent.ACTION_UP -> {
                val diffX = event.x - initialX
                
                // 如果手势检测器没有处理，并且滑动距离超过阈值，我们自己处理
                if (!gestureResult && abs(diffX) > minSwipeDistance) {
                    if (diffX > 0) {
                        swipeListener?.onSwipe(SwipeDirection.RIGHT)
                    } else {
                        swipeListener?.onSwipe(SwipeDirection.LEFT)
                    }
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event) || gestureResult
    }
    
    /**
     * 设置滑动手势回调
     */
    fun setOnSwipeGestureListener(listener: OnSwipeGestureListener) {
        this.swipeListener = listener
    }
    
    /**
     * 开始左滑动画
     */
    fun startAnimation() {
        if (isAnimating) return
        
        animator?.cancel()
        animator = ValueAnimator.ofFloat(startX, endX).apply {
            duration = animationDuration
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animation ->
                translationX = animation.animatedValue as Float
                invalidate()
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    isAnimating = true
                }
                
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    // 延迟一段时间后重新开始动画，形成循环效果
                    postDelayed({ startAnimation() }, animationDelay)
                }
            })
            
            start()
        }
    }
    
    /**
     * 重置动画到初始状态
     */
    fun resetAnimation() {
        stopAnimation()
        translationX = startX
        invalidate()
    }
    
    /**
     * 停止动画
     */
    fun stopAnimation() {
        animator?.cancel()
        isAnimating = false
    }

    fun isAnimating(): Boolean {
        return isAnimating
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
        handBitmap?.recycle()
        handBitmap = null
    }
}