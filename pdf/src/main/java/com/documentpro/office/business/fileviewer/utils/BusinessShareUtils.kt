package com.documentpro.office.business.fileviewer.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import java.io.File

object BusinessShareUtils {
    /**
     * 分享单个文件
     */
    @JvmStatic
    fun share(context: Context, fileInfo: BusinessFileInfo) {
        share(context, listOf(fileInfo))
    }

    /**
     * 分享多个文件
     */
    @JvmStatic
    fun share(context: Context, fileList: List<BusinessFileInfo>) {
        if (fileList.isEmpty()) return
        val uris = ArrayList<Uri>()
        val mimeTypes = HashSet<String>()
        fileList.forEach { fileInfo ->
            val file = java.io.File(fileInfo.path)
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            uris.add(uri)
            mimeTypes.add(fileInfo.type)
        }
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uris[0])
                type = mimeTypes.firstOrNull() ?: "application/octet-stream"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                type = if (mimeTypes.size == 1) mimeTypes.first() else "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "分享文件"))
    }

    fun openPdfDefaultLaunch(context: Context) {
        val exampleFile = File(context.filesDir, "doc/Demo.pdf")
        if (!exampleFile.exists()) {
            try {
                exampleFile.parentFile?.mkdirs()
                context.assets.open("doc/Demo.pdf").use { input ->
                    exampleFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (exampleFile.exists()) {
            val pdfUri = androidx.core.content.FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider",
                exampleFile
            )

            // 创建打开PDF的Intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // 添加FLAG_ACTIVITY_NEW_TASK标志
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val listQueryIntentActivities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            requireNotNull(listQueryIntentActivities) { "queryIntentActivities returned null" }

            for (resolveInfo in listQueryIntentActivities) {
                context.grantUriPermission(resolveInfo.activityInfo.packageName, pdfUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 将自己的应用程序放到列表的开头
            val myPackageName = context.packageName
            val resolvedInfo = listQueryIntentActivities.find {
                it.activityInfo.packageName == myPackageName
            }

            if (resolvedInfo != null) {
                listQueryIntentActivities.remove(resolvedInfo)
                listQueryIntentActivities.add(0, resolvedInfo)
            }

            context.startActivity(intent)
        }
    }

}