package com.hq.mupdf.toolbar

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.hq.mupdf.R

/**
 * PDF工具栏管理类
 * 封装工具栏的UI控件和交互逻辑
 * 
 * @param context 上下文
 * @param rootView 包含工具栏的根视图
 * @param listener 工具栏事件监听器
 */
class PDFToolbar(
    private val context: Context,
    private val rootView: View,
    private var listener: PDFToolbarListener?
) {
    
    companion object {
        private const val TAG = "PDFToolbar"
    }
    
    // 缩放控件
    private val zoomOutBtn: ImageButton by lazy { rootView.findViewById(R.id.zoomOutBtn) }
    private val zoomInBtn: ImageButton by lazy { rootView.findViewById(R.id.zoomInBtn) }
    private val zoomLevel: TextView by lazy { rootView.findViewById(R.id.zoomLevel) }
    private val fitWidthBtn: ImageButton by lazy { rootView.findViewById(R.id.fitWidthBtn) }
    
    // 当前选中的工具
    private var currentTool: ToolType = ToolType.SELECT
    
    // 当前缩放级别
    private var currentZoomLevel: Float = 100f
    
    init {
        setupToolbarListeners()
        updateZoomDisplay()
    }
    
    /**
     * 设置工具栏事件监听器
     */
    fun setListener(listener: PDFToolbarListener) {
        this.listener = listener
    }
    
    /**
     * 设置工具栏事件监听
     */
    private fun setupToolbarListeners() {
        // 缩放按钮
        zoomOutBtn.setOnClickListener { performZoom(ZoomType.ZOOM_OUT) }
        zoomInBtn.setOnClickListener { performZoom(ZoomType.ZOOM_IN) }
        fitWidthBtn.setOnClickListener { performZoom(ZoomType.FIT_WIDTH) }
    }
    
    /**
     * 执行缩放操作
     * @param zoomType 缩放类型
     */
    private fun performZoom(zoomType: ZoomType) {
                    // Zoom action: $zoomType
        
        // 通知监听器执行实际的缩放操作
        listener?.onZoomAction(zoomType)
        
        // 注意：不再在这里更新currentZoomLevel，
        // 因为实际的缩放级别将通过setZoomLevel方法从外部同步
    }
    
    /**
     * 设置缩放级别
     * @param zoomLevel 缩放级别 (25-500)
     */
    fun setZoomLevel(zoomLevel: Float) {
        currentZoomLevel = zoomLevel.coerceIn(25f, 500f)
        updateZoomDisplay()
    }
    
    /**
     * 获取当前缩放级别
     */
    fun getCurrentZoomLevel(): Float = currentZoomLevel
    
    /**
     * 获取当前选中的工具
     */
    fun getCurrentTool(): ToolType = currentTool
    
    /**
     * 更新缩放显示
     */
    private fun updateZoomDisplay() {
        zoomLevel.text = "${currentZoomLevel.toInt()}%"
    }
    
    /**
     * 显示工具栏
     */
    fun showToolbar() {
        rootView.findViewById<View>(R.id.pdfToolbarContainer)?.visibility = View.VISIBLE
        listener?.onToolbarVisibilityChanged(true)
    }
    
    /**
     * 隐藏工具栏
     */
    fun hideToolbar() {
        rootView.findViewById<View>(R.id.pdfToolbarContainer)?.visibility = View.GONE
        listener?.onToolbarVisibilityChanged(false)
    }
    
    /**
     * 切换工具栏可见性
     */
    fun toggleToolbar() {
        val toolbarContainer = rootView.findViewById<View>(R.id.pdfToolbarContainer)
        if (toolbarContainer?.visibility == View.VISIBLE) {
            hideToolbar()
        } else {
            showToolbar()
        }
    }
    
    /**
     * 启用/禁用工具栏
     * @param enabled 是否启用
     */
    fun setToolbarEnabled(enabled: Boolean) {
        zoomOutBtn.isEnabled = enabled
        zoomInBtn.isEnabled = enabled
        fitWidthBtn.isEnabled = enabled
    }
    
    /**
     * 重置工具栏状态
     */
    fun resetToolbar() {
        setZoomLevel(100f)
        setToolbarEnabled(true)
        showToolbar()
    }
    
    /**
     * 显示Toast消息
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 释放资源
     */
    fun onDestroy() {
        listener = null
    }
}
