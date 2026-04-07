package com.documentpro.office.business.fileviewer.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * 列表动画工具类
 * 提供各种列表项和RecyclerView的动画效果
 */
object BusinessListAnimationUtils {

    /**
     * 列表项进入动画类型
     */
    enum class ItemAnimationType {
        FADE_IN,           // 淡入
        SLIDE_IN_LEFT,     // 从左滑入
        SLIDE_IN_RIGHT,    // 从右滑入
        SLIDE_IN_BOTTOM,   // 从底部滑入
        SCALE_IN,          // 缩放进入
        BOUNCE_IN,         // 弹跳进入
        ROTATE_FADE_IN     // 旋转淡入
    }

    /**
     * 为列表项设置进入动画
     * @param view 要应用动画的视图
     * @param animationType 动画类型
     * @param delay 延迟时间(ms)
     * @param duration 动画时长(ms)
     * @param onEnd 动画结束回调
     */
    fun applyItemAnimation(
        view: View,
        animationType: ItemAnimationType = ItemAnimationType.SLIDE_IN_BOTTOM,
        delay: Long = 0,
        duration: Long = 400,
        onEnd: (() -> Unit)? = null
    ) {
        // 重置视图状态
        resetViewState(view)
        
        val animator = when (animationType) {
            ItemAnimationType.FADE_IN -> createFadeInAnimation(view, duration)
            ItemAnimationType.SLIDE_IN_LEFT -> createSlideInLeftAnimation(view, duration)
            ItemAnimationType.SLIDE_IN_RIGHT -> createSlideInRightAnimation(view, duration)
            ItemAnimationType.SLIDE_IN_BOTTOM -> createSlideInBottomAnimation(view, duration)
            ItemAnimationType.SCALE_IN -> createScaleInAnimation(view, duration)
            ItemAnimationType.BOUNCE_IN -> createBounceInAnimation(view, duration)
            ItemAnimationType.ROTATE_FADE_IN -> createRotateFadeInAnimation(view, duration)
        }
        
        animator.startDelay = delay
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        
        animator.start()
    }

    /**
     * 为列表项设置渐进式动画（适用于多个项目依次出现）
     * @param views 视图列表
     * @param animationType 动画类型
     * @param staggerDelay 交错延迟时间(ms)
     * @param duration 动画时长(ms)
     */
    fun applyStaggeredAnimation(
        views: List<View>,
        animationType: ItemAnimationType = ItemAnimationType.SLIDE_IN_BOTTOM,
        staggerDelay: Long = 100,
        duration: Long = 400
    ) {
        views.forEachIndexed { index, view ->
            applyItemAnimation(
                view = view,
                animationType = animationType,
                delay = index * staggerDelay,
                duration = duration
            )
        }
    }

    /**
     * 重置视图状态
     */
    private fun resetViewState(view: View) {
        view.alpha = 1f
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.rotation = 0f
        view.visibility = View.VISIBLE
    }

