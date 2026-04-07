package com.documentpro.office.business.fileviewer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ResourceUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.noober.background.drawable.DrawableCreator
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ui.pdf.model.BusinessPdfPageInfo
import com.documentpro.office.business.fileviewer.databinding.ItemPdfPageGridBinding


/**
 * PDF页面网格适配器 - 简洁现代化设计
 */
class BusinessPdfPageGridAdapter(
    private val defaultSelected: Boolean,
    private val onSelectionChanged: (BusinessPdfPageInfo, Boolean) -> Unit
) : ListAdapter<BusinessPdfPageInfo, BusinessPdfPageGridAdapter.ViewHolder>(PdfPageDiffCallback()) {

    inner class ViewHolder(private val binding: ItemPdfPageGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pageInfo: BusinessPdfPageInfo) {
            binding.apply {
                // 设置页面编号
                tvPageNumber.text = String.format("%02d", pageInfo.pageNumber)

                // 加载缩略图
                loadThumbnail(pageInfo)
                
                // 设置选中状态
                updateSelectionState(pageInfo.isSelected)

                // 设置点击事件
                cardContainer.setOnClickListener {
                    toggleSelection(pageInfo)
                }
            }
        }
        
        /**
         * 加载缩略图
         */
        private fun loadThumbnail(pageInfo: BusinessPdfPageInfo) {
            binding.apply {
                if (pageInfo.thumbnail != null) {
                    Glide.with(ivThumbnail.context)
                        .load(pageInfo.thumbnail)
                        .transform(CenterCrop(), RoundedCorners(10))
                        .placeholder(R.drawable.ic_pdf_placeholder)
                        .error(R.drawable.ic_pdf_placeholder)
                        .into(ivThumbnail)
                } else {
                    ivThumbnail.setImageResource(R.drawable.ic_pdf_placeholder)
                }
            }
        }
        
        /**
         * 切换选择状态
         */
        private fun toggleSelection(pageInfo: BusinessPdfPageInfo) {
            val newSelectedState = !pageInfo.isSelected
            pageInfo.isSelected = newSelectedState
            updateSelectionState(newSelectedState)
            onSelectionChanged(pageInfo, newSelectedState)
        }
        
        /**
         * 更新选择状态
         */
        private fun updateSelectionState(selected: Boolean) {
            binding.apply {
                ivSelectionIcon.isVisible = selected
                val context = cardContainer.context
                val drawable = DrawableCreator.Builder().setCornersRadius(ConvertUtils.dp2px(10f).toFloat())
                    .setStrokeColor(if (selected) context.getColor(R.color.theme_color) else context.getColor(R.color.divider))
                    .setStrokeWidth(ConvertUtils.dp2px(0.7f).toFloat())
                    .build()
                cardContainer.background = drawable
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPdfPageGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 获取所有选中的页面
     */
    fun getSelectedPages(): List<BusinessPdfPageInfo> {
        return currentList.filter { it.isSelected }
    }

    /**
     * 全选/取消全选
     */
    fun selectAll(selected: Boolean) {
        currentList.forEach { pageInfo ->
            if (pageInfo.isSelected != selected) {
                pageInfo.isSelected = selected
            }
        }
        notifyDataSetChanged()
    }

    /**
     * 获取选中页面数量
     */
    fun getSelectedCount(): Int {
        return currentList.count { it.isSelected }
    }

    private class PdfPageDiffCallback : DiffUtil.ItemCallback<BusinessPdfPageInfo>() {
        override fun areItemsTheSame(oldItem: BusinessPdfPageInfo, newItem: BusinessPdfPageInfo): Boolean {
            return oldItem.getUniqueId() == newItem.getUniqueId()
        }

        override fun areContentsTheSame(oldItem: BusinessPdfPageInfo, newItem: BusinessPdfPageInfo): Boolean {
            return oldItem == newItem
        }
    }
}