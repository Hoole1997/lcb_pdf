package com.documentpro.office.business.fileviewer.ui.pdf.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hq.mupdf.utils.MuPDFUtils
import com.documentpro.office.business.fileviewer.base.BaseModel
import com.documentpro.office.business.fileviewer.utils.BusinessPdfUtils
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PDF页面网格展示ViewModel
 */
class BusinessPdfPageGridModel() : BaseModel() {

    private val _pdfPagesEvent = MutableLiveData<List<BusinessPdfPageInfo>>()
    val pdfPagesEvent: LiveData<List<BusinessPdfPageInfo>> = _pdfPagesEvent

    private val _mergeResultEvent = MutableLiveData<BusinessFileInfo?>()
    val mergeResultEvent: LiveData<BusinessFileInfo?> = _mergeResultEvent

    private val _splitResultEvent = MutableLiveData<Boolean>()
    val splitResultEvent: LiveData<Boolean> = _splitResultEvent

    private val _loadingEvent = MutableLiveData<Boolean>()
    val loadingEvent: LiveData<Boolean> = _loadingEvent

    /**
     * 加载PDF页面数据
     * @param fileList PDF文件列表
     * @param defaultSelected 是否默认选中所有页面
     */
    fun loadPdfPages(fileList: List<BusinessFileInfo>, defaultSelected: Boolean) {
        viewModelScope.launch {
            _loadingEvent.value = true
            
            try {
                val allPages = mutableListOf<BusinessPdfPageInfo>()
                
                withContext(Dispatchers.IO) {
                    fileList.forEach { fileInfo ->
                        val pageCount = BusinessPdfUtils.getPdfPageCount(fileInfo.path)
                        for (pageIndex in 0 until pageCount) {
                            val pageInfo = BusinessPdfPageInfo(
                                filePath = fileInfo.path,
                                fileName = fileInfo.name,
                                pageIndex = pageIndex,
                                pageNumber = pageIndex + 1,
                                isSelected = defaultSelected
                            )
                            allPages.add(pageInfo)
                        }
                    }
                    
                    // 生成缩略图
                    allPages.forEach { pageInfo ->
                        try {
                            pageInfo.thumbnail = BusinessPdfUtils.generatePageThumbnail(
                                pageInfo.filePath,
                                pageInfo.pageIndex,
                                400, // 缩略图宽度
                                560  // 缩略图高度
                            )
                        } catch (e: Exception) {
                            // 缩略图生成失败，使用默认图片
                            pageInfo.thumbnail = null
                        }
                    }
                }
                
                _pdfPagesEvent.value = allPages
            } catch (e: Exception) {
                // 加载失败，返回空列表
                _pdfPagesEvent.value = emptyList()
            } finally {
                _loadingEvent.value = false
            }
        }
    }

    /**
     * 合并PDF页面
     */
    fun mergePdfPages(outputFileName: String, selectedPages: List<BusinessPdfPageInfo>) {
        viewModelScope.launch {
            _loadingEvent.value = true
            
            try {
                val result = withContext(Dispatchers.IO) {
                    BusinessPdfUtils.mergePdfPages(outputFileName, selectedPages)
                }
                _mergeResultEvent.value = result
            } catch (e: Exception) {
                _mergeResultEvent.value = null
            } finally {
                _loadingEvent.value = false
            }
        }
    }

    /**
     * 拆分PDF页面
     */
    fun splitPdfPages(selectedPages: List<BusinessPdfPageInfo>) {
        viewModelScope.launch {
            _loadingEvent.value = true
            
            try {
                val result = withContext(Dispatchers.IO) {
                    BusinessPdfUtils.splitPdfPages(selectedPages)
                }
                _splitResultEvent.value = result
            } catch (e: Exception) {
                _splitResultEvent.value = false
            } finally {
                _loadingEvent.value = false
            }
        }
    }
}