package com.documentpro.office.business.fileviewer.ui.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.ActivityUtils
import com.documentpro.office.business.fileviewer.BuildConfig
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.base.BaseModel
import com.documentpro.office.business.fileviewer.databinding.ActivityUninstallOptionsBinding
import com.documentpro.office.business.fileviewer.ui.main.BusinessWorkspaceActivity
import com.documentpro.office.business.fileviewer.utils.BusinessSplashForegroundController

class UninstallOptionActivity : BaseActivity<ActivityUninstallOptionsBinding, BaseModel>() {

    private var mField_1: Int = -1

    companion object {
        var onResultCallback: (() -> Unit)? = null
        private const val TAG = "UninstallOptionActivity"
        @JvmStatic
        fun start(context: Context, call: (() -> Unit)? = null) {
            onResultCallback = call
            val starter = Intent(context, UninstallOptionActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun initBinding(): ActivityUninstallOptionsBinding {
        return ActivityUninstallOptionsBinding.inflate(layoutInflater)
    }

    override fun initModel(): BaseModel {
        return BaseModel()
    }

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        // 设置选项点击事件
        execAction_1 ()

        // 设置按钮点击事件
        execAction_4 ()

        // 设置软键盘处理
        execAction_6 ()
        BusinessPointLog.logEvent("Uninstall_Option_Show")
        execLoad_8 ()
    }

    private fun execAction_1() {
        // 选项1
        binding.option1Layout.setOnClickListener {
            execAction_2 (it)
            execAction_3 (1, binding.option1Text)
        }

        // 选项2
        binding.option2Layout.setOnClickListener {
            execAction_2 (it)
            execAction_3 (2, binding.option2Text)
        }

        // 选项3
        binding.option3Layout.setOnClickListener {
            execAction_2 (it)
            execAction_3 (3, binding.option3Text)
        }

        // 选项4
        binding.option4Layout.setOnClickListener {
            execAction_2 (it)
            execAction_3 (4, binding.option4Text)
        }

        // 选项5
        binding.option5Layout.setOnClickListener {
            execAction_2 (it)
            execAction_3 (5, binding.option5Text)
        }
    }

    private fun execAction_2(view: View) {
        arrayListOf(
            binding.option1Layout,
            binding.option2Layout,
            binding.option3Layout,
            binding.option4Layout,
            binding.option5Layout
        ).forEach {
            val isSelect = it == view
            it.setBackgroundResource(if (isSelect) R.drawable.bg_edit_text_border_sel else R.drawable.bg_edit_text_border)
        }
    }

    private fun execAction_3(optionId: Int, selectedTextView: TextView) {
        // 重置所有选项为未选中状态
        val uncheckedDrawable = ContextCompat.getDrawable(this, R.drawable.ic_option_unchecked)
        binding.option1Text.setCompoundDrawablesWithIntrinsicBounds(null, null, uncheckedDrawable, null)
        binding.option2Text.setCompoundDrawablesWithIntrinsicBounds(null, null, uncheckedDrawable, null)
        binding.option3Text.setCompoundDrawablesWithIntrinsicBounds(null, null, uncheckedDrawable, null)
        binding.option4Text.setCompoundDrawablesWithIntrinsicBounds(null, null, uncheckedDrawable, null)
        binding.option5Text.setCompoundDrawablesWithIntrinsicBounds(null, null, uncheckedDrawable, null)
        binding.option1Text.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.option2Text.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.option3Text.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.option4Text.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.option5Text.setTextColor(ContextCompat.getColor(this, R.color.black))

        // 设置当前选项为选中状态
        val checkedDrawable = ContextCompat.getDrawable(this, R.drawable.ic_option_checked)
        selectedTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, checkedDrawable, null)
        selectedTextView.setTextColor(ContextCompat.getColor(this, R.color.radio_button_red))
mField_1 = optionId
    }

    private fun execAction_4() {
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            ActivityUtils.startActivity(BusinessWorkspaceActivity::class.java)
            ActivityUtils.finishActivity(UninstallPromptActivity::class.java)
            finish()
        }

        // 卸载按钮
        binding.btnUninstall.setOnClickListener {
            execProcess_5 ()
        }
    }

    private fun execProcess_5() {
        BusinessPointLog.logEvent("Uninstall_Click")
        // 获取用户输入的详细信息
        val details = binding.editTextDetails.text.toString().trim()

        // 这里可以处理卸载逻辑
        // 例如：记录用户反馈、执行卸载等

        // 调用回调
        onResultCallback?.invoke()

        // 关闭当前页面
        finish()

        // 跳转系统页
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${packageName}")
        startActivity(intent)
        BusinessSplashForegroundController.markNextIntercept()
    }

    private fun execAction_6() {
        // 设置EditText焦点监听
        binding.editTextDetails.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // 获得焦点时，延迟滚动到EditText位置
                Handler(Looper.getMainLooper()).postDelayed({
                    execAction_7 ()
                }, 300)
            }
        }
    }
    
    private fun execAction_7() {
        try {
            // 计算EditText在ScrollView中的位置
            val editTextLocation = IntArray(2)
            binding.editTextDetails.getLocationInWindow(editTextLocation)
            
            // 简单滚动，确保EditText可见
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, binding.editTextDetails.top - 200)
            }
        } catch (e: Exception) {
            // 如果计算失败，使用简单的滚动
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, binding.editTextDetails.top - 300)
            }
        }
    }

    private fun execLoad_8() {
    }

    override fun initObserve() {
        // 无需观察数据变化
    }

    override fun initTag(): String {
        return TAG
    }
} 