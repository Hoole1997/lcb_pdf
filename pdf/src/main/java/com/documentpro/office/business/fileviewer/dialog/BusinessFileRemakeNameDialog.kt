package com.documentpro.office.business.fileviewer.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.FileUtils
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.LayoutRemakeFileNameDialogBinding
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("ViewConstructor")
class BusinessFileRemakeNameDialog(context: Context,val fileInfo: BusinessFileInfo,val position: Int, val onResult:(BusinessFileInfo, Int) -> Unit) : BottomPopupView(context) {

    companion object {
        fun show(context: Context,fileInfo: BusinessFileInfo,position: Int,onResult:(BusinessFileInfo, Int) -> Unit) {
            XPopup.Builder(context)
                .moveUpToKeyboard(true)
                .hasNavigationBar(false)
                .asCustom(BusinessFileRemakeNameDialog(context,fileInfo,position,onResult))
                .show()
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_remake_file_name_dialog
    }

    override fun onCreate() {
        super.onCreate()
        val binding = LayoutRemakeFileNameDialogBinding.bind(popupImplView)

        binding.etContent.setText(FileUtils.getFileNameNoExtension(fileInfo.name))
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        binding.ivClear.setOnClickListener {
            binding.etContent.setText("")
        }
        binding.btnConfirm.setOnClickListener {
            val content = binding.etContent.text.toString().trim()
            if (TextUtils.isEmpty(content)) return@setOnClickListener
            
            lifecycleScope.launch(Dispatchers.IO) {
                // 获取原文件的扩展名
                val originalExtension = FileUtils.getFileExtension(fileInfo.path)
                // 构建新文件名（包含扩展名）
                val newFileName = if (originalExtension.isNotEmpty()) {
                    "$content.$originalExtension"
                } else {
                    content
                }
                
                // 获取父目录
                val parentDir = FileUtils.getDirName(fileInfo.path)
                val newPath = if (parentDir.endsWith("/")) {
                    parentDir + newFileName
                } else {
                    "$parentDir/$newFileName"
                }
                
                // 检查新文件名是否已存在
                if (FileUtils.isFileExists(newPath) && newPath != fileInfo.path) {
                    withContext(Dispatchers.Main) {
                        // 这里可以显示文件已存在的提示
                        // ToastUtils.showShort("文件名已存在")
                    }
                    return@launch
                }
                
                // 执行重命名操作
                val renameResult = FileUtils.rename(fileInfo.path, newFileName)
                if (renameResult) {
                    withContext(Dispatchers.Main) {
                        // 创建新的FileInfo对象，而不是修改原对象
                        val updatedFileInfo = fileInfo.copy(
                            name = newFileName,
                            path = newPath
                        )
                        onResult.invoke(updatedFileInfo, position)
                        dismiss()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // 重命名失败的提示
                        // ToastUtils.showShort("重命名失败")
                    }
                }
            }
        }
    }

}