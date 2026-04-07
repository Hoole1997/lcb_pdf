package com.hq.mupdf.listeners

import android.content.Context
import android.widget.Toast
import com.hq.mupdf.bottombar.PDFBottomBarListener
import com.hq.mupdf.viewer.PDFViewer
import com.hq.mupdf.R

/**
 * PDF BottomBar监听器实现
 * 处理PDF文档底部栏的交互事件
 */
class PDFViewerBottomBarListener(
    private val context: Context,
    private val pdfViewer: PDFViewer,
    private val onThumbnailToggle: () -> Unit = {},
    private val onDirectionToggle: (Boolean) -> Unit = {},
    private val onPageJumpSuccess: (Int) -> Unit = {},
    private val onPageNavigationError: (String) -> Unit = {},
    private val onVisibilityChanged: (Boolean) -> Unit = {}
) : PDFBottomBarListener {
    

    
    /**
     * 页面跳转事件
     */
    override fun onPageJump(targetPage: Int, totalPages: Int): Boolean {
        return if (targetPage > 0 && targetPage <= totalPages) {
            pdfViewer.jumpToPage(targetPage - 1) // 转换为0基索引
            onPageJumpSuccess(targetPage)
            true
        } else {
            val errorMsg = context.getString(R.string.cannot_jump_to_page, targetPage)
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            onPageNavigationError(errorMsg)
            false
        }
    }
    
    /**
     * 上一页按钮点击事件
     */
    override fun onPreviousPage(currentPage: Int): Boolean {
        return if (currentPage > 1) {
            pdfViewer.goToPreviousPage()
            true
        } else {
            val errorMsg = context.getString(R.string.already_first_page)
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            onPageNavigationError(errorMsg)
            false
        }
    }
    
    /**
     * 下一页按钮点击事件
     */
    override fun onNextPage(currentPage: Int): Boolean {
        return if (currentPage < pdfViewer.getTotalPages()) {
            pdfViewer.goToNextPage()
            true
        } else {
            val errorMsg = context.getString(R.string.already_last_page)
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            onPageNavigationError(errorMsg)
            false
        }
    }
    
    /**
     * 缩略图切换事件
     */
    override fun onThumbnailToggle() {
        this.onThumbnailToggle.invoke() // 调用传入的缩略图切换回调
    }
    
    /**
     * 查看方向切换事件
     */
    override fun onDirectionToggle(isHorizontal: Boolean) {
        // 设置PDF查看器的方向
        pdfViewer.setViewDirection(isHorizontal)
        
        // 调用自定义处理器
        this.onDirectionToggle.invoke(isHorizontal)
    }
    
    /**
     * 底部栏可见性改变事件
     */
    override fun onBottomBarVisibilityChanged(isVisible: Boolean) {
        // 可以在这里处理底部栏显示/隐藏时的其他UI调整
        
        // 调用自定义处理器
        onVisibilityChanged(isVisible)
    }
}
