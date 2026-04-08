package com.documentpro.office.business.fileviewer.ui.language

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.ItemLanguageBinding
import com.documentpro.office.business.fileviewer.databinding.ItemLanguageHeaderBinding
import net.corekit.core.utils.BusinessLanguageController
import com.murgupluoglu.flagkit.FlagKit
import kotlin.code

class BusinessLanguageAdapter(
    private val languages: MutableList<BusinessLanguageItem>,
    private val onItemClick:(BusinessLanguageItem)->Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(languageItem: BusinessLanguageItem) {
            binding.tvLanguageName.text = languageItem.nativeName
            binding.ivSelectedIndicator.setImageResource(
                if (languageItem.isSelected) R.drawable.ic_language_selected_indicator
                else 0
            )
            binding.tvLanguageName.setTextColor(
                if (languageItem.isSelected) ContextCompat.getColor(binding.root.context, R.color.theme_color)
                else ContextCompat.getColor(binding.root.context, R.color.black)
            )
            // 使用 FlagKit 设置国旗图标
            try {
                val countryCode = BusinessLanguageController.getInstance().getCountryCode(languageItem.code)
                val resourceId = FlagKit.getResId(binding.root.context, countryCode)
                binding.ivCountryFlag.setImageResource(resourceId)
            } catch (e: Exception) {
                // 如果找不到对应的国旗资源，设置默认图标
                binding.ivCountryFlag.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // 设置选中项的渐变背景
            if (languageItem.isSelected) {
                binding.llLanguageItem.setBackgroundResource(R.drawable.bg_lang_item_primary)
            } else {
                binding.llLanguageItem.setBackgroundResource(R.drawable.bg_lang_item_secondary)
            }

            binding.llLanguageItem.setOnClickListener {
                languages.forEach { it.isSelected = false }
                languageItem.isSelected = true
                notifyDataSetChanged()
                onItemClick.invoke(languageItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LanguageViewHolder -> holder.bind(languages[position] as BusinessLanguageItem)
        }
    }

    override fun getItemCount(): Int = languages.size

    /**
     * 获取选中的语言
     */
    fun getSelectedLanguage(): BusinessLanguageItem? {
        return languages.find { it.isSelected }
    }

    /**
     * 设置默认选中语言
     */
    fun setDefaultLanguage(languageCode: String) {
        languages.forEach { it.isSelected = it.code == languageCode }
        notifyDataSetChanged()
    }

} 
