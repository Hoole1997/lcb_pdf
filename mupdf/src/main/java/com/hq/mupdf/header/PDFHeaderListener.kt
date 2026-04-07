package com.hq.mupdf.header

/**
 * PDF Header事件监听器接口
 * 定义PDF头部组件与外部组件的通信协议
 */
interface PDFHeaderListener {
    
    /**
     * 返回按钮点击事件
     */
    fun onBackButtonClick() {
        // 默认实现：可选重写
    }
    
    /**
     * 搜索按钮点击事件
     */
    fun onSearchButtonClick() {
        // 默认实现：可选重写
    }
    
    /**
     * 书签按钮点击事件
     * @param isBookmarked 当前书签状态
     */
    fun onBookmarkButtonClick(isBookmarked: Boolean) {
        // 默认实现：可选重写
    }
    
    /**
     * 保存按钮点击事件
     * @param fileName 当前文件名
     * @param currentPage 当前页码
     */
    fun onSaveButtonClick(fileName: String?, currentPage: Int) {
        // 默认实现：可选重写
    }
    
    /**
     * 菜单按钮点击事件
     */
    fun onMenuButtonClick() {
        // 默认实现：可选重写
    }
    
    /**
     * 标题点击事件
     * @param fileName 文件名
     */
    fun onTitleClick(fileName: String?) {
        // 默认实现：可选重写
    }
    
    /**
     * 页码信息点击事件
     * @param currentPage 当前页码
     * @param totalPages 总页数
     */
    fun onPageInfoClick(currentPage: Int, totalPages: Int) {
        // 默认实现：可选重写
    }
}

/**
 * PDF Header状态变化监听器接口
 * 用于监听Header内部状态的变化
 */
interface PDFHeaderStateListener {
    
    /**
     * Header可见性变化
     * @param isVisible 是否可见
     */
    fun onHeaderVisibilityChanged(isVisible: Boolean) {
        // 默认实现：可选重写
    }
    
    /**
     * 书签状态变化
     * @param isBookmarked 是否已添加书签
     */
    fun onBookmarkStateChanged(isBookmarked: Boolean) {
        // 默认实现：可选重写
    }
}

/**
 * PDF Header数据更新监听器接口
 * 用于接收外部数据更新通知
 */
interface PDFHeaderDataListener {
    
    /**
     * 文档信息更新
     * @param fileName 文件名
     * @param currentPage 当前页码
     * @param totalPages 总页数
     */
    fun onDocumentInfoUpdated(fileName: String?, currentPage: Int, totalPages: Int) {
        // 默认实现：可选重写
    }
    
    /**
     * 页面变化
     * @param currentPage 当前页码
     * @param totalPages 总页数
     */
    fun onPageChanged(currentPage: Int, totalPages: Int) {
        // 默认实现：可选重写
    }
    
    /**
     * 加载状态变化
     * @param isLoading 是否正在加载
     */
    fun onLoadingStateChanged(isLoading: Boolean) {
        // 默认实现：可选重写
    }
}
