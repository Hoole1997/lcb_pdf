package com.hq.mupdf.bottombar

import android.content.Context
import com.hq.mupdf.R

/**
 * PDF底部栏事件监听接口
 * 定义底部栏各种用户交互的回调方法
 */
interface PDFBottomBarListener {
    
    /**
     * 页面跳转事件
     * @param targetPage 目标页码（从1开始）
     * @param totalPages 总页数
     * @return 是否成功跳转到目标页面
     */
    fun onPageJump(targetPage: Int, totalPages: Int): Boolean
    
    /**
     * 上一页按钮点击事件
     * @param currentPage 当前页码（从0开始）
     * @return 是否成功跳转
     */
    fun onPreviousPage(currentPage: Int): Boolean
    
    /**
     * 下一页按钮点击事件
     * @param currentPage 当前页码（从0开始）
     * @return 是否成功跳转
     */
    fun onNextPage(currentPage: Int): Boolean
    
    /**
     * 缩略图切换事件
     */
    fun onThumbnailToggle()
    
    /**
     * 查看方向切换事件
     * @param isHorizontal 是否为横向模式（true为横向，false为竖向）
     */
    fun onDirectionToggle(isHorizontal: Boolean)
    
    /**
     * 底部栏可见性改变事件
     * @param isVisible 是否可见
     */
    fun onBottomBarVisibilityChanged(isVisible: Boolean)
}

/**
 * 页面导航类型枚举
 */
enum class NavigationType(val typeName: String) {
    PREVIOUS("previous"),
    NEXT("next"),
    JUMP("jump"),
    FIRST("first"),
    LAST("last");
    
    fun getDisplayName(context: Context): String = when(this) {
        PREVIOUS -> context.getString(R.string.action_previous)
        NEXT -> context.getString(R.string.action_next)
        JUMP -> context.getString(R.string.action_jump)
        FIRST -> context.getString(R.string.action_first)
        LAST -> context.getString(R.string.action_last)
    }
}

/**
 * 底部栏控制类型枚举
 */
enum class BottomBarControlType(val controlName: String) {
    PAGE_NAVIGATION("navigation"),
    THUMBNAIL("thumbnail"),
    DIRECTION_TOGGLE("direction"),
    VISIBILITY("visibility");
    
    fun getDisplayName(context: Context): String = when(this) {
        PAGE_NAVIGATION -> context.getString(R.string.category_page_navigation)
        THUMBNAIL -> context.getString(R.string.category_thumbnail)
        DIRECTION_TOGGLE -> context.getString(R.string.category_direction_toggle)
        VISIBILITY -> context.getString(R.string.category_visibility)
    }
}

/**
 * PDF查看方向枚举
 */
enum class PDFViewDirection(val orientationName: String) {
    HORIZONTAL("horizontal"),
    VERTICAL("vertical");
    
    fun getDisplayName(context: Context): String = when(this) {
        HORIZONTAL -> context.getString(R.string.direction_horizontal)
        VERTICAL -> context.getString(R.string.direction_vertical)
    }
}
