package com.documentpro.office.business.fileviewer.ui.success

import com.documentpro.office.business.fileviewer.ui.tool.ToolType

data class BusinessSuccessFunction(
    val toolType: ToolType,
    val iconRes: Int,      // 图标资源
    val title: String,     // 标题
    val desc: String,      // 描述
    val btnText: String,   // 按钮文字
) {
    /**
     * 判断是否为广告占位符
     */
    fun isAd(): Boolean = title == "ad"
    
    companion object {
        /**
         * 创建广告占位符
         */
        fun createAdPlaceholder(): BusinessSuccessFunction {
            return BusinessSuccessFunction(
                toolType = ToolType.IMPORT_FILE,  // 占位符，不会被使用
                iconRes = 0,
                title = "ad",
                desc = "",
                btnText = ""
            )
        }
    }
}