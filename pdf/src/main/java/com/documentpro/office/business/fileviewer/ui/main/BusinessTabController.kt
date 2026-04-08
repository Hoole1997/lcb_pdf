package com.documentpro.office.business.fileviewer.ui.main

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.ActivityMainBinding

class BusinessTabController(
    private val binding: ActivityMainBinding,
    private val viewPager: ViewPager2
) {

    private data class BottomNavItem(
        val container: View,
        val icon: ImageView,
        val label: TextView,
        @param:DrawableRes val selectedIconRes: Int,
        @param:DrawableRes val unselectedIconRes: Int
    )

    private val navItems by lazy {
        listOf(
            BottomNavItem(
                binding.navFile,
                binding.navFileIcon,
                binding.navFileLabel,
                R.drawable.ic_main_navigation_file_24px,
                R.drawable.ic_main_navigation_file_unselected_24px
            ),
            BottomNavItem(
                binding.navRecent,
                binding.navRecentIcon,
                binding.navRecentLabel,
                R.drawable.ic_main_navigation_recent_selected_24px,
                R.drawable.ic_main_navigation_recent_24px
            ),
            BottomNavItem(
                binding.navFavorite,
                binding.navFavoriteIcon,
                binding.navFavoriteLabel,
                R.drawable.ic_main_navigation_favorite_selected_24px,
                R.drawable.ic_main_navigation_favorite_24px
            ),
            BottomNavItem(
                binding.navTools,
                binding.navToolsIcon,
                binding.navToolsLabel,
                R.drawable.ic_main_navigation_tools_selected_24px,
                R.drawable.ic_main_navigation_tools_24px
            )
        )
    }

    private var currentIndex = 0
    private var onTabSelectedListener: ((Int) -> Unit)? = null

    init {
        setupTabs()
    }

    private fun setupTabs() {
        navItems.forEachIndexed { index, item ->
            item.container.setOnClickListener {
                if (currentIndex == index) return@setOnClickListener
                updateSelectedState(index)
                onTabSelectedListener?.invoke(index)
            }
        }

        binding.bottomNav.post {
            updateSelectedState(viewPager.currentItem.coerceIn(0, navItems.lastIndex))
        }
    }

    private fun updateSelectedState(selectedIndex: Int) {
        if (selectedIndex !in navItems.indices) return

        currentIndex = selectedIndex
        val context = binding.root.context
        val selectedColor = ContextCompat.getColor(context, R.color.main_bottom_nav_selected)
        val unselectedTextColor = ContextCompat.getColor(context, R.color.main_bottom_nav_unselected_text)

        navItems.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val labelColor = if (isSelected) selectedColor else unselectedTextColor

            item.icon.imageTintList = null
            item.icon.setImageResource(if (isSelected) item.selectedIconRes else item.unselectedIconRes)
            item.label.setTextColor(labelColor)
            item.label.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            item.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    fun setSelectedTab(index: Int) {
        updateSelectedState(index)
    }

    fun getCurrentTab(): Int {
        return viewPager.currentItem
    }

    fun show() {
        binding.bottomNav.visibility = View.VISIBLE
    }

    fun hide() {
        binding.bottomNav.visibility = View.GONE
    }
}
