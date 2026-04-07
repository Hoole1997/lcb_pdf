package com.documentpro.office.business.fileviewer.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.databinding.LayoutFileMoreDialogBinding
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.ui.pdf.MergePdfActivity
import com.documentpro.office.business.fileviewer.utils.BusinessRecentStorage
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType
import com.documentpro.office.business.fileviewer.utils.queryfile.equalsFileType

class BusinessFileMoreDialog(
    context: Context, 
    private val fileInfo: BusinessFileInfo,
    private val mainModel: BusinessMainModel? = null
) : BottomSheetDialog(context) {
    private var isFav: Boolean = false
    private var onFavClickListener: (() -> Unit)? = null
    private var onShareClickListener: (() -> Unit)? = null
    private var onAddDesktopClickListener: (() -> Unit)? = null
    private var onRenameClickListener: (() -> Unit)? = null
    private var onDetailClickListener: (() -> Unit)? = null
    private var onFileLockClickListener: (() -> Unit)? = null
    private var onDeleteFileClickListener: (() -> Unit)? = null
    private lateinit var binding: LayoutFileMoreDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutFileMoreDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        initViews()
    }

    private fun initViews() {
        // 设置文件信息
        binding.ivIcon.setImageResource(fileInfo.icon())
        binding.tvName.text = fileInfo.name
        binding.tvPath.text = fileInfo.path

        binding.btnFileLock.isVisible = equalsFileType(fileInfo.type) == BusinessFileType.PDF

        // 初始化收藏状态
        isFav = BusinessRecentStorage.isFavoriteFile(fileInfo.path)
        binding.ivFav.setImageResource(if (isFav) R.mipmap.ic_favorite_filled else R.mipmap.ic_favorite_outline)

        // 收藏按钮
        binding.btnFav.setOnClickListener { view ->
            view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_click))
            if (isFav) {
                BusinessPointLog.logEvent("File_Collect_Cancel_Click")
            } else {
                BusinessPointLog.logEvent("File_Collect_Click")
            }
            if (mainModel != null) {
                // 使用MainModel统一管理收藏状态和刷新
                isFav = mainModel.toggleFavoriteAndRefresh(fileInfo)
                binding.ivFav.setImageResource(if (isFav) R.mipmap.ic_favorite_filled else R.mipmap.ic_favorite_outline)
            } else {
                // 兼容旧的回调方式
                isFav = !isFav
                binding.ivFav.setImageResource(if (isFav) R.mipmap.ic_favorite_filled else R.mipmap.ic_favorite_outline)
            }
            
            onFavClickListener?.invoke()
        }

        // 合并按钮
        binding.btnMerge.setOnClickListener { view ->
            dismiss()
            view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_click))
            MergePdfActivity.launchForMerge(context)
        }

        // 分享按钮
        binding.btnShare.setOnClickListener { view ->
            dismiss()
            view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_click))
            onShareClickListener?.invoke()
        }

        // 添加到桌面按钮
        binding.btnAddDesktop.setOnClickListener { view ->
            view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_click))
            onAddDesktopClickListener?.invoke()
        }

        // 重命名按钮
        binding.btnRename.setOnClickListener { view ->
            dismiss()
            view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_click))
            onRenameClickListener?.invoke()
        }

        // 设置密码
        binding.btnFileLock.setOnClickListener {
            dismiss()
            it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_click))
            onFileLockClickListener?.invoke()
        }

        // 详情按钮
        binding.btnDetail.setOnClickListener { view ->
            dismiss()
            view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_click))
            onDetailClickListener?.invoke()
        }

        // 删除
        binding.btnDelete.setOnClickListener {
            dismiss()
            it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_click))
            onDeleteFileClickListener?.invoke()
        }
    }

//    fun setFavState(fav: Boolean) {
//        isFav = fav
//        binding.ivFav.setImageResource(if (fav) R.mipmap.ic_favorite_filled else R.mipmap.ic_favorite_outline)
//    }

    fun setOnFavClickListener(listener: () -> Unit) {
        onFavClickListener = listener
    }

    fun setOnShareClickListener(listener: () -> Unit): BusinessFileMoreDialog {
        onShareClickListener = listener
        return this
    }

    fun setOnAddDesktopClickListener(listener: () -> Unit) {
        onAddDesktopClickListener = listener
    }

    fun setOnRenameClickListener(listener: () -> Unit): BusinessFileMoreDialog {
        onRenameClickListener = listener
        return this
    }

    fun setOnDetailClickListener(listener: () -> Unit) : BusinessFileMoreDialog {
        onDetailClickListener = listener
        return this
    }

    fun setOnFileLockClickListener(listener: () -> Unit): BusinessFileMoreDialog {
        onFileLockClickListener = listener
        return this
    }

    fun setOnFileDeleteClickListener(listener: () -> Unit): BusinessFileMoreDialog {
        onDeleteFileClickListener = listener
        return this
    }
}