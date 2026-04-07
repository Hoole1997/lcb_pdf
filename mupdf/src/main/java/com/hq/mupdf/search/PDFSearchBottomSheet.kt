package com.hq.mupdf.search

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hq.mupdf.R
import com.hq.mupdf.interfaces.PDFSearchResult
import com.hq.mupdf.interfaces.PDFSearchListener

/**
 * PDF搜索BottomSheet对话框
 */
class PDFSearchBottomSheet : BottomSheetDialogFragment(), PDFSearchListener {
    
    companion object {
        
        fun newInstance(): PDFSearchBottomSheet {
            return PDFSearchBottomSheet()
        }
    }
    
    // UI组件
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageView
    private lateinit var clearButton: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    
    // 键盘监听相关
    private var rootView: View? = null
    private var originalBottomSheetHeight = 0
    private var keyboardHeight = 0
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    
    // 搜索结果适配器
    private lateinit var searchAdapter: SearchResultAdapter
    
    // 搜索控制器（由父Activity设置）
    var searchController: ((String, PDFSearchListener) -> Unit)? = null
    var navigationController: ((PDFSearchResult) -> Unit)? = null
    var stopSearchController: (() -> Unit)? = null
    
    // 搜索状态
    private var isSearching = false
    private var currentQuery = ""
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        
        // 设置BottomSheet的最大高度为屏幕的2/5
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                setupBottomSheetBehavior(behavior, it)
            }
        }
        
        return dialog
    }
    
    private fun setupBottomSheetBehavior(behavior: BottomSheetBehavior<FrameLayout>, bottomSheet: FrameLayout) {
        // 获取屏幕高度
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        // 计算最大高度为屏幕的3/5
        val maxHeight = (screenHeight * 0.6).toInt()
        
        // 设置最大高度
        behavior.maxHeight = maxHeight
        
        // 设置初始状态
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        behavior.isHideable = true
        behavior.skipCollapsed = false
        
        // 设置初始高度为最大高度的80%，确保有良好的显示效果
        val peekHeight = (maxHeight * 0.8).toInt()
        behavior.peekHeight = peekHeight
        
        // 更新BottomSheet的布局参数
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = maxHeight
        bottomSheet.layoutParams = layoutParams
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_search, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        rootView = view
        initViews(view)
        setupRecyclerView()
        setupListeners()
        setupKeyboardListener()
        
        // 确保布局在视图创建后能正确应用高度限制
        view.post {
            adjustLayoutForScreenSize()
        }
    }
    
    /**
     * 根据屏幕尺寸调整布局
     */
    private fun adjustLayoutForScreenSize() {
        dialog?.let { dialog ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                // 重新应用高度设置，确保在布局完成后生效
                setupBottomSheetBehavior(behavior, it)
            }
        }
    }
    
    /**
     * 初始化视图组件
     */
    private fun initViews(view: View) {
        searchInput = view.findViewById(R.id.et_search_input)
        searchButton = view.findViewById(R.id.iv_search)
        clearButton = view.findViewById(R.id.iv_clear)
        progressBar = view.findViewById(R.id.progress_bar)
        statusText = view.findViewById(R.id.tv_status)
        resultsRecyclerView = view.findViewById(R.id.rv_search_results)
        emptyView = view.findViewById(R.id.tv_empty)
        
        // 初始状态
        updateSearchButton()
        updateClearButton()
        showEmptyView()
    }
    
    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        searchAdapter = SearchResultAdapter { result ->
            onSearchResultClick(result)
        }
        
        resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 搜索输入框文本变化监听
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSearchButton()
                updateClearButton()
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 软键盘搜索按钮监听
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (isSearching) {
                    stopSearch()
                } else {
                    startSearch()
                }
                true
            } else {
                false
            }
        }
        
        // 搜索按钮点击
        searchButton.setOnClickListener {
            if (isSearching) {
                stopSearch()
            } else {
                startSearch()
            }
        }
        
        // 清除按钮点击
        clearButton.setOnClickListener {
            clearSearch()
        }
    }
    
    /**
     * 设置键盘监听
     */
    private fun setupKeyboardListener() {
        rootView?.let { root ->
            globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                val rect = Rect()
                root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = root.height
                val visibleHeight = rect.height()
                val currentKeyboardHeight = screenHeight - visibleHeight
                
                // 键盘高度变化
                if (currentKeyboardHeight != keyboardHeight) {
                    keyboardHeight = currentKeyboardHeight
                    adjustForKeyboard(currentKeyboardHeight > 0)
                }
            }
            root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        }
    }
    
    /**
     * 隐藏软键盘并清除输入框焦点
     */
    private fun hideKeyboardAndClearFocus() {
        try {
            // 隐藏软键盘
            val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(searchInput.windowToken, 0)
            
            // 清除输入框焦点
            searchInput.clearFocus()
            
        } catch (e: Exception) {
            // Error hiding keyboard
        }
    }
    
    /**
     * 根据键盘状态调整布局
     */
    private fun adjustForKeyboard(isKeyboardVisible: Boolean) {
        dialog?.let { dialog ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                
                if (isKeyboardVisible) {
                    // 键盘弹出时，调整BottomSheet高度和位置
                    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                    val adjustedHeight = screenHeight - keyboardHeight
                    val maxHeight = (adjustedHeight * 0.8).toInt() // 占剩余空间的80%
                    
                    // 设置调整后的高度
                    behavior.maxHeight = maxHeight
                    val layoutParams = sheet.layoutParams
                    layoutParams.height = maxHeight
                    sheet.layoutParams = layoutParams
                    
                    // 确保BottomSheet展开到适当位置
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    // 键盘收起时，恢复原始高度
                    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                    val maxHeight = (screenHeight * 0.6).toInt() // 恢复到3/5高度
                    
                    behavior.maxHeight = maxHeight
                    val layoutParams = sheet.layoutParams
                    layoutParams.height = maxHeight
                    sheet.layoutParams = layoutParams
                    
                    // 可以选择保持展开状态或恢复到collapsed状态
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
    }
    
    /**
     * 开始搜索
     */
    private fun startSearch() {
        val query = searchInput.text.toString().trim()
        if (query.isEmpty()) {
            return
        }
        
        // 隐藏软键盘和清除焦点
        hideKeyboardAndClearFocus()
        
        currentQuery = query
        isSearching = true
        
        updateSearchButton()
        showProgress(getString(R.string.searching))
        
        // 调用搜索控制器
        searchController?.invoke(query, this)
    }
    
    /**
     * 停止搜索
     */
    private fun stopSearch() {
        isSearching = false
        
        updateSearchButton()
        hideProgress()
        
        // 调用停止搜索控制器
        stopSearchController?.invoke()
    }
    
    /**
     * 清除搜索
     */
    private fun clearSearch() {
        searchInput.text.clear()
        searchAdapter.clearResults()
        showEmptyView()
        
        if (isSearching) {
            stopSearch()
        }
    }
    
    /**
     * 搜索结果点击处理
     */
    private fun onSearchResultClick(result: PDFSearchResult) {
        // 调用导航控制器
        navigationController?.invoke(result)
        
        // 可选：关闭BottomSheet
        // dismiss()
    }
    
    /**
     * 更新搜索按钮状态
     */
    private fun updateSearchButton() {
        val hasText = searchInput.text.toString().trim().isNotEmpty()
        
        if (isSearching) {
            searchButton.setImageResource(R.drawable.ic_stop)
            searchButton.isEnabled = true
        } else {
            searchButton.setImageResource(R.drawable.ic_search)
            searchButton.isEnabled = hasText
        }
    }
    
    /**
     * 更新清除按钮状态
     */
    private fun updateClearButton() {
        val hasText = searchInput.text.toString().isNotEmpty()
        clearButton.visibility = if (hasText) View.VISIBLE else View.GONE
    }
    
    /**
     * 显示进度
     */
    private fun showProgress(message: String) {
        progressBar.visibility = View.VISIBLE
        statusText.text = message
        statusText.visibility = View.VISIBLE
        resultsRecyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
    }
    
    /**
     * 隐藏进度
     */
    private fun hideProgress() {
        progressBar.visibility = View.GONE
        statusText.visibility = View.GONE
    }
    
    /**
     * 显示搜索结果
     */
    private fun showResults() {
        hideProgress()
        resultsRecyclerView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
    }
    
    /**
     * 显示空视图
     */
    private fun showEmptyView() {
        hideProgress()
        resultsRecyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
    }
    
    // PDFSearchListener实现
    
    override fun onSearchStart(query: String) {
        if (!isAdded) return
        showProgress(getString(R.string.searching))
    }
    
    override fun onSearchProgress(query: String, progress: Int, total: Int) {
        if (!isAdded) return
        val progressText = getString(R.string.search_progress_format, progress, total)
        statusText.text = progressText
    }
    
    override fun onSearchResultsFound(query: String, results: List<PDFSearchResult>) {
        if (!isAdded) return
        searchAdapter.updateResults(query, results)
        showResults()
        
        val statusMessage = getString(R.string.search_results_found_format, results.size)
        statusText.text = statusMessage
        statusText.visibility = View.VISIBLE
    }
    
    override fun onSearchNoResults(query: String) {
        if (!isAdded) return
        searchAdapter.clearResults()
        emptyView.text = getString(R.string.search_no_results_format, query)
        showEmptyView()
    }
    
    override fun onSearchComplete(query: String, totalResults: Int) {
        if (!isAdded) return
        isSearching = false
        updateSearchButton()
    }
    
    override fun onSearchError(query: String, error: String) {
        if (!isAdded) return
        isSearching = false
        updateSearchButton()
        
        emptyView.text = getString(R.string.search_error_message_format, error)
        showEmptyView()
    }
    
    override fun onSearchCanceled(query: String) {
        if (!isAdded) return
        isSearching = false
        updateSearchButton()
        hideProgress()
    }
    
    override fun onDestroyView() {
        // 移除键盘监听器
        globalLayoutListener?.let { listener ->
            rootView?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
        }
        globalLayoutListener = null
        rootView = null
        super.onDestroyView()
    }
}
