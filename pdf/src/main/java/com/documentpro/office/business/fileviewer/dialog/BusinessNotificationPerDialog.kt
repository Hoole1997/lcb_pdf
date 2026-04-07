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
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.FullScreenPopupView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.LayoutPermissionDialogBinding
import com.documentpro.office.business.fileviewer.databinding.LayoutPermissionNotiDialogBinding

class BusinessNotificationPerDialog(
    context: Context,
    val onConfirmListener: () -> Unit,
    val onDismissListener: () -> Unit,
) : FullScreenPopupView(context) {

    companion object {
        fun show(
            context: Context,
            onConfirmListener: () -> Unit,
            onDismissListener: () -> Unit,
        ) {
            XPopup.Builder(context)
                .hasNavigationBar(false)
                .hasStatusBar(false)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .asCustom(
                    BusinessNotificationPerDialog(
                        context,
                        onConfirmListener,
                        onDismissListener,
                    )
                )
                .show()
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_permission_noti_dialog
    }

    override fun onCreate() {
        super.onCreate()
        val binding = LayoutPermissionNotiDialogBinding.bind(popupImplView)

        binding.ivClose.setOnClickListener {
            dismiss()
            onDismissListener.invoke()
        }
        binding.btnConfirm.setOnClickListener {
            dismiss()
            onConfirmListener.invoke()
        }
    }
}