package com.documentpro.office.business.fileviewer.utils.queryfile

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * URI文档信息类 - 支持直接从URI加载而不需要转换为本地文件
 */
@Parcelize
data class BusinessURIDocumentInfo(
    val uri: Uri,                    // 文档URI
    var name: String,                // 文件名称
    val type: String,                // MIME 类型
    val size: Long,                  // 文件大小
    val isReadable: Boolean = true,  // 是否可读
    var isLocked: Boolean = false    // 是否加密/带密码锁
) : Serializable, Parcelable {

    companion object {
        /**
         * 从URI创建URIDocumentInfo
         */
        fun fromUri(context: Context, uri: Uri): BusinessURIDocumentInfo? {
            return try {
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        val name = if (nameIndex != -1) it.getString(nameIndex) else "unknown.pdf"
                        val size = if (sizeIndex != -1) it.getLong(sizeIndex) else 0L
                        
                        BusinessURIDocumentInfo(
                            uri = uri,
                            name = name,
                            type = "application/pdf",
                            size = size,
                            isReadable = true,
                            isLocked = false
                        )
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                // 降级处理，使用默认信息
                BusinessURIDocumentInfo(
                    uri = uri,
                    name = "document.pdf",
                    type = "application/pdf",
                    size = 0L,
                    isReadable = true,
                    isLocked = false
                )
            }
        }
    }

    /**
     * 获取文件图标
     */
    fun icon(): Int {
        return com.documentpro.office.business.fileviewer.R.mipmap.ic_pdf
    }

    /**
     * 获取文件扩展名
     */
    fun getExtension(): String {
        return name.substringAfterLast('.', "pdf")
    }
}