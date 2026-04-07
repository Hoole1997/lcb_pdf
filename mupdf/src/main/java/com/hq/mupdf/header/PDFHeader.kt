package com.hq.mupdf.header

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hq.mupdf.R
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.viewer.SearchTask
import com.hq.mupdf.viewer.SearchTaskResult

/**
 * PDF Header组件封装类
 * 负责管理PDF文档头部的显示和交互功能
 * 
 * 主要功能：
 * - 文档标题和页码信息显示
 * - 导航和操作按钮管理
 * - 用户交互事件处理
 * - 动态配置和样式调整
 */
class PDFHeader(
    private val context: Context,
    private val headerContainer: View,
    private val config: PDFHeaderConfig = PDFHeaderConfig.default(),
    private val listener: PDFHeaderListener? = null,
    private val stateListener: PDFHeaderStateListener? = null
) {
    
    companion object {
        private const val TAG = "PDFHeader"
    }
    
    // UI控件引用
    private val pdfBackBtn: ImageButton = headerContainer.findViewById(R.id.pdfBackBtn)
    private val pdfFileName: TextView = headerContainer.findViewById(R.id.pdfFileName)
    private val pdfPageInfo: TextView = headerContainer.findViewById(R.id.pdfPageInfo)
    private val searchPdfBtn: ImageButton = headerContainer.findViewById(R.id.searchPdfBtn)
    private val bookmarkPdfBtn: ImageButton = headerContainer.findViewById(R.id.bookmarkPdfBtn)
    private val savePdfBtn: ImageButton = headerContainer.findViewById(R.id.savePdfBtn)
    private val menuPdfBtn: ImageButton = headerContainer.findViewById(R.id.menuPdfBtn)
    
    // 状态变量
    private var isVisible: Boolean = true
    private var isBookmarked: Boolean = false
    private var currentFileName: String? = null
    private var currentPage: Int = 1
    private var totalPages: Int = 1
    private var isLoading: Boolean = false
    
    // 搜索相关（保留用于向后兼容）
    private var muPDFCore: MuPDFCore? = null
    private var searchTask: SearchTask? = null
    private var onSearchResultListener: ((SearchTaskResult?) -> Unit)? = null
    
    init {
        Log.d(TAG, "Initializing PDFHeader with config: $config")
        setupUI()
        setupEventListeners()
        applyConfiguration()
    }
    
    /**
     * 设置UI组件初始状态
     */
    private fun setupUI() {
        // 设置初始文本
        pdfFileName.text = "加载中..."
        pdfPageInfo.text = "- / -"
        
        // 设置书签按钮初始状态
        updateBookmarkButton(false)
        
        Log.d(TAG, "UI components initialized")
    }
    
    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 返回按钮
        pdfBackBtn.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            listener?.onBackButtonClick()
        }
        
        // 搜索按钮
        searchPdfBtn.setOnClickListener {
            Log.d(TAG, "Search button clicked")
            listener?.onSearchButtonClick()
        }
        
        // 书签按钮
        bookmarkPdfBtn.setOnClickListener {
            Log.d(TAG, "Bookmark button clicked, current state: $isBookmarked")
            toggleBookmark()
            listener?.onBookmarkButtonClick(isBookmarked)
        }
        
        // 分享按钮
        savePdfBtn.setOnClickListener {
            Log.d(TAG, "Share button clicked, file: $currentFileName, page: $currentPage")
            listener?.onSaveButtonClick(currentFileName, currentPage)
        }
        
        // 菜单按钮
        menuPdfBtn.setOnClickListener {
            Log.d(TAG, "Menu button clicked")
            listener?.onMenuButtonClick()
        }
        
        // 标题点击
        pdfFileName.setOnClickListener {
            Log.d(TAG, "Title clicked: $currentFileName")
            listener?.onTitleClick(currentFileName)
        }
        
        // 页码信息点击
        pdfPageInfo.setOnClickListener {
            Log.d(TAG, "Page info clicked: $currentPage/$totalPages")
            listener?.onPageInfoClick(currentPage, totalPages)
        }
        
        Log.d(TAG, "Event listeners setup completed")
    }
    
    /**
     * 应用配置设置
     */
    private fun applyConfiguration() {
        // 按钮可见性
        pdfBackBtn.visibility = if (config.showBackButton) View.VISIBLE else View.GONE
        searchPdfBtn.visibility = if (config.showSearchButton) View.VISIBLE else View.GONE
        bookmarkPdfBtn.visibility = if (config.showBookmarkButton) View.VISIBLE else View.GONE
        savePdfBtn.visibility = if (config.showShareButton) View.VISIBLE else View.GONE
        menuPdfBtn.visibility = if (config.showMenuButton) View.VISIBLE else View.GONE
        pdfPageInfo.visibility = if (config.showPageInfo) View.VISIBLE else View.GONE
        
        // 文本样式
        pdfFileName.maxLines = config.titleMaxLines
        pdfFileName.textSize = config.titleTextSize
        pdfPageInfo.textSize = config.pageInfoTextSize
        
        // 背景色
        config.backgroundColorRes?.let { colorRes ->
            headerContainer.setBackgroundColor(ContextCompat.getColor(context, colorRes))
        }
        
        // 高度设置
        val layoutParams = headerContainer.layoutParams
        layoutParams.height = (config.heightDp * context.resources.displayMetrics.density).toInt()
        headerContainer.layoutParams = layoutParams
        
        // 边距设置
        val paddingPx = (config.horizontalPaddingDp * context.resources.displayMetrics.density).toInt()
        headerContainer.setPadding(paddingPx, headerContainer.paddingTop, paddingPx, headerContainer.paddingBottom)
        
        // 按钮大小
        val buttonSizePx = (config.buttonSizeDp * context.resources.displayMetrics.density).toInt()
        listOf(pdfBackBtn, searchPdfBtn, bookmarkPdfBtn, savePdfBtn, menuPdfBtn).forEach { button ->
            val buttonParams = button.layoutParams
            buttonParams.width = buttonSizePx
            buttonParams.height = buttonSizePx
            button.layoutParams = buttonParams
        }
        
        // 阴影效果
        if (config.enableElevation) {
            headerContainer.elevation = config.elevationDp * context.resources.displayMetrics.density
        } else {
            headerContainer.elevation = 0f
        }
        
        Log.d(TAG, "Configuration applied successfully")
    }
    
    // ===================
    // 公共API方法
    // ===================
    
    /**
     * 更新文档信息
     * @param fileName 文件名
     * @param currentPage 当前页码
     * @param totalPages 总页数
     */
    fun updateDocumentInfo(fileName: String?, currentPage: Int, totalPages: Int) {
        this.currentFileName = fileName
        this.currentPage = currentPage
        this.totalPages = totalPages
        
        // 更新UI
        pdfFileName.text = fileName ?: "未知文件"
        updatePageInfo(currentPage, totalPages)
        
        Log.d(TAG, "Document info updated: $fileName, page $currentPage/$totalPages")
    }
    
    /**
     * 更新页码信息
     * @param currentPage 当前页码
     * @param totalPages 总页数
     */
    fun updatePageInfo(currentPage: Int, totalPages: Int) {
        this.currentPage = currentPage
        this.totalPages = totalPages
        
        pdfPageInfo.text = "$currentPage / $totalPages"
        
        Log.d(TAG, "Page info updated: $currentPage/$totalPages")
    }
    
    /**
     * 设置文件名
     * @param fileName 文件名
     */
    fun setFileName(fileName: String?) {
        this.currentFileName = fileName
        pdfFileName.text = fileName ?: "未知文件"
        
        Log.d(TAG, "File name set: $fileName")
    }
    
    /**
     * 设置加载状态
     * @param isLoading 是否正在加载
     */
    fun setLoadingState(isLoading: Boolean) {
        this.isLoading = isLoading
        
        if (isLoading) {
            pdfFileName.text = "加载中..."
            pdfPageInfo.text = "- / -"
            // 禁用操作按钮
            setButtonsEnabled(false)
        } else {
            // 恢复文件名显示
            pdfFileName.text = currentFileName ?: "未知文件"
            updatePageInfo(currentPage, totalPages)
            // 启用操作按钮
            setButtonsEnabled(true)
        }
        
        Log.d(TAG, "Loading state changed: $isLoading")
    }
    
    /**
     * 设置书签状态
     * @param isBookmarked 是否已添加书签
     */
    fun setBookmarkState(isBookmarked: Boolean) {
        this.isBookmarked = isBookmarked
        updateBookmarkButton(isBookmarked)
        stateListener?.onBookmarkStateChanged(isBookmarked)
        
        Log.d(TAG, "Bookmark state set: $isBookmarked")
    }
    
    /**
     * 切换书签状态
     */
    fun toggleBookmark() {
        setBookmarkState(!isBookmarked)
    }
    
    /**
     * 显示Header
     */
    fun show() {
        if (!isVisible) {
            headerContainer.visibility = View.VISIBLE
            isVisible = true
            stateListener?.onHeaderVisibilityChanged(true)
            Log.d(TAG, "Header shown")
        }
    }
    
    /**
     * 隐藏Header
     */
    fun hide() {
        if (isVisible) {
            headerContainer.visibility = View.GONE
            isVisible = false
            stateListener?.onHeaderVisibilityChanged(false)
            Log.d(TAG, "Header hidden")
        }
    }
    
    /**
     * 切换Header可见性
     */
    fun toggle() {
        if (isVisible) {
            hide()
        } else {
            show()
        }
    }
    
    /**
     * 检查Header是否可见
     */
    fun isVisible(): Boolean = isVisible
    
    /**
     * 检查是否已添加书签
     */
    fun isBookmarked(): Boolean = isBookmarked
    
    /**
     * 获取当前文件名
     */
    fun getCurrentFileName(): String? = currentFileName
    
    /**
     * 获取当前页码
     */
    fun getCurrentPage(): Int = currentPage
    
    /**
     * 获取总页数
     */
    fun getTotalPages(): Int = totalPages
    
    /**
     * 检查是否正在加载
     */
    fun isLoading(): Boolean = isLoading
    
    /**
     * 获取Header容器视图
     */
    fun getHeaderView(): View = headerContainer
    
    /**
     * 获取配置信息
     */
    fun getConfig(): PDFHeaderConfig = config
    
    /**
     * 获取搜索按钮视图（用于引导等功能）
     */
    fun getSearchButton(): View = searchPdfBtn
    
    // ===================
    // 私有辅助方法
    // ===================
    
    /**
     * 更新书签按钮样式
     */
    private fun updateBookmarkButton(isBookmarked: Boolean) {
        val iconRes = if (isBookmarked) {
            R.drawable.ic_bookmark // 已添加书签的图标
        } else {
            R.drawable.ic_bookmark_border // 未添加书签的图标
        }
        
        bookmarkPdfBtn.setImageResource(iconRes)
        
        bookmarkPdfBtn.imageTintList = ContextCompat.getColorStateList(context, 
            if (isBookmarked) R.color.pdf_primary_color else R.color.black)
    }
    
    /**
     * 设置操作按钮启用状态
     */
    private fun setButtonsEnabled(enabled: Boolean) {
        listOf(searchPdfBtn, bookmarkPdfBtn, savePdfBtn, menuPdfBtn).forEach { button ->
            button.isEnabled = enabled
            button.alpha = if (enabled) 1.0f else 0.5f
        }
    }
    
    // ===================
    // 搜索功能相关方法
    // ===================
    
    /**
     * 设置PDF核心对象
     * @param core MuPDFCore实例
     */
    fun setPDFCore(core: MuPDFCore?) {
        this.muPDFCore = core
        initializeSearchTask()
    }
    

    
    /**
     * 设置搜索结果监听器
     * @param listener 搜索结果回调
     */
    fun setOnSearchResultListener(listener: (SearchTaskResult?) -> Unit) {
        this.onSearchResultListener = listener
    }
    
    /**
     * 初始化搜索任务
     */
    private fun initializeSearchTask() {
        muPDFCore?.let { core ->
            searchTask = object : SearchTask(context, core) {
                override fun onTextFound(result: SearchTaskResult?) {
                    Log.d(TAG, "Search result found: $result")
                    SearchTaskResult.set(result)
                    
                    // 通知外部监听器
                    onSearchResultListener?.invoke(result)
                }
            }
        }
    }
    

    

    

    

    

    
    /**
     * 清理资源
     */
    fun onDestroy() {
        Log.d(TAG, "PDFHeader destroyed")
        
        // 清理搜索相关资源
        searchTask?.stop()
        searchTask = null
        muPDFCore = null
        onSearchResultListener = null
        
        // 清理监听器引用，防止内存泄漏
        pdfBackBtn.setOnClickListener(null)
        searchPdfBtn.setOnClickListener(null)
        bookmarkPdfBtn.setOnClickListener(null)
        savePdfBtn.setOnClickListener(null)
        menuPdfBtn.setOnClickListener(null)
        pdfFileName.setOnClickListener(null)
        pdfPageInfo.setOnClickListener(null)
    }
}
