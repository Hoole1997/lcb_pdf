package com.documentpro.office.business.fileviewer.dialog

import android.annotation.SuppressLint
import android.content.Context
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.TimeUtils
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.LayoutFileDetailDialogBinding
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.toFileSizeString

@SuppressLint("ViewConstructor")
class BusinessFileDetailDialog(context: Context,val fileInfo: BusinessFileInfo) : BottomPopupView(context) {

    companion object {
        fun show(context: Context,fileInfo: BusinessFileInfo) {
            XPopup.Builder(context)
                .moveUpToKeyboard(true)
                .hasNavigationBar(false)
                .asCustom(BusinessFileDetailDialog(context,fileInfo))
                .show()
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_file_detail_dialog
    }

    override fun onCreate() {
        super.onCreate()
        val binding = LayoutFileDetailDialogBinding.bind(popupImplView)

        binding.ivFileType.setImageResource(fileInfo.icon())
        binding.tvFileName.text = fileInfo.name
        binding.tvFileSize.text = fileInfo.size.toFileSizeString()
        binding.tvFilePath.text = fileInfo.path
        binding.tvFileType.text = FileUtils.getFileExtension(fileInfo.path)
        binding.tvFileChangeTime.text = TimeUtils.millis2String(fileInfo.dateModified*1000)
    }

}