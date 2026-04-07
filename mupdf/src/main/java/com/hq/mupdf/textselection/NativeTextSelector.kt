package com.hq.mupdf.textselection

import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.artifex.mupdf.fitz.Point
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.StructuredText
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.viewer.PageView

/**
 * 基于MuPDF原生API的简单文本选择器
 * 替代复杂的TextPositionCalculator实现
 */
class NativeTextSelector {
    
    companion object {
        private const val TAG = "NativeTextSelector"
    }
    
    // 缓存机制
    private var cachedStructuredText: StructuredText? = null
    private var cachedPageNumber: Int = -1
    
    /**
     * 选择结果数据类
     */
    data class SelectionResult(
        val selectedText: String,
        val highlightQuads: Array<Quad>,
        val bounds: RectF
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SelectionResult
            if (selectedText != other.selectedText) return false
            if (!highlightQuads.contentEquals(other.highlightQuads)) return false
            if (bounds != other.bounds) return false
            return true
        }

        override fun hashCode(): Int {
            var result = selectedText.hashCode()
            result = 31 * result + highlightQuads.contentHashCode()
            result = 31 * result + bounds.hashCode()
            return result
        }
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        cachedStructuredText?.destroy()
        cachedStructuredText = null
        cachedPageNumber = -1
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * 获取或创建StructuredText
     */
    private fun getStructuredText(core: MuPDFCore, pageNumber: Int): StructuredText? {
        return try {
            // 检查缓存
            if (cachedPageNumber == pageNumber && cachedStructuredText != null) {
                return cachedStructuredText
            }
            
            // 清理旧缓存
            cachedStructuredText?.destroy()
            
            // 创建新的StructuredText - 使用正确的API
            val structuredText = core.getPageStructuredText(pageNumber)
            
            // 更新缓存
            cachedStructuredText = structuredText
            cachedPageNumber = pageNumber
            
            Log.d(TAG, "Created StructuredText for page $pageNumber")
            structuredText
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create StructuredText for page $pageNumber", e)
            null
        }
    }
    
    /**
     * 将屏幕坐标转换为PDF坐标（Point对象）
     */
    fun screenToPdfPoint(screenX: Float, screenY: Float, pageView: PageView): Point {
        val scale = pageView.sourceScale
        val relativeX = screenX - pageView.left
        val relativeY = screenY - pageView.top
        val pdfX = relativeX / scale
        val pdfY = relativeY / scale
        return Point(pdfX, pdfY)
    }
    
    /**
     * 将PDF坐标转换为屏幕坐标
     */
    private fun pdfToScreen(pdfX: Float, pdfY: Float, pageView: PageView): PointF {
        val scale = pageView.sourceScale
        val screenX = pdfX * scale + pageView.left
        val screenY = pdfY * scale + pageView.top
        return PointF(screenX, screenY)
    }
    
    /**
     * 将Quad转换为屏幕坐标的RectF
     */
    private fun quadToScreenRect(quad: Quad, pageView: PageView): RectF {
        val p1 = pdfToScreen(quad.ul_x, quad.ul_y, pageView)
        val p2 = pdfToScreen(quad.ur_x, quad.ur_y, pageView)
        val p3 = pdfToScreen(quad.ll_x, quad.ll_y, pageView)
        val p4 = pdfToScreen(quad.lr_x, quad.lr_y, pageView)
        
        val left = minOf(p1.x, p2.x, p3.x, p4.x)
        val top = minOf(p1.y, p2.y, p3.y, p4.y)
        val right = maxOf(p1.x, p2.x, p3.x, p4.x)
        val bottom = maxOf(p1.y, p2.y, p3.y, p4.y)
        
        return RectF(left, top, right, bottom)
    }
    
