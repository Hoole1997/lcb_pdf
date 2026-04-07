package com.documentpro.office.business.fileviewer.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.runCatching

/**
 * 存储权限检测器
 * 在onResume时循环检测存储权限，每次延迟200ms，直到满足条件为止
 * 
 * @param lifecycleOwner LifecycleOwner对象，用于监听生命周期
 * @param permissionChecker 权限检查的闭包函数
 * @param onPermissionGranted 权限满足时的回调
 */
class BusinessPermissionDetector(
    private val lifecycleOwner: LifecycleOwner,
    private val permissionChecker: () -> Boolean,
    private val onPermissionGranted: () -> Unit
) : LifecycleEventObserver {
    
    private var detectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
    
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                if(!permissionChecker.invoke()){
                    startPermissionDetection()
                }
            }
            Lifecycle.Event.ON_RESUME -> {

            }
            Lifecycle.Event.ON_PAUSE -> {

            }
            Lifecycle.Event.ON_DESTROY -> {
                cleanup()
            }
            else -> {
                // 其他生命周期事件不处理
            }
        }
    }
    
    /**
     * 开始权限检测
     */
    private fun startPermissionDetection() {
        // 如果已经有检测任务在运行，先取消
        stopPermissionDetection()
        
        detectionJob = scope.launch {
            runCatching {
                while (isActive) {
                    // 使用传入的权限检查闭包
                    if (permissionChecker.invoke()) {
                        // 权限满足，执行回调
                        onPermissionGranted.invoke()
                        break
                    }

                    // 延迟200ms后继续检测
                    delay(200L)
                }
            }
        }
    }
    
    /**
     * 停止权限检测
     */
    private fun stopPermissionDetection() {
        detectionJob?.cancel()
        detectionJob = null
    }
    
    /**
     * 清理资源
     */
    private fun cleanup() {
        stopPermissionDetection()
        lifecycleOwner.lifecycle.removeObserver(this)
        scope.cancel()
    }
    
    /**
     * 手动停止检测器
     */
    fun stop() {
        cleanup()
    }
    
    companion object {
        /**
         * 创建存储权限检测器的便捷方法
         * 
         * @param lifecycleOwner LifecycleOwner对象
         * @param permissionChecker 权限检查的闭包函数
         * @param onPermissionGranted 权限满足时的回调
         * @return StoragePermissionDetector实例
         */
        fun create(
            lifecycleOwner: LifecycleOwner,
            permissionChecker: () -> Boolean,
            onPermissionGranted: () -> Unit
        ): BusinessPermissionDetector {
            return BusinessPermissionDetector(
                lifecycleOwner,
                permissionChecker,
                onPermissionGranted
            )
        }
    }
}
