package com.documentpro.office.business.fileviewer.ui.office

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityOfficeViewBinding
import com.documentpro.office.business.fileviewer.dialog.BusinessFileLockDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessStoreScoreDialog
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.utils.BusinessPdfUtils
import com.documentpro.office.business.fileviewer.utils.BusinessRecentStorage
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.equalsFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

class OfficeViewActivity : BaseActivity<ActivityOfficeViewBinding, BusinessMainModel>() {

    companion object {
        private const val TAG = "OfficeViewActivity"
        private const val PARAM_FILE_INFO = "param_file_info"

        /**
         * 启动Office文档查看器
         * @param context 上下文
         * @param fileInfo 文件信息对象
         */
        fun launch(context: Context, fileInfo: BusinessFileInfo) {
            val intent = Intent(context, OfficeViewActivity::class.java)

            // 使用Bundle包装Parcelable对象
            val bundle = Bundle()
            bundle.putParcelable(PARAM_FILE_INFO, fileInfo)
            intent.putExtra("file_bundle", bundle)

            context.startActivity(intent)
        }
    }

    var fileInfo: BusinessFileInfo? = null
    override fun initBinding(): ActivityOfficeViewBinding {
        return ActivityOfficeViewBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessMainModel {
        return viewModels<BusinessMainModel>().value
    }

    @SuppressLint("JavascriptInterface")
    override fun initView() {
        try {
            // 记录初始化时的Intent信息，便于调试
            android.util.Log.d(
                TAG,
                "Intent action: ${intent.action}, extras: ${
                    intent.extras?.keySet()?.joinToString()
                }"
            )

            // 从Bundle中获取FileInfo对象
            val bundle = intent.getBundleExtra("file_bundle")
            android.util.Log.d(TAG, "Bundle获取结果: ${bundle != null}")

            if (bundle == null) {
                android.util.Log.e(TAG, "无法获取文件信息Bundle")
                finish()
                return
            }

            try {
                // 简化获取FileInfo的逻辑
                @Suppress("DEPRECATION")
                fileInfo = bundle.getParcelable(PARAM_FILE_INFO)
                android.util.Log.d(TAG, "从Bundle获取FileInfo结果: ${fileInfo != null}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "从Bundle获取FileInfo失败", e)
                fileInfo = null
            }

            if (fileInfo == null) {
                android.util.Log.e(TAG, "无法从Bundle获取文件信息")
                finish()
                return
            }

            fileInfo?.let {
                android.util.Log.d(TAG, "成功获取FileInfo: ${it.name}, 路径: ${it.path}")
                useDefaultToolbar(binding.toolbar, it.name)

                binding.webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportZoom(true)
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
                }
                binding.webView.addJavascriptInterface(object : Object() {
                    @JavascriptInterface
                    fun readFile(filePath: String, callback: String) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val file = File(filePath)
                                if (!file.exists()) {
                                    withContext(Dispatchers.Main) {
                                        binding.webView.evaluateJavascript(
                                            "window.handleFileReadError('${filePath}', '文件不存在')",
                                            null
                                        )
                                    }
                                    return@launch
                                }

                                // 使用兼容的方式读取文件
                                val fileData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    // Android 8.0 及以上使用 Files API
                                    Files.readAllBytes(file.toPath())
                                } else {
                                    // Android 8.0 以下使用传统方式
                                    file.readBytes()
                                }

                                val base64Data = Base64.encodeToString(fileData, Base64.NO_WRAP)

                                withContext(Dispatchers.Main) {
                                    binding.webView.evaluateJavascript(
                                        "window.handleFileReadSuccess('${filePath}', '$base64Data')",
                                        null
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    binding.webView.evaluateJavascript(
                                        "window.handleFileReadError('${filePath}', '${e.message}')",
                                        null
                                    )
                                }
                            }
                        }
                    }
                }, "Android")
                if (it.isLocked) {
                    showFileLockDialog(it, false)
                } else {
                    execLoad_1 (it)
                }

                BusinessPointLog.logEvent(
                    "Document_Open", mapOf(
                        "Document_Type" to equalsFileType(it.type).name
                    )
                )
            }
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    closePage()
                }
            })
            execLoad_2 ()
            BusinessStoreScoreDialog.checkShow(this)
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e(TAG, "初始化视图时发生异常", e)
            finish()
        }
        loadNative(binding.adViewContainer)
    }

    fun showFileLockDialog(fileInfo: BusinessFileInfo, isLock: Boolean) {
        BusinessFileLockDialog.show(this, isLock) {
            lifecycleScope.launch {
                val success = if (isLock) {
                    BusinessPdfUtils.encryptPdfInPlace(fileInfo.path, it, it)
                } else {
                    BusinessPdfUtils.decryptPdfInPlace(fileInfo.path, it)
                }
                if (success) {
                    withContext(Dispatchers.Main) {
                        // 更新文件的锁定状态
                        fileInfo.isLocked = isLock
                        // 同步更新最近列表和收藏列表中的锁定状态
                        BusinessRecentStorage.updateFileLockStatus(fileInfo.path, isLock)
                        
                        execLoad_1 (fileInfo)

                    }
                }
            }
        }
    }

    private fun execLoad_1(fileInfo: BusinessFileInfo) {
        binding.webView.loadUrl("file:///android_asset/officeview/preview.html?url=${fileInfo.path}")
    }

    override fun initObserve() {

    }

    override fun initTag(): String {
        return TAG
    }

    private fun execLoad_2() {
    }

    private fun execDisplay_3(nextAction: () -> Unit) {
        nextAction.invoke()
    }

    override fun closePage() {
        BusinessPointLog.logEvent(
            "Document_Back", mapOf(
                "Document_Type" to if (fileInfo != null) equalsFileType(fileInfo!!.type).name else ""
            )
        )
        execDisplay_3 {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }
}
