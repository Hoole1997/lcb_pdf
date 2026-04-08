package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.documentpro.office.business.fileviewer.R
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessSortType

data class SortOption(val type: BusinessSortType)

class BusinessSortBottomSheetDialog(
    context: Context,
    private val selectedType: BusinessSortType = BusinessSortType.MODIFIED_DESC,
    private val onSortSelected: (type: BusinessSortType) -> Unit
) : BottomSheetDialog(context) {

    private val sortOptions = listOf(
        SortOption(BusinessSortType.MODIFIED_DESC),
        SortOption(BusinessSortType.MODIFIED_ASC),
        SortOption(BusinessSortType.NAME_ASC),
        SortOption(BusinessSortType.NAME_DESC),
        SortOption(BusinessSortType.SIZE_DESC),
        SortOption(BusinessSortType.SIZE_ASC)
    )

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_sort_bottom_sheet, null)
        val layoutOptions = view.findViewById<LinearLayout>(R.id.layout_options)
        val itemSpacing = context.resources.displayMetrics.density.times(20).toInt()

        sortOptions.forEachIndexed { index, option ->
            val optionView = LayoutInflater.from(context).inflate(R.layout.item_sort_option, layoutOptions, false)
            val tvTitle = optionView.findViewById<TextView>(R.id.tv_option_title)
            val tvSub = optionView.findViewById<TextView>(R.id.tv_option_sub)
            val ivCheck = optionView.findViewById<ImageView>(R.id.iv_check)

            (optionView.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
                params.topMargin = if (index == 0) 0 else itemSpacing
                optionView.layoutParams = params
            }

            tvTitle.text = context.getString(option.type.titleRes)
            tvSub.text = context.getString(option.type.subRes)
            tvTitle.isSelected = option.type == selectedType
            optionView.isSelected = option.type == selectedType
            tvTitle.setTextColor(ContextCompat.getColor(context, if (option.type == selectedType) R.color.main_home_sort_item_select else R.color.main_home_sort_item_normal))
            tvSub.setTextColor(ContextCompat.getColor(context, if (option.type == selectedType) R.color.main_home_sort_item_select else R.color.main_home_sort_item_normal))
            ivCheck.isVisible = option.type == selectedType
            optionView.setOnClickListener {
                onSortSelected(option.type)
                dismiss()
            }
            layoutOptions.addView(optionView)
        }

        setContentView(view)
    }
} 
