package com.documentpro.office.business.fileviewer.ui.pdf

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.TimeUtils
import com.documentpro.office.business.fileviewer.BuildConfig
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.adapter.BusinessPdfPageGridAdapter
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityPdfPageGridBinding
import com.documentpro.office.business.fileviewer.dialog.BusinessPDFSaveDialog
import com.documentpro.office.business.fileviewer.dialog.SaveMode
import com.documentpro.office.business.fileviewer.ui.pdf.model.BusinessPdfPageGridModel
import com.documentpro.office.business.fileviewer.ui.pdf.model.BusinessPdfPageInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo

/**
 * PDF页面九宫格展示Activity
 * 用于合并模式：展示选中PDF的所有页面，默认全选
 * 用于拆分模式：展示单个PDF的所有页面，默认不选
 */
class PdfPageGridActivity : BaseActivity<ActivityPdfPageGridBinding, BusinessPdfPageGridModel>() {

    companion object {
        private const val EXTRA_FILE_LIST = "extra_file_list"
        private const val EXTRA_MODE = "extra_mode"

        private const val TAG = "PdfPageGridActivity"

        fun launch(context: Context, fileList: ArrayList<BusinessFileInfo>, mode: Int) {
            context.startActivity(Intent(context, PdfPageGridActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_FILE_LIST, fileList)
                putExtra(EXTRA_MODE, mode)
            })
        }
    }

    private lateinit var pdfPageAdapter: BusinessPdfPageGridAdapter
    private var mField_1: Int = MergePdfActivity.MODE_MERGE
    private lateinit var fileList: ArrayList<BusinessFileInfo>

    override fun initBinding(): ActivityPdfPageGridBinding {
        return ActivityPdfPageGridBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessPdfPageGridModel {
        return viewModels<BusinessPdfPageGridModel>().value
    }

    override fun initView() {
        // 获取参数
mField_1 = intent.getIntExtra(EXTRA_MODE, MergePdfActivity.MODE_MERGE)
        fileList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_FILE_LIST, BusinessFileInfo::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_FILE_LIST) ?: arrayListOf()
        }

        // 设置工具栏标题
        val title = if (mField_1 == MergePdfActivity.MODE_MERGE) {
            getString(R.string.merge_pdf_title)
        } else {
            getString(R.string.split_pdf_title)
        }
        binding.tvTitle.text = title
        useDefaultToolbar(binding.toolbar, "")
        // 设置RecyclerView
        pdfPageAdapter = BusinessPdfPageGridAdapter(mField_1 == MergePdfActivity.MODE_MERGE) { pageInfo, isSelected ->
            // 页面选择状态改变回调
            onPageSelectionChanged(pageInfo, isSelected)
        }

        binding.rvPages.apply {
            layoutManager = GridLayoutManager(this@PdfPageGridActivity, 3) // 3列九宫格
            adapter = pdfPageAdapter
        }

        // 初始化底部按钮状态
        execAction_3 ()
        execAction_2 ()
        execAction_4 ()

        binding.btnAction.setOnClickListener {
            val selectedPages = pdfPageAdapter.getSelectedPages()
            if (selectedPages.isNotEmpty()) {
                execAction_5 (selectedPages)
            }
        }
        if (mField_1 == MergePdfActivity.MODE_MERGE) {
            binding.btnSelectAll.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.mipmap.ic_checkbox_selected,0)
        } else {
            binding.btnSelectAll.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.mipmap.ic_checkbox_unselected,0)
        }
        binding.btnSelectAll.setOnClickListener {
            val selectedCount = pdfPageAdapter.getSelectedCount()
            val totalCount = pdfPageAdapter.itemCount

            if (selectedCount == totalCount) {
                // 当前全部选中，执行取消全选
                pdfPageAdapter.selectAll(false)
                binding.btnSelectAll.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.mipmap.ic_checkbox_unselected,0)
            } else {
                // 当前未全选，执行全选
                pdfPageAdapter.selectAll(true)
                binding.btnSelectAll.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.mipmap.ic_checkbox_selected,0)
            }
            execAction_3 ()
            execAction_2 ()
            execAction_4 ()
        }

        // 加载PDF页面数据
        execLoad_1 ()
        execLoad_6 ()
        BusinessPointLog.logEvent(
            event = if (mField_1 == MergePdfActivity.MODE_MERGE) "MergePdf_PageGrid_Show" else "SplitPdf_PageGrid_Show"
        )
    }

    override fun initObserve() {
        model.pdfPagesEvent.observe(this) { pages ->
            pdfPageAdapter.submitList(pages)
            execAction_3 ()
            execAction_2 ()
//            updateSelectAllButton()
            execAction_4 ()
        }

        model.mergeResultEvent.observe(this) { fileInfo ->
            if (fileInfo != null) {
                BusinessPointLog.logEvent("MergePdf_Success")
            } else {
                BusinessPointLog.logEvent("MergePdf_Failed")
                // 显示错误提示
            }
            fileInfo?.let {
                BusinessDocumentActivity.launch(this,fileInfo)
                ActivityUtils.finishActivity(MergePdfActivity::class.java)
                finish()
            }
        }

        model.splitResultEvent.observe(this) { success ->
            if (success) {
                BusinessPointLog.logEvent("SplitPdf_Success")
                finish()
            } else {
                BusinessPointLog.logEvent("SplitPdf_Failed")
                // 显示错误提示
            }
        }
    }

    override fun initTag(): String {
        return TAG
    }

    /**
     * 加载PDF页面数据
     */
    private fun execLoad_1() {
        model.loadPdfPages(fileList,mField_1 == MergePdfActivity.MODE_MERGE)
    }

    /**
     * 页面选择状态改变回调
     */
    private fun onPageSelectionChanged(pageInfo: BusinessPdfPageInfo, isSelected: Boolean) {
        pageInfo.isSelected = isSelected
        execAction_3 ()
        execAction_2 ()
//        updateSelectAllButton()
        execAction_4 ()
    }

    /**
     * 更新选择信息显示
     */
    private fun execAction_2() {

    }

    /**
     * 更新底部按钮状态
     */
    private fun execAction_3() {
        val selectedCount = pdfPageAdapter.getSelectedPages().size
        
        binding.btnAction.isEnabled = selectedCount > 0
        binding.btnAction.text = if (mField_1 == MergePdfActivity.MODE_MERGE) {
            getString(R.string.action_merge)
        } else {
            getString(R.string.action_split)
        }
    }

    /**
     * 更新标题显示选择数量
     */
    private fun execAction_4() {
//        val selectedCount = pdfPageAdapter.getSelectedCount()
//        binding.tvTitle.text = "已选择 $selectedCount"
    }

    /**
     * 执行合并操作
     */
    private fun execAction_5(selectedPages: List<BusinessPdfPageInfo>) {
        if (mField_1 == MergePdfActivity.MODE_MERGE) {
            BusinessPointLog.logEvent("MergePdf_Execute", mapOf("pageCount" to selectedPages.size))
        } else {
            BusinessPointLog.logEvent("SplitPdf_Execute", mapOf("pageCount" to selectedPages.size))
        }
        BusinessPDFSaveDialog.show(
            context = this,
            currentFileName = "",
            customHint = "${if (mField_1 == MergePdfActivity.MODE_MERGE) "Merge" else "Split"}-${TimeUtils.millis2String(System.currentTimeMillis(),"yyyy-MM-dd-HH-mm-ss")}.pdf",
            saveMode = SaveMode.SAVE_AS_NEW_ONLY,
            onSaveListener = object :BusinessPDFSaveDialog.OnSaveListener {
                override fun onSave(fileName: String, saveAsNew: Boolean) {
                    model.mergePdfPages(fileName,selectedPages)
                }
                override fun onCancel() {}
            }
        )
    }

//    /**
//     * 执行拆分操作
//     */
//    private fun performSplit(selectedPages: List<BusinessPdfPageInfo>) {
//        model.mergePdfPages(selectedPages)
//    }

    private fun execLoad_6() {

    }
}