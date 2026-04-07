package com.documentpro.office.business.fileviewer.ui.splash

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.documentpro.office.business.fileviewer.R

class BusinessGuidePagerAdapter(private val pages: List<BusinessGuidePage>) :
    RecyclerView.Adapter<BusinessGuidePagerAdapter.GuideViewHolder>() {

    class GuideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivGuide: ImageView = itemView.findViewById(R.id.iv_guide)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvContent: TextView = itemView.findViewById(R.id.tv_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guide_page, parent, false)
        return GuideViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        val page = pages[position]
        holder.ivGuide.setImageResource(page.imageRes)
        holder.tvTitle.text = page.title
        holder.tvContent.text = page.content
    }

    override fun getItemCount(): Int = pages.size
} 