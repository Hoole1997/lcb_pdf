package com.hq.mupdf.interfaces

import com.artifex.mupdf.fitz.Quad

/**
 * PDF搜索结果数据类
 */
data class PDFSearchResult(
    val query: String,                    // 搜索关键词
    val pageNumber: Int,                  // 页面编号（从0开始）
    val displayPageNumber: Int,           // 显示页面编号（从1开始）
    val contextText: String,              // 上下文文本
    val highlightText: String,            // 高亮文本
    val searchBoxes: Array<Array<Quad>>? = null  // 搜索位置信息
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PDFSearchResult

        if (query != other.query) return false
        if (pageNumber != other.pageNumber) return false
        if (displayPageNumber != other.displayPageNumber) return false
        if (contextText != other.contextText) return false
        if (highlightText != other.highlightText) return false

        return true
    }

    override fun hashCode(): Int {
        var result = query.hashCode()
        result = 31 * result + pageNumber
        result = 31 * result + displayPageNumber
        result = 31 * result + contextText.hashCode()
        result = 31 * result + highlightText.hashCode()
        return result
    }
}

/**
 * PDF搜索监听器接口
 */
interface PDFSearchListener {
    /**
     * 搜索开始
     */
    fun onSearchStart(query: String)
    
    /**
     * 搜索进度更新
     */
    fun onSearchProgress(query: String, progress: Int, total: Int)
    
    /**
     * 搜索结果找到
     */
    fun onSearchResultsFound(query: String, results: List<PDFSearchResult>)
    
    /**
     * 搜索未找到结果
     */
    fun onSearchNoResults(query: String)
    
    /**
     * 搜索完成
     */
    fun onSearchComplete(query: String, totalResults: Int)
    
    /**
     * 搜索错误
     */
    fun onSearchError(query: String, error: String)
    
    /**
     * 搜索取消
     */
    fun onSearchCanceled(query: String)
}

/**
 * PDF搜索控制器接口
 */
interface PDFSearchController {
    /**
     * 开始搜索
     */
    fun startSearch(query: String, listener: PDFSearchListener)
    
    /**
     * 停止搜索
     */
    fun stopSearch()
    
    /**
     * 跳转到指定搜索结果
     */
    fun jumpToSearchResult(result: PDFSearchResult)
    
    /**
     * 清除搜索高亮
     */
    fun clearSearchHighlight()
    
    /**
     * 获取当前搜索状态
     */
    fun isSearching(): Boolean
}

/**
 * PDF页面跳转接口
 */
interface PDFNavigationController {
    /**
     * 跳转到指定页面
     */
    fun jumpToPage(pageIndex: Int)
    
    /**
     * 获取当前页面
     */
    fun getCurrentPage(): Int
    
    /**
     * 获取总页数
     */
    fun getTotalPages(): Int
}
