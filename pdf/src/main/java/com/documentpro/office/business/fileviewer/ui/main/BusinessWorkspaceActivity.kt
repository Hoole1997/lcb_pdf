package com.documentpro.office.business.fileviewer.ui.main

import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.android.common.bill.ads.PreloadController
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityWorkspaceContainerBinding
import com.documentpro.office.business.fileviewer.ui.splash.DemoFileCopyController
import com.documentpro.office.business.fileviewer.utils.LauncherApplyTrack
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import kotlinx.coroutines.launch

class BusinessWorkspaceActivity : BaseActivity<ActivityWorkspaceContainerBinding, BusinessMainModel>() {

    companion object {
        private const val TAG = "BusinessWorkspaceActivity"
    }

    private var workspaceFragment: BusinessWorkspaceFragment? = null
    private lateinit var demoFileCopyController: DemoFileCopyController

    override fun initBinding(): ActivityWorkspaceContainerBinding {
        return ActivityWorkspaceContainerBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessMainModel {
        return viewModels<BusinessMainModel>().value
    }

    override fun initView() {
        LauncherApplyTrack.appMainAcTrack(this)
        demoFileCopyController = DemoFileCopyController(this)
        // 设置状态栏
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .hideBar(BarHide.FLAG_HIDE_NAVIGATION_BAR)
            .init()

        // 加载 Fragment
        workspaceFragment = BusinessWorkspaceFragment()
        supportFragmentManager.commit {
            replace(R.id.fragment_container, workspaceFragment!!)
            runOnCommit {
            }
        }
        copyDemoFilesIfNeeded()
        PreloadController.preloadAll(this)
    }

    private fun copyDemoFilesIfNeeded() {
        lifecycleScope.launch {
            if (demoFileCopyController.copyDemoFiles()) {
                model.refreshAllDataSources()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        workspaceFragment?.handleNewIntent()
    }

    override fun initWindowPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun initObserve() {
        // Fragment 中已经处理了观察逻辑
    }

    override fun initTag(): String {
        return TAG
    }
}
