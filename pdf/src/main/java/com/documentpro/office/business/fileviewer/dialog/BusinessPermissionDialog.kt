package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.ContextThemeWrapper
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.FullScreenPopupView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.LayoutPermissionDialogBinding

class BusinessPermissionDialog(
    context: Context,
    val onConfirmListener: () -> Unit,
    val onDismissListener: () -> Unit,
    val onRejectListener: () -> Unit,
    val onShowListener: (() -> Unit?)? = null
) : FullScreenPopupView(context) {

    companion object {
        fun show(
            context: Context,
            onConfirmListener: () -> Unit,
            onDismissListener: () -> Unit,
            onRejectListener: () -> Unit,
            onShowListener: (() -> Unit?)? = null
        ) {
            val themedContext = ContextThemeWrapper(context, R.style.Theme_PDFViewer)
            XPopup.Builder(context)
                .hasNavigationBar(false)
                .hasStatusBar(false)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .hasShadowBg(true)
                .asCustom(
                    BusinessPermissionDialog(
                        themedContext,
                        onConfirmListener,
                        onDismissListener,
                        onRejectListener,
                        onShowListener
                    )
                )
                .show()
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_permission_dialog
    }

    override fun onCreate() {
        super.onCreate()
        val binding = LayoutPermissionDialogBinding.bind(popupImplView)
        BusinessPointLog.logEvent("Guide", mapOf("Guide" to 7))
        onShowListener?.invoke()


        binding.tvAfter.setOnClickListener {
            onRejectListener.invoke()
            dismiss()
        }
        binding.btnConfirm.setOnClickListener {
            onConfirmListener.invoke()
            dismiss()
        }
    }

    override fun dismiss() {
        super.dismiss()
        onDismissListener.invoke()
    }
}
