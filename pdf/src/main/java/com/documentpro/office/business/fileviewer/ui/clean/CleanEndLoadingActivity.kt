package com.documentpro.office.business.fileviewer.ui.clean

import android.content.Intent
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.BarUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityEndLoadingBinding
import com.documentpro.office.business.fileviewer.ui.success.CleanerSuccessActivity
import com.documentpro.office.business.fileviewer.utils.loadInterstitial

class CleanEndLoadingActivity : BaseActivity<ActivityEndLoadingBinding, BusinessCleanModel>() {

    companion object {
        private const val TAG = "CleanActivity"

        fun start(context: FragmentActivity,
                  size: String,unit: String){
            val intent = Intent(context,CleanEndLoadingActivity::class.java)
            intent.putExtra("size",size)
            intent.putExtra("unit",unit)
            context.startActivity(intent)
        }
    }


    override fun initBinding(): ActivityEndLoadingBinding {
        return ActivityEndLoadingBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessCleanModel {
        return viewModels<BusinessCleanModel>().value
    }

    override fun initView() {
        useDefaultToolbar(binding.toolbar, "")
        BarUtils.addMarginTopEqualStatusBarHeight(binding.toolbar)


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })

        binding.colorGradientView.startGradientAnimation(4000L)

        binding.tvFileSize.text = intent.getStringExtra("size")
        binding.tvFileSizeUnit.text = intent.getStringExtra("unit")

        binding.colorGradientView.postDelayed({
            loadInterstitial{
                startActivity(Intent(this,CleanerSuccessActivity::class.java).apply {
                    intent.extras?.let {
                        putExtras(it)
                    }
                })
                finish()
            }}, 4000L)
    }


    override fun initObserve() {

    }

    override fun initTag(): String {
        return TAG
    }



    override fun onDestroy() {
        super.onDestroy()
        binding.colorGradientView.stopGradientAnimation()
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