package com.documentpro.office.business.fileviewer.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.documentpro.office.business.fileviewer.base.BaseModel
import com.documentpro.office.business.fileviewer.utils.BusinessRecentStorage
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BusinessMainModel : BaseModel() {

    val refreshHomeModel = MutableLiveData<Boolean>()
    val refreshRecentModel = MutableLiveData<Boolean>()
    val refreshFavoriteModel = MutableLiveData<Boolean>()
    val refreshAllModel = MutableLiveData<Boolean>() // 刷新所有数据源
    val fileSystemChangedModel = MutableLiveData<Boolean>() // 文件系统变化
    val checkShowGuideScanLive = MutableLiveData<Boolean>()
    val clickScanButton = MutableLiveData<Boolean>()

    fun refreshFileScan() {
        refreshHomeModel.postValue(true)
    }

    fun refreshRecentFiles() {
        refreshRecentModel.postValue(true)
    }

    fun refreshFavoriteFiles() {
        refreshFavoriteModel.postValue(true)
    }

    /**
     * 刷新所有数据源（用于权限恢复、应用切换等场景）
     */
    fun refreshAllDataSources() {
        refreshAllModel.postValue(true)
    }

    /**
     * 文件系统发生变化（删除、重命名、新增文件等）
     * 需要同时刷新文件系统和最近文件列表
     */
    fun notifyFileSystemChanged() {
        fileSystemChangedModel.postValue(true)
        // 文件系统变化可能影响最近文件的有效性
        refreshRecentModel.postValue(true)
    }

    /**
     * 添加文件到最近列表并刷新页面
     * @param fileInfo 文件信息
     */
    fun addToRecentAndRefresh(fileInfo: BusinessFileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                BusinessRecentStorage.addRecentFile(fileInfo)
                // 在主线程中通知刷新
                refreshRecentModel.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 添加文件到收藏列表并刷新页面
     * @param fileInfo 文件信息
     */
    fun addToFavoriteAndRefresh(fileInfo: BusinessFileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                BusinessRecentStorage.addFavoriteFile(fileInfo)
                // 在主线程中通知刷新
                refreshFavoriteModel.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 从收藏列表移除文件并刷新页面
     * @param filePath 文件路径
     */
    fun removeFromFavoriteAndRefresh(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                BusinessRecentStorage.removeFavoriteFile(filePath)
                // 在主线程中通知刷新
                refreshFavoriteModel.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 切换文件收藏状态并刷新页面
     * @param fileInfo 文件信息
     * @return 切换后的收藏状态（true=已收藏，false=已取消收藏）
     */
    fun toggleFavoriteAndRefresh(fileInfo: BusinessFileInfo): Boolean {
        return if (BusinessRecentStorage.isFavoriteFile(fileInfo.path)) {
            removeFromFavoriteAndRefresh(fileInfo.path)
            false
        } else {
            addToFavoriteAndRefresh(fileInfo)
            true
        }
    }

    fun checkShowGuideScan() {
        checkShowGuideScanLive.postValue(true)
    }

    fun clickScanButton() {
        clickScanButton.postValue(true)
    }

    var onNotificationOpenImage2PDF :(()->Unit)?=null
    var onNotificationEncryptPDF :(()->Unit)?=null
    var onNotificationMergePDF :(()->Unit)?=null
    var onNotificationSplitPDF :(()->Unit)?=null
    var onNotificationDecryptPDF :(()->Unit)?=null
    var onNotificationJunkCLeaner :(()->Unit)?=null
    var onNotificationProcessManager :(()->Unit)?=null

}