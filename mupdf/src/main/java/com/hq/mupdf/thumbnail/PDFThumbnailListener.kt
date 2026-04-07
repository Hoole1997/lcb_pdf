package com.hq.mupdf.thumbnail

import android.content.Context
import com.hq.mupdf.R

/**
 * PDF缩略图事件监听接口
 * 定义缩略图栏各种用户交互的回调方法
 */
interface PDFThumbnailListener {
    
    /**
     * 缩略图点击事件
     * @param pageIndex 页面索引（从0开始）
     * @param pageNumber 页码（从1开始）
     */
    fun onThumbnailClick(pageIndex: Int, pageNumber: Int)
    
    /**
     * 缩略图长按事件
     * @param pageIndex 页面索引（从0开始）
     * @param pageNumber 页码（从1开始）
     */
    fun onThumbnailLongPress(pageIndex: Int, pageNumber: Int)
    
    /**
     * 缩略图栏可见性改变事件
     * @param isVisible 是否可见
     */
    fun onThumbnailBarVisibilityChanged(isVisible: Boolean)
    
    /**
     * 缩略图加载成功事件
     * @param pageIndex 页面索引（从0开始）
     */
    fun onThumbnailLoadSuccess(pageIndex: Int)
    
    /**
     * 缩略图加载失败事件
     * @param pageIndex 页面索引（从0开始）
     * @param error 错误信息
     */
    fun onThumbnailLoadError(pageIndex: Int, error: String)
    
    /**
     * 当前页面指示器更新事件
     * @param previousPage 之前的页面索引
     * @param currentPage 当前页面索引
     */
    fun onCurrentPageIndicatorChanged(previousPage: Int, currentPage: Int)
}

/**
 * 缩略图操作类型枚举
 */
enum class ThumbnailActionType(val actionName: String) {
    CLICK("click"),
    LONG_PRESS("long_press"),
    SCROLL("scroll"),
    LOAD("load"),
    VISIBILITY_CHANGE("visibility_change");
    
    fun getDisplayName(context: Context): String = when(this) {
        CLICK -> context.getString(R.string.event_click)
        LONG_PRESS -> context.getString(R.string.event_long_press)
        SCROLL -> context.getString(R.string.event_scroll)
        LOAD -> context.getString(R.string.event_load)
        VISIBILITY_CHANGE -> context.getString(R.string.event_visibility_change)
    }
}

/**
 * 缩略图状态枚举
 */
enum class ThumbnailState(val stateName: String) {
    LOADING("loading"),
    LOADED("loaded"),
    ERROR("error"),
    CURRENT("current"),
    NORMAL("normal");
    
    fun getDisplayName(context: Context): String = when(this) {
        LOADING -> context.getString(R.string.state_loading)
        LOADED -> context.getString(R.string.state_loaded)
        ERROR -> context.getString(R.string.state_error)
        CURRENT -> context.getString(R.string.state_current)
        NORMAL -> context.getString(R.string.state_normal)
    }
}
