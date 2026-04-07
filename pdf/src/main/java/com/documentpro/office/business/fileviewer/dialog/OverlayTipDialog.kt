package com.documentpro.office.business.fileviewer.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import com.blankj.utilcode.util.SPUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.LayoutOverlayTipDialogBinding
import com.documentpro.office.business.fileviewer.ui.setting.BusinessSettingActivity
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.FullScreenPopupView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OverlayTipDialog(
    context: Context,
    private val onDismissListener: (isConfirmed: Boolean) -> Unit
) : FullScreenPopupView(context) {

    private var isConfirmed = false
    private var isDismissing = false
    private var arrowAnimator: AnimatorSet? = null

    companion object {
        private const val SP_KEY_OVERLAY_TIP_SHOWN = "overlay_tip_dialog_shown"

        /**
         * 检查并显示弹框（仅在首次显示时）
         * @param onDismiss 弹框关闭时的回调，可以在这里打开负一屏
         */
        fun checkShow(context: Context, onDismiss: (() -> Unit)? = null) {
            val hasShown = getHasShown()
            if (!hasShown) {
                show(context, onDismiss)
            }
        }

        fun getHasShown(): Boolean =
            SPUtils.getInstance().getBoolean(SP_KEY_OVERLAY_TIP_SHOWN, false)

        /**
         * 显示自定义弹框
         * @param onDismiss 弹框关闭时的回调，可以在这里打开负一屏
         */
        fun show(context: Context, onDismiss: (() -> Unit)? = null) {
            val dialog = OverlayTipDialog(context) { result ->
                // 弹框关闭时的回调
                onDismiss?.invoke()
            }

            XPopup.Builder(context)
                .hasNavigationBar(false)
                .hasStatusBar(false)
                .dismissOnBackPressed(true)
                .dismissOnTouchOutside(false)
                .enableDrag(false)
                .asCustom(dialog)
                .show()
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_overlay_tip_dialog
    }

    override fun onCreate() {
        super.onCreate()
        val binding = LayoutOverlayTipDialogBinding.bind(popupImplView)

        // 标记已显示，持久化到 SharedPreferences
        SPUtils.getInstance().put(SP_KEY_OVERLAY_TIP_SHOWN, true)

        // 启动箭头动画
        startArrowAnimation(binding.ivNext)

        binding.main.setOnClickListener {
           dismiss()
        }
    }

    /**
     * 启动箭头动画
     * 箭头一直从左往右平移，到达右边时淡出，然后直接从左边淡入重新开始
     */
    private fun startArrowAnimation(arrow: android.widget.ImageView) {
        // 停止之前的动画
        arrowAnimator?.cancel()

        val distance = 40f * context.resources.displayMetrics.density

        // 使用 AnimatorListener 在动画结束时重置位置并重新开始
        fun createSingleAnimation() {
            // 平移动画：从左往右移动
            val translateX = ObjectAnimator.ofFloat(arrow, "translationX", 0f, distance).apply {
                duration = 800
            }

            // 透明度动画：先保持可见，最后淡出
            val alpha = ObjectAnimator.ofFloat(arrow, "alpha", 1f, 1f, 0f).apply {
                duration = 800
            }

            arrowAnimator = AnimatorSet().apply {
                playTogether(translateX, alpha)
                interpolator = LinearInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // 动画结束后，立即重置位置和透明度，然后重新开始
                        arrow.translationX = 0f
                        arrow.alpha = 1f
                        // 延迟一点点再开始下一次，让淡入更自然
                        arrow.postDelayed({
                            if (arrowAnimator != null) {
                                createSingleAnimation()
                            }
                        }, 100)
                    }
                })
                start()
            }
        }

        createSingleAnimation()
    }

    /**
     * 停止箭头动画
     */
    private fun stopArrowAnimation() {
        arrowAnimator?.cancel()
        arrowAnimator = null
    }

    override fun onDismiss() {
        super.onDismiss()
        stopArrowAnimation()
    }

    override fun dismiss() {
        // 如果正在执行消失动画，直接返回
        if (isDismissing) {
            return
        }

        // 标记正在消失
        isDismissing = true

        // 执行自定义的消失动画
        animateDismissFromLeftToRight()
    }

    /**
     * 从左到右的消失动画
     */
    private fun animateDismissFromLeftToRight() {
        val view = popupImplView ?: run {
            // 如果没有视图，直接调用父类方法并触发回调
            isDismissing = false
            super.dismiss()
            onDismissListener.invoke(isConfirmed)
            return
        }

        val screenWidth = context.resources.displayMetrics.widthPixels

        // 创建从左到右的平移动画
        val translateAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, screenWidth.toFloat())
        translateAnimator.duration = 300
        translateAnimator.interpolator = AccelerateInterpolator()

        // 创建透明度动画
        val alphaAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
        alphaAnimator.duration = 300
        alphaAnimator.interpolator = AccelerateInterpolator()

        // 使用 AnimatorSet 同时执行两个动画
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(translateAnimator, alphaAnimator)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 动画结束后，重置标志并调用父类的 dismiss 来真正关闭弹框
                isDismissing = false
                super@OverlayTipDialog.dismiss()
                // 调用回调
                onDismissListener.invoke(isConfirmed)
            }
        })

        // 启动动画
        animatorSet.start()
    }

}

