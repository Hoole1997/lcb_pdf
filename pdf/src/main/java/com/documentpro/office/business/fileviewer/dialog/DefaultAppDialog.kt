package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import android.graphics.Color
import androidx.core.text.HtmlCompat
import com.blankj.utilcode.util.SPUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.LayoutDefaultAppDialogBinding
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.impl.FullScreenPopupView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DefaultAppDialog(
    context: Context,
    private val onDismissListener: (isTryItNow: Boolean) -> Unit
) : BottomPopupView(context) {

    private var isTryItNow = false
    private var showCount = 0

    companion object {
        private const val SP_KEY_SHOW_COUNT = "default_app_dialog_show_count"

        /**
         * 获取当前显示次数
         */
        fun getShowCount(): Int {
            return SPUtils.getInstance().getInt(SP_KEY_SHOW_COUNT, 0)
        }

        /**
         * 增加显示次数并返回新值
         */
        private fun incrementShowCount(): Int {
            val newCount = getShowCount() + 1
            SPUtils.getInstance().put(SP_KEY_SHOW_COUNT, newCount, true)
            return newCount
        }

        /**
         * 显示默认应用授权弹窗，返回是否点击了 Try it now
         * @return true 表示点击了 Try it now，false 表示关闭弹窗
         */
        suspend fun show(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
            val dialog = DefaultAppDialog(context) { result ->
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

            // 处理取消情况
            continuation.invokeOnCancellation {
                // 如果协程被取消，确保弹框被关闭
                if (dialog.isShow) {
                    dialog.dismiss()
                }
            }
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_default_app_dialog
    }

    override fun onCreate() {
        super.onCreate()
        // 增加显示次数并记录
        showCount = incrementShowCount()
        BusinessPointLog.logEvent("Set_Default_Popup_Show", mapOf("count" to showCount))
        val binding = LayoutDefaultAppDialogBinding.bind(popupImplView)

        // 设置标题文本，解析 HTML 标签
        // 注意：需要在代码中设置默认文本颜色，否则会覆盖 HTML 颜色
        binding.tvTitle.setTextColor(Color.parseColor("#333333"))
        val htmlText = context.getString(R.string.default_app_dialog_title)
        val spannedText = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.tvTitle.text = spannedText

        // 关闭按钮点击事件
        binding.ivClose.setOnClickListener {
            isTryItNow = false
            dismiss()
        }

        // Try it now 按钮点击事件
        binding.btnTryItNow.setOnClickListener {
            isTryItNow = true
            dismiss()
        }
    }

    override fun onDismiss() {
        super.onDismiss()
        // 无论哪种方式关闭，都调用 onDismissListener
        val eventName = if (isTryItNow) "Set_Default_Popup_Allow" else "Set_Default_Popup_Deny"
        BusinessPointLog.logEvent(eventName, mapOf("count" to showCount))
        onDismissListener.invoke(isTryItNow)
    }
}

