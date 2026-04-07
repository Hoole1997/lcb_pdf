package com.hq.mupdf.thumbnail

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hq.mupdf.viewer.MuPDFCore
import com.hq.mupdf.R
import kotlinx.coroutines.*

/**
 * PDF缩略图适配器
 * 负责显示PDF页面缩略图列表
 */
class PDFThumbnailAdapter(
    private val pdfCore: MuPDFCore,
    private val onThumbnailClick: (Int) -> Unit
) : RecyclerView.Adapter<PDFThumbnailAdapter.ThumbnailViewHolder>() {
    
    companion object {
        private const val TAG = "PDFThumbnailAdapter"
        private const val THUMBNAIL_WIDTH = 120
        private const val THUMBNAIL_HEIGHT = 160
    }
    
    private val pageCount = pdfCore.countPages()
    private var currentPage = 0
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_thumbnail, parent, false)
        return ThumbnailViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bind(position)
    }
    
    override fun getItemCount(): Int = pageCount
    
    /**
     * 设置当前页面
     */
    fun setCurrentPage(page: Int) {
        val oldPage = currentPage
        currentPage = page
        
        // 更新指示器
        notifyItemChanged(oldPage)
        notifyItemChanged(currentPage)
    }
    
    /**
     * 预加载缩略图
     */
    fun preloadThumbnails(startPage: Int, endPage: Int) {
        for (page in startPage..endPage.coerceAtMost(pageCount - 1)) {
            if (page >= 0) {
                scope.launch {
                    generateThumbnail(page)
                }
            }
        }
    }
    
    inner class ThumbnailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val thumbnailImage: ImageView = itemView.findViewById(R.id.thumbnailImage)
        private val thumbnailLoading: ProgressBar = itemView.findViewById(R.id.thumbnailLoading)
        private val pageNumber: TextView = itemView.findViewById(R.id.pageNumber)
        
        fun bind(position: Int) {
            val pageNum = position + 1
            pageNumber.text = pageNum.toString()
            
            // 设置选中状态 - 对应HTML版本的active类
            val isSelected = position == currentPage
            itemView.isSelected = isSelected
            
            // 设置页码文字颜色 - 对应HTML版本的选中/未选中样式
            if (isSelected) {
                pageNumber.setTextColor(itemView.context.getColor(android.R.color.white))
            } else {
                pageNumber.setTextColor(itemView.context.getColor(R.color.text_secondary))
            }
            
            // 设置缩略图预览容器的选中状态
            val thumbnailContainer = itemView.findViewById<View>(R.id.thumbnailImage).parent as View
            thumbnailContainer.isSelected = isSelected
            
            // 当前页面指示器已移除 - 选中状态现在通过背景变化显示
            
            // 设置点击事件
            itemView.setOnClickListener {
                onThumbnailClick(position)
            }
            
            // 重置状态
            thumbnailImage.setImageBitmap(null)
            thumbnailLoading.visibility = View.VISIBLE
            
            // 异步加载缩略图
            scope.launch {
                try {
                    val bitmap = generateThumbnail(position)
                    if (bitmap != null) {
                        thumbnailImage.setImageBitmap(bitmap)
                        thumbnailLoading.visibility = View.GONE
                        // Thumbnail $pageNum loaded successfully
                    } else {
                        showThumbnailError(pageNum)
                    }
                } catch (e: Exception) {
                    // Failed to load thumbnail $pageNum
                    showThumbnailError(pageNum)
                }
            }
        }
        
        private fun showThumbnailError(pageNum: Int) {
            thumbnailLoading.visibility = View.GONE
            // TODO: 显示错误占位图
            // Thumbnail $pageNum failed to load
        }
    }
    
    /**
     * 生成PDF页面缩略图
     */
    private suspend fun generateThumbnail(pageIndex: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Generating thumbnail for page ${pageIndex + 1}
                
                // 获取页面尺寸
                val pageSize = pdfCore.getPageSize(pageIndex)
                
                // 计算缩略图尺寸，保持比例
                val aspect = if (pageSize.x > 0f) pageSize.y / pageSize.x else 1.33f
                val width = THUMBNAIL_WIDTH
                val height = (width * aspect).toInt().coerceIn(80, THUMBNAIL_HEIGHT)
                
                // 创建缩略图Bitmap
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                
                // 渲染页面到缩略图
                pdfCore.drawPage(bitmap, pageIndex, width, height, 0, 0, width, height, null)
                
                bitmap
            } catch (e: Exception) {
                // Error generating thumbnail for page ${pageIndex + 1}
                null
            }
        }
    }
    

    
    /**
     * 刷新所有缩略图
     */
    fun refreshAllThumbnails() {
        // 刷新所有缩略图
        notifyDataSetChanged()
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        scope.cancel()
    }
}
