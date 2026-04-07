package com.hq.mupdf.textselection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.artifex.mupdf.fitz.PDFPage
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.hq.mupdf.R
import com.hq.mupdf.annotation.PDFAnnotationHelper
import com.hq.mupdf.viewer.MuPDFCore
import android.widget.PopupWindow
import com.hq.mupdf.annotation.AnnotationManagerV2

/**
 * 现代化文本选择上下文菜单
 * 支持高亮、下划线、删除线注释功能
 */
class TextSelectionContextMenu(
    private val context: Context,
    private val pdfPage: PDFPage? = null,
    private val core: MuPDFCore? = null,
    private val pageNumber: Int = 0,
    private val onDismiss: (() -> Unit)? = null,
    private val onAnnotationCreated: (() -> Unit)? = null
) {
    private var popupWindow: PopupWindow? = null
    private val annotationManager = AnnotationManagerV2()
    
    companion object {
        private const val TAG = "TextSelectionMenu"
    }
    
    /**
     * 显示上下文菜单
     */
    fun show(
        anchorView: View, 
        selectionResult: NativeTextSelector.SelectionResult, 
        x: Float, 
        y: Float
    ) {
        dismiss() // 先关闭已有的菜单
        
        val menuView = LayoutInflater.from(context)
            .inflate(R.layout.text_selection_menu, null)
        
        setupButtons(menuView, selectionResult)
        setupPopupWindow(menuView, anchorView, x, y)
    }
    
    /**
     * 设置按钮事件
     */
    private fun setupButtons(
        menuView: View, 
        selectionResult: NativeTextSelector.SelectionResult
    ) {
        // 复制按钮
        menuView.findViewById<MaterialButton>(R.id.btn_copy).setOnClickListener {
            copyToClipboard(selectionResult.selectedText)
            dismiss()
        }
        
        // 高亮按钮
        menuView.findViewById<MaterialButton>(R.id.btn_highlight).setOnClickListener {
            createAnnotation(selectionResult, AnnotationManagerV2.TYPE_HIGHLIGHT, "高亮")
            dismiss()
        }
        
        // 下划线按钮
        menuView.findViewById<MaterialButton>(R.id.btn_underline).setOnClickListener {
            createAnnotation(selectionResult, AnnotationManagerV2.TYPE_UNDERLINE, "下划线")
            dismiss()
        }
        
        // 删除线按钮
        menuView.findViewById<MaterialButton>(R.id.btn_strikethrough).setOnClickListener {
            createAnnotation(selectionResult, AnnotationManagerV2.TYPE_STRIKETHROUGH, "删除线")
            dismiss()
        }
        
        // 清除注释按钮
        menuView.findViewById<MaterialButton>(R.id.btn_clear_annotation).setOnClickListener {
            clearAnnotations()
            dismiss()
        }
    }
    
    /**
     * 创建注释
     */
    private fun createAnnotation(
        selectionResult: NativeTextSelector.SelectionResult,
        annotationType: Int,
        typeName: String
    ) {
        Log.d(TAG, "🔍 开始创建${typeName}注释")
        Log.d(TAG, "🔍 选择结果 - 文本: '${selectionResult.selectedText}'")
        Log.d(TAG, "🔍 选择结果 - 边界: ${selectionResult.bounds}")
        Log.d(TAG, "🔍 选择结果 - 四边形数量: ${selectionResult.highlightQuads.size}")
        
        // 优先使用新的便捷方法，它会自动处理缓存刷新
        if (core != null) {
            try {
                Log.d(TAG, "🔍 使用PDFAnnotationHelper创建注释...")
                val annotation = PDFAnnotationHelper.createTextAnnotation(
                    core,
                    pageNumber,
                    selectionResult,
                    annotationType
                )
                
                if (annotation != null) {
                    showSuccessToast("已添加${typeName}注释")
                    Log.d(TAG, "✅ 成功创建${typeName}注释")
                    
                    // 通知UI层进行刷新
                    // 注意：核心的页面对象刷新已在PDFAnnotationHelper中处理
                    try {
                        onAnnotationCreated?.invoke()
                        Log.d(TAG, "📢 已通知UI层进行刷新")
                    } catch (e: Exception) {
                        Log.e(TAG, "通知UI刷新失败: ${e.message}")
                    }
                } else {
                    showErrorToast("添加${typeName}注释失败")
                    Log.e(TAG, "❌ 创建${typeName}注释失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "创建注释异常", e)
                showErrorToast("添加注释失败：${e.message}")
            }
        } else if (pdfPage != null) {

        } else {
            showErrorToast("无法添加注释：页面信息缺失")
            Log.w(TAG, "⚠️ PDF页面和MuPDFCore都为空，无法创建注释")
        }
    }
    
    /**
     * 设置PopupWindow
     */
    private fun setupPopupWindow(menuView: View, anchorView: View, x: Float, y: Float) {
        // 创建PopupWindow
        popupWindow = PopupWindow(
            menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isFocusable = true
            elevation = 12f
            
            setOnDismissListener {
                onDismiss?.invoke()
            }
        }
        
        // 计算最佳显示位置
        calculateAndShowPosition(anchorView, menuView, x, y)
    }
    
    /**
     * 智能定位算法
     */
    private fun calculateAndShowPosition(anchorView: View, menuView: View, x: Float, y: Float) {
        // 获取屏幕尺寸
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // 动态测量菜单尺寸
        menuView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val menuWidth = menuView.measuredWidth.takeIf { it > 0 } ?: 320
        val menuHeight = menuView.measuredHeight.takeIf { it > 0 } ?: 60
        
        // 获取锚点视图在屏幕上的位置
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        
        // 计算初始位置（尝试在选择区域上方居中显示）
        var popupX = (location[0] + x - menuWidth / 2).toInt()
        var popupY = (location[1] + y - menuHeight - 24).toInt() // 24dp间距
        
        // 智能边界检查和调整
        val margin = 16 // 16dp边距
        
        // 水平位置调整
        when {
            popupX < margin -> popupX = margin
            popupX + menuWidth > screenWidth - margin -> popupX = screenWidth - menuWidth - margin
        }
        
        // 垂直位置调整
        if (popupY < 100) { // 上方空间不足，显示在选择区域下方
            popupY = (location[1] + y + 24).toInt()
        }
        
        // 确保不超出底部
        if (popupY + menuHeight > screenHeight - 100) {
            popupY = screenHeight - menuHeight - 100
        }
        
        Log.d(TAG, "📍 菜单位置: x=$popupX, y=$popupY (尺寸: ${menuWidth}x${menuHeight})")
        
        try {
            popupWindow?.showAtLocation(anchorView, Gravity.NO_GRAVITY, popupX, popupY)
            
            // 添加入场动画
            val animation = AnimationUtils.loadAnimation(context, R.anim.menu_fade_in)
            menuView.startAnimation(animation)
            
            Log.d(TAG, "✅ 菜单显示成功，动画已启动")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 菜单显示失败", e)
            // 回退方案：简单Toast
            Toast.makeText(context, context.getString(R.string.menu_display_failed), Toast.LENGTH_SHORT).show()
            copyToClipboard("") // 占位文本
        }
    }
    
    /**
     * 复制文本到剪贴板
     */
    private fun copyToClipboard(text: String) {
        try {
            val formattedText = formatTextForClipboard(text)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("PDF文本", formattedText)
            clipboard.setPrimaryClip(clip)
            showSuccessToast("已复制")
            Log.d(TAG, "✅ 文本复制成功: ${text.take(20)}...")
        } catch (e: Exception) {
            Log.e(TAG, "复制失败", e)
            showErrorToast("复制失败")
        }
    }
    
    /**
     * 格式化文本以便复制
     */
    private fun formatTextForClipboard(text: String): String {
        return text
            .trim()
            .replace("\\s+".toRegex(), " ") // 规范化空白字符
            .replace("- ", "") // 移除单词拆分连字符
            .replace("\\n\\s*\\n".toRegex(), "\n\n") // 规范化段落分隔
            .replace("([.!?])([A-Z])".toRegex(), "$1 $2") // 确保句子间有适当空格
    }
    
    /**
     * 显示成功Toast
     */
    private fun showSuccessToast(message: String) {
//        val toast = Toast.makeText(context, "✅ $message", Toast.LENGTH_SHORT)
//        toast.show()
    }
    
    /**
     * 显示错误Toast
     */
    private fun showErrorToast(message: String) {
//        val toast = Toast.makeText(context, "❌ $message", Toast.LENGTH_SHORT)
//        toast.show()
    }
    
    /**
     * 清除注释
     */
    private fun clearAnnotations() {
        if (core == null) {
            showErrorToast("无法清除注释：页面信息缺失")
            Log.w(TAG, "⚠️ MuPDFCore为空，无法清除注释")
            return
        }
        
        try {
            Log.d(TAG, "🧹 开始清除当前页面注释，页码: $pageNumber")
            
            // 使用PDFAnnotationHelper清除当前页面的所有注释
            val clearedCount = PDFAnnotationHelper.clearAllAnnotations(core, pageNumber)
            
            if (clearedCount > 0) {
                showSuccessToast("已清除 $clearedCount 个注释")
                Log.d(TAG, "✅ 成功清除 $clearedCount 个注释")
                
                // 通知UI层进行刷新
                try {
                    onAnnotationCreated?.invoke()
                    Log.d(TAG, "📢 已通知UI层进行刷新")
                } catch (e: Exception) {
                    Log.e(TAG, "通知UI刷新失败: ${e.message}")
                }
            } else {
                showSuccessToast("当前页面没有注释")
                Log.d(TAG, "ℹ️ 当前页面没有找到注释")
            }
        } catch (e: Exception) {
            Log.e(TAG, "清除注释异常", e)
            showErrorToast("清除注释失败：${e.message}")
        }
    }
    
    /**
     * 关闭菜单
     */
    fun dismiss() {
        popupWindow?.contentView?.let { menuView ->
            try {
                // 添加退出动画
                val animation = AnimationUtils.loadAnimation(context, R.anim.menu_fade_out)
                animation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        popupWindow?.dismiss()
                        popupWindow = null
                    }
                })
                menuView.startAnimation(animation)
            } catch (e: Exception) {
                Log.e(TAG, "动画异常，直接关闭菜单", e)
                popupWindow?.dismiss()
                popupWindow = null
            }
        } ?: run {
            popupWindow?.dismiss()
            popupWindow = null
        }
    }
    
    /**
     * 是否正在显示
     */
    fun isShowing(): Boolean {
        return popupWindow?.isShowing == true
    }
}
