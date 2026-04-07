package com.documentpro.office.business.fileviewer.ui.process

import android.content.Intent
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityProcessLoadingBinding
import com.documentpro.office.business.fileviewer.ui.clean.BusinessCleanModel
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.loadNative

class ProcessStartLoadingActivity : BaseActivity<ActivityProcessLoadingBinding, BusinessCleanModel>() {

    companion object {
        private const val TAG = "CleanActivity"

        fun start(context: FragmentActivity){
            val intent = Intent(context,ProcessStartLoadingActivity::class.java)
            context.startActivity(intent)
        }
    }


    override fun initBinding(): ActivityProcessLoadingBinding {
        return ActivityProcessLoadingBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessCleanModel {
        return viewModels<BusinessCleanModel>().value
    }

    override fun initView() {
        useDefaultToolbar(binding.toolbar, "")
        BarUtils.addMarginTopEqualStatusBarHeight(binding.toolbar)

        loadNative(binding.adContainer)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // 监听 Lottie 动画进度并同步到 ProgressBar
        setupLottieProgressListener()

    }

    /**
     * 设置 Lottie 动画进度监听器
     */
    private fun setupLottieProgressListener() {
        binding.lv.addAnimatorUpdateListener { animation ->
            // 获取动画进度 (0.0 - 1.0)
            val progress = animation.animatedValue as Float

            // 转换为百分比 (0-100)
            val percentage = (progress * 100).toInt()

            // 更新 ProgressBar
            binding.pbScanning.progress = percentage

            // 更新文字显示百分比
            binding.tvFileSize.text = percentage.toString()

            if(percentage == 100){
                ProcessDetailActivity.start(this@ProcessStartLoadingActivity)
                finish()
            }
        }
    }


    override fun initObserve() {

    }

    override fun initTag(): String {
        return TAG
    }



    override fun onDestroy() {
        super.onDestroy()
        // 移除 Lottie 动画监听器
        binding.lv.removeAllAnimatorListeners()
    }



    override fun closePage() {
       onBackPressed()
    }

    override fun useDefaultToolbar(toolbar: Toolbar, title: String) {
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarVisibility(this, false)
        setSupportActionBar(toolbar)
        supportActionBar?.setDefaultDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = title
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
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

}
