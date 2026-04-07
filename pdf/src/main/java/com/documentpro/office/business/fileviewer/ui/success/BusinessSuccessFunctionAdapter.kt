package com.documentpro.office.business.fileviewer.ui.success

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.documentpro.office.business.fileviewer.databinding.ItemSuccessFunctionBinding
import com.documentpro.office.business.fileviewer.databinding.ItemSuccessAdBinding
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.android.common.bill.ui.NativeAdStyleType

class BusinessSuccessFunctionAdapter(
    private val list: List<BusinessSuccessFunction>,
    private val onBtnClick: (BusinessSuccessFunction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FUNCTION = 0
        private const val VIEW_TYPE_AD = 1
    }

    inner class FunctionViewHolder(val binding: ItemSuccessFunctionBinding) : RecyclerView.ViewHolder(binding.root)
    
    inner class AdViewHolder(val binding: ItemSuccessAdBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (list[position].isAd()) VIEW_TYPE_AD else VIEW_TYPE_FUNCTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AD -> {
                val binding = ItemSuccessAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding)
            }
            else -> {
                val binding = ItemSuccessFunctionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                FunctionViewHolder(binding)
            }
        }
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        
        when (holder) {
            is FunctionViewHolder -> {
                holder.binding.ivIcon.setImageResource(item.iconRes)
                holder.binding.tvTitle.text = item.title
                holder.binding.tvDesc.text = item.desc
                holder.binding.btnAction.text = item.btnText
                // 设置按钮背景色
                val bg = holder.binding.btnAction.background.mutate() as? GradientDrawable
//                bg?.setColor(item.btnColor)
                holder.binding.btnAction.setOnClickListener { onBtnClick(item) }
            }
            is AdViewHolder -> {
                // 加载原生广告
                val context = holder.binding.root.context
                if (context is FragmentActivity) {
                    context.loadNative(holder.binding.adContainer, styleType = NativeAdStyleType.LARGE)
                }
            }
        }
    }
} 