    /**
     * 创建淡入动画
     */
    private fun createFadeInAnimation(view: View, duration: Long): Animator {
        view.alpha = 0f
        return ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
        }
    }

    /**
     * 创建从左滑入动画
     */
    private fun createSlideInLeftAnimation(view: View, duration: Long): Animator {
        val translationX = -view.width.toFloat()
        view.translationX = translationX
        view.alpha = 0f
        
        val animatorSet = AnimatorSet()
        val translateAnimator = ObjectAnimator.ofFloat(view, "translationX", translationX, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        animatorSet.playTogether(translateAnimator, alphaAnimator)
        animatorSet.duration = duration
        animatorSet.interpolator = DecelerateInterpolator()
        
        return animatorSet
    }

    /**
     * 创建从右滑入动画
     */
    private fun createSlideInRightAnimation(view: View, duration: Long): Animator {
        val translationX = view.width.toFloat()
        view.translationX = translationX
        view.alpha = 0f
        
        val animatorSet = AnimatorSet()
        val translateAnimator = ObjectAnimator.ofFloat(view, "translationX", translationX, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        animatorSet.playTogether(translateAnimator, alphaAnimator)
        animatorSet.duration = duration
        animatorSet.interpolator = DecelerateInterpolator()
        
        return animatorSet
    }

    /**
     * 创建从底部滑入动画
     */
    private fun createSlideInBottomAnimation(view: View, duration: Long): Animator {
        val translationY = 200f // 固定的滑入距离
        view.translationY = translationY
        view.alpha = 0f
        
        val animatorSet = AnimatorSet()
        val translateAnimator = ObjectAnimator.ofFloat(view, "translationY", translationY, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        animatorSet.playTogether(translateAnimator, alphaAnimator)
        animatorSet.duration = duration
        animatorSet.interpolator = DecelerateInterpolator()
        
        return animatorSet
    }

    /**
     * 创建缩放进入动画
     */
    private fun createScaleInAnimation(view: View, duration: Long): Animator {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        
        val animatorSet = AnimatorSet()
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
        animatorSet.duration = duration
        animatorSet.interpolator = OvershootInterpolator()
        
        return animatorSet
    }

    /**
     * 创建弹跳进入动画
     */
    private fun createBounceInAnimation(view: View, duration: Long): Animator {
        val translationY = 100f
        view.translationY = translationY
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.alpha = 0f
        
        val animatorSet = AnimatorSet()
        val translateAnimator = ObjectAnimator.ofFloat(view, "translationY", translationY, 0f)
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.1f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.1f, 1f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        animatorSet.playTogether(translateAnimator, scaleXAnimator, scaleYAnimator, alphaAnimator)
        animatorSet.duration = duration
        animatorSet.interpolator = OvershootInterpolator()
        
        return animatorSet
    }

    /**
     * 创建旋转淡入动画
     */
    private fun createRotateFadeInAnimation(view: View, duration: Long): Animator {
        view.rotation = -90f
        view.alpha = 0f
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        
        val animatorSet = AnimatorSet()
        val rotateAnimator = ObjectAnimator.ofFloat(view, "rotation", -90f, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f)
        
        animatorSet.playTogether(rotateAnimator, alphaAnimator, scaleXAnimator, scaleYAnimator)
        animatorSet.duration = duration
        animatorSet.interpolator = DecelerateInterpolator()
        
        return animatorSet
    }

    /**
     * 创建自定义的RecyclerView ItemAnimator
     */
    fun createCustomItemAnimator(): RecyclerView.ItemAnimator {
        return object : DefaultItemAnimator() {
            override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
                holder?.let { viewHolder ->
                    // 为新添加的项目应用动画
                    applyItemAnimation(
                        view = viewHolder.itemView,
                        animationType = ItemAnimationType.SLIDE_IN_BOTTOM,
                        duration = 300
                    )
                }
                return super.animateAdd(holder)
            }

            override fun animateRemove(holder: RecyclerView.ViewHolder?): Boolean {
                holder?.let { viewHolder ->
                    // 为移除的项目应用动画
                    val animator = createFadeOutAnimation(viewHolder.itemView, 300)
                    animator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            dispatchRemoveFinished(viewHolder)
                        }
                    })
                    animator.start()
                    return false // 返回false表示我们自己处理动画
                }
                return super.animateRemove(holder)
            }
        }
    }

    /**
     * 创建淡出动画
     */
    private fun createFadeOutAnimation(view: View, duration: Long): Animator {
        return ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    /**
     * 为整个RecyclerView设置初始动画
     * @param recyclerView RecyclerView实例
     * @param animationType 动画类型
     * @param staggerDelay 交错延迟
     */
    fun animateRecyclerView(
        recyclerView: RecyclerView,
        animationType: ItemAnimationType = ItemAnimationType.SLIDE_IN_BOTTOM,
        staggerDelay: Long = 50
    ) {
        recyclerView.alpha = 0f
        recyclerView.animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // RecyclerView显示后，为可见的item添加交错动画
                    val visibleViews = mutableListOf<View>()
                    for (i in 0 until recyclerView.childCount) {
                        visibleViews.add(recyclerView.getChildAt(i))
                    }
                    applyStaggeredAnimation(visibleViews, animationType, staggerDelay)
                }
            })
            .start()
    }
}
