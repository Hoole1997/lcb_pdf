package com.documentpro.office.business.fileviewer.ui.home

import androidx.lifecycle.MutableLiveData
import com.documentpro.office.business.fileviewer.base.BaseModel
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo

class BusinessChooseModel : BaseModel() {

    val fileInfoEvent = MutableLiveData<List<BusinessFileInfo>>()
    val allChooseEvent = MutableLiveData<Boolean>(false)

    fun submitFileList(fileInfoList: List<BusinessFileInfo>) {
        fileInfoEvent.postValue(fileInfoList)
    }

    fun switchAllChoose() {
        val allChoose = allChooseEvent.value ?:false
        allChooseEvent.postValue(!allChoose)
    }

}