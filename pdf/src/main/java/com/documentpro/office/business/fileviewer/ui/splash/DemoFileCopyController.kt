package com.documentpro.office.business.fileviewer.ui.splash

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Demo文件拷贝控制器
 * 
 * 负责将assets中的示例文档复制到应用目录
 */
class DemoFileCopyController(private val context: Context) {

    companion object {
        private const val TAG = BusinessSplashScreenActivity.TAG
        
        // 需要复制的示例文件列表
        private val DEMO_FILES = listOf(
            "Demo.pdf" to "doc/Demo.pdf",
            "Demo.docx" to "doc/Demo.docx",
            "Demo.xlsx" to "doc/Demo.xlsx",
            "Demo.pptx" to "doc/Demo.pptx",
            "Demo.txt" to "doc/Demo.txt"
        )
    }

    /**
     * 复制示例文档文件
     * 在IO线程执行，不阻塞主线程
     * @return 是否有文件被复制
     */
    suspend fun copyDemoFiles(): Boolean = withContext(Dispatchers.IO) {
        try {
            val docDir = File(context.filesDir, "doc")
            if (!docDir.exists()) {
                docDir.mkdirs()
            }

            var hasCopied = false

            DEMO_FILES.forEach { (fileName, assetPath) ->
                try {
                    val file = File(docDir, fileName)
                    if (!file.exists()) {
                        context.assets.open(assetPath).use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        hasCopied = true
                        Log.d(TAG, "复制文件成功: $fileName")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "复制文件失败: $fileName", e)
                    // 单个文件失败不影响其他文件
                }
            }

            if (hasCopied) {
                Log.d(TAG, "示例文件复制完成")
            } else {
                Log.d(TAG, "示例文件已存在，无需复制")
            }

            hasCopied
        } catch (e: Exception) {
            Log.e(TAG, "复制示例文件异常", e)
            false
        }
    }

    /**
     * 检查示例文件是否存在
     */
    fun isDemoFilesExist(): Boolean {
        val docDir = File(context.filesDir, "doc")
        if (!docDir.exists()) return false
        
        return DEMO_FILES.all { (fileName, _) ->
            File(docDir, fileName).exists()
        }
    }
}
