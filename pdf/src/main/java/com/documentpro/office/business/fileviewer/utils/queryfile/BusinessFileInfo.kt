package com.documentpro.office.business.fileviewer.utils.queryfile

import android.content.Context
import android.os.Parcelable
import com.blankj.utilcode.util.LogUtils
import com.documentpro.office.business.fileviewer.ui.office.OfficeViewActivity
import com.documentpro.office.business.fileviewer.ui.pdf.ImagePreviewActivity
import com.documentpro.office.business.fileviewer.ui.pdf.BusinessDocumentActivity
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class BusinessFileInfo(
    var name: String,            // 文件名称
    var path: String,            // 文件路径
    val type: String,            // MIME 类型
    val size: Long,              // 文件大小
    val dateModified: Long,      // 最后修改时间
    val dateCreated: Long?,       // 创建时间（可选）
    val extension: String?,      // 文件扩展名（可选）
    val isReadable: Boolean,      // 是否可读
    val isWritable: Boolean,      // 是否可写
    val isHidden: Boolean,        // 是否为隐藏文件
    val checksum: String?,        // 文件的校验和（可选）
    var isLocked: Boolean = false // 是否加密/带密码锁
) : Serializable, Parcelable {

    @IgnoredOnParcel
    var select: Boolean = false

    /**
     * 判断是否为广告占位符
     */
    fun isAd(): Boolean = name == "ad"

    fun icon(): Int {
        return when (equalsFileType(type)) {
            BusinessFileType.PDF -> com.documentpro.office.business.fileviewer.R.mipmap.ic_pdf
            BusinessFileType.EXCEL -> com.documentpro.office.business.fileviewer.R.mipmap.ic_excel
            BusinessFileType.IMAGE -> com.documentpro.office.business.fileviewer.R.mipmap.ic_image
            BusinessFileType.WORD -> com.documentpro.office.business.fileviewer.R.mipmap.ic_word
            BusinessFileType.PPT -> com.documentpro.office.business.fileviewer.R.mipmap.ic_ppt
            BusinessFileType.TXT -> com.documentpro.office.business.fileviewer.R.mipmap.ic_txt
            else -> 0
        }
    }

    fun open(context: Context) {
        LogUtils.d(equalsFileType(type))
        if (equalsFileType(type) == BusinessFileType.IMAGE) {
            ImagePreviewActivity.launch(context,arrayListOf(this))
        } else if (equalsFileType(type) == BusinessFileType.PDF) {
            BusinessDocumentActivity.launch(context,this)
        }else {
            OfficeViewActivity.launch(context, this)
        }
    }

    companion object {
        /**
         * 创建广告占位符
         * 
         * 用于在文件列表中插入广告位。当 BusinessFileAdapter 检测到 name == "ad" 时，
         * 会显示广告容器而不是文件信息。
         * 
         * 使用示例：
         * ```
         * val fileList = mutableListOf<BusinessFileInfo>(...)
         * // 在第3个位置插入广告
         * fileList.add(2, BusinessFileInfo.createAdPlaceholder())
         * adapter.submitList(fileList)
         * ```
         * 
         * @return 广告占位符对象
         */
        fun createAdPlaceholder(): BusinessFileInfo {
            return BusinessFileInfo(
                name = "ad",
                path = "",
                type = "",
                size = 0L,
                dateModified = 0L,
                dateCreated = null,
                extension = null,
                isReadable = false,
                isWritable = false,
                isHidden = false,
                checksum = null,
                isLocked = false
            )
        }
    }
} 