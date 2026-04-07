package com.hq.mupdf.toolbar

/**
 * PDF工具栏事件监听接口
 * 定义了工具栏各种操作的回调事件
 */
interface PDFToolbarListener {
    
    /**
     * 工具选择事件
     * @param toolType 工具类型
     */
    fun onToolSelected(toolType: ToolType)
    
    /**
     * 缩放事件
     * @param zoomType 缩放类型
     */
    fun onZoomAction(zoomType: ZoomType)
    
    /**
     * 工具栏可见性改变事件
     * @param isVisible 是否可见
     */
    fun onToolbarVisibilityChanged(isVisible: Boolean)
}

/**
 * 工具类型枚举
 */
enum class ToolType(val toolName: String, val displayName: String) {
    SELECT("select", "选择工具"),
    TEXT("text", "文本工具"),
    HIGHLIGHT("highlight", "高亮工具"),
    UNDERLINE("underline", "下划线工具"),
    STRIKETHROUGH("strikethrough", "删除线工具"),
    COPY("copy", "复制工具"),
    CLEAR_ANNOTATION("clear_annotation", "清除注释工具");
    
    companion object {
        fun fromToolName(toolName: String): ToolType {
            return values().find { it.toolName == toolName } ?: SELECT
        }
    }
}

/**
 * 缩放类型枚举
 */
enum class ZoomType {
    ZOOM_IN,     // 放大
    ZOOM_OUT,    // 缩小
    FIT_WIDTH,   // 适合宽度
    CUSTOM       // 自定义缩放
}
