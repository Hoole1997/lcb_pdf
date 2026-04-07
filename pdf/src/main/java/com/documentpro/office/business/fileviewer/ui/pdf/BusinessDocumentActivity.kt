package com.documentpro.office.business.fileviewer.ui.pdf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityPdfViewBinding
import com.documentpro.office.business.fileviewer.dialog.BusinessFileDetailDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessFileLockDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessFileMoreDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessFileRemakeNameDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessPDFSaveDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessStoreScoreDialog
import com.documentpro.office.business.fileviewer.ui.home.BusinessFileListFragment
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.utils.BusinessPdfUtils
import com.documentpro.office.business.fileviewer.utils.BusinessRecentStorage
import com.documentpro.office.business.fileviewer.utils.BusinessShareUtils
import com.documentpro.office.business.fileviewer.utils.BusinessGuideCallbackController
import com.documentpro.office.business.fileviewer.utils.RandomInterstitialController
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessURIDocumentInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.equalsFileType
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import com.hq.mupdf.R
import com.hq.mupdf.bottombar.PDFBottomBar
import com.hq.mupdf.bottombar.PDFBottomBarListener
import com.hq.mupdf.header.PDFHeader
import com.hq.mupdf.header.PDFHeaderConfig
import com.hq.mupdf.interfaces.PDFNavigationController
import com.hq.mupdf.interfaces.PDFSearchResult
import com.hq.mupdf.listeners.PDFViewerDocumentListener
import com.hq.mupdf.listeners.PDFViewerHeaderListener
import com.hq.mupdf.save.PDFSaveResult
import com.hq.mupdf.search.PDFSearchBottomSheet
import com.hq.mupdf.search.PDFSearchController
import com.hq.mupdf.thumbnail.PDFThumbnailBar
import com.hq.mupdf.toolbar.EnhancedPDFToolbar
import com.hq.mupdf.toolbar.PDFToolbarListener
import com.hq.mupdf.toolbar.ToolType
import com.hq.mupdf.toolbar.ZoomType
import com.hq.mupdf.utils.MuPDFUtils
import com.hq.mupdf.utils.PDFDocumentInfo
import com.hq.mupdf.viewer.PDFViewer
import com.hq.mupdf.viewer.PDFViewerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.documentpro.office.business.fileviewer.R as AppR

