package com.documentpro.office.business.fileviewer.ui.main

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.wwdablu.soumya.lottiebottomnav.FontBuilder
import com.wwdablu.soumya.lottiebottomnav.LottieBottomNav
import com.wwdablu.soumya.lottiebottomnav.MenuItem
import com.wwdablu.soumya.lottiebottomnav.MenuItemBuilder
import com.documentpro.office.business.fileviewer.R
import com.wwdablu.soumya.lottiebottomnav.ILottieBottomNavCallback

class BusinessTabController(
    private val context: Context,
    private val lottieBottomNav: LottieBottomNav,
    private val viewPager: ViewPager2
) {
    
    private var onTabSelectedListener: ((Int) -> Unit)? = null
    
    init {
        setupTabs()
    }
    
    private fun setupTabs() {
        // 创建字体项
        val fileFontItem = FontBuilder.create(context.getString(R.string.main_tab_file))
            .selectedTextColor(context.getColor(R.color.theme_color))
            .unSelectedTextColor(context.getColor(R.color.font_bold))
            .selectedTextSize(12)
            .unSelectedTextSize(12)
            .build()

        val recentFontItem = FontBuilder.create(context.getString(R.string.main_tab_recent))
            .selectedTextColor(context.getColor(R.color.theme_color))
            .unSelectedTextColor(context.getColor(R.color.font_bold))
            .selectedTextSize(12)
            .unSelectedTextSize(12)
            .build()

        val favoriteFontItem = FontBuilder.create(context.getString(R.string.main_tab_favorite))
            .selectedTextColor(context.getColor(R.color.theme_color))
            .unSelectedTextColor(context.getColor(R.color.font_bold))
            .selectedTextSize(12)
            .unSelectedTextSize(12)
            .build()

        val toolsFontItem = FontBuilder.create(context.getString(R.string.main_tab_tools))
            .selectedTextColor(context.getColor(R.color.theme_color))
            .unSelectedTextColor(context.getColor(R.color.font_bold))
            .selectedTextSize(12)
            .unSelectedTextSize(12)
            .build()

        // 创建菜单项
        val fileItem = MenuItemBuilder.create("file_icon.json", MenuItem.Source.Assets, fileFontItem, "file")
            .pausedProgress(1f)
            .selectedLottieName("file_icon_sel.json")
            .unSelectedLottieName("file_icon.json")
            .loop(false)
            .build()

        val recentItem = MenuItemBuilder.create("recent_icon.json", MenuItem.Source.Assets, recentFontItem, "recent")
            .pausedProgress(1f)
            .selectedLottieName("recent_icon_sel.json")
            .unSelectedLottieName("recent_icon.json")
            .loop(false)
            .build()

        val favoriteItem = MenuItemBuilder.create("favorite_icon.json", MenuItem.Source.Assets, favoriteFontItem, "favorite")
            .pausedProgress(1f)
            .selectedLottieName("favorite_icon_sel.json")
            .unSelectedLottieName("favorite_icon.json")
            .loop(false)
            .build()

        val toolsItem = MenuItemBuilder.create("tools_icon.json", MenuItem.Source.Assets, toolsFontItem, "tools")
            .pausedProgress(1f)
            .selectedLottieName("tools_icon_sel.json")
            .unSelectedLottieName("tools_icon.json")
            .loop(false)
            .build()

        // 设置菜单项
        lottieBottomNav.setMenuItemList(listOf(fileItem, recentItem, favoriteItem, toolsItem))

        // 设置选中监听器
        lottieBottomNav.setCallback(object : ILottieBottomNavCallback{
            override fun onMenuSelected(
                oldIndex: Int,
                newIndex: Int,
                menuItem: MenuItem?
            ) {
                // 更新文字粗体状态
                updateTextBoldState(oldIndex, newIndex)
                onTabSelectedListener?.invoke(newIndex)
            }

            override fun onAnimationStart(
                index: Int,
                menuItem: MenuItem?
            ) {
            }

            override fun onAnimationEnd(
                index: Int,
                menuItem: MenuItem?
            ) {
            }

            override fun onAnimationCancel(
                index: Int,
                menuItem: MenuItem?
            ) {
            }

        })
        
        // 初始化时设置第一个tab为粗体
        lottieBottomNav.post {
            updateTextBoldState(-1, 0)
        }
    }
    
    /**
     * 更新文字粗体状态
     */
    private fun updateTextBoldState(oldIndex: Int, newIndex: Int) {
        try {
            // 遍历LottieBottomNav的所有子view，找到TextView并设置粗体
            for (i in 0 until lottieBottomNav.childCount) {
                val child = lottieBottomNav.getChildAt(i)
                if (child is android.view.ViewGroup) {
                    // 递归查找TextView
                    findAndUpdateTextView(child, i, newIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 递归查找并更新TextView的粗体状态
     */
    private fun findAndUpdateTextView(viewGroup: android.view.ViewGroup, tabIndex: Int, selectedIndex: Int) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            when (child) {
                is TextView -> {
                    // 找到TextView，设置粗体状态
                    if (tabIndex == selectedIndex) {
                        child.setTypeface(null, Typeface.BOLD)
                    } else {
                        child.setTypeface(null, Typeface.NORMAL)
                    }
                }
                is android.view.ViewGroup -> {
                    // 如果是ViewGroup，递归查找
                    findAndUpdateTextView(child, tabIndex, selectedIndex)
                }
            }
        }
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }
    
    fun setSelectedTab(index: Int) {
        lottieBottomNav.setSelectedIndex(index)
    }
    
    fun getCurrentTab(): Int {
        return viewPager.currentItem
    }
    
    fun show() {
        lottieBottomNav.visibility = View.VISIBLE
    }
    
    fun hide() {
        lottieBottomNav.visibility = View.GONE
    }
} 