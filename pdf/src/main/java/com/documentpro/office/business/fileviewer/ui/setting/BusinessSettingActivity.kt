package com.documentpro.office.business.fileviewer.ui.setting

import androidx.activity.viewModels
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityBusinessSettingBinding
import com.documentpro.office.business.fileviewer.ui.web.WebActivity
import android.net.Uri
import android.content.Intent
import com.blankj.utilcode.util.AppUtils
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.dialog.BusinessStoreScoreDialog
import com.documentpro.office.business.fileviewer.ui.language.LanguageActivity
import com.documentpro.office.business.fileviewer.utils.BusinessSplashForegroundController

class BusinessSettingActivity : BaseActivity<ActivityBusinessSettingBinding, BusinessSettingModel>() {

    companion object {
        private const val TAG = "BusinessSettingActivity"
        private const val FEEDBACK_EMAIL = "tk202507101@outlook.com"
        const val PRIVACY_URL = "https://devs343.com/privacy.html"
    }

    override fun initBinding(): ActivityBusinessSettingBinding {
        return ActivityBusinessSettingBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessSettingModel {
        return viewModels<BusinessSettingModel>().value
    }

    override fun initView() {
        useDefaultToolbar(binding.toolbar,"")

        binding.btnLanguage.setOnClickListener {
            BusinessPointLog.logEvent("Guide", mapOf("Guide" to 1))
            LanguageActivity.launch(this,true)
        }
        binding.btnFeedback.setOnClickListener {
            BusinessSplashForegroundController.markNextIntercept()
            // 跳转邮箱发送邮件
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$FEEDBACK_EMAIL")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // TODO: 处理没有邮箱应用的情况
            }
        }
        binding.btnStar.setOnClickListener {
            // 跳转谷歌商店评分
//            HCSDKManager.review()
            BusinessStoreScoreDialog.show(this)
        }
        binding.btnPrivacyPolicy.setOnClickListener {
            WebActivity.launch(this, PRIVACY_URL)
        }
//        binding.btnTermsOfService.setOnClickListener {
//            WebActivity.launch(this, getString(R.string.setting_user_agreement_url))
//        }
        binding.tvVersion.text = "v${AppUtils.getAppVersionName()}(${AppUtils.getAppVersionCode()})"
    }

    override fun initObserve() {

    }

    override fun initTag(): String {
        return TAG
    }
}