    /**
     * 🎯 字符级别精确拖动选择 - 用于手柄拖动调整
     */
    fun selectTextWithCharMode(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        pageView: PageView,
        core: MuPDFCore
    ): SelectionResult? {
        return try {
            val structuredText = getStructuredText(core, pageView.page) ?: return null
            
            // 转换屏幕坐标为PDF坐标
            val startPoint = screenToPdfPoint(startX, startY, pageView)
            val endPoint = screenToPdfPoint(endX, endY, pageView)
            
            Log.d(TAG, "🎯 拖动选择策略: PDF (${startPoint.x}, ${startPoint.y}) to (${endPoint.x}, ${endPoint.y})")
            
            // 🎯 计算拖动距离，用于选择策略
            val dragDistanceX = Math.abs(endPoint.x - startPoint.x)
            val dragDistanceY = Math.abs(endPoint.y - startPoint.y)
            val totalDragDistance = Math.sqrt((dragDistanceX * dragDistanceX + dragDistanceY * dragDistanceY).toDouble()).toFloat()
            
            Log.d(TAG, "🎯 拖动距离分析: X=$dragDistanceX, Y=$dragDistanceY, 总距离=$totalDragDistance")
            
            // 🚀 优化策略：根据拖动距离选择最合适的方法
            var selectedText: String?
            var highlightQuads: Array<Quad>
            var currentStartPoint = startPoint
            var currentEndPoint = endPoint
            
            // 对于小距离拖动（特别是行尾微调），使用更灵活的策略
            if (totalDragDistance < 20f) {
                Log.d(TAG, "🎯 小距离拖动($totalDragDistance < 20px)，使用微调策略")
                
                // 先尝试扩展选择区域，给MuPDF更大的搜索范围
                val expandedStartX = startPoint.x - 2f
                val expandedStartY = startPoint.y - 1f
                val expandedEndX = endPoint.x + 2f
                val expandedEndY = endPoint.y + 1f
                
                val expandedStart = Point(expandedStartX, expandedStartY)
                val expandedEnd = Point(expandedEndX, expandedEndY)
                
                selectedText = structuredText.copy(expandedStart, expandedEnd)
                if (!selectedText.isNullOrEmpty()) {
                    highlightQuads = structuredText.highlight(expandedStart, expandedEnd)
                    currentStartPoint = expandedStart
                    currentEndPoint = expandedEnd
                    Log.d(TAG, "🎯 扩展区域copy成功: '$selectedText'")
                } else {
                    // 扩展失败，尝试原始范围
                    selectedText = structuredText.copy(startPoint, endPoint)
                    highlightQuads = structuredText.highlight(startPoint, endPoint)
                    Log.d(TAG, "🎯 扩展失败，使用原始范围: '$selectedText'")
                }
            } else {
                Log.d(TAG, "🎯 正常距离拖动，使用标准策略")
                // 正常距离拖动，优先使用直接copy
                selectedText = structuredText.copy(startPoint, endPoint)
                highlightQuads = structuredText.highlight(startPoint, endPoint)
            }
            
            // 🔑 后处理：如果没有获得有效结果，尝试fallback策略
            if (selectedText.isNullOrEmpty()) {
                Log.d(TAG, "🎯 基础策略失败，尝试字符级snap选择")
                val snapQuad = structuredText.snapSelection(startPoint, endPoint, StructuredText.SELECT_CHARS)
                if (snapQuad != null) {
                    currentStartPoint = Point(snapQuad.ul_x, snapQuad.ul_y)
                    currentEndPoint = Point(snapQuad.lr_x, snapQuad.lr_y)
                    selectedText = structuredText.copy(currentStartPoint, currentEndPoint)
                    highlightQuads = structuredText.highlight(currentStartPoint, currentEndPoint)
                    Log.d(TAG, "🎯 字符级snap fallback成功: '$selectedText', ${highlightQuads.size} quads")
                } else {
                    Log.d(TAG, "🎯 所有策略都失败")
                    return null
                }
            } else if (highlightQuads.isEmpty()) {
                Log.d(TAG, "🎯 有文本但无highlight，尝试字符级snap修正")
                val snapQuad = structuredText.snapSelection(currentStartPoint, currentEndPoint, StructuredText.SELECT_CHARS)
                if (snapQuad != null) {
                    currentStartPoint = Point(snapQuad.ul_x, snapQuad.ul_y)
                    currentEndPoint = Point(snapQuad.lr_x, snapQuad.lr_y)
                    selectedText = structuredText.copy(currentStartPoint, currentEndPoint)
                    highlightQuads = structuredText.highlight(currentStartPoint, currentEndPoint)
                    Log.d(TAG, "🎯 字符级snap修正成功: '$selectedText', ${highlightQuads.size} quads")
                } else {
                    Log.d(TAG, "🎯 保持原始结果，无highlight")
                }
            } else {
                Log.d(TAG, "🎯 策略成功: '$selectedText', ${highlightQuads.size} quads")
            }
            
            if (selectedText.isNullOrEmpty()) {
                Log.d(TAG, "🎯 最终selectedText为空")
                return null
            }
            
            // 计算bounds
            val bounds = if (highlightQuads.isNotEmpty()) {
                quadToScreenRect(highlightQuads.first(), pageView)
            } else {
                RectF(startX, startY, endX, endY)
            }

            SelectionResult(selectedText, highlightQuads, bounds)
            
        } catch (e: Exception) {
            Log.e(TAG, "字符级别拖动选择失败", e)
            null
        }
    }
    
