package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import android.view.View
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.LayoutCleanSuccessDialogBinding

class BusinessCleanSuccessDialog(context: Context,val onAnimationFinishedListener:() -> Unit) : CenterPopupView(context) {

    companion object {

        fun show(context: Context,onAnimationFinishedListener:() -> Unit) {
            XPopup.Builder(context)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .asCustom(BusinessCleanSuccessDialog(context,onAnimationFinishedListener))
                .show()
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_clean_success_dialog
    }

    override fun onCreate() {
        super.onCreate()
        val binding = LayoutCleanSuccessDialogBinding.bind(popupImplView)

        binding.ivCleanIng.postDelayed({
            binding.ivCleanSuccess.visibility = View.VISIBLE
            binding.ivCleanSuccess.startAnimation()
            binding.ivCleanSuccess.postDelayed({
                onAnimationFinishedListener.invoke()
            },1500)
        },5000)
    }

}