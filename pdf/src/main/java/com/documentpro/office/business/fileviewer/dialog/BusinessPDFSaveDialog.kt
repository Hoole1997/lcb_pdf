package com.documentpro.office.business.fileviewer.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.documentpro.office.business.fileviewer.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 保存模式枚举
 */
enum class SaveMode {
    SAVE_AS_NEW_ONLY,     // 仅保存为新文件
    OVERWRITE_ONLY,       // 仅覆盖当前文件
    BOTH                  // 都可以选择（默认）
}

/**
 * PDF保存对话框 - 现代化设计
 * 提供优雅的用户界面和良好的交互体验
 */
class BusinessPDFSaveDialog private constructor(
    private val context: Context,
    private val currentFileName: String,
    private val onSaveListener: OnSaveListener,
    private val isBack: Boolean = false,
    private val customHint: String? = null,
    private val saveMode: SaveMode = SaveMode.BOTH
) {
    
    interface OnSaveListener {
        fun onSave(fileName: String, saveAsNew: Boolean)
        fun onCancel()
    }
    
    private lateinit var dialog: Dialog
    private lateinit var fileNameInput: EditText
    private lateinit var fileNameLayout: FrameLayout
    private lateinit var fileNameHelper: TextView
    private lateinit var fileNameError: TextView
    private lateinit var saveOptionGroup: RadioGroup
    private lateinit var saveAsNewRadio: RadioButton
    private lateinit var overwriteRadio: RadioButton
    private lateinit var saveAsNewContainer: LinearLayout
    private lateinit var overwriteContainer: LinearLayout
    private lateinit var confirmButton: TextView
    private lateinit var cancelButton: TextView
    private lateinit var dialogTitle: TextView
    private lateinit var saveOptionsTitle: TextView
    
    private var isInitialized = false
    
    companion object {
        /**
         * 创建并显示保存对话框
         * @param context 上下文
         * @param currentFileName 当前文件名
         * @param onSaveListener 保存监听器
         * @param isBack 是否为退出时的保存提示
         * @param customHint 自定义hint文件名
         * @param saveMode 保存模式，默认为BOTH（都可以选择）
         */
        fun show(
            context: Context,
            currentFileName: String,
            onSaveListener: OnSaveListener,
            isBack: Boolean = false,
            customHint: String? = null,
            saveMode: SaveMode = SaveMode.BOTH
        ) {
            BusinessPDFSaveDialog(context, currentFileName, onSaveListener, isBack, customHint, saveMode).show()
        }
    }
    
    init {
        createDialog()
        setupUI()
        setupEventListeners()
    }
    
    /**
     * 创建对话框
     */
    private fun createDialog() {
        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // 设置对话框内容视图
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_pdf_save, null)
        dialog.setContentView(view)
        
        // 设置对话框窗口属性
        dialog.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.gravity = Gravity.CENTER
            window.attributes = layoutParams
            
            // 设置动画
            window.setWindowAnimations(R.style.DialogAnimation)
            
            // 设置软键盘模式
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        
        // 设置对话框属性
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
    }
    
    /**
     * 设置UI组件
     */
    private fun setupUI() {
        findViews()
        initializeDialogText()
        initializeFileNameInput()
        initializeRadioButtons()
        updateOptionSelection() // 初始化选项状态
        updateConfirmButtonState()
        
        isInitialized = true
    }
    
    /**
     * 查找视图组件
     */
    private fun findViews() {
        fileNameInput = dialog.findViewById(R.id.fileNameInput)
        fileNameLayout = dialog.findViewById(R.id.fileNameLayout)
        fileNameHelper = dialog.findViewById(R.id.fileNameHelper)
        fileNameError = dialog.findViewById(R.id.fileNameError)
        saveOptionGroup = dialog.findViewById(R.id.saveOptionGroup)
        saveAsNewRadio = dialog.findViewById(R.id.saveAsNewRadio)
        overwriteRadio = dialog.findViewById(R.id.overwriteRadio)
        saveAsNewContainer = dialog.findViewById(R.id.saveAsNewContainer)
        overwriteContainer = dialog.findViewById(R.id.overwriteContainer)
        confirmButton = dialog.findViewById(R.id.confirmButton)
        cancelButton = dialog.findViewById(R.id.cancelButton)
        dialogTitle = dialog.findViewById(R.id.dialogTitle)
        saveOptionsTitle = dialog.findViewById(R.id.saveOptionsTitle)
    }
    
    /**
     * 初始化对话框文案
     */
    private fun initializeDialogText() {
        if (isBack) {
            dialogTitle.text = context.getString(R.string.pdf_save_unsaved_changes_title)
            saveOptionsTitle.text = context.getString(R.string.pdf_save_back_options_title)
//            cancelButton.text = context.getString(R.string.pdf_save_dont_save_and_exit)
        } else {
            dialogTitle.text = context.getString(R.string.pdf_save_dialog_title)
            saveOptionsTitle.text = context.getString(R.string.pdf_save_options_title)
//            cancelButton.text = context.getString(R.string.action_cancel)
        }
    }
    
    /**
     * 初始化文件名输入框
     */
    private fun initializeFileNameInput() {
        val defaultFileName = customHint ?: generateDefaultFileName()
        fileNameInput.setText(defaultFileName)
        
        // 设置光标位置到文件名末尾（不包括扩展名）
        val dotIndex = defaultFileName.lastIndexOf('.')
        if (dotIndex > 0) {
            fileNameInput.setSelection(dotIndex)
        } else {
            fileNameInput.selectAll()
        }
        
        // 根据场景设置提示文本
        when {
            isBack -> {
                fileNameInput.hint = context.getString(R.string.pdf_save_new_file_hint)
                fileNameHelper.text = context.getString(R.string.pdf_save_copy_desc)
            }
            saveMode == SaveMode.OVERWRITE_ONLY -> {
                fileNameInput.hint = context.getString(R.string.pdf_save_current_file_hint, currentFileName)
                fileNameHelper.text = context.getString(R.string.pdf_save_overwrite_desc)
            }
            else -> {
                fileNameInput.hint = customHint?.let { context.getString(R.string.pdf_save_suggested_name_hint) } ?: context.getString(R.string.pdf_save_file_name_hint)
                fileNameHelper.text = context.getString(R.string.pdf_save_file_name_helper)
            }
        }
    }
    
    /**
     * 生成默认文件名
     * 智能处理已有时间后缀的文件名，更新而非追加
     */
    private fun generateDefaultFileName(): String {
        val originalName = currentFileName.removeSuffix(".pdf")
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // 检查文件名是否已包含时间后缀
        val updatedName = updateExistingTimeSuffix(originalName, currentDate)
        return "$updatedName.pdf"
    }
    
    /**
     * 更新文件名中已存在的时间后缀
     * @param fileName 不含扩展名的文件名
     * @param newDate 新的日期字符串
     * @return 更新后的文件名
     */
    private fun updateExistingTimeSuffix(fileName: String, newDate: String): String {
        // 匹配各种时间格式的正则表达式
        val timePatterns = listOf(
            // yyyy-MM-dd 格式
            Regex("_\\d{4}-\\d{2}-\\d{2}$"),
            // yyyy年MM月dd日 格式
            Regex("_\\d{4}年\\d{1,2}月\\d{1,2}日$"),
            // yyyyMMdd 格式
            Regex("_\\d{8}$"),
            // yyyy-MM-dd HH:mm 格式
            Regex("_\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$"),
            // yyyy-MM-dd_HH-mm 格式
            Regex("_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}$"),
            // MM-dd 格式
            Regex("_\\d{2}-\\d{2}$"),
            // copy、副本等后缀
            Regex("_copy\\d*$", RegexOption.IGNORE_CASE),
            Regex("_副本\\d*$"),
            // 括号内的数字或日期
            Regex("\\(\\d+\\)$"),
            Regex("\\(\\d{4}-\\d{2}-\\d{2}\\)$")
        )
        
        // 检查是否匹配任何时间模式
        for (pattern in timePatterns) {
            if (pattern.containsMatchIn(fileName)) {
                // 移除已有的时间后缀，添加新的日期
                val baseFileName = pattern.replace(fileName, "")
                return "${baseFileName}_$newDate"
            }
        }
        
        // 如果没有找到时间后缀，检查是否已经是今天的日期
        if (fileName.endsWith("_$newDate")) {
            // 已经是今天的日期，直接返回
            return fileName
        }
        
        // 没有时间后缀，正常添加
        return "${fileName}_$newDate"
    }
    
    /**
     * 初始化单选按钮
     */
    private fun initializeRadioButtons() {
        // 根据保存模式设置单选按钮的可见性和默认选择
        when (saveMode) {
            SaveMode.SAVE_AS_NEW_ONLY -> {
                // 只显示保存为新文件选项
                saveAsNewContainer.visibility = View.VISIBLE
                overwriteContainer.visibility = View.GONE
                saveAsNewRadio.isChecked = true
                saveOptionGroup.check(R.id.saveAsNewRadio)
                // 隐藏选项标题，因为只有一个选项
                saveOptionsTitle.visibility = View.GONE
            }
            SaveMode.OVERWRITE_ONLY -> {
                // 只显示覆盖当前文件选项
                saveAsNewContainer.visibility = View.GONE
                overwriteContainer.visibility = View.VISIBLE
                overwriteRadio.isChecked = true
                saveOptionGroup.check(R.id.overwriteRadio)
                // 隐藏选项标题，因为只有一个选项
                saveOptionsTitle.visibility = View.GONE
            }
            SaveMode.BOTH -> {
                // 显示所有选项（默认行为）
                saveAsNewContainer.visibility = View.VISIBLE
                overwriteContainer.visibility = View.VISIBLE
                saveAsNewRadio.isChecked = true
                saveOptionGroup.check(R.id.saveAsNewRadio)
                saveOptionsTitle.visibility = View.VISIBLE
            }
        }
        
        // 根据是否为退出场景设置不同的文案
//        if (isBack) {
//            saveAsNewRadio.text = context.getString(R.string.pdf_save_copy_and_exit)
//            overwriteRadio.text = context.getString(R.string.pdf_save_overwrite_and_exit)
//        } else {
//            saveAsNewRadio.text = context.getString(R.string.pdf_save_as_new)
//            overwriteRadio.text = context.getString(R.string.pdf_save_overwrite)
//        }
    }
    
    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 文件名输入监听
        fileNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateFileName()
                updateConfirmButtonState()
            }
        })
        
        // 保存选项容器点击监听
        saveAsNewContainer.setOnClickListener {
            saveAsNewRadio.isChecked = true
            saveOptionGroup.check(R.id.saveAsNewRadio)
            updateOptionSelection()
        }
        
        overwriteContainer.setOnClickListener {
            overwriteRadio.isChecked = true
            saveOptionGroup.check(R.id.overwriteRadio)
            updateOptionSelection()
        }
        
        // 保存选项变化监听
        saveOptionGroup.setOnCheckedChangeListener { _, checkedId ->
            updateOptionSelection()
        }
        
        // 确认按钮点击
        confirmButton.setOnClickListener {
            handleConfirm()
        }
        
        // 取消按钮点击
        cancelButton.setOnClickListener {
            handleCancel()
        }
        
        // 对话框取消监听
        dialog.setOnCancelListener {
            onSaveListener.onCancel()
        }
    }
    
    /**
     * 更新选项选择状态
     */
    private fun updateOptionSelection() {
        when {
            saveMode == SaveMode.SAVE_AS_NEW_ONLY -> {
                // 只有保存为新文件选项，始终启用文件名输入
                fileNameLayout.isEnabled = true
                fileNameInput.isEnabled = true
                fileNameLayout.alpha = 1.0f
                fileNameInput.alpha = 1.0f
                clearError()
            }
            saveMode == SaveMode.OVERWRITE_ONLY -> {
                // 只有覆盖当前文件选项，禁用文件名输入
                fileNameLayout.isEnabled = false
                fileNameInput.isEnabled = false
                fileNameLayout.alpha = 0.6f
                fileNameInput.alpha = 0.6f
                clearError()
            }
            saveOptionGroup.checkedRadioButtonId == R.id.saveAsNewRadio -> {
                // 选择了保存为新文件
                fileNameLayout.isEnabled = true
                fileNameInput.isEnabled = true
                fileNameLayout.alpha = 1.0f
                fileNameInput.alpha = 1.0f
                clearError()
            }
            saveOptionGroup.checkedRadioButtonId == R.id.overwriteRadio -> {
                // 选择了覆盖当前文件
                fileNameLayout.isEnabled = false
                fileNameInput.isEnabled = false
                fileNameLayout.alpha = 0.6f
                fileNameInput.alpha = 0.6f
                clearError()
            }
        }
        updateConfirmButtonState()
    }
    
    /**
     * 验证文件名
     */
    private fun validateFileName(): Boolean {
        val fileName = fileNameInput.text?.toString()?.trim() ?: ""
        
        return when {
            fileName.isEmpty() -> {
                showError(context.getString(R.string.pdf_save_error_empty_name))
                false
            }
            fileName.length > 255 -> {
                showError(context.getString(R.string.pdf_save_error_name_too_long))
                false
            }
            !isValidFileName(fileName) -> {
                showError(context.getString(R.string.pdf_save_error_invalid_chars))
                false
            }
            !fileName.endsWith(".pdf", ignoreCase = true) -> {
                showError(context.getString(R.string.pdf_save_error_must_end_pdf))
                false
            }
            else -> {
                clearError()
                true
            }
        }
    }
    
    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        fileNameError.text = message
        fileNameError.visibility = View.VISIBLE
        fileNameHelper.visibility = View.GONE
    }
    
    /**
     * 清除错误信息
     */
    private fun clearError() {
        fileNameError.visibility = View.GONE
        fileNameHelper.visibility = View.VISIBLE
    }
    
    /**
     * 检查文件名是否合法
     */
    private fun isValidFileName(fileName: String): Boolean {
        val invalidChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        return invalidChars.none { fileName.contains(it) }
    }
    
    /**
     * 更新确认按钮状态
     */
    private fun updateConfirmButtonState() {
        val isValid = when (saveMode) {
            SaveMode.SAVE_AS_NEW_ONLY -> validateFileName()
            SaveMode.OVERWRITE_ONLY -> true
            SaveMode.BOTH -> when (saveOptionGroup.checkedRadioButtonId) {
                R.id.saveAsNewRadio -> validateFileName()
                R.id.overwriteRadio -> true
                else -> false
            }
        }
        
        confirmButton.isEnabled = isValid
        confirmButton.alpha = if (isValid) 1.0f else 0.6f
        
        // 更新按钮背景
        val backgroundRes = if (isValid) {
            R.drawable.bg_button_modern_primary
        } else {
            R.drawable.bg_button_modern_primary // 禁用状态在drawable中已定义
        }
        confirmButton.setBackgroundResource(backgroundRes)
    }
    
    /**
     * 处理确认操作
     */
    private fun handleConfirm() {
        val saveAsNew = when (saveMode) {
            SaveMode.SAVE_AS_NEW_ONLY -> true
            SaveMode.OVERWRITE_ONLY -> false
            SaveMode.BOTH -> saveOptionGroup.checkedRadioButtonId == R.id.saveAsNewRadio
        }
        
        val fileName = if (saveAsNew) {
            fileNameInput.text?.toString()?.trim() ?: ""
        } else {
            currentFileName
        }
        
        if (saveAsNew && !validateFileName()) {
            return
        }
        
        onSaveListener.onSave(fileName, saveAsNew)
        dialog.dismiss()
    }
    
    /**
     * 处理取消操作
     */
    private fun handleCancel() {
        onSaveListener.onCancel()
        dialog.dismiss()
    }
    
    /**
     * 显示对话框
     */
    fun show() {
        if (!dialog.isShowing) {
            dialog.show()
            
            // 延迟显示软键盘，确保对话框已完全显示
            fileNameInput.postDelayed({
                fileNameInput.requestFocus()
            }, 100)
        }
    }
    
    /**
     * 隐藏对话框
     */
    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
}