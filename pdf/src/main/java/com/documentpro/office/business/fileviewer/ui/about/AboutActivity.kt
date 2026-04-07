package com.documentpro.office.business.fileviewer.ui.about

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.blankj.utilcode.util.AppUtils
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityAboutBinding
import com.documentpro.office.business.fileviewer.ui.setting.BusinessSettingActivity
import com.documentpro.office.business.fileviewer.ui.web.WebActivity

class AboutActivity : BaseActivity<ActivityAboutBinding, AboutModel>() {

    companion object {
        private const val TAG = "AboutActivity"

        fun launch(context: Context) {
            val intent = Intent(context, AboutActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun initBinding(): ActivityAboutBinding {
        return ActivityAboutBinding.inflate(layoutInflater)
    }

    override fun initModel(): AboutModel {
        return viewModels<AboutModel>().value
    }

    override fun initView() {
        useDefaultToolbar(binding.toolbar, getString(com.documentpro.office.business.fileviewer.R.string.about_section_title))

        // 设置版本号
        binding.tvVersion.text = "Version:${AppUtils.getAppVersionName()}"

        // Privacy Policy 点击事件
        binding.tvPrivacyPolicy.setOnClickListener {
            WebActivity.launch(this, BusinessSettingActivity.PRIVACY_URL)
        }

        binding.btnUninstallInstructions.isVisible = false
        binding.adContainer.isVisible = false
    }

    override fun initObserve() {
        // 如果需要观察数据，可以在这里实现
    }

    override fun initTag(): String {
        return TAG
    }
}
