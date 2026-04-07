package com.documentpro.office.business.fileviewer.ui.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.ActivityUtils
import com.google.android.material.tabs.TabLayout
import com.gyf.immersionbar.ImmersionBar
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseLazyFragment
import com.documentpro.office.business.fileviewer.databinding.FragmentHomeBinding
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.ui.search.SearchFileActivity
import com.documentpro.office.business.fileviewer.ui.setting.BusinessSettingActivity
import com.documentpro.office.business.fileviewer.utils.BusinessColorAnimatorUtils
import com.documentpro.office.business.fileviewer.utils.BusinessSplashForegroundController
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType
import com.documentpro.office.business.fileviewer.utils.queryfile.notifySystemToScan

class BusinessHomeFragment : BaseLazyFragment<FragmentHomeBinding, BusinessHomeModel>() {

    companion object {
        private const val TAG = "BusinessHomeFragment"
    }

    private val fragmentList = arrayListOf<BusinessFileListFragment>()
    private lateinit var mainModel: BusinessMainModel

    private var scaleXAnimator: ObjectAnimator? = null
    private var scaleYAnimator: ObjectAnimator? = null
    private var alphaAnimator: ObjectAnimator? = null
    private var backgroundAnimator: android.animation.ValueAnimator? = null
    private var currentThemePosition: Int = -1 // 当前主题位置，用于动画判断
    private var isInitialThemeSet: Boolean = false // 标记是否已经设置过初始主题
    private var isInitializing: Boolean = true // 标记是否正在初始化

    private val tabTitleList = arrayListOf(
        "All",
        BusinessFileType.PDF.name,
        BusinessFileType.WORD.name,
        BusinessFileType.EXCEL.name,
        BusinessFileType.PPT.name,
        BusinessFileType.TXT.name,
        BusinessFileType.IMAGE.name
    )

