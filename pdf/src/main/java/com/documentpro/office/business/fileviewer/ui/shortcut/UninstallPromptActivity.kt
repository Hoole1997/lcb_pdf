package com.documentpro.office.business.fileviewer.ui.shortcut

import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.ActivityUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.base.BaseModel
import com.documentpro.office.business.fileviewer.databinding.ActivityUninstallPromptBinding
import com.documentpro.office.business.fileviewer.dialog.BusinessCardNativeAdDialog
import com.documentpro.office.business.fileviewer.ui.main.BusinessWorkspaceActivity
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.loadNative

/**
 * 卸载提示页Activity
 * 当用户通过快捷方式进入时显示
 */
class UninstallPromptActivity : BaseActivity<ActivityUninstallPromptBinding, BaseModel>() {


    companion object {
        private const val TAG = "UninstallPromptActivity"
        var onResultCallback: (() -> Unit)? = null

        @JvmStatic
        fun start(context: Context, call: (() -> Unit)? = null) {
            onResultCallback = call
            val starter = Intent(context, UninstallPromptActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun initBinding(): ActivityUninstallPromptBinding {
        return ActivityUninstallPromptBinding.inflate(layoutInflater)
    }

    override fun initModel(): BaseModel {
        return BaseModel()
    }

    override fun initView() {
        // 设置标题文本，高亮显示特定部分
        val fullText = getString(R.string.shortcut_uninstall_title)
        val boldText = getString(R.string.shortcut_uninstall_title_highlight)
        execAction_2 (fullText, boldText)
        // 设置按钮点击事件
        execAction_3 ()
        BusinessPointLog.logEvent("Uninstall_Show")
        execLoad_1 ()
        loadNative( binding.adContainer)
    }

    private fun execLoad_1(){

    }

    private fun execAction_2(fullText: String, boldText: String) {
        val spannableString = android.text.SpannableString(fullText)

        // 查找粗体文本在完整文本中的位置
        val startIndex = fullText.indexOf(boldText)
        if (startIndex != -1) {
            val endIndex = startIndex + boldText.length

            spannableString.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                startIndex,
                endIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableString.setSpan(
                android.text.style.ForegroundColorSpan(resources.getColor(R.color.font_bold, null)),
                startIndex,
                endIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.title.text = spannableString
    }

    private fun execAction_3() {
        binding.tvStill.setOnClickListener {
            BusinessCardNativeAdDialog.show(this) {
                loadInterstitial {
                    execDisplay_4 {
                        UninstallOptionActivity.start(this){
                            onResultCallback?.invoke()
//                    ActivityUtils.startActivity(BusinessWorkspaceActivity::class.java)
                            finish()
                        }
                    }
                }
            }


        }
        binding.tvAgree.setOnClickListener {
            onResultCallback?.invoke()
            ActivityUtils.startActivity(BusinessWorkspaceActivity::class.java)
            finish()
        }
    }

    private fun execDisplay_4(nextAction: () -> Unit) {
        if (isFinishing || isDestroyed) {
            nextAction.invoke()
            return
        }

        nextAction.invoke()
    }


    override fun initObserve() {
        // 无需观察数据变化
    }

    override fun initTag(): String {
        return TAG
    }
} 