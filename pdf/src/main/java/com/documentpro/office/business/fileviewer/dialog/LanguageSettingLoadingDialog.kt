package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.documentpro.office.business.fileviewer.R

class LanguageSettingLoadingDialog(context: Context) : CenterPopupView(context) {

    companion object {
        fun show(context: Context): LanguageSettingLoadingDialog {
            val dialog = LanguageSettingLoadingDialog(context)
            XPopup.Builder(context)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .asCustom(dialog)
                .show()
            return dialog
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_loading_dialog
    }
}
