package com.hq.mupdf.textselection

import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdf.fitz.Quad
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.viewer.PageView
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 文本位置计算器
 * 处理屏幕坐标与PDF坐标的转换，以及文本字符位置的计算
 * 优化版本：减少搜索调用，提高性能
 */
class TextPositionCalculator {
    
    // 缓存机制
    private var cachedPageText: String? = null
    private var cachedPageNumber: Int = -1
    private var cachedTextBlocks: List<TextBlock>? = null
    
    // 性能配置
    private val enableDetailedLogging = false // 生产环境设为false
    private val maxSearchCalls = 5 // 最大搜索调用次数
    private val earlyExitDistance = 15f // 早期退出距离阈值
    
    /**
     * 清理缓存（在页面改变时调用）
     */
    fun clearCache() {
        cachedPageText = null
        cachedPageNumber = -1
        cachedTextBlocks = null
        if (enableDetailedLogging) {
            Log.d("TextPositionCalculator", "Cache cleared")
        }
    }

    data class TextPosition(
        val charIndex: Int,
        val x: Float,
        val y: Float,
        val pageNumber: Int
    )

    data class TextBlock(
        val text: String,
        val bounds: RectF,
        val startIndex: Int,
        val endIndex: Int
    )

    /**
     * 将屏幕触摸坐标转换为PDF页面坐标
     * 改进的坐标转换，考虑页面偏移和旋转
     */
    fun screenToPdfCoordinates(
        screenX: Float,
        screenY: Float,
        pageView: PageView
    ): PointF {
        try {
            // 获取页面的缩放比例 - 更准确的计算
            val scale = pageView.sourceScale
            
            // 计算相对于PageView的坐标
            val relativeX = screenX - pageView.left
            val relativeY = screenY - pageView.top
            
            // 转换为PDF坐标
            val pdfX = relativeX / scale
            val pdfY = relativeY / scale
            
            if (enableDetailedLogging) {
                Log.d("TextPositionCalculator", 
                    "Screen ($screenX, $screenY) -> PDF ($pdfX, $pdfY), scale: $scale")
            }
            
            return PointF(pdfX, pdfY)
        } catch (e: Exception) {
            Log.e("TextPositionCalculator", "Error converting screen to PDF coordinates", e)
            return PointF(screenX, screenY)
        }
    }

    /**
     * 将PDF坐标转换为屏幕坐标
     */
    fun pdfToScreenCoordinates(
        pdfX: Float,
        pdfY: Float,
        pageView: PageView
    ): PointF {
        // 使用与screenToPdfCoordinates相同的缩放比例计算
        val scale = pageView.sourceScale
        
        val screenX = pdfX * scale + pageView.left
        val screenY = pdfY * scale + pageView.top
        
        return PointF(screenX, screenY)
    }

    /**
     * 在页面文本中查找最接近指定坐标的字符位置
     * 改进版本：使用精确的字符级搜索
     */
    fun findNearestCharacter(
        pdfX: Float,
        pdfY: Float,
        pageText: String,
        core: MuPDFCore,
        pageNumber: Int
    ): TextPosition {
        try {
            Log.d("TextPositionCalculator", "Finding character at PDF coordinates: ($pdfX, $pdfY)")
            
            if (pageText.isEmpty()) {
                return TextPosition(0, pdfX, pdfY, pageNumber)
            }

            // 首先尝试使用MuPDF的结构化文本功能获取精确位置
            try {
                val structuredText = core.getPageStructuredText(pageNumber)
                if (structuredText != null) {
                    return findNearestCharacterFromStructuredText(pdfX, pdfY, structuredText, pageNumber, core)
                }
            } catch (e: Exception) {
                Log.w("TextPositionCalculator", "Failed to get structured text, falling back to search method")
            }

            // 后备方案：使用搜索方法
            return findNearestCharacterBySearch(pdfX, pdfY, pageText, core, pageNumber)
            
        } catch (e: Exception) {
            Log.e("TextPositionCalculator", "Error finding character position", e)
            return estimateCharacterPosition(pdfX, pdfY, pageText, pageNumber)
        }
    }
    
