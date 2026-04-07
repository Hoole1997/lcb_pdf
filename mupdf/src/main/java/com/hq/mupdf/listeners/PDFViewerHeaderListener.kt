package com.hq.mupdf.listeners

import android.content.Context
import android.widget.Toast
import com.hq.mupdf.header.PDFHeaderListener
import com.hq.mupdf.R

/**
 * PDF Header监听器实现
 * 处理PDF文档头部组件的交互事件
 */
class PDFViewerHeaderListener(
    private val context: Context,
    private val onBackPressed: () -> Unit,
    private val onSearchRequested: () -> Unit = {},
    private val onBookmarkToggle: (Boolean) -> Unit = {},
    private val onSaveRequested: (String?, Int) -> Unit = { _, _ -> },
    private val onMenuRequested: () -> Unit = {},
    private val onTitleTapped: (String?) -> Unit = {},
    private val onPageInfoTapped: (Int, Int) -> Unit = { _, _ -> }
) : PDFHeaderListener {
    

    
    override fun onBackButtonClick() {
        onBackPressed()
    }
    
    override fun onSearchButtonClick() {
        onSearchRequested()
    }
    
    override fun onBookmarkButtonClick(isBookmarked: Boolean) {
        // 调用自定义处理器
        onBookmarkToggle(isBookmarked)
    }
    
    override fun onSaveButtonClick(fileName: String?, currentPage: Int) {
        // 调用自定义处理器
        onSaveRequested(fileName, currentPage)
    }
    
    override fun onMenuButtonClick() {
        // 调用自定义处理器
        onMenuRequested()
    }
    
    override fun onTitleClick(fileName: String?) {
        // 默认处理：显示文件名
        Toast.makeText(context, context.getString(R.string.file_name_format, fileName ?: context.getString(R.string.unknown_file)), Toast.LENGTH_SHORT).show()
        
        // 调用自定义处理器
        onTitleTapped(fileName)
    }
    
    override fun onPageInfoClick(currentPage: Int, totalPages: Int) {
        // 默认处理：显示页码信息
        Toast.makeText(context, context.getString(R.string.current_page_format, currentPage, totalPages), Toast.LENGTH_SHORT).show()
        
        // 调用自定义处理器
        onPageInfoTapped(currentPage, totalPages)
    }
}
