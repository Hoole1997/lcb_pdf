package com.documentpro.office.business.fileviewer.ui.process

import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.StringUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityProcessSuccessBinding
import com.documentpro.office.business.fileviewer.ui.clean.CleanStartLoadingActivity
import com.documentpro.office.business.fileviewer.ui.success.BusinessSuccessFunction
import com.documentpro.office.business.fileviewer.ui.success.BusinessSuccessFunctionAdapter
import com.documentpro.office.business.fileviewer.ui.success.BusinessSuccessModel
import com.documentpro.office.business.fileviewer.ui.tool.ToolType
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.android.common.bill.ui.NativeAdStyleType

class ProcessSuccessActivity : BaseActivity<ActivityProcessSuccessBinding, BusinessSuccessModel>() {

    companion object {
        private const val TAG = "SuccessActivity"
    }

    val functionList = buildList {
        add(
            BusinessSuccessFunction(
                toolType = ToolType.JUNK_CLEANER,
                iconRes = R.mipmap.tool_file_clean,
                title = StringUtils.getString(R.string.clean_junk_clean),
                desc = StringUtils.getString(R.string.process_manager_clean_tip),
                btnText = StringUtils.getString(R.string.process_manager_clean_now)
            )
        )
    }


    override fun initBinding(): ActivityProcessSuccessBinding {
        return ActivityProcessSuccessBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessSuccessModel {
        return viewModels<BusinessSuccessModel>().value
    }

    override fun initView() {
        useDefaultToolbar(binding.toolbar,"")
        BarUtils.addMarginTopEqualStatusBarHeight(binding.toolbar)

        val adapter = BusinessSuccessFunctionAdapter(functionList, ::execAction_1)
        binding.rvFunction.adapter = adapter
        binding.res.text = StringUtils.getString(R.string.process_manager_suc_tip,"${intent.getIntExtra("processCount",0)}")

        // 添加成功图标的缩放弹跳动画
        startSuccessIconAnimation()

        execLoad_2 ()
        loadNative(binding.adContainer, styleType = NativeAdStyleType.LARGE)
    }

    /**
     * 启动成功图标的缩放弹跳动画
     */
    private fun startSuccessIconAnimation() {
        binding.ivSuc.apply {
            // 设置初始状态为缩小
            scaleX = 0f
            scaleY = 0f
            alpha = 0f

            // 启动动画
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(OvershootInterpolator(2f))
                .setStartDelay(100)
                .start()
        }
    }

    override fun initObserve() {

    }

    override fun initTag(): String {
        return TAG
    }

    private fun execAction_1(successFunction: BusinessSuccessFunction) {
        // 过滤掉广告占位符
        if (successFunction.isAd()) {
            return
        }

        loadInterstitial {
            when(successFunction.toolType) {
                ToolType.JUNK_CLEANER -> {
                    ActivityUtils.startActivity(CleanStartLoadingActivity::class.java)

                }
                else -> {}
            }
            finish()
        }


    }

    private fun execLoad_2() {

    }

    override fun initWindowPadding() {
        findViewById<ViewGroup>(R.id.main)?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