    /**
     * 使用结构化文本查找最接近的字符（更精确）
     */
    private fun findNearestCharacterFromStructuredText(
        pdfX: Float,
        pdfY: Float,
        structuredText: StructuredText,
        pageNumber: Int,
        core: MuPDFCore
    ): TextPosition {
        try {
            // 将结构化文本转换为普通文本，然后使用更精确的搜索
            val textContent = structuredText.asText()
            Log.d("TextPositionCalculator", "Using structured text for more precise character finding")
            
            // 使用改进的搜索方法，但应该比普通方法更精确
            return findNearestCharacterBySearch(pdfX, pdfY, textContent, core, pageNumber)
            
        } catch (e: Exception) {
            Log.e("TextPositionCalculator", "Error processing structured text", e)
            // 如果结构化文本处理失败，使用估算方法
            return estimateCharacterPosition(pdfX, pdfY, "", pageNumber)
        }
    }
    
    /**
     * 使用搜索方法查找字符位置（后备方案）
     */
    private fun findNearestCharacterBySearch(
        pdfX: Float,
        pdfY: Float,
        pageText: String,
        core: MuPDFCore,
        pageNumber: Int
    ): TextPosition {
        // 检查缓存
        val textBlocks = getOrBuildTextBlocks(pageText, core, pageNumber)
        
        // 改进的候选区域筛选 - 更宽松的条件以处理大间距文本
        val candidateBlocks = textBlocks.filter { block ->
            val centerY = block.bounds.centerY()
            val centerX = block.bounds.centerX()
            
            // 使用更宽松的Y坐标范围，更大的X坐标范围来处理长空格
            val yRange = 80f // 增加Y坐标容忍度
            val xRange = 400f // 大幅增加X坐标容忍度以跨越空白区域
            
            abs(centerY - pdfY) < yRange || abs(centerX - pdfX) < xRange
        }.sortedBy { block ->
            // 按距离排序，但给Y坐标更高的权重（同行文本优先）
            val centerY = block.bounds.centerY()
            val centerX = block.bounds.centerX()
            val yDistance = abs(centerY - pdfY)
            val xDistance = abs(centerX - pdfX)
            
            // Y距离权重为3倍，优先选择同行的文本
            yDistance * 3f + xDistance
        }.take(maxSearchCalls) // 只检查最近的几个文本块
        
        if (candidateBlocks.isEmpty()) {
            Log.w("TextPositionCalculator", "No candidate blocks found, using estimation")
            return estimateCharacterPosition(pdfX, pdfY, pageText, pageNumber)
        }
        
        var bestDistance = Float.MAX_VALUE
        var bestIndex = 0
        var bestActualX = pdfX
        var bestActualY = pdfY
        
        // 只对候选块进行精确搜索
        for (block in candidateBlocks) {
            try {
                val searchResults = core.searchPage(pageNumber, block.text.trim())
                if (searchResults.isNotEmpty() && searchResults[0].isNotEmpty()) {
                    val quad = searchResults[0][0]
                    
                    // 计算Quad的中心点
                    val quadCenterX = (quad.ul_x + quad.ur_x + quad.ll_x + quad.lr_x) / 4f
                    val quadCenterY = (quad.ul_y + quad.ur_y + quad.ll_y + quad.lr_y) / 4f
                    
                    // 计算距离
                    val distance = sqrt(
                        (quadCenterX - pdfX) * (quadCenterX - pdfX) +
                        (quadCenterY - pdfY) * (quadCenterY - pdfY)
                    )
                    
                    if (distance < bestDistance) {
                        bestDistance = distance
                        // 在块内估算字符位置
                        val relativeX = pdfX - quad.ul_x
                        val blockWidth = quad.ur_x - quad.ul_x
                        val charRatio = if (blockWidth > 0) relativeX / blockWidth else 0f
                        val estimatedCharOffset = (block.text.length * charRatio.coerceIn(0f, 1f)).toInt()
                        
                        bestIndex = block.startIndex + estimatedCharOffset.coerceIn(0, block.text.length - 1)
                        bestActualX = quadCenterX
                        bestActualY = quadCenterY
                    }
                    
                    // 如果找到非常近的匹配，提前退出
                    if (distance < earlyExitDistance) break
                }
            } catch (e: Exception) {
                Log.w("TextPositionCalculator", "Search failed for block: ${block.text.take(20)}")
            }
        }
        
        // 如果没有找到精确匹配，使用估算方法作为后备
        if (bestDistance == Float.MAX_VALUE) {
            Log.w("TextPositionCalculator", "No precise match found, using estimation")
            return estimateCharacterPosition(pdfX, pdfY, pageText, pageNumber)
        }
        
        Log.d("TextPositionCalculator", 
            "Best match found at index $bestIndex, distance: $bestDistance")
        
        return TextPosition(
            charIndex = bestIndex.coerceIn(0, pageText.length - 1),
            x = bestActualX,
            y = bestActualY,
            pageNumber = pageNumber
        )
    }
    
