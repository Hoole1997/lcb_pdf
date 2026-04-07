package com.hq.mupdf.save

import android.content.Context
import com.hq.mupdf.R

/**
 * PDF保存选项枚举
 * 基于MuPDF支持的保存选项
 */
enum class PDFSaveOption(val value: String) {
    /**
     * 增量保存 - 只保存修改的部分，速度更快
     * 适用于已经存在的PDF文档的小修改
     */
    INCREMENTAL("incremental"),
    
    /**
     * 压缩保存 - 压缩文档以减小文件大小
     */
    COMPRESS("compress"),
    
    /**
     * 垃圾回收 - 移除未使用的对象
     */
    GARBAGE("garbage"),
    
    /**
     * 线性化 - 优化用于web查看（快速web查看）
     * 允许PDF在完全下载前就开始显示
     */
    LINEAR("linear"),
    
    /**
     * 清理 - 清理和优化文档结构
     */
    CLEAN("clean"),
    
    /**
     * 清理敏感信息 - 移除潜在的敏感信息
     */
    SANITIZE("sanitize"),
    
    /**
     * 美化 - 格式化PDF内容以便阅读（调试用）
     */
    PRETTY("pretty"),
    
    /**
     * ASCII编码 - 使用ASCII编码保存
     */
    ASCII("ascii")
}

/**
 * PDF保存配置类
 * 提供便捷的方法来组合多个保存选项
 */
data class PDFSaveConfig(
    private val options: Set<PDFSaveOption> = emptySet(),
    
    /**
     * 是否强制增量保存
     * 如果文档支持且此选项为true，将优先使用增量保存
     */
    val forceIncremental: Boolean = false,
    
    /**
     * 是否启用压缩（默认启用）
     */
    val enableCompression: Boolean = true,
    
    /**
     * 是否启用垃圾回收（默认启用）
     */
    val enableGarbageCollection: Boolean = true,
    
    /**
     * 是否线性化（适用于web查看）
     */
    val linearize: Boolean = false,
    
    /**
     * 是否清理文档
     */
    val clean: Boolean = false,
    
    /**
     * 是否清理敏感信息
     */
    val sanitize: Boolean = false
) {
    
    /**
     * 构建最终的保存选项字符串
     */
    fun buildOptionsString(): String {
        val finalOptions = mutableSetOf<PDFSaveOption>()
        
        // 添加明确指定的选项
        finalOptions.addAll(options)
        
        // 根据配置添加选项
        if (enableCompression) {
            finalOptions.add(PDFSaveOption.COMPRESS)
        }
        
        if (enableGarbageCollection) {
            finalOptions.add(PDFSaveOption.GARBAGE)
        }
        
        if (linearize) {
            finalOptions.add(PDFSaveOption.LINEAR)
        }
        
        if (clean) {
            finalOptions.add(PDFSaveOption.CLEAN)
        }
        
        if (sanitize) {
            finalOptions.add(PDFSaveOption.SANITIZE)
        }
        
        // 如果强制增量保存，添加增量选项
        if (forceIncremental) {
            finalOptions.add(PDFSaveOption.INCREMENTAL)
        }
        
        return finalOptions.joinToString(",") { it.value }
    }
    
    /**
     * 检查是否包含增量保存选项
     */
    fun hasIncrementalOption(): Boolean {
        return forceIncremental || options.contains(PDFSaveOption.INCREMENTAL)
    }
    
    companion object {
        /**
         * 默认配置 - 启用压缩和垃圾回收
         */
        fun default(): PDFSaveConfig {
            return PDFSaveConfig(
                enableCompression = true,
                enableGarbageCollection = true
            )
        }
        
        /**
         * 快速保存配置 - 优先使用增量保存
         */
        fun fastSave(): PDFSaveConfig {
            return PDFSaveConfig(
                forceIncremental = true,
                enableCompression = false,
                enableGarbageCollection = false
            )
        }
        
        /**
         * 最小文件大小配置 - 启用所有压缩选项
         */
        fun minimumSize(): PDFSaveConfig {
            return PDFSaveConfig(
                enableCompression = true,
                enableGarbageCollection = true,
                clean = true
            )
        }
        
        /**
         * Web优化配置 - 适合在线查看
         */
        fun webOptimized(): PDFSaveConfig {
            return PDFSaveConfig(
                enableCompression = true,
                enableGarbageCollection = true,
                linearize = true,
                clean = true
            )
        }
        
        /**
         * 安全配置 - 清理敏感信息
         */
        fun secure(): PDFSaveConfig {
            return PDFSaveConfig(
                enableCompression = true,
                enableGarbageCollection = true,
                clean = true,
                sanitize = true
            )
        }
        
        /**
         * 自定义配置构建器
         */
        fun builder(): Builder {
            return Builder()
        }
    }
    
    /**
     * 配置构建器
     */
    class Builder {
        private val options = mutableSetOf<PDFSaveOption>()
        private var forceIncremental = false
        private var enableCompression = true
        private var enableGarbageCollection = true
        private var linearize = false
        private var clean = false
        private var sanitize = false
        
        fun addOption(option: PDFSaveOption) = apply {
            options.add(option)
        }
        
        fun addOptions(vararg options: PDFSaveOption) = apply {
            this.options.addAll(options)
        }
        
        fun forceIncremental(force: Boolean = true) = apply {
            this.forceIncremental = force
        }
        
        fun enableCompression(enable: Boolean = true) = apply {
            this.enableCompression = enable
        }
        
        fun enableGarbageCollection(enable: Boolean = true) = apply {
            this.enableGarbageCollection = enable
        }
        
        fun linearize(enable: Boolean = true) = apply {
            this.linearize = enable
        }
        
        fun clean(enable: Boolean = true) = apply {
            this.clean = enable
        }
        
        fun sanitize(enable: Boolean = true) = apply {
            this.sanitize = enable
        }
        
        fun build(): PDFSaveConfig {
            return PDFSaveConfig(
                options = options.toSet(),
                forceIncremental = forceIncremental,
                enableCompression = enableCompression,
                enableGarbageCollection = enableGarbageCollection,
                linearize = linearize,
                clean = clean,
                sanitize = sanitize
            )
        }
    }
}

/**
 * PDF保存结果
 */
data class PDFSaveResult(
    val success: Boolean,
    val outputPath: String? = null,
    val fileSize: Long = -1,
    val errorMessage: String? = null,
    val usedIncremental: Boolean = false,
    val originalSize: Long = -1
) {
    /**
     * 计算压缩比例
     */
    fun getCompressionRatio(): Float {
        return if (originalSize > 0 && fileSize > 0) {
            1.0f - (fileSize.toFloat() / originalSize.toFloat())
        } else {
            0.0f
        }
    }
    
    /**
     * 获取文件大小变化的描述
     */
    fun getSizeChangeDescription(context: Context): String {
        return when {
            originalSize <= 0 || fileSize <= 0 -> context.getString(R.string.file_size_unavailable)
            fileSize < originalSize -> {
                val ratio = getCompressionRatio() * 100
                context.getString(R.string.file_size_reduced_format, ratio)
            }
            fileSize > originalSize -> {
                val increase = ((fileSize.toFloat() / originalSize.toFloat()) - 1.0f) * 100
                context.getString(R.string.file_size_increased_format, increase)
            }
            else -> context.getString(R.string.file_size_no_change)
        }
    }
}