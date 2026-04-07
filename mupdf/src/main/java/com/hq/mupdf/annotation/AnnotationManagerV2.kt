package com.hq.mupdf.annotation

import android.content.Context
import android.graphics.RectF
import android.util.Log
import com.artifex.mupdf.fitz.*
import com.hq.mupdf.textselection.NativeTextSelector
import com.hq.mupdf.R

/**
 * PDF注释管理器 V2
 * 专门解决注释边界计算问题的版本
 */
class AnnotationManagerV2 {
    
    companion object {
        private const val TAG = "AnnotationManagerV2"
        
        // 注释类型映射
        const val TYPE_HIGHLIGHT = PDFAnnotation.TYPE_HIGHLIGHT
        const val TYPE_UNDERLINE = PDFAnnotation.TYPE_UNDERLINE
        const val TYPE_STRIKETHROUGH = PDFAnnotation.TYPE_STRIKE_OUT
        
        // 默认颜色 (RGB浮点值 0.0-1.0)
        private val DEFAULT_HIGHLIGHT_COLOR = floatArrayOf(1.0f, 0.75f, 0.14f) // 黄色高亮
        private val DEFAULT_UNDERLINE_COLOR = floatArrayOf(0.23f, 0.51f, 0.96f) // 蓝色下划线
        private val DEFAULT_STRIKETHROUGH_COLOR = floatArrayOf(0.94f, 0.27f, 0.27f) // 红色删除线
        
        private const val DEFAULT_OPACITY = 0.7f
    }
    
