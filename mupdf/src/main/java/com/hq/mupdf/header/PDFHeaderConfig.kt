package com.hq.mupdf.header

/**
 * PDF Header配置类
 * 用于配置PDF头部组件的外观和行为
 */
data class PDFHeaderConfig(
    /**
     * 是否显示返回按钮
     */
    val showBackButton: Boolean = true,
    
    /**
     * 是否显示搜索按钮
     */
    val showSearchButton: Boolean = true,
    
    /**
     * 是否显示书签按钮
     */
    val showBookmarkButton: Boolean = true,
    
    /**
     * 是否显示分享按钮
     */
    val showShareButton: Boolean = true,
    
    /**
     * 是否显示菜单按钮
     */
    val showMenuButton: Boolean = true,
    
    /**
     * 是否显示页码信息
     */
    val showPageInfo: Boolean = true,
    
    /**
     * 标题最大行数
     */
    val titleMaxLines: Int = 1,
    
    /**
     * 标题文本大小 (sp)
     */
    val titleTextSize: Float = 16f,
    
    /**
     * 页码信息文本大小 (sp)
     */
    val pageInfoTextSize: Float = 12f,
    
    /**
     * Header背景色资源ID
     */
    val backgroundColorRes: Int? = null,
    
    /**
     * Header高度 (dp)
     */
    val heightDp: Int = 56,
    
    /**
     * 水平边距 (dp)
     */
    val horizontalPaddingDp: Int = 16,
    
    /**
     * 按钮大小 (dp)
     */
    val buttonSizeDp: Int = 40,
    
    /**
     * 是否启用阴影效果
     */
    val enableElevation: Boolean = true,
    
    /**
     * 阴影高度 (dp)
     */
    val elevationDp: Float = 2f
) {
    companion object {
        /**
         * 默认配置
         */
        fun default() = PDFHeaderConfig()
        
        /**
         * 最小化配置 - 只显示基本功能
         */
        fun minimal() = PDFHeaderConfig(
            showSearchButton = false,
            showBookmarkButton = false,
            showShareButton = false,
            showMenuButton = false
        )
        
        /**
         * 只读配置 - 适用于只读PDF查看器
         */
        fun readOnly() = PDFHeaderConfig(
            showBookmarkButton = false
        )
        
        /**
         * 紧凑配置 - 适用于小屏幕
         */
        fun compact() = PDFHeaderConfig(
            heightDp = 48,
            buttonSizeDp = 36,
            titleTextSize = 14f,
            pageInfoTextSize = 11f,
            horizontalPaddingDp = 12
        )
    }
}
