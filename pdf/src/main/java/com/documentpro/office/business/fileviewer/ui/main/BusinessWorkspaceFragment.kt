package com.documentpro.office.business.fileviewer.ui.main

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.app.hubert.guide.NewbieGuide
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.android.common.bill.ads.AdException
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseFragment
import com.documentpro.office.business.fileviewer.databinding.ActivityMainBinding
import com.documentpro.office.business.fileviewer.dialog.BusinessCardNativeAdDialog
import com.documentpro.office.business.fileviewer.dialog.BusinessPDFDefaultLaunchDialog
import com.documentpro.office.business.fileviewer.dialog.DefaultAppDialog
import com.documentpro.office.business.fileviewer.dialog.ExitBusinessDialog
import com.documentpro.office.business.fileviewer.dialog.OverlayTipDialog
import com.documentpro.office.business.fileviewer.ui.favorite.BusinessFavoriteFragment
import com.documentpro.office.business.fileviewer.ui.home.BusinessHomeFragment
import com.documentpro.office.business.fileviewer.ui.language.LanguageActivity
import com.documentpro.office.business.fileviewer.ui.pdf.PDFScannerActivity
import com.documentpro.office.business.fileviewer.ui.recent.BusinessRecentFragment
import com.documentpro.office.business.fileviewer.ui.tool.BusinessToolsFragment
import com.documentpro.office.business.fileviewer.utils.BusinessPermissionDialogUtils
import com.documentpro.office.business.fileviewer.utils.BusinessPermissionDetector
import com.documentpro.office.business.fileviewer.utils.BusinessGuideCallbackController
import com.documentpro.office.business.fileviewer.utils.BusinessGuideCallbackController.GuideCallbackListener
import com.documentpro.office.business.fileviewer.utils.BusinessShareUtils
import com.documentpro.office.business.fileviewer.utils.BusinessSplashForegroundController
import com.documentpro.office.business.fileviewer.utils.BusinessStorageUtils
import com.documentpro.office.business.fileviewer.utils.DefaultLauncherController
import com.documentpro.office.business.fileviewer.utils.LauncherApplyTrack
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lxj.xpopup.XPopup
import kotlinx.coroutines.launch

class BusinessWorkspaceFragment : BaseFragment<ActivityMainBinding, BusinessMainModel>() {

    companion object {
        private const val TAG = "BusinessWorkspaceFragment"
    }

    private lateinit var tabController: BusinessTabController

    private var pdfDefaultLaunchDialogShow = false
    private var permissionDetector: BusinessPermissionDetector? = null

    // 注册PDF扫描Activity的launcher
    private val pdfScannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                BusinessPointLog.logEvent("Guide", mapOf("Guide" to 10))
                BusinessPointLog.logEvent("Guide", mapOf("Guide" to 11))
                model?.refreshFileScan()
                showPDFDefaultLaunchDialog()
            } else {
                BusinessPointLog.logEvent("Scan_Close")
            }
        }

    // 默认桌面权限控制器（必须在 onCreate 之前初始化）
    private lateinit var defaultLauncherController: DefaultLauncherController

    override fun initBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initViewModel(): BusinessMainModel {
        return activityViewModels<BusinessMainModel>().value
    }


    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        tabController.setSelectedTab(binding.pageContainer.currentItem)
//        requestDefaultDesktop()
    }

    override fun initView() {
        // 处理系统栏 insets，只设置底部导航栏占位高度
        // 状态栏区域由子 Fragment 自己处理（通过 paddingTop）
        val rootView = binding.root
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBarsInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // 设置底部占位 View 的高度为导航栏高度
            binding.navBarSpacer.layoutParams?.height = systemBarsInsets.bottom
            binding.navBarSpacer.requestLayout()
            // 不消费 insets，让子 View 也能收到（用于子 Fragment 设置 paddingTop）
            insets
        }
        // 触发 insets 应用
        binding.root.requestApplyInsets()

        defaultLauncherController = DefaultLauncherController(
            activity = requireActivity(),
            lifecycleOwner = this
        )

        // 监听存储权限返回
        permissionDetector = BusinessPermissionDetector(
            requireActivity(),
            permissionChecker = { XXPermissions.isGrantedPermissions(requireContext(), Permission.MANAGE_EXTERNAL_STORAGE) },
            onPermissionGranted = {
                val intent = Intent(requireContext(), requireActivity().javaClass)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            })
        initNavigation()

        if(!BusinessGuideCallbackController.hasGuideShown(requireContext())){
            // 没展示过需要等待用户点击引导页的skip或者从demo pdf页返回
            // guide1和BusinessFileListFragment中设置的对应
            BusinessGuideCallbackController.setListener(
                object : GuideCallbackListener {
                    override fun onGuideCompleted(completionType: BusinessGuideCallbackController.GuideCompletionType) {
                        binding.pageContainer.post {
                            requestStorage()
                        }
                    }
                },
            )
        }

