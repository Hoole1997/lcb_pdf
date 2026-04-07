package com.documentpro.office.business.fileviewer.utils

/**
 * 闪屏页前台拉起控制器
 * 用于控制应用从后台返回前台时，是否拉起闪屏页
 * 使用临时变量，不做持久化存储
 */
object BusinessSplashForegroundController {

    // 是否标记下一次拦截
    private var shouldInterceptNext = false

    /**
     * 判断是否允许拉起闪屏页
     * @return true-允许拉起，false-拦截不拉起
     */
    fun shouldLaunchSplash(): Boolean {
        // 如果标记了下一次拦截，则拦截
        if (shouldInterceptNext) {
            shouldInterceptNext = false  // 自动清除标记
            return false
        }
        // 默认允许拉起
        return true
    }

    /**
     * 标记下一次拦截拉起
     * 调用此方法后，下一次从后台返回前台时将不会拉起闪屏页
     */
    fun markNextIntercept() {
        shouldInterceptNext = true
    }

    /**
     * 清除拦截标记
     */
    fun clearIntercept() {
        shouldInterceptNext = false
    }

    /**
     * 判断是否标记了拦截
     */
    fun isInterceptMarked(): Boolean {
        return shouldInterceptNext
    }
}

