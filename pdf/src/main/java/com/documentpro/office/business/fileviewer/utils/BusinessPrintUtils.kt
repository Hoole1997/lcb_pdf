package com.documentpro.office.business.fileviewer.utils

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.blankj.utilcode.util.LogUtils
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object BusinessPrintUtils {

    fun printFile(activity: Context, fileInfo: BusinessFileInfo) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val jobName = "Document Print Job ${fileInfo.name}"

        // 使用自定义 PrintDocumentAdapter 来处理文件内容
        printManager.print(jobName, object : PrintDocumentAdapter() {
            private var myFile: File? = null

            override fun onLayout(
                oldAttributes: android.print.PrintAttributes?,
                newAttributes: android.print.PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                cancellationSignal?.setOnCancelListener { callback?.onLayoutCancelled() }

                // 简单的布局，只声明文档信息
                if (callback != null) {
                    val info = PrintDocumentInfo.Builder(fileInfo.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN) // 对于文件，页数通常未知直到渲染
                        .build()
                    callback.onLayoutFinished(info, newAttributes != oldAttributes)
                }
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                cancellationSignal?.setOnCancelListener { callback?.onWriteCancelled() }

                // 将文件内容写入目标 ParcelFileDescriptor
                var input: FileInputStream? = null
                var output: FileOutputStream? = null
                try {
                    myFile = File(fileInfo.path)
                    input = FileInputStream(myFile)
                    output = FileOutputStream(destination?.fileDescriptor)

                    val buf = ByteArray(1024)
                    var size: Int
                    while (input.read(buf).also { size = it } > 0) {
                        output.write(buf, 0, size)
                    }

                    callback?.onWriteFinished(pages)
                } catch (e: Exception) {
                    LogUtils.e("Error writing print content", e)
                    callback?.onWriteFailed(e.toString())
                } finally {
                    try { input?.close() } catch (e: Exception) { e.printStackTrace() }
                    try { output?.close() } catch (e: Exception) { e.printStackTrace() }
                }
            }

        }, null) // PrintAttributes 为 null 表示使用默认属性
    }
} 