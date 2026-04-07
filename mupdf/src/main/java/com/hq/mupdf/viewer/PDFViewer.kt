package com.hq.mupdf.viewer

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.viewer.ReaderView
import com.hq.mupdf.viewer.PageAdapter
import com.hq.mupdf.viewer.ContentInputStream
import com.hq.mupdf.viewer.SearchTask
import com.hq.mupdf.viewer.SearchTaskResult
import com.hq.mupdf.textselection.TextSelectionManager

import kotlinx.coroutines.*
import java.io.File

/**
 * PDF查看器核心类 - 基于ReaderView的简化实现
 * 移除ViewPager2依赖，仅使用原生MuPDF ReaderView组件
 * 支持手势缩放、翻页等完整功能
 */
class PDFViewer(
    private val context: Context,
    private val loadingOverlay: FrameLayout,
    private val config: PDFViewerConfig = PDFViewerConfig(),
    private val listener: PDFViewerListener? = null
) {
    

    
    // PDF核心组件
    private var pdfCore: MuPDFCore? = null
    private var readerView: ReaderView? = null
    private var pageAdapter: PageAdapter? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 文档状态
    private var isDocumentLoaded = false
    private var currentPage = 0
    private var totalPages = 0
    private var currentZoomLevel = 1.0f
    private var documentInfo: PDFDocumentInfo? = null
    
    // 搜索功能组件
    private var searchTask: SearchTask? = null
    private var searchResultListener: ((SearchTaskResult?) -> Unit)? = null
    
    // 文本选择管理器
    private var textSelectionManager: TextSelectionManager? = null
    
    // 源文件信息
    private var pdfUri: Uri? = null
    private var pdfPath: String? = null
    private var pdfName: String? = null
    private var pdfSize: Long = 0
    
    init {
    }
    
    /**
     * 从文件路径加载PDF文档
     * @param filePath PDF文件路径
     * @param fileName 文件名（可选）
     * @param fileSize 文件大小（可选）
     */
    fun loadFromFile(filePath: String, fileName: String? = null, fileSize: Long = -1) {
        this.pdfPath = filePath
        this.pdfName = fileName ?: File(filePath).name
        this.pdfSize = fileSize
        this.pdfUri = null
        
        loadPDFDocument()
    }
    
    /**
     * 从URI加载PDF文档
     * @param uri PDF文件URI
     * @param fileName 文件名（可选）
     * @param fileSize 文件大小（可选）
     */
    fun loadFromUri(uri: Uri, fileName: String? = null, fileSize: Long = -1) {
        this.pdfUri = uri
        this.pdfName = fileName ?: "未知文件"
        this.pdfSize = fileSize
        this.pdfPath = null
        
        loadPDFDocument()
    }
    
    /**
     * 加载PDF文档的核心方法
     */
    private fun loadPDFDocument() {
        if (isDocumentLoaded) {
            return
        }
        
        showLoadingOverlay(true)
        listener?.onDocumentLoadStart()
        
        scope.launch {
            try {
                val core = withContext(Dispatchers.IO) {
                    createMuPDFCore()
                }
                
                withContext(Dispatchers.Main) {
                    setupDocumentWithCore(core)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoadingOverlay(false)
                    listener?.onDocumentLoadError("加载文档失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 创建MuPDFCore实例
     */
    private suspend fun createMuPDFCore(): MuPDFCore {
        return when {
            pdfPath != null -> {
                val file = File(pdfPath!!)
                val fileBytes = file.readBytes()
                MuPDFCore(fileBytes, "application/pdf")
            }
            pdfUri != null -> {
                val contentResolver = context.contentResolver
                val inputStream = ContentInputStream(contentResolver, pdfUri!!, pdfSize)
                MuPDFCore(inputStream, "application/pdf")
            }
            else -> {
                throw IllegalStateException("No PDF source specified")
            }
        }
    }
    
    /**
     * 使用MuPDF核心设置文档
     */
    private suspend fun setupDocumentWithCore(core: MuPDFCore) {
        try {
            pdfCore = core
            totalPages = core.countPages()
            currentPage = 0
            
            // 创建文档信息
            documentInfo = createDocumentInfo(core)
            
            // 设置ReaderView
            setupReaderView(core)
            
            // 初始化搜索功能
            initializeSearchTask(core)
            
            isDocumentLoaded = true
            showLoadingOverlay(false)
            
            // 通知文档加载完成
            listener?.onDocumentLoadSuccess(totalPages, documentInfo!!)
            listener?.onPageChanged(currentPage, totalPages)
            
        } catch (e: Exception) {
            showLoadingOverlay(false)
            listener?.onDocumentLoadError("设置文档失败: ${e.message}")
        }
    }
    
    /**
     * 创建文档信息对象
     */
    private fun createDocumentInfo(core: MuPDFCore): PDFDocumentInfo {
        return PDFDocumentInfo(
            fileName = pdfName ?: "未知文件",
            fileSize = pdfSize,
            pageCount = totalPages,
            title = core.title ?: "",
            author = "",
            subject = "",
            creator = "",
            producer = "",
            creationDate = "",
            modificationDate = ""
        )
    }
    
    /**
     * 设置官方ReaderView（支持手势缩放）
     */
    private fun setupReaderView(core: MuPDFCore) {
        // 创建扩展的ReaderView来支持页面变化监听
        val reader = object : ReaderView(context) {
            override fun onMoveToChild(i: Int) {
                super.onMoveToChild(i)
                currentPage = i
                listener?.onPageChanged(currentPage, totalPages)
                // 清除文本选择高亮
                textSelectionManager?.cancelSelection()
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // 创建PageAdapter
        val adapter = PageAdapter(context, core)
        reader.adapter = adapter
        
        // 设置当前页面
        reader.displayedViewIndex = currentPage
        
        // 添加到父容器中
        val parentContainer = loadingOverlay.parent as? FrameLayout
        parentContainer?.addView(reader)
        
        // 保存引用
        this.readerView = reader
        this.pageAdapter = adapter
        
        // 如果已经设置了缩放监听器，现在应用到ReaderView
        zoomChangeListener?.let { listener ->
            reader.setOnZoomChangeListener(object : ReaderView.OnZoomChangeListener {
                override fun onZoomChanged(zoomLevel: Float) {
                    listener(zoomLevel)
                }
            })
        }
        
        // 初始化文本选择管理器
        textSelectionManager = TextSelectionManager(context, core, reader)
    }
    
    /**
     * 初始化搜索任务
     */
    private fun initializeSearchTask(core: MuPDFCore) {
        searchTask = object : SearchTask(context, core) {
            override fun onTextFound(result: SearchTaskResult?) {
                SearchTaskResult.set(result)
                
                // 通知监听器搜索结果
                if (result != null) {
                    // 找到搜索结果
                    listener?.onSearchResultFound(
                        result.txt, 
                        result.pageNumber, 
                        result.searchBoxes.size
                    )
                    
                    // 跳转到对应页面
                    readerView?.setDisplayedViewIndex(result.pageNumber)
                    readerView?.resetupChildren() // 重新设置子视图以显示搜索高亮
                    currentPage = result.pageNumber
                    listener?.onPageChanged(currentPage, totalPages)
                    // 清除文本选择高亮
                    textSelectionManager?.cancelSelection()
                } else {
                    // 没有找到搜索结果
                    val currentSearchResult = SearchTaskResult.get()
                    if (currentSearchResult != null) {
                        listener?.onSearchResultNotFound(currentSearchResult.txt)
                    }
                }
                
                // 通知搜索结果监听器
                searchResultListener?.invoke(result)
            }
        }
    }
    
    /**
     * 显示/隐藏加载覆盖层
     */
    private fun showLoadingOverlay(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    // ==============================================
    // 公共API方法
    // ==============================================
    
    /**
     * 跳转到指定页面
     * @param pageIndex 页面索引（从0开始）
     */
    fun goToPage(pageIndex: Int) {
        if (!isDocumentLoaded || pageIndex < 0 || pageIndex >= totalPages) {
            return
        }
        
        readerView?.displayedViewIndex = pageIndex
        currentPage = pageIndex
        listener?.onPageChanged(currentPage, totalPages)
        // 清除文本选择高亮
        textSelectionManager?.cancelSelection()
    }
    
    /**
     * 下一页
     */
    fun nextPage() {
        if (currentPage < totalPages - 1) {
            goToPage(currentPage + 1)
        }
    }
    
    /**
     * 上一页
     */
    fun previousPage() {
        if (currentPage > 0) {
            goToPage(currentPage - 1)
        }
    }
    
    /**
     * 获取当前页码（从1开始）
     */
    fun getCurrentPageNumber(): Int = currentPage + 1
    
    /**
     * 获取总页数
     */
    fun getTotalPages(): Int = totalPages
    
    /**
     * 获取文档信息
     */
    fun getDocumentInfo(): PDFDocumentInfo? = documentInfo
    
    /**
     * 检查文档是否已加载
     */
    fun isLoaded(): Boolean = isDocumentLoaded
    
    /**
     * 获取当前缩放级别
     */
    fun getCurrentZoomLevel(): Float = currentZoomLevel
    
    /**
     * 释放资源
     */
    fun release() {
        scope.cancel()
        
        try {
            // 停止搜索任务
            searchTask?.stop()
            
            readerView?.let { reader ->
                val parent = reader.parent as? FrameLayout
                parent?.removeView(reader)
            }
            pdfCore?.onDestroy()
            pageAdapter = null
            readerView = null
            pdfCore = null
            searchTask = null
            
            isDocumentLoaded = false
            
        } catch (e: Exception) {
            // Error releasing resources
        }
    }
    
    /**
     * 方向切换功能（保留接口兼容性）
     */
    fun setOrientation(isHorizontal: Boolean) {
        // ReaderView原生支持方向变化，无需特殊处理
    }
    
    // ==============================================
    // 兼容性API方法
    // ==============================================
    
        /**
     * 放大页面
     */
    fun zoomIn() {
        readerView?.let { reader ->
            reader.zoomIn()
        }
    }

    /**
     * 缩小页面
     */
    fun zoomOut() {
        readerView?.let { reader ->
            reader.zoomOut()
        }
    }

    /**
     * 适应宽度
     */
    fun fitWidth() {
        readerView?.let { reader ->
            reader.fitToWidth()
        }
    }
    
    /**
     * 获取当前缩放级别
     */
    fun getCurrentZoom(): Float {
        return readerView?.currentZoom ?: 1.0f
    }
    
    /**
     * 设置缩放级别
     */
    fun setZoom(zoom: Float) {
        readerView?.setZoom(zoom)
    }
    
    // 保存缩放监听器，在ReaderView初始化后设置
    private var zoomChangeListener: ((Float) -> Unit)? = null
    
    /**
     * 设置缩放变化监听器
     */
    fun setOnZoomChangeListener(listener: (Float) -> Unit) {
        this.zoomChangeListener = listener
        // 如果ReaderView已经初始化，立即设置监听器
        readerView?.setOnZoomChangeListener(object : ReaderView.OnZoomChangeListener {
            override fun onZoomChanged(zoomLevel: Float) {
                listener(zoomLevel)
            }
        })
    }
    
    /**
     * 跳转到指定页面
     */
    fun jumpToPage(pageIndex: Int) {
        goToPage(pageIndex)
    }
    
    /**
     * 上一页
     */
    fun goToPreviousPage() {
        previousPage()
    }
    
    /**
     * 下一页
     */
    fun goToNextPage() {
        nextPage()
    }
    
    /**
     * 设置查看方向
     */
    fun setViewDirection(isHorizontal: Boolean) {
        readerView?.setScrollDirection(isHorizontal)
    }
    
    /**
     * 获取PDF核心对象
     */
    fun getPDFCore(): MuPDFCore? = pdfCore
    
    /**
     * 检查PDF文档是否可以进行增量保存
     * @return 是否支持增量保存
     */
    fun canBeSavedIncrementally(): Boolean {
        return try {
            val core = pdfCore ?: return false
            val document = core.getDoc()
            val pdfDocument = document.asPDF()
            pdfDocument.canBeSavedIncrementally()
        } catch (e: Exception) {
            android.util.Log.w("PDFViewer", "检查增量保存支持时出错: ${e.message}")
            false
        }
    }
    
    /**
     * 检查PDF是否有未保存的修改
     */
    fun hasUnsavedChanges(): Boolean {
        return try {
            val core = pdfCore ?: return false
            val document = core.getDoc()
            val pdfDocument = document.asPDF()
            pdfDocument.hasUnsavedChanges()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 销毁资源（兼容方法）
     */
    fun onDestroy() {
        release()
    }
    
    /**
     * 检查是否为横向方向
     */
    fun isHorizontalDirection(): Boolean = true // ReaderView自动处理方向
    
    // ==============================================
    // 搜索功能API
    // ==============================================
    
    /**
     * 搜索文本
     * @param query 搜索关键词
     * @param direction 搜索方向 (1: 向前, -1: 向后)
     */
    fun searchText(query: String, direction: Int = 1) {
        if (!isDocumentLoaded || query.isEmpty()) {
            return
        }
        
        // 通知搜索开始
        listener?.onSearchStart(query)
        
        val searchResult = SearchTaskResult.get()
        val searchPage = if (searchResult != null) searchResult.pageNumber else -1
        
        searchTask?.go(query, direction, currentPage, searchPage)
    }
    
    /**
     * 搜索下一个
     */
    fun searchNext() {
        val currentSearchResult = SearchTaskResult.get()
        if (currentSearchResult != null) {
            searchText(currentSearchResult.txt, 1)
        }
    }
    
    /**
     * 搜索上一个
     */
    fun searchPrevious() {
        val currentSearchResult = SearchTaskResult.get()
        if (currentSearchResult != null) {
            searchText(currentSearchResult.txt, -1)
        }
    }
    
    /**
     * 清除搜索结果
     */
    fun clearSearchResults() {
        SearchTaskResult.set(null)
        readerView?.resetupChildren() // 清除搜索高亮
        searchResultListener?.invoke(null)
        
        // 通知搜索结束
        listener?.onSearchEnd()
    }
    
    /**
     * 停止当前搜索
     */
    fun stopSearch() {
        searchTask?.stop()
        
        // 通知搜索结束
        listener?.onSearchEnd()
    }
    
    /**
     * 设置搜索结果监听器
     * @param listener 搜索结果回调 (result: SearchTaskResult?) -> Unit
     */
    fun setOnSearchResultListener(listener: (SearchTaskResult?) -> Unit) {
        this.searchResultListener = listener
    }
    
    /**
     * 获取当前搜索结果
     * @return 当前搜索结果，如果没有则返回null
     */
    fun getCurrentSearchResult(): SearchTaskResult? {
        return SearchTaskResult.get()
    }
    
    /**
     * 检查是否有活跃的搜索
     * @return 是否有搜索结果
     */
    fun hasActiveSearch(): Boolean {
        return SearchTaskResult.get() != null
    }
    
    /**
     * 获取MuPDF核心实例
     * @return MuPDFCore实例，如果未初始化返回null
     */
    fun getCore(): MuPDFCore? {
        return pdfCore
    }
    
    /**
     * 获取文本选择管理器
     * @return TextSelectionManager实例，如果未初始化返回null
     */
    fun getTextSelectionManager(): TextSelectionManager? {
        return textSelectionManager
    }
    
    /**
     * 刷新当前页面以显示搜索高亮等更新
     */
    fun refreshCurrentPage() {
        readerView?.refresh()
    }
    
    /**
     * 清理资源
     */
    fun destroy() {
        textSelectionManager?.destroy()
        textSelectionManager = null
        searchTask = null
        scope.cancel()
    }
}