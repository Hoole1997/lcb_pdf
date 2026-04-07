package com.documentpro.office.business.fileviewer.utils

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel

/**
 * 统一的刷新管理器
 * 负责协调不同数据源的刷新逻辑，避免重复刷新和生命周期问题
 */
class BusinessRefreshManager(
    private val lifecycleOwner: LifecycleOwner,
    private val mainModel: BusinessMainModel,
    private val config: RefreshConfig
) {
    
    companion object {
        private const val TAG = "BusinessRefreshManager"
    }
    
    data class RefreshConfig(
        val dataSource: Int, // DATA_SOURCE_FILE_SYSTEM, DATA_SOURCE_RECENT, DATA_SOURCE_FAVORITE
        val onRefresh: () -> Unit,
        val enableFileSystemRefresh: Boolean = true,
        val enableRecentRefresh: Boolean = true,
        val enableFavoriteRefresh: Boolean = true,
        val enableAllRefresh: Boolean = true
    )
    
    private var isObserving = false
    
    fun startObserving() {
        if (isObserving) return
        isObserving = true
        
        Log.d(TAG, "开始监听刷新事件，数据源: ${config.dataSource}")
        
        // 根据数据源监听对应的刷新事件
        when (config.dataSource) {
            0 -> { // DATA_SOURCE_FILE_SYSTEM
                if (config.enableFileSystemRefresh) {
                    mainModel.refreshHomeModel.observe(lifecycleOwner, refreshObserver)
                }
                if (config.enableFileSystemRefresh) {
                    mainModel.fileSystemChangedModel.observe(lifecycleOwner, refreshObserver)
                }
            }
            1 -> { // DATA_SOURCE_RECENT
                if (config.enableRecentRefresh) {
                    mainModel.refreshRecentModel.observe(lifecycleOwner, refreshObserver)
                }
            }
            2 -> { // DATA_SOURCE_FAVORITE
                if (config.enableFavoriteRefresh) {
                    mainModel.refreshFavoriteModel.observe(lifecycleOwner, refreshObserver)
                }
            }
        }
        
        // 所有数据源都监听全局刷新事件
        if (config.enableAllRefresh) {
            mainModel.refreshAllModel.observe(lifecycleOwner, refreshObserver)
        }
    }
    
    private val refreshObserver = Observer<Boolean> { shouldRefresh ->
        if (shouldRefresh == true) {
            Log.d(TAG, "触发刷新，数据源: ${config.dataSource}")
            config.onRefresh()
        }
    }
    
    fun stopObserving() {
        if (!isObserving) return
        isObserving = false
        
        Log.d(TAG, "停止监听刷新事件，数据源: ${config.dataSource}")
        
        mainModel.refreshHomeModel.removeObserver(refreshObserver)
        mainModel.refreshRecentModel.removeObserver(refreshObserver)
        mainModel.refreshFavoriteModel.removeObserver(refreshObserver)
        mainModel.refreshAllModel.removeObserver(refreshObserver)
        mainModel.fileSystemChangedModel.removeObserver(refreshObserver)
    }
}


