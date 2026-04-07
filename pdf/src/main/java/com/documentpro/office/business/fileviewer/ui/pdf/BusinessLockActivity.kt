package com.documentpro.office.business.fileviewer.ui.pdf

import android.content.Context
import android.content.Intent
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityBusinessLockBinding
import com.documentpro.office.business.fileviewer.ui.home.BusinessFileListFragment
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.utils.loadInterstitial

class BusinessLockActivity : BaseActivity<ActivityBusinessLockBinding, BusinessMainModel>() {

    companion object {
        private const val TAG = "BusinessLockActivity"
        private const val PARAM_LOCK = "param_lock"
        private const val PARAM_PRINT = "param_print"
        fun launch(context: Context,isLock: Boolean) {
            context.startActivity(Intent().apply {
                setClass(context, BusinessLockActivity::class.java)
                putExtra(PARAM_LOCK,isLock)
            })
        }

        fun launchFromPrint(context: Context) {
            context.startActivity(Intent().apply {
                setClass(context, BusinessLockActivity::class.java)
                putExtra(PARAM_LOCK,true)
                putExtra(PARAM_PRINT,true)
            })
        }
    }

    private var mField_1 = false
    private var mField_2 = false

    override fun initBinding(): ActivityBusinessLockBinding {
        return ActivityBusinessLockBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessMainModel {
        return viewModels<BusinessMainModel>().value
    }

    override fun finish() {
        super.finish()
    }

    override fun initView() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        mField_1 = intent.getBooleanExtra(PARAM_LOCK,false)
        mField_2 = intent.getBooleanExtra(PARAM_PRINT,false)

        useDefaultToolbar(binding.toolbar,"")
        binding.tvTitle.text = if (mField_2) {
            getString(R.string.tool_print_pdf)
        } else {
            if (mField_1) getString(R.string.tool_lock_pdf) else getString(R.string.tool_unlock_pdf)
        }

        val fragment = if (mField_2) {
            BusinessFileListFragment.newInstanceFromPrint()
        } else {
            // 对于锁定操作：如果是锁定模式，则显示未锁定的文件；如果是解锁模式，则显示已锁定的文件
            BusinessFileListFragment.newInstanceFromPDF(if (mField_1) 2 else 1)
        }
        supportFragmentManager.beginTransaction().replace(R.id.fl_container,fragment).commit()
    }

    override fun initObserve() {

    }

    override fun initTag(): String {
        return TAG
    }
}