class BusinessDocumentActivity : BaseActivity<ActivityPdfViewBinding, BusinessMainModel>(), PDFNavigationController,
    EnhancedPDFToolbar.EnhancedPDFToolbarListener {

    companion object {
        private const val TAG = "BusinessDocumentActivity"
        private const val PARAM_FILE_INFO = "param_file_info"
        private const val PARAM_URI_DOC_INFO = "param_uri_doc_info"

        /**
         * 启动PDF查看器（使用FileInfo）
         * @param context 上下文
         * @param fileInfo 文件信息对象
         */
        fun launch(context: Context, fileInfo: BusinessFileInfo) {
            val intent = Intent(context, BusinessDocumentActivity::class.java)

            // 使用Bundle包装Parcelable对象
            val bundle = Bundle()
            bundle.putParcelable(PARAM_FILE_INFO, fileInfo)
            intent.putExtra("file_bundle", bundle)

            context.startActivity(intent)
        }

        /**
         * 启动PDF查看器（使用URI）
         * @param context 上下文
         * @param uriDocInfo URI文档信息对象
         */
        fun launchFromUri(context: Context, uriDocInfo: BusinessURIDocumentInfo) {
            val intent = Intent(context, BusinessDocumentActivity::class.java)

            // 使用Bundle包装Parcelable对象
            val bundle = Bundle()
            bundle.putParcelable(PARAM_URI_DOC_INFO, uriDocInfo)
            intent.putExtra("uri_bundle", bundle)

            context.startActivity(intent)
        }
    }

    var fileInfo: BusinessFileInfo? = null
    var uriDocInfo: BusinessURIDocumentInfo? = null
    private var mField_1 = false

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
    private var mField_2 = false  // 初始状态，将在缩略图栏初始化后更新

    // 搜索相关
    private lateinit var searchController: PDFSearchController
    private var mField_3: PDFSearchBottomSheet? = null

    override fun initBinding(): ActivityPdfViewBinding {
        return ActivityPdfViewBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessMainModel {
        return viewModels<BusinessMainModel>().value
    }

    override fun finish() {
        RandomInterstitialController.showRandomInterstitial(this, onAdDismissed = {
            super.finish()
        })
    }

    override fun initView() {
        ImmersionBar
            .with(this)
            .transparentStatusBar()
            .statusBarDarkFont(true)
            .hideBar(BarHide.FLAG_HIDE_BAR)
            .init()
        initUI()

        // 加载PDF文档
        execLoad_8 ()

        execLoad_3 ()

        BusinessPointLog.logEvent(
            "Document_Open", mapOf(
                "Document_Type" to "PDF"
            )
        )
        BusinessStoreScoreDialog.checkShow(this)

        loadNative(binding.adViewContainer)
    }

    private fun initUI() {
        // 主要视图组件
        loadingOverlay = findViewById(AppR.id.loadingOverlay)

        // 创建独立的监听器实例
        execSetup_6 ()

        // 初始化头部管理器
        val headerConfig = PDFHeaderConfig.default()
        val headerContainer = findViewById<View>(R.id.pdfHeader)
        pdfHeader = PDFHeader(this, headerContainer, headerConfig, headerListener)

        // 缩略图管理器将在PDF加载后初始化

        // 现在创建所有监听器（除了PDFViewer监听器外）
        execSetup_7 ()

        // 初始化工具栏和底部栏管理器（使用创建好的监听器）
        pdfToolbar = EnhancedPDFToolbar(this, findViewById(android.R.id.content), this)
        pdfBottomBar = PDFBottomBar(this, findViewById(android.R.id.content), bottomBarListener)
        pdfToolbar.toggleToolbar()//主动隐藏工具栏
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closePage()
            }
        })
    }

    /**
     * 统一处理错误，显示提示并安全退出
     */
    private fun execProcess_1(message: String) {
        if (!mField_1 && !isDestroyed) {
//            ToastUtils.showShort(message)
            closePage()
        }
    }

    /**
     * 安全关闭Activity，避免重复调用finish()
     */
    private fun execAction_2() {
        if (!mField_1 && !isDestroyed) {mField_1 = true
            finish()
        }
    }

    override fun initObserve() {
        // 观察数据变化
    }

    override fun initTag(): String {
        return TAG
    }

    fun showFileLockDialog(fileInfo: BusinessFileInfo, isLock: Boolean) {
        if (mField_1 || isDestroyed) return

        BusinessFileLockDialog.show(this, isLock) {
            if (mField_1 || isDestroyed) return@show

            lifecycleScope.launch {
                try {
                    val success = if (isLock) {
                        BusinessPdfUtils.encryptPdfInPlace(fileInfo.path, it, it)
                    } else {
                        BusinessPdfUtils.decryptPdfInPlace(fileInfo.path, it)
                    }

                    if (success) {
                        withContext(Dispatchers.Main) {
                            if (!mField_1 && !isDestroyed) {
                                // 更新文件的锁定状态
                                fileInfo.isLocked = isLock
                                // 同步更新最近列表和收藏列表中的锁定状态
                                BusinessRecentStorage.updateFileLockStatus(fileInfo.path, isLock)
                                
                                pdfViewer.loadFromFile(fileInfo.path, fileInfo.name, fileInfo.size)

                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (!mField_1 && !isDestroyed) {
                                ToastUtils.showShort(if (isLock) getString(AppR.string.pdf_encrypt_error) else getString(AppR.string.pdf_decrypt_error))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing PDF lock: ${e.message}")
                    withContext(Dispatchers.Main) {
                        if (!mField_1 && !isDestroyed) {
                            ToastUtils.showShort(getString(AppR.string.pdf_process_error))
                        }
                    }
                }
            }
        }
    }

    /**
     * 显示文件加解密对话框（URI版本）
     * 注意：URI文件通常无法直接修改，此方法仅作为占位符
     */
    fun showFileLockDialog(uriDocInfo: BusinessURIDocumentInfo, isLock: Boolean) {
        if (mField_1 || isDestroyed) return

        // URI文件通常无法直接修改，显示提示
        ToastUtils.showShort(getString(AppR.string.external_files_no_crypto))
    }

    private fun execLoad_3() {
        if (mField_1 || isDestroyed) return
    }

    private fun execDisplay_4(nextAction: () -> Unit) {
        if (mField_1 || isDestroyed) {
            nextAction.invoke()
            return
        }
        nextAction.invoke()
    }

    override fun closePage() {
        if (pdfViewer.hasUnsavedChanges()) {
            execDisplay_14 (isBack = true)
        } else {
            execAction_5 ()
        }
    }
    
    /**
     * 执行退出操作
     */
    private fun execAction_5() {
        val documentType = when {
            fileInfo != null -> equalsFileType(fileInfo!!.type).name
            uriDocInfo != null -> "PDF"
            else -> ""
        }

        BusinessPointLog.logEvent(
            "Document_Back", mapOf(
                "Document_Type" to documentType
            )
        )
        execDisplay_4 {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 通知引导回调控制器文档已关闭
        BusinessGuideCallbackController.notifyDocumentClosed()
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
        try {
            if (mField_3?.isAdded == true) {
                mField_3?.dismissAllowingStateLoss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mField_3 = null

        Log.d(TAG, "BusinessDocumentActivity destroyed")
    }

    /**
     * 创建独立的监听器实例
     */
    private fun execSetup_6() {
        // 创建Header监听器
        headerListener = PDFViewerHeaderListener(
            context = this,
            onBackPressed = { onBackPressedDispatcher.onBackPressed() },
            onSearchRequested = { execDisplay_12 () },
            onBookmarkToggle = { isBookmarked ->
                // 书签状态变化处理 - 显示提示消息
                fileInfo?.let {
                    if (isBookmarked) {
                        BusinessRecentStorage.addFavoriteFile(it)
                    } else {
                        BusinessRecentStorage.removeFavoriteFile(it.path)
                    }
                    val message = if (isBookmarked) getString(AppR.string.added_to_favorites) else getString(AppR.string.removed_from_favorites)
                    execDisplay_11 (message)
                }
            },
            onSaveRequested = { fileName, currentPage ->
                // 保存功能处理
                execDisplay_14 ()
            },
            onMenuRequested = {
                // 菜单功能处理
                execDisplay_18 ()
            },
            onTitleTapped = { fileName ->
                // 标题点击处理

            },
            onPageInfoTapped = { currentPage, totalPages ->
                // 页码信息点击处理

            }
        )
    }

    /**
     * 创建非PDFViewer监听器（简化版本）
     */
    private fun execSetup_7() {
        // 简化的Toolbar监听器（不依赖pdfViewer）
        toolbarListener = object : PDFToolbarListener {
            override fun onToolSelected(toolType: ToolType) {

                // 在这里使用this@PDFViewerActivity.pdfViewer
                // 处理工具选择逻辑
            }

            override fun onZoomAction(zoomType: ZoomType) {
                // 在这里使用this@PDFViewerActivity.pdfViewer
                when (zoomType) {
                    ZoomType.ZOOM_IN -> pdfViewer.zoomIn()
                    ZoomType.ZOOM_OUT -> pdfViewer.zoomOut()
                    ZoomType.FIT_WIDTH -> pdfViewer.fitWidth()
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
                    pdfViewer.jumpToPage(targetPage - 1) // 转换为0基索引
                    return true
                }
                return false
            }

            override fun onPreviousPage(currentPage: Int): Boolean {
                return if (currentPage > 0) {
                    pdfViewer.goToPreviousPage()
                    true
                } else {
                    execDisplay_11 (getString(AppR.string.already_first_page))
                    false
                }
            }

            override fun onNextPage(currentPage: Int): Boolean {
                return if (currentPage < pdfViewer.getTotalPages()) {
                    pdfViewer.goToNextPage()
                    true
                } else {
                    execDisplay_11 (getString(AppR.string.already_last_page))
                    false
                }
            }

            override fun onThumbnailToggle() {
                execAction_10 ()
            }

            override fun onDirectionToggle(isHorizontal: Boolean) {
                Log.d(TAG, "Activity - onDirectionToggle called with: $isHorizontal")

                // 设置PDF查看器的方向
                pdfViewer.setViewDirection(isHorizontal)

                // 方向切换时可以显示提示信息
//                execDisplay_11 ("已切换为${if (isHorizontal) "横向" else "竖向"}查看")
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
                
                // 设置文档信息到PDFHeader，启用收藏功能
                fileInfo?.let { info ->
                    pdfHeader.setBookmarkState(BusinessRecentStorage.isFavoriteFile(info.path))
                }

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
     * 使用新的PDF查看器加载文档
     */
    private fun execLoad_8() {

        try {
            // 处理从系统启动器打开PDF的情况
            if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
                // 直接从URI加载，不需要转换为本地文件
                if (!execProcess_9(intent)) {
                    // 如果处理失败，直接返回，避免继续执行
                    return
                }
            } else {
                // 内部启动逻辑 - 支持FileInfo和URIDocumentInfo
                val fileBundle = intent.getBundleExtra("file_bundle")
                val uriBundle = intent.getBundleExtra("uri_bundle")

                when {
                    // 优先处理URI文档
                    uriBundle != null -> {
                        try {
                            @Suppress("DEPRECATION")
                            uriDocInfo = uriBundle.getParcelable(PARAM_URI_DOC_INFO)
                            Log.d(TAG, "从Bundle获取URIDocumentInfo结果: ${uriDocInfo != null}")

                            uriDocInfo?.let {
                                pdfViewer.loadFromUri(it.uri, it.name, it.size)
                            } ?: run {
                                execProcess_1 ("无法加载URI文档")
                                return
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "从Bundle获取URIDocumentInfo失败", e)
                            execProcess_1 ("加载URI文档失败")
                            return
                        }
                    }
                    // 处理传统FileInfo
                    fileBundle != null -> {
                        try {
                            @Suppress("DEPRECATION")
                            fileInfo = fileBundle.getParcelable(PARAM_FILE_INFO)
                            Log.d(TAG, "从Bundle获取FileInfo结果: ${fileInfo != null}")

                            fileInfo?.let {
                                pdfViewer.loadFromFile(it.path, it.name, it.size)
                            } ?: run {
                                execProcess_1 ("无法加载PDF文件")
                                return
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "从Bundle获取FileInfo失败", e)
                            execProcess_1 ("加载PDF文件失败")
                            return
                        }
                    }

                    else -> {
                        Log.e(TAG, "无法获取文件信息")
                        execProcess_1 ("无法加载PDF文件")
                        return
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in loadPDFDocument: ${e.message}")
            execProcess_1 ("初始化PDF查看器失败")
            finish()
        }

    }


    /**
     * 处理从外部应用或系统文件管理器打开PDF的情况（使用URI直接加载）
     * @return 是否处理成功
     */
    private fun execProcess_9(intent: Intent): Boolean {
        val uri = intent.data
        if (uri != null) {
            try {
                // 直接从URI创建URIDocumentInfo，不需要转换为本地文件
                uriDocInfo = BusinessURIDocumentInfo.fromUri(this, uri)

                if (uriDocInfo == null) {
                    execProcess_1 ("无法解析PDF文件")
                    return false
                }

                // 如果是内容URI，需要申请临时权限
                if (uri.scheme == "content") {
                    try {
                        // 获取临时读取权限
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        // 无法获取权限，但可以尝试继续
                        Log.w(TAG, "Failed to take persistable permission: ${e.message}")
                    }
                }

                // 直接从URI加载，不需要转换为本地文件
                pdfViewer.loadFromUri(uriDocInfo!!.uri, uriDocInfo!!.name, uriDocInfo!!.size)

                Log.d(TAG, "Successfully started loading PDF from URI: ${uri}")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error handling external PDF: ${e.message}")
                execProcess_1 ("无法打开PDF文件")
                return false
            }
        } else {
            execProcess_1 ("无效的PDF文件")
            return false
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
mField_2 = isVisible
                    pdfBottomBar.updateThumbnailButtonState(isVisible)
                }
            )

            // 同步缩略图栏的显示状态
mField_2 = pdfThumbnailBar.isThumbnailBarVisible()

            // 更新底部栏按钮状态
            pdfBottomBar.updateThumbnailButtonState(mField_2)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize thumbnail bar", e)
        }
    }

    /**
     * 切换缩略图显示/隐藏
     */
    private fun execAction_10() {
        if (!::pdfThumbnailBar.isInitialized) {
            execDisplay_11 (getString(AppR.string.thumbnail_not_initialized))
            return
        }

        // 在切换前先同步状态
        pdfThumbnailBar.syncThumbnailBarState()

        pdfThumbnailBar.toggleThumbnailBar()

        // 延迟更新状态，确保动画已开始
        Handler(Looper.getMainLooper()).postDelayed({mField_2 = pdfThumbnailBar.isThumbnailBarVisible()
            // 更新按钮状态
            pdfBottomBar.updateThumbnailButtonState(mField_2)
        }, 50)
    }


    /**
     * 显示Toast消息
     */
    private fun execDisplay_11(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ================== 搜索功能实现 ==================

    /**
     * 显示搜索BottomSheet
     */
    private fun execDisplay_12() {
        Log.d(TAG, "Showing search bottom sheet")

        // 如果已经显示，不重复创建
        if (mField_3?.isVisible == true) {
            return
        }
mField_3 = PDFSearchBottomSheet.newInstance().apply {
            // 设置搜索控制器
            searchController = { query, listener ->
                this@BusinessDocumentActivity.searchController.startSearch(query, listener)
            }

            // 设置导航控制器
            navigationController = { result ->
                execAction_13 (result)
            }

            // 设置停止搜索控制器
            stopSearchController = {
                this@BusinessDocumentActivity.searchController.stopSearch()
            }
        }
mField_3?.show(supportFragmentManager, "PDFSearchBottomSheet")
    }

    /**
     * 跳转到搜索结果
     */
    private fun execAction_13(result: PDFSearchResult) {
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
    
    // ================== PDF保存功能实现 ==================
    
    /**
     * 显示PDF保存对话框
     * @param isBack 是否为退出时的保存提示
     */
    private fun execDisplay_14(isBack: Boolean = false) {
        if (mField_1 || isDestroyed) return
        
        val currentFileName = when {
            fileInfo != null -> fileInfo!!.name
            uriDocInfo != null -> uriDocInfo!!.name
            else -> "document.pdf"
        }
        
        BusinessPDFSaveDialog.show(
            context = this,
            currentFileName = currentFileName,
            isBack = isBack,
            onSaveListener = object : BusinessPDFSaveDialog.OnSaveListener {
                override fun onSave(fileName: String, saveAsNew: Boolean) {
                    execProcess_15 (fileName, saveAsNew, isBack)
                }
                
                override fun onCancel() {
                    if (isBack) {
                        // 用户选择不保存并退出
                        execAction_5 ()
                    }
                    // 否则只是取消保存操作
                }
            }
        )
    }
    
    /**
     * 处理PDF保存操作
     * @param fileName 文件名
     * @param saveAsNew 是否保存为新文件
     * @param isBack 是否为退出时的保存
     */
    private fun execProcess_15(fileName: String, saveAsNew: Boolean, isBack: Boolean = false) {
        if (mField_1 || isDestroyed) return
        
        lifecycleScope.launch {
            try {
                execDisplay_11 (getString(AppR.string.saving_pdf))
                
                // 创建文档信息对象
                val documentInfo = execSetup_16 ()
                
                // 使用MuPDFUtils执行保存操作
                val result = if (saveAsNew) {
                    MuPDFUtils.executeSaveOperation(
                        context = this@BusinessDocumentActivity,
                        muPDFCore = pdfViewer.getPDFCore(),
                        documentInfo = documentInfo,
                        targetFileName = fileName,
                        saveAsNew = true
                    )
                } else {
                    MuPDFUtils.quickOverwriteSave(
                        context = this@BusinessDocumentActivity,
                        muPDFCore = pdfViewer.getPDFCore(),
                        documentInfo = documentInfo
                    )
                }
                
                // 处理保存结果
                execProcess_17 (result, fileName, saveAsNew, isBack)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving PDF: ${e.message}")
                execDisplay_11 ("保存失败: ${e.message}")
            }
        }
    }
    
    /**
     * 创建文档信息对象
     * @return PDFDocumentInfo对象
     */
    private fun execSetup_16(): PDFDocumentInfo {
        return when {
            fileInfo != null -> PDFDocumentInfo(
                fileName = fileInfo!!.name,
                filePath = fileInfo!!.path,
                fileUri = null,
                fileSize = fileInfo!!.size,
                isUriDocument = false
            )
            uriDocInfo != null -> PDFDocumentInfo(
                fileName = uriDocInfo!!.name,
                filePath = null,
                fileUri = uriDocInfo!!.uri,
                fileSize = uriDocInfo!!.size,
                isUriDocument = true
            )
            else -> PDFDocumentInfo(
                fileName = "document.pdf",
                filePath = null,
                fileUri = null,
                fileSize = -1L,
                isUriDocument = false
            )
        }
    }
    
    /**
     * 处理保存结果
     * @param result 保存结果
     * @param fileName 文件名
     * @param saveAsNew 是否保存为新文件
     * @param isBack 是否为退出时的保存
     */
    private fun execProcess_17(result: PDFSaveResult, fileName: String, saveAsNew: Boolean, isBack: Boolean = false) {
        if (result.success) {
            // 成功保存
            val message = if (isBack) {
                when {
                    saveAsNew -> getString(AppR.string.save_as_copy_exiting)
                    result.usedIncremental -> getString(AppR.string.incremental_save_exiting)
                    else -> getString(AppR.string.save_success_exiting)
                }
            } else {
                when {
                    result.usedIncremental -> getString(AppR.string.incremental_save_success)
                    result.originalSize > 0 -> "${getString(AppR.string.save_success)} ${result.getSizeChangeDescription(this)}"
                    else -> getString(AppR.string.save_success)
                }
            }
            execDisplay_11 (message)
            
            // 记录保存事件
            BusinessPointLog.logEvent(
                "PDF_Save", mapOf(
                    "Save_Type" to if (saveAsNew) "New_File" else "Overwrite",
                    "File_Name" to fileName,
                    "Used_Incremental" to result.usedIncremental,
                    "Output_Size" to result.fileSize,
                    "Is_Back_Save" to isBack
                )
            )
            
            // 如果是退出时保存，延迟执行退出操作
            if (isBack) {
                Handler(Looper.getMainLooper()).postDelayed({
                    execAction_5 ()
                }, 1000) // 1秒后执行退出操作，让用户看到保存成功提示
            }
        } else {
            // 保存失败
                            execDisplay_11 (getString(AppR.string.save_failed, result.errorMessage))
            Log.e(TAG, "PDF保存失败: ${result.errorMessage}")
        }
    }

    private fun execDisplay_18() {
        fileInfo?.let { fileInfo ->
            BusinessFileMoreDialog(this, fileInfo, model)
                .setOnRenameClickListener {
                    BusinessFileRemakeNameDialog.show(this, fileInfo, 0) { mFileInfo, mPosition ->
                        model.notifyFileSystemChanged()
                        pdfHeader.setFileName(fileInfo.name)
                    }
                }
                .setOnFileLockClickListener {
                    BusinessFileLockDialog.show(this, true) { password ->
                        execAction_19 (fileInfo, 0, true, password)
                    }
                }
                .setOnDetailClickListener {
                    BusinessFileDetailDialog.show(this, fileInfo)
                }
                .setOnShareClickListener {
                    BusinessShareUtils.share(this, fileInfo)
                }
                .setOnFileDeleteClickListener {
                    AlertDialog.Builder(this)
                        .setTitle(getString(AppR.string.choose_file_delete_confirm_title))
                        .setMessage(getString(AppR.string.choose_file_delete_single_confirm_message, fileInfo.name))
                        .setNegativeButton(getString(AppR.string.choose_file_delete_confirm_cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setPositiveButton(getString(AppR.string.choose_file_delete_confirm_ok)) { dialog, _ ->
                            closePage()
                            dialog.dismiss()
                        }
                        .show()
                }
                .show()
        }
    }

    /**
     * 执行文件加锁/解锁操作
     */
    private fun execAction_19(
        fileInfo: BusinessFileInfo,
        position: Int,
        isLock: Boolean,
        password: String
    ) {
        lifecycleScope.launch {
            val success = try {
                if (isLock) {
                    BusinessPointLog.logEvent("LockPDF_Fnish")
                    BusinessPdfUtils.encryptPdfInPlace(fileInfo.path, password, password)
                } else {
                    BusinessPointLog.logEvent("UnlockPDF_Fnish")
                    BusinessPdfUtils.decryptPdfInPlace(fileInfo.path, password)
                }
            } catch (e: Exception) {
                Log.e(BusinessFileListFragment.Companion.TAG, "文件${if (isLock) "加锁" else "解锁"}失败: ${e.message}")
                false
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    // 更新文件状态并刷新列表
                    fileInfo.isLocked = isLock
                    // 同步更新最近列表和收藏列表中的锁定状态
                    BusinessRecentStorage.updateFileLockStatus(fileInfo.path, isLock)

                    finish()
                } else {
                    ToastUtils.showShort(getString(AppR.string.dialog_password_incorrect))
                }
            }
        }
    }

}