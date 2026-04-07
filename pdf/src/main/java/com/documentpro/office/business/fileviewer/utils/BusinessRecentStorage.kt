package com.documentpro.office.business.fileviewer.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import java.lang.reflect.Type

/**
 * 文件存储工具类
 */
object BusinessRecentStorage {
    
    private const val KEY_RECENT_FILES = "recent_files"
    private const val KEY_FAVORITE_FILES = "favorite_files"
    private const val MAX_RECENT_FILES = 100 // 最大存储100个最近文件
    private const val MAX_FAVORITE_FILES = 100 // 最大存储100个收藏文件
    
    private val gson = Gson()
    
    /**
     * 添加文件到最近列表
     * @param fileInfo 文件信息
     */
    fun addRecentFile(fileInfo: BusinessFileInfo) {
        val currentList = getRecentFiles().toMutableList()
        
        // 移除已存在的相同文件（基于路径判断）
        currentList.removeAll { it.path == fileInfo.path }
        
        // 添加到列表头部
        currentList.add(0, fileInfo)
        
        // 限制最大数量
        if (currentList.size > MAX_RECENT_FILES) {
            currentList.subList(MAX_RECENT_FILES, currentList.size).clear()
        }
        
        // 保存到存储
        saveRecentFiles(currentList)
    }
    
