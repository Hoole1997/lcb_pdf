package com.documentpro.office.business.fileviewer.ui.recent

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.documentpro.office.business.fileviewer.base.BaseModel
import com.documentpro.office.business.fileviewer.utils.BusinessRecentStorage
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessSortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BusinessRecentModel : BaseModel() {
    
    val fileInfoEvent = MutableLiveData<List<BusinessFileInfo>>()
    val refreshChooseEvent = MutableLiveData<List<BusinessFileInfo>>()
    
    /**
     * 查询最近文件列表
     * @param queryType 查询类型，如 "All", "PDF", "WORD" 等
     * @param sortType 排序类型，支持按修改时间、文件名、文件大小排序
     */
    fun queryFileList(queryType: String, sortType: BusinessSortType) {
        viewModelScope.launch(Dispatchers.IO) {
            val recentFiles = BusinessRecentStorage.getRecentFilesByType(queryType)
            val sortedFiles = sortRecentFiles(recentFiles, sortType)
            fileInfoEvent.postValue(sortedFiles)
        }
    }

    /**
     * 对最近文件进行排序
     * @param files 文件列表
     * @param sortType 排序类型
     * @return 排序后的文件列表
     */
    private fun sortRecentFiles(files: List<BusinessFileInfo>, sortType: BusinessSortType): List<BusinessFileInfo> {
        return when (sortType) {
            BusinessSortType.MODIFIED_DESC -> files.sortedByDescending { it.dateModified }
            BusinessSortType.MODIFIED_ASC -> files.sortedBy { it.dateModified }
            BusinessSortType.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            BusinessSortType.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            BusinessSortType.SIZE_DESC -> files.sortedByDescending { it.size }
            BusinessSortType.SIZE_ASC -> files.sortedBy { it.size }
        }
    }
    
    /**
     * 刷新选择状态
     * @param list 文件列表
     */
    fun refreshChoose(list: List<BusinessFileInfo>) {
        refreshChooseEvent.postValue(list)
    }
    
    /**
     * 添加文件到最近列表
     * @param fileInfo 文件信息
     */
    fun addRecentFile(fileInfo: BusinessFileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            BusinessRecentStorage.addRecentFile(fileInfo)
        }
    }
    
    /**
     * 移除最近文件
     * @param filePath 文件路径
     */
    fun removeRecentFile(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            BusinessRecentStorage.removeRecentFile(filePath)
        }
    }
    
    /**
     * 清空所有最近文件
     */
    fun clearRecentFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            BusinessRecentStorage.clearRecentFiles()
        }
    }
}