package com.hq.mupdf.textselection

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.artifex.mupdf.fitz.StructuredText
import com.hq.mupdf.R
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.viewer.PageView
import com.hq.mupdf.viewer.ReaderView
import com.hq.mupdf.annotation.PDFAnnotationHelper

/**
 * 简化版文本选择管理器
 * 基于MuPDF原生API，替代复杂的TextSelectionManager
 */
class SimpleTextSelectionManager(
    private val context: Context,
    private val core: MuPDFCore,
    private val readerView: ReaderView,
    private val thumbnailBar: com.hq.mupdf.thumbnail.PDFThumbnailBar? = null
) : TextSelectionOverlay.OnHandleDragListener {
    companion object {
        private const val TAG = "SimpleTextSelection"
        private const val LONG_PRESS_TIMEOUT = 500L
        private const val TOUCH_TOLERANCE = 10f
    }

    // 核心组件
    private val nativeSelector = NativeTextSelector()
    private var selectionOverlay: TextSelectionOverlay? = null
    private var contextMenu: TextSelectionContextMenu? = null
    
    // 状态管理
    private var isSelectionMode = false
    
    /**
     * 是否在选择模式
     */
    fun isInSelectionMode(): Boolean {
        return isSelectionMode
    }
    private var currentPageView: PageView? = null
    private var currentSelection: NativeTextSelector.SelectionResult? = null
    
    // 触摸状态
    private var touchStartTime: Long = 0L
    private var touchStartX: Float = 0f
    private var touchStartY: Float = 0f
    private var isDragging: Boolean = false
    
    // 选择状态 - 保存精确的文本选择端点坐标（PDF坐标系）
    private var selectionStartPdfX: Float = 0f
    private var selectionStartPdfY: Float = 0f
    private var selectionEndPdfX: Float = 0f
    private var selectionEndPdfY: Float = 0f
    
    // 🚀 响应式手柄跟踪：检测选择结果变化，实现智能手柄更新
    private var lastSelectionText: String? = null
    private var lastSelectionStartPdfX: Float = 0f
    private var lastSelectionStartPdfY: Float = 0f
    private var lastSelectionEndPdfX: Float = 0f 
    private var lastSelectionEndPdfY: Float = 0f
    
    // 🎯 手势容错：记录拖动开始位置，用于方向检测
    private var handleDragStartX = 0f
    private var handleDragStartY = 0f
    private var isHandleDragging = false
    
    // 🎯 手势容错参数  
    private val verticalDragThreshold = 45f // 像素，垂直拖动阈值，进一步放宽容错范围
    private val minDragDistance = 8f // 🚀 像素，降低拖动敏感度，避免误触发（从3f增加到8f）
    
    // 监听器 - 兼容TextSelectionManager的接口
    private var onSelectionChangeListener: Any? = null
    private var onSearchRequestListener: Any? = null

    fun setOnSelectionChangeListener(listener: Any) {
        onSelectionChangeListener = listener
    }

    fun setOnSearchRequestListener(listener: Any) {
        onSearchRequestListener = listener
    }
    
    // 内部方法来调用监听器
    private fun notifySelectionStarted() {
        try {
            val method = onSelectionChangeListener?.javaClass?.getMethod("onSelectionStarted")
            method?.invoke(onSelectionChangeListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling onSelectionStarted", e)
        }
    }
    
    private fun notifySelectionChanged(text: String) {
        try {
            val method = onSelectionChangeListener?.javaClass?.getMethod("onSelectionChanged", String::class.java)
            method?.invoke(onSelectionChangeListener, text)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling onSelectionChanged", e)
        }
    }
    
    private fun notifySelectionEnded() {
        try {
            val method = onSelectionChangeListener?.javaClass?.getMethod("onSelectionEnded")
            method?.invoke(onSelectionChangeListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling onSelectionEnded", e)
        }
    }
    
    private fun notifySearchRequested(query: String) {
        try {
            val method = onSearchRequestListener?.javaClass?.getMethod("onSearchRequested", String::class.java)
            method?.invoke(onSearchRequestListener, query)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling onSearchRequested", e)
        }
    }
    
    /**
     * 处理触摸事件
     */
    fun handleTouchEvent(event: MotionEvent, pageView: PageView): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return handleTouchDown(event, pageView)
            }
            MotionEvent.ACTION_MOVE -> {
                return handleTouchMove(event, pageView)
            }
            MotionEvent.ACTION_UP -> {
                return handleTouchUp(event, pageView)
            }
            MotionEvent.ACTION_CANCEL -> {
                return handleTouchCancel()
            }
        }
        return false
    }
    
    private fun handleTouchDown(event: MotionEvent, pageView: PageView): Boolean {
        touchStartTime = System.currentTimeMillis()
        touchStartX = event.x
        touchStartY = event.y
        isDragging = false
        
        // 如果当前在选择模式，检查是否点击了手柄
        if (isSelectionMode) {
            // 这里可以添加手柄检测逻辑
            // 暂时简化处理：点击任何地方都退出选择模式
            // TODO: 改为只有点击选择区域外才退出，让用户可以继续拖拽手柄
            Log.d(TAG, "点击处理 - 保持选择状态，让用户可以继续拖拽")
            return true
        }
        
        return false
    }
    
    private fun handleTouchMove(event: MotionEvent, pageView: PageView): Boolean {
        if (!isSelectionMode) return false
        
        // 如果已经有选择内容，不在这里处理拖拽
        // 拖拽由 TextSelectionOverlay 的手柄专门处理
        if (currentSelection != null) {
            Log.d(TAG, "已有选择内容，拖拽由手柄处理")
            return false
        }
        
        val dx = event.x - touchStartX
        val dy = event.y - touchStartY
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        
        if (distance > TOUCH_TOLERANCE) {
            isDragging = true
            
            // 更新选择范围（转换为PDF坐标）
            val endPdfPoint = nativeSelector.screenToPdfPoint(event.x, event.y, pageView)
            selectionEndPdfX = endPdfPoint.x
            selectionEndPdfY = endPdfPoint.y
            
            updateSelection(pageView)
            return true
        }
        
        return false
    }
    
    private fun handleTouchUp(event: MotionEvent, pageView: PageView): Boolean {
        val touchDuration = System.currentTimeMillis() - touchStartTime
        
        if (!isDragging && touchDuration > LONG_PRESS_TIMEOUT) {
            // 长按启动文本选择
            return startTextSelection(event.x, event.y, pageView)
        } else if (isDragging && isSelectionMode && currentSelection == null) {
            // 只有在没有现有选择内容时才处理拖拽结束
            // 如果已有选择内容，拖拽由手柄处理
            finishSelection(pageView)
            return true
        }
        
        return false
    }
    
    private fun handleTouchCancel(): Boolean {
        if (isSelectionMode) {
            endSelection()
            return true
        }
        return false
    }
    
    /**
     * 开始文本选择（长按触发）
     */
    fun startTextSelection(x: Float, y: Float, pageView: PageView): Boolean {
        try {
            Log.d(TAG, "Starting text selection at ($x, $y)")
            
            // 🔍 智能缩放：只有在需要时才进行缩放操作，避免不必要的界面抖动
            try {
                val currentZoom = readerView.getCurrentZoom()
                val currentView = readerView.getDisplayedView()
                
                if (currentView != null && currentView.getWidth() > 0) {
                    // 计算适合宽度的理想缩放比例
                    val idealZoom = readerView.getWidth().toFloat() / currentView.getWidth().toFloat()
                    val zoomDifference = Math.abs(currentZoom - idealZoom)
                    
                    // 只有当当前缩放与理想缩放差异较大时（超过5%）才进行调整
                    if (zoomDifference > idealZoom * 0.05f) {
                        readerView.fitToWidthAndCenter()
                        Log.d(TAG, "缩放调整：从 $currentZoom 调整到适应宽度 $idealZoom")
                    } else {
                        Log.d(TAG, "当前缩放 $currentZoom 已接近理想缩放 $idealZoom，跳过缩放操作")
                    }
                } else {
                    Log.d(TAG, "无法获取当前页面信息，跳过缩放操作")
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查缩放状态时发生错误", e)
            }
            
            // 关闭现有的上下文菜单
            contextMenu?.dismiss()
            
            currentPageView = pageView
            
            // 🎯 双模式策略：
            // - 初始选择：单词级别（更容易选中文本，避免"No text found"）
            // - 拖动调整：字符级别（精确调整选择边界）
            val selection = nativeSelector.smartSelect(
                x, y, pageView, core, StructuredText.SELECT_WORDS
            )
            
            if (selection == null) {
                Log.d(TAG, "No text found at touch position")
                return false
            }
            
            // 设置选择状态
            isSelectionMode = true
            currentSelection = selection
            
            // 🔑 关键修复：从高亮区域获取精确的文本选择端点（PDF坐标）
            if (selection.highlightQuads.isNotEmpty()) {
                val firstQuad = selection.highlightQuads.first()
                val lastQuad = selection.highlightQuads.last()
                
                // 保存真正的文本选择开始和结束点（PDF坐标）
                selectionStartPdfX = firstQuad.ul_x  // 第一个字符的左上角
                selectionStartPdfY = firstQuad.ul_y
                selectionEndPdfX = lastQuad.lr_x    // 最后一个字符的右下角  
                selectionEndPdfY = lastQuad.lr_y
                
                Log.d(TAG, "初始选择坐标基于精确的highlightQuads (PDF坐标):")
                Log.d(TAG, "  第一个quad: ul(${firstQuad.ul_x}, ${firstQuad.ul_y})")
                Log.d(TAG, "  最后一个quad: lr(${lastQuad.lr_x}, ${lastQuad.lr_y})")
                Log.d(TAG, "  保存的PDF坐标: start($selectionStartPdfX, $selectionStartPdfY), end($selectionEndPdfX, $selectionEndPdfY)")
            } else {
                // 回退到bounds（不理想但确保有值）
                selectionStartPdfX = selection.bounds.left
                selectionStartPdfY = selection.bounds.top
                selectionEndPdfX = selection.bounds.right
                selectionEndPdfY = selection.bounds.bottom
                Log.d(TAG, "回退到bounds坐标 (PDF): start($selectionStartPdfX, $selectionStartPdfY), end($selectionEndPdfX, $selectionEndPdfY)")
            }
            
            // 创建选择覆盖层
            setupSelectionOverlay(pageView)
            
            // 显示选择
            updateSelectionDisplay(selection)
            
            // 触觉反馈
            performHapticFeedback("SELECTION_START")
            
            // 通知监听器
            notifySelectionStarted()
            notifySelectionChanged(selection.selectedText)
            
            Log.d(TAG, "Text selection started: '${selection.selectedText}'")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting text selection", e)
            return false
        }
    }
    
    /**
     * 更新选择范围（拖拽过程中）
     */
    private fun updateSelection(pageView: PageView) {
        try {
            // 将保存的PDF坐标转换为屏幕坐标进行区域选择
            val scale = pageView.sourceScale
            val startScreenX = selectionStartPdfX * scale + pageView.left
            val startScreenY = selectionStartPdfY * scale + pageView.top
            val endScreenX = selectionEndPdfX * scale + pageView.left
            val endScreenY = selectionEndPdfY * scale + pageView.top
            
            val selection = nativeSelector.selectText(
                startScreenX, startScreenY,
                endScreenX, endScreenY,
                pageView, core
            )
            
            if (selection != null) {
                currentSelection = selection
                updateSelectionDisplay(selection)
                notifySelectionChanged(selection.selectedText)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating selection", e)
        }
    }
    
    /**
     * 完成选择（拖拽结束）
     */
    private fun finishSelection(pageView: PageView) {
        try {
            currentSelection?.let { selection ->
                Log.d(TAG, "Selection finished: '${selection.selectedText}'")
                
                // 显示上下文菜单
                showContextMenu(selection)
                
                // 触觉反馈
                performHapticFeedback("SELECTION_END")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finishing selection", e)
        }
    }
    
    /**
     * 设置选择覆盖层
     */
    private fun setupSelectionOverlay(pageView: PageView) {
        Log.d(TAG, "setupSelectionOverlay called")
        if (selectionOverlay == null) {
            Log.d(TAG, "Creating new TextSelectionOverlay")
            selectionOverlay = TextSelectionOverlay(context)
            // 设置手柄拖拽监听器
            selectionOverlay?.setOnHandleDragListener(this)
            // 获取readerView的父容器
            val parent = readerView.parent
            if (parent is ViewGroup) {
                // 设置布局参数，使其覆盖整个父容器
                val layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                selectionOverlay?.layoutParams = layoutParams
                parent.addView(selectionOverlay)
                Log.d(TAG, "TextSelectionOverlay added to parent container")
            } else {
                Log.e(TAG, "ReaderView's parent is not a ViewGroup, cannot add TextSelectionOverlay")
            }
        } else {
            Log.d(TAG, "TextSelectionOverlay already exists")
        }
    }
    
    /**
     * 更新选择显示
     */
    private fun updateSelectionDisplay(selection: NativeTextSelector.SelectionResult) {
        Log.d(TAG, "updateSelectionDisplay called")
        val highlightRects = selection.highlightQuads.map { quad ->
            currentPageView?.let { pageView ->
                // 将Quad转换为RectF (这个逻辑在NativeTextSelector中已经实现)
                val scale = pageView.sourceScale
                val left = minOf(quad.ul_x, quad.ll_x) * scale + pageView.left
                val top = minOf(quad.ul_y, quad.ur_y) * scale + pageView.top
                val right = maxOf(quad.ur_x, quad.lr_x) * scale + pageView.left
                val bottom = maxOf(quad.ll_y, quad.lr_y) * scale + pageView.top
                RectF(left, top, right, bottom)
            } ?: RectF()
        }
        
        Log.d(TAG, "highlightRects: $highlightRects")
        
        // 🔑 关键修复：手柄位置基于高亮区域的首尾，确保与实际高亮区域一致
        val startHandleX: Float
        val startHandleY: Float
        val endHandleX: Float
        val endHandleY: Float
        
        if (highlightRects.isNotEmpty()) {
            // 开始手柄：第一个高亮矩形的左上角
            val firstRect = highlightRects.first()
            startHandleX = firstRect.left
            startHandleY = firstRect.top
            
            // 结束手柄：最后一个高亮矩形的右下角
            val lastRect = highlightRects.last()
            endHandleX = lastRect.right
            endHandleY = lastRect.bottom
            
            Log.d(TAG, "基于高亮区域的手柄位置:")
            Log.d(TAG, "  第一个高亮矩形: $firstRect")
            Log.d(TAG, "  最后一个高亮矩形: $lastRect")
            Log.d(TAG, "  startHandle: ($startHandleX, $startHandleY)")
            Log.d(TAG, "  endHandle: ($endHandleX, $endHandleY)")
        } else {
            // 如果没有高亮矩形，回退到使用bounds
            val currentPageView = currentPageView!!
            val scale = currentPageView.sourceScale
            startHandleX = selection.bounds.left * scale + currentPageView.left
            startHandleY = selection.bounds.top * scale + currentPageView.top
            endHandleX = selection.bounds.right * scale + currentPageView.left
            endHandleY = selection.bounds.bottom * scale + currentPageView.top
            
            Log.d(TAG, "回退到基于MuPDF bounds的手柄位置:")
            Log.d(TAG, "  startHandle: ($startHandleX, $startHandleY)")
            Log.d(TAG, "  endHandle: ($endHandleX, $endHandleY)")
        }
        
        selectionOverlay?.updateSelection(
            highlightRects,
            startHandleX, startHandleY,
            endHandleX, endHandleY
        )
    }
    
    /**
     * 🚀 响应式手柄更新：直接使用拖动坐标更新手柄位置（用于选择结果不变时）
     */
    private fun updateSelectionDisplayWithDragCoords(
        dragX: Float,
        dragY: Float,
        isStartHandle: Boolean
    ) {
        val currentSel = currentSelection ?: return
        
        Log.d(TAG, "🚀 响应式手柄更新: dragX=$dragX, dragY=$dragY, isStartHandle=$isStartHandle")
        
        // 构造当前选择的高亮区域
        val highlightRects = currentSel.highlightQuads.map { quad ->
            currentPageView?.let { pageView ->
                val scale = pageView.sourceScale
                val left = minOf(quad.ul_x, quad.ll_x) * scale + pageView.left
                val top = minOf(quad.ul_y, quad.ur_y) * scale + pageView.top
                val right = maxOf(quad.ur_x, quad.lr_x) * scale + pageView.left
                val bottom = maxOf(quad.ll_y, quad.lr_y) * scale + pageView.top
                RectF(left, top, right, bottom)
            }
        }.filterNotNull()
        
        if (highlightRects.isNotEmpty()) {
            // 计算响应式手柄位置
            val startHandleX: Float
            val startHandleY: Float
            val endHandleX: Float
            val endHandleY: Float
            
            if (isStartHandle) {
                // 拖动开始手柄：使用拖动坐标作为开始位置，保持原结束位置
                startHandleX = dragX
                startHandleY = dragY
                val lastRect = highlightRects.last()
                endHandleX = lastRect.right
                endHandleY = lastRect.bottom
            } else {
                // 拖动结束手柄：保持原开始位置，使用拖动坐标作为结束位置
                val firstRect = highlightRects.first()
                startHandleX = firstRect.left
                startHandleY = firstRect.top
                endHandleX = dragX
                endHandleY = dragY
            }
            
            Log.d(TAG, "🚀 响应式手柄坐标: start($startHandleX, $startHandleY), end($endHandleX, $endHandleY)")
            
            // 更新覆盖层显示
            selectionOverlay?.updateSelection(
                highlightRects,
                startHandleX, startHandleY,
                endHandleX, endHandleY,
                preserveHandlePositions = true
            )
        }
    }
    
    /**
     * 在拖动时更新选择显示，使用MuPDF返回的精确bounds
     */
    private fun updateSelectionDisplayWithDragCoords(
        selection: NativeTextSelector.SelectionResult,
        dragX: Float,
        dragY: Float,
        isStartHandle: Boolean
    ) {
        Log.d(TAG, "updateSelectionDisplayWithDragCoords called")
        Log.d(TAG, "MuPDF selection bounds: ${selection.bounds}")
        Log.d(TAG, "原始拖动坐标: dragX=$dragX, dragY=$dragY, isStartHandle=$isStartHandle")
        
        val highlightRects = selection.highlightQuads.map { quad ->
            currentPageView?.let { pageView ->
                val scale = pageView.sourceScale
                val left = minOf(quad.ul_x, quad.ll_x) * scale + pageView.left
                val top = minOf(quad.ul_y, quad.ur_y) * scale + pageView.top
                val right = maxOf(quad.ur_x, quad.lr_x) * scale + pageView.left
                val bottom = maxOf(quad.ll_y, quad.lr_y) * scale + pageView.top
                RectF(left, top, right, bottom)
            } ?: RectF()
        }
        
        // 🔑 关键修复：手柄位置基于高亮区域的首尾，确保与实际高亮区域一致
        val currentPageView = currentPageView!!
        
        // 根据高亮矩形计算手柄位置
        val startHandleX: Float
        val startHandleY: Float
        val endHandleX: Float
        val endHandleY: Float
        
        if (highlightRects.isNotEmpty()) {
            // 开始手柄：第一个高亮矩形的左上角
            val firstRect = highlightRects.first()
            startHandleX = firstRect.left
            startHandleY = firstRect.top
            
            // 结束手柄：最后一个高亮矩形的右下角
            val lastRect = highlightRects.last()
            endHandleX = lastRect.right
            endHandleY = lastRect.bottom
            
            Log.d(TAG, "基于高亮区域的手柄位置:")
            Log.d(TAG, "  第一个高亮矩形: $firstRect")
            Log.d(TAG, "  最后一个高亮矩形: $lastRect")
            Log.d(TAG, "  startHandle: ($startHandleX, $startHandleY)")
            Log.d(TAG, "  endHandle: ($endHandleX, $endHandleY)")
        } else {
            // 如果没有高亮矩形，回退到使用bounds
            val scale = currentPageView.sourceScale
            startHandleX = selection.bounds.left * scale + currentPageView.left
            startHandleY = selection.bounds.top * scale + currentPageView.top
            endHandleX = selection.bounds.right * scale + currentPageView.left
            endHandleY = selection.bounds.bottom * scale + currentPageView.top
            
            Log.d(TAG, "回退到基于MuPDF bounds的手柄位置:")
            Log.d(TAG, "  startHandle: ($startHandleX, $startHandleY)")
            Log.d(TAG, "  endHandle: ($endHandleX, $endHandleY)")
        }
        
        selectionOverlay?.updateSelection(
            highlightRects,
            startHandleX, startHandleY,
            endHandleX, endHandleY,
            preserveHandlePositions = true
        )
    }
    
    /**
     * 处理手柄拖拽
     */
    private fun handleHandleDrag(startHandle: PointF, endHandle: PointF, pageView: PageView) {
        try {
            val selection = nativeSelector.selectText(
                startHandle.x, startHandle.y,
                endHandle.x, endHandle.y,
                pageView, core
            )
            
            if (selection != null) {
                currentSelection = selection
                updateSelectionDisplay(selection)
                notifySelectionChanged(selection.selectedText)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling handle drag", e)
        }
    }
    
    /**
     * 显示上下文菜单
     */
    private fun showContextMenu(selection: NativeTextSelector.SelectionResult) {
        contextMenu?.dismiss()
        
        val menuX = selection.bounds.centerX()
        val menuY = selection.bounds.top - 50f // 菜单显示在选择区域上方
        
        // 获取当前页面信息
        val currentPageNumber = currentPageView?.page ?: 0
        Log.d(TAG, "🔍 获取PDF页面 - 页码: $currentPageNumber")
        val pdfPage = PDFAnnotationHelper.getPDFPage(core, currentPageNumber)
        Log.d(TAG, "🔍 PDF页面获取结果: ${if (pdfPage != null) "成功" else "失败"}")
        
        contextMenu = TextSelectionContextMenu(
            context = context, 
            pdfPage = pdfPage,
            core = core,
            pageNumber = currentPageNumber,
            onDismiss = { 
                // 不自动结束选择，让用户可以继续拖拽手柄
                Log.d(TAG, "上下文菜单消失，但保持选择状态以便继续拖拽 ${getCurrentSelection()?.selectedText}")
            },
            onAnnotationCreated = {
                // 注释创建后的UI层刷新和清除选择状态
                // 注意：核心的页面对象刷新已在PDFAnnotationHelper中处理
                try {
                    Log.d(TAG, "🎨 开始UI层面的页面刷新...")
                    
                    // 延迟执行UI刷新，确保底层page对象已重新加载
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            // 1. 强制PageView重新渲染（这会使用重新加载的page对象）
                            currentPageView?.update()
                            
                            // 2. 触发整个ReaderView的重绘
                            readerView.invalidate()
                            
                            Log.d(TAG, "✅ UI层刷新完成")
                            
                            // 3. 延迟清除文本选择状态，提供更好的用户体验
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    Log.d(TAG, "🧹 注释创建完成，清除文本选择状态...")
                                    
                                    // 分步骤清除，确保每个步骤都能执行
                                    isSelectionMode = false
                                    currentSelection = null
                                    
                                    // 重置保存的精确PDF坐标
                                    selectionStartPdfX = 0f
                                    selectionStartPdfY = 0f
                                    selectionEndPdfX = 0f
                                    selectionEndPdfY = 0f
                                    
                                    // 重置手势拖动状态
                                    isHandleDragging = false
                                    handleDragStartX = 0f
                                    handleDragStartY = 0f
                                    
                                    // 清除overlay的选择状态
                                    selectionOverlay?.let { overlay ->
                                        Log.d(TAG, "清除overlay选择状态...")
                                        overlay.clearSelection()
                                        Log.d(TAG, "overlay.clearSelection() 已调用")
                                    }
                                    
                                    // 关闭上下文菜单
                                    contextMenu?.dismiss()
                                    
                                    // 通知选择结束
                                    notifySelectionEnded()
                                    
                                    Log.d(TAG, "✅ 文本选择状态已完全清除")
                                } catch (e: Exception) {
                                    Log.w(TAG, "清除选择状态失败: ${e.message}")
                                }
                            }, 150) // 再延迟150ms确保用户能看到注释创建的效果
                            
                        } catch (e: Exception) {
                            Log.w(TAG, "UI刷新失败: ${e.message}")
                            // 降级处理
                            try {
                                currentPageView?.invalidate()
                                readerView.invalidate()
                                Log.d(TAG, "✅ 降级UI刷新完成")
                                
                                // 即使刷新失败，也要清除选择状态
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    try {
                                        Log.d(TAG, "🧹 降级处理：清除文本选择状态...")
                                        isSelectionMode = false
                                        currentSelection = null
                                        selectionOverlay?.clearSelection()
                                        contextMenu?.dismiss()
                                        notifySelectionEnded()
                                        Log.d(TAG, "✅ 降级处理：文本选择状态已清除")
                                    } catch (e: Exception) {
                                        Log.w(TAG, "降级清除选择状态失败: ${e.message}")
                                    }
                                }, 100)
                                
                            } catch (e2: Exception) {
                                Log.e(TAG, "降级UI刷新也失败: ${e2.message}")
                            }
                        }
                    }, 100) // 100ms延迟确保底层刷新完成
                    
                } catch (e: Exception) {
                    Log.e(TAG, "UI刷新处理失败: ${e.message}")
                }
            }
        )
        
        // 显示上下文菜单
        contextMenu?.show(readerView, selection, menuX, menuY)
    }
    

    
    /**
     * 处理上下文菜单动作 - 简化实现
     */
    private fun handleContextMenuAction(action: String, selection: NativeTextSelector.SelectionResult) {
        when (action) {
            "COPY" -> {
                copyToClipboard(selection.selectedText)
                // 复制后保持选择状态，让用户可以继续拖拽调整
                Log.d(TAG, "复制完成，保持选择状态以便继续调整")
            }
            "SEARCH" -> {
                notifySearchRequested(selection.selectedText)
                endSelection() // 搜索后结束选择
            }
            "SHARE" -> {
                shareText(selection.selectedText)
                endSelection() // 分享后结束选择
            }
        }
    }
    
    /**
     * 复制文本到剪贴板
     */
    private fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Selected Text", text)
            clipboard.setPrimaryClip(clip)
            
            Log.d(TAG, "Text copied to clipboard: '$text'")
            
            // 可以显示Toast提示
            android.widget.Toast.makeText(context, context.getString(R.string.text_copied), android.widget.Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to clipboard", e)
        }
    }
    
    /**
     * 分享文本
     */
    private fun shareText(text: String) {
        try {
            val intent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, text)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "分享文本"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing text", e)
        }
    }
    
    /**
     * 结束文本选择
     */
    fun endSelection() {
        try {
            isSelectionMode = false
            currentSelection = null
            
            // 重置保存的精确PDF坐标
            selectionStartPdfX = 0f
            selectionStartPdfY = 0f
            selectionEndPdfX = 0f
            selectionEndPdfY = 0f
            
            // 🎯 重置手势拖动状态
            isHandleDragging = false
            handleDragStartX = 0f
            handleDragStartY = 0f
            
            selectionOverlay?.clearSelection()
            contextMenu?.dismiss()
            
            notifySelectionEnded()
            
            Log.d(TAG, "Text selection ended")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error ending selection", e)
        }
    }
    
    /**
     * 触觉反馈
     */
    private fun performHapticFeedback(type: String) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                when (type) {
                    "SELECTION_START" -> {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    "WORD_SELECTION" -> {
                        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    "SELECTION_END" -> {
                        vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing haptic feedback", e)
        }
    }
    
    /**
     * 搜索文本
     */
    fun searchText(query: String, pageView: PageView): List<RectF> {
        return nativeSelector.searchText(query, pageView, core)
    }
    
    /**
     * 获取页面文本
     */
    fun getPageText(pageNumber: Int): String {
        return nativeSelector.getPageText(core, pageNumber)
    }
    
    /**
     * 清理资源
     */
    fun destroy() {
        endSelection()
        // 由于TextSelectionOverlay现在添加到了ReaderView的父容器中，
        // 我们需要从父容器中移除它
        selectionOverlay?.let { 
            val parent = readerView.parent as? ViewGroup
            parent?.removeView(it)
        }
        selectionOverlay = null
        contextMenu?.dismiss()
        contextMenu = null
        nativeSelector.destroy()
        
        Log.d(TAG, "SimpleTextSelectionManager destroyed")
    }
    
    /**
     * 页面改变时的清理
     */
    fun onPageChanged() {
        endSelection()
        nativeSelector.clearCache()
    }
    
    // ====== OnHandleDragListener 接口实现 ======
    
    override fun onStartHandleDrag(x: Float, y: Float) {

        // 🎯 首次拖动时记录开始位置
        if (!isHandleDragging) {
            handleDragStartX = x
            handleDragStartY = y
            isHandleDragging = true
        }
        
        // 应用手势容错的拖拽更新
        updateSelectionByDragWithGesture(x, y, true)
    }
    
    override fun onEndHandleDrag(x: Float, y: Float) {

        // 🎯 首次拖动时记录开始位置
        if (!isHandleDragging) {
            handleDragStartX = x
            handleDragStartY = y
            isHandleDragging = true
        }
        
        // 应用手势容错的拖拽更新
        updateSelectionByDragWithGesture(x, y, false)
    }
    
    override fun onHandleDragEnd() {

        // 🎯 重置手势拖动状态
        isHandleDragging = false
        handleDragStartX = 0f
        handleDragStartY = 0f
        
        // 拖拽结束后可以显示上下文菜单
        currentSelection?.let { selection ->
            showContextMenu(selection)
        }
    }
    
    override fun onSelectionMenuRequested(x: Float, y: Float) {
        currentSelection?.let { selection ->
            showContextMenu(selection)
        }
    }
    
    override fun onClearSelection() {

        // 🎯 智能清除逻辑：如果上下文菜单正在显示，延迟一点清除，给用户操作时间
        if (contextMenu?.isShowing() == true) {
            // 延迟100ms清除，给用户点击菜单的时间
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (contextMenu?.isShowing() != true) {
                    Log.d(TAG, "延迟清除：菜单已关闭，执行清除")
                    endSelection()
                } else {
                    Log.d(TAG, "延迟清除：菜单仍在显示，取消清除")
                }
            }, 100)
        } else {
            Log.d(TAG, "立即清除高亮选择")
            endSelection()
        }
    }
    
    /**
     * 🎯 带手势容错的拖拽更新选择
     */
    private fun updateSelectionByDragWithGesture(x: Float, y: Float, isStartHandle: Boolean) {
        // 计算拖动距离
        val deltaX = x - handleDragStartX
        val deltaY = y - handleDragStartY
        
        // 计算拖动的主要方向
        val horizontalDistance = kotlin.math.abs(deltaX)
        val verticalDistance = kotlin.math.abs(deltaY)
        val totalDistance = Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
        
        // 🚀 优化：如果移动距离太小，直接跳过，避免无效更新
        if (totalDistance < minDragDistance) {
            Log.d(TAG, "⚡ 移动距离太小($totalDistance < $minDragDistance)，跳过更新")
            return
        }
        
        Log.d(TAG, "🎯 手势分析: 水平=$horizontalDistance, 垂直=$verticalDistance, 总距离=$totalDistance")
        
        val adjustedX: Float
        val adjustedY: Float
        
        // 🔑 优化逻辑：优先识别横向拖动，使用渐进式容错
        val horizontalRatio = if (verticalDistance > 0) horizontalDistance / verticalDistance else Float.MAX_VALUE
        
        // 🎯 严格横向检测：优先支持跨行选择，减少误判
        val dynamicThreshold = when {
            totalDistance < 30f -> verticalDragThreshold * 1.2f  // 初始30px内：54px容错
            totalDistance < 60f -> verticalDragThreshold * 1.0f  // 60px内：45px容错  
            else -> verticalDragThreshold * 0.8f  // 超过60px：36px容错
        }
        
        // 🚀 严格横向拖动判断：优先支持跨行选择
        val isHorizontalDrag = (horizontalRatio >= 3.0f &&  // 水平距离必须是垂直距离的3倍以上
                               verticalDistance < dynamicThreshold &&  // 且垂直移动很小
                               horizontalDistance > 25f) ||  // 且有明显的水平移动
                              (verticalDistance < 8f && horizontalDistance > 40f)  // 或者垂直移动极小但水平移动很大
        
        if (isHorizontalDrag) {
            // 横向拖动：限制Y坐标变化，保持同行选择
            adjustedX = x
            adjustedY = handleDragStartY  // 保持初始Y坐标
            Log.d(TAG, "🔒 严格检测：横向拖动（同行选择）")
            Log.d(TAG, "   水平/垂直比例=${String.format("%.1f", horizontalRatio)}, 总距离=${String.format("%.1f", totalDistance)}")
            Log.d(TAG, "   动态阈值=${String.format("%.1f", dynamicThreshold)}, 垂直距离=$verticalDistance")
            Log.d(TAG, "   水平距离=$horizontalDistance, 限制Y坐标: 原始($x, $y) -> 调整($adjustedX, $adjustedY)")
        } else {
            // 明显的垂直拖动：允许跨行选择
            adjustedX = x
            adjustedY = y
            Log.d(TAG, "📄 优先支持：跨行拖动（换行选择）")
            Log.d(TAG, "   水平/垂直比例=${String.format("%.1f", horizontalRatio)}, 总距离=${String.format("%.1f", totalDistance)}")
            Log.d(TAG, "   动态阈值=${String.format("%.1f", dynamicThreshold)}, 垂直距离=$verticalDistance")
            Log.d(TAG, "   水平距离=$horizontalDistance, 保持原始坐标: ($x, $y)")
        }
        
        // 使用调整后的坐标进行选择更新
        updateSelectionByDrag(adjustedX, adjustedY, isStartHandle)
    }

    /**
     * 通过拖拽更新选择
     */
    private fun updateSelectionByDrag(x: Float, y: Float, isStartHandle: Boolean) {
        try {
            // 🔑 关键修复：使用保存的currentPageView，不要重新查找
            val pageView = currentPageView ?: return
            if (pageView.page == null) return
            
            Log.d(TAG, "使用已保存的currentPageView进行拖动更新，pageView hash: ${pageView.hashCode()}")
            Log.d(TAG, "页面位置: left=${pageView.left}, top=${pageView.top}, scale=${pageView.sourceScale}")
            
            // 获取当前选择信息
            val currentSel = currentSelection ?: return
            
            // 🔑 关键修复：将屏幕坐标转换为PDF坐标，使用精确的固定端坐标
            val dragPdfPoint = nativeSelector.screenToPdfPoint(x, y, pageView)
            
            val newSelection = if (isStartHandle) {
                // 拖拽开始手柄，使用精确的结束点PDF坐标作为固定端
                val fixedEndScreenX = selectionEndPdfX * pageView.sourceScale + pageView.left
                val fixedEndScreenY = selectionEndPdfY * pageView.sourceScale + pageView.top
                
                Log.d(TAG, "拖拽开始手柄: 到PDF(${ dragPdfPoint.x}, ${ dragPdfPoint.y})")
                Log.d(TAG, "  固定结束点PDF坐标: ($selectionEndPdfX, $selectionEndPdfY)")
                Log.d(TAG, "  固定结束点屏幕坐标: ($fixedEndScreenX, $fixedEndScreenY)")
                
                // 🎯 拖动调整使用字符级别精确选择
                nativeSelector.selectTextWithCharMode(
                    x, y,  // 拖动到的新位置（屏幕坐标）
                    fixedEndScreenX, fixedEndScreenY,  // 精确的固定结束位置（屏幕坐标）
                    pageView, 
                    core
                )
            } else {
                // 拖拽结束手柄，使用精确的开始点PDF坐标作为固定端
                val fixedStartScreenX = selectionStartPdfX * pageView.sourceScale + pageView.left
                val fixedStartScreenY = selectionStartPdfY * pageView.sourceScale + pageView.top
                
                Log.d(TAG, "拖拽结束手柄: 到PDF(${ dragPdfPoint.x}, ${ dragPdfPoint.y})")
                Log.d(TAG, "  固定开始点PDF坐标: ($selectionStartPdfX, $selectionStartPdfY)")
                Log.d(TAG, "  固定开始点屏幕坐标: ($fixedStartScreenX, $fixedStartScreenY)")
                
                // 🎯 拖动调整使用字符级别精确选择
                nativeSelector.selectTextWithCharMode(
                    fixedStartScreenX, fixedStartScreenY,  // 精确的固定开始位置（屏幕坐标）
                    x, y,  // 拖动到的新位置（屏幕坐标）
                    pageView,
                    core
                )
            }
            
            // 🚀 响应式手柄更新：检测选择是否有变化
            val hasSelectionChanged = newSelection != null && (
                newSelection.selectedText != lastSelectionText ||
                Math.abs(selectionStartPdfX - lastSelectionStartPdfX) > 1f ||
                Math.abs(selectionStartPdfY - lastSelectionStartPdfY) > 1f ||
                Math.abs(selectionEndPdfX - lastSelectionEndPdfX) > 1f ||
                Math.abs(selectionEndPdfY - lastSelectionEndPdfY) > 1f
            )
            
            if (newSelection != null) {
                currentSelection = newSelection
                
                // 🔑 关键修复：从新的highlightQuads更新精确的PDF坐标
                if (newSelection.highlightQuads.isNotEmpty()) {
                    val newFirstQuad = newSelection.highlightQuads.first()
                    val newLastQuad = newSelection.highlightQuads.last()
                    
                    // 更新精确的PDF坐标
                    selectionStartPdfX = newFirstQuad.ul_x
                    selectionStartPdfY = newFirstQuad.ul_y
                    selectionEndPdfX = newLastQuad.lr_x
                    selectionEndPdfY = newLastQuad.lr_y
                    
                    Log.d(TAG, "拖拽后更新精确的PDF坐标:")
                    Log.d(TAG, "  新的开始点PDF: ($selectionStartPdfX, $selectionStartPdfY)")
                    Log.d(TAG, "  新的结束点PDF: ($selectionEndPdfX, $selectionEndPdfY)")
                    Log.d(TAG, "  是否拖拽开始手柄: $isStartHandle")
                    
                    if (hasSelectionChanged) {
                        Log.d(TAG, "🎯 检测到选择变化，正常更新手柄位置")
                        // 保存当前状态用于下次比较
                        lastSelectionText = newSelection.selectedText
                        lastSelectionStartPdfX = selectionStartPdfX
                        lastSelectionStartPdfY = selectionStartPdfY
                        lastSelectionEndPdfX = selectionEndPdfX
                        lastSelectionEndPdfY = selectionEndPdfY
                        
                        updateSelectionDisplayWithDragCoords(x, y, isStartHandle)
                    } else {
                        Log.d(TAG, "🚀 选择结果相同，但手柄需跟随拖动 - 使用响应式更新")
                        
                        // 即使选择结果相同，也要让拖动的手柄跟随用户操作
                        val dragPdfPoint = nativeSelector.screenToPdfPoint(x, y, pageView)
                        
                        if (isStartHandle) {
                            // 更新开始手柄位置到用户拖动位置
                            val adjustedStartScreenX = dragPdfPoint.x * pageView.sourceScale + pageView.left
                            val adjustedStartScreenY = dragPdfPoint.y * pageView.sourceScale + pageView.top
                            
                            Log.d(TAG, "🚀 响应式更新开始手柄: PDF(${dragPdfPoint.x}, ${dragPdfPoint.y}) -> Screen($adjustedStartScreenX, $adjustedStartScreenY)")
                            
                            updateSelectionDisplayWithDragCoords(adjustedStartScreenX, adjustedStartScreenY, true)
                        } else {
                            // 更新结束手柄位置到用户拖动位置
                            val adjustedEndScreenX = dragPdfPoint.x * pageView.sourceScale + pageView.left
                            val adjustedEndScreenY = dragPdfPoint.y * pageView.sourceScale + pageView.top
                            
                            Log.d(TAG, "🚀 响应式更新结束手柄: PDF(${dragPdfPoint.x}, ${dragPdfPoint.y}) -> Screen($adjustedEndScreenX, $adjustedEndScreenY)")
                            
                            updateSelectionDisplayWithDragCoords(adjustedEndScreenX, adjustedEndScreenY, false)
                        }
                    }
                } else {
                    // 回退到bounds
                    selectionStartPdfX = newSelection.bounds.left
                    selectionStartPdfY = newSelection.bounds.top
                    selectionEndPdfX = newSelection.bounds.right
                    selectionEndPdfY = newSelection.bounds.bottom
                    Log.d(TAG, "拖拽后回退到bounds PDF坐标")
                }
                
                updateSelectionDisplayWithDragCoords(newSelection, x, y, isStartHandle)
                Log.d(TAG, "拖拽更新选择成功: ${newSelection.selectedText}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "拖拽更新选择时出错", e)
        }
    }
    
    /**
     * 找到当前的PageView
     */
    private fun findCurrentPageView(): PageView? {
        // 简化实现：返回第一个PageView
        for (i in 0 until readerView.childCount) {
            val child = readerView.getChildAt(i)
            if (child is PageView) {
                return child
            }
        }
        return null
    }

    fun getCurrentSelection(): NativeTextSelector.SelectionResult? {
        return currentSelection
    }

}
