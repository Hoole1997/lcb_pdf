package com.documentpro.office.business.fileviewer.ui.language

data class BusinessLanguageItem(
    val code: String,        // 语言代码
    val nativeName: String,  // 本地语言名称
    var isSelected: Boolean = false
) 