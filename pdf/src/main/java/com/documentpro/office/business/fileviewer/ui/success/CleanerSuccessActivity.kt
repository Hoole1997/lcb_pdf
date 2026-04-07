package com.documentpro.office.business.fileviewer.ui.success

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.StringUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivitySuccessBinding
import com.documentpro.office.business.fileviewer.ui.clean.CleanStartLoadingActivity
import com.documentpro.office.business.fileviewer.ui.office.OfficeViewActivity
import com.documentpro.office.business.fileviewer.ui.pdf.BusinessLockActivity
import com.documentpro.office.business.fileviewer.ui.pdf.PDFScannerActivity
import com.documentpro.office.business.fileviewer.ui.process.ProcessStartLoadingActivity
import com.documentpro.office.business.fileviewer.ui.tool.ToolType
import com.documentpro.office.business.fileviewer.utils.BusinessSplashForegroundController
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.documentpro.office.business.fileviewer.utils.queryfile.fileInfoFromUri
import com.android.common.bill.ui.NativeAdStyleType

class CleanerSuccessActivity : BaseActivity<ActivitySuccessBinding, BusinessSuccessModel>() {

    companion object {
        private const val TAG = "SuccessActivity"
    }

    val functionList = buildList {
        add(BusinessSuccessFunction(
            toolType = ToolType.PROCES_MGR,
            iconRes = R.drawable.tool_progress_manager,
            title = StringUtils.getString(R.string.process_manager_title),
            desc = StringUtils.getString(R.string.process_manager_desc),
            btnText = StringUtils.getString(R.string.process_manager_scan)
        ))
//        add(BusinessSuccessFunction(
//            toolType = ToolType.IMPORT_FILE,
//            iconRes = R.mipmap.tool_import_file,
//            title = StringUtils.getString(R.string.success_import_pdf_title),
//            desc = StringUtils.getString(R.string.success_import_pdf_desc),
//            btnText = StringUtils.getString(R.string.success_import_pdf_btn)
//        ))
//        add(BusinessSuccessFunction.createAdPlaceholder())
//        add(BusinessSuccessFunction(
//            toolType = ToolType.SCAN_PDF,
//            iconRes = R.mipmap.tool_scan_pdf,
//            title = StringUtils.getString(R.string.success_scan_pdf_title),
//            desc = StringUtils.getString(R.string.success_scan_pdf_desc),
//            btnText = StringUtils.getString(R.string.success_scan_pdf_btn)
//        ))
//
//        add(BusinessSuccessFunction(
//            toolType = ToolType.LOCK_PDF,
//            iconRes = R.mipmap.tool_lock_pdf,
//            title = StringUtils.getString(R.string.success_lock_pdf_title),
//            desc = StringUtils.getString(R.string.success_lock_pdf_desc),
//            btnText = StringUtils.getString(R.string.success_lock_pdf_btn)
//        ))
//        add(BusinessSuccessFunction(
//            toolType = ToolType.UNLOCK_PDF,
//            iconRes = R.mipmap.tool_unlock_pdf,
//            title = StringUtils.getString(R.string.success_unlock_pdf_title),
//            desc = StringUtils.getString(R.string.success_unlock_pdf_desc),
//            btnText = StringUtils.getString(R.string.success_unlock_pdf_btn)
//        ))
//        add(BusinessSuccessFunction(
//            toolType = ToolType.FILE_CLEAN,
//            iconRes = R.mipmap.tool_file_clean,
//            title = StringUtils.getString(R.string.success_clean_title),
//            desc = StringUtils.getString(R.string.success_clean_desc),
//            btnText = StringUtils.getString(R.string.success_clean_btn)
//        ))
        // 可继续添加更多功能
    }

    private val mField_1 = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val fileInfo = fileInfoFromUri(this, it)
            fileInfo?.let { info ->
                OfficeViewActivity.launch(this, info)
            }
        }
    }

    private val mField_2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
        }
    }

    override fun initBinding(): ActivitySuccessBinding {
        return ActivitySuccessBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessSuccessModel {
        return viewModels<BusinessSuccessModel>().value
    }

    override fun initView() {
        useDefaultToolbar(binding.toolbar,"")
        BarUtils.addMarginTopEqualStatusBarHeight(binding.toolbar)

        val adapter = BusinessSuccessFunctionAdapter(functionList,::execAction_1)
        binding.rvFunction.adapter = adapter
        binding.res.text = StringUtils.getString(R.string.clean_junk_clean_suc,intent.getStringExtra("size")+intent.getStringExtra("unit"))

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
                .setInterpolator(android.view.animation.OvershootInterpolator(2f))
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
                ToolType.LOCK_PDF -> BusinessLockActivity.launch(this,true)
                ToolType.UNLOCK_PDF -> BusinessLockActivity.launch(this,false)
                ToolType.IMPORT_FILE -> {
                    BusinessSplashForegroundController.markNextIntercept()
                    mField_1.launch(arrayOf("application/pdf"))
                }
                ToolType.SCAN_PDF -> {
                    BusinessSplashForegroundController.markNextIntercept()
                    val intent = Intent(this, PDFScannerActivity::class.java)
                    mField_2.launch(intent)
                }
                ToolType.IMAGE_PDF -> {
                    val intent = Intent(this, PDFScannerActivity::class.java)
                    mField_2.launch(intent)
                }
                ToolType.FILE_CLEAN -> {
                    ActivityUtils.startActivity(CleanStartLoadingActivity::class.java)
                }
                ToolType.PRINT_PDF -> {

                }
                ToolType.ADD_MODEL -> {

                }

                ToolType.MERGE_PDF -> {

                }
                ToolType.PROCES_MGR -> {
                    ProcessStartLoadingActivity.start(this)
                }
                else->{}
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
