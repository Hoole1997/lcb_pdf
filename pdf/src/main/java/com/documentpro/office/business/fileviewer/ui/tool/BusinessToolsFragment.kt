package com.documentpro.office.business.fileviewer.ui.tool

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
import com.documentpro.office.business.fileviewer.utils.toFileSizeNumString
import com.documentpro.office.business.fileviewer.utils.toFileSizeString
import com.documentpro.office.business.fileviewer.utils.toFileSizeUnitString
import com.documentpro.office.business.fileviewer.widget.BusinessProgressSegment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                // 获取总存储空间
                val total = BusinessCleanUtils.getDeviceTotalStorage().toFloat()
                val used = BusinessCleanUtils.getDeviceUsedStorage().toFloat()

                // 获取各类存储大小
                val context = context ?: return@launch
                val contentResolver = context.contentResolver

                val appsSize = BusinessCleanUtils.getAllAppsStorageSize(context).toFloat()
                val videosSize =
                    BusinessCleanUtils.getAllVideosStorageSize(contentResolver).toFloat()
                val imagesSize =
                    BusinessCleanUtils.getAllImagesStorageSize(contentResolver).toFloat()
                val musicSize = BusinessCleanUtils.getAllMusicStorageSize(contentResolver).toFloat()

                // 计算百分比
                val appsPercent = if (total > 0) (appsSize / total) * 100f else 0f
                val videosPercent = if (total > 0) (videosSize / total) * 100f else 0f
                val imagesPercent = if (total > 0) (imagesSize / total) * 100f else 0f
                val musicPercent = if (total > 0) (musicSize / total) * 100f else 0f

                // 计算其他存储（总使用量减去已知类型）
                val knownUsedSize = appsSize + videosSize + imagesSize + musicSize
                val otherSize = if (used > knownUsedSize) used - knownUsedSize else 0f
                val otherPercent = if (total > 0) (otherSize / total) * 100f else 0f

                // 在主线程更新UI
                withContext(Dispatchers.Main) {
                    binding.tvUsedMemory.text = used.toLong().toFileSizeString()
                    binding.tvTotalMemory.text = total.toLong().toFileSizeString()
                    binding.tvUsedMemory1.text = used.toLong().toFileSizeNumString()
                    binding.tvUsedMemory1Unit.text = total.toLong().toFileSizeUnitString()
                    // 创建进度段列表
                    val segments = mutableListOf<BusinessProgressSegment>()

                    // 添加各类存储段（只添加占用大于0的）
                    if (appsPercent > 0) {
                        segments.add(
                            BusinessProgressSegment(
                                percentage = appsPercent,
                                color = resources.getColor(R.color.file_type_app)
                            )
                        )
                    }

                    if (videosPercent > 0) {
                        segments.add(
                            BusinessProgressSegment(
                                percentage = videosPercent,
                                color = resources.getColor(R.color.file_type_video)
                            )
                        )
                    }

                    if (imagesPercent > 0) {
                        segments.add(
                            BusinessProgressSegment(
                                percentage = imagesPercent,
                                color = resources.getColor(R.color.file_type_image)
                            )
                        )
                    }

                    if (musicPercent > 0) {
                        segments.add(
                            BusinessProgressSegment(
                                percentage = musicPercent,
                                color = resources.getColor(R.color.file_type_music)
                            )
                        )
                    }

                    // 设置进度段
                    if (segments.isNotEmpty()) {
                        // 内部控制首次动画
                        binding.circleProgress.setProgressSegments(segments)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 出错时在主线程显示默认值
                withContext(Dispatchers.Main) {
                    val used = BusinessCleanUtils.getDeviceUsedStorage().toFloat()
                    val total = BusinessCleanUtils.getDeviceTotalStorage().toFloat()
                    binding.tvUsedMemory.text = used.toLong().toFileSizeString()
                    binding.tvTotalMemory.text = total.toLong().toFileSizeString()
                    binding.tvUsedMemory1.text = used.toLong().toFileSizeNumString()
                    binding.tvUsedMemory1Unit.text = total.toLong().toFileSizeUnitString()
                    binding.circleProgress.setProgressSegments(
                        listOf(
                            BusinessProgressSegment(
                                percentage = if (total > 0) (used / total) * 100f else 0f,
                                color = resources.getColor(R.color.theme_color)
                            )
                        )
                    )
                }
            }
        }
    }

}
