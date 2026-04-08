package com.documentpro.office.business.fileviewer.ui.pdf

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.documentpro.office.business.fileviewer.BuildConfig
import com.lxj.xpopup.photoview.PhotoView
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityImagePreviewBinding
import com.documentpro.office.business.fileviewer.dialog.BusinessStoreScoreDialog
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType
import com.documentpro.office.business.fileviewer.utils.queryfile.equalsFileType

class ImagePreviewActivity : BaseActivity<ActivityImagePreviewBinding, BusinessMainModel>() {

    companion object {
        private const val PARAM_IMAGE_LIST = "param_image_list"
        private const val PARAM_POSITION = "param_position"
        private const val TAG = "ImagePreviewActivity"
        fun launch(context: Context, imageInfoList: ArrayList<BusinessFileInfo>,mField_1: Int = 0) {
            val intent = Intent(context, ImagePreviewActivity::class.java)
            
            // 使用Bundle包装Parcelable对象
            val bundle = Bundle()
            bundle.putParcelableArrayList(PARAM_IMAGE_LIST, imageInfoList)
            intent.putExtra("image_bundle", bundle)
            
            // 位置参数直接放在Intent中
            intent.putExtra(PARAM_POSITION,mField_1)
            
            context.startActivity(intent)
        }
    }

    private lateinit var imageList: ArrayList<BusinessFileInfo>
    private var mField_1 = 0
    private lateinit var pager: ViewPager
    private lateinit var tvPagerIndicator: android.widget.TextView
    private lateinit var tvSave: android.widget.TextView

    override fun initBinding(): ActivityImagePreviewBinding {
        return ActivityImagePreviewBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessMainModel {
        return viewModels<BusinessMainModel>().value
    }

    @Suppress("DEPRECATION")
    override fun initView() {
        try {
            // 获取位置参数
mField_1 = intent.getIntExtra(PARAM_POSITION, 0)
            
            // 从Bundle中获取图片列表
            val bundle = intent.getBundleExtra("image_bundle")
            Log.d(TAG, "Bundle获取结果: ${bundle != null}")
            
            if (bundle == null) {
                Log.e(TAG, "无法获取图片信息Bundle")
                finish()
                return
            }
            
            // 从Bundle中获取图片列表
            @Suppress("DEPRECATION")
            val imageListFromBundle = bundle.getParcelableArrayList<BusinessFileInfo>(PARAM_IMAGE_LIST)
            
            if (imageListFromBundle == null || imageListFromBundle.isEmpty()) {
                Log.e(TAG, "图片列表为空")
                finish()
                return
            }
            
            imageList = imageListFromBundle
            
            useDefaultToolbar(binding.toolbar, imageList[mField_1].name)

            // 初始化ViewPager和指示器
            pager = binding.pager
            tvPagerIndicator = binding.tvPagerIndicator
            tvSave = binding.tvSave

            // 设置ViewPager
            val photoViewAdapter = PhotoViewAdapter()
            pager.adapter = photoViewAdapter
            pager.currentItem =mField_1
            pager.offscreenPageLimit = 2
            pager.addOnPageChangeListener(photoViewAdapter)

            // 显示页码指示器
            execDisplay_1 ()

            // 设置保存按钮点击事件
            tvSave.setOnClickListener {
                execAction_2 ()
            }
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    execDisplay_6 {
                        finish()
                    }
                }
            })
            execLoad_5 ()
            BusinessStoreScoreDialog.checkShow(this)
        } catch (e: Exception) {
            Log.e(TAG, "初始化视图时发生错误", e)
            finish()
        }
    }

    private fun execDisplay_1() {
        if (imageList.size > 1) {
            tvPagerIndicator.text =
                getString(R.string.image_preview_pager_indicator,mField_1 + 1, imageList.size)
        }
    }

    private fun execAction_2() {
        // TODO: 实现保存图片功能
        ToastUtils.showShort(getString(R.string.image_preview_save_developing))
    }

    inner class PhotoViewAdapter : PagerAdapter(), ViewPager.OnPageChangeListener {
        override fun getCount(): Int = imageList.size

        override fun isViewFromObject(
            view: View,
            `o`: Any
        ): Boolean {
            return view == o
        }

        override fun instantiateItem(container: ViewGroup,mField_1: Int): Any {
            val fl = execAction_3 (container.context)
            val progressBar = execAction_4 (container.context)

            // 创建PhotoView
            val photoView = PhotoView(container.context)
            photoView.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // 加载图片
            val fileInfo = imageList[mField_1]
            when (equalsFileType(fileInfo.type)) {
                BusinessFileType.IMAGE -> {
                    progressBar.visibility = View.VISIBLE
                    Glide.with(this@ImagePreviewActivity)
                        .load(fileInfo.path)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable?>,
                                isFirstResource: Boolean
                            ): Boolean {
                                progressBar.visibility = View.GONE
                                ToastUtils.showShort(getString(R.string.image_preview_load_failed))
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable?>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                progressBar.visibility = View.GONE
                                return false
                            }

                        })
                        .into(photoView)
                }

                else -> {
                    ToastUtils.showShort(getString(R.string.image_preview_unsupported_type))
                }
            }

            fl.addView(photoView)
            fl.addView(progressBar)
            container.addView(fl)
            return fl
        }

        private fun execAction_3(context: Context): FrameLayout {
            val fl = FrameLayout(context)
            fl.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return fl
        }

        private fun execAction_4(context: Context): ProgressBar {
            val progressBar = ProgressBar(context)
            progressBar.isIndeterminate = true
            val size = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
            val params = FrameLayout.LayoutParams(size, size)
            params.gravity = Gravity.CENTER
            progressBar.layoutParams = params
            progressBar.visibility = View.GONE
            return progressBar
        }

        override fun destroyItem(container: ViewGroup,mField_1: Int, o: Any) {
            container.removeView(o as View)
        }

        override fun onPageScrolled(mField_1: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }

        override fun onPageSelected(mField_1: Int) {
            this@ImagePreviewActivity.mField_1 =mField_1
            execDisplay_1 ()
            // 更新标题
            supportActionBar?.title = imageList[mField_1].name
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    private fun execLoad_5() {

    }

    private fun execDisplay_6(nextAction: () -> Unit) {
        nextAction.invoke()
    }

    override fun closePage() {
        execDisplay_6 {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun initObserve() {
        // 暂无需要观察的数据
    }

    override fun initTag(): String {
        return TAG
    }
}
