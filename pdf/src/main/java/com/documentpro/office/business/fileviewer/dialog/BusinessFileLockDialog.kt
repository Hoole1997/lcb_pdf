package com.documentpro.office.business.fileviewer.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.LayoutFileLockDialogBinding

@SuppressLint("ViewConstructor")
class BusinessFileLockDialog(context: Context,val isLock: Boolean, val onResult:(String) -> Unit) : BottomPopupView(context) {

    companion object {
        fun show(context: Context,isLock: Boolean,onResult:(String) -> Unit) {
            XPopup.Builder(context)
                .moveUpToKeyboard(true)
                .hasNavigationBar(false)
                .asCustom(BusinessFileLockDialog(context,isLock,onResult))
                .show()
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.layout_file_lock_dialog
    }

    override fun onCreate() {
        super.onCreate()
        val binding = LayoutFileLockDialogBinding.bind(popupImplView)

        var isPasswordVisible = false
        // 初始icon
        binding.ivVisible.setImageResource(R.mipmap.ic_eye_closed)
        binding.etContent.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.etContent.setSelection(binding.etContent.text?.length ?: 0)

        if (isLock) {
            binding.tvTitle.text = context.getString(R.string.lock_set_password)
            binding.tvDescription.text = context.getString(R.string.lock_set_password_desc)
            BusinessPointLog.logEvent("LockPDF_Show")
        } else {
            binding.tvTitle.text = context.getString(R.string.lock_remove_password)
            binding.tvDescription.text = context.getString(R.string.lock_remove_password_desc)
            BusinessPointLog.logEvent("UnlockPDF_Show")
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        binding.ivVisible.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etContent.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivVisible.setImageResource(R.mipmap.ic_eye_open)
            } else {
                binding.etContent.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivVisible.setImageResource(R.mipmap.ic_eye_closed)
            }
            // 保持光标在末尾
            binding.etContent.setSelection(binding.etContent.text?.length ?: 0)
        }
        binding.btnConfirm.setOnClickListener {
            val content = binding.etContent.text.toString().trim()
            if (TextUtils.isEmpty(content))return@setOnClickListener
            onResult.invoke(content)
            dismiss()
        }
    }

}