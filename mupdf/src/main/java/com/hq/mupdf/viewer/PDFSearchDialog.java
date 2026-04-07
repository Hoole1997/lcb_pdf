package com.hq.mupdf.viewer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hq.mupdf.R;

/**
 * 现代化的PDF搜索对话框
 * 提供优雅的搜索界面和交互体验
 */
public class PDFSearchDialog extends Dialog {
    
    public interface OnSearchListener {
        void onSearch(String query, boolean matchCase, boolean wholeWord);
        void onSearchNext();
        void onSearchPrevious();
        void onSearchClosed();
    }
    
    private EditText searchInput;
    private ImageButton clearBtn, closeBtn, prevBtn, nextBtn;
    private TextView searchResultInfo, searchProgressText;
    private LinearLayout searchControls, searchOptions, searchProgress;
    private CheckBox matchCaseCheckbox, wholeWordCheckbox;
    
    private OnSearchListener searchListener;
    private boolean isSearching = false;
    private int currentResult = 0;
    private int totalResults = 0;
    
    public PDFSearchDialog(Context context) {
        super(context);
    }
    
    public PDFSearchDialog(Context context, OnSearchListener listener) {
        super(context);
        this.searchListener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置对话框样式
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_pdf_search);
        
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.TOP);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            
            // 设置动画
            window.getAttributes().windowAnimations = R.style.SearchDialogAnimation;
        }
        
        initViews();
        setupListeners();
        
        // 自动弹出键盘
        showKeyboard();
    }
    
    private void initViews() {
        searchInput = findViewById(R.id.searchInput);
        clearBtn = findViewById(R.id.clearBtn);
        closeBtn = findViewById(R.id.closeBtn);
        prevBtn = findViewById(R.id.prevBtn);
        nextBtn = findViewById(R.id.nextBtn);
        searchResultInfo = findViewById(R.id.searchResultInfo);
        searchProgressText = findViewById(R.id.searchProgressText);
        searchControls = findViewById(R.id.searchControls);
        searchOptions = findViewById(R.id.searchOptions);
        searchProgress = findViewById(R.id.searchProgress);
        matchCaseCheckbox = findViewById(R.id.matchCaseCheckbox);
        wholeWordCheckbox = findViewById(R.id.wholeWordCheckbox);
    }
    
    private void setupListeners() {
        // 搜索输入框监听
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.length() > 0;
                clearBtn.setVisibility(hasText ? View.VISIBLE : View.GONE);
                
                if (hasText) {
                    performSearch(s.toString());
                } else {
                    hideSearchControls();
                    resetSearchResults();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 键盘搜索按钮
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchInput.getText().toString().trim();
                    if (!query.isEmpty()) {
                        performSearch(query);
                    }
                    return true;
                }
                return false;
            }
        });
        
        // 清除按钮
        clearBtn.setOnClickListener(v -> {
            searchInput.setText("");
            searchInput.requestFocus();
        });
        
        // 关闭按钮
        closeBtn.setOnClickListener(v -> closeDialog());
        
        // 导航按钮
        prevBtn.setOnClickListener(v -> {
            if (searchListener != null) {
                searchListener.onSearchPrevious();
            }
        });
        
        nextBtn.setOnClickListener(v -> {
            if (searchListener != null) {
                searchListener.onSearchNext();
            }
        });
        
        // 搜索选项变化
        matchCaseCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });
        
        wholeWordCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });
    }
    
    private void performSearch(String query) {
        if (searchListener != null && !isSearching) {
            showSearchProgress();
            boolean matchCase = matchCaseCheckbox.isChecked();
            boolean wholeWord = wholeWordCheckbox.isChecked();
            searchListener.onSearch(query, matchCase, wholeWord);
        }
    }
    
    private void showKeyboard() {
        searchInput.requestFocus();
        searchInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
    }
    
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }
    
    private void showSearchControls() {
        if (searchControls.getVisibility() != View.VISIBLE) {
            searchControls.setVisibility(View.VISIBLE);
            animateSlideDown(searchControls);
        }
    }
    
    private void hideSearchControls() {
        if (searchControls.getVisibility() == View.VISIBLE) {
            animateSlideUp(searchControls, () -> searchControls.setVisibility(View.GONE));
        }
    }
    
    private void showSearchProgress() {
        isSearching = true;
        searchProgress.setVisibility(View.VISIBLE);
        hideSearchControls();
        animateSlideDown(searchProgress);
    }
    
    private void hideSearchProgress() {
        isSearching = false;
        if (searchProgress.getVisibility() == View.VISIBLE) {
            animateSlideUp(searchProgress, () -> searchProgress.setVisibility(View.GONE));
        }
    }
    
    private void animateSlideDown(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", -view.getHeight(), 0);
        animator.setDuration(250);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }
    
    private void animateSlideUp(View view, Runnable onEnd) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 0, -view.getHeight());
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onEnd != null) {
                    onEnd.run();
                }
            }
        });
        animator.start();
    }
    
    private void resetSearchResults() {
        currentResult = 0;
        totalResults = 0;
        updateNavigationButtons();
    }
    
    private void updateNavigationButtons() {
        boolean hasResults = totalResults > 0;
        prevBtn.setEnabled(hasResults && currentResult > 1);
        nextBtn.setEnabled(hasResults && currentResult < totalResults);
        
        // 更新按钮颜色
        int enabledColor = getContext().getResources().getColor(R.color.primary_color);
        int disabledColor = getContext().getResources().getColor(R.color.text_secondary);
        
        prevBtn.setColorFilter(prevBtn.isEnabled() ? enabledColor : disabledColor);
        nextBtn.setColorFilter(nextBtn.isEnabled() ? enabledColor : disabledColor);
    }
    
    private void closeDialog() {
        hideKeyboard();
        if (searchListener != null) {
            searchListener.onSearchClosed();
        }
        dismiss();
    }
    
    @Override
    public void onBackPressed() {
        closeDialog();
    }
    
    // 公共方法供外部调用
    
    /**
     * 设置搜索结果
     */
    public void setSearchResults(int current, int total) {
        hideSearchProgress();
        
        this.currentResult = current;
        this.totalResults = total;
        
        if (total > 0) {
            searchResultInfo.setText(getContext().getString(R.string.search_result_format, current, total));
            showSearchControls();
        } else {
            searchResultInfo.setText(getContext().getString(R.string.no_search_results));
            showSearchControls();
        }
        
        updateNavigationButtons();
    }
    
    /**
     * 显示搜索错误
     */
    public void showSearchError(String error) {
        hideSearchProgress();
        searchResultInfo.setText(error != null ? error : getContext().getString(R.string.search_error));
        showSearchControls();
        updateNavigationButtons();
    }
    
    /**
     * 设置搜索进度文本
     */
    public void setSearchProgressText(String text) {
        searchProgressText.setText(text);
    }
    
    /**
     * 获取当前搜索文本
     */
    public String getSearchText() {
        return searchInput.getText().toString().trim();
    }
    
    /**
     * 设置搜索文本
     */
    public void setSearchText(String text) {
        searchInput.setText(text);
        if (text != null && !text.isEmpty()) {
            searchInput.setSelection(text.length());
        }
    }
    
    /**
     * 检查是否匹配大小写
     */
    public boolean isMatchCase() {
        return matchCaseCheckbox.isChecked();
    }
    
    /**
     * 检查是否全词匹配
     */
    public boolean isWholeWord() {
        return wholeWordCheckbox.isChecked();
    }
}
