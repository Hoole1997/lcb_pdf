package com.documentpro.office.business.fileviewer.utils

import android.content.Context
import net.corekit.core.ext.DataStoreBoolDelegate

/**
 * 首次引导提示回调控制器（单例）
 *
 * 用于处理以下两种场景的回调：
 * 1. 用户点击"跳过"按钮
 * 2. 用户通过引导打开 BusinessDocumentActivity 后关闭页面
 */
object BusinessGuideCallbackController {

    private const val NEWBIE_GUIDE_SP_NAME = "NewbieGuide"
    private const val GUIDE_LABEL = "guide1"

    /** 引导是否已展示过，使用DataStoreBoolDelegate持久化 */
    private var _hasGuideShown by DataStoreBoolDelegate("business_guide_shown", false)

    /**
     * 回调监听器接口
     */
    interface GuideCallbackListener {
        /**
         * 引导完成回调
         * @param completionType 完成类型
         */
        fun onGuideCompleted(completionType: GuideCompletionType)
    }

    /**
     * 引导完成类型
     */
    enum class GuideCompletionType {
        /** 用户点击跳过 */
        SKIPPED,
        /** 用户通过引导打开文档后关闭 */
        DOCUMENT_CLOSED
    }

    private var listener: GuideCallbackListener? = null

    /** 标记是否是从引导触发的文档打开 */
    private var isOpenedFromGuide: Boolean = false

    /**
     * 设置回调监听器
     * @param listener 监听器实例
     */
    fun setListener(listener: GuideCallbackListener?) {
        this.listener = listener
    }

    /**
     * 移除回调监听器
     */
    fun removeListener() {
        this.listener = null
    }

    /**
     * 标记从引导打开文档
     * 在通过引导点击打开 BusinessDocumentActivity 前调用
     */
    fun markOpenedFromGuide() {
        isOpenedFromGuide = true
    }

    /**
     * 检查是否是从引导打开的
     */
    fun isFromGuide(): Boolean = isOpenedFromGuide

    /**
     * 通知跳过引导
     * 在用户点击"跳过"按钮时调用
     */
    fun notifySkipped() {
        _hasGuideShown = true
        listener?.onGuideCompleted(GuideCompletionType.SKIPPED)
    }

    /**
     * 通知文档关闭
     * 在 BusinessDocumentActivity 关闭时调用（仅当是从引导打开时）
     */
    fun notifyDocumentClosed() {
        if (isOpenedFromGuide) {
            isOpenedFromGuide = false
            _hasGuideShown = true
            listener?.onGuideCompleted(GuideCompletionType.DOCUMENT_CLOSED)
        }
    }

    /**
     * 重置状态
     */
    fun reset() {
        isOpenedFromGuide = false
    }

    /**
     * 检查引导是否已展示过
     * @param context 上下文（保留参数以兼容现有调用）
     * @return true 表示已展示过，false 表示未展示
     */
    fun hasGuideShown(context: Context): Boolean {
        return _hasGuideShown
    }
}
