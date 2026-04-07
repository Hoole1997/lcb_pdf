package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.LayoutExitBusinessDialogBinding
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.ethanhua.skeleton.ViewSkeletonScreen
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.core.CenterPopupView
import com.android.common.bill.ui.NativeAdStyleType

class ExitBusinessDialog(val context: FragmentActivity) : BottomPopupView(context) {

    companion object {
        private const val TAG = "ExitBusinessDialog"

        fun show(context: FragmentActivity) {
            XPopup.Builder(context)
                .hasNavigationBar(true)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .asCustom(ExitBusinessDialog(context))
                .show()
        }
    }

    private lateinit var skeleton: ViewSkeletonScreen

    override fun getImplLayoutId(): Int {
        return R.layout.layout_exit_business_dialog
    }

    override fun onCreate() {
        super.onCreate()
        val binding = LayoutExitBusinessDialogBinding.bind(popupImplView)

        // 1. 为 rootLL 添加导航栏高度的 paddingBottom
        val navHeight = BarUtils.getNavBarHeight()
        binding.rootLL.post {
            binding.rootLL.setPadding(
                binding.rootLL.paddingLeft,
                binding.rootLL.paddingTop,
                binding.rootLL.paddingRight,
                binding.rootLL.paddingBottom + navHeight
            )
        }

        // 2. 骨架图默认显示在 ad_Container
        skeleton = ViewSkeletonScreen.Builder(binding.adContainer)
            .load(R.layout.layout_skeleton_ad)
            .shimmer(true)
            .show()

        context.loadNative(binding.adContainer, styleType = NativeAdStyleType.LARGE, call = {
            if(it) skeleton.hide()
        })

        binding.btnConfirm.setOnClickListener {
            ActivityUtils.startHomeActivity()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

}