    /**
     * 执行文本选择 - MuPDF原生API版本
     */
    fun selectText(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        pageView: PageView,
        core: MuPDFCore
    ): SelectionResult? {
        return try {
            val structuredText = getStructuredText(core, pageView.page) ?: return null
            
            // 转换屏幕坐标为PDF坐标
            val startPoint = screenToPdfPoint(startX, startY, pageView)
            val endPoint = screenToPdfPoint(endX, endY, pageView)
            
            Log.d(TAG, "Selecting text from PDF (${startPoint.x}, ${startPoint.y}) to (${endPoint.x}, ${endPoint.y})")
            
            // 使用MuPDF原生API进行文本选择
            val selectedText = structuredText.copy(startPoint, endPoint)
            
            if (selectedText.isNullOrEmpty()) {
                Log.d(TAG, "No text selected")
                return null
            }
            
            // 获取高亮区域
            val highlightQuads = structuredText.highlight(startPoint, endPoint)
            
            if (highlightQuads.isEmpty()) {
                Log.d(TAG, "No highlight quads found")
                return null
            }
            
            // 计算选择区域的边界
            var left = Float.MAX_VALUE
            var top = Float.MAX_VALUE
            var right = Float.MIN_VALUE
            var bottom = Float.MIN_VALUE
            
            highlightQuads.forEach { quad ->
                val rect = quadToScreenRect(quad, pageView)
                left = minOf(left, rect.left)
                top = minOf(top, rect.top)
                right = maxOf(right, rect.right)
                bottom = maxOf(bottom, rect.bottom)
            }
            
            val bounds = RectF(left, top, right, bottom)
            
            Log.d(TAG, "Selected text: '$selectedText', ${highlightQuads.size} quads, bounds: $bounds")
            
            SelectionResult(selectedText, highlightQuads, bounds)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting text", e)
            null
        }
    }
    