    /**
     * 获取或构建文本块缓存
     * 将页面文本分割成较大的块，减少搜索次数
     */
    private fun getOrBuildTextBlocks(pageText: String, core: MuPDFCore, pageNumber: Int): List<TextBlock> {
        // 检查缓存
        if (cachedPageNumber == pageNumber && cachedPageText == pageText && cachedTextBlocks != null) {
            return cachedTextBlocks!!
        }
        
        val blocks = mutableListOf<TextBlock>()
        val lines = pageText.split('\n')
        var currentIndex = 0
        
        // 动态调整参数基于内容密度
        val contentDensity = lines.count { it.trim().isNotEmpty() }
        val lineHeight = when {
            contentDensity <= 5 -> 24f  // 稀疏内容
            contentDensity <= 20 -> 18f // 中等密度
            else -> 15f                 // 密集内容
        }
        
        val startY = when {
            contentDensity <= 5 -> 100f  // 稀疏内容可能从中上部开始
            contentDensity <= 20 -> 75f  // 中等内容
            else -> 50f                  // 密集内容从顶部开始
        }
        
        var currentY = startY
        
        // 智能块构建：考虑格式和空格
        var blockStart = 0
        var blockText = StringBuilder()
        var blockStartY = currentY
        var emptyLineCount = 0
        
        if (enableDetailedLogging) {
            Log.d("TextPositionCalculator", "Building blocks for ${lines.size} lines, density: $contentDensity")
        }
        
        for ((lineIndex, line) in lines.withIndex()) {
            if (line.trim().isEmpty()) {
                emptyLineCount++
                currentIndex += line.length + 1
                currentY += lineHeight
                
                // 如果连续空行太多，或者达到了段落分隔，结束当前块
                if (emptyLineCount >= 2 && blockText.isNotEmpty()) {
                    blocks.add(createTextBlock(blockText.toString(), blockStartY, currentY, blockStart, currentIndex - 1))
                    blockText.clear()
                    blockStart = currentIndex
                    blockStartY = currentY
                }
                continue
            }
            
            emptyLineCount = 0
            
            // 检测是否是新的段落或格式变化
            val isNewParagraph = detectNewParagraph(line, lineIndex, lines)
            val isFormatChange = detectFormatChange(line, blockText.toString())
            
            // 如果检测到新段落或格式变化，且当前块不为空，结束当前块
            if ((isNewParagraph || isFormatChange) && blockText.isNotEmpty()) {
                blocks.add(createTextBlock(blockText.toString(), blockStartY, currentY, blockStart, currentIndex - 1))
                blockText.clear()
                blockStart = currentIndex
                blockStartY = currentY
            }
            
            // 添加行到当前块
            if (blockText.isNotEmpty()) {
                blockText.append(" ")
            }
            blockText.append(line.trim())
            currentIndex += line.length + 1
            currentY += lineHeight
            
            // 动态块大小限制：稀疏内容可以有更大的块
            val maxBlockSize = when {
                contentDensity <= 5 -> 200   // 稀疏内容，更大的块
                contentDensity <= 20 -> 150  // 中等内容
                else -> 100                  // 密集内容，较小的块
            }
            
            if (blockText.length > maxBlockSize) {
                blocks.add(createTextBlock(blockText.toString(), blockStartY, currentY, blockStart, currentIndex - 1))
                blockText.clear()
                blockStart = currentIndex
                blockStartY = currentY
            }
        }
        
        // 添加最后一个块
        if (blockText.isNotEmpty()) {
            blocks.add(createTextBlock(blockText.toString(), blockStartY, currentY + lineHeight, blockStart, currentIndex - 1))
        }
        
        // 更新缓存
        cachedPageText = pageText
        cachedPageNumber = pageNumber
        cachedTextBlocks = blocks
        
        if (enableDetailedLogging) {
            Log.d("TextPositionCalculator", "Built ${blocks.size} text blocks for page $pageNumber (density: $contentDensity)")
        }
        return blocks
    }
    
    /**
     * 创建文本块
     */
    private fun createTextBlock(text: String, startY: Float, endY: Float, startIndex: Int, endIndex: Int): TextBlock {
        val estimatedWidth = text.length * estimateCharacterWidth(text)
        return TextBlock(
            text = text,
            bounds = RectF(0f, startY, estimatedWidth, endY),
            startIndex = startIndex,
            endIndex = endIndex
        )
    }
    
