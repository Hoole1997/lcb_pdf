package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import android.util.Log
import com.blankj.utilcode.util.SPUtils
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.LayoutPdfDefaultLaunchDialogBinding

class BusinessPDFDefaultLaunchDialog(
    context: Context,
    val onConfirmListener: () -> Unit,
    val onShowListener: () -> Unit,
    val onDismissListener: () -> Unit
) : CenterPopupView(context) {

    companion object {

        private var showCount = 0
        private const val TAG = "BusinessPDFDefaultLaunchDialog"

        fun isRejectPDFDefaultLaunch(): Boolean {
            if (!SPUtils.getInstance().getBoolean("isPDFDefaultLaunchDialog", true)) {
                return false
            }
            return SPUtils.getInstance().getBoolean("isRejectPDFDefaultLaunch", false)
        }

        fun checkShow(
            context: Context,
            onConfirmListener: () -> Unit,
            onShowListener: () -> Unit,
            onDismissListener: () -> Unit
        ) {
            if (showCount >= 3) {
                Log.d(TAG, "已达最大显示次数")
                onDismissListener.invoke()
                return
            }
            val isPDFDefaultLaunchDialog =
                SPUtils.getInstance().getBoolean("isPDFDefaultLaunchDialog", true)
            if (isPDFDefaultLaunchDialog) {
                show(context, onConfirmListener, onShowListener, onDismissListener)
            } else {
                onDismissListener.invoke()
            }
        }

        fun show(
            context: Context,
            onConfirmListener: () -> Unit,
            onShowListener: () -> Unit,
            onDismissListener: () -> Unit
        ) {
            XPopup.Builder(context)
                .moveUpToKeyboard(true)
                .asCustom(
                    BusinessPDFDefaultLaunchDialog(
                        context,
                        onConfirmListener,
                        onShowListener, onDismissListener
                    )
                )
                .show()
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_pdf_default_launch_dialog
    }

    override fun onCreate() {
        super.onCreate()
        showCount++
        val binding = LayoutPdfDefaultLaunchDialogBinding.bind(popupImplView)
        onShowListener.invoke()

        binding.btnConfirm.setOnClickListener {
            SPUtils.getInstance().put("isPDFDefaultLaunchDialog", false)
            onConfirmListener.invoke()
            dismiss()
        }
        binding.btnConfirm1.setOnClickListener {
            SPUtils.getInstance().put("isPDFDefaultLaunchDialog", false)
            onConfirmListener.invoke()
            dismiss()
        }
        binding.root.setOnClickListener {
            SPUtils.getInstance().put("isPDFDefaultLaunchDialog", false)
            onConfirmListener.invoke()
            dismiss()
        }
        binding.btnConfirm2.setOnClickListener {
            SPUtils.getInstance().put("isPDFDefaultLaunchDialog", false)
            onConfirmListener.invoke()
            dismiss()
        }
    }

    override fun dismiss() {
        super.dismiss()
        val isPDFDefaultLaunchDialog = SPUtils.getInstance().getBoolean("isPDFDefaultLaunchDialog", true)
        if (isPDFDefaultLaunchDialog) {
            SPUtils.getInstance().put("isRejectPDFDefaultLaunch", true)
        }
        onDismissListener.invoke()
    }

}