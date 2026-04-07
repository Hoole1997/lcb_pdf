package com.documentpro.office.business.fileviewer.utils.queryfile

import com.documentpro.office.business.fileviewer.R

enum class BusinessSortType(val titleRes: Int, val subRes: Int) {
    MODIFIED_DESC(R.string.main_home_sort_modified, R.string.main_home_sort_modified_desc),
    MODIFIED_ASC(R.string.main_home_sort_modified, R.string.main_home_sort_modified_asc),
    NAME_ASC(R.string.main_home_sort_name, R.string.main_home_sort_name_asc), 
    NAME_DESC(R.string.main_home_sort_name, R.string.main_home_sort_name_desc),
    SIZE_DESC(R.string.main_home_sort_size, R.string.main_home_sort_size_desc),
    SIZE_ASC(R.string.main_home_sort_size, R.string.main_home_sort_size_asc)
} 