    /**
     * 检测是否是新段落
     */
    private fun detectNewParagraph(line: String, lineIndex: Int, lines: List<String>): Boolean {
        if (lineIndex == 0) return true
        
        // 检查上一行
        val prevLineIndex = lineIndex - 1
        if (prevLineIndex >= 0 && prevLineIndex < lines.size) {
            val prevLine = lines[prevLineIndex].trim()
            
            // 如果上一行以句号、问号、感叹号结尾，可能是新段落
            if (prevLine.endsWith(".") || prevLine.endsWith("?") || prevLine.endsWith("!")) {
                return true
            }
            
            // 如果当前行以特殊字符开头（如数字、大写字母等），可能是新段落
            val currentTrimmed = line.trim()
            if (currentTrimmed.matches(Regex("^[0-9]+[.)].+")) || // 数字列表
                currentTrimmed.matches(Regex("^[A-Z][.)].+")) ||   // 字母列表  
                currentTrimmed.matches(Regex("^[IVX]+[.)].+"))     // 罗马数字
            ) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * 检测格式变化
     */
    private fun detectFormatChange(newLine: String, currentBlockText: String): Boolean {
        if (currentBlockText.isEmpty()) return false
        
        // 分析当前块和新行的特征
        val currentHasNumbers = currentBlockText.any { it.isDigit() }
        val newHasNumbers = newLine.any { it.isDigit() }
        
        val currentHasUpperCase = currentBlockText.count { it.isUpperCase() }
        val newHasUpperCase = newLine.count { it.isUpperCase() }
        
        val currentSpaceRatio = currentBlockText.count { it == ' ' }.toFloat() / currentBlockText.length
        val newSpaceRatio = newLine.count { it == ' ' }.toFloat() / newLine.length
        
        // 如果数字密度、大写字母密度或空格密度有显著变化，认为是格式变化
        val numberChange = currentHasNumbers != newHasNumbers
        val caseChange = kotlin.math.abs(currentHasUpperCase - newHasUpperCase) > newLine.length * 0.3f
        val spaceChange = kotlin.math.abs(currentSpaceRatio - newSpaceRatio) > 0.2f
        
        return numberChange || caseChange || spaceChange
    }

    /**
     * 估算字符位置的后备方法 - 改进版本以处理长空格
     */
    private fun estimateCharacterPosition(
        pdfX: Float,
        pdfY: Float,
        pageText: String,
        pageNumber: Int
    ): TextPosition {
        if (pageText.isEmpty()) {
            return TextPosition(0, pdfX, pdfY, pageNumber)
        }
        
        val lines = pageText.split('\n')
        var currentIndex = 0
        var bestDistance = Float.MAX_VALUE
        var bestIndex = 0
        var bestX = pdfX
        var bestY = pdfY
        
        // 动态估算行高，适应不同的PDF内容密度
        val estimatedLineHeight = when {
            lines.size <= 5 -> 24f  // 稀疏内容，可能行高更大
            lines.size <= 20 -> 18f // 中等密度
            else -> 12f             // 密集内容
        }
        
        // 动态估算起始Y位置，考虑页面不满的情况
        val startY = when {
            lines.size <= 5 -> 100f  // 稀疏内容可能从页面中上部开始
            lines.size <= 20 -> 75f  // 中等内容从稍上方开始
            else -> 50f              // 密集内容从页面顶部开始
        }
        
        var currentY = startY
        
        // 扩大搜索范围，特别是对于稀疏内容
        val lineSearchRange = estimatedLineHeight * 5f // 增加搜索范围
        
        Log.d("TextPositionCalculator", "Estimating position for ${lines.size} lines, lineHeight: $estimatedLineHeight, startY: $startY")
        
        for ((lineIndex, line) in lines.withIndex()) {
            if (line.trim().isEmpty()) {
                currentIndex += line.length + 1
                currentY += estimatedLineHeight
                continue
            }
            
            val lineDistance = abs(pdfY - currentY)
            
            // 更宽松的匹配条件，特别是对于稀疏内容
            val shouldConsiderLine = when {
                lines.size <= 5 -> lineDistance < lineSearchRange * 2f // 稀疏内容，更宽松
                lines.size <= 20 -> lineDistance < lineSearchRange * 1.5f // 中等内容
                else -> lineDistance < lineSearchRange // 密集内容
            }
            
            if (shouldConsiderLine) {
                // 智能字符宽度估算，考虑格式化文本和空格
                val estimatedCharWidth = estimateCharacterWidth(line)
                var estimatedCharIndex = (pdfX / estimatedCharWidth).toInt()
                
                // 处理长空格的情况：如果X坐标超出了预期的行长度很多，
                // 可能存在长空格，需要智能调整
                val expectedLineWidth = line.length * estimatedCharWidth
                if (pdfX > expectedLineWidth * 1.5f) {
                    // 用户点击在长空格区域，选择行末尾
                    estimatedCharIndex = line.length - 1
                    Log.d("TextPositionCalculator", "Detected long spacing, selecting line end")
                } else if (pdfX > expectedLineWidth) {
                    // 轻微超出，可能是小空格，选择行末尾
                    estimatedCharIndex = line.length - 1
                } else {
                    // 正常范围内，按比例计算
                    estimatedCharIndex = (estimatedCharIndex).coerceIn(0, line.length - 1)
                }
                
                val charIndex = currentIndex + estimatedCharIndex
                
                // 计算实际字符X位置
                val actualCharX = when {
                    estimatedCharIndex >= line.length - 1 -> expectedLineWidth
                    else -> estimatedCharIndex * estimatedCharWidth
                }
                
                // 动态权重：稀疏内容更偏向Y轴匹配，密集内容更偏向精确位置
                val yWeight = when {
                    lines.size <= 5 -> 0.3f   // 稀疏内容，降低Y权重
                    lines.size <= 20 -> 0.5f  // 中等内容
                    else -> 0.7f              // 密集内容，提高Y权重
                }
                
                val distance = lineDistance * yWeight + abs(pdfX - actualCharX) * (1f - yWeight)
                
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestIndex = charIndex
                    bestX = actualCharX
                    bestY = currentY
                    
                    Log.d("TextPositionCalculator", 
                        "Better match at line $lineIndex, char $estimatedCharIndex, distance: $distance")
                }
            }
            
            currentIndex += line.length + 1
            currentY += estimatedLineHeight
        }
        
        // 如果没有找到合适的位置，使用更智能的后备方案
        if (bestDistance == Float.MAX_VALUE) {
            Log.w("TextPositionCalculator", "No suitable line found, using intelligent fallback")
            
            // 找到最接近Y坐标的行
            currentY = startY
            currentIndex = 0
            var closestLineIndex = 0
            var closestYDistance = Float.MAX_VALUE
            
            for ((lineIndex, line) in lines.withIndex()) {
                val yDistance = abs(pdfY - currentY)
                if (yDistance < closestYDistance && line.trim().isNotEmpty()) {
                    closestYDistance = yDistance
                    closestLineIndex = currentIndex + line.length / 2 // 选择行的中间
                    bestX = line.length * estimateCharacterWidth(line) / 2f
                    bestY = currentY
                }
                currentIndex += line.length + 1
                currentY += estimatedLineHeight
            }
            
            bestIndex = closestLineIndex
        }
        
        return TextPosition(
            charIndex = bestIndex.coerceIn(0, pageText.length - 1),
            x = bestX,
            y = bestY,
            pageNumber = pageNumber
        )
    }
    
    /**
     * 智能估算字符宽度，考虑不同的字体和格式
     */
    private fun estimateCharacterWidth(line: String): Float {
        if (line.isEmpty()) return 6f
        
        // 分析行的内容来估算字符宽度
        val hasNumbers = line.any { it.isDigit() }
        val hasUpperCase = line.any { it.isUpperCase() }
        val spaceCount = line.count { it == ' ' }
        val totalChars = line.length
        
        // 基础字符宽度
        var baseWidth = when {
            hasNumbers && hasUpperCase -> 7f  // 可能是标题或代码，字符较宽
            hasUpperCase -> 6.5f              // 包含大写字母
            else -> 6f                        // 普通文本
        }
        
        // 如果空格很多，可能存在格式化，调整宽度估算
        if (spaceCount > totalChars * 0.3f) {
            baseWidth *= 1.2f // 格式化文本，字符间可能更宽
        }
        
        return baseWidth
    }

    /**
     * 根据字符索引计算文本选择区域
     * 优化版本：避免搜索过长文本，使用分段搜索
     */
    fun calculateSelectionRects(
        startIndex: Int,
        endIndex: Int,
        pageText: String,
        pageView: PageView,
        core: MuPDFCore
    ): List<RectF> {
        if (startIndex >= endIndex || pageText.isEmpty()) {
            return emptyList()
        }

        val rects = mutableListOf<RectF>()
        
        try {
            // 获取选择的文本
            val selectedText = pageText.substring(
                startIndex.coerceIn(0, pageText.length),
                endIndex.coerceIn(0, pageText.length)
            )
            
            if (selectedText.trim().isEmpty()) {
                return emptyList()
            }
            
            if (enableDetailedLogging) {
                Log.d("TextPositionCalculator", "Calculating selection rects for text length: ${selectedText.length}")
            }
            
            // 如果选择的文本太长，分段搜索
            if (selectedText.length > 50) {
                Log.d("TextPositionCalculator", "Long selection detected, using segmented search")
                return calculateSelectionRectsSegmented(startIndex, endIndex, pageText, pageView, core)
            }
            
            // 对于短文本，使用直接搜索
            val searchResults = core.searchPage(pageView.page, selectedText.trim())
            
            if (searchResults.isNotEmpty()) {
                // 只取第一个匹配结果
                val quadArray = searchResults[0]
                for (quad in quadArray) {
                    // 将PDF坐标转换为屏幕坐标
                    val topLeft = pdfToScreenCoordinates(quad.ul_x, quad.ul_y, pageView)
                    val topRight = pdfToScreenCoordinates(quad.ur_x, quad.ur_y, pageView)
                    val bottomLeft = pdfToScreenCoordinates(quad.ll_x, quad.ll_y, pageView)
                    val bottomRight = pdfToScreenCoordinates(quad.lr_x, quad.lr_y, pageView)
                    
                    // 创建矩形（使用四个点的边界）
                    val left = minOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x)
                    val top = minOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y)
                    val right = maxOf(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x)
                    val bottom = maxOf(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y)
                    
                    val rect = RectF(left, top, right, bottom)
                    rects.add(rect)
                    
                    if (enableDetailedLogging) {
                        Log.d("TextPositionCalculator", "Added selection rect: $rect")
                    }
                }
            } else {
                Log.w("TextPositionCalculator", "No search results found, using fallback method")
                // 如果搜索失败，使用估算方法
                return calculateSelectionRectsEstimated(startIndex, endIndex, pageText, pageView)
            }
            
        } catch (e: Exception) {
            Log.e("TextPositionCalculator", "Error calculating selection rects", e)
            return calculateSelectionRectsEstimated(startIndex, endIndex, pageText, pageView)
        }
        
