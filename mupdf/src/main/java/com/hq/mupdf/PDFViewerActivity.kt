package com.hq.mupdf

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.hq.mupdf.toolbar.EnhancedPDFToolbar
import com.hq.mupdf.toolbar.PDFToolbarListener
import com.hq.mupdf.toolbar.ToolType
import com.hq.mupdf.toolbar.ZoomType
import com.hq.mupdf.bottombar.PDFBottomBar
import com.hq.mupdf.bottombar.PDFBottomBarListener
import com.hq.mupdf.header.PDFHeader
import com.hq.mupdf.header.PDFHeaderConfig
import com.hq.mupdf.viewer.PDFViewer
import com.hq.mupdf.viewer.PDFViewerConfig
import com.hq.mupdf.listeners.PDFViewerHeaderListener
import com.hq.mupdf.listeners.PDFViewerDocumentListener

import android.widget.Toast
import com.hq.mupdf.thumbnail.PDFThumbnailBar
import com.hq.mupdf.search.PDFSearchBottomSheet
import com.hq.mupdf.search.PDFSearchController
import com.hq.mupdf.interfaces.PDFNavigationController
import com.hq.mupdf.interfaces.PDFSearchResult
import java.io.File

/**
 * PDF查看器Activity
 * 基于HTML样式1:1还原的PDF查看器
 * 支持左右滑动翻页和底部页码控制
 */
class PDFViewerActivity : AppCompatActivity(), PDFNavigationController, EnhancedPDFToolbar.EnhancedPDFToolbarListener {
    
    companion object {
        private const val TAG = "PDFViewerActivity"
        
        // Intent参数常量
        const val EXTRA_PDF_URI = "pdf_uri"
        const val EXTRA_PDF_NAME = "pdf_name"
        const val EXTRA_PDF_SIZE = "pdf_size"
        const val EXTRA_PDF_PATH = "pdf_path"
    }
    
    // PDF文件信息
    private var pdfUri: Uri? = null
    private var pdfName: String? = null
    private var pdfSize: Long = 0
    private var pdfPath: String? = null
    
    // PDF查看器组件
    private lateinit var pdfViewer: PDFViewer
    
    // UI组件
    private lateinit var loadingOverlay: FrameLayout
    
    // 头部管理器
    private lateinit var pdfHeader: PDFHeader
    
    // 工具栏管理器
    private lateinit var pdfToolbar: EnhancedPDFToolbar
    
    // 底部栏管理器
    private lateinit var pdfBottomBar: PDFBottomBar
    
    // 独立的监听器实例
    private lateinit var headerListener: PDFViewerHeaderListener
    private lateinit var toolbarListener: PDFToolbarListener
    private lateinit var bottomBarListener: PDFBottomBarListener
    private lateinit var documentListener: PDFViewerDocumentListener
    
    // 缩略图相关
    private lateinit var pdfThumbnailBar: PDFThumbnailBar
    private var isThumbnailVisible = false  // 初始状态，将在缩略图栏初始化后更新
    