    /**
     * 智能选择 - 支持字符、单词、行级别的选择
     */
    fun smartSelect(
        x: Float, y: Float,
        pageView: PageView,
        core: MuPDFCore,
        mode: Int = StructuredText.SELECT_WORDS
    ): SelectionResult? {
        return try {
            val structuredText = getStructuredText(core, pageView.page) ?: return null
            
            val point = screenToPdfPoint(x, y, pageView)
            
            val modeText = when(mode) {
                StructuredText.SELECT_CHARS -> "字符级别"
                StructuredText.SELECT_WORDS -> "单词级别" 
                else -> "其他级别"
            }
            Log.d(TAG, "🎯 智能选择[$modeText]: PDF (${point.x}, ${point.y})")
            Log.d(TAG, "输入屏幕坐标: ($x, $y), 缩放: ${pageView.sourceScale}")
            
            // 🔑 对字符级别选择使用特殊策略
            var snapQuad: Quad? = null
            
            if (mode == StructuredText.SELECT_CHARS) {
                Log.d(TAG, "🎯 使用字符级别选择策略（优化容错范围：1px→2px→3px）")
                
                // 策略1: 优先使用单点精确选择
                Log.d(TAG, "尝试单点精确字符选择: (${point.x}, ${point.y})")
                snapQuad = structuredText.snapSelection(point, point, mode)
                
                // 策略2: 如果单点失败，尝试小区域容差
                if (snapQuad == null) {
                    val smallTolerance = 1.0f / pageView.sourceScale  // 1像素容差
                    val startPoint = Point(point.x - smallTolerance, point.y - smallTolerance)
                    val endPoint = Point(point.x + smallTolerance, point.y + smallTolerance)
                    
                    Log.d(TAG, "尝试小区域选择: start(${startPoint.x}, ${startPoint.y}) -> end(${endPoint.x}, ${endPoint.y})")
                    snapQuad = structuredText.snapSelection(startPoint, endPoint, mode)
                }
            } else {
                // 其他级别选择使用原来的策略
                snapQuad = structuredText.snapSelection(point, point, mode)
            }
            
            // 🔑 字符级别的最终策略：如果snapSelection完全失败，尝试逐步扩大精确搜索
            if (snapQuad == null && mode == StructuredText.SELECT_CHARS) {
                Log.d(TAG, "字符级别snapSelection失败，尝试逐步精确搜索")
                
                // 策略2: 尝试1像素容差
                val tolerance1 = 1.0f / pageView.sourceScale
                val start1 = Point(point.x - tolerance1, point.y - tolerance1)
                val end1 = Point(point.x + tolerance1, point.y + tolerance1)
                Log.d(TAG, "尝试1像素容差: start(${start1.x}, ${start1.y}) -> end(${end1.x}, ${end1.y})")
                snapQuad = structuredText.snapSelection(start1, end1, StructuredText.SELECT_CHARS)
                
                // 策略3: 尝试2像素容差
                if (snapQuad == null) {
                    val tolerance2 = 2.0f / pageView.sourceScale  
                    val start2 = Point(point.x - tolerance2, point.y - tolerance2)
                    val end2 = Point(point.x + tolerance2, point.y + tolerance2)
                    Log.d(TAG, "尝试2像素容差: start(${start2.x}, ${start2.y}) -> end(${end2.x}, ${end2.y})")
                    snapQuad = structuredText.snapSelection(start2, end2, StructuredText.SELECT_CHARS)
                }
                
                // 策略4: 尝试3像素容差
                if (snapQuad == null) {
                    val tolerance3 = 3.0f / pageView.sourceScale  
                    val start3 = Point(point.x - tolerance3, point.y - tolerance3)
                    val end3 = Point(point.x + tolerance3, point.y + tolerance3)
                    Log.d(TAG, "尝试3像素容差: start(${start3.x}, ${start3.y}) -> end(${end3.x}, ${end3.y})")
                    snapQuad = structuredText.snapSelection(start3, end3, StructuredText.SELECT_CHARS)
                }
            }
            
            if (snapQuad == null) {
                Log.d(TAG, "No snap selection found even with fallback")
                return null
            }
            
            // 使用snap的结果进行文本复制
            var startPoint = Point(snapQuad.ul_x, snapQuad.ul_y)
            var endPoint = Point(snapQuad.lr_x, snapQuad.lr_y)
            
            var selectedText = structuredText.copy(startPoint, endPoint)
            
            // 🔑 字符级别文本复制如果失败，继续尝试容错策略
            if (selectedText.isNullOrEmpty() && mode == StructuredText.SELECT_CHARS) {
                Log.d(TAG, "字符级别文本复制失败，尝试容错策略")
                Log.d(TAG, "snapQuad bounds: ul(${snapQuad.ul_x}, ${snapQuad.ul_y}) lr(${snapQuad.lr_x}, ${snapQuad.lr_y})")
                Log.d(TAG, "startPoint: (${startPoint.x}, ${startPoint.y}), endPoint: (${endPoint.x}, ${endPoint.y})")
                
                // 策略2: 尝试1像素容差
                val tolerance1 = 1.0f / pageView.sourceScale
                val start1 = Point(point.x - tolerance1, point.y - tolerance1)
                val end1 = Point(point.x + tolerance1, point.y + tolerance1)
                Log.d(TAG, "尝试1像素容差重新选择: (${start1.x}, ${start1.y}) -> (${end1.x}, ${end1.y})")
                
                var retrySnapQuad = structuredText.snapSelection(start1, end1, mode)
                if (retrySnapQuad != null) {
                    val retryText = structuredText.copy(Point(retrySnapQuad.ul_x, retrySnapQuad.ul_y), Point(retrySnapQuad.lr_x, retrySnapQuad.lr_y))
                    if (!retryText.isNullOrEmpty()) {
                        Log.d(TAG, "1像素容差成功: '$retryText'")
                        selectedText = retryText
                        snapQuad = retrySnapQuad
                        startPoint = Point(snapQuad.ul_x, snapQuad.ul_y)
                        endPoint = Point(snapQuad.lr_x, snapQuad.lr_y)
                    }
                }
                
                // 策略3: 如果1px还是失败，尝试2像素容差
                if (selectedText.isNullOrEmpty()) {
                    val tolerance2 = 2.0f / pageView.sourceScale
                    val start2 = Point(point.x - tolerance2, point.y - tolerance2)
                    val end2 = Point(point.x + tolerance2, point.y + tolerance2)
                    Log.d(TAG, "尝试2像素容差重新选择: (${start2.x}, ${start2.y}) -> (${end2.x}, ${end2.y})")
                    
                    retrySnapQuad = structuredText.snapSelection(start2, end2, mode)
                    if (retrySnapQuad != null) {
                        val retryText = structuredText.copy(Point(retrySnapQuad.ul_x, retrySnapQuad.ul_y), Point(retrySnapQuad.lr_x, retrySnapQuad.lr_y))
                        if (!retryText.isNullOrEmpty()) {
                            Log.d(TAG, "2像素容差成功: '$retryText'")
                            selectedText = retryText
                            snapQuad = retrySnapQuad
                            startPoint = Point(snapQuad.ul_x, snapQuad.ul_y)
                            endPoint = Point(snapQuad.lr_x, snapQuad.lr_y)
                        }
                    }
                }
                
                // 策略4: 如果2px还是失败，尝试3像素容差
                if (selectedText.isNullOrEmpty()) {
                    val tolerance3 = 3.0f / pageView.sourceScale
                    val start3 = Point(point.x - tolerance3, point.y - tolerance3)
                    val end3 = Point(point.x + tolerance3, point.y + tolerance3)
                    Log.d(TAG, "尝试3像素容差重新选择: (${start3.x}, ${start3.y}) -> (${end3.x}, ${end3.y})")
                    
                    retrySnapQuad = structuredText.snapSelection(start3, end3, mode)
                    if (retrySnapQuad != null) {
                        val retryText = structuredText.copy(Point(retrySnapQuad.ul_x, retrySnapQuad.ul_y), Point(retrySnapQuad.lr_x, retrySnapQuad.lr_y))
                        if (!retryText.isNullOrEmpty()) {
                            Log.d(TAG, "3像素容差成功: '$retryText'")
                            selectedText = retryText
                            snapQuad = retrySnapQuad
                            startPoint = Point(snapQuad.ul_x, snapQuad.ul_y)
                            endPoint = Point(snapQuad.lr_x, snapQuad.lr_y)
                        }
                    }
                }
            }
            
            // 🔑 如果所有字符级别策略都失败，作为最终fallback尝试单词级别
            if (selectedText.isNullOrEmpty() && mode == StructuredText.SELECT_CHARS) {
                Log.d(TAG, "所有字符级别策略都失败，切换到单词级别作为最终fallback")
                
                // 最终策略：单词级别选择
                val wordSnapQuad = structuredText.snapSelection(point, point, StructuredText.SELECT_WORDS)
                if (wordSnapQuad != null) {
                    val wordText = structuredText.copy(Point(wordSnapQuad.ul_x, wordSnapQuad.ul_y), Point(wordSnapQuad.lr_x, wordSnapQuad.lr_y))
                    if (!wordText.isNullOrEmpty()) {
                        Log.d(TAG, "单词级别fallback成功: '$wordText'")
                        selectedText = wordText
                        snapQuad = wordSnapQuad
                        startPoint = Point(snapQuad.ul_x, snapQuad.ul_y)
                        endPoint = Point(snapQuad.lr_x, snapQuad.lr_y)
                    }
                }
            }
            
            if (selectedText.isNullOrEmpty()) {
                Log.d(TAG, "包括单词级别fallback在内的所有策略都失败")
                return null
            }
            
            // 获取精确的高亮区域
            val highlightQuads = structuredText.highlight(startPoint, endPoint)
            
            val bounds = quadToScreenRect(snapQuad, pageView)
            
            // 🔍 详细分析选择结果
            Log.d(TAG, "=== 字符级别选择结果分析 ===")
            Log.d(TAG, "选中文本: '$selectedText'")
            Log.d(TAG, "文本长度: ${selectedText.length} 字符")
            Log.d(TAG, "是否包含换行: ${selectedText.contains('\n')}")
            Log.d(TAG, "高亮区域数量: ${highlightQuads.size}")
            Log.d(TAG, "文本边界: (${startPoint.x}, ${startPoint.y}) -> (${endPoint.x}, ${endPoint.y})")
            Log.d(TAG, "屏幕边界: ${bounds}")
            
            // 检测是否使用了单词级别fallback
            val usedWordFallback = mode == StructuredText.SELECT_CHARS && 
                (selectedText.length > 10 || selectedText.contains(' '))
            
            if (usedWordFallback) {
                Log.d(TAG, "🔄 使用了单词级别fallback (${selectedText.length}字符)，确保了文本选择成功")
                Log.d(TAG, "文本预览: '${selectedText.take(30)}${if (selectedText.length > 30) "..." else ""}'")
            } else if (selectedText.length > 15) {
                Log.w(TAG, "⚠️ 选中了较长文本 (${selectedText.length}字符)，可能超出精确字符选择范围")
                Log.w(TAG, "文本预览: '${selectedText.take(50)}${if (selectedText.length > 50) "..." else ""}'")
                
                // 分析可能的原因
                if (selectedText.contains('\n')) {
                    Log.w(TAG, "包含换行符，选中了多行文本")
                }
                if (highlightQuads.size > 8) {
                    Log.w(TAG, "高亮区域较多 (${highlightQuads.size}个)，可能选中了大文本块")
                }
            } else if (selectedText.length > 5) {
                Log.d(TAG, "📝 选中了中等长度文本 (${selectedText.length}字符)，可接受的字符选择")
            } else {
                Log.d(TAG, "✅ 选中了精确的短文本 (${selectedText.length}字符)，完美的字符级别选择")
            }
            Log.d(TAG, "================================")
            
            Log.d(TAG, "Smart selected: '$selectedText', ${highlightQuads.size} quads")
            
            SelectionResult(selectedText, highlightQuads, bounds)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in smart select", e)
            null
        }
    }
    
