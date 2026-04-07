package com.documentpro.office.business.fileviewer.ui.pdf.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * PDF页面信息数据类
 */
@Parcelize
data class BusinessPdfPageInfo(
    val filePath: String,         // PDF文件路径
    val fileName: String,         // 文件名
    val pageIndex: Int,           // 页面索引（从0开始）
    val pageNumber: Int,          // 页面号（从1开始）
    var isSelected: Boolean = false, // 是否被选中
    var thumbnail: Bitmap? = null    // 页面缩略图
) : Parcelable {
    
    /**
     * 获取页面显示名称
     */
    fun getDisplayName(): String {
        return "$fileName - Page $pageNumber"
    }
    
    /**
     * 获取唯一标识符
     */
    fun getUniqueId(): String {
        return "${filePath}_$pageIndex"
    }
}