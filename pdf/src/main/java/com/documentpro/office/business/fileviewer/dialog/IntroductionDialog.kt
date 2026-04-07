package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.text.HtmlCompat
import androidx.core.view.updateLayoutParams
import com.blankj.utilcode.util.BarUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.LayoutIntroductionDialogBinding
import com.documentpro.office.business.fileviewer.ui.setting.BusinessSettingActivity
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.FullScreenPopupView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IntroductionDialog(
    context: Context,
    private val onDismissListener: (isGetStarted: Boolean) -> Unit
) : FullScreenPopupView(context) {

    private var isGetStarted = false

    companion object {
        /**
         * 显示介绍弹框，返回是否点击了 Get Started
         * @return true 表示点击了 Get Started，false 表示按返回键关闭
         */
        suspend fun show(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
            val dialog = IntroductionDialog(context) { result ->
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
        return R.layout.layout_introduction_dialog
    }

    override fun onCreate() {
        super.onCreate()

        BusinessPointLog.logEvent("Introduce_Page_Show", mapOf())
        val binding = LayoutIntroductionDialogBinding.bind(popupImplView)

        // 设置标题文本，解析 HTML 标签（支持国际化）
        // 注意：需要在代码中设置默认文本颜色，而不是在 XML 中，否则会覆盖 HTML 颜色
        binding.tvTitle.setTextColor(android.graphics.Color.parseColor("#333333"))
        val htmlText = context.getString(R.string.introduction_title)
        val spannedText = HtmlCompat.fromHtml(
            htmlText,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.tvTitle.text = spannedText

        binding.tvTitle2.setTextColor(android.graphics.Color.parseColor("#333333"))
        val htmlText2 = context.getString(R.string.introduction_title_2)
        val spannedText2 = HtmlCompat.fromHtml(
            htmlText2,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.tvTitle2.text = spannedText2

        // Get Started 按钮点击事件
        binding.btnGetStarted.setOnClickListener {
            isGetStarted = true
            dismiss()
        }

        // Privacy Policy 点击事件
        binding.tvPrivacyPolicy.setOnClickListener {
            openPrivacyPolicy()
        }

    }

    override fun onDismiss() {
        super.onDismiss()
        // 无论哪种方式关闭，都调用 onDismissListener
        BusinessPointLog.logEvent((if(isGetStarted) "Introduce_Click_Allow" else "Introduce_Click_Deny"), mapOf())
        onDismissListener.invoke(isGetStarted)
    }

    /**
     * 打开隐私政策
     */
    private fun openPrivacyPolicy() {
        val privacyPolicyUrl = BusinessSettingActivity.PRIVACY_URL
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

