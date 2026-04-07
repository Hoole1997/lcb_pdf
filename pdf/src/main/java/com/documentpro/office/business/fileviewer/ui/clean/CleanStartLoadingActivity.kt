package com.documentpro.office.business.fileviewer.ui.clean

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Environment
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityCleanLoadingBinding
import com.documentpro.office.business.fileviewer.utils.BusinessCleanUtils
import com.documentpro.office.business.fileviewer.utils.BusinessPermissionDialogUtils
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.documentpro.office.business.fileviewer.utils.toFileSizeNumString
import com.documentpro.office.business.fileviewer.utils.toFileSizeUnitString
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

class CleanStartLoadingActivity : BaseActivity<ActivityCleanLoadingBinding, BusinessCleanModel>() {
    private var mField_1: ValueAnimator? = null
    private var mField_2: Long = 0

    companion object {
        private const val TAG = "CleanActivity"
    }

    // 模拟的系统文件夹路径
    private val mField_3 = listOf(
        Environment.getExternalStorageDirectory().absolutePath + "/Android/data",
        Environment.getExternalStorageDirectory().absolutePath + "/Android/obb",
        Environment.getExternalStorageDirectory().absolutePath + "/DCIM",
        Environment.getExternalStorageDirectory().absolutePath + "/Pictures",
        Environment.getExternalStorageDirectory().absolutePath + "/Download",
        Environment.getExternalStorageDirectory().absolutePath + "/Documents",
        Environment.getExternalStorageDirectory().absolutePath + "/Movies",
        Environment.getExternalStorageDirectory().absolutePath + "/Music",
        Environment.getExternalStorageDirectory().absolutePath + "/Alarms",
        Environment.getExternalStorageDirectory().absolutePath + "/Notifications",
        Environment.getExternalStorageDirectory().absolutePath + "/Ringtones",
        Environment.getExternalStorageDirectory().absolutePath + "/Podcasts"
    )

    override fun initBinding(): ActivityCleanLoadingBinding {
        return ActivityCleanLoadingBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessCleanModel {
        return viewModels<BusinessCleanModel>().value
    }

    override fun initView() {
        useDefaultToolbar(binding.toolbar, "")
        BarUtils.addMarginTopEqualStatusBarHeight(binding.toolbar)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closePage()
            }
        })

        loadNative(binding.adContainer)

        val cleanAction = {
            // 自动开始扫描
            execAction_4()
        }
        if (XXPermissions.isGrantedPermissions(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
            cleanAction.invoke()
        } else {
            BusinessPermissionDialogUtils.showFilePermissionDialog(
                this,
                needDialog = true,
                needStartPermissionPage = false,
                nextAction = {
                    if (it) {
                        BusinessPointLog.logEvent(
                            "All_File_Success", mapOf(
                                "File_Request_Position" to 3
                            )
                        )
                        cleanAction.invoke()
                    } else {
                        cleanAction.invoke()
                        ToastUtils.showShort(getString(R.string.toast_get_permission_failed))
                    }
                },
                onConfirmListener = {
                    BusinessPointLog.logEvent(
                        "All_File_Click", mapOf(
                            "File_Request_Position" to 3
                        )
                    )
                },
                onShowListener = {
                    BusinessPointLog.logEvent(
                        "All_File_Request_Show", mapOf(
                            "File_Request_Position" to 3
                        )
                    )
                }
            )
        }
        execLoad_1()
    }

    private fun execLoad_1() {

    }

    override fun initObserve() {
        lifecycleScope.launch {
            model.cleanGroups.collectLatest { groups ->

            }
        }

        lifecycleScope.launch {
            model.totalSize.collectLatest { size ->
                binding.tvFileSize.text = size.toFileSizeNumString()
                binding.tvFileSizeUnit.text = size.toFileSizeUnitString()
                // 更新按钮文字
                if (!model.isScanning.value) {

                }
            }
        }

        lifecycleScope.launch {
            model.isScanning.collectLatest { isScanning ->

                if (isScanning) {
                    execAction_2()
                } else {
                    execAction_3()
                }
            }
        }

        lifecycleScope.launch {
            model.scanningFolder.collectLatest { folder ->
                binding.tvScanningState.text = folder
            }
        }
    }

    override fun initTag(): String {
        return TAG
    }

    private fun execAction_2() {
        execAction_3()
        mField_1 = ValueAnimator.ofInt(0, 100).apply {
            this.duration = mField_2
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Int
                binding.pbScanning.progress = progress
                model.updateScanProgress(progress)
                if(progress == 100){
                    nextPage()
                }
            }
            start()
        }
    }

    private fun execAction_3() {
        mField_1?.cancel()
        mField_1 = null
    }

    private fun nextPage() {
        startActivity(Intent(this, CleanActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        execAction_3()
        binding.colorGradientView.stopGradientAnimation()
    }

    private fun execAction_4() {
        BusinessPointLog.logEvent("Clean_Start")
        lifecycleScope.launch {
            // 在开始扫描前生成随机时间
            mField_2 = Random.nextInt(5000, 8000).toLong()
            
            // 启动颜色渐变动画
            binding.colorGradientView.startGradientAnimation(mField_2)
            
            model.startScan()

            withContext(Dispatchers.IO) {
                val realScan = {
                    // 实际扫描文件
                    val contentResolver = applicationContext.contentResolver
                    BusinessCleanUtils.scanObsoleteApks(contentResolver) { item ->
                        model.addCleanItem(item)
                    }
                    //增加5-10个空文件夹
                    for (i in 0..Random.nextInt(5, 10)) {
                        val folder = File(Environment.getExternalStorageDirectory(), "temp$i")
                        model.addCleanItem(
                            CleanItem(
                                (i + 12345).toLong() * 123456,
                                "$i.temp",
                                "${Environment.getExternalStorageDirectory()}/temp$i",
                                (200 + i).toLong(),
                                CleanType.TEMP,
                                true
                            )
                        )
                    }
                    BusinessCleanUtils.scanTempFiles(contentResolver) { item ->
                        model.addCleanItem(item)
                    }
                    BusinessCleanUtils.scanJunkFiles(applicationContext, contentResolver) { item ->
                        model.addCleanItem(item)
                    }
                    BusinessCleanUtils.scanLogFiles(contentResolver) { item ->
                        model.addCleanItem(item)
                    }
                }
                // 模拟扫描系统文件夹
                simulateSystemScan(
                    scanMiddleAction = {
                        realScan.invoke()
                    },
                    scanEndAction = {
                        model.stopScan()
                    })

            }
        }
    }

    private suspend fun simulateSystemScan(
        scanMiddleAction:()->Unit,
        scanEndAction: () -> Unit) {
        val folderCount = mField_3.size
        val delayPerFolder = mField_2 / folderCount

        mField_3.forEachIndexed { index, folder ->
            withContext(Dispatchers.Main) {
                model.updateScanningFolder(folder)
            }
            delay(delayPerFolder)
            if(index == 5){
                scanMiddleAction.invoke()
            }
        }
        scanEndAction.invoke()
    }

    private fun execLoad_5() {

    }

    private fun execDisplay_6(adPos: String, nextAction: () -> Unit) {
        nextAction.invoke()
    }

    override fun closePage() {
        if (model.isScanning.value) {
            BusinessPointLog.logEvent(
                "Clean_Finish", mapOf(
                    "Clean_Result" to "Back"
                )
            )
        }
        finish()
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
            execDisplay_6(
                adPos = "IV_CleanBack",
                nextAction = {
                    closePage()
                }
            )
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