        return rects
    }
    
    /**
     * 分段计算长文本的选择区域
     */
    private fun calculateSelectionRectsSegmented(
        startIndex: Int,
        endIndex: Int,
        pageText: String,
        pageView: PageView,
        core: MuPDFCore
    ): List<RectF> {
        val rects = mutableListOf<RectF>()
        
        try {
            // 获取文本块缓存
            val textBlocks = getOrBuildTextBlocks(pageText, core, pageView.page)
            
            // 找到包含选择范围的文本块
            val relevantBlocks = textBlocks.filter { block ->
                block.startIndex < endIndex && block.endIndex > startIndex
            }
            
            for (block in relevantBlocks) {
                val blockStart = maxOf(startIndex, block.startIndex)
                val blockEnd = minOf(endIndex, block.endIndex)
                
                if (blockStart < blockEnd) {
                    // 计算在块内的相对位置
                    val relativeStart = blockStart - block.startIndex
                    val relativeEnd = blockEnd - block.startIndex
                    val blockSelectedText = block.text.substring(
                        relativeStart.coerceIn(0, block.text.length),
                        relativeEnd.coerceIn(0, block.text.length)
                    )
                    
                    if (blockSelectedText.trim().isNotEmpty()) {
                        try {
                            val searchResults = core.searchPage(pageView.page, block.text.trim())
                            if (searchResults.isNotEmpty() && searchResults[0].isNotEmpty()) {
                                val quad = searchResults[0][0]
                                
                                // 估算选择区域在块内的位置
                                val blockWidth = quad.ur_x - quad.ul_x
                                val blockHeight = quad.ll_y - quad.ul_y
                                val startRatio = relativeStart.toFloat() / block.text.length
                                val endRatio = relativeEnd.toFloat() / block.text.length
                                
                                val selectionLeft = quad.ul_x + blockWidth * startRatio
                                val selectionRight = quad.ul_x + blockWidth * endRatio
                                
                                val screenTopLeft = pdfToScreenCoordinates(selectionLeft, quad.ul_y, pageView)
                                val screenBottomRight = pdfToScreenCoordinates(selectionRight, quad.ll_y, pageView)
                                
                                val rect = RectF(
                                    screenTopLeft.x,
                                    screenTopLeft.y,
                                    screenBottomRight.x,
                                    screenBottomRight.y
                                )
                                rects.add(rect)
                            }
                        } catch (e: Exception) {
                            Log.w("TextPositionCalculator", "Failed to search block: ${block.text.take(20)}")
                        }
                    }
                }
            }
            
            if (rects.isEmpty()) {
                // 如果分段搜索也失败，使用估算方法
                return calculateSelectionRectsEstimated(startIndex, endIndex, pageText, pageView)
            }
            
        } catch (e: Exception) {
            Log.e("TextPositionCalculator", "Error in segmented selection calculation", e)
            return calculateSelectionRectsEstimated(startIndex, endIndex, pageText, pageView)
        }
        
        return rects
    }

    /**
     * 估算方法计算选择区域（后备方案）
     */
    private fun calculateSelectionRectsEstimated(
        startIndex: Int,
        endIndex: Int,
        pageText: String,
        pageView: PageView
    ): List<RectF> {
        val rects = mutableListOf<RectF>()
        
        try {
            val lines = pageText.split('\n')
            var currentIndex = 0
            val estimatedLineHeight = 24f
            val estimatedCharWidth = 12f
            var currentY = 100f
            
            for (line in lines) {
                val lineStart = currentIndex
                val lineEnd = currentIndex + line.length
                
                if (lineEnd > startIndex && lineStart < endIndex) {
                    val selectionStart = maxOf(startIndex, lineStart) - lineStart
                    val selectionEnd = minOf(endIndex, lineEnd) - lineStart
                    
                    val rect = RectF(
                        selectionStart * estimatedCharWidth,
                        currentY,
                        selectionEnd * estimatedCharWidth,
                        currentY + estimatedLineHeight
                    )
                    
                    rects.add(rect)
                }
                
                currentIndex = lineEnd + 1
                currentY += estimatedLineHeight
            }
            
        } catch (e: Exception) {
            Log.e("TextPositionCalculator", "Error in estimated selection calculation", e)
            // 使用页面中心区域作为兜底
            val centerX = pageView.width * 0.5f
            val centerY = pageView.height * 0.5f
            rects.add(RectF(centerX - 50f, centerY - 10f, centerX + 50f, centerY + 10f))
        }
        
        return rects
    }

    /**
     * 计算选择手柄的位置
     */
    fun calculateHandlePositions(
        startIndex: Int,
        endIndex: Int,
        pageText: String,
        pageView: PageView,
        core: MuPDFCore
    ): Pair<PointF, PointF> {
        val rects = calculateSelectionRects(startIndex, endIndex, pageText, pageView, core)
        
        if (rects.isEmpty()) {
            // 如果没有选择矩形，基于字符位置估算手柄位置
            Log.w("TextPositionCalculator", "No selection rects found, estimating handle positions")
            return estimateHandlePositions(startIndex, endIndex, pageText, pageView, core)
        }
        
        val firstRect = rects.first()
        val lastRect = rects.last()
        
        // 计算更好的手柄位置
        val startHandle = PointF(firstRect.left, firstRect.centerY())
        val endHandle = PointF(lastRect.right, lastRect.centerY())
        
        return Pair(startHandle, endHandle)
    }
    
    /**
     * 当无法获取选择矩形时，估算手柄位置
     */
    private fun estimateHandlePositions(
        startIndex: Int,
        endIndex: Int,
        pageText: String,
        pageView: PageView,
        core: MuPDFCore
    ): Pair<PointF, PointF> {
        try {
            // 尝试基于字符搜索来估算位置
            val startChar = if (startIndex < pageText.length) pageText[startIndex].toString() else "a"
            val endChar = if (endIndex < pageText.length) pageText[endIndex].toString() else "a"
            
            // 首先尝试搜索周围的文本来获得更准确的位置
            val contextStart = maxOf(0, startIndex - 10)
            val contextEnd = minOf(pageText.length, startIndex + 10)
            val startContext = pageText.substring(contextStart, contextEnd).trim()
            
            val endContextStart = maxOf(0, endIndex - 10)
            val endContextEnd = minOf(pageText.length, endIndex + 10)
            val endContext = pageText.substring(endContextStart, endContextEnd).trim()
            
            var startHandle: PointF? = null
            var endHandle: PointF? = null
            
            // 尝试搜索开始位置的上下文
            if (startContext.length > 3) {
                try {
                    val searchResults = core.searchPage(pageView.page, startContext)
                    if (searchResults.isNotEmpty() && searchResults[0].isNotEmpty()) {
                        val quad = searchResults[0][0]
                        val pdfX = quad.ul_x + (quad.ur_x - quad.ul_x) * 0.5f
                        val pdfY = quad.ul_y + (quad.ll_y - quad.ul_y) * 0.5f
                        val screenPos = pdfToScreenCoordinates(pdfX, pdfY, pageView)
                        startHandle = PointF(screenPos.x, screenPos.y)
                    }
                } catch (e: Exception) {
                    Log.w("TextPositionCalculator", "Failed to search start context")
                }
            }
            
            // 尝试搜索结束位置的上下文
            if (endContext.length > 3 && endContext != startContext) {
                try {
                    val searchResults = core.searchPage(pageView.page, endContext)
                    if (searchResults.isNotEmpty() && searchResults[0].isNotEmpty()) {
                        val quad = searchResults[0][0]
                        val pdfX = quad.ul_x + (quad.ur_x - quad.ul_x) * 0.5f
                        val pdfY = quad.ul_y + (quad.ll_y - quad.ul_y) * 0.5f
                        val screenPos = pdfToScreenCoordinates(pdfX, pdfY, pageView)
                        endHandle = PointF(screenPos.x + 20f, screenPos.y) // 稍微偏移以区分
                    }
                } catch (e: Exception) {
                    Log.w("TextPositionCalculator", "Failed to search end context")
                }
            }
            
            // 如果搜索失败，使用基于行列的估算
            if (startHandle == null || endHandle == null) {
                return estimateHandlePositionsByLineColumn(startIndex, endIndex, pageText, pageView)
            }
            
            Log.d("TextPositionCalculator", "Context-based handle positions: start(${startHandle.x}, ${startHandle.y}), end(${endHandle.x}, ${endHandle.y})")
            
            return Pair(startHandle, endHandle)
            
        } catch (e: Exception) {
            Log.e("TextPositionCalculator", "Error estimating handle positions", e)
            return estimateHandlePositionsByLineColumn(startIndex, endIndex, pageText, pageView)
        }
    }
    
    /**
     * 基于行列位置估算手柄位置
     */
    private fun estimateHandlePositionsByLineColumn(
        startIndex: Int,
        endIndex: Int,
        pageText: String,
        pageView: PageView
    ): Pair<PointF, PointF> {
        try {
            // 基于页面布局的估算
            val estimatedCharWidth = pageView.width / 80f // 假设每行80个字符
            val estimatedLineHeight = pageView.height / 50f // 假设50行
            
            // 计算行数和行内位置
            val linesBeforeStart = pageText.substring(0, minOf(startIndex, pageText.length)).count { it == '\n' }
            val linesBeforeEnd = pageText.substring(0, minOf(endIndex, pageText.length)).count { it == '\n' }
            
            val lastNewlineBeforeStart = pageText.substring(0, minOf(startIndex, pageText.length)).lastIndexOf('\n')
            val lastNewlineBeforeEnd = pageText.substring(0, minOf(endIndex, pageText.length)).lastIndexOf('\n')
            
            val startLinePos = startIndex - lastNewlineBeforeStart - 1
            val endLinePos = endIndex - lastNewlineBeforeEnd - 1
            
            // 计算屏幕位置，考虑页面边距
            val marginX = pageView.width * 0.05f // 5% 边距
            val marginY = pageView.height * 0.1f // 10% 顶部边距
            
            val startY = marginY + linesBeforeStart * estimatedLineHeight
            val endY = marginY + linesBeforeEnd * estimatedLineHeight
            val startX = marginX + maxOf(0, startLinePos) * estimatedCharWidth
            val endX = marginX + maxOf(0, endLinePos) * estimatedCharWidth
            
            val startHandle = PointF(
                startX.coerceIn(marginX, pageView.width - marginX),
                startY.coerceIn(marginY, pageView.height - marginY)
            )
            val endHandle = PointF(
                endX.coerceIn(marginX, pageView.width - marginX),
                endY.coerceIn(marginY, pageView.height - marginY)
            )
            
            Log.d("TextPositionCalculator", "Line-column based handle positions: start(${startHandle.x}, ${startHandle.y}), end(${endHandle.x}, ${endHandle.y})")
            
            return Pair(startHandle, endHandle)
            
        } catch (e: Exception) {
            Log.e("TextPositionCalculator", "Error in line-column estimation", e)
            // 最后的兜底方案
            return Pair(
                PointF(pageView.width * 0.3f, pageView.height * 0.5f),
                PointF(pageView.width * 0.7f, pageView.height * 0.5f)
            )
        }
    }
}
