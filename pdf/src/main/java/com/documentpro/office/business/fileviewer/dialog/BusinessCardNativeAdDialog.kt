package com.documentpro.office.business.fileviewer.dialog

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.LayoutCardAdBusinessDialogBinding
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.android.common.bill.ads.ext.AdShowExt
import com.android.common.bill.ui.NativeAdStyleType

class BusinessCardNativeAdDialog(val context: FragmentActivity, val onDismissListener: (() -> Unit)?=null,) : CenterPopupView(context) {

    private var createTime = 0L
    private val MIN_DISPLAY_TIME = 300L // 最少显示300毫秒

    companion object {

        private var lastShowTime = 0L
        private const val SHOW_INTERVAL = 60 * 1000L // 一分钟间隔

        fun showOncePerMinute(context: FragmentActivity) {
            return
            val currentTime = System.currentTimeMillis()

            // 检查是否在一分钟内已经触发过
            if (currentTime - lastShowTime < SHOW_INTERVAL) {
                return
            }

            // 无缓存跳过
//            if(!AdShowExt.isNativeAdReady()){
//                return
//            }

            // 更新最后显示时间
            lastShowTime = currentTime

            XPopup.Builder(context)
                .hasNavigationBar(true)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .asCustom(BusinessCardNativeAdDialog(context))
                .show()
        }

        fun show(context: FragmentActivity,onDismiss:()->Unit) {
            return
            // 无缓存跳过
//            if(!AdShowExt.isNativeAdReady()){
//                return
//            }

            XPopup.Builder(context)
                .hasNavigationBar(true)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .asCustom(BusinessCardNativeAdDialog(context,onDismiss))
                .show()
        }

    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_card_ad_business_dialog
    }

    override fun onCreate() {
        super.onCreate()
        createTime = System.currentTimeMillis()
        val binding = LayoutCardAdBusinessDialogBinding.bind(popupImplView)

        binding.close.setOnClickListener {
            dismiss()
        }

        showLoading(binding)

        context.loadNative(binding.adsContainer, styleType = NativeAdStyleType.LARGE, call = {
            if(!it) {
                showLoading(binding)
                dismiss()
            } else{
                binding.close.isVisible = true
            }
        })
    }

    private fun showLoading(binding: LayoutCardAdBusinessDialogBinding) {
        val loadingView = LayoutInflater.from(context)
            .inflate(
                R.layout.layout_fullscreen_loading,
                binding.adsContainer,
                false
            )
        loadingView.setBackgroundColor(Color.TRANSPARENT)
        binding.adsContainer.removeAllViews()
        binding.adsContainer.addView(loadingView)
        binding.adsContainer.visibility = VISIBLE
    }

    override fun dismiss() {
        lifecycleScope.launch {
            val elapsedTime = System.currentTimeMillis() - createTime

            // 如果显示时间不足300ms，延迟到300ms
            if (elapsedTime < MIN_DISPLAY_TIME) {
                val remainingTime = MIN_DISPLAY_TIME - elapsedTime
                delay(remainingTime)
            }

            withContext(Dispatchers.Main) {
                super.dismiss()
                onDismissListener?.invoke()
            }
        }
    }

}
