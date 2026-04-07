package com.hq.mupdf.header

import android.content.Context
import android.view.View
import android.widget.Toast
import com.hq.mupdf.R

/**
 * PDF Header使用示例
 * 展示如何使用PDFHeader组件
 */
class PDFHeaderUsageExample {
    
    companion object {
        
        /**
         * 基本使用示例
         */
        fun basicUsage(context: Context, headerView: View) {
            // 1. 创建默认配置
            val config = PDFHeaderConfig.default()
            
            // 2. 创建监听器
            val listener = object : PDFHeaderListener {
                override fun onBackButtonClick() {
                    // 处理返回按钮点击
                    Toast.makeText(context, context.getString(R.string.back_button_clicked), Toast.LENGTH_SHORT).show()
                }
                
                override fun onSearchButtonClick() {
                    // 处理搜索按钮点击
                    Toast.makeText(context, context.getString(R.string.search_button_clicked), Toast.LENGTH_SHORT).show()
                }
                
                override fun onBookmarkButtonClick(isBookmarked: Boolean) {
                    // 处理书签按钮点击
                    val message = if (isBookmarked) context.getString(R.string.bookmark_added) else context.getString(R.string.bookmark_removed)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
            
            // 3. 创建PDFHeader实例
            val pdfHeader = PDFHeader(context, headerView, config, listener)
            
            // 4. 设置文档信息
            pdfHeader.updateDocumentInfo("example_document.pdf", 1, 20)
        }
        
        /**
         * 自定义配置示例
         */
        fun customConfigUsage(context: Context, headerView: View) {
            // 创建自定义配置
            val config = PDFHeaderConfig(
                showSearchButton = false,      // 隐藏搜索按钮
                showBookmarkButton = true,     // 显示书签按钮
                showShareButton = false,       // 隐藏分享按钮
                showMenuButton = true,         // 显示菜单按钮
                titleTextSize = 18f,           // 增大标题字体
                pageInfoTextSize = 14f,        // 增大页码字体
                heightDp = 64,                 // 增加高度
                enableElevation = true         // 启用阴影
            )
            
            val pdfHeader = PDFHeader(context, headerView, config)
        }
        
        /**
         * 紧凑布局示例
         */
        fun compactUsage(context: Context, headerView: View) {
            // 使用紧凑配置
            val config = PDFHeaderConfig.compact()
            val pdfHeader = PDFHeader(context, headerView, config)
            
            // 设置文档信息
            pdfHeader.setFileName("compact_layout_document.pdf")
            pdfHeader.updatePageInfo(5, 10)
        }
        
        /**
         * 只读模式示例
         */
        fun readOnlyUsage(context: Context, headerView: View) {
            // 使用只读配置
            val config = PDFHeaderConfig.readOnly()
            val pdfHeader = PDFHeader(context, headerView, config)
        }
        
        /**
         * 动态功能控制示例
         */
        fun dynamicControlExample(context: Context, headerView: View) {
            val config = PDFHeaderConfig.default()
            val pdfHeader = PDFHeader(context, headerView, config)
            
            // 动态设置加载状态
            pdfHeader.setLoadingState(true)
            
            // 模拟文档加载完成
            // pdfHeader.setLoadingState(false)
            // pdfHeader.updateDocumentInfo("动态加载文档.pdf", 1, 15)
            
            // 动态控制书签状态
            pdfHeader.setBookmarkState(true)  // 添加书签
            
            // 动态控制可见性
            pdfHeader.hide()  // 隐藏
            // pdfHeader.show()  // 显示
            // pdfHeader.toggle()  // 切换
        }
        
        /**
         * 完整监听器示例
         */
        fun fullListenerExample(context: Context, headerView: View) {
            val listener = object : PDFHeaderListener {
                override fun onBackButtonClick() {
                    // 返回上一页面或关闭当前页面
                }
                
                override fun onSearchButtonClick() {
                    // 打开搜索界面
                }
                
                override fun onBookmarkButtonClick(isBookmarked: Boolean) {
                    // 添加或移除书签
                    if (isBookmarked) {
                        // 保存书签到数据库
                    } else {
                        // 从数据库删除书签
                    }
                }
                
                override fun onSaveButtonClick(fileName: String?, currentPage: Int) {
                    // 分享当前文档或页面
                    val shareText = context.getString(R.string.file_name_format, fileName ?: context.getString(R.string.unknown_file)) + ", " + context.getString(R.string.current_page_format, currentPage, 0)
                    // 执行分享逻辑
                }
                
                override fun onMenuButtonClick() {
                    // 显示更多操作菜单
                }
                
                override fun onTitleClick(fileName: String?) {
                    // 显示文件详细信息
                }
                
                override fun onPageInfoClick(currentPage: Int, totalPages: Int) {
                    // 显示页面跳转对话框
                }
            }
            
            val stateListener = object : PDFHeaderStateListener {
                override fun onHeaderVisibilityChanged(isVisible: Boolean) {
                    // 响应Header可见性变化
                }
                
                override fun onBookmarkStateChanged(isBookmarked: Boolean) {
                    // 响应书签状态变化
                }
            }
            
            val pdfHeader = PDFHeader(context, headerView, PDFHeaderConfig.default(), listener, stateListener)
        }
    }
}
