package com.documentpro.office.business.fileviewer.ui.tool

import android.content.Context
import android.view.ViewGroup
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.DataBindingHolder
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.ItemUtilsBinding

class BusinessToolsAdapter : BaseQuickAdapter<Tools, DataBindingHolder<ItemUtilsBinding>>() {
    override fun onBindViewHolder(
        holder: DataBindingHolder<ItemUtilsBinding>,
        position: Int,
        item: Tools?
    ) {
        item?.let {
            holder.binding.apply {
                ivTool.setImageResource(it.iconRes)
                tvToolName.text = it.toolName
            }
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): DataBindingHolder<ItemUtilsBinding> {
        return DataBindingHolder(R.layout.item_utils,parent)
    }


}

data class Tools(
    val iconRes:Int,
    val toolName: String,
    val toolType: ToolType
)

enum class ToolType {
    IMPORT_FILE,
    SCAN_PDF,
    LOCK_PDF,
    UNLOCK_PDF,
    IMAGE_PDF,
    FILE_CLEAN,
    PRINT_PDF,
    ADD_MODEL,
    MERGE_PDF,
    PROCES_MGR,
    JUNK_CLEANER,
}