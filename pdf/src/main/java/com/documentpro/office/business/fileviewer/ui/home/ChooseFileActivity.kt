package com.documentpro.office.business.fileviewer.ui.home

import android.content.res.ColorStateList
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.FileUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityChooseFileBinding
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.utils.BusinessShareUtils
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChooseFileActivity : BaseActivity<ActivityChooseFileBinding, BusinessChooseModel>(){

    companion object {
        private const val TAG = "ChooseFileActivity"
        private const val PARAM_QUERY_TYPE = "param_query_type"
        const val RESULT_PARAM_DELETE_LIST = "deleteList"
        fun launchIntent(context: Context,mField_1: String): Intent {
            return Intent().apply {
                setClass(context, ChooseFileActivity::class.java)
                putExtra(PARAM_QUERY_TYPE,mField_1)
            }
        }
    }

    private var mField_1 = BusinessFileType.PDF.name
    private var mField_2: MenuItem? = null
    private lateinit var listFragment: BusinessFileListFragment
    private lateinit var mainModel: BusinessMainModel

    override fun initBinding(): ActivityChooseFileBinding {
        return ActivityChooseFileBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessChooseModel {
        mainModel = viewModels<BusinessMainModel>().value
        return viewModels<BusinessChooseModel>().value
    }

    override fun initView() {
        useDefaultToolbar(binding.toolbar,"")
        binding.tvTitle.text = getString(R.string.choose_file_selected_count, 0)
        updateBottomActionButtons(0)
        mField_1 = intent.getStringExtra(PARAM_QUERY_TYPE) ?: BusinessFileType.PDF.name

        listFragment = BusinessFileListFragment.newInstance(mField_1,true)
        supportFragmentManager.beginTransaction().replace(R.id.fl_container,listFragment).commit()

        binding.btnDelete.setOnClickListener {
            execAction_2 ()
        }
        binding.btnShare.setOnClickListener {
            execAction_3 ()
        }
    }

    override fun closePage() {
        setResult(RESULT_OK)
        finish()
    }

    override fun initObserve() {
        model.fileInfoEvent.observe(this) {
            val selectCount = it.count {
                it.select
            }
            binding.tvTitle.text = getString(R.string.choose_file_selected_count, selectCount)
            updateBottomActionButtons(selectCount)
        }
        model.allChooseEvent.observe(this) {
            execAction_1 ()
        }
    }

    override fun initTag(): String {
        return TAG
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu_choose,menu)
        mField_2 = menu?.findItem(R.id.action_choose)
        execAction_1 ()
        return super.onCreateOptionsMenu(menu)
    }

    private fun execAction_1() {
        val isAllChoose = model.allChooseEvent.value == true
        mField_2?.setIcon(
            if (isAllChoose) R.mipmap.ic_checkbox_selected else R.mipmap.ic_checkbox_unselected
        )
    }

    private fun updateBottomActionButtons(selectCount: Int) {
        val hasSelection = selectCount > 0
        binding.btnDelete.isEnabled = hasSelection
        binding.btnShare.isEnabled = hasSelection
        binding.btnDelete.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (hasSelection) R.color.theme_color_soft_background else R.color.clean_button_disabled_background
            )
        )
        binding.btnDelete.setTextColor(
            ContextCompat.getColor(
                this,
                if (hasSelection) R.color.theme_color else R.color.clean_button_disabled_text
            )
        )
        binding.btnShare.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (hasSelection) R.color.theme_color else R.color.clean_button_disabled_background
            )
        )
        binding.btnShare.setTextColor(
            ContextCompat.getColor(
                this,
                if (hasSelection) R.color.white else R.color.clean_button_disabled_text
            )
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_choose) {
            model.switchAllChoose()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun execAction_2() {
        val selectList = model.fileInfoEvent.value?.filter {
            it.select
        } ?:arrayListOf()
        if (selectList.isEmpty())return
        val delete = {
            lifecycleScope.launch(Dispatchers.IO) {
                selectList.forEach {
                    FileUtils.delete(it.path)
                }
                withContext(Dispatchers.Main) {
                    mainModel.refreshFileScan()
                }
            }
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_file_delete_confirm_title))
            .setMessage(getString(R.string.choose_file_delete_confirm_message, selectList.size))
            .setNegativeButton(getString(R.string.choose_file_delete_confirm_cancel)) { dialog,_ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.choose_file_delete_confirm_ok)) { dialog,_ ->
                delete.invoke()
                dialog.dismiss()
            }
            .show()
    }

    private fun execAction_3() {
        val selectList = model.fileInfoEvent.value?.filter {
            it.select
        } ?:arrayListOf()
        if (selectList.isEmpty())return
        BusinessShareUtils.share(this,selectList)
    }

}
