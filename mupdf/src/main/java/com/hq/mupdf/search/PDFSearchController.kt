package com.hq.mupdf.search

import android.content.Context
import com.hq.mupdf.interfaces.PDFSearchController
import com.hq.mupdf.interfaces.PDFSearchListener
import com.hq.mupdf.interfaces.PDFSearchResult
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.viewer.SearchTask
import com.hq.mupdf.viewer.SearchTaskResult
import kotlinx.coroutines.*

/**
 * PDF搜索控制器实现类
 * 负责管理PDF文档的搜索功能
 */
class PDFSearchController(
    private val context: Context,
    private var muPDFCore: MuPDFCore? = null
) : PDFSearchController {
    

    
    private var searchTask: SearchTask? = null
    private var currentSearchJob: Job? = null
    private var isSearching = false
    private var currentListener: PDFSearchListener? = null
    
    // 搜索结果缓存
    private val searchResults = mutableListOf<PDFSearchResult>()
    private var currentQuery = ""
    
    init {
        initializeSearchTask()
    }
    
    /**
     * 设置MuPDFCore
     */
    fun setMuPDFCore(core: MuPDFCore?) {
        this.muPDFCore = core
        initializeSearchTask()
    }
    
    /**
     * 初始化搜索任务
     */
    private fun initializeSearchTask() {
        muPDFCore?.let { core ->
            searchTask = object : SearchTask(context, core) {
                override fun onTextFound(result: SearchTaskResult?) {
                    handleSearchResult(result)
                }
            }
        }
    }
    
    override fun startSearch(query: String, listener: PDFSearchListener) {
        if (query.isEmpty()) {
            listener.onSearchError(query, "搜索关键词不能为空")
            return
        }
        
        if (muPDFCore == null) {
            listener.onSearchError(query, "PDF文档未加载")
            return
        }
        
        // 停止之前的搜索
        stopSearch()
        
        currentListener = listener
        currentQuery = query
        isSearching = true
        searchResults.clear()
        
        listener.onSearchStart(query)
        
        // 启动搜索协程
        currentSearchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                performSearchAsync(query, listener)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onSearchError(query, e.message ?: "搜索出错")
                    isSearching = false
                }
            }
        }
    }
    
    /**
     * 异步执行搜索
     */
    private suspend fun performSearchAsync(query: String, listener: PDFSearchListener) {
        val core = muPDFCore ?: return
        val totalPages = core.countPages()
        
        withContext(Dispatchers.Main) {
            listener.onSearchProgress(query, 0, totalPages)
        }
        
        // 逐页搜索
        for (pageIndex in 0 until totalPages) {
            if (!isSearching) break // 检查是否已取消
            
            try {
                val searchBoxes = core.searchPage(pageIndex, query)
                
                if (searchBoxes.isNotEmpty()) {
                    val result = createSearchResult(query, pageIndex, searchBoxes, core)
                    searchResults.add(result)
                    
                    withContext(Dispatchers.Main) {
                        listener.onSearchProgress(query, pageIndex + 1, totalPages)
                    }
                }
            } catch (e: Exception) {
                // Error searching page, continue to next page
            }
            
            // 更新进度
            withContext(Dispatchers.Main) {
                listener.onSearchProgress(query, pageIndex + 1, totalPages)
            }
        }
        
        // 搜索完成
        withContext(Dispatchers.Main) {
            if (isSearching) {
                if (searchResults.isNotEmpty()) {
                    listener.onSearchResultsFound(query, searchResults)
                } else {
                    listener.onSearchNoResults(query)
                }
                listener.onSearchComplete(query, searchResults.size)
                isSearching = false
            }
        }
    }
    
    /**
     * 创建搜索结果对象
     */
    private fun createSearchResult(
        query: String, 
        pageIndex: Int, 
        searchBoxes: Array<Array<com.artifex.mupdf.fitz.Quad>>,
        core: MuPDFCore
    ): PDFSearchResult {
        val displayPageNumber = pageIndex + 1
        
        // 获取页面文本并提取上下文
        val contextText = try {
            val pageText = core.getPageText(pageIndex)
            if (pageText.isNotEmpty()) {
                extractContext(pageText, query)
            } else {
                "第 $displayPageNumber 页"
            }
        } catch (e: Exception) {
            "第 $displayPageNumber 页"
        }
        
        return PDFSearchResult(
            query = query,
            pageNumber = pageIndex,
            displayPageNumber = displayPageNumber,
            contextText = contextText,
            highlightText = query,
            searchBoxes = searchBoxes
        )
    }
    
    /**
     * 从页面文本中提取上下文
     */
    private fun extractContext(pageText: String, query: String): String {
        val queryIndex = pageText.indexOf(query, ignoreCase = true)
        if (queryIndex == -1) {
            // 如果没有找到查询词，返回页面开头部分
            return pageText.take(100).trim() + if (pageText.length > 100) "..." else ""
        }
        
        // 向前查找合适的断句位置
        val contextStart = findContextStart(pageText, queryIndex, 60)
        // 向后查找合适的断句位置  
        val contextEnd = findContextEnd(pageText, queryIndex + query.length, 60)
        
        val context = pageText.substring(contextStart, contextEnd).trim()
        val prefix = if (contextStart > 0) "..." else ""
        val suffix = if (contextEnd < pageText.length) "..." else ""
        
        return "$prefix$context$suffix"
    }
    
    /**
     * 向前查找合适的上下文开始位置
     */
    private fun findContextStart(text: String, queryIndex: Int, maxLength: Int): Int {
        val start = maxOf(0, queryIndex - maxLength)
        
        // 在范围内查找句号、换行符等自然断句点
        for (i in queryIndex - 1 downTo start) {
            when (text[i]) {
                '.', '。', '\n', '\r' -> {
                    // 找到断句点，从下一个字符开始
                    return minOf(i + 1, queryIndex)
                }
                ' ', '\t' -> {
                    // 在查询词前20个字符内找到空格，使用它作为断点
                    if (queryIndex - i <= 20) {
                        return i + 1
                    }
                }
            }
        }
        
        return start
    }
    
    /**
     * 向后查找合适的上下文结束位置
     */
    private fun findContextEnd(text: String, queryEndIndex: Int, maxLength: Int): Int {
        val end = minOf(text.length, queryEndIndex + maxLength)
        
        // 在范围内查找句号、换行符等自然断句点
        for (i in queryEndIndex until end) {
            when (text[i]) {
                '.', '。', '\n', '\r' -> {
                    // 找到断句点，包含这个标点符号
                    return i + 1
                }
                ' ', '\t' -> {
                    // 在查询词后20个字符内找到空格，使用它作为断点
                    if (i - queryEndIndex >= 20) {
                        return i
                    }
                }
            }
        }
        
        return end
    }
    
    /**
     * 处理SearchTask的搜索结果
     */
    private fun handleSearchResult(result: SearchTaskResult?) {
        if (result != null && currentListener != null) {
            val searchResult = PDFSearchResult(
                query = result.txt,
                pageNumber = result.pageNumber,
                displayPageNumber = result.pageNumber + 1,
                contextText = "在第 ${result.pageNumber + 1} 页找到匹配项",
                highlightText = result.txt,
                searchBoxes = result.searchBoxes
            )
            
            currentListener?.onSearchResultsFound(result.txt, listOf(searchResult))
        }
    }
    
    override fun stopSearch() {
        isSearching = false
        currentSearchJob?.cancel()
        currentSearchJob = null
        searchTask?.stop()
        
        currentListener?.onSearchCanceled(currentQuery)
        currentListener = null
    }
    
    override fun jumpToSearchResult(result: PDFSearchResult) {
        // 设置搜索结果用于高亮显示
        val searchTaskResult = SearchTaskResult.create(
            result.query,
            result.pageNumber,
            result.searchBoxes
        )
        
        SearchTaskResult.set(searchTaskResult)
    }
    
    override fun clearSearchHighlight() {
        SearchTaskResult.set(null)
        searchResults.clear()
    }
    
    override fun isSearching(): Boolean = isSearching
    
    /**
     * 获取当前搜索结果
     */
    fun getCurrentSearchResults(): List<PDFSearchResult> = searchResults.toList()
    
    /**
     * 释放资源
     */
    fun release() {
        stopSearch()
        searchTask = null
        muPDFCore = null
    }
}
