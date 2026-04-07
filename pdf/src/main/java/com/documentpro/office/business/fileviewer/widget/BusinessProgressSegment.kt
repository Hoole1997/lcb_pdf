package com.documentpro.office.business.fileviewer.widget

import androidx.annotation.ColorInt

/**
 * 进度段数据类，用于表示多颜色进度条中的一个颜色段
 * @param percentage 该段占总进度的百分比 (0-100)
 * @param color 该段的颜色
 */
data class BusinessProgressSegment(
    val percentage: Float,
    @ColorInt val color: Int
)