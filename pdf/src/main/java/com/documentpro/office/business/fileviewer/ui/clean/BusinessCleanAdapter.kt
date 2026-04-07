package com.documentpro.office.business.fileviewer.ui.clean

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.ItemCleanGroupBinding
import com.documentpro.office.business.fileviewer.databinding.ItemCleanItemBinding
import com.documentpro.office.business.fileviewer.utils.toFileSizeString
import kotlinx.coroutines.flow.StateFlow

class BusinessCleanAdapter(
    private val onGroupSelected: (CleanType) -> Unit,
    private val onItemSelected: (CleanType, Long) -> Unit,
    private val scanProgress: StateFlow<Int>
) : BaseExpandableListAdapter() {

    var groups: List<CleanGroup> = emptyList()
    private var isScanning: Boolean = false
    private val loadingAnimators = mutableMapOf<Int, ObjectAnimator>()

    fun setData(newGroups: List<CleanGroup>) {
        groups = newGroups
        notifyDataSetChanged()
    }

    fun setScanning(scanning: Boolean) {
        isScanning = scanning
        if (!scanning) {
            // 停止所有动画
            loadingAnimators.values.forEach { it.cancel() }
            loadingAnimators.clear()
        }
        notifyDataSetChanged()
    }

    override fun getGroupCount(): Int = groups.size

    override fun getChildrenCount(groupPosition: Int): Int = groups[groupPosition].items.size

    override fun getGroup(groupPosition: Int): CleanGroup = groups[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): CleanItem = 
        groups[groupPosition].items[childPosition]

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = true

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val binding = convertView?.let { ItemCleanGroupBinding.bind(it) }
            ?: ItemCleanGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        
        val group = getGroup(groupPosition)
        binding.tvTitle.text = group.type.title
        binding.tvSize.text = group.items.sumOf { it.size }.toFileSizeString()
        binding.icon.setImageResource(when(group.type){
            CleanType.OBSOLETE_APK->R.drawable.ic_junk_apk
            CleanType.JUNK->R.drawable.ic_junk_junk
            CleanType.TEMP->R.drawable.ic_junk_temp
            CleanType.LOG->R.drawable.ic_junk_log
        })

        // 设置箭头旋转动画
        val arrow = binding.ivArrow
        val rotation = if (isExpanded) 90f else 0f
        arrow.animate()
            .rotation(rotation)
            .setDuration(200)
            .start()

        // 设置标题区域点击事件
        binding.llTitle.setOnClickListener {
            val expandableListView = parent as? android.widget.ExpandableListView
            if (expandableListView?.isGroupExpanded(groupPosition) == true) {
                expandableListView.collapseGroup(groupPosition)
            } else {
                expandableListView?.expandGroup(groupPosition)
            }
        }

        // 根据扫描状态显示不同的内容
        if (isScanning) {
            binding.cbSelect.visibility = View.GONE
            binding.ivLoading.visibility = View.VISIBLE
            binding.ivLoading.setImageResource(R.mipmap.ic_clean_loading)
            
            // 停止之前的动画
            loadingAnimators[groupPosition]?.cancel()
            
            // 创建新的旋转动画
            loadingAnimators[groupPosition] = ObjectAnimator.ofFloat(binding.ivLoading, "rotation", 0f, 360f).apply {
                duration = 600 // 加快动画速度
                repeatCount = ObjectAnimator.INFINITE
                start()
            }

            // 根据扫描进度设置动画速度
            val progress = scanProgress.value
            val speed = when {
                progress < 25 -> 600L  // 开始阶段
                progress < 50 -> 500L  // 加速阶段
                progress < 75 -> 400L  // 更快阶段
                else -> 300L          // 最快阶段
            }
            loadingAnimators[groupPosition]?.duration = speed
        } else {
            binding.cbSelect.visibility = View.VISIBLE
            binding.ivLoading.visibility = View.GONE
            binding.cbSelect.setImageResource(
                if (group.isAllSelected) R.mipmap.ic_checkbox_selected
                else R.mipmap.ic_checkbox_unselected
            )
            binding.cbSelect.setOnClickListener {
                onGroupSelected(group.type)
            }
        }

        return binding.root
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val binding = convertView?.let { ItemCleanItemBinding.bind(it) }
            ?: ItemCleanItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        
        val item = getChild(groupPosition, childPosition)
        binding.tvName.text = item.name
        binding.tvPath.text = item.path
        binding.tvSize.text = item.size.toFileSizeString()
        
        binding.cbSelect.setImageResource(
            if (item.isSelected) R.mipmap.ic_checkbox_selected
            else R.mipmap.ic_checkbox_unselected
        )

        binding.root.setOnClickListener {
            onItemSelected(item.type, item.id)
        }

        return binding.root
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true
} 