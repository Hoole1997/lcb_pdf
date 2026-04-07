package com.documentpro.office.business.fileviewer.base

import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.LogUtils

open class BaseLazyFragment<DB : ViewBinding, VM : ViewModel> : BaseFragment<DB, VM>() {

    // 是否已经加载过数据
    var isDataLoaded = false

    override fun initBinding(): DB {
        return binding
    }

    override fun initView() {

    }

    override fun initViewModel(): VM? {
        return model
    }

    override fun initObserve() {

    }

    override fun onResume() {
        super.onResume()
        // 当Fragment显示时加载数据
        if (!isDataLoaded && isVisible) {
            lazyLoad()
            isDataLoaded = true
        }
    }

    //使用viewpager配合BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT使用
    open fun lazyLoad() {
        LogUtils.d("懒加载")
    }
}