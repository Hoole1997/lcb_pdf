package com.documentpro.office.business.fileviewer.ui.tool

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseLazyFragment
import com.documentpro.office.business.fileviewer.databinding.FragmentToolsBinding
import com.documentpro.office.business.fileviewer.ui.clean.CleanStartLoadingActivity
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.ui.office.OfficeViewActivity
import com.documentpro.office.business.fileviewer.ui.pdf.BusinessLockActivity
import com.documentpro.office.business.fileviewer.ui.pdf.MergePdfActivity
import com.documentpro.office.business.fileviewer.ui.pdf.PDFScannerActivity
import com.documentpro.office.business.fileviewer.ui.process.ProcessStartLoadingActivity
import com.documentpro.office.business.fileviewer.ui.setting.BusinessSettingActivity
import com.documentpro.office.business.fileviewer.utils.BusinessCleanUtils
import com.documentpro.office.business.fileviewer.utils.BusinessSplashForegroundController
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.documentpro.office.business.fileviewer.utils.queryfile.fileInfoFromUri
import com.documentpro.office.business.fileviewer.utils.toFileSizeString
import com.documentpro.office.business.fileviewer.widget.BusinessProgressSegment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

class BusinessToolsFragment : BaseLazyFragment<FragmentToolsBinding, BusinessMainModel>() {

    //    private lateinit var toolsAdapter: BusinessToolsAdapter
    companion object {
        private const val TAG = "BusinessToolsFragment"
    }

