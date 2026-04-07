package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import com.blankj.utilcode.util.SPUtils
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.LayoutGuideScanDialogBinding
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator

class BusinessGuideScanBottomDialog(context: Context,val onConfirmListener:() -> Unit,val onShowListener:() -> Unit,val onDismissListener:() -> Unit) : BottomPopupView(context) {

    private var scaleXAnimator: ObjectAnimator? = null
    private var scaleYAnimator: ObjectAnimator? = null
    private var alphaAnimator: ObjectAnimator? = null

    companion object {
        private const val TAG = "BusinessGuideScanBottomDialog"
        fun checkShow(context: Context,onConfirmListener:() -> Unit,onShowListener:() -> Unit,onDismissListener:() -> Unit) {
            val isFirstGuideScan = SPUtils.getInstance().getBoolean("isFirstGuideScan",true)
            if (isFirstGuideScan) {
                show(context,onConfirmListener,onShowListener,onDismissListener)
            } else {
                onDismissListener.invoke()
            }
        }

        fun show(context: Context,onConfirmListener:() -> Unit,onShowListener:() -> Unit,onDismissListener:() -> Unit) {
            XPopup.Builder(context)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .hasNavigationBar(false)
                .asCustom(BusinessGuideScanBottomDialog(context,onConfirmListener,onShowListener,onDismissListener))
                .show()
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_guide_scan_dialog
    }

    override fun onCreate() {
        super.onCreate()
        SPUtils.getInstance().put("isFirstGuideScan",false)
        val binding = LayoutGuideScanDialogBinding.bind(popupImplView)
        // 呼吸动画：缩放+透明度，分别启动
        scaleXAnimator = ObjectAnimator.ofFloat(binding.ivScanCircle, "scaleX", 1f, 1.15f, 1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
        scaleYAnimator = ObjectAnimator.ofFloat(binding.ivScanCircle, "scaleY", 1f, 1.15f, 1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
        alphaAnimator = ObjectAnimator.ofFloat(binding.ivScanCircle, "alpha", 1f, 0.6f, 1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
        binding.root.setOnClickListener {
            onConfirmListener.invoke()
            dismiss()
        }
        binding.btnUse.setOnClickListener {
            onConfirmListener.invoke()
            dismiss()
        }
        binding.ivScanCircle.setOnClickListener {
            onConfirmListener.invoke()
            dismiss()
        }
        binding.ivClose.setOnClickListener {
            onDismissListener.invoke()
            dismiss()
        }
        binding.ivScanCircle.setOnClickListener {
            onConfirmListener.invoke()
            dismiss()
        }
        BusinessPointLog.logEvent("Guide", mapOf("Guide" to 8))
        onShowListener.invoke()
    }

    override fun beforeShow() {
        super.beforeShow()

    }

    override fun dismiss() {
        Log.d(TAG,"dismiss")
        scaleXAnimator?.cancel()
        scaleYAnimator?.cancel()
        alphaAnimator?.cancel()
        super.dismiss()
        onDismissListener.invoke()
    }

}