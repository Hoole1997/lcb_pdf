package com.documentpro.office.business.fileviewer.ui.clean

import androidx.fragment.app.viewModels
import com.documentpro.office.business.fileviewer.base.BaseLazyFragment
import com.documentpro.office.business.fileviewer.databinding.FragmentCleanBinding

class BusinessCleanFragment : BaseLazyFragment<FragmentCleanBinding, BusinessCleanModel>() {

    override fun initBinding(): FragmentCleanBinding {
        return FragmentCleanBinding.inflate(layoutInflater)
    }

    override fun initViewModel(): BusinessCleanModel? {
        return viewModels<BusinessCleanModel>().value
    }

    override fun lazyLoad() {
        super.lazyLoad()

    }

}