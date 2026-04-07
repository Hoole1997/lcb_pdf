package com.hq.mupdf.viewer

/**
 * PDF查看器事件监听器接口
 * 定义PDF查看器与外部组件交互的回调方法
 */
interface PDFViewerListener {
    
    /**
     * PDF文档加载开始
     */
    fun onDocumentLoadStart()
    
    /**
     * PDF文档加载成功
     * @param pageCount 总页数
     * @param documentInfo 文档信息
     */
    fun onDocumentLoadSuccess(pageCount: Int, documentInfo: PDFDocumentInfo)
    
    /**
     * PDF文档加载失败
     * @param error 错误信息
     */
    fun onDocumentLoadError(error: String)
    
    /**
     * 页面发生变化
     * @param currentPage 当前页码（从0开始）
     * @param totalPages 总页数
     */
    fun onPageChanged(currentPage: Int, totalPages: Int)
    
    /**
     * 页面渲染开始
     * @param pageIndex 页面索引（从0开始）
     */
    fun onPageRenderStart(pageIndex: Int)
    
    /**
     * 页面渲染成功
     * @param pageIndex 页面索引（从0开始）
     */
    fun onPageRenderSuccess(pageIndex: Int)
    
    /**
     * 页面渲染失败
     * @param pageIndex 页面索引（从0开始）
     * @param error 错误信息
     */
    fun onPageRenderError(pageIndex: Int, error: String)
    
    /**
     * 缩放级别发生变化
     * @param zoomLevel 当前缩放级别
     */
    fun onZoomChanged(zoomLevel: Float)
    
    /**
     * 查看方向发生变化
     * @param isHorizontal true为横向，false为竖向
     */
    fun onDirectionChanged(isHorizontal: Boolean)
    
    /**
     * 单击页面事件
     * @param pageIndex 页面索引（从0开始）
     * @param x 点击位置X坐标（相对于页面）
     * @param y 点击位置Y坐标（相对于页面）
     */
    fun onPageClick(pageIndex: Int, x: Float, y: Float)
    
    /**
     * 长按页面事件
     * @param pageIndex 页面索引（从0开始）
     * @param x 长按位置X坐标（相对于页面）
     * @param y 长按位置Y坐标（相对于页面）
     */
    fun onPageLongPress(pageIndex: Int, x: Float, y: Float)
    
    /**
     * 搜索开始事件
     * @param query 搜索关键词
     */
    fun onSearchStart(query: String)
    
    /**
     * 搜索结果找到
     * @param query 搜索关键词
     * @param pageIndex 找到结果的页面索引（从0开始）
     * @param totalMatches 该页面的匹配数量
     */
    fun onSearchResultFound(query: String, pageIndex: Int, totalMatches: Int)
    
    /**
     * 搜索结果未找到
     * @param query 搜索关键词
     */
    fun onSearchResultNotFound(query: String)
    
    /**
     * 搜索结束或取消
     */
    fun onSearchEnd()
    
    /**
     * 查看器错误事件
     * @param error 错误信息
     */
    fun onViewerError(error: String)
}

/**
 * PDF文档信息数据类
 */
data class PDFDocumentInfo(
    val fileName: String,
    val fileSize: Long,
    val pageCount: Int,
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val creationDate: String? = null,
    val modificationDate: String? = null
)

/**
 * PDF查看器配置类
 */
data class PDFViewerConfig(
    // 渲染配置
    val scaleMultiplier: Float = 1.15f,
    val maxZoomLevel: Float = 5.0f,
    val minZoomLevel: Float = 0.5f,
    val defaultZoomLevel: Float = 1.0f,
    
    // 页面配置
    val offscreenPageLimit: Int = 1,
    val pageMarginDp: Int = 2,
    val enablePageTransformer: Boolean = true,
    
    // 手势配置
    val enablePinchToZoom: Boolean = true,
    val enableDoubleTapZoom: Boolean = true,
    val enableSwipeNavigation: Boolean = true,
    val enableNativeGestures: Boolean = true, // 使用官方ReaderView支持手势缩放
    val horizontalScrolling: Boolean = true, // true=水平滑动(左右翻页), false=垂直滑动(上下翻页)
    
    // 性能配置
    val enablePageCache: Boolean = true,
    val cacheSize: Int = 10,
    val preloadPages: Int = 2,
    
    // UI配置
    val showPageNumber: Boolean = true,
    val showLoadingIndicator: Boolean = true,
    val backgroundColor: Int = android.graphics.Color.WHITE
)
