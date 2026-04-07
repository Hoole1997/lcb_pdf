package com.documentpro.office.business.fileviewer.utils

import com.blankj.utilcode.util.StringUtils
import com.documentpro.office.business.fileviewer.R

class BusinessExt {
}

fun Long.toFileSizeString(): String {
    val units = arrayOf(
        StringUtils.getString(R.string.file_size_b),
        StringUtils.getString(R.string.file_size_kb),
        StringUtils.getString(R.string.file_size_mb),
        StringUtils.getString(R.string.file_size_gb),
        StringUtils.getString(R.string.file_size_tb)
    )
    var size = this.toDouble()
    var unitIndex = 0

    // 循环计算，直到 size 小于 1024
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return String.format("%.2f %s", size, units[unitIndex])
}

fun Long.toFileSizeUnitString(): String {
    val units = arrayOf(
        StringUtils.getString(R.string.file_size_b),
        StringUtils.getString(R.string.file_size_kb),
        StringUtils.getString(R.string.file_size_mb),
        StringUtils.getString(R.string.file_size_gb),
        StringUtils.getString(R.string.file_size_tb)
    )
    var size = this.toDouble()
    var unitIndex = 0

    // 循环计算，直到 size 小于 1024
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return units[unitIndex]
}

fun Long.toFileSizeNumString(): String {
    val units = arrayOf(
        StringUtils.getString(R.string.file_size_b),
        StringUtils.getString(R.string.file_size_kb),
        StringUtils.getString(R.string.file_size_mb),
        StringUtils.getString(R.string.file_size_gb),
        StringUtils.getString(R.string.file_size_tb)
    )
    var size = this.toDouble()
    var unitIndex = 0

    // 循环计算，直到 size 小于 1024
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return String.format("%.2f", size)
}