//        lifecycleScope.launch {
//            delay(300L)
//            requestDefaultDesktop()
//        }

    }

    private fun loadBottomBannerAd() {
        binding.adViewContainer.visibility = View.GONE
        binding.adViewContainer.removeAllViews()

        lifecycleScope.launch {
            val result = runCatching {
                AdShowExt.showBannerAd(requireActivity(), binding.adViewContainer)
            }.getOrElse {
                AdResult.Failure(AdException(code = -1, message = it.message ?: "banner load failed", cause = it))
            }

            when (result) {
                is AdResult.Success -> binding.adViewContainer.visibility = View.VISIBLE
                is AdResult.Failure -> binding.adViewContainer.visibility = View.GONE
            }
        }
    }

    fun launcherRequestPermission() {
        if(BusinessGuideCallbackController.hasGuideShown(requireContext())){
            requestStorage()
        }else{
            //没展示过监听，已经在初始化设置了
        }
        loadInterAdOnce()
        loadBottomBannerAd()
    }

    fun loadInterAdOnce(){
        val key = "inter_ad_loaded_once"
        if (BusinessStorageUtils.getBoolean(key, false)) {
            return
        }
        BusinessStorageUtils.putBoolean(key, true)
        requireActivity().loadInterstitial {  }
    }

    fun requestDefaultDesktop() {
        lifecycleScope.launch {
            if(OverlayTipDialog.getHasShown()){
                LauncherApplyTrack.Home_Launcher()
                defaultLauncherController.requestDefaultLauncher (onResult = {

                })
            }
        }
    }

    private fun requestStorage() {
        BusinessPermissionDialogUtils.showFilePermissionDialog(
            requireActivity(),
            needDialog = true,
            needStartPermissionPage = true,
            nextAction = {
                if (!it) {
                    ToastUtils.showShort(getString(R.string.toast_get_permission_failed))
                } else {
                    BusinessPointLog.logEvent(
                        "All_File_Success", mapOf(
                            "File_Request_Position" to 4
                        )
                    )
                }
            },
            onShowListener = {
                BusinessPointLog.logEvent(
                    "All_File_Request_Show", mapOf(
                        "File_Request_Position" to 4
                    )
                )
            },
            onConfirmListener = {
                BusinessPointLog.logEvent(
                    "All_File_Click", mapOf(
                        "File_Request_Position" to 4
                    )
                )
            },
        )
    }

    private fun showExitBusinessDialog() {
        XPopup.Builder(requireContext())
            .dismissOnBackPressed(true)
            .dismissOnTouchOutside(true)
            .asCustom(ExitBusinessDialog(requireActivity()))
            .show()
    }

    override fun initObserve() {
        model?.refreshHomeModel?.observe(viewLifecycleOwner) {
            binding.pageContainer.post {
                tabController.setSelectedTab(0)
            }
        }
        model?.clickScanButton?.observe(viewLifecycleOwner) {
            if (it && !ActivityUtils.isActivityExistsInStack(LanguageActivity::class.java)) {
                BusinessPointLog.logEvent("Guide", mapOf("Guide" to 9))
                BusinessPointLog.logEvent("Scan_Click")

                //点击扫描按钮的时候，先弹出引导文件按钮权限弹框，再检测扫描引导弹框
                BusinessPermissionDialogUtils.showFilePermissionDialog(
                    requireActivity(),
                    needDialog = true,
                    needStartPermissionPage = true,
                    nextAction = {
                        if (!it) {
                            ToastUtils.showShort(getString(R.string.toast_get_permission_failed))
                        } else {
                            BusinessPointLog.logEvent(
                                "All_File_Success", mapOf(
                                    "File_Request_Position" to 3
                                )
                            )
                            BusinessSplashForegroundController.markNextIntercept()
                            val intent = Intent(requireContext(), PDFScannerActivity::class.java)
                            pdfScannerLauncher.launch(intent)
                        }
                    },
                    onShowListener = {
                        BusinessPointLog.logEvent(
                            "All_File_Request_Show", mapOf(
                                "File_Request_Position" to 3
                            )
                        )
                    },
                    onConfirmListener = {
                        BusinessPointLog.logEvent(
                            "All_File_Click", mapOf(
                                "File_Request_Position" to 3
                            )
                        )
                    },
                )
            }
        }
    }

    private fun initNavigation() {
        binding.pageContainer.apply {
            val fragmentList = arrayListOf(
                BusinessHomeFragment(),
                BusinessRecentFragment(),
                BusinessFavoriteFragment(),
                BusinessToolsFragment()
            )
            adapter = object : FragmentStateAdapter(this@BusinessWorkspaceFragment) {
                override fun createFragment(position: Int): Fragment {
                    return fragmentList[position]
                }

                override fun getItemCount(): Int {
                    return fragmentList.size
                }
            }
            offscreenPageLimit = fragmentList.size
            isUserInputEnabled = false
        }

        // 初始化TabController
        val lottieBottomNav = binding.lottieBottomNav
        tabController = BusinessTabController(requireActivity(), lottieBottomNav, binding.pageContainer)
        // 设置tab选中监听器
        tabController.setOnTabSelectedListener { position ->
            binding.pageContainer.setCurrentItem(position, false)
            BusinessCardNativeAdDialog.showOncePerMinute(requireActivity())
        }
    }

    private fun showPDFDefaultLaunchDialog() {
        if (pdfDefaultLaunchDialogShow) return
        BusinessPDFDefaultLaunchDialog.checkShow(
            requireActivity(),
            onConfirmListener = {
                BusinessShareUtils.openPdfDefaultLaunch(requireActivity())
            }, onShowListener = {
                pdfDefaultLaunchDialogShow = true
            }, onDismissListener = {
                pdfDefaultLaunchDialogShow = false
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BusinessGuideCallbackController.removeListener()
    }

    fun handleNewIntent() {
        // 独立入口模式下不再处理通知跳转。
    }
}
