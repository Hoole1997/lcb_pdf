package com.documentpro.office.business.fileviewer.ui.process

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.documentpro.office.business.fileviewer.databinding.ItemProcessBinding

/**
 * 进程列表适配器
 */
class ProcessListAdapter(
    private val onStopClick: (ProcessInfo) -> Unit
) : RecyclerView.Adapter<ProcessListAdapter.ProcessViewHolder>() {

    val processList = mutableListOf<ProcessInfo>()

    fun submitList(list: List<ProcessInfo>) {
        processList.clear()
        processList.addAll(list)
        notifyDataSetChanged()
    }

    fun updateItem(position: Int) {
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessViewHolder {
        val binding = ItemProcessBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProcessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProcessViewHolder, position: Int) {
        holder.bind(processList[position])
    }

    override fun getItemCount(): Int = processList.size

    inner class ProcessViewHolder(
        private val binding: ItemProcessBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(processInfo: ProcessInfo) {
            binding.ivAppIcon.setImageDrawable(processInfo.icon)
            binding.tvAppName.text = processInfo.appName

            if (processInfo.isStopped) {
                // 已停止状态
                binding.btnStop.isEnabled = false
                binding.btnStop.alpha = 0.5f
                binding.tvAppName.alpha = 0.5f
                binding.ivAppIcon.alpha = 0.5f
            } else {
                // 运行中状态
                binding.btnStop.isEnabled = true
                binding.btnStop.alpha = 1f
                binding.tvAppName.alpha = 1f
                binding.ivAppIcon.alpha = 1f
                
                binding.btnStop.setOnClickListener {
                    onStopClick(processInfo)
                }
            }
        }
    }
}

