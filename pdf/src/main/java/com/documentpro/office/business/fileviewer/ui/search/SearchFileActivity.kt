package com.documentpro.office.business.fileviewer.ui.search

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivitySearchFileBinding
import com.documentpro.office.business.fileviewer.ui.home.BusinessFileListFragment
import com.documentpro.office.business.fileviewer.ui.home.BusinessHomeModel
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType
import com.documentpro.office.business.fileviewer.utils.queryfile.notifySystemToScan
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import com.documentpro.office.business.fileviewer.utils.loadInterstitial

class SearchFileActivity : BaseActivity<ActivitySearchFileBinding, BusinessHomeModel>() {

    companion object {
        private const val TAG = "SearchFileActivity"
    }

    private val mField_1 = arrayListOf<BusinessFileListFragment>()
    private var mField_2: String = ""

    override fun initBinding(): ActivitySearchFileBinding {
        return ActivitySearchFileBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessHomeModel {
        return viewModels<BusinessHomeModel>().value
    }

    override fun finish() {
        loadInterstitial {
            super.finish()
        }
    }

    override fun initView() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        useDefaultToolbar(binding.toolbar,"")

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

            }

            override fun afterTextChanged(s: Editable?) {
                val keyword = s?.toString() ?: ""
mField_2 = keyword
                val currentFragment = mField_1.getOrNull(binding.vpPage.currentItem)
                currentFragment?.filterByKeyword(keyword)
            }
        })

        initTab()

        // 请求焦点并弹出键盘
        binding.etSearch.post { // 使用post确保视图已经布局完成
            binding.etSearch.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun initTab() {
        val tabTitleList = arrayListOf(
            BusinessFileType.PDF.name,
            BusinessFileType.WORD.name,
            BusinessFileType.EXCEL.name,
            BusinessFileType.PPT.name,
            BusinessFileType.TXT.name,
            BusinessFileType.IMAGE.name
        )
        mField_1.clear()
        tabTitleList.forEachIndexed { index, text ->
            val tab = binding.tab.newTab()
            tab.text = text
            binding.tab.addTab(tab)
            mField_1.add(BusinessFileListFragment.newInstance(text))
        }
        binding.vpPage.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment =mField_1[position]
            override fun getItemCount(): Int = mField_1.size
        }
        binding.vpPage.offscreenPageLimit = mField_1.size

        binding.tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // 同步ViewPager
                val pos = binding.tab.selectedTabPosition
                if (binding.vpPage.currentItem != pos) {
                    binding.vpPage.currentItem = pos
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
                binding.vpPage.post {
                    // 当页面切换时，将当前关键字传递给新的 Fragment
                    val selectedFragment = mField_1.getOrNull(position)
                    selectedFragment?.filterByKeyword(mField_2)
                }
            }
        })

        binding.vpPage.post {
            // 默认选中第一个Tab并高亮
            binding.vpPage.currentItem = 0
        }
    }

    override fun initObserve() {

    }

    override fun initTag(): String {
        return TAG
    }

    override fun onResume() {
        super.onResume()
        notifySystemToScan()
    }
}
