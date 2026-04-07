package com.documentpro.office.business.fileviewer.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileUtils
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseModel
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessSortType
import com.documentpro.office.business.fileviewer.utils.queryfile.queryExcelFiles
import com.documentpro.office.business.fileviewer.utils.queryfile.queryImageFiles
import com.documentpro.office.business.fileviewer.utils.queryfile.queryDemoFiles
import com.documentpro.office.business.fileviewer.utils.queryfile.queryPdfFiles
import com.documentpro.office.business.fileviewer.utils.queryfile.queryPptFiles
import com.documentpro.office.business.fileviewer.utils.queryfile.queryTextFiles
import com.documentpro.office.business.fileviewer.utils.queryfile.queryWordFiles
import com.documentpro.office.business.fileviewer.utils.BusinessRecentStorage
import com.documentpro.office.business.fileviewer.utils.BusinessPdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class BusinessHomeModel : BaseModel() {

    val fileInfoEvent = MutableLiveData<List<BusinessFileInfo>>()
    val refreshChooseEvent = MutableLiveData<List<BusinessFileInfo>>()

    fun queryFileList(queryType: String,sortType: BusinessSortType) {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                if (queryType == "All") {
                    // 并行查询所有文件类型，提升查询速度
                    val deferredResults = listOf(
                        async { queryPdfFiles(sortType) },
                        async { queryWordFiles(sortType) },
                        async { queryExcelFiles(sortType) },
                        async { queryPptFiles(sortType) },
                        async { queryTextFiles(sortType) },
                        async { queryImageFiles(sortType) }
                    )
                    val results = deferredResults.awaitAll()
                    val list = results.flatten()
                    fileInfoEvent.postValue(list)
                } else {
                    fileInfoEvent.postValue(
                        when (queryType) {
                            BusinessFileType.PDF.name -> queryPdfFiles(sortType)
                            BusinessFileType.WORD.name -> queryWordFiles(sortType)
                            BusinessFileType.EXCEL.name -> queryExcelFiles(sortType)
                            BusinessFileType.PPT.name -> queryPptFiles(sortType)
                            BusinessFileType.TXT.name -> queryTextFiles(sortType)
                            BusinessFileType.IMAGE.name -> queryImageFiles(sortType)
                            else -> null
                        }
                    )
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
            BusinessPointLog.logEvent("query_file_list_error",mapOf("error" to e.message.toString()))
        }
    }

    fun refreshChoose(list: List<BusinessFileInfo>) {
        refreshChooseEvent.postValue(list)
    }

    /**
     * 查询Demo文件列表（不需要存储权限）
     * 用于在未授权存储权限时显示引导
     * @param queryType 查询类型，如 "All", "PDF", "WORD" 等
     * @param sortType 排序类型
     */
    fun queryDemoFileList(queryType: String, sortType: BusinessSortType) {
        viewModelScope.launch(Dispatchers.IO) {
            val demoFiles = queryDemoFiles(queryType, sortType)
            fileInfoEvent.postValue(demoFiles)
        }
    }
    
    /**
     * 查询最近文件列表
     * @param queryType 查询类型，如 "All", "PDF", "WORD" 等
     * @param sortType 排序类型，支持按修改时间、文件名、文件大小排序
     */
    fun queryRecentFileList(queryType: String, sortType: BusinessSortType) {
        viewModelScope.launch(Dispatchers.IO) {
            val recentFiles = BusinessRecentStorage.getRecentFilesByType(queryType)
            val sortedFiles = sortRecentFiles(recentFiles, sortType)
            
            // 验证并更新PDF文件的加锁状态
            val validFiles = validateAndUpdatePdfLockStatus(sortedFiles, isRecent = true)
            
            fileInfoEvent.postValue(validFiles)
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
     * 查询收藏文件列表
     * @param queryType 查询类型，如 "All", "PDF", "WORD" 等
     * @param sortType 排序类型，支持按修改时间、文件名、文件大小排序
     */
    fun queryFavoriteFileList(queryType: String, sortType: BusinessSortType) {
        viewModelScope.launch(Dispatchers.IO) {
            val favoriteFiles = BusinessRecentStorage.getFavoriteFilesByType(queryType)
            val sortedFiles = sortFavoriteFiles(favoriteFiles, sortType)
            
            // 验证并更新PDF文件的加锁状态
            val validFiles = validateAndUpdatePdfLockStatus(sortedFiles, isRecent = false)
            
            fileInfoEvent.postValue(validFiles)
        }
    }

    /**
     * 对收藏文件进行排序
     * @param files 文件列表
     * @param sortType 排序类型
     * @return 排序后的文件列表
     */
    private fun sortFavoriteFiles(files: List<BusinessFileInfo>, sortType: BusinessSortType): List<BusinessFileInfo> {
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
     * 验证并更新文件列表中PDF文件的加锁状态
     * @param files 文件列表
     * @param isRecent 是否是最近文件列表
     * @return 验证后的文件列表
     */
    private fun validateAndUpdatePdfLockStatus(
        files: List<BusinessFileInfo>,
        isRecent: Boolean
    ): List<BusinessFileInfo> {
        return files.mapNotNull { fileInfo ->
            if (!FileUtils.isFileExists(fileInfo.path)) {
                null
            } else {
                // 如果是PDF文件，检查并更新实际的加锁状态
                if (fileInfo.type.equals(BusinessFileType.PDF.name)) {
                    try {
                        val actualLockStatus = BusinessPdfUtils.isPdfLocked(fileInfo.path)
                        if (actualLockStatus != fileInfo.isLocked) {
                            // 实际状态与存储状态不一致，更新存储
                            if (isRecent) {
                                BusinessRecentStorage.updateRecentFileLockStatus(fileInfo.path, actualLockStatus)
                            } else {
                                BusinessRecentStorage.updateFavoriteFileLockStatus(fileInfo.path, actualLockStatus)
                            }
                            fileInfo.copy(isLocked = actualLockStatus)
                        } else {
                            fileInfo
                        }
                    } catch (e: Exception) {
                        // 如果检查失败，返回原始文件信息
                        fileInfo
                    }
                } else {
                    fileInfo
                }
            }
        }
    }

}