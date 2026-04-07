package com.documentpro.office.business.fileviewer.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.TimeUtils
import com.chad.library.adapter4.viewholder.DataBindingHolder
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.toFileSizeString
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import com.documentpro.office.business.fileviewer.R
import com.chad.library.adapter4.BaseDifferAdapter
import com.documentpro.office.business.fileviewer.databinding.ItemFileBinding
import com.documentpro.office.business.fileviewer.utils.loadNative

class BusinessFileAdapter(val chooseMode: Boolean) : BaseDifferAdapter<BusinessFileInfo, DataBindingHolder<ItemFileBinding>>(FileDiffCallback()) {
    private var highlightKeyword: String = ""

    init {
        // 启用动画效果
        animationEnable = true
        setItemAnimation(AnimationType.SlideInBottom)
    }
    
    /**
     * 自定义DiffUtil回调，用于文件列表的差异计算
     */
    private class FileDiffCallback : DiffUtil.ItemCallback<BusinessFileInfo>() {
        override fun areItemsTheSame(oldItem: BusinessFileInfo, newItem: BusinessFileInfo): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: BusinessFileInfo, newItem: BusinessFileInfo): Boolean {
            return oldItem.name == newItem.name && 
                oldItem.size == newItem.size && 
                oldItem.isLocked == newItem.isLocked &&
                oldItem.select == newItem.select &&
                oldItem.dateCreated == newItem.dateCreated
        }
    }

    fun setHighlightKeyword(keyword: String) {
        val oldKeyword = highlightKeyword
        highlightKeyword = keyword
        // 只有关键词变化时才刷新
        if (oldKeyword != keyword) {
            notifyDataSetChanged()
        }
    }

    override fun onBindViewHolder(
        holder: DataBindingHolder<ItemFileBinding>,
        position: Int,
        item: BusinessFileInfo?
    ) {
        item?.let {
            holder.binding.apply {
                // 判断是否为广告占位符
                if (it.isAd()) {
                    // 显示广告容器，隐藏文件信息
                    adContainer.isVisible = true
                    fileInfoContainer.isGone = true
                    
                    // 加载原生广告
                    val ctx = context
                    if (ctx is FragmentActivity) {
                        ctx.loadNative(adContainer)
                    }
                } else {
                    // 隐藏广告容器，显示文件信息
                    adContainer.isGone = true
                    fileInfoContainer.isVisible = true
                    
                    // 显示文件信息
                    ivIcon.setImageResource(it.icon())
                    if (highlightKeyword.isNotBlank() && it.name.contains(highlightKeyword, true)) {
                        val start = it.name.indexOf(highlightKeyword, ignoreCase = true)
                        val end = start + highlightKeyword.length
                        val spannable = SpannableString(it.name)
                        spannable.setSpan(
                            ForegroundColorSpan(ContextCompat.getColor(root.context, R.color.main_home_tab_selected)),
                            start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        tvFileName.text = spannable
                    } else {
                        tvFileName.text = it.name
                    }
                    tvFileSize.text = it.size.toFileSizeString()
                    it.dateCreated?.let { dateCreated ->
                        tvFileCreateTime.text = TimeUtils.millis2String(dateCreated*1000)
                    }
                    if (chooseMode) {
                        if (it.isLocked) {
                            ivLock.isVisible = true
                            ivChoose.isGone = true
                        } else {
                            ivLock.isGone = true
                            ivChoose.isVisible = true
                        }
                        ivMore.isGone = true
                        ivChoose.setImageResource(if (it.select) R.mipmap.ic_checkbox_selected else R.mipmap.ic_checkbox_unselected)
                    } else {
                        ivLock.isVisible = it.isLocked
                        ivMore.isGone = it.isLocked
                        ivChoose.isVisible = false
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): DataBindingHolder<ItemFileBinding> {
        return DataBindingHolder(R.layout.item_file, parent)
    }
}