    // 搜索相关
    private lateinit var searchController: PDFSearchController
    private var searchBottomSheet: PDFSearchBottomSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer_new)
        
        // 初始化UI
        initViews()
        
        // 获取传递的PDF信息
        retrievePDFInfo()
        
        // 设置UI事件监听
        setupUIListeners()
        
        // 加载PDF文档
        loadPDFDocumentWithViewer()
        
        Log.d(TAG, "PDFViewerActivity created successfully")
    }
    
    /**
     * 创建独立的监听器实例
     */
    private fun createListeners() {
        // 创建Header监听器
        headerListener = PDFViewerHeaderListener(
            context = this,
            onBackPressed = { onBackPressedDispatcher.onBackPressed() },
            onSearchRequested = { showSearchBottomSheet() },
            onBookmarkToggle = { isBookmarked -> 
                // 书签状态变化处理
                Log.d(TAG, "书签状态: $isBookmarked")
            },
            onSaveRequested = { fileName, currentPage ->
                // 分享功能处理
                Log.d(TAG, "分享文档: $fileName, 页码: $currentPage")
            },
            onMenuRequested = { 
                // 菜单功能处理
                Log.d(TAG, "菜单按钮点击")
            },
            onTitleTapped = { fileName -> 
                // 标题点击处理
                Log.d(TAG, "标题点击: $fileName")
            },
            onPageInfoTapped = { currentPage, totalPages -> 
                // 页码信息点击处理
                Log.d(TAG, "页码点击: $currentPage/$totalPages")
            }
        )
    }
    
    /**
     * 创建非PDFViewer监听器（简化版本）
     */
    private fun createNonViewerListeners() {
        // 简化的Toolbar监听器（不依赖pdfViewer）
        toolbarListener = object : PDFToolbarListener {
            override fun onToolSelected(toolType: ToolType) {
                Log.d(TAG, "工具选择: ${toolType.toolName}")
                // 在这里使用this@PDFViewerActivity.pdfViewer
                // 处理工具选择逻辑
            }
            
            override fun onZoomAction(zoomType: ZoomType) {
                Log.d(TAG, "缩放动作: $zoomType")
                // 在这里使用this@PDFViewerActivity.pdfViewer
                when (zoomType) {
                    ZoomType.ZOOM_IN -> this@PDFViewerActivity.pdfViewer.zoomIn()
                    ZoomType.ZOOM_OUT -> this@PDFViewerActivity.pdfViewer.zoomOut()
                    ZoomType.FIT_WIDTH -> this@PDFViewerActivity.pdfViewer.fitWidth()
                    ZoomType.CUSTOM -> Log.d(TAG, "执行自定义缩放操作")
                }
            }
            
            override fun onToolbarVisibilityChanged(isVisible: Boolean) {
                Log.d(TAG, "工具栏可见性: $isVisible")
            }
        }
        
        // 简化的BottomBar监听器（不依赖pdfViewer）
        bottomBarListener = object : PDFBottomBarListener {
            override fun onPageJump(targetPage: Int, totalPages: Int): Boolean {
                if (targetPage > 0 && targetPage <= totalPages) {
                    this@PDFViewerActivity.pdfViewer.jumpToPage(targetPage - 1) // 转换为0基索引
                    return true
                }
                return false
            }
            
            override fun onPreviousPage(currentPage: Int): Boolean {
                return if (currentPage > 1) {
                    this@PDFViewerActivity.pdfViewer.goToPreviousPage()
                    true
                } else {
                    showToast(getString(R.string.already_first_page))
                    false
                }
            }
            
            override fun onNextPage(currentPage: Int): Boolean {
                return if (currentPage < this@PDFViewerActivity.pdfViewer.getTotalPages()) {
                    this@PDFViewerActivity.pdfViewer.goToNextPage()
                    true
                } else {
                    showToast(getString(R.string.already_last_page))
                    false
                }
            }
            
            override fun onThumbnailToggle() {
                toggleThumbnails()
            }
            
            override fun onDirectionToggle(isHorizontal: Boolean) {
                Log.d(TAG, "Activity - onDirectionToggle called with: $isHorizontal")
                
                // 设置PDF查看器的方向
                this@PDFViewerActivity.pdfViewer.setViewDirection(isHorizontal)
                
                // 方向切换时可以显示提示信息
//                showToast("已切换为${if (isHorizontal) "横向" else "竖向"}查看")
            }
            
            override fun onBottomBarVisibilityChanged(isVisible: Boolean) {
                // 可以在这里处理底部栏可见性变化的逻辑
            }
        }
    }
    
    /**
     * 创建PDFViewer监听器（直接创建）
     */
    private fun createViewerListener(): PDFViewerDocumentListener {
        return PDFViewerDocumentListener(
            context = this,
            pdfHeader = pdfHeader,
            pdfBottomBar = pdfBottomBar,
            pdfToolbar = pdfToolbar,
            onDocumentReady = { pageCount, documentInfo -> 
                Log.d(TAG, "文档准备完成: ${documentInfo.fileName}, 共${pageCount}页")
                // 初始化缩略图管理器
                initializeThumbnailBar()
                // 设置初始缩放级别为100%
                pdfToolbar.setZoomLevel(100f)
                // 设置搜索控制器的MuPDFCore引用
                searchController.setMuPDFCore(pdfViewer.getPDFCore())
                // 设置PDFCore，提供备用搜索机制（保持向后兼容）
                pdfHeader.setPDFCore(pdfViewer.getPDFCore())

                Log.d(TAG, "文档初始化完成")
            },
            onDocumentError = { error -> 
                Log.e(TAG, "文档加载错误: $error")
            },
            onPageNavigation = { currentPage, totalPages -> 
                Log.d(TAG, "页面导航: ${currentPage + 1}/$totalPages")
                // 更新缩略图当前页面
                if (::pdfThumbnailBar.isInitialized) {
                    pdfThumbnailBar.setCurrentPage(currentPage)
                }
                // 页面导航完成
            },
            onZoomChanged = { zoomLevel -> 
                Log.d(TAG, "缩放级别变化: $zoomLevel")
                // 同步更新工具栏显示（作为备用机制）
                val zoomPercent = (zoomLevel * 100).toInt()
                pdfToolbar.setZoomLevel(zoomPercent.toFloat())
            },
            onPageInteraction = { pageIndex, x, y -> 
                Log.d(TAG, "页面交互: 第${pageIndex + 1}页, 坐标($x, $y)")
            },
            onViewerError = { error -> 
                Log.e(TAG, "查看器错误: $error")
            }
        )
    }
    
    /**
     * 初始化视图组件
     */
    private fun initViews() {
        // 主要视图组件
        loadingOverlay = findViewById(R.id.loadingOverlay)
        
        // 创建独立的监听器实例
        createListeners()
        
        // 初始化头部管理器
        val headerConfig = PDFHeaderConfig.default()
        val headerContainer = findViewById<View>(R.id.pdfHeader)
        pdfHeader = PDFHeader(this, headerContainer, headerConfig, headerListener)

        // 缩略图管理器将在PDF加载后初始化
        
        // 现在创建所有监听器（除了PDFViewer监听器外）
        createNonViewerListeners()
        
        // 初始化工具栏和底部栏管理器（使用创建好的监听器）
        pdfToolbar = EnhancedPDFToolbar(this, findViewById(android.R.id.content), this)
        pdfBottomBar = PDFBottomBar(this, findViewById(android.R.id.content), bottomBarListener)
        
        // 初始化PDF查看器（使用直接创建的监听器）
        val viewerConfig = PDFViewerConfig(
            enableNativeGestures = true,  // 启用官方手势支持，包括缩放
            horizontalScrolling = false   // 设为false启用垂直滑动（上下翻页）
        )
        pdfViewer = PDFViewer(this, loadingOverlay, viewerConfig, createViewerListener())
        
        // 初始化搜索控制器
        searchController = PDFSearchController(this)
        
        // 为支持setListener的组件设置监听器  
        pdfToolbar.setListener(this)
        
        // 设置缩放变化监听器，同步更新工具栏显示
        pdfViewer.setOnZoomChangeListener { zoomLevel ->
            // 将缩放级别转换为百分比并更新工具栏显示
            val zoomPercent = (zoomLevel * 100).toInt()
            pdfToolbar.setZoomLevel(zoomPercent.toFloat())
            Log.d(TAG, "Zoom changed to: ${zoomPercent}%")
        }

        pdfViewer.getPDFCore()?.let { pdfCore ->
//            textSelectionManager = SimpleTextSelectionManager(this, pdfCore, pdfViewer)
        }
    }
    
    /**
     * 从Intent中获取PDF文件信息
     */
    private fun retrievePDFInfo() {
        intent?.let { intent ->
            // 获取PDF URI
            pdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_PDF_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_PDF_URI)
            }
            
            // 获取PDF文件名
            pdfName = intent.getStringExtra(EXTRA_PDF_NAME) ?: getString(R.string.unknown_file)
            
            // 获取PDF文件大小（未知时使用 -1）
            pdfSize = intent.getLongExtra(EXTRA_PDF_SIZE, -1)
            
            // 获取PDF文件路径
            pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
            
            Log.d(TAG, "PDF Info - Name: $pdfName, Size: $pdfSize, Path: $pdfPath, Uri: $pdfUri")
            
            // 验证必要参数：允许使用路径或URI，至少一个存在
            if (pdfUri == null && (pdfPath.isNullOrEmpty() || !File(pdfPath!!).exists())) {
                Log.e(TAG, "Both pdfUri and valid pdfPath are missing")
                showError(getString(R.string.cannot_open_document))
                finish()
                return
            }
        } ?: run {
            Log.e(TAG, "Intent is null")
            showError(getString(R.string.cannot_open_document))
            finish()
        }
    }
    
    /**
     * 设置UI事件监听
     */
    private fun setupUIListeners() {
        // 注意：头部按钮的事件监听现在由PDFHeader管理
        // 通过PDFHeaderListener接口处理事件
        
        // 注意：底部栏的事件监听现在由PDFBottomBar管理
        // 通过PDFBottomBarListener接口处理事件
        
        // 注意：页面变化监听现在由PDFViewer管理
        // 通过PDFViewerListener接口处理事件
    }
    
    /**
     * 使用新的PDF查看器加载文档
     */
    private fun loadPDFDocumentWithViewer() {
        try {
            when {
                !pdfPath.isNullOrEmpty() && File(pdfPath!!).exists() -> {
                    pdfViewer.loadFromFile(pdfPath!!, pdfName, pdfSize)
                }
                pdfUri != null -> {
                    pdfViewer.loadFromUri(pdfUri!!, pdfName, pdfSize)
                }
                else -> {
                    showError(getString(R.string.cannot_open_document))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PDF loading", e)
            showError(getString(R.string.cannot_open_document_Reason, e.message ?: ""))
        }
    }
    



    
    /**
     * 初始化缩略图管理器
     */
    private fun initializeThumbnailBar() {
        try {
            val pdfCore = pdfViewer.getPDFCore()
            if (pdfCore == null) {
                Log.w(TAG, "Cannot initialize thumbnail bar: PDFCore is null")
                return
            }
            
            pdfThumbnailBar = PDFThumbnailBar(
                activity = this,
                rootView = findViewById(android.R.id.content),
                pdfCore = pdfCore,
                onThumbnailClick = { pageIndex ->
                    pdfViewer.jumpToPage(pageIndex)
                },
                onVisibilityChanged = { isVisible ->
                    // 同步更新底部栏按钮状态
                    isThumbnailVisible = isVisible
                    pdfBottomBar.updateThumbnailButtonState(isVisible)
                }
            )
            
            // 同步缩略图栏的显示状态
            isThumbnailVisible = pdfThumbnailBar.isThumbnailBarVisible()
            
            // 更新底部栏按钮状态
            pdfBottomBar.updateThumbnailButtonState(isThumbnailVisible)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize thumbnail bar", e)
        }
    }
    
    /**
     * 切换缩略图显示/隐藏
     */
    private fun toggleThumbnails() {
        if (!::pdfThumbnailBar.isInitialized) {
            showToast(getString(R.string.loading_pdf_page))
            return
        }
        
        // 在切换前先同步状态
        pdfThumbnailBar.syncThumbnailBarState()
        
        pdfThumbnailBar.toggleThumbnailBar()
        
        // 延迟更新状态，确保动画已开始
        Handler(Looper.getMainLooper()).postDelayed({
            isThumbnailVisible = pdfThumbnailBar.isThumbnailBarVisible()
            // 更新按钮状态
            pdfBottomBar.updateThumbnailButtonState(isThumbnailVisible)
        }, 50)
    }
    

    
    /**
     * 显示Toast消息
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示错误消息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, message)
        
        // 显示加载覆盖层
        loadingOverlay.visibility = View.VISIBLE
    }
    
    // ================== 搜索功能实现 ==================
    
    /**
     * 显示搜索BottomSheet
     */
    private fun showSearchBottomSheet() {
        Log.d(TAG, "Showing search bottom sheet")
        
        // 如果已经显示，不重复创建
        if (searchBottomSheet?.isVisible == true) {
            return
        }
        
        searchBottomSheet = PDFSearchBottomSheet.newInstance().apply {
            // 设置搜索控制器
            searchController = { query, listener ->
                this@PDFViewerActivity.searchController.startSearch(query, listener)
            }
            
            // 设置导航控制器
            navigationController = { result ->
                jumpToSearchResult(result)
            }
            
            // 设置停止搜索控制器
            stopSearchController = {
                this@PDFViewerActivity.searchController.stopSearch()
            }
        }
        
        searchBottomSheet?.show(supportFragmentManager, "PDFSearchBottomSheet")
    }
    
    /**
     * 跳转到搜索结果
     */
    private fun jumpToSearchResult(result: PDFSearchResult) {
        Log.d(TAG, "Jumping to search result: page ${result.displayPageNumber}")
        
        // 设置搜索高亮
        searchController.jumpToSearchResult(result)
        
        // 跳转到指定页面
        jumpToPage(result.pageNumber)
        
        // 刷新ReaderView以显示搜索高亮
        pdfViewer.refreshCurrentPage()
    }
    
    // ================== PDFNavigationController 接口实现 ==================
    
    override fun jumpToPage(pageIndex: Int) {
        Log.d(TAG, "Jumping to page: ${pageIndex + 1}")
        pdfViewer.jumpToPage(pageIndex)
    }
    
    override fun getCurrentPage(): Int {
        return pdfViewer.getCurrentPageNumber()
    }
    
    override fun getTotalPages(): Int {
        return pdfViewer.getTotalPages()
    }
    
    override fun onZoomIn() {
        pdfViewer.zoomIn()
    }
    
    override fun onZoomOut() {
        pdfViewer.zoomOut()
    }
    
    override fun onFitWidth() {
        pdfViewer.fitWidth()
    }
    
    /**
     * 显示成功Toast
     */
    private fun showSuccessToast(message: String) {
        Toast.makeText(this, "✅ $message", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示错误Toast
     */
    private fun showErrorToast(message: String) {
        Toast.makeText(this, "❌ $message", Toast.LENGTH_SHORT).show()
    }

    
    override fun onDestroy() {
        super.onDestroy()
        
        // 清理资源
        pdfViewer.onDestroy()
        pdfToolbar.onDestroy()
        pdfBottomBar.onDestroy()
        pdfHeader.onDestroy()
        
        // 清理缩略图管理器
        if (::pdfThumbnailBar.isInitialized) {
            pdfThumbnailBar.onDestroy()
        }
        
        // 清理搜索控制器
        searchController.release()
        searchBottomSheet?.dismiss()
        
        Log.d(TAG, "PDFViewerActivity destroyed")
    }
    

}
