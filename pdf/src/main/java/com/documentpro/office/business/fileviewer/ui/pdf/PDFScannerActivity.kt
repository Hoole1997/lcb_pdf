package com.documentpro.office.business.fileviewer.ui.pdf

import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.activity.viewModels
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityPdfScannerBinding
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import java.io.File

class PDFScannerActivity : BaseActivity<ActivityPdfScannerBinding, BusinessMainModel>() {

    companion object {
        private const val TAG = "PDFScannerActivity"
    }

    override fun initBinding(): ActivityPdfScannerBinding {
        return ActivityPdfScannerBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessMainModel {
        return viewModels<BusinessMainModel>().value
    }

    override fun initView() {
        initDocumentScanner()
    }

    override fun initObserve() {

    }

    override fun initTag(): String {
        return TAG
    }

    private fun initDocumentScanner() {
        // 配置扫描器参数，允许图库导入，最多2页，支持JPEG和PDF输出
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()
        // 获取扫描器客户端
        val scanner = GmsDocumentScanning.getClient(options)
        // 注册扫描结果回调
        val scannerLauncher = registerForActivityResult(StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // 解析扫描结果
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                // 打印每一页图片的Uri
                scanResult?.pages?.forEach { page ->
                    val imageUri = page.imageUri
                    LogUtils.d("扫描到的图片Uri: $imageUri")
                }
                // 打印PDF信息，并将PDF移动到filesDir
                scanResult?.pdf?.let { pdf ->
                    val pdfUri = pdf.uri
                    val pageCount = pdf.pageCount
                    LogUtils.d("扫描生成的PDF Uri: $pdfUri, 页数: $pageCount")

                    // 1. 拷贝PDF到cache目录
                    val inputStream = contentResolver.openInputStream(pdfUri)
                    val cacheFile = File(cacheDir, "scan_result.pdf")
                    inputStream?.use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    // 2. 生成带时间戳的新文件名
                    val timeStr = TimeUtils.getNowString(TimeUtils.getSafeDateFormat("yyyyMMdd_HHmmss"))
                    val destFile = File(filesDir, "scan_result_${timeStr}.pdf")
                    val success = FileUtils.move(cacheFile, destFile)
                    if (success) {
                        LogUtils.d("PDF已移动到APP私有目录: ${destFile.absolutePath}")
                    } else {
                        LogUtils.d("PDF移动失败，仍在: ${cacheFile.absolutePath}")
                    }
                }
                setResult(RESULT_OK)
                closePage()
            } else {
                closePage()
            }
        }
        // 启动扫描器
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

}