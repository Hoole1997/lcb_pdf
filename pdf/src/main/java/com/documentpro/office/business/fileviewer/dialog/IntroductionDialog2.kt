package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import androidx.core.text.HtmlCompat
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.LayoutIntroductionDialog2Binding
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.FullScreenPopupView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IntroductionDialog2(
    context: Context,
    private val onDismissListener: (isContinue: Boolean) -> Unit
) : FullScreenPopupView(context) {

    private var isContinue = false

    companion object {
        /**
         * 显示介绍弹框，返回是否点击了 Continue
         * @return true 表示点击了 Continue，false 表示点击了 Not now 或按返回键关闭
         */
        suspend fun show(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
            val dialog = IntroductionDialog2(context) { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            XPopup.Builder(context)
                .hasNavigationBar(false)
                .hasStatusBar(false)
                .dismissOnBackPressed(true)
                .dismissOnTouchOutside(false)
                .enableDrag(false)
                .asCustom(dialog)
                .show()

            continuation.invokeOnCancellation {
                if (dialog.isShow) {
                    dialog.dismiss()
                }
            }
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_introduction_dialog_2
    }

    override fun onCreate() {
        super.onCreate()

        BusinessPointLog.logEvent("Introduce2_Page_Show", mapOf())
        val binding = LayoutIntroductionDialog2Binding.bind(popupImplView)

        // 设置标题文本，解析 HTML 标签
        binding.tvTitle.setTextColor(android.graphics.Color.parseColor("#333333"))
        val htmlText = context.getString(R.string.introduction_title_3)
        val spannedText = HtmlCompat.fromHtml(
            htmlText,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.tvTitle.text = spannedText


        // Continue 按钮点击事件
        binding.btnContinue.setOnClickListener {
            isContinue = true
            dismiss()
        }

        // Not now 点击事件
        binding.tvNotNow.setOnClickListener {
            isContinue = false
            dismiss()
        }
    }

    override fun onDismiss() {
        super.onDismiss()
        BusinessPointLog.logEvent(
            if (isContinue) "Introduce2_Click_Continue" else "Introduce2_Click_NotNow",
            mapOf()
        )
        onDismissListener.invoke(isContinue)
    }
}
