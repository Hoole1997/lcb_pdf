package com.hq.mupdf.toolbar

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.hq.mupdf.R

/**
 * 简化版PDF工具栏管理类
 * 只保留缩放和自适应宽度功能
 * 
 * @param context 上下文
 * @param rootView 包含工具栏的根视图
 * @param listener 工具栏事件监听器
 */
class EnhancedPDFToolbar(
    private val context: Context,
    private val rootView: View,
    private var listener: EnhancedPDFToolbarListener?
) {
    
    companion object {
        private const val TAG = "EnhancedPDFToolbar"
    }
    
    /**
     * PDF工具栏事件监听器 - 仅缩放功能
     */
    interface EnhancedPDFToolbarListener {
        // 缩放事件
        fun onZoomIn()
        fun onZoomOut()
        fun onFitWidth()
    }
    
    // 缩放控件
    private val zoomOutBtn: ImageButton by lazy { rootView.findViewById(R.id.zoomOutBtn) }
    private val zoomInBtn: ImageButton by lazy { rootView.findViewById(R.id.zoomInBtn) }
    private val zoomLevel: TextView by lazy { rootView.findViewById(R.id.zoomLevel) }
    private val fitWidthBtn: ImageButton by lazy { rootView.findViewById(R.id.fitWidthBtn) }
    
    // 当前缩放级别
    private var currentZoomLevel: Float = 100f
    
    init {
        setupToolbarListeners()
        updateZoomDisplay()
    }
    
    /**
     * 设置工具栏事件监听器
     */
    fun setListener(listener: EnhancedPDFToolbarListener) {
        this.listener = listener
    }
    
    /**
     * 设置工具栏事件监听
     */
    private fun setupToolbarListeners() {
        // 缩放按钮
        zoomOutBtn.setOnClickListener { 
            currentZoomLevel = (currentZoomLevel - 25f).coerceAtLeast(25f)
            updateZoomDisplay()
            listener?.onZoomOut() 
        }
        zoomInBtn.setOnClickListener { 
            currentZoomLevel += 25f
            updateZoomDisplay()
            listener?.onZoomIn() 
        }
        fitWidthBtn.setOnClickListener { 
            currentZoomLevel = 100f
            updateZoomDisplay()
            listener?.onFitWidth() 
        }
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
    }
    
    /**
     * 隐藏工具栏
     */
    fun hideToolbar() {
        rootView.findViewById<View>(R.id.pdfToolbarContainer)?.visibility = View.GONE
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
     * 释放资源
     */
    fun onDestroy() {
        listener = null
    }
}