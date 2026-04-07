package com.documentpro.office.business.fileviewer.ui.recent

import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.ActivityUtils
import com.google.android.material.tabs.TabLayout
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseFragment
import com.documentpro.office.business.fileviewer.base.BaseLazyFragment
import com.documentpro.office.business.fileviewer.databinding.FragmentRecentBinding
import com.documentpro.office.business.fileviewer.ui.home.BusinessFileListFragment
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.ui.search.SearchFileActivity
import com.documentpro.office.business.fileviewer.ui.setting.BusinessSettingActivity
import com.documentpro.office.business.fileviewer.utils.BusinessColorAnimatorUtils
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType

class BusinessRecentFragment : BaseLazyFragment<FragmentRecentBinding, BusinessRecentModel>() {
    
    companion object {
        private const val TAG = "BusinessRecentFragment"
    }
    
    private val fragmentList = arrayListOf<BusinessFileListFragment>()
    private lateinit var mainModel: BusinessMainModel
    private var backgroundAnimator: android.animation.ValueAnimator? = null
    private var currentThemePosition: Int = -1 // 当前主题位置，用于动画判断
    private var isInitialThemeSet: Boolean = false // 标记是否已经设置过初始主题
    private var isInitializing: Boolean = true // 标记是否正在初始化
    
    override fun initBinding(): FragmentRecentBinding {
        return FragmentRecentBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 设置Toolbar菜单
        binding.toolbar.inflateMenu(R.menu.main_recent_menu)
        
        // 菜单点击处理
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    ActivityUtils.startActivity(SearchFileActivity::class.java)
                    true
                }

                R.id.action_setting -> {
                    ActivityUtils.startActivity(BusinessSettingActivity::class.java)
                    true
                }

