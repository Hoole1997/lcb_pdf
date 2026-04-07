package com.hq.mupdf.thumbnail

import android.app.Activity
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.R

/**
 * PDF缩略图栏管理类
 * 封装缩略图栏的UI操作和业务逻辑
 */
class PDFThumbnailBar(
    private val activity: Activity,
    private val rootView: View,
    private val pdfCore: MuPDFCore,
    private val onThumbnailClick: (Int) -> Unit,
    private val onVisibilityChanged: ((Boolean) -> Unit)? = null
) {
    
    companion object {
        private const val TAG = "PDFThumbnailBar"
    }
    
    // 缩略图栏组件
    private val thumbnailContainer: View by lazy { rootView.findViewById(R.id.pdfThumbnails) }
    private val thumbnailRecyclerView: RecyclerView by lazy { rootView.findViewById(R.id.thumbnailRecyclerView) }
    private val closeThumbnailBtn: ImageButton by lazy { rootView.findViewById(R.id.closeThumbnailBtn) }
    private val thumbnailsToggleBtn: View by lazy { rootView.findViewById(R.id.thumbnailsToggleBtn) }
    
    // 缩略图适配器
    private var thumbnailAdapter: PDFThumbnailAdapter? = null
    
    // 状态管理
    private var isThumbnailVisible = false  // 初始状态为隐藏，将在初始化时设置为显示
    private var currentPage = 0
    private var isAnimating = false  // 添加动画状态标记
    
    init {
        initializeThumbnailBar()
    }
    
    /**
     * 初始化缩略图栏
     */
    private fun initializeThumbnailBar() {
        setupRecyclerView()
        setupCloseButton()
        
        // 默认显示缩略图栏
//        showThumbnailBar()
    }
    
    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        // 设置水平布局管理器
        thumbnailRecyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        
        // 创建缩略图适配器
        thumbnailAdapter = PDFThumbnailAdapter(pdfCore) { pageIndex ->
            onThumbnailClick(pageIndex)
        }
        
        thumbnailRecyclerView.adapter = thumbnailAdapter
    }
    
    /**
     * 设置关闭按钮和切换按钮
     */
    private fun setupCloseButton() {
        closeThumbnailBtn.setOnClickListener {
            hideThumbnailBar()
        }
        
        // 设置头部的缩略图切换按钮 - 对应HTML版本的thumbnails-toggle
        thumbnailsToggleBtn.setOnClickListener {
            hideThumbnailBar()
        }
    }
    
    /**
     * 显示缩略图栏
     */
    fun showThumbnailBar() {
        try {
            // 如果正在动画中，取消当前动画
            if (isAnimating) {
                thumbnailContainer.animate().cancel()
                isAnimating = false
            }
            
            // 显示容器
            thumbnailContainer.visibility = View.VISIBLE
            
            if (!isThumbnailVisible) {
                isAnimating = true
                
                // 获取容器高度，如果为0则使用默认值
                val containerHeight = if (thumbnailContainer.height > 0) {
                    thumbnailContainer.height.toFloat()
                } else {
                    140f * activity.resources.displayMetrics.density  // 使用布局中定义的高度
                }
                
                // 设置初始状态（从底部收缩状态）
                thumbnailContainer.alpha = 0f
                thumbnailContainer.translationY = containerHeight * 0.8f  // 向下偏移80%高度，留一点重叠
                thumbnailContainer.scaleY = 0.2f  // 垂直缩放到20%
                thumbnailContainer.scaleX = 1f   // 确保水平缩放正常
                thumbnailContainer.pivotY = containerHeight  // 设置缩放锚点在底部
                
                // 执行展开动画 - 从底部向上展开
                thumbnailContainer.animate()
                    .alpha(1f)
                    .translationY(0f)  // 回到原位置
                    .scaleY(1f)  // 恢复到原始大小
                    .scaleX(1f)  // 确保水平缩放正常
                    .setDuration(300)  // 缩短动画时间
                    .setInterpolator(android.view.animation.OvershootInterpolator(0.6f))
                    .withStartAction {
                        // 动画开始时立即更新状态
                        isThumbnailVisible = true
                        onVisibilityChanged?.invoke(true)
                    }
                    .withEndAction {
                        isAnimating = false
                        // Show animation completed
                    }
                    .start()
            } else {
                // 如果已经标记为可见，直接设置为完全显示状态
                thumbnailContainer.alpha = 1f
                thumbnailContainer.translationY = 0f
                thumbnailContainer.scaleY = 1f
                thumbnailContainer.scaleX = 1f
            }
            
            // 预加载当前页面附近的缩略图
            val startPage = (currentPage - 2).coerceAtLeast(0)
            val endPage = (currentPage + 5).coerceAtMost(pdfCore.countPages() - 1)
            thumbnailAdapter?.preloadThumbnails(startPage, endPage)
            
            // 滚动到当前页面
            scrollToCurrentPage()
        } catch (e: Exception) {
            // Failed to show thumbnail bar
            isAnimating = false
        }
    }
    
    /**
     * 隐藏缩略图栏
     */
    fun hideThumbnailBar() {
        if (isThumbnailVisible) {
            // 如果正在动画中，取消当前动画
            if (isAnimating) {
                thumbnailContainer.animate().cancel()
                isAnimating = false
            }
            
            isAnimating = true
            
            // 获取容器高度，如果为0则使用默认值
            val containerHeight = if (thumbnailContainer.height > 0) {
                thumbnailContainer.height.toFloat()
            } else {
                140f * activity.resources.displayMetrics.density  // 使用布局中定义的高度
            }
            
            // 设置缩放锚点在底部中心
            thumbnailContainer.pivotY = containerHeight
            thumbnailContainer.pivotX = thumbnailContainer.width / 2f
            
            // 计算向下收缩的距离 - 收缩到接近底部控制栏的位置
            val targetTranslationY = containerHeight * 0.85f  // 向下移动85%的高度
            
            // 执行收起动画 - 向底部收缩成小条状
            thumbnailContainer.animate()
                .alpha(0.1f)  // 几乎透明
                .translationY(targetTranslationY)  // 向下移动到接近底部控制栏
                .scaleY(0.05f)  // 垂直缩放到5%，形成一个很细的条状
                .scaleX(0.8f)   // 水平稍微缩小
                .setDuration(250)  // 缩短动画时间
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withStartAction {
                    // 动画开始时立即更新状态
                    isThumbnailVisible = false
                    onVisibilityChanged?.invoke(false)
                }
                .withEndAction {
                    // 动画结束后立即处理
                    thumbnailContainer.visibility = View.GONE
                    // 重置所有变换状态，为下次显示做准备
                    thumbnailContainer.scaleY = 1f
                    thumbnailContainer.scaleX = 1f
                    thumbnailContainer.translationY = 0f
                    thumbnailContainer.alpha = 1f
                    isAnimating = false
                                            // Hide animation completed
                }
                .start()
        }
    }
    
    /**
     * 切换缩略图栏可见性
     */
    fun toggleThumbnailBar() {
        // 如果正在动画中，忽略切换请求
        if (isAnimating) {
            return
        }
        
        // Toggling thumbnail bar, current state: visible=$isThumbnailVisible
        
        if (isThumbnailVisible) {
            hideThumbnailBar()
        } else {
            showThumbnailBar()
        }
    }
    
    /**
     * 设置当前页面
     */
    fun setCurrentPage(page: Int) {
        currentPage = page
        thumbnailAdapter?.setCurrentPage(page)
        
        // 如果缩略图栏可见，滚动到当前页面
        if (isThumbnailVisible) {
            scrollToCurrentPage()
        }
        
        // Current page set to: ${page + 1}
    }
    
    /**
     * 滚动到当前页面
     */
    private fun scrollToCurrentPage() {
        thumbnailRecyclerView.post {
            thumbnailRecyclerView.smoothScrollToPosition(currentPage)
        }
    }
    
    /**
     * 获取缩略图栏可见性
     */
    fun isThumbnailBarVisible(): Boolean = isThumbnailVisible
    
    /**
     * 获取缩略图栏实际可见状态（包括动画状态）
     */
    fun isThumbnailBarActuallyVisible(): Boolean {
        return thumbnailContainer.visibility == View.VISIBLE && thumbnailContainer.alpha > 0.5f
    }
    
    /**
     * 强制同步状态 - 用于修复状态不一致问题
     */
    fun syncThumbnailBarState() {
        val actuallyVisible = isThumbnailBarActuallyVisible()
        if (isThumbnailVisible != actuallyVisible && !isAnimating) {
            // State mismatch detected, syncing
            isThumbnailVisible = actuallyVisible
            onVisibilityChanged?.invoke(isThumbnailVisible)
        }
    }
    
    /**
     * 预加载缩略图
     */
    fun preloadThumbnails() {
        val pageCount = pdfCore.countPages()
        val batchSize = 5
        
        // 分批预加载，避免一次性加载太多
        for (startPage in 0 until pageCount step batchSize) {
            val endPage = (startPage + batchSize - 1).coerceAtMost(pageCount - 1)
            thumbnailAdapter?.preloadThumbnails(startPage, endPage)
        }
        
        // Thumbnail preloading started
    }
    
    /**
     * 刷新缩略图
     */
    fun refreshThumbnails() {
        thumbnailAdapter?.notifyDataSetChanged()
        // Thumbnails refreshed
    }
    

    
    /**
     * 清理资源
     */
    fun onDestroy() {
        thumbnailAdapter?.cleanup()
        // PDFThumbnailBar destroyed
    }
}
