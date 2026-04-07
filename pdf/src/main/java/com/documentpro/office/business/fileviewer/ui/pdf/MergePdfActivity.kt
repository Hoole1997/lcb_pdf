package com.documentpro.office.business.fileviewer.ui.pdf

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import com.blankj.utilcode.util.KeyboardUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityPdfMergeBinding
import com.documentpro.office.business.fileviewer.ui.home.BusinessChooseModel
import com.documentpro.office.business.fileviewer.ui.home.BusinessFileListFragment
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType

class MergePdfActivity : BaseActivity<ActivityPdfMergeBinding, BusinessChooseModel>() , KeyboardUtils.OnSoftInputChangedListener{
    
    companion object {
        private const val TAG = "MergePdfActivity"
        const val EXTRA_MODE = "extra_mode"
        const val MODE_MERGE = 0
        const val MODE_SPLIT = 1
        
        fun launchForMerge(context: Context) {
            context.startActivity(Intent(context, MergePdfActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_MERGE)
            })
        }
        
        fun launchForSplit(context: Context) {
            context.startActivity(Intent(context, MergePdfActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_SPLIT)
            })
        }
    }

    private var mField_1: String = ""
    private lateinit var fileListFragment: BusinessFileListFragment
    private var mField_2: Int = MODE_MERGE

    override fun initBinding(): ActivityPdfMergeBinding {
        return ActivityPdfMergeBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessChooseModel {
        return viewModels<BusinessChooseModel>().value
    }

    override fun closePage() {
        execDisplay_1 (
            adPos = "IV_MergeBack",
            nextAction = {
                finish()
            }
        )
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
        // 获取模式参数
mField_2 = intent.getIntExtra(EXTRA_MODE, MODE_MERGE)
        
        // 设置工具栏标题
        val title = if (mField_2 == MODE_MERGE) {
            getString(R.string.merge_pdf_title)
        } else {
            getString(R.string.split_pdf_title)
        }
        binding.btnMerge.text = title
        binding.tvTitle.setText(title)
        useDefaultToolbar(binding.toolbar, "")

        fileListFragment = BusinessFileListFragment.newInstance(BusinessFileType.PDF.name, true)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_container, fileListFragment)
            .commit()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                execDisplay_1 (
                    adPos = "IV_MergeBack",
                    nextAction = {
                        finish()
                    }
                )
            }
        })

        KeyboardUtils.registerSoftInputChangedListener(this,this)
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

            }

            override fun afterTextChanged(s: Editable?) {
                val keyword = s?.toString() ?: ""
                mField_1 = keyword
                fileListFragment.filterByKeyword(keyword)
                Log.d("MergePdfActivity","过滤关键词$keyword")
            }
        })
        binding.btnMerge.setOnClickListener {
            BusinessPointLog.logEvent(
                event = if (mField_2 == MODE_MERGE) "MergePdf_Click" else "SplitPdf_Click"
            )
            
            // 获取选中的文件列表
            val selectedFiles = execAction_3 ()
            if (selectedFiles.isNotEmpty()) {
                // 根据模式决定最少选择文件数量
                val minFileCount = if (mField_2 == MODE_MERGE) 2 else 1
                if (selectedFiles.size >= minFileCount) {
                    // 跳转到PDF页面展示Activity
                    PdfPageGridActivity.launch(
                        this,
                        ArrayList(selectedFiles),mField_2
                    )
                }
            }
        }
        BusinessPointLog.logEvent(
            event = if (mField_2 == MODE_MERGE) "MergePdf_Show" else "SplitPdf_Show"
        )
    }

    private fun execDisplay_1(adPos: String,nextAction: () -> Unit) {
        nextAction.invoke()
    }

    override fun initObserve() {
        model.fileInfoEvent.observe(this) {
            //选择item会回调
            val selectList = it.filter {
                it.select
            }
            
            // 更新合并按钮状态
            execAction_2 (selectList.size)
        }
    }

    override fun initTag(): String {
        return TAG
    }

    override fun onSoftInputChanged(height: Int) {
        if (height == 0) {
            binding.etSearch.clearFocus()
        }
    }
    
    /**
     * 更新合并/拆分按钮状态
     * @param selectedCount 选中的文件数量
     */
    private fun execAction_2(selectedCount: Int) {
        if (mField_2 == MODE_MERGE) {
            // 合并模式：需要至少2个文件
            binding.btnMerge.isEnabled = selectedCount >= 2
            binding.btnMerge.text = if (selectedCount >= 2) {
                getString(R.string.merge_button_text_selected, selectedCount)
            } else {
                getString(R.string.merge_button_text_default)
            }
        } else {
            // 拆分模式：需要至少1个文件
            binding.btnMerge.isEnabled = selectedCount >= 1
            binding.btnMerge.text = if (selectedCount >= 1) {
                getString(R.string.split_button_text_selected, selectedCount)
            } else {
                getString(R.string.split_button_text_default)
            }
        }
    }
    
    /**
     * 获取选中的文件列表
     */
    private fun execAction_3(): List<BusinessFileInfo> {
        return model.fileInfoEvent.value?.filter { it.select } ?: emptyList()
    }
}