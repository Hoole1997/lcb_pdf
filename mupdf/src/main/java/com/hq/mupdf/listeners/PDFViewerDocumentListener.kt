package com.hq.mupdf.listeners

import android.content.Context
import android.widget.Toast
import com.hq.mupdf.viewer.PDFViewerListener
import com.hq.mupdf.viewer.PDFDocumentInfo
import com.hq.mupdf.header.PDFHeader
import com.hq.mupdf.bottombar.PDFBottomBar
import com.hq.mupdf.toolbar.EnhancedPDFToolbar
import com.hq.mupdf.toolbar.PDFToolbar
import com.hq.mupdf.R

/**
 * PDF Viewer监听器实现
 * 处理PDF文档查看器的事件
 */
class PDFViewerDocumentListener(
    private val context: Context,
    private val pdfHeader: PDFHeader,
    private val pdfBottomBar: PDFBottomBar,
    private val pdfToolbar: EnhancedPDFToolbar,
    private val onDocumentReady: (Int, PDFDocumentInfo) -> Unit = { _, _ -> },
    private val onDocumentError: (String) -> Unit = {},
    private val onPageNavigation: (Int, Int) -> Unit = { _, _ -> },
    private val onZoomChanged: (Float) -> Unit = {},
    private val onPageInteraction: (Int, Float, Float) -> Unit = { _, _, _ -> },
    private val onViewerError: (String) -> Unit = {}
) : PDFViewerListener {
    

    
    override fun onDocumentLoadStart() {
        // 可以在这里显示加载状态
    }
    
    override fun onDocumentLoadSuccess(pageCount: Int, documentInfo: PDFDocumentInfo) {
        // 更新Header
        pdfHeader.updateDocumentInfo(documentInfo.fileName, 1, pageCount)
        
        // 初始化底部栏
        pdfBottomBar.setCurrentPage(0, pageCount)
        // 调用自定义处理器
        onDocumentReady(pageCount, documentInfo)
    }
    
    override fun onDocumentLoadError(error: String) {
        // 显示错误提示
        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        
        // 调用自定义处理器
        onDocumentError(error)
    }
    
    override fun onPageChanged(currentPage: Int, totalPages: Int) {
        // 更新页面控制按钮状态
        updatePageControls(currentPage, totalPages)
        
        // 调用自定义处理器
        onPageNavigation(currentPage, totalPages)
    }
    
    override fun onPageRenderStart(pageIndex: Int) {
    }
    
    override fun onPageRenderSuccess(pageIndex: Int) {
    }
    
    override fun onPageRenderError(pageIndex: Int, error: String) {
    }
    
    override fun onZoomChanged(zoomLevel: Float) {
        // 可以在这里更新UI显示当前缩放级别
        onZoomChanged(zoomLevel)
    }
    
    override fun onDirectionChanged(isHorizontal: Boolean) {
        // 更新底部栏按钮状态
        pdfBottomBar.updateDirectionButtonState(isHorizontal)
    }
    
    override fun onPageClick(pageIndex: Int, x: Float, y: Float) {
        // 可以在这里处理页面点击事件，比如显示/隐藏工具栏
        pdfToolbar.toggleToolbar()
        
        // 调用自定义处理器
        onPageInteraction(pageIndex, x, y)
    }
    
    override fun onPageLongPress(pageIndex: Int, x: Float, y: Float) {
        // 可以在这里处理页面长按事件，比如显示上下文菜单
        Toast.makeText(context, context.getString(R.string.long_press_function_pending), Toast.LENGTH_SHORT).show()
        
        // 调用自定义处理器
        onPageInteraction(pageIndex, x, y)
    }
    
    override fun onViewerError(error: String) {
        // 显示错误提示
        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        
        // 调用自定义处理器
        onViewerError(error)
    }
    
    override fun onSearchStart(query: String) {
        // 可以在这里显示搜索进度指示器或更新UI状态
        Toast.makeText(context, context.getString(R.string.search_query_format, query), Toast.LENGTH_SHORT).show()
    }
    
    override fun onSearchResultFound(query: String, pageIndex: Int, totalMatches: Int) {
        // 显示搜索结果提示
        val message = context.getString(R.string.search_result_found_format, query, pageIndex + 1, totalMatches)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onSearchResultNotFound(query: String) {
        // 显示未找到结果的提示
        Toast.makeText(context, context.getString(R.string.search_not_found_format, query), Toast.LENGTH_SHORT).show()
    }
    
    override fun onSearchEnd() {
        // 可以在这里隐藏搜索进度指示器或重置UI状态
    }
    
    /**
     * 更新页面控制按钮状态
     */
    private fun updatePageControls(currentPage: Int, totalPages: Int) {
        val currentPageNum = currentPage + 1
        
        // 更新Header页码信息
        pdfHeader.updatePageInfo(currentPageNum, totalPages)
        
        // 通过底部栏管理器更新页面控制状态
        pdfBottomBar.setCurrentPage(currentPage, totalPages)
    }
}
