package com.hq.mupdf.search

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hq.mupdf.R
import com.hq.mupdf.interfaces.PDFSearchResult

/**
 * 搜索结果列表适配器
 */
class SearchResultAdapter(
    private val onItemClick: (PDFSearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {
    
    private var searchResults: List<PDFSearchResult> = emptyList()
    private var currentQuery: String = ""
    
    /**
     * 更新搜索结果
     */
    fun updateResults(query: String, results: List<PDFSearchResult>) {
        currentQuery = query
        searchResults = results
        notifyDataSetChanged()
    }
    
    /**
     * 清除搜索结果
     */
    fun clearResults() {
        currentQuery = ""
        searchResults = emptyList()
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(searchResults[position], currentQuery)
    }
    
    override fun getItemCount(): Int = searchResults.size
    
    /**
     * 搜索结果ViewHolder
     */
    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pageNumberText: TextView = itemView.findViewById(R.id.tv_page_number)
        private val contextText: TextView = itemView.findViewById(R.id.tv_context)
        private val highlightText: TextView = itemView.findViewById(R.id.tv_highlight)
        
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(searchResults[position])
                }
            }
        }
        
        fun bind(result: PDFSearchResult, query: String) {
            // 设置页面编号
            pageNumberText.text = itemView.context.getString(R.string.page_number_format, result.displayPageNumber)
            
            // 设置上下文文本并高亮查询词
            contextText.text = highlightQueryInText(result.contextText, query)
            
            // 隐藏单独的高亮文本，因为已经在上下文中高亮了
            highlightText.visibility = View.GONE
        }
        
        /**
         * 在文本中高亮查询词
         */
        private fun highlightQueryInText(text: String, query: String): Spannable {
            val spannable = SpannableString(text)
            val normalizedText = text.lowercase()
            val normalizedQuery = query.lowercase()
            
            var startIndex = 0
            while (startIndex < normalizedText.length) {
                val index = normalizedText.indexOf(normalizedQuery, startIndex)
                if (index == -1) break
                
                val endIndex = index + query.length
                
                // 添加背景色高亮
                val highlightColor = ContextCompat.getColor(itemView.context, R.color.searchHighlight)
                spannable.setSpan(
                    BackgroundColorSpan(highlightColor),
                    index,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                // 添加粗体样式
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    index,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                startIndex = endIndex
            }
            
            return spannable
        }
    }
}
