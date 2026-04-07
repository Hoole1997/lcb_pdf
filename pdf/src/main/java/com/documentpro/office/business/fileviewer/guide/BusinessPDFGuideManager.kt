package com.documentpro.office.business.fileviewer.guide

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import com.app.hubert.guide.NewbieGuide
import com.app.hubert.guide.core.Controller
import com.app.hubert.guide.listener.OnGuideChangedListener
import com.app.hubert.guide.model.GuidePage
import com.app.hubert.guide.model.HighLight
import com.documentpro.office.business.fileviewer.R

/**
 * PDF阅读器引导管理器
 * 负责管理PDF阅读器的用户引导流程
 * 
 * 主要功能：
 * - 首次使用引导
 * - 功能介绍和演示
 * - 用户偏好设置
 */
class BusinessPDFGuideManager private constructor(private val activity: Activity) {
    
    companion object {
        private const val TAG = "BusinessPDFGuideManager"
        private const val PREF_NAME = "pdf_guide_preferences"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_GUIDE_VERSION = "guide_version"
        private const val CURRENT_GUIDE_VERSION = 1
        
        /**
         * 创建引导管理器实例
         */
        fun create(activity: Activity): BusinessPDFGuideManager {
            return BusinessPDFGuideManager(activity)
        }
    }
    
    private val sharedPrefs: SharedPreferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * 检查是否需要显示引导
     */
    fun shouldShowGuide(): Boolean {
        val isFirstLaunch = sharedPrefs.getBoolean(KEY_FIRST_LAUNCH, true)
        val savedVersion = sharedPrefs.getInt(KEY_GUIDE_VERSION, 0)
        
        return isFirstLaunch || savedVersion < CURRENT_GUIDE_VERSION
    }
    
    /**
     * 标记引导已完成
     */
    fun markGuideCompleted() {
        sharedPrefs.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .putInt(KEY_GUIDE_VERSION, CURRENT_GUIDE_VERSION)
            .apply()
        
        Log.d(TAG, "引导已完成，版本: $CURRENT_GUIDE_VERSION")
    }
    
    /**
     * 重置引导状态（用于测试）
     */
    fun resetGuideState() {
        sharedPrefs.edit()
            .putBoolean(KEY_FIRST_LAUNCH, true)
            .putInt(KEY_GUIDE_VERSION, 0)
            .apply()
        
        Log.d(TAG, "引导状态已重置")
    }
    
    /**
     * 开始显示引导页面
     */
    fun startGuide(
        pdfViewerArea: View? = null,
        onGuideComplete: (() -> Unit)? = null
    ) {
        if (!shouldShowGuide()) {
            Log.d(TAG, "不需要显示引导")
            onGuideComplete?.invoke()
            return
        }
        
        Log.d(TAG, "开始显示PDF阅读器引导")
        
        try {
            // 创建引导页面序列
            val guideBuilder = NewbieGuide.with(activity)
                .setLabel("pdf_reader_guide")
                .alwaysShow(false) // 生产环境设为false
                .setOnGuideChangedListener(object : OnGuideChangedListener {
                    override fun onShowed(controller: Controller?) {
                        Log.d(TAG, "引导页显示")
                    }

                    override fun onRemoved(controller: Controller?) {
                        Log.d(TAG, "引导页关闭")
                        markGuideCompleted()
                        onGuideComplete?.invoke()
                    }
                })
            
            // 注释工具介绍
            pdfViewerArea?.let { viewer ->
                guideBuilder.addGuidePage(createAnnotationMenuPage(viewer))
            }
            
            // 显示引导
            guideBuilder.show()
            
        } catch (e: Exception) {
            Log.e(TAG, "显示引导时出错", e)
            // 出错时标记引导已完成，避免重复尝试
            markGuideCompleted()
            onGuideComplete?.invoke()
        }
    }
    
    /**
     * 创建注释工具介绍页面
     */
    private fun createAnnotationMenuPage(targetView: View): GuidePage {
        return GuidePage.newInstance()
            .setLayoutRes(R.layout.guide_pdf_annotation_menu, R.id.btn_next)
            .setEverywhereCancelable(true)
            .setBackgroundColor(0x80000000.toInt()) // 半透明黑色背景
    }
    
    /**
     * 强制显示引导（用于调试或用户手动触发）
     */
    fun forceShowGuide(
        pdfViewerArea: View? = null,
        onGuideComplete: (() -> Unit)? = null
    ) {
        resetGuideState()
        startGuide(pdfViewerArea, onGuideComplete)
    }
}