package com.hq.mupdf.textselection

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.viewer.PageView
import com.hq.mupdf.viewer.ReaderView

/**
 * 文本选择管理器
 * 现在基于MuPDF原生API实现，替代复杂的自定义计算
 * 保持向后兼容的公共接口
 */
class TextSelectionManager(
    private val context: Context,
    private val core: MuPDFCore,
    private val readerView: ReaderView
) {
    companion object {
        private const val TAG = "TextSelectionManager"
    }

    // 使用新的简单实现
    private val simpleManager = SimpleTextSelectionManager(context, core, readerView)

    // 监听器接口保持兼容性
    interface OnSelectionChangeListener {
        fun onSelectionStarted()
        fun onSelectionChanged(selectedText: String)
        fun onSelectionEnded()
    }

    interface OnSearchRequestListener {
        fun onSearchRequested(query: String)
    }

    fun setOnSelectionChangeListener(listener: OnSelectionChangeListener) {
        simpleManager.setOnSelectionChangeListener(listener)
    }

    fun setOnSearchRequestListener(listener: OnSearchRequestListener) {
        simpleManager.setOnSearchRequestListener(listener)
    }

    /**
     * 开始文本选择 - 简化实现
     */
    fun startSelection(x: Float, y: Float, pageView: PageView): Boolean {
        // 委托给简单实现
        return simpleManager.startTextSelection(x, y, pageView)
    }

    /**
     * 处理触摸事件 - 委托给简单实现
     */
    fun handleTouchEvent(event: MotionEvent, pageView: PageView): Boolean {
        return simpleManager.handleTouchEvent(event, pageView)
    }
    
    /**
     * 处理触摸事件 - 兼容ReaderView的onTouchEvent调用
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        // 获取当前显示的PageView
        val pageView = readerView.getDisplayedView() as? PageView
        return if (pageView != null) {
            simpleManager.handleTouchEvent(event, pageView)
        } else {
            false
        }
    }

    /**
     * 更新选择范围 - 简化实现
     */
    fun updateSelection(endX: Float, endY: Float): Boolean {
        return true
    }

    /**
     * 结束选择
     */
    fun endSelection() {
        simpleManager.endSelection()
    }
    
    /**
     * 取消选择 - 兼容ReaderView的cancelSelection调用
     */
    fun cancelSelection() {
        endSelection()
    }

    /**
     * 获取选中的文本
     */
    fun getSelectedText(): String {
        return ""
    }

    /**
     * 复制选中的文本到剪贴板
     */
    fun copySelectedText() {
    }

    /**
     * 是否在选择模式
     */
    fun isInSelectionMode(): Boolean {
        return simpleManager.isInSelectionMode()
    }

    /**
     * 切换到单词选择模式
     */
    fun switchToWordSelection() {
    }

    /**
     * 显示上下文菜单
     */
    fun showContextMenu() {
    }

    /**
     * 隐藏上下文菜单
     */
    fun hideContextMenu() {
        endSelection()
    }

    /**
     * 获取当前选择结果
     */
    fun getCurrentSelection(): NativeTextSelector.SelectionResult? {
        return simpleManager.getCurrentSelection()
    }
    
    /**
     * 搜索文本
     */
    fun searchText(query: String, pageView: PageView): List<RectF> {
        return simpleManager.searchText(query, pageView)
    }

    /**
     * 获取页面文本
     */
    fun getPageText(pageNumber: Int): String {
        return simpleManager.getPageText(pageNumber)
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        simpleManager.onPageChanged()
    }

    /**
     * 页面改变处理
     */
    fun onPageChanged() {
        simpleManager.onPageChanged()
    }

    /**
     * 资源清理
     */
    fun destroy() {
        Log.d(TAG, "Destroying TextSelectionManager")
        simpleManager.destroy()
    }
}
