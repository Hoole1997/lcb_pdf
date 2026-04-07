package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import android.util.Log
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.databinding.LayoutStoreScoreDialogBinding

class BusinessStoreScoreDialog(context: Context) : BottomPopupView(context) {

    companion object {
        fun checkShow(context: Context) {
            val isFirstStoreScore = SPUtils.getInstance().getBoolean("isFirstStoreScore", true)
            if (isFirstStoreScore) {
                show(context)
            }
        }

        fun show(context: Context) {
            XPopup.Builder(context)
                .hasNavigationBar(false)
                .asCustom(BusinessStoreScoreDialog(context))
                .show()
        }
    }

    val manager = ReviewManagerFactory.create(context)

    override fun getImplLayoutId(): Int {
        return R.layout.layout_store_score_dialog
    }

    override fun onCreate() {
        super.onCreate()
        val binding = LayoutStoreScoreDialogBinding.bind(popupImplView)
        
        // 默认选中五颗星
        binding.ratingBar.rating = 5f
        binding.btnScore.isEnabled = true
        
        val request = manager.requestReviewFlow()
        var reviewInfo:ReviewInfo? = null
        request.addOnCompleteListener {
            if (it.isSuccessful) {
                // We got the ReviewInfo object
                reviewInfo = it.result
            } else {
                // There was some problem, log or handle the error code.
                Log.d("评论弹框","请求失败 ${it.exception?.message}")
            }
        }
        SPUtils.getInstance().put("isFirstStoreScore", false)
        binding.btnScore.setOnClickListener {
            if (binding.ratingBar.rating == 0f) return@setOnClickListener
            
            // 只有5星时才调用评价，其它时候直接显示toast
            if (binding.ratingBar.rating == 5f) {
                if (reviewInfo == null) {
                    ToastUtils.showShort(context.getString(R.string.store_score_feedback_thanks))
                    dismiss()
                } else {
                    launchReview(reviewInfo!!)
                }
            } else {
                // 非5星，直接显示感谢并关闭
                ToastUtils.showShort(context.getString(R.string.store_score_feedback_thanks))
                dismiss()
            }
        }
        binding.ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            // 当用户改变评分时的处理
            if (fromUser) {
                // 处理用户评分
                if (rating == 0f) {
                    binding.btnScore.isEnabled = false
                    binding.btnScore.alpha = 0.5f  // 禁用时整体变灰
                } else {
                    binding.btnScore.isEnabled = true
                    binding.btnScore.alpha = 1f  // 启用时恢复正常

                }
            }
        }
    }

    private fun launchReview(reviewInfo: ReviewInfo) {
        manager.launchReviewFlow(ActivityUtils.getTopActivity(),reviewInfo).addOnCompleteListener {
            ToastUtils.showShort(context.getString(R.string.store_score_feedback_thanks))
            dismiss()
        }.addOnFailureListener {
            ToastUtils.showShort(context.getString(R.string.store_score_feedback_thanks))
            dismiss()
        }
    }

}