    override fun initBinding(): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(layoutInflater)
    }

    override fun initViewModel(): BusinessHomeModel? {
        mainModel = activityViewModels<BusinessMainModel>().value
        return viewModels<BusinessHomeModel>().value
    }

    override fun initView() {
        // 设置Toolbar菜单
        binding.toolbar.inflateMenu(R.menu.main_home_menu)
        
        // 菜单点击处理
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    ActivityUtils.startActivity(SearchFileActivity::class.java)
                    true
                }

                R.id.action_sort -> {
                    fragmentList[binding.vpPage.currentItem].showSortDialog()
                    true
                }

                R.id.action_choose -> {
                    safetyContext()?.let {
                        startActivity(ChooseFileActivity.launchIntent(
                            it,
                            tabTitleList[binding.vpPage.currentItem]
                        ))
                    }
//                    fragmentList[binding.vpPage.currentItem].launchChoose()
                    true
                }

                R.id.action_setting -> {
                    ActivityUtils.startActivity(BusinessSettingActivity::class.java)
                    true
                }

                else -> false
            }
        }
        binding.btnScan.setOnClickListener {
            requireActivity().loadInterstitial {
                BusinessSplashForegroundController.markNextIntercept()
                mainModel.clickScanButton()
            }

        }
        initTab()
        scanBtnAnim()
    }

    private fun initTab() {
        fragmentList.clear()
        tabTitleList.forEachIndexed { index, text ->
            val tab = binding.tab.newTab()
            tab.text = text
            binding.tab.addTab(tab)
            fragmentList.add(BusinessFileListFragment.newInstance(text))
        }
        binding.vpPage.offscreenPageLimit = fragmentList.size // 减少内存占用
        binding.vpPage.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment = fragmentList[position]
            override fun getItemCount(): Int = fragmentList.size
        }

        binding.tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // 根据选中的tab改变AppBarLayout背景颜色和文字颜色
                val position = binding.tab.selectedTabPosition
                
                // 同步ViewPager
                if (binding.vpPage.currentItem != position) {
                    binding.vpPage.currentItem = position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        binding.vpPage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (binding.tab.selectedTabPosition != position) {
                    binding.tab.selectTab(binding.tab.getTabAt(position))
                }
                // 更新AppBar主题色，初始化期间强制不使用动画
                updateAppBarTheme(position, forceNoAnimation = isInitializing)
            }
        })

        binding.vpPage.post {
//            // 默认选中PDF Tab并高亮
//            binding.vpPage.currentItem = 1
//            // 初始化时设置主题，不使用动画
//            updateAppBarTheme(1, forceNoAnimation = true)
            // 初始化完成，后续允许动画
            isInitializing = false
        }
    }

    /**
     * 根据选中的tab位置更新AppBarLayout的主题色
     * @param position tab位置
     * @param forceNoAnimation 强制不使用动画（用于初始化）
     */
    private fun updateAppBarTheme(position: Int, forceNoAnimation: Boolean = false) {
        // 如果位置没有变化，则不执行动画
        if (currentThemePosition == position) {
            return
        }
        
        // 取消之前的动画
        backgroundAnimator?.cancel()
        
        val oldPosition = currentThemePosition
        currentThemePosition = position
        
        // 定义背景资源
        val getBackgroundResource = { pos: Int ->
            when (pos) {
                0 -> R.color.white // All - 默认背景色
                1 -> R.drawable.gradient_pdf_theme // PDF - 红色渐变
                2 -> R.drawable.gradient_word_theme // WORD - 蓝色渐变
                3 -> R.drawable.gradient_excel_theme // EXCEL - 绿色渐变
                4 -> R.drawable.gradient_ppt_theme // PPT - 橙色渐变
                5 -> R.drawable.gradient_txt_theme // TXT - 蓝紫色渐变
                6 -> R.drawable.gradient_image_theme // IMAGE - 紫色渐变
                else -> R.color.default_background // 默认背景色
            }
        }
        
        val oldBackgroundResource = if (oldPosition >= 0) getBackgroundResource(oldPosition) else null
        val newBackgroundResource = getBackgroundResource(position)
        
        // 根据背景类型选择合适的menu
        val menuResource = when (position) {
            0 -> R.menu.main_home_menu // All - 使用黑色图标menu
            else -> R.menu.main_home_menu_white // 其他 - 使用白色图标menu
        }
        
        try {
            // 如果是第一次设置主题、oldPosition无效、或者强制不使用动画，直接设置，不使用动画
            if (!isInitialThemeSet || oldPosition < 0 || oldBackgroundResource == null || forceNoAnimation) {
                val backgroundDrawable = ContextCompat.getDrawable(requireContext(), newBackgroundResource)
                binding.appBarLayout.background = backgroundDrawable
                updateMenu(menuResource)
                updateToolbarTextColor(position)
                updateTabTextColors(position)
                
                // 标记初始主题已设置
                if (!isInitialThemeSet) {
                    isInitialThemeSet = true
                }
                return
            }
            
            // 确定滑动方向：向右移动时从左滑入，向左移动时从右滑入
            val fromLeft = position > oldPosition
            
            // 创建滑动动画 - 可以替换为其他动画效果
            backgroundAnimator = BusinessColorAnimatorUtils.createSlideAnimation(
                context = requireContext(),
                targetView = binding.appBarLayout,
                fromLeft = fromLeft,
                startColorRes = if (oldBackgroundResource == R.color.default_background) oldBackgroundResource else null,
                endColorRes = if (newBackgroundResource == R.color.default_background) newBackgroundResource else null,
                startDrawableRes = if (oldBackgroundResource != R.color.default_background) oldBackgroundResource else null,
                endDrawableRes = if (newBackgroundResource != R.color.default_background) newBackgroundResource else null,
                duration = 400L,
                onUpdate = { drawable ->
                    binding.appBarLayout.background = drawable
                },
                onEnd = {
                    // 动画结束后确保设置正确的最终背景
                    val finalDrawable = ContextCompat.getDrawable(requireContext(), newBackgroundResource)
                    binding.appBarLayout.background = finalDrawable
                }
            )
            
            // 立即更新menu和文字颜色（不需要动画）
            updateMenu(menuResource)
            updateToolbarTextColor(position)
            updateTabTextColors(position)
            
            // 启动背景动画
            backgroundAnimator?.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "设置背景动画失败: ${e.message}")
            // 降级方案：直接设置背景，不使用动画
            binding.appBarLayout.setBackgroundResource(newBackgroundResource)
            updateMenu(menuResource)
            updateToolbarTextColor(position)
            updateTabTextColors(position)
        }
        
        Log.d(TAG, "更新AppBar主题色，位置: $position, 背景资源: $newBackgroundResource")
    }

    /**
     * 更新menu资源
     * @param menuRes menu资源ID
     */
    private fun updateMenu(menuRes: Int) {
        binding.toolbar.menu.clear()
        binding.toolbar.inflateMenu(menuRes)
        
        // 重新设置菜单点击监听器
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    ActivityUtils.startActivity(SearchFileActivity::class.java)
                    true
                }

                R.id.action_sort -> {
                    fragmentList[binding.vpPage.currentItem].showSortDialog()
                    true
                }

                R.id.action_choose -> {
                    safetyContext()?.let {
                        startActivity(ChooseFileActivity.launchIntent(
                            it,
                            tabTitleList[binding.vpPage.currentItem]
                        ))
                    }
                    true
                }

                R.id.action_setting -> {
                    ActivityUtils.startActivity(BusinessSettingActivity::class.java)
                    true
                }

                else -> false
            }
        }
    }

    /**
     * 根据当前主题更新Toolbar标题颜色
     * @param position tab位置
     */
    private fun updateToolbarTextColor(position: Int) {
        // 直接使用SpannableString设置颜色，更可靠
        setTitleColorManually(position)
    }
    
    /**
     * 手动设置标题颜色（使用SpannableString，确保颜色正确显示）
     */
    private fun setTitleColorManually(position: Int) {
        val titleText = "PDF Viewer"
        val spannableString = SpannableString(titleText)
        
        // 根据position设置PDF的颜色
        val pdfColor = if (position == 0) {
            ContextCompat.getColor(requireContext(), R.color.theme_color) // #fc4949 红色
        } else {
            ContextCompat.getColor(requireContext(), android.R.color.white) // 白色
        }
        
        val readerColor = if (position == 0) {
            ContextCompat.getColor(requireContext(), R.color.black) // 黑色
        } else {
            ContextCompat.getColor(requireContext(), android.R.color.white) // 白色
        }
        
        // 设置"PDF "(包含空格)的颜色 (0-4)
        spannableString.setSpan(
            ForegroundColorSpan(pdfColor), 
            0, 4, 
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        // 设置"Reader"的颜色 (4-10)
        spannableString.setSpan(
            ForegroundColorSpan(readerColor), 
            4, titleText.length, 
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        binding.tvTitle.text = spannableString
        Log.d(TAG, "手动设置标题颜色完成，position: $position, PDF颜色: ${String.format("#%06X", 0xFFFFFF and pdfColor)}")
    }

    /**
     * 根据当前主题更新Tab文字颜色和TabIndicator颜色
     * @param position 当前选中的tab位置
     */
    private fun updateTabTextColors(position: Int) {
        when (position) {
            0 -> {
                // All标签选中时：未选中为灰色，选中为红色
                val selectedColor = ContextCompat.getColor(requireContext(), R.color.theme_color)
                val unselectedColor = ContextCompat.getColor(requireContext(), R.color.main_home_tab_unselected)
                binding.tab.setTabTextColors(unselectedColor, selectedColor)
                
                // TabIndicator设置为黑色
                val indicatorColor = ContextCompat.getColor(requireContext(), R.color.theme_color)
                binding.tab.setSelectedTabIndicatorColor(indicatorColor)
            }
            else -> {
                // 其他标签选中时：所有tab文字都为白色（未选中和选中都是白色）
                val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
                binding.tab.setTabTextColors(whiteColor, whiteColor)
                
                // TabIndicator设置为白色
                val indicatorColor = ContextCompat.getColor(requireContext(), android.R.color.white)
                binding.tab.setSelectedTabIndicatorColor(indicatorColor)
            }
        }
        
        Log.d(TAG, "更新Tab文字颜色和TabIndicator颜色，位置: $position")
    }

    override fun initObserve() {
        super.initObserve()
        mainModel.refreshHomeModel.observe(this) {
            binding.vpPage.post {
                binding.vpPage.currentItem = 0
            }
        }
        mainModel.checkShowGuideScanLive.observe(this) {
//            showGuideScanDialog({
//                showPDFDefaultLaunchDialog()
//            })
        }
    }

    private fun scanBtnAnim() {
        // 呼吸动画：缩放+透明度，分别启动
        scaleXAnimator = ObjectAnimator.ofFloat(binding.btnScan, "scaleX", 1f, 1.15f, 1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
        scaleYAnimator = ObjectAnimator.ofFloat(binding.btnScan, "scaleY", 1f, 1.15f, 1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
        alphaAnimator = ObjectAnimator.ofFloat(binding.btnScan, "alpha", 1f, 0.6f, 1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }
    }

    private fun preRequestAd() {

    }

    override fun onResume() {
        super.onResume()
        notifySystemToScan()
        preRequestAd()
    }

    override fun lazyLoad() {
        super.lazyLoad()
        BusinessPointLog.logEvent("Home_Show")
    }

    override fun onDestroy() {
        super.onDestroy()
        scaleXAnimator?.cancel()
        scaleYAnimator?.cancel()
        alphaAnimator?.cancel()
        backgroundAnimator?.cancel()
    }
}