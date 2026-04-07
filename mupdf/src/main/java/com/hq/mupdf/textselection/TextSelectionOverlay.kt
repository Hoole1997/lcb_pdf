package com.hq.mupdf.textselection

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.util.Log
import android.view.View
import com.hq.mupdf.R

/**
 * 文本选择覆盖层
 * 用于显示文本选择的高亮区域和操作手柄
 */
class TextSelectionOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    init {
        Log.d("TextSelectionOverlay", "TextSelectionOverlay created")
        visibility = View.VISIBLE
    }

    // 绘制工具
    private val selectionPaint = Paint().apply {
        color = context.getColor(R.color.text_selection_highlight)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val handlePaint = Paint().apply {
        color = context.getColor(R.color.text_selection_handle)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val handleStrokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4.5f // 更粗的边框配合更大的手柄
        isAntiAlias = true
    }
    
    init {
        // 确保 View 可以接收触摸事件
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
        Log.d("TextSelectionOverlay", "TextSelectionOverlay 初始化完成，已设置触摸属性")
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d("TextSelectionOverlay", "onMeasure: width=${measuredWidth}, height=${measuredHeight}")
    }
    
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        Log.d("TextSelectionOverlay", "onLayout: changed=$changed, size=(${right-left}, ${bottom-top}), isClickable=$isClickable")
    }

    // 选择区域数据
    private var selectionRects = mutableListOf<RectF>()
    private var startHandle: PointF? = null
    private var endHandle: PointF? = null
    private val handleRadius = 32f // 🎯 超大手柄视觉大小，确保清晰可见
    private val handleStickHeight = 48f // 🎯 更长的手柄杆，便于精确定位
    private val touchRadius = 96f // 🚀 巨大触摸区域，确保极佳的拖拽体验（比视觉大小大3倍）

    // 触摸处理
    private var isDraggingStartHandle = false
    private var isDraggingEndHandle = false
    private var isStartHandlePressed = false
    private var isEndHandlePressed = false
    
    // 🎯 防误触逻辑
    private var lastHandleDragEndTime = 0L
    private val CLEAR_SELECTION_DELAY = 200L  // 手柄拖动结束后200ms内不响应清除
    private var onHandleDragListener: OnHandleDragListener? = null
    
    // 手柄是否可见和可操作
    private var handlesVisible = false

    interface OnHandleDragListener {
        fun onStartHandleDrag(x: Float, y: Float)
        fun onEndHandleDrag(x: Float, y: Float)
        fun onHandleDragEnd()
        fun onSelectionMenuRequested(centerX: Float, centerY: Float)
        fun onClearSelection()  // 🎯 新增：清除选择的回调
    }

    fun setOnHandleDragListener(listener: OnHandleDragListener) {
        onHandleDragListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        Log.d("TextSelectionOverlay", "onDraw called, selectionRects size: ${selectionRects.size}")

        // 绘制选择高亮区域
        selectionRects.forEach { rect ->
            canvas.drawRect(rect, selectionPaint)
        }

        // 只有在手柄可见时才绘制
        Log.d("TextSelectionOverlay", "onDraw: handlesVisible=$handlesVisible, startHandle=$startHandle, endHandle=$endHandle")
        if (handlesVisible) {
            // 绘制开始手柄
            startHandle?.let { handle ->
                Log.d("TextSelectionOverlay", "Drawing start handle: $handle")
                drawSelectionHandle(canvas, handle.x, handle.y, true, isStartHandlePressed)
            }

            // 绘制结束手柄
            endHandle?.let { handle ->
                Log.d("TextSelectionOverlay", "Drawing end handle: $handle")
                drawSelectionHandle(canvas, handle.x, handle.y, false, isEndHandlePressed)
            }
        }
    }

    private fun drawSelectionHandle(canvas: Canvas, x: Float, y: Float, isStart: Boolean, isPressed: Boolean = false) {
        // 优化手柄绘制 - 超大手柄配套的粗手柄杆
        val stickWidth = 5.5f // 更粗的手柄杆配合更大的手柄
        val handleY: Float
        val stickTop: Float
        val stickBottom: Float
        
        if (isStart) {
            // 开始手柄：圆形在上，杆向下
            handleY = y - handleStickHeight
            stickTop = handleY
            stickBottom = y
        } else {
            // 结束手柄：圆形在下，杆向上  
            handleY = y + handleStickHeight
            stickTop = y
            stickBottom = handleY
        }

        // 绘制手柄杆（更粗一些）
        val stickPaint = Paint(handlePaint).apply {
            strokeWidth = stickWidth * 1.2f // 稍微加粗手柄杆
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(x, stickTop, x, stickBottom, stickPaint)

        // 绘制阴影（配合超大手柄的明显阴影）
        val shadowOffset = 4.5f  // 更大的阴影偏移配合超大手柄
        val shadowPaint = Paint().apply {
            color = Color.BLACK
            alpha = 85  // 稍深的阴影
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x + shadowOffset, handleY + shadowOffset, handleRadius + 2f, shadowPaint)

        // 绘制手柄圆形主体（按下时更大更明显）
        val currentRadius = if (isPressed) handleRadius * 1.4f else handleRadius // 更明显的放大效果
        val currentHandlePaint = if (isPressed) {
            Paint(handlePaint).apply { 
                alpha = 160  // 按下时更透明一些
                // 按下时使用稍亮的颜色
                color = Color.rgb(
                    minOf(255, Color.red(handlePaint.color) + 30),
                    minOf(255, Color.green(handlePaint.color) + 30), 
                    minOf(255, Color.blue(handlePaint.color) + 30)
                )
            }
        } else {
            handlePaint
        }
        
        canvas.drawCircle(x, handleY, currentRadius, currentHandlePaint)
        
        // 绘制白色边框
        canvas.drawCircle(x, handleY, currentRadius, handleStrokePaint)
        
        // 绘制中心内部圆点（现代网页风格）
        val innerDotRadius = currentRadius * 0.3f
        val innerDotPaint = Paint().apply {
            color = Color.WHITE
            alpha = if (isPressed) 200 else 255
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, handleY, innerDotRadius, innerDotPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("TextSelectionOverlay", "!!! onTouchEvent 被调用了 !!! action=${event.action}, handlesVisible=$handlesVisible")
        
        // 如果手柄不可见，不处理触摸事件
        if (!handlesVisible) {
            Log.d("TextSelectionOverlay", "手柄不可见，跳过触摸事件处理")
            return false
        }
        
        Log.d("TextSelectionOverlay", "处理触摸事件: action=${event.action}, 手柄可见: $handlesVisible")
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                Log.d("TextSelectionOverlay", "🎯 ACTION_DOWN: 触摸点($x,$y)")

                // 🚀 优先检查所有手柄，避免遗漏
                var handleFound = false
                
                // 检查是否点击了开始手柄
                startHandle?.let { handle ->
                    Log.d("TextSelectionOverlay", "🔍 检查开始手柄区域: 点击($x,$y), 手柄基点(${handle.x},${handle.y})")
                    Log.d("TextSelectionOverlay", "🎯 手柄触摸参数: touchRadius=$touchRadius, handleRadius=$handleRadius")
                    val hitStart = isPointInStartHandle(x, y, handle.x, handle.y)
                    Log.d("TextSelectionOverlay", "🎯 开始手柄命中检测: $hitStart")
                    if (hitStart) {
                        Log.d("TextSelectionOverlay", "🚀 开始拖拽开始手柄")
                        isDraggingStartHandle = true
                        isStartHandlePressed = true
                        handleFound = true
                        invalidate() // 重绘以显示按下效果
                        return true
                    }
                }

                // 检查是否点击了结束手柄（只有开始手柄未命中时才检查）
                if (!handleFound) {
                    endHandle?.let { handle ->
                        Log.d("TextSelectionOverlay", "🔍 检查结束手柄区域: 点击($x,$y), 手柄基点(${handle.x},${handle.y})")
                        Log.d("TextSelectionOverlay", "🎯 手柄触摸参数: touchRadius=$touchRadius, handleRadius=$handleRadius")
                        val hitEnd = isPointInEndHandle(x, y, handle.x, handle.y)
                        Log.d("TextSelectionOverlay", "🎯 结束手柄命中检测: $hitEnd")
                        if (hitEnd) {
                            Log.d("TextSelectionOverlay", "🚀 开始拖拽结束手柄")
                            isDraggingEndHandle = true
                            isEndHandlePressed = true
                            handleFound = true
                            invalidate() // 重绘以显示按下效果
                            return true
                        }
                    }
                }
                
                // 🎯 如果没有命中任何手柄，记录详细信息
                if (!handleFound) {
                    Log.d("TextSelectionOverlay", "❌ ACTION_DOWN: 未命中任何手柄")
                    Log.d("TextSelectionOverlay", "   开始手柄: ${startHandle?.let { "(${it.x}, ${it.y})" } ?: "null"}")
                    Log.d("TextSelectionOverlay", "   结束手柄: ${endHandle?.let { "(${it.x}, ${it.y})" } ?: "null"}")
                    Log.d("TextSelectionOverlay", "   触摸区域: radius=$touchRadius")
                }
                
                // 如果点击了选择区域但不是手柄，不做任何处理
                // 保持手柄可见，让用户可以继续操作
                if (isPointInSelection(x, y)) {
                    // 只记录点击，不触发菜单请求，避免手柄被意外隐藏
                    Log.d("TextSelectionOverlay", "点击了选择区域，保持手柄可见")
                    return true
                }
                
                // 🎯 新功能：点击空白区域取消高亮选择
                if (!handleFound && handlesVisible) {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastDrag = currentTime - lastHandleDragEndTime
                    
                    if (timeSinceLastDrag < CLEAR_SELECTION_DELAY) {
                        Log.d("TextSelectionOverlay", "🛡️ 手柄拖动刚结束($timeSinceLastDrag ms < $CLEAR_SELECTION_DELAY ms)，跳过清除选择")
                        return true
                    }
                    
                    Log.d("TextSelectionOverlay", "🗑️ 点击空白区域，请求取消高亮选择 (距离上次拖动: $timeSinceLastDrag ms)")
                    onHandleDragListener?.onClearSelection()
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                Log.d("TextSelectionOverlay", "🎯 ACTION_MOVE: 移动到($x,$y)")
                Log.d("TextSelectionOverlay", "   拖动状态: isDraggingStartHandle=$isDraggingStartHandle, isDraggingEndHandle=$isDraggingEndHandle")
                
                if (isDraggingStartHandle) {
                    Log.d("TextSelectionOverlay", "🚀 ACTION_MOVE: 拖拽开始手柄到 ($x, $y)")
                    onHandleDragListener?.onStartHandleDrag(x, y)
                    invalidate() // 强制重绘以实时更新
                    return true
                }
                if (isDraggingEndHandle) {
                    Log.d("TextSelectionOverlay", "🚀 ACTION_MOVE: 拖拽结束手柄到 ($x, $y)")
                    onHandleDragListener?.onEndHandleDrag(x, y)
                    invalidate() // 强制重绘以实时更新
                    return true
                }
                
                // 🚀 紧急恢复机制：如果手指移动但没有拖动状态，重新检测手柄
                Log.d("TextSelectionOverlay", "⚠️ ACTION_MOVE: 手指移动但无拖动状态 - 尝试紧急恢复")
                
                // 使用更宽松的检测条件重新检查手柄
                startHandle?.let { handle ->
                    val emergencyDistance = Math.sqrt(
                        Math.pow((x - handle.x).toDouble(), 2.0) +
                        Math.pow((y - handle.y).toDouble(), 2.0)
                    )
                    Log.d("TextSelectionOverlay", "🆘 紧急检测开始手柄: 距离=$emergencyDistance, 扩大阈值=${touchRadius * 1.5f}")
                    if (emergencyDistance <= touchRadius * 1.5f) { // 扩大50%检测范围
                        Log.d("TextSelectionOverlay", "🚀 紧急恢复: 开始拖拽开始手柄")
                        isDraggingStartHandle = true
                        isStartHandlePressed = true
                        onHandleDragListener?.onStartHandleDrag(x, y)
                        invalidate()
                        return true
                    }
                }
                
                endHandle?.let { handle ->
                    val emergencyDistance = Math.sqrt(
                        Math.pow((x - handle.x).toDouble(), 2.0) +
                        Math.pow((y - handle.y).toDouble(), 2.0)
                    )
                    Log.d("TextSelectionOverlay", "🆘 紧急检测结束手柄: 距离=$emergencyDistance, 扩大阈值=${touchRadius * 1.5f}")
                    if (emergencyDistance <= touchRadius * 1.5f) { // 扩大50%检测范围
                        Log.d("TextSelectionOverlay", "🚀 紧急恢复: 开始拖拽结束手柄")
                        isDraggingEndHandle = true
                        isEndHandlePressed = true
                        onHandleDragListener?.onEndHandleDrag(x, y)
                        invalidate()
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasPressed = isStartHandlePressed || isEndHandlePressed
                val wasDragging = isDraggingStartHandle || isDraggingEndHandle
                
                // 重置按下状态
                isStartHandlePressed = false
                isEndHandlePressed = false
                
                if (wasDragging) {
                    // 🎯 记录手柄拖动结束时间，用于防误触
                    lastHandleDragEndTime = System.currentTimeMillis()
                    // 拖拽结束，确保手柄保持可见
                    isDraggingStartHandle = false
                    isDraggingEndHandle = false
                    handlesVisible = true // 明确确保手柄可见
                    onHandleDragListener?.onHandleDragEnd()
                    Log.d("TextSelectionOverlay", "拖拽结束，记录时间: $lastHandleDragEndTime")
                    invalidate() // 重绘以移除按下效果
                    return true
                }
                
                if (wasPressed) {
                    // 确保手柄在释放后保持可见
                    handlesVisible = true
                    invalidate() // 重绘以移除按下效果
                    return true
                }
            }
        }

        return false
    }
    
    /**
     * 手动触发上下文菜单（例如长按时调用）
     */
    fun requestSelectionMenu() {
        val center = getSelectionCenter()
        center?.let { 
            onHandleDragListener?.onSelectionMenuRequested(it.x, it.y)
        }
    }

    /**
     * 🎯 智能开始手柄触摸检测：覆盖整个手柄区域（圆形+杆）
     */
    private fun isPointInStartHandle(pointX: Float, pointY: Float, handleBaseX: Float, handleBaseY: Float): Boolean {
        // 开始手柄：圆形在上，杆向下
        val handleCircleY = handleBaseY - handleStickHeight
        
        // 检测圆形区域（扩大触摸半径）
        val distanceToCircle = Math.sqrt(
            Math.pow((pointX - handleBaseX).toDouble(), 2.0) +
            Math.pow((pointY - handleCircleY).toDouble(), 2.0)
        )
        val isInCircle = distanceToCircle <= touchRadius
        
                // 检测杆区域（矩形区域）- 🚀 使用超大触摸宽度
        val stickLeft = handleBaseX - touchRadius  // 杆的触摸宽度等于触摸半径
        val stickRight = handleBaseX + touchRadius  // 让整个触摸区域都能抓住杆
        val stickTop = handleCircleY
        val stickBottom = handleBaseY
        val isInStick = pointX >= stickLeft && pointX <= stickRight &&
                       pointY >= stickTop && pointY <= stickBottom
        
        val result = isInCircle || isInStick
        Log.d("TextSelectionOverlay", "🎯开始手柄检测: 点击($pointX,$pointY)")
        Log.d("TextSelectionOverlay", "  圆形中心($handleBaseX,$handleCircleY), 距离=$distanceToCircle, 半径=$touchRadius, 圆形命中=$isInCircle")
        Log.d("TextSelectionOverlay", "  杆区域[$stickLeft-$stickRight, $stickTop-$stickBottom], 杆命中=$isInStick")
        Log.d("TextSelectionOverlay", "  最终结果=$result")
        
        return result
    }
    
    /**
     * 🎯 智能结束手柄触摸检测：覆盖整个手柄区域（圆形+杆）
     */
    private fun isPointInEndHandle(pointX: Float, pointY: Float, handleBaseX: Float, handleBaseY: Float): Boolean {
        // 结束手柄：圆形在下，杆向上
        val handleCircleY = handleBaseY + handleStickHeight
        
        // 检测圆形区域（扩大触摸半径）
        val distanceToCircle = Math.sqrt(
            Math.pow((pointX - handleBaseX).toDouble(), 2.0) +
            Math.pow((pointY - handleCircleY).toDouble(), 2.0)
        )
        val isInCircle = distanceToCircle <= touchRadius
        
                // 检测杆区域（矩形区域）- 🚀 使用超大触摸宽度  
        val stickLeft = handleBaseX - touchRadius  // 杆的触摸宽度等于触摸半径
        val stickRight = handleBaseX + touchRadius  // 让整个触摸区域都能抓住杆
        val stickTop = handleBaseY
        val stickBottom = handleCircleY
        val isInStick = pointX >= stickLeft && pointX <= stickRight &&
                       pointY >= stickTop && pointY <= stickBottom
        
        val result = isInCircle || isInStick
        Log.d("TextSelectionOverlay", "🎯结束手柄检测: 点击($pointX,$pointY)")
        Log.d("TextSelectionOverlay", "  圆形中心($handleBaseX,$handleCircleY), 距离=$distanceToCircle, 半径=$touchRadius, 圆形命中=$isInCircle")
        Log.d("TextSelectionOverlay", "  杆区域[$stickLeft-$stickRight, $stickTop-$stickBottom], 杆命中=$isInStick")
        Log.d("TextSelectionOverlay", "  最终结果=$result")
        
        return result
    }
    
    private fun isPointInHandle(pointX: Float, pointY: Float, handleX: Float, handleY: Float): Boolean {
        // 🎯 保留原方法以备用
        val distance = Math.sqrt(
            Math.pow((pointX - handleX).toDouble(), 2.0) +
            Math.pow((pointY - handleY).toDouble(), 2.0)
        )
        return distance <= touchRadius
    }
    
    private fun isPointInSelection(x: Float, y: Float): Boolean {
        return selectionRects.any { rect ->
            rect.contains(x, y)
        }
    }

    /**
     * 更新选择区域
     */
    fun updateSelection(
        rects: List<RectF>,
        startX: Float = 0f,
        startY: Float = 0f,
        endX: Float = 0f,
        endY: Float = 0f,
        preserveHandlePositions: Boolean = false
    ) {
        Log.d("TextSelectionOverlay", "updateSelection called with ${rects.size} rects, preserveHandlePositions=$preserveHandlePositions")
        selectionRects.clear()
        selectionRects.addAll(rects)

        // 🔑 简化逻辑：优先使用传入的精确坐标（基于MuPDF bounds计算）
        if (preserveHandlePositions && (startX != 0f || startY != 0f || endX != 0f || endY != 0f)) {
            // 在拖动更新时，始终使用传入的基于MuPDF bounds的精确坐标
            startHandle = PointF(startX, startY)
            endHandle = PointF(endX, endY)
            Log.d("TextSelectionOverlay", "拖动更新 - 使用基于MuPDF bounds的精确坐标:")
            Log.d("TextSelectionOverlay", "  startHandle: ($startX, $startY)")
            Log.d("TextSelectionOverlay", "  endHandle: ($endX, $endY)")
            Log.d("TextSelectionOverlay", "  isDraggingStart: $isDraggingStartHandle, isDraggingEnd: $isDraggingEndHandle")
        } else if (rects.isNotEmpty()) {
            // 🎯 优化手柄位置计算：更精确的行首行尾定位
            val firstRect = rects.first()
            val lastRect = rects.last()
            
            // 🚀 行首手柄：稍微向左偏移，避免边界问题
            val startX = Math.max(0f, firstRect.left - 2f) // 向左偏移2px，但不超出边界
            val startY = firstRect.top + (firstRect.height() * 0.3f) // 垂直居中偏上
            
            // 🚀 行尾手柄：稍微向右偏移，避免边界问题  
            val endX = lastRect.right + 2f // 向右偏移2px
            val endY = lastRect.bottom - (lastRect.height() * 0.3f) // 垂直居中偏下
            
            startHandle = PointF(startX, startY)
            endHandle = PointF(endX, endY)
            
            Log.d("TextSelectionOverlay", "🎯 优化手柄位置计算:")
            Log.d("TextSelectionOverlay", "   原始firstRect: (${firstRect.left}, ${firstRect.top}, ${firstRect.right}, ${firstRect.bottom})")
            Log.d("TextSelectionOverlay", "   原始lastRect: (${lastRect.left}, ${lastRect.top}, ${lastRect.right}, ${lastRect.bottom})")
            Log.d("TextSelectionOverlay", "   优化后startHandle: ($startX, $startY)")
            Log.d("TextSelectionOverlay", "   优化后endHandle: ($endX, $endY)")
        } else {
            // 如果没有选择区域，使用传入的坐标
            startHandle = PointF(startX, startY)
            endHandle = PointF(endX, endY)
            Log.d("TextSelectionOverlay", "没有选择区域，使用传入坐标 - start: ($startX, $startY), end: ($endX, $endY)")
        }
        
        // 确保视图可见
        if (visibility != View.VISIBLE) {
            Log.d("TextSelectionOverlay", "Setting visibility to VISIBLE")
            visibility = View.VISIBLE
        }
        
        // 显示手柄
        handlesVisible = true
        
        Log.d("TextSelectionOverlay", "手柄已设置为可见, startHandle=$startHandle, endHandle=$endHandle")
        Log.d("TextSelectionOverlay", "Calling invalidate")
        invalidate()
    }

    /**
     * 清除选择
     */
    fun clearSelection() {
        Log.d("TextSelectionOverlay", "!!! clearSelection 被调用了 !!! 调用堆栈：")
        Thread.currentThread().stackTrace.take(10).forEach { element ->
            Log.d("TextSelectionOverlay", "   at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
        }
        
        Log.d("TextSelectionOverlay", "清除前: selectionRects.size=${selectionRects.size}, handlesVisible=$handlesVisible")
        
        selectionRects.clear()
        startHandle = null
        endHandle = null
        handlesVisible = false
        isStartHandlePressed = false
        isEndHandlePressed = false
        isDraggingStartHandle = false
        isDraggingEndHandle = false
        
        Log.d("TextSelectionOverlay", "清除后: selectionRects.size=${selectionRects.size}, handlesVisible=$handlesVisible")
        Log.d("TextSelectionOverlay", "调用invalidate()强制重绘...")
        
        // 强制重绘以清除蓝色高亮
        invalidate()
        
        // 额外确保父视图也重绘
        (parent as? android.view.View)?.invalidate()
        
        Log.d("TextSelectionOverlay", "✅ clearSelection 完成")
    }

    /**
     * 是否有选择内容
     */
    fun hasSelection(): Boolean {
        return selectionRects.isNotEmpty()
    }

    /**
     * 获取选择区域的中心点，用于显示上下文菜单
     */
    fun getSelectionCenter(): PointF? {
        if (selectionRects.isEmpty()) return null

        var totalX = 0f
        var totalY = 0f
        var count = 0

        selectionRects.forEach { rect ->
            totalX += rect.centerX()
            totalY += rect.centerY()
            count++
        }

        return if (count > 0) {
            PointF(totalX / count, totalY / count)
        } else null
    }
    
    /**
     * 临时隐藏手柄（例如显示菜单时）
     * 注意：这是临时的，用户触摸后会重新显示
     */
    fun hideHandlesTemporarily() {
        handlesVisible = false
        invalidate()
    }
    
    /**
     * 显示手柄
     */
    fun showHandles() {
        if (hasSelection()) {
            handlesVisible = true
            invalidate()
        }
    }
    
    /**
     * 确保手柄可见（强制显示）
     */
    fun ensureHandlesVisible() {
        if (hasSelection()) {
            handlesVisible = true
            invalidate()
            Log.d("TextSelectionOverlay", "强制确保手柄可见")
        }
    }
    
    /**
     * 更新单个手柄位置（拖拽时调用）
     */
    fun updateHandlePosition(isStart: Boolean, x: Float, y: Float) {
        if (isStart) {
            startHandle = PointF(x, y)
        } else {
            endHandle = PointF(x, y)
        }
        invalidate()
    }
}


