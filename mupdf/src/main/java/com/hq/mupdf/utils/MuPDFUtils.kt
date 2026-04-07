package com.hq.mupdf.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hq.mupdf.save.PDFSaveConfig
import com.hq.mupdf.save.PDFSaveResult
import com.hq.mupdf.viewer.MuPDFCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF文档信息包装类
 * 简化参数传递和信息管理
 */
data class PDFDocumentInfo(
    val fileName: String,
    val filePath: String? = null,
    val fileUri: Uri? = null,
    val fileSize: Long = -1L,
    val isUriDocument: Boolean = fileUri != null
) {
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String = fileName.takeIf { it.isNotBlank() } ?: "document.pdf"
    
    /**
     * 检查是否为本地文件
     */
    fun isLocalFile(): Boolean = !filePath.isNullOrBlank()
    
    /**
     * 获取文件扩展名
     */
    fun getFileExtension(): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }
}

/**
 * MuPDF工具类
 * 提供PDF文档操作的静态方法，包括保存、压缩、优化等功能
 */
object MuPDFUtils {
    
    private const val TAG = "MuPDFUtils"
    
    /**
     * 保存PDF文档到指定路径
     * @param context 上下文
     * @param muPDFCore MuPDF核心对象
     * @param outputPath 输出文件路径
     * @param config 保存配置，默认使用标准配置
     * @return 保存结果
     */
    suspend fun savePDF(
        context: Context,
        muPDFCore: MuPDFCore?,
        outputPath: String,
        config: PDFSaveConfig = PDFSaveConfig.default()
    ): PDFSaveResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            
            try {
                val core = muPDFCore ?: return@withContext PDFSaveResult(
                    success = false,
                    errorMessage = "MuPDF核心对象为空"
                )
                
                // 获取Document对象
                val document = core.getDoc()
                
                // 确保目标目录存在
                val targetFile = File(outputPath)
                targetFile.parentFile?.mkdirs()
                
                // 尝试转换为PDFDocument以使用完整的保存功能
                val pdfDocument = try {
                    document.asPDF()
                } catch (e: Exception) {
                    null
                }
                
                var usedIncremental = false
                val optionsString = config.buildOptionsString()
                
                if (pdfDocument != null) {
                    // 检查是否可以使用增量保存
                    if (config.hasIncrementalOption() && pdfDocument.canBeSavedIncrementally()) {
                        pdfDocument.save(outputPath, "incremental")
                        usedIncremental = true
                    } else {
                        pdfDocument.save(outputPath, optionsString)
                    }
                }
                
                // 获取保存后的文件大小
                val newSize = targetFile.length()
                val saveTime = System.currentTimeMillis() - startTime
                
                Log.i(TAG, "PDF保存成功: $outputPath (${saveTime}ms)")
                
                PDFSaveResult(
                    success = true,
                    outputPath = outputPath,
                    fileSize = newSize,
                    usedIncremental = usedIncremental,
                    originalSize = -1L // 原始大小需要从外部传入
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "PDF保存失败: ${e.message}", e)
                PDFSaveResult(
                    success = false,
                    errorMessage = "保存失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 保存PDF文档（带原始大小信息）
     * @param context 上下文
     * @param muPDFCore MuPDF核心对象
     * @param outputPath 输出文件路径
     * @param originalPath 原始文件路径（用于获取原始大小）
     * @param config 保存配置
     * @return 保存结果
     */
    suspend fun savePDFWithOriginalSize(
        context: Context,
        muPDFCore: MuPDFCore?,
        outputPath: String,
        originalPath: String?,
        config: PDFSaveConfig = PDFSaveConfig.default()
    ): PDFSaveResult {
        return withContext(Dispatchers.IO) {
            // 获取原始文件大小
            val originalSize = originalPath?.let { path ->
                try {
                    File(path).length()
                } catch (e: Exception) {
                    -1L
                }
            } ?: -1L
            
            // 执行保存操作
            val result = savePDF(context, muPDFCore, outputPath, config)
            
            // 返回带有原始大小信息的结果
            result.copy(originalSize = originalSize)
        }
    }
    
    /**
     * 保存PDF文档（从URI源）
     * @param context 上下文
     * @param muPDFCore MuPDF核心对象
     * @param outputPath 输出文件路径
     * @param sourceUri 源文件URI（用于获取原始大小）
     * @param config 保存配置
     * @return 保存结果
     */
    suspend fun savePDFFromUri(
        context: Context,
        muPDFCore: MuPDFCore?,
        outputPath: String,
        sourceUri: Uri?,
        config: PDFSaveConfig = PDFSaveConfig.default()
    ): PDFSaveResult {
        return withContext(Dispatchers.IO) {
            // 获取原始文件大小
            val originalSize = sourceUri?.let { uri ->
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { 
                        it.statSize 
                    } ?: -1L
                } catch (e: Exception) {
                    -1L
                }
            } ?: -1L
            
            // 执行保存操作
            val result = savePDF(context, muPDFCore, outputPath, config)
            
            // 返回带有原始大小信息的结果
            result.copy(originalSize = originalSize)
        }
    }
    
    /**
     * 检查PDF文档是否可以进行增量保存
     * @param muPDFCore MuPDF核心对象
     * @return 是否支持增量保存
     */
    fun canBeSavedIncrementally(muPDFCore: MuPDFCore?): Boolean {
        return try {
            val core = muPDFCore ?: return false
            val document = core.getDoc()
            val pdfDocument = document.asPDF()
            pdfDocument.canBeSavedIncrementally()
        } catch (e: Exception) {
            Log.w(TAG, "检查增量保存支持时出错: ${e.message}")
            false
        }
    }
    
    /**
     * 复制文件作为备用保存方法（当MuPDF保存失败时使用）
     * @param context 上下文
     * @param sourcePath 源文件路径
     * @param outputPath 输出文件路径
     * @return 保存结果
     */
    suspend fun copyFileAsFallback(
        context: Context,
        sourcePath: String?,
        outputPath: String
    ): PDFSaveResult {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(sourcePath ?: return@withContext PDFSaveResult(
                    success = false,
                    errorMessage = "源文件路径为空"
                ))
                
                if (!sourceFile.exists()) {
                    return@withContext PDFSaveResult(
                        success = false,
                        errorMessage = "源文件不存在"
                    )
                }
                
                val targetFile = File(outputPath)
                targetFile.parentFile?.mkdirs()
                
                val originalSize = sourceFile.length()
                sourceFile.copyTo(targetFile, overwrite = true)
                val newSize = targetFile.length()
                
                Log.i(TAG, "文件复制保存成功: $outputPath")
                
                PDFSaveResult(
                    success = true,
                    outputPath = outputPath,
                    fileSize = newSize,
                    usedIncremental = false,
                    originalSize = originalSize
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "文件复制保存失败: ${e.message}", e)
                PDFSaveResult(
                    success = false,
                    errorMessage = "复制保存失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 从URI复制文件作为备用保存方法
     * @param context 上下文
     * @param sourceUri 源文件URI
     * @param outputPath 输出文件路径
     * @return 保存结果
     */
    suspend fun copyUriAsFallback(
        context: Context,
        sourceUri: Uri?,
        outputPath: String
    ): PDFSaveResult {
        return withContext(Dispatchers.IO) {
            try {
                val uri = sourceUri ?: return@withContext PDFSaveResult(
                    success = false,
                    errorMessage = "源URI为空"
                )
                
                val targetFile = File(outputPath)
                targetFile.parentFile?.mkdirs()
                
                // 获取原始大小
                val originalSize = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { 
                        it.statSize 
                    } ?: -1L
                } catch (e: Exception) { -1L }
                
                // 复制文件
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    targetFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                val newSize = targetFile.length()
                
                Log.i(TAG, "URI复制保存成功: $outputPath")
                
                PDFSaveResult(
                    success = true,
                    outputPath = outputPath,
                    fileSize = newSize,
                    usedIncremental = false,
                    originalSize = originalSize
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "URI复制保存失败: ${e.message}", e)
                PDFSaveResult(
                    success = false,
                    errorMessage = "URI复制保存失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 智能保存PDF文档（自动选择最佳保存方式）
     * @param context 上下文
     * @param muPDFCore MuPDF核心对象
     * @param outputPath 输出文件路径
     * @param sourcePath 源文件路径（可选，用于备用保存）
     * @param sourceUri 源文件URI（可选，用于备用保存）
     * @param config 保存配置
     * @return 保存结果
     */
    suspend fun smartSavePDF(
        context: Context,
        muPDFCore: MuPDFCore?,
        outputPath: String,
        sourcePath: String? = null,
        sourceUri: Uri? = null,
        config: PDFSaveConfig = PDFSaveConfig.default()
    ): PDFSaveResult {
        return withContext(Dispatchers.IO) {
            // 首先尝试使用MuPDF保存
            val muPDFResult = when {
                sourcePath != null -> savePDFWithOriginalSize(context, muPDFCore, outputPath, sourcePath, config)
                sourceUri != null -> savePDFFromUri(context, muPDFCore, outputPath, sourceUri, config)
                else -> savePDF(context, muPDFCore, outputPath, config)
            }
            
            // 如果MuPDF保存成功，直接返回结果
            if (muPDFResult.success) {
                return@withContext muPDFResult
            }
            
            Log.w(TAG, "MuPDF保存失败，尝试备用保存方法: ${muPDFResult.errorMessage}")
            
            // 如果MuPDF保存失败，尝试使用备用方法
            val fallbackResult = when {
                sourcePath != null -> copyFileAsFallback(context, sourcePath, outputPath)
                sourceUri != null -> copyUriAsFallback(context, sourceUri, outputPath)
                else -> PDFSaveResult(
                    success = false,
                    errorMessage = "MuPDF保存失败且无备用源: ${muPDFResult.errorMessage}"
                )
            }
            
            if (fallbackResult.success) {
                Log.i(TAG, "备用保存方法成功")
            } else {
                Log.e(TAG, "所有保存方法均失败")
            }
            
            fallbackResult
        }
    }
    
    /**
     * 获取建议的保存配置
     * @param fileName 文件名（用于判断保存意图）
     * @param fileSize 文件大小（用于选择最佳配置）
     * @param canUseIncremental 是否可以使用增量保存
     * @return 建议的保存配置
     */
    fun getSuggestedSaveConfig(
        fileName: String,
        fileSize: Long = -1L,
        canUseIncremental: Boolean = false
    ): PDFSaveConfig {
        return when {
            // 文件名包含压缩相关关键词
            fileName.contains("compressed", ignoreCase = true) || 
            fileName.contains("compact", ignoreCase = true) -> PDFSaveConfig.minimumSize()
            
            // 文件名包含web相关关键词
            fileName.contains("web", ignoreCase = true) || 
            fileName.contains("online", ignoreCase = true) -> PDFSaveConfig.webOptimized()
            
            // 文件名包含安全相关关键词
            fileName.contains("secure", ignoreCase = true) || 
            fileName.contains("clean", ignoreCase = true) -> PDFSaveConfig.secure()
            
            // 小修改且支持增量保存
            canUseIncremental -> PDFSaveConfig.fastSave()
            
            // 大文件（>10MB）使用压缩
            fileSize > 10 * 1024 * 1024 -> PDFSaveConfig.minimumSize()
            
            // 默认配置
            else -> PDFSaveConfig.default()
        }
    }
    
    // ================== 高级保存管理器 ==================
    
    /**
     * 高级PDF保存管理器
     * 简化Activity中的保存操作，提供完整的保存流程管理
     * 
     * @param context 上下文
     * @param muPDFCore MuPDF核心对象
     * @param documentInfo 文档信息
     * @param targetFileName 目标文件名
     * @param saveAsNew 是否保存为新文件
     * @param saveDirectory 保存目录（为空时自动选择）
     * @param customConfig 自定义保存配置（为空时自动选择）
     * @return 保存结果和详细信息
     */
    suspend fun executeSaveOperation(
        context: Context,
        muPDFCore: MuPDFCore?,
        documentInfo: PDFDocumentInfo,
        targetFileName: String,
        saveAsNew: Boolean,
        saveDirectory: String? = null,
        customConfig: PDFSaveConfig? = null
    ): PDFSaveResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始执行保存操作: $targetFileName, 保存为新文件: $saveAsNew")
                
                // 1. 确定输出路径
                val outputPath = determineOutputPath(
                    context = context,
                    documentInfo = documentInfo,
                    targetFileName = targetFileName,
                    saveAsNew = saveAsNew,
                    saveDirectory = saveDirectory
                )
                
                if (outputPath == null) {
                    return@withContext PDFSaveResult(
                        success = false,
                        errorMessage = "无法确定保存路径"
                    )
                }
                
                // 2. 选择保存配置
                val saveConfig = customConfig ?: selectOptimalSaveConfig(
                    muPDFCore = muPDFCore,
                    documentInfo = documentInfo,
                    targetFileName = targetFileName,
                    saveAsNew = saveAsNew
                )
                
                // 3. 执行保存操作
                val result = smartSavePDF(
                    context = context,
                    muPDFCore = muPDFCore,
                    outputPath = outputPath,
                    sourcePath = documentInfo.filePath,
                    sourceUri = documentInfo.fileUri,
                    config = saveConfig
                )
                
                Log.d(TAG, "保存操作完成: ${result.success}, 路径: ${result.outputPath}")
                result
                
            } catch (e: Exception) {
                Log.e(TAG, "保存操作异常: ${e.message}", e)
                PDFSaveResult(
                    success = false,
                    errorMessage = "保存操作失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 快速保存操作（覆盖当前文件）
     * 专门用于覆盖现有文件的简化操作
     * 
     * @param context 上下文
     * @param muPDFCore MuPDF核心对象
     * @param documentInfo 文档信息
     * @param customConfig 自定义保存配置
     * @return 保存结果
     */
    suspend fun quickOverwriteSave(
        context: Context,
        muPDFCore: MuPDFCore?,
        documentInfo: PDFDocumentInfo,
        customConfig: PDFSaveConfig? = null
    ): PDFSaveResult {
        return withContext(Dispatchers.IO) {
            try {
                // 只支持本地文件的覆盖保存
                val originalPath = documentInfo.filePath
                if (originalPath.isNullOrBlank()) {
                    return@withContext PDFSaveResult(
                        success = false,
                        errorMessage = "URI文件不支持覆盖保存"
                    )
                }
                
                // 检查文件是否存在
                if (!File(originalPath).exists()) {
                    return@withContext PDFSaveResult(
                        success = false,
                        errorMessage = "原文件不存在: $originalPath"
                    )
                }
                
                // 选择最优配置
                val saveConfig = customConfig ?: run {
                    if (canBeSavedIncrementally(muPDFCore)) {
                        PDFSaveConfig.fastSave()
                    } else {
                        PDFSaveConfig.default()
                    }
                }
                
                // 执行覆盖保存
                smartSavePDF(
                    context = context,
                    muPDFCore = muPDFCore,
                    outputPath = originalPath,
                    sourcePath = originalPath,
                    sourceUri = null,
                    config = saveConfig
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "快速覆盖保存失败: ${e.message}", e)
                PDFSaveResult(
                    success = false,
                    errorMessage = "覆盖保存失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 获取保存操作的建议信息
     * 提供保存前的预检信息
     * 
     * @param muPDFCore MuPDF核心对象
     * @param documentInfo 文档信息
     * @param targetFileName 目标文件名
     * @param saveAsNew 是否保存为新文件
     * @return 建议信息
     */
    fun getSaveRecommendations(
        muPDFCore: MuPDFCore?,
        documentInfo: PDFDocumentInfo,
        targetFileName: String,
        saveAsNew: Boolean
    ): Map<String, Any> {
        val recommendations = mutableMapOf<String, Any>()
        
        try {
            // 基本信息
            recommendations["canUseIncremental"] = canBeSavedIncrementally(muPDFCore)
            recommendations["originalFileSize"] = documentInfo.fileSize
            recommendations["isLocalFile"] = documentInfo.isLocalFile()
            
            // 建议的保存配置
            val suggestedConfig = selectOptimalSaveConfig(
                muPDFCore = muPDFCore,
                documentInfo = documentInfo,
                targetFileName = targetFileName,
                saveAsNew = saveAsNew
            )
            recommendations["suggestedConfig"] = suggestedConfig.buildOptionsString()
            
            // 预估保存时间（基于文件大小）
            val estimatedTime = when {
                documentInfo.fileSize < 1024 * 1024 -> "< 1秒"
                documentInfo.fileSize < 10 * 1024 * 1024 -> "1-5秒"
                documentInfo.fileSize < 50 * 1024 * 1024 -> "5-15秒"
                else -> "15秒以上"
            }
            recommendations["estimatedSaveTime"] = estimatedTime
            
            // 支持的操作
            recommendations["supportsOverwrite"] = documentInfo.isLocalFile()
            recommendations["supportsSaveAsNew"] = true
            
        } catch (e: Exception) {
            Log.w(TAG, "获取保存建议失败: ${e.message}")
            recommendations["error"] = e.message ?: "未知错误"
        }
        
        return recommendations
    }
    
    // ================== 私有辅助方法 ==================
    
    /**
     * 确定输出路径
     */
    private fun determineOutputPath(
        context: Context,
        documentInfo: PDFDocumentInfo,
        targetFileName: String,
        saveAsNew: Boolean,
        saveDirectory: String?
    ): String? {
        return try {
            when {
                // 覆盖保存：使用原文件路径
                !saveAsNew && documentInfo.isLocalFile() -> {
                    documentInfo.filePath
                }
                
                // 保存为新文件且指定了保存目录
                saveAsNew && !saveDirectory.isNullOrBlank() -> {
                    val dir = File(saveDirectory)
                    if (!dir.exists()) dir.mkdirs()
                    File(dir, targetFileName).absolutePath
                }
                
                // 保存为新文件且原文件是本地文件：保存到同一目录
                saveAsNew && documentInfo.isLocalFile() -> {
                    val originalFile = File(documentInfo.filePath!!)
                    val parentDir = originalFile.parent ?: return null
                    File(parentDir, targetFileName).absolutePath
                }
                
                // 保存为新文件且原文件是URI：保存到应用私有目录
                saveAsNew -> {
                    val saveDir = File(context.getExternalFilesDir(null), "Saved_PDFs")
                    if (!saveDir.exists()) saveDir.mkdirs()
                    File(saveDir, targetFileName).absolutePath
                }
                
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "确定输出路径失败: ${e.message}")
            null
        }
    }
    
    /**
     * 选择最优保存配置
     */
    private fun selectOptimalSaveConfig(
        muPDFCore: MuPDFCore?,
        documentInfo: PDFDocumentInfo,
        targetFileName: String,
        saveAsNew: Boolean
    ): PDFSaveConfig {
        return try {
            // 如果是覆盖保存且支持增量保存，优先使用快速保存
            if (!saveAsNew && canBeSavedIncrementally(muPDFCore)) {
                PDFSaveConfig.fastSave()
            } else {
                // 使用智能配置选择
                getSuggestedSaveConfig(
                    fileName = targetFileName,
                    fileSize = documentInfo.fileSize,
                    canUseIncremental = canBeSavedIncrementally(muPDFCore)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "选择保存配置失败，使用默认配置: ${e.message}")
            PDFSaveConfig.default()
        }
    }
}