    /**
     * 搜索文本并返回高亮区域
     */
    fun searchText(
        query: String,
        pageView: PageView,
        core: MuPDFCore
    ): List<RectF> {
        return try {
            val structuredText = getStructuredText(core, pageView.page) ?: return emptyList()
            
            // 使用MuPDF原生搜索 - 简化版本，不传递搜索选项
            val searchResults = structuredText.search(query)
            
            val screenRects = mutableListOf<RectF>()
            
            searchResults.forEach { quadArray ->
                quadArray.forEach { quad ->
                    val screenRect = quadToScreenRect(quad, pageView)
                    screenRects.add(screenRect)
                }
            }
            
            Log.d(TAG, "Found ${screenRects.size} search results for '$query'")
            screenRects
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching text", e)
            emptyList()
        }
    }
    
    /**
     * 获取页面文本内容 - 使用MuPDF原生API
     */
    fun getPageText(core: MuPDFCore, pageNumber: Int): String {
        return try {
            // 直接使用MuPDFCore的方法，它会自动处理资源释放
            core.getPageText(pageNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page text", e)
            ""
        }
    }
    
    /**
     * 获取页面的结构化文本信息（JSON格式）
     */
    fun getPageStructure(core: MuPDFCore, pageNumber: Int): String {
        return try {
            val structuredText = getStructuredText(core, pageNumber) ?: return ""
            structuredText.asJSON()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page structure", e)
            ""
        }
    }
    
    /**
     * 资源清理
     */
    fun destroy() {
        clearCache()
        Log.d(TAG, "NativeTextSelector destroyed")
    }
}