    /**
     * 创建文本标记注释 - 使用矩形边界的简化版本
     */
    fun createTextAnnotationSimple(
        page: PDFPage,
        selectionResult: NativeTextSelector.SelectionResult,
        annotationType: Int,
        color: FloatArray? = null,
        opacity: Float = DEFAULT_OPACITY
    ): PDFAnnotation? {
        try {
            Log.d(TAG, "🎯 [V2] 创建注释 - 类型: $annotationType (${getAnnotationTypeName(null, annotationType)})")
            Log.d(TAG, "🎯 [V2] 页面信息: ${page.javaClass.simpleName}")
            
            // 创建注释
            val annotation = page.createAnnotation(annotationType)
            Log.d(TAG, "🎯 [V2] 注释对象创建成功: ${annotation.javaClass.simpleName}")
            
            // 设置颜色
            val annotationColor = color ?: getDefaultColor(annotationType)
            annotation.setColor(annotationColor)
            Log.d(TAG, "🎯 [V2] 设置颜色: [${annotationColor.joinToString(", ")}]")
            
            // 设置透明度
            annotation.setOpacity(opacity)
            Log.d(TAG, "🎯 [V2] 设置透明度: $opacity")
            
            // 🔥 修复策略：根据注释类型使用不同的边界设置策略
            if (selectionResult.highlightQuads.isNotEmpty()) {
                Log.d(TAG, "🎯 [V2] 设置四边形点数量: ${selectionResult.highlightQuads.size}")
                
                // 打印四边形详细信息
                selectionResult.highlightQuads.forEachIndexed { index, quad ->
                    Log.d(TAG, "🎯 [V2] 四边形$index: (${quad.ul_x}, ${quad.ul_y}) - (${quad.ur_x}, ${quad.ur_y}) - (${quad.lr_x}, ${quad.lr_y}) - (${quad.ll_x}, ${quad.ll_y})")
                }
                
                // 🚀 关键修复：验证QuadPoints数据并只设置QuadPoints
                // 验证QuadPoints数据的有效性
                var isValidQuadPoints = true
                selectionResult.highlightQuads.forEach { quad ->
                    if (quad.ul_x.isNaN() || quad.ul_y.isNaN() || 
                        quad.ur_x.isNaN() || quad.ur_y.isNaN() ||
                        quad.ll_x.isNaN() || quad.ll_y.isNaN() ||
                        quad.lr_x.isNaN() || quad.lr_y.isNaN()) {
                        isValidQuadPoints = false
                        Log.e(TAG, "❌ [V2] 发现无效的QuadPoints (NaN): $quad")
                    }
                    if (quad.ul_x.isInfinite() || quad.ul_y.isInfinite() || 
                        quad.ur_x.isInfinite() || quad.ur_y.isInfinite() ||
                        quad.ll_x.isInfinite() || quad.ll_y.isInfinite() ||
                        quad.lr_x.isInfinite() || quad.lr_y.isInfinite()) {
                        isValidQuadPoints = false
                        Log.e(TAG, "❌ [V2] 发现无限值QuadPoints: $quad")
                    }
                }
                
                if (!isValidQuadPoints) {
                    Log.e(TAG, "❌ [V2] QuadPoints数据无效，无法创建注释")
                    throw RuntimeException("QuadPoints数据包含无效值")
                }
                
                // 📝 MuPDF规则：文本标记注释(Highlight/Underline/StrikeThrough)的边界由QuadPoints自动计算
                // 不应该（也不能）手动设置setRect()
                annotation.setQuadPoints(selectionResult.highlightQuads)
                Log.d(TAG, "🎯 [V2] ${getAnnotationTypeName(null, annotationType)}注释-设置四边形点完成")
                
                // 📊 立即检查设置后的边界，看MuPDF是否正确计算了边界
                val boundsAfterSet = annotation.bounds
                Log.d(TAG, "🔍 [V2] 设置QuadPoints后的边界: $boundsAfterSet")
                
            } else {
                Log.w(TAG, "⚠️ [V2] 警告：选择结果中没有四边形点，无法创建文本标记注释")
                throw RuntimeException("文本标记注释必须有QuadPoints数据")
            }
            
            // 设置创建时间
            annotation.setCreationDate(java.util.Date())
            annotation.setModificationDate(java.util.Date())
            
            // 设置作者信息
            annotation.setAuthor("PDF Reader V2")
            
            // 检查注释的最终状态
            Log.d(TAG, "🎯 [V2] 注释最终状态:")
            Log.d(TAG, "  - 类型: ${annotation.type}")
            Log.d(TAG, "  - 颜色: [${annotation.color?.joinToString(", ") ?: "无"}]")
            Log.d(TAG, "  - 透明度: ${annotation.opacity}")
            Log.d(TAG, "  - 边界: ${annotation.bounds}")
            
            Log.d(TAG, "✅ [V2] 注释创建成功")
            return annotation
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [V2] 创建注释失败", e)
            Log.e(TAG, "❌ [V2] 错误详情: ${e.message}")
            Log.e(TAG, "❌ [V2] 错误堆栈: ${e.stackTraceToString()}")
            return null
        }
    }
    
    /**
     * 获取注释类型的默认颜色
     */
    private fun getDefaultColor(annotationType: Int): FloatArray {
        return when (annotationType) {
            TYPE_HIGHLIGHT -> DEFAULT_HIGHLIGHT_COLOR
            TYPE_UNDERLINE -> DEFAULT_UNDERLINE_COLOR
            TYPE_STRIKETHROUGH -> DEFAULT_STRIKETHROUGH_COLOR
            else -> DEFAULT_HIGHLIGHT_COLOR
        }
    }
    
    /**
     * 获取注释类型名称
     */
    private fun getAnnotationTypeName(context: Context?, annotationType: Int): String {
        return when (annotationType) {
            TYPE_HIGHLIGHT -> context?.getString(R.string.annotation_highlight) ?: "Highlight"
            TYPE_UNDERLINE -> context?.getString(R.string.annotation_underline) ?: "Underline"
            TYPE_STRIKETHROUGH -> context?.getString(R.string.annotation_strikethrough) ?: "Strikethrough"
            else -> context?.getString(R.string.annotation_unknown_type, annotationType) ?: "Unknown type ($annotationType)"
        }
    }
}