    // 注册PDF选择器
    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val fileInfo = fileInfoFromUri(requireContext(), it)
                fileInfo?.let { info ->
                    OfficeViewActivity.launch(requireActivity(), info)
                }
            }
        }

    // 注册PDF扫描Activity的launcher
    private val pdfScannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {

                model?.refreshFileScan()
            }
        }

    private val image2PdfLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {

                model?.refreshFileScan()
            }
        }

    override fun initBinding(): FragmentToolsBinding {
        return FragmentToolsBinding.inflate(layoutInflater)
    }

    override fun initViewModel(): BusinessMainModel? {
        return activityViewModels<BusinessMainModel>().value
    }

    override fun lazyLoad() {
        super.lazyLoad()
        BusinessPointLog.logEvent("Tools_Show")
        requireActivity().loadNative(binding.adContainer, call = {
            if(it){
                binding.adContainer.findViewById<CardView>(R.id.cv).setCardBackgroundColor(android.graphics.Color.WHITE)
            }
        })
    }

    override fun onResume() {
        super.onResume()
//        BarUtils.addMarginTopEqualStatusBarHeight(binding.toolbar)
        refreshStorage()
    }

    override fun initView() {
        super.initView()

        model?.onNotificationOpenImage2PDF = {
            onClick(binding.btnImageToPdf,false)
        }
        model?.onNotificationEncryptPDF = {
            onClick(binding.btnLockPdf,false)
        }
        model?.onNotificationMergePDF = {
            onClick(binding.btnMergePdf,false)
        }
        model?.onNotificationSplitPDF = {
            onClick(binding.btnSplitPdf,false)
        }
        model?.onNotificationDecryptPDF = {
            onClick(binding.btnUnlockPdf,false)
        }
        model?.onNotificationJunkCLeaner = {
            onClick(binding.btnCleanTop,false)
        }
        model?.onNotificationProcessManager = {
            onClick(binding.btnProcess,false)
        }

        // 设置Toolbar菜单
        binding.toolbar.inflateMenu(R.menu.main_utils_menu)

        // 其它菜单点击
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_setting -> {
                    ActivityUtils.startActivity(BusinessSettingActivity::class.java)
                    true
                }

                else -> false
            }
        }
        initItemClick()
    }

    private fun initItemClick() {
        binding.btnCleanTop.setOnClickListener(::onClick)
        binding.topLayout.setOnClickListener(::onClick)
        binding.btnClean.setOnClickListener(::onClick)
        binding.btnLockPdf.setOnClickListener(::onClick)
        binding.btnUnlockPdf.setOnClickListener(::onClick)
        binding.btnImportPdf.setOnClickListener(::onClick)
        binding.btnScanPdf.setOnClickListener(::onClick)
        binding.btnImageToPdf.setOnClickListener(::onClick)
        binding.btnPrintPdf.setOnClickListener(::onClick)
        binding.btnMergePdf.setOnClickListener(::onClick)
        binding.btnSplitPdf.setOnClickListener(::onClick)
        binding.btnProcess.setOnClickListener(::onClick)
    }

    fun onClick(
        view: View,
        adEnable: Boolean = true
    ) {
        requireActivity().loadInterstitial(condition = {adEnable}) {
            when (view.id) {
                binding.btnClean.id, binding.btnCleanTop.id, binding.topLayout.id -> {
                    ActivityUtils.startActivity(CleanStartLoadingActivity::class.java)
                }

                binding.btnLockPdf.id -> BusinessLockActivity.launch(requireActivity(), true)
                binding.btnUnlockPdf.id -> BusinessLockActivity.launch(requireActivity(), false)
                binding.btnImportPdf.id -> {
                    BusinessSplashForegroundController.markNextIntercept()
                    BusinessPointLog.logEvent("ImportFile_Show")
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                }

                binding.btnScanPdf.id -> {
                    BusinessSplashForegroundController.markNextIntercept()
                    BusinessPointLog.logEvent("Scan_Click")
                    val intent = Intent(requireContext(), PDFScannerActivity::class.java)
                    pdfScannerLauncher.launch(intent)
                }

                binding.btnImageToPdf.id -> {
                    BusinessSplashForegroundController.markNextIntercept()
                    BusinessPointLog.logEvent("Scan_Click")
                    BusinessPointLog.logEvent("Image_PDF_Show")
                    BusinessPointLog.logEvent("ImagePDF_Show")
                    if (safetyContext() != null) {
                        if (safetyContext() is AppCompatActivity) {
                            val intent =
                                Intent(requireContext(), PDFScannerActivity::class.java)
                            image2PdfLauncher.launch(intent)
                        }
                    }
                }

                binding.btnPrintPdf.id -> {
                    BusinessPointLog.logEvent("PrintPDF_Show")
                    BusinessLockActivity.launchFromPrint(requireActivity())
                }

                binding.btnMergePdf.id -> {
                    safetyContext()?.let {
                        MergePdfActivity.launchForMerge(it)
                    }
                }

                binding.btnSplitPdf.id -> {
                    safetyContext()?.let {
                        MergePdfActivity.launchForSplit(it)
                    }
                }

                binding.btnProcess.id -> {
                    safetyContext()?.let {
                        ProcessStartLoadingActivity.start(requireActivity())
                    }
                }
            }
        }

    }

    private fun refreshStorage() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val total = BusinessCleanUtils.getDeviceTotalStorage().toFloat()
                val used = BusinessCleanUtils.getDeviceUsedStorage().toFloat()
                withContext(Dispatchers.Main) {
                    updateStorageCard(used, total)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val used = BusinessCleanUtils.getDeviceUsedStorage().toFloat()
                    val total = BusinessCleanUtils.getDeviceTotalStorage().toFloat()
                    updateStorageCard(used, total)
                }
            }
        }
    }

    private fun updateStorageCard(used: Float, total: Float) {
        val usedPercent = if (total > 0f) {
            ((used / total) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        binding.tvUsedMemory.text = buildStorageSummary(used.toLong(), total.toLong())
        binding.tvTotalMemory.text = total.toLong().toFileSizeString()
        binding.tvUsedMemory1.text = "${usedPercent.roundToInt()}%"
        binding.circleProgress.setProgressSegments(
            listOf(
                BusinessProgressSegment(
                    percentage = usedPercent,
                    color = ContextCompat.getColor(requireContext(), R.color.theme_color)
                )
            )
        )
    }

    private fun buildStorageSummary(used: Long, total: Long): String {
        return getString(
            R.string.tool_storage_summary,
            formatCompactFileSize(used, keepDecimals = true),
            formatCompactFileSize(total)
        )
    }

    private fun formatCompactFileSize(sizeInBytes: Long, keepDecimals: Boolean = false): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = sizeInBytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.lastIndex) {
            size /= 1024
            unitIndex++
        }

        val formatted = if (keepDecimals && size % 1.0 != 0.0) {
            String.format(Locale.US, "%.2f", size)
        } else if (size % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", size)
        } else {
            String.format(Locale.US, "%.2f", size).trimEnd('0').trimEnd('.')
        }
        return "$formatted${units[unitIndex]}"
    }

}
