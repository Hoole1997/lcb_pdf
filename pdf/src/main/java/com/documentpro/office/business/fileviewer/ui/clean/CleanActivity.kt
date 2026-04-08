package com.documentpro.office.business.fileviewer.ui.clean

import android.content.res.ColorStateList
import android.os.Environment
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ToastUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityCleanBinding
import com.documentpro.office.business.fileviewer.utils.BusinessCleanUtils
import com.documentpro.office.business.fileviewer.utils.BusinessPermissionDialogUtils
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.documentpro.office.business.fileviewer.utils.toFileSizeNumString
import com.documentpro.office.business.fileviewer.utils.toFileSizeString
import com.documentpro.office.business.fileviewer.utils.toFileSizeUnitString
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.android.common.bill.ui.NativeAdStyleType
import java.io.File
import kotlin.random.Random

class CleanActivity : BaseActivity<ActivityCleanBinding, BusinessCleanModel>() {
    private lateinit var cleanAdapter: BusinessCleanAdapter

    companion object {
        private const val TAG = "CleanActivity"
    }

    override fun initBinding(): ActivityCleanBinding {
        return ActivityCleanBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessCleanModel {
        return viewModels<BusinessCleanModel>().value
    }

    override fun initView() {
        useDefaultToolbar(binding.toolbar, "")
        BarUtils.addMarginTopEqualStatusBarHeight(binding.toolbar)

        loadNative(binding.adContainer, styleType = NativeAdStyleType.LARGE)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closePage()
            }
        })

        cleanAdapter = BusinessCleanAdapter(
            onGroupSelected = { type -> model.toggleGroupSelection(type) },
            onItemSelected = { type, id -> model.toggleItemSelection(type, id) },
            scanProgress = model.scanProgress
        )
        binding.elvFile.setAdapter(cleanAdapter)

        binding.btnClean.setOnClickListener {

            if (model.isScanning.value) {
                // 扫描中，不做任何操作
                return@setOnClickListener
            }

            val cleanAction = {
                // 获取选中的文件
                val selectedItems = model.getSelectedItems()
                var childCount = 0
                cleanAdapter.groups.forEach { group ->
                    childCount += group.items.filter {
                        it.isSelected
                    }.size
                }
                if (selectedItems.size == childCount) {
                    BusinessPointLog.logEvent(
                        "Clean_Finish", mapOf(
                            "Clean_Result" to "Succ2"
                        )
                    )
                } else {
                    BusinessPointLog.logEvent(
                        "Clean_Finish", mapOf(
                            "Clean_Result" to "Succ1"
                        )
                    )
                }
                // 开始删除文件
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        selectedItems.forEach { item ->
                            try {
                                FileUtils.delete(item.path)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    closePage()
                }
            }
            execDisplay_6(
                adPos = "IV_CleanClick",
                nextAction = {
                    cleanAction.invoke()
                }
            )

            CleanEndLoadingActivity.start(
                this,
                binding.tvFileSize.text.toString(),
                binding.tvFileSizeUnit.text.toString())

        }

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
                cleanAdapter.setData(groups)
            }
        }

        lifecycleScope.launch {
            model.totalSize.collectLatest { size ->
                binding.tvFileSize.text = size.toFileSizeNumString()
                binding.tvFileSizeUnit.text = size.toFileSizeUnitString()
                // 更新按钮文字
                if (!model.isScanning.value) {
                    binding.btnClean.text =
                        getString(R.string.clean_button_text, size.toFileSizeString())
                }
            }
        }

        lifecycleScope.launch {
            model.isScanning.collectLatest { isScanning ->
                binding.btnClean.text =
                    if (isScanning) getString(R.string.clean_scanning) else getString(
                        R.string.clean_button_text,
                        model.totalSize.value.toFileSizeString()
                    )
                binding.btnClean.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this@CleanActivity,
                        if (isScanning) R.color.clean_button_disabled_background else R.color.theme_color
                    )
                )
                binding.btnClean.setTextColor(
                    ContextCompat.getColor(
                        this@CleanActivity,
                        if (isScanning) R.color.clean_button_disabled_text else R.color.white
                    )
                )

                cleanAdapter.setScanning(isScanning)
            }
        }

        lifecycleScope.launch {
            model.scanningFolder.collectLatest { folder ->
            }
        }
    }

    override fun initTag(): String {
        return TAG
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun execAction_4() {
        BusinessPointLog.logEvent("Clean_Start")
        lifecycleScope.launch {
            model.startScan()

            withContext(Dispatchers.IO) {
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
                model.stopScan()
            }
        }
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
