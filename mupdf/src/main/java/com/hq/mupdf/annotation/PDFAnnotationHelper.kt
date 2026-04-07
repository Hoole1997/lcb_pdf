package com.hq.mupdf.annotation

import android.util.Log
import com.artifex.mupdf.fitz.*
import com.hq.mupdf.viewer.MuPDFCore

/**
 * PDF注释辅助工具
 * 处理Document到PDFDocument的转换和注释相关操作
 */
object PDFAnnotationHelper {
    private const val TAG = "PDFAnnotationHelper"
    
    /**
     * 尝试从MuPDFCore获取PDFPage
     * @param core MuPDF核心实例
     * @param pageNumber 页码 (0-based)
     * @return PDFPage对象，如果失败返回null
     */
    fun getPDFPage(core: MuPDFCore, pageNumber: Int): PDFPage? {
        return try {
            Log.d(TAG, "🔍 开始获取PDFPage - 页码: $pageNumber")
            
            // 方法1: 尝试直接从MuPDFCore获取当前页面
            try {
                val pageField = MuPDFCore::class.java.getDeclaredField("page")
                pageField.isAccessible = true
                val currentPageNumberField = MuPDFCore::class.java.getDeclaredField("currentPage")
                currentPageNumberField.isAccessible = true
                
                val currentPageNumber = currentPageNumberField.getInt(core)
                Log.d(TAG, "🔍 MuPDFCore当前页码: $currentPageNumber, 请求页码: $pageNumber")
                
                if (currentPageNumber == pageNumber) {
                    val page = pageField.get(core) as? Page
                    if (page is PDFPage) {
                        Log.d(TAG, "✅ 成功从MuPDFCore获取当前PDFPage")
                        return page
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 无法从MuPDFCore获取当前页面: ${e.message}")
            }
            
            // 方法2: 通过反射获取Document对象并加载页面
            val docField = MuPDFCore::class.java.getDeclaredField("doc")
            docField.isAccessible = true
            val document = docField.get(core) as Document
            
            Log.d(TAG, "🔍 Document类型: ${document.javaClass.simpleName}")
            
            // 检查是否为PDF文档
            if (document is PDFDocument) {
                Log.d(TAG, "✅ 文档是PDFDocument，加载页面...")
                val page = document.loadPage(pageNumber)
                if (page is PDFPage) {
                    Log.d(TAG, "✅ 成功加载PDFPage")
                    return page
                } else {
                    Log.w(TAG, "⚠️ 加载的页面不是PDFPage类型: ${page.javaClass.simpleName}")
                }
            } else {
                Log.w(TAG, "⚠️ 文档不是PDFDocument类型，尝试转换...")
                // 尝试转换为PDFDocument
                val pdfDoc = document.asPDF()
                if (pdfDoc != null) {
                    Log.d(TAG, "✅ 成功转换为PDFDocument")
                    val page = pdfDoc.loadPage(pageNumber)
                    if (page is PDFPage) {
                        return page
                    }
                }
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取PDFPage失败", e)
            null
        }
    }
    
    /**
     * 尝试强制转换为PDFPage
     */
    private fun tryConvertToPDFPage(document: Document, pageNumber: Int): PDFPage? {
        return try {
            val page = document.loadPage(pageNumber)
            
            // 尝试类型转换
            if (page is PDFPage) {
                Log.d(TAG, "✅ 成功转换为PDFPage")
                page
            } else {
                Log.w(TAG, "⚠️ 页面不是PDFPage类型: ${page.javaClass.simpleName}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 转换PDFPage失败", e)
            null
        }
    }
    
    /**
     * 清除指定页面的所有注释
     * @param core MuPDF核心实例
     * @param pageNumber 页码 (0-based)
     * @return 清除的注释数量，失败返回-1
     */
    fun clearAllAnnotations(core: MuPDFCore, pageNumber: Int): Int {
        return try {
            Log.d(TAG, "🗑️ 开始清除页面 $pageNumber 的所有注释")
            
            val pdfPage = getPDFPage(core, pageNumber) ?: run {
                Log.w(TAG, "⚠️ 无法获取PDFPage，无法清除注释")
                return -1
            }
            
            // 获取所有注释
            val annotations = pdfPage.annotations
            if (annotations == null || annotations.isEmpty()) {
                Log.d(TAG, "📝 页面 $pageNumber 没有注释需要清除")
                return 0
            }
            
            val totalAnnotations = annotations.size
            Log.d(TAG, "📝 页面 $pageNumber 共有 $totalAnnotations 个注释需要清除")
            
            var deletedCount = 0
            // 删除所有注释
            for (annotation in annotations) {
                try {
                    pdfPage.deleteAnnotation(annotation)
                    deletedCount++
                    Log.d(TAG, "✅ 成功删除注释 $deletedCount/$totalAnnotations")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 删除注释失败: ${e.message}")
                }
            }
            
            // 更新页面
            try {
                val updateMethod = pdfPage.javaClass.getMethod("update")
                updateMethod.invoke(pdfPage)
                Log.d(TAG, "✅ 页面更新成功")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 页面更新失败: ${e.message}")
            }
            
            // 强制刷新页面显示
            try {
                core.forceRefreshPage(pageNumber)
                Log.d(TAG, "✅ 强制刷新页面成功")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 强制刷新页面失败: ${e.message}")
            }
            
            Log.d(TAG, "🎉 成功清除页面 $pageNumber 的 $deletedCount 个注释")
            deletedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清除注释失败", e)
            -1
        }
    }
    
    /**
     * 清除所有页面的注释
     * @param core MuPDF核心实例
     * @return 清除的总注释数量，失败返回-1
     */
    fun clearAllAnnotationsInDocument(core: MuPDFCore): Int {
        return try {
            Log.d(TAG, "🗑️ 开始清除文档中所有注释")
            
            val totalPages = core.countPages()
            var totalDeleted = 0
            
            for (pageNum in 0 until totalPages) {
                val deletedOnPage = clearAllAnnotations(core, pageNum)
                if (deletedOnPage >= 0) {
                    totalDeleted += deletedOnPage
                }
            }
            
            Log.d(TAG, "🎉 成功清除文档中总共 $totalDeleted 个注释")
            totalDeleted
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清除文档注释失败", e)
            -1
        }
    }
    
    /**
     * 检查文档是否支持注释
     */
    fun isAnnotationSupported(core: MuPDFCore): Boolean {
        return try {
            getPDFPage(core, 0) != null
        } catch (e: Exception) {
            Log.e(TAG, "检查注释支持失败", e)
            false
        }
    }
    
    /**
     * 创建文本注释的便捷方法
     */
    fun createTextAnnotation(
        core: MuPDFCore,
        pageNumber: Int,
        selectionResult: com.hq.mupdf.textselection.NativeTextSelector.SelectionResult,
        annotationType: Int
    ): PDFAnnotation? {
        val pdfPage = getPDFPage(core, pageNumber) ?: return null
        val annotationManager = AnnotationManagerV2() // 使用V2版本
        
        Log.d(TAG, "🎯 开始创建注释：类型=${annotationType}, 页码=${pageNumber}")
        
        // 创建前先查询页面注释数量
        val beforeCount = pdfPage.annotations?.size ?: 0
        Log.d(TAG, "📊 创建前页面注释数量: $beforeCount")
        
        val annotation = annotationManager.createTextAnnotationSimple(
            pdfPage,
            selectionResult,
            annotationType
        )
        
        if (annotation != null) {
            // 创建后再次查询注释数量
            val afterCount = pdfPage.annotations?.size ?: 0
            Log.d(TAG, "📊 创建后页面注释数量: $afterCount (增加: ${afterCount - beforeCount})")
            
            // 验证注释是否真的添加到页面
            val allAnnotations = pdfPage.annotations
            if (allAnnotations != null) {
                val newAnnotation = allAnnotations.find { it == annotation }
                if (newAnnotation != null) {
                    Log.d(TAG, "✅ 验证成功：注释已添加到页面")
                    Log.d(TAG, "✅ 注释详情：类型=${newAnnotation.type}, 边界=${newAnnotation.bounds}")
                    Log.d(TAG, "✅ 注释颜色：${newAnnotation.color?.joinToString(",") ?: "无"}")
                    Log.d(TAG, "✅ 注释透明度：${newAnnotation.opacity}")
                } else {
                    Log.w(TAG, "⚠️ 警告：注释创建成功但未在页面注释列表中找到")
                }
            } else {
                Log.w(TAG, "⚠️ 警告：无法获取页面注释列表进行验证")
            }
            
            // ⚡ 关键修复：确保注释完全提交后再刷新页面
            try {
                Log.d(TAG, "💾 注释已创建，等待提交完成...")
                
                // 强制刷新PDF文档状态，确保注释提交
                try {
                    // 如果page是PDFPage，尝试调用update方法
                    val updateMethod = pdfPage.javaClass.getMethod("update")
                    updateMethod.invoke(pdfPage)
                    Log.d(TAG, "📄 PDF页面状态已更新")
                } catch (e: Exception) {
                    Log.w(TAG, "无法调用页面update方法: ${e.message}")
                }
                
                // 延迟执行页面刷新，确保注释提交完成
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        Log.d(TAG, "🔄 开始强制重新加载页面对象...")
                        
                        // 使用增强的刷新方法，强制重新加载page对象
                        core.refreshPageForAnnotation(pageNumber)
                        
                        Log.d(TAG, "✅ 页面对象重新加载完成")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "页面刷新失败: ${e.message}")
                    }
                }, 50) // 50ms延迟确保注释提交完成
                
            } catch (e: Exception) {
                Log.e(TAG, "注释刷新处理失败: ${e.message}")
            }
        } else {
            Log.e(TAG, "❌ 注释创建失败")
        }
        
        return annotation
    }
    

    

}