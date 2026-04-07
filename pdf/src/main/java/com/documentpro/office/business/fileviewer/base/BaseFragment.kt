package com.documentpro.office.business.fileviewer.base

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.ActivityUtils

abstract class BaseFragment<DB : ViewBinding, VM : ViewModel> : Fragment() {

    lateinit var binding: DB
    var model: VM? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = initBinding()
        model = initViewModel()
        initView()
        initObserve()
        return binding.root
    }

    abstract fun initBinding(): DB
    abstract fun initView()
    abstract fun initViewModel(): VM?
    abstract fun initObserve()

    private fun activityActive(): Boolean {
        return !(activity == null || activity?.isDestroyed == true || activity?.isFinishing == true)
    }

    fun safetyContext(): Activity? {
        if (activityActive()) {
            return activity
        } else {
            return ActivityUtils.getTopActivity()
        }
    }

}