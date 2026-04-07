package com.hq.mupdf.bottombar

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView

import com.hq.mupdf.R

/**
 * PDF底部栏管理类
 * 封装底部栏的UI操作和业务逻辑，通过接口与外部交互
 */
class PDFBottomBar(
    private val activity: Activity,
    private val rootView: View,
    private val listener: PDFBottomBarListener?
) {
    

    
    // 底部栏控件
    private val prevPageBtn: ImageButton by lazy { rootView.findViewById(R.id.prevPageBtn) }
    private val nextPageBtn: ImageButton by lazy { rootView.findViewById(R.id.nextPageBtn) }
    private val pageInput: EditText by lazy { rootView.findViewById(R.id.pageInput) }
    private val pageTotal: TextView by lazy { rootView.findViewById(R.id.pageTotal) }
    private val thumbnailToggle: ImageButton by lazy { rootView.findViewById(R.id.thumbnailToggle) }
    private val directionToggleBtn: ImageButton by lazy { rootView.findViewById(R.id.directionToggleBtn) }
    
    // 状态管理
    private var currentPage: Int = 0
    private var totalPages: Int = 0
    private var isPageInputChanging: Boolean = false
    private var isHorizontalDirection: Boolean = true // 默认为横向
    
    init {
        initializeBottomBarControls()
        // 设置缩略图按钮的初始样式
        updateThumbnailButtonState(true) // 默认显示状态
        // 设置方向按钮的初始状态
        updateDirectionButtonState(isHorizontalDirection)
    }
    
    /**
     * 初始化底部栏控件和事件监听
     */
    private fun initializeBottomBarControls() {
        setupNavigationButtons()
        setupPageInput()
        setupControlButtons()
    }
    
    /**
     * 设置导航按钮事件
     */
    private fun setupNavigationButtons() {
        prevPageBtn.setOnClickListener {
            listener?.onPreviousPage(currentPage)
        }
        
        nextPageBtn.setOnClickListener {
            listener?.onNextPage(currentPage)
        }
    }
    
    /**
     * 设置页码输入框事件
     */
    private fun setupPageInput() {
        pageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isPageInputChanging) return
                
                val pageText = s.toString()
                if (pageText.isNotEmpty()) {
                    try {
                        val pageNum = pageText.toInt()
                        if (pageNum in 1..totalPages) {
                            val success = listener?.onPageJump(pageNum, totalPages) ?: false
                            if (!success) {
                                // 恢复到当前页码并保持光标位置
                                restorePageInputWithCursor()
                            } else {
                                // 成功跳转后，将光标定位到文本末尾
                                pageInput.post {
                                    pageInput.setSelection(pageInput.text.length)
                                }
                            }
                        } else {
                            // 恢复到当前页码并保持光标位置
                            restorePageInputWithCursor()
                        }
                    } catch (e: NumberFormatException) {
                        // 恢复到当前页码并保持光标位置
                        restorePageInputWithCursor()
                    }
                }
            }
        })
        
        // 页码输入框选中时全选文本
        pageInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                pageInput.selectAll()
            }
        }
        
        // 设置键盘事件监听
        setupPageInputKeyListener()
        
        // 设置输入法动作监听
        setupPageInputEditorActionListener()
    }
    
    /**
     * 设置页码输入框的键盘事件监听
     */
    private fun setupPageInputKeyListener() {
        pageInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                        // 确认输入并失去焦点
                        confirmPageInput()
                        true
                    }
                    KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
                        // 取消输入，恢复原值并失去焦点
                        cancelPageInput()
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    }
    
    /**
     * 设置页码输入框的输入法动作监听
     */
    private fun setupPageInputEditorActionListener() {
        pageInput.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT -> {
                    confirmPageInput()
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * 确认页码输入
     */
    private fun confirmPageInput() {
        val pageText = pageInput.text.toString()
        if (pageText.isNotEmpty()) {
            try {
                val pageNum = pageText.toInt()
                if (pageNum in 1..totalPages) {
                    listener?.onPageJump(pageNum, totalPages)
                }
            } catch (e: NumberFormatException) {
                // 输入无效，恢复原值
                restorePageInputWithCursor()
            }
        }
        // 清除焦点并隐藏键盘
        pageInput.clearFocus()
        hideKeyboard()
    }
    
    /**
     * 取消页码输入
     */
    private fun cancelPageInput() {
        // 恢复到当前页码
        restorePageInputWithCursor()
        // 清除焦点并隐藏键盘
        pageInput.clearFocus()
        hideKeyboard()
    }
    
    /**
     * 恢复页码输入框内容并优化光标位置
     */
    private fun restorePageInputWithCursor() {
        isPageInputChanging = true
        val currentPageText = (currentPage + 1).toString()
        pageInput.setText(currentPageText)
        // 将光标定位到文本末尾
        pageInput.post {
            pageInput.setSelection(currentPageText.length)
        }
        isPageInputChanging = false
    }
    
    /**
     * 设置控制按钮事件
     */
    private fun setupControlButtons() {
        thumbnailToggle.setOnClickListener {
            listener?.onThumbnailToggle()
        }
        
        directionToggleBtn.setOnClickListener {
            toggleDirection()
        }
    }
    
    /**
     * 设置当前页码和总页数
     * @param currentPage 当前页码（从0开始）
     * @param totalPages 总页数
     */
    fun setCurrentPage(currentPage: Int, totalPages: Int) {
        this.currentPage = currentPage
        this.totalPages = totalPages
        
        // 更新页码输入框（避免触发TextWatcher）并优化光标位置
        isPageInputChanging = true
        val currentPageText = (currentPage + 1).toString()
        pageInput.setText(currentPageText)
        // 如果输入框有焦点，将光标定位到文本末尾
        if (pageInput.hasFocus()) {
            pageInput.post {
                pageInput.setSelection(currentPageText.length)
            }
        }
        isPageInputChanging = false
        
        // 更新总页数显示
        pageTotal.text = totalPages.toString()
        
        // 更新按钮状态
        updateNavigationButtonsState()
    }
    
    /**
     * 获取当前页码（从0开始）
     */
    fun getCurrentPage(): Int = currentPage
    
    /**
     * 获取总页数
     */
    fun getTotalPages(): Int = totalPages
    
    /**
     * 跳转到第一页
     */
    fun goToFirstPage() {
        if (totalPages > 0) {
            listener?.onPageJump(1, totalPages)
        }
    }
    
    /**
     * 跳转到最后一页
     */
    fun goToLastPage() {
        if (totalPages > 0) {
            listener?.onPageJump(totalPages, totalPages)
        }
    }
    
    /**
     * 跳转到指定页面
     * @param pageNumber 页码（从1开始）
     * @return 是否成功跳转
     */
    fun jumpToPage(pageNumber: Int): Boolean {
        if (pageNumber in 1..totalPages) {
            return listener?.onPageJump(pageNumber, totalPages) ?: false
        }
        return false
    }
    
    /**
     * 更新导航按钮状态
     */
    private fun updateNavigationButtonsState() {
        // 更新按钮启用状态
        prevPageBtn.isEnabled = currentPage > 0
        nextPageBtn.isEnabled = currentPage < totalPages - 1
        
        // 更新按钮透明度
        prevPageBtn.alpha = if (currentPage > 0) 1.0f else 0.5f
        nextPageBtn.alpha = if (currentPage < totalPages - 1) 1.0f else 0.5f
    }
    
    /**
     * 显示底部栏
     */
    fun showBottomBar() {
        rootView.findViewById<View>(R.id.pdfControls)?.visibility = View.VISIBLE
        listener?.onBottomBarVisibilityChanged(true)
    }
    
    /**
     * 隐藏底部栏
     */
    fun hideBottomBar() {
        rootView.findViewById<View>(R.id.pdfControls)?.visibility = View.GONE
        listener?.onBottomBarVisibilityChanged(false)
    }
    
    /**
     * 切换底部栏可见性
     */
    fun toggleBottomBar() {
        val bottomBar = rootView.findViewById<View>(R.id.pdfControls)
        if (bottomBar?.visibility == View.VISIBLE) {
            hideBottomBar()
        } else {
            showBottomBar()
        }
    }
    
    /**
     * 启用/禁用底部栏
     * @param enabled 是否启用
     */
    fun setBottomBarEnabled(enabled: Boolean) {
        prevPageBtn.isEnabled = enabled && currentPage > 0
        nextPageBtn.isEnabled = enabled && currentPage < totalPages - 1
        pageInput.isEnabled = enabled
        thumbnailToggle.isEnabled = enabled
        directionToggleBtn.isEnabled = enabled
        
        // 更新透明度
        val alpha = if (enabled) 1.0f else 0.5f
        rootView.findViewById<View>(R.id.pdfControls)?.alpha = alpha
    }
    
    /**
     * 重置底部栏到初始状态
     */
    fun resetBottomBar() {
        setCurrentPage(0, 0)
        setBottomBarEnabled(true)
        showBottomBar()
    }
    
    /**
     * 更新缩略图按钮状态
     * @param isVisible 缩略图是否可见
     */
    fun updateThumbnailButtonState(isVisible: Boolean) {
        try {
            // 设置按钮的激活状态，激活时显示红色背景
            thumbnailToggle.isActivated = isVisible
            
        } catch (e: Exception) {
            // Failed to update thumbnail button state
        }
    }
    
    /**
     * 获取页码输入框的当前值
     */
    fun getPageInputValue(): Int {
        return try {
            pageInput.text.toString().toInt()
        } catch (e: NumberFormatException) {
            currentPage + 1
        }
    }
    
    /**
     * 设置页码输入框焦点
     */
    fun focusPageInput() {
        pageInput.requestFocus()
        // 延迟执行全选，确保焦点已获取
        pageInput.post {
            pageInput.selectAll()
        }
    }
    
    /**
     * 清除页码输入框焦点
     */
    fun clearPageInputFocus() {
        pageInput.clearFocus()
        hideKeyboard()
    }
    
    /**
     * 隐藏软键盘
     */
    private fun hideKeyboard() {
        try {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(pageInput.windowToken, 0)
        } catch (e: Exception) {
            // Failed to hide keyboard
        }
    }
    
    /**
     * 检查是否可以导航到上一页
     */
    fun canNavigatePrevious(): Boolean = currentPage > 0
    
    /**
     * 检查是否可以导航到下一页
     */
    fun canNavigateNext(): Boolean = currentPage < totalPages - 1
    
    /**
     * 获取底部栏可见性
     */
    fun isBottomBarVisible(): Boolean {
        return rootView.findViewById<View>(R.id.pdfControls)?.visibility == View.VISIBLE
    }
    
    /**
     * 切换查看方向
     */
    private fun toggleDirection() {
        isHorizontalDirection = !isHorizontalDirection
        updateDirectionButtonState(isHorizontalDirection)
        listener?.onDirectionToggle(isHorizontalDirection)
    }
    
    /**
     * 更新方向按钮状态
     * @param isHorizontal 是否为横向模式
     */
    fun updateDirectionButtonState(isHorizontal: Boolean) {
        try {
            this.isHorizontalDirection = isHorizontal
            // 根据当前方向设置图标
            val iconRes = if (isHorizontal) {
                R.drawable.ic_direction_horizontal
            } else {
                R.drawable.ic_direction_vertical
            }
            directionToggleBtn.setImageResource(iconRes)
            
        } catch (e: Exception) {
            // Failed to update direction button state
        }
    }
    
    /**
     * 获取当前查看方向
     * @return true为横向，false为竖向
     */
    fun isHorizontalDirection(): Boolean = isHorizontalDirection
    
    /**
     * 设置查看方向
     * @param isHorizontal 是否为横向模式
     */
    fun setViewDirection(isHorizontal: Boolean) {
        if (this.isHorizontalDirection != isHorizontal) {
            this.isHorizontalDirection = isHorizontal
            updateDirectionButtonState(isHorizontal)
        }
    }
    
    /**
     * 清理资源
     * 在Activity销毁时调用
     */
    fun onDestroy() {
        // 清理可能的资源引用
    }
}