    /**
     * 获取所有最近文件
     * @return 最近文件列表，按添加时间倒序
     */
    fun getRecentFiles(): List<BusinessFileInfo> {
        val jsonString = BusinessStorageUtils.getString(KEY_RECENT_FILES, "")
        if (jsonString.isEmpty()) {
            return emptyList()
        }
        
        return try {
            val type: Type = object : TypeToken<List<BusinessFileInfo>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 根据文件类型获取最近文件
     * @param fileType 文件类型，如 "PDF", "WORD" 等，"All" 表示所有类型
     * @return 过滤后的最近文件列表
     */
    fun getRecentFilesByType(fileType: String): List<BusinessFileInfo> {
        val allRecentFiles = getRecentFiles()
        
        if (fileType == "All") {
            return allRecentFiles
        }
        
        return allRecentFiles.filter { fileInfo ->
            com.documentpro.office.business.fileviewer.utils.queryfile.equalsFileType(fileInfo.type).name == fileType
        }
    }
    
    /**
     * 移除指定路径的最近文件
     * @param filePath 文件路径
     */
    fun removeRecentFile(filePath: String) {
        val currentList = getRecentFiles().toMutableList()
        currentList.removeAll { it.path == filePath }
        saveRecentFiles(currentList)
    }
    
    /**
     * 清空所有最近文件
     */
    fun clearRecentFiles() {
        BusinessStorageUtils.putString(KEY_RECENT_FILES, "")
    }
    
    /**
     * 检查文件是否存在于最近列表中
     * @param filePath 文件路径
     * @return 是否存在
     */
    fun isRecentFile(filePath: String): Boolean {
        return getRecentFiles().any { it.path == filePath }
    }
    
    /**
     * 保存最近文件列表到存储
     * @param recentFiles 最近文件列表
     */
    private fun saveRecentFiles(recentFiles: List<BusinessFileInfo>) {
        val jsonString = gson.toJson(recentFiles)
        BusinessStorageUtils.putString(KEY_RECENT_FILES, jsonString)
    }
    
    /**
     * 获取最近文件数量
     * @return 最近文件数量
     */
    fun getRecentFilesCount(): Int {
        return getRecentFiles().size
    }

    // ==================== 收藏文件相关方法 ====================

    /**
     * 添加文件到收藏列表
     * @param fileInfo 文件信息
     */
    fun addFavoriteFile(fileInfo: BusinessFileInfo) {
        val currentList = getFavoriteFiles().toMutableList()
        
        // 移除已存在的相同文件（基于路径判断）
        currentList.removeAll { it.path == fileInfo.path }
        
        // 添加到列表头部
        currentList.add(0, fileInfo)
        
        // 限制最大数量，超出则移除最老的
        if (currentList.size > MAX_FAVORITE_FILES) {
            currentList.subList(MAX_FAVORITE_FILES, currentList.size).clear()
        }
        
        // 保存到存储
        saveFavoriteFiles(currentList)
    }
    
    /**
     * 获取所有收藏文件
     * @return 收藏文件列表，按收藏时间倒序
     */
    fun getFavoriteFiles(): List<BusinessFileInfo> {
        val jsonString = BusinessStorageUtils.getString(KEY_FAVORITE_FILES, "")
        if (jsonString.isEmpty()) {
            return emptyList()
        }
        
        return try {
            val type: Type = object : TypeToken<List<BusinessFileInfo>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 根据文件类型获取收藏文件
     * @param fileType 文件类型，如 "PDF", "WORD" 等，"All" 表示所有类型
     * @return 过滤后的收藏文件列表
     */
    fun getFavoriteFilesByType(fileType: String): List<BusinessFileInfo> {
        val allFavoriteFiles = getFavoriteFiles()
        
        if (fileType == "All") {
            return allFavoriteFiles
        }
        
        return allFavoriteFiles.filter { fileInfo ->
            com.documentpro.office.business.fileviewer.utils.queryfile.equalsFileType(fileInfo.type).name == fileType
        }
    }
    
    /**
     * 移除指定路径的收藏文件
     * @param filePath 文件路径
     */
    fun removeFavoriteFile(filePath: String) {
        val currentList = getFavoriteFiles().toMutableList()
        currentList.removeAll { it.path == filePath }
        saveFavoriteFiles(currentList)
    }
    
    /**
     * 清空所有收藏文件
     */
    fun clearFavoriteFiles() {
        BusinessStorageUtils.putString(KEY_FAVORITE_FILES, "")
    }
    
    /**
     * 检查文件是否被收藏
     * @param filePath 文件路径
     * @return 是否被收藏
     */
    fun isFavoriteFile(filePath: String): Boolean {
        return getFavoriteFiles().any { it.path == filePath }
    }
    
    /**
     * 保存收藏文件列表到存储
     * @param favoriteFiles 收藏文件列表
     */
    private fun saveFavoriteFiles(favoriteFiles: List<BusinessFileInfo>) {
        val jsonString = gson.toJson(favoriteFiles)
        BusinessStorageUtils.putString(KEY_FAVORITE_FILES, jsonString)
    }
    
    /**
     * 获取收藏文件数量
     * @return 收藏文件数量
     */
    fun getFavoriteFilesCount(): Int {
        return getFavoriteFiles().size
    }
    
    // ==================== 文件锁状态更新方法 ====================
    
    /**
     * 更新最近文件列表中文件的锁定状态
     * @param filePath 文件路径
     * @param isLocked 新的锁定状态
     */
    fun updateRecentFileLockStatus(filePath: String, isLocked: Boolean) {
        val currentList = getRecentFiles().toMutableList()
        val fileIndex = currentList.indexOfFirst { it.path == filePath }
        
        if (fileIndex != -1) {
            // 创建新的文件对象（使用copy方法）
            val updatedFile = currentList[fileIndex].copy(isLocked = isLocked)
            currentList[fileIndex] = updatedFile
            saveRecentFiles(currentList)
        }
    }
    
    /**
     * 更新收藏文件列表中文件的锁定状态
     * @param filePath 文件路径
     * @param isLocked 新的锁定状态
     */
    fun updateFavoriteFileLockStatus(filePath: String, isLocked: Boolean) {
        val currentList = getFavoriteFiles().toMutableList()
        val fileIndex = currentList.indexOfFirst { it.path == filePath }
        
        if (fileIndex != -1) {
            // 创建新的文件对象（使用copy方法）
            val updatedFile = currentList[fileIndex].copy(isLocked = isLocked)
            currentList[fileIndex] = updatedFile
            saveFavoriteFiles(currentList)
        }
    }
    
    /**
     * 同时更新最近文件和收藏文件列表中文件的锁定状态
     * @param filePath 文件路径
     * @param isLocked 新的锁定状态
     */
    fun updateFileLockStatus(filePath: String, isLocked: Boolean) {
        updateRecentFileLockStatus(filePath, isLocked)
        updateFavoriteFileLockStatus(filePath, isLocked)
    }
}