                else -> false
            }
        }
        initTab()
    }

    override fun initViewModel(): BusinessRecentModel? {
        mainModel = activityViewModels<BusinessMainModel>().value
        return viewModels<BusinessRecentModel>().value
    }

    override fun initObserve() {
        mainModel.refreshHomeModel.observe(this) {
            binding.vpPage.post {
                binding.vpPage.currentItem = 0
            }
        }
        
        // 移除直接操作Fragment的代码，由FileListFragment自己订阅refreshRecentModel
        // 这样可以避免生命周期问题，每个Fragment根据自己的dataSource来决定是否响应刷新
    }

    private fun initTab() {
        val tabTitleList = arrayListOf(
            "All",
            BusinessFileType.PDF.name,
            BusinessFileType.WORD.name,
            BusinessFileType.EXCEL.name,
            BusinessFileType.PPT.name,
            BusinessFileType.TXT.name,
            BusinessFileType.IMAGE.name
        )
        
        fragmentList.clear()
        tabTitleList.forEachIndexed { index, text ->
            val tab = binding.tab.newTab()
            tab.text = text
            binding.tab.addTab(tab)
            // 创建 BusinessFileListFragment 实例，传递数据源类型为 RECENT
            fragmentList.add(BusinessFileListFragment.newInstanceForRecent(text))
        }
        
        binding.vpPage.offscreenPageLimit = 2 // 优化内存使用，只缓存相邻的2个页面
        binding.vpPage.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment = fragmentList[position]
            override fun getItemCount(): Int = fragmentList.size
        }

        // 设置Tab和ViewPager的双向同步
        setupTabViewPagerSync()

        binding.vpPage.post {
            // 默认选中第一个Tab并高亮
            binding.vpPage.currentItem = 0
            // 初始化时设置主题，不使用动画
            updateAppBarTheme(0, forceNoAnimation = true)
            // 初始化完成，后续允许动画
            isInitializing = false
        }
    }
    
    /**
     * 设置Tab和ViewPager的双向同步
     */
    private fun setupTabViewPagerSync() {
        // Tab选中监听
        binding.tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val position = binding.tab.selectedTabPosition
                if (binding.vpPage.currentItem != position) {
                    binding.vpPage.currentItem = position
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        
        // ViewPager页面切换监听
        binding.vpPage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (binding.tab.selectedTabPosition != position) {
                    binding.tab.selectTab(binding.tab.getTabAt(position))
                }
                // 更新AppBar主题色
                updateAppBarTheme(position, forceNoAnimation = isInitializing)
            }
        })
    }

    /**
     * 根据选中的tab位置更新AppBarLayout的主题色
     * @param position tab位置
     * @param forceNoAnimation 强制不使用动画（用于初始化）
     */
    private fun updateAppBarTheme(position: Int, forceNoAnimation: Boolean = false) {
        // 如果位置没有变化，则不执行动画
        if (currentThemePosition == position) return
        
        // 取消之前的动画
        backgroundAnimator?.cancel()
        
        val oldPosition = currentThemePosition
        currentThemePosition = position
        
        val newBackgroundResource = getBackgroundResource(position)
        val menuResource = getMenuResource(position)
        
        try {
            // 如果是第一次设置主题或强制不使用动画，直接设置
            if (!isInitialThemeSet || forceNoAnimation) {
                setThemeDirectly(newBackgroundResource, menuResource, position)
                return
            }
            
            // 如果有有效的旧位置，使用动画
            if (oldPosition >= 0) {
                setThemeWithAnimation(oldPosition, position, newBackgroundResource, menuResource)
            } else {
                setThemeDirectly(newBackgroundResource, menuResource, position)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "设置背景动画失败: ${e.message}")
            // 降级方案：直接设置背景
            setThemeDirectly(newBackgroundResource, menuResource, position)
        }
        
        Log.d(TAG, "更新AppBar主题色，位置: $position")
    }
    
    /**
     * 获取背景资源
     */
    private fun getBackgroundResource(position: Int): Int {
        return when (position) {
            0 -> R.color.white
            1 -> R.drawable.gradient_pdf_theme
            2 -> R.drawable.gradient_word_theme  
            3 -> R.drawable.gradient_excel_theme
            4 -> R.drawable.gradient_ppt_theme
            5 -> R.drawable.gradient_txt_theme
            6 -> R.drawable.gradient_image_theme
            else -> R.color.default_background
        }
    }
    
    /**
     * 获取菜单资源
     */
    private fun getMenuResource(position: Int): Int {
        return if (position == 0) R.menu.main_recent_menu else R.menu.main_recent_menu_white
    }
    
    /**
     * 直接设置主题（无动画）
     */
    private fun setThemeDirectly(backgroundResource: Int, menuResource: Int, position: Int) {
        val backgroundDrawable = ContextCompat.getDrawable(requireContext(), backgroundResource)
        binding.appBarLayout.background = backgroundDrawable
        updateMenu(menuResource)
        updateToolbarTextColor(position)
        updateTabTextColors(position)
        
        if (!isInitialThemeSet) {
            isInitialThemeSet = true
        }
    }
    
    /**
     * 使用动画设置主题
     */
    private fun setThemeWithAnimation(oldPosition: Int, newPosition: Int, newBackgroundResource: Int, menuResource: Int) {
        val oldBackgroundResource = getBackgroundResource(oldPosition)
        val fromLeft = newPosition > oldPosition
        
        // 创建滑动动画
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
        
        // 立即更新menu和文字颜色
        updateMenu(menuResource)
        updateToolbarTextColor(newPosition)
        updateTabTextColors(newPosition)
        
        // 启动背景动画
        backgroundAnimator?.start()
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
        val titleColor = if (position == 0) {
            ContextCompat.getColor(requireContext(), R.color.black)
        } else {
            ContextCompat.getColor(requireContext(), android.R.color.white)
        }
        binding.toolbar.setTitleTextColor(titleColor)
    }

    /**
     * 根据当前主题更新Tab文字颜色和TabIndicator颜色
     * @param position 当前选中的tab位置
     */
    private fun updateTabTextColors(position: Int) {
        if (position == 0) {
            // All标签：未选中为灰色，选中为红色
            val selectedColor = ContextCompat.getColor(requireContext(), R.color.theme_color)
            val unselectedColor = ContextCompat.getColor(requireContext(), R.color.main_home_tab_unselected)
            binding.tab.setTabTextColors(unselectedColor, selectedColor)
            binding.tab.setSelectedTabIndicatorColor(selectedColor)
        } else {
            // 其他标签：所有文字都为白色
            val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
            binding.tab.setTabTextColors(whiteColor, whiteColor)
            binding.tab.setSelectedTabIndicatorColor(whiteColor)
        }
        
        Log.d(TAG, "更新Tab文字颜色，位置: $position")
    }

    private fun preRequestAd() {
        // 最近文件页面的广告预请求逻辑（如果需要的话）
    }

    override fun onResume() {
        super.onResume()
        preRequestAd()
    }

    override fun lazyLoad() {
        super.lazyLoad()
        BusinessPointLog.logEvent("Recent_Show")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        backgroundAnimator?.cancel()
    }
}