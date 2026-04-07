package com.documentpro.office.business.fileviewer.ui.process

import android.graphics.drawable.Drawable

/**
 * 进程信息数据类
 */
data class ProcessInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val memoryUsage: Long = 0L, // 内存使用量（字节）
    var isStopped: Boolean = false
)

