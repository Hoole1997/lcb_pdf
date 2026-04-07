package com.documentpro.office.business.fileviewer.ui.clean

import com.blankj.utilcode.util.StringUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CleanItem(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val type: CleanType,
    val isSelected: Boolean = false
)

data class CleanGroup(
    val type: CleanType,
    val items: List<CleanItem> = emptyList(),
    val isAllSelected: Boolean = false
)

enum class CleanType(val title: String) {
    JUNK(StringUtils.getString(R.string.clean_junk_files)),
    OBSOLETE_APK(StringUtils.getString(R.string.clean_obsolete_apk)),
    TEMP(StringUtils.getString(R.string.clean_temp_files)),
    LOG(StringUtils.getString(R.string.clean_log_files))
}

class BusinessCleanModel : BaseModel() {
    private val _cleanGroups = MutableStateFlow<List<CleanGroup>>(emptyList())
    val cleanGroups: StateFlow<List<CleanGroup>> = _cleanGroups.asStateFlow()

    private val _totalSize = MutableStateFlow(0L)
    val totalSize: StateFlow<Long> = _totalSize.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress.asStateFlow()

    private val _scanningFolder = MutableStateFlow("")
    val scanningFolder: StateFlow<String> = _scanningFolder.asStateFlow()

    fun startScan() {
        _isScanning.value = true
        _scanProgress.value = 0
        // 初始化默认选中的组
        _cleanGroups.value = listOf(
            CleanGroup(CleanType.JUNK, isAllSelected = true),
            CleanGroup(CleanType.OBSOLETE_APK, isAllSelected = true),  // 默认选中
            CleanGroup(CleanType.TEMP, isAllSelected = true),          // 默认选中
            CleanGroup(CleanType.LOG, isAllSelected = true)            // 默认选中
        )
        // 重置总大小
        _totalSize.value = 0L
    }

    fun stopScan() {
        _isScanning.value = false
        _scanProgress.value = 100
    }

    fun updateScanProgress(progress: Int) {
        _scanProgress.value = progress
    }

    fun updateScanningFolder(folder: String) {
        _scanningFolder.value = folder
    }

    fun addCleanItem(item: CleanItem) {
        _cleanGroups.update { groups ->
            groups.map { group ->
                if (group.type == item.type) {
                    // 根据组的选中状态设置项目的选中状态
                    val isSelected = group.isAllSelected
                    val newItem = item.copy(isSelected = isSelected)
                    // 如果项目被选中，更新总大小
                    if (isSelected) {
                        _totalSize.update { it + item.size }
                    }
                    group.copy(items = group.items + newItem)
                } else {
                    group
                }
            }
        }
    }

    fun toggleGroupSelection(type: CleanType) {
        _cleanGroups.update { groups ->
            groups.map { group ->
                if (group.type == type) {
                    val newSelected = !group.isAllSelected
                    val newItems = group.items.map { it.copy(isSelected = newSelected) }
                    // 更新总大小
                    val sizeDiff = if (newSelected) {
                        newItems.sumOf { it.size }
                    } else {
                        -newItems.sumOf { it.size }
                    }
                    _totalSize.update { it + sizeDiff }
                    group.copy(
                        isAllSelected = newSelected,
                        items = newItems
                    )
                } else {
                    group
                }
            }
        }
    }

    fun toggleItemSelection(type: CleanType, id: Long) {
        _cleanGroups.update { groups ->
            groups.map { group ->
                if (group.type == type) {
                    val newItems = group.items.map { item ->
                        if (item.id == id) {
                            val newSelected = !item.isSelected
                            // 更新总大小
                            val sizeDiff = if (newSelected) item.size else -item.size
                            _totalSize.update { it + sizeDiff }
                            item.copy(isSelected = newSelected)
                        } else {
                            item
                        }
                    }
                    group.copy(
                        items = newItems,
                        isAllSelected = newItems.all { it.isSelected }
                    )
                } else {
                    group
                }
            }
        }
    }

    fun getSelectedItems(): List<CleanItem> {
        return _cleanGroups.value.flatMap { group ->
            group.items.filter { it.isSelected }
        }
    }

    private fun updateTotalSize() {
        val total = _cleanGroups.value.sumOf { group ->
            group.items.sumOf { it.size }
        }
        _totalSize.value = total
    }

    fun removeCleanItem(item: CleanItem) {
        val currentGroups = _cleanGroups.value.toMutableList()
        val groupIndex = currentGroups.indexOfFirst { it.type == item.type }
        if (groupIndex != -1) {
            val group = currentGroups[groupIndex]
            val updatedItems = group.items.filter { it.id != item.id }
            if (updatedItems.isEmpty()) {
                // 如果组内没有项目了，移除整个组
                currentGroups.removeAt(groupIndex)
            } else {
                // 更新组内的项目
                currentGroups[groupIndex] = group.copy(items = updatedItems)
            }
            _cleanGroups.value = currentGroups
            updateTotalSize()
        }
    }
}