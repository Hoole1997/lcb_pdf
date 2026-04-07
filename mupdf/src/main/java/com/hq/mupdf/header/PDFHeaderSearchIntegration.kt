package com.hq.mupdf.header

import android.app.Activity
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.viewer.ReaderView
import com.hq.mupdf.viewer.SearchTaskResult

/**
 * PDFHeader搜索功能集成示例
 * 展示如何在Activity中使用PDFHeader的现代化搜索功能
 */
class PDFHeaderSearchIntegration {
    
    companion object {
        
        /**
         * 在Activity中设置PDFHeader搜索功能
         * @param activity Activity实例
         * @param pdfHeader PDFHeader实例
         * @param muPDFCore MuPDFCore实例
         * @param readerView ReaderView实例（可选，用于更新搜索结果显示）
         */
        fun setupSearchIntegration(
            activity: Activity,
            pdfHeader: PDFHeader,
            muPDFCore: MuPDFCore,
            readerView: ReaderView? = null
        ) {
            // 设置PDF核心对象
            pdfHeader.setPDFCore(muPDFCore)
            
            // 设置搜索结果监听器
            pdfHeader.setOnSearchResultListener { result ->
                handleSearchResult(result, readerView)
            }
        }
        
        /**
         * 处理搜索结果
         * @param result 搜索结果
         * @param readerView ReaderView实例
         */
        private fun handleSearchResult(result: SearchTaskResult?, readerView: ReaderView?) {
            if (result != null) {
                // 跳转到搜索结果页面
                readerView?.setDisplayedViewIndex(result.pageNumber)
                
                // 重新设置子视图以显示搜索高亮
                readerView?.resetupChildren()
            } else {
                
                // 清除搜索高亮
                readerView?.resetupChildren()
            }
        }
        
        /**
         * 在Activity的onDestroy中清理搜索资源
         * @param pdfHeader PDFHeader实例
         */
        fun cleanupSearchIntegration(pdfHeader: PDFHeader) {
            pdfHeader.onDestroy()
        }
    }
}

/**
 * Activity中的使用示例
 */
/*
class ExamplePDFActivity : Activity() {
    private lateinit var pdfHeader: PDFHeader
    private lateinit var muPDFCore: MuPDFCore
    private lateinit var readerView: ReaderView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)
        
        // 初始化组件
        val headerView = findViewById<View>(R.id.pdfHeader)
        pdfHeader = PDFHeader(this, headerView, listener = object : PDFHeaderListener {
            override fun onBackButtonClick() {
                finish()
            }
            
            override fun onSearchButtonClick() {
                // 搜索功能已在PDFHeader内部处理
            }
            
            // 其他按钮事件...
        })
        
        // 初始化PDF核心和阅读器
        muPDFCore = // 初始化你的MuPDFCore
        readerView = // 初始化你的ReaderView
        
        // 设置搜索集成
        PDFHeaderSearchIntegration.setupSearchIntegration(
            this, 
            pdfHeader, 
            muPDFCore, 
            readerView
        )
        
        // 更新文档信息
        pdfHeader.updateDocumentInfo("示例文档.pdf", 1, 100)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        PDFHeaderSearchIntegration.cleanupSearchIntegration(pdfHeader)
    }
}
*/
