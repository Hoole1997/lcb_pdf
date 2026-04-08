package com.documentpro.office.business.fileviewer.ui.splash

import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.SPUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityGuideBinding
import com.documentpro.office.business.fileviewer.ui.language.LanguageActivity
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.ui.main.BusinessWorkspaceActivity
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import kotlin.math.abs

class GuideActivity : BaseActivity<ActivityGuideBinding, BusinessMainModel>() {

    companion object {
        private const val TAG = "GuideActivity"
    }

    private lateinit var pages: List<BusinessGuidePage>
    private lateinit var adapter: BusinessGuidePagerAdapter
    private lateinit var gestureDetector: GestureDetector

    override fun initBinding(): ActivityGuideBinding {
        return ActivityGuideBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessMainModel {
        return viewModels<BusinessMainModel>().value
    }

    private var mField_1: Int = 0

    private var mField_2 = 0

    private var countDownTimer: CountDownTimer? = null
    private val countDownDuration = 2000L // 2秒
    private val countDownInterval = 100L // 100ms更新一次（用于更平滑的更新）
    private var isCountDownEnabled = true // 是否允许自动启动倒计时

    override fun initView() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                binding.btnNext.performClick()
            }
        })
        execAction_6 ()
        // 初始化引导页数据
        pages = listOf(
            BusinessGuidePage(
                R.drawable.guide_img1,
                getString(R.string.guide_page1_title),
                getString(R.string.guide_page1_desc)
            ),
            BusinessGuidePage(
                R.drawable.guide_img2,
                getString(R.string.guide_page2_title),
                getString(R.string.guide_page2_desc)
            ),
            BusinessGuidePage(
                R.drawable.guide_img3,
                getString(R.string.guide_page3_title),
                getString(R.string.guide_page3_desc)
            )
//            ,
//            BusinessGuidePage(
//                R.drawable.guide_img4,
//                getString(R.string.guide_page4_title),
//                getString(R.string.guide_page4_desc)
//            )
        )

        adapter = BusinessGuidePagerAdapter(pages)
        binding.vpGuide.adapter = adapter

        // 添加触摸事件监听
        binding.vpGuide.getChildAt(0).setOnTouchListener { _, event ->
            if (binding.vpGuide.currentItem == pages.lastIndex) {
                gestureDetector.onTouchEvent(event)
            }
            false
        }

        // 初始化指示器
        execAction_2 (pages.size)
        execAction_3 (0)

        // ViewPager监听
        binding.vpGuide.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                execAction_3 (position)
mField_1 = position
//                binding.btnNext.text = getString(R.string.guide_btn_next)
                BusinessPointLog.logEvent("Guide", mapOf("Guide" to position + 3))
                if (position > 0) {
                    execLoad_4 (position)
                }
                Log.d(TAG, position.toString())

                // 页面切换后，如果不是最后一页且允许倒计时则启动倒计时
                if (position < pages.lastIndex && isCountDownEnabled) {
                    startCountDown()
                } else {
                    // 最后一页或倒计时被禁用时停止倒计时
                    cancelCountDown()
                }
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)

                // 检测用户滑动：SCROLL_STATE_DRAGGING 表示用户正在拖动（左滑或右滑）
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    // 用户主动滑动时停止所有倒计时，并禁用后续自动倒计时
                    stopAllCountDown()
                }
            }
        })

        // 按钮点击
        binding.btnNext.setOnClickListener {
            // 点击按钮时停止所有倒计时，并禁用后续自动倒计时
            stopAllCountDown()

            val pos = binding.vpGuide.currentItem
            if (pos < pages.lastIndex) {
                binding.vpGuide.currentItem = pos + 1
            } else {
                execAction_1 ()
            }
        }
        binding.tvSkip.setOnClickListener {
            // 点击跳过时停止所有倒计时
            stopAllCountDown()
            BusinessPointLog.logEvent("JumpGuide", mapOf("Position" to binding.vpGuide.currentItem.toString()))
            execAction_1 ()
        }
        execLoad_4 (0)

        // 初始页面启动倒计时（如果不是最后一页且允许倒计时）
        if (binding.vpGuide.currentItem < pages.lastIndex && isCountDownEnabled) {
            startCountDown()
        }
    }

    /**
     * 启动倒计时
     */
    private fun startCountDown() {
        cancelCountDown() // 先取消之前的倒计时

        val baseText = getString(R.string.guide_btn_next)
        // 先显示(2)
        binding.btnNext.text = "$baseText (2)"

        countDownTimer = object : CountDownTimer(countDownDuration, countDownInterval) {
            override fun onTick(millisUntilFinished: Long) {
                // 向上取整：(millisUntilFinished + 999) / 1000
                // 这样1000ms显示1，0ms时显示0（但不会到达，因为会先onFinish）
                val seconds = ((millisUntilFinished + 999) / 1000).toInt()
                if (seconds > 0) {
                    binding.btnNext.text = "$baseText ($seconds)"
                }
            }

            override fun onFinish() {
                // 恢复按钮文字
                binding.btnNext.text = baseText
                // 倒计时结束，切换到下一页
                val currentPos = binding.vpGuide.currentItem
                if (currentPos < pages.lastIndex) {
                    binding.vpGuide.currentItem = currentPos + 1
                    // 注意：onPageSelected 会在页面切换后自动调用，那里会重新启动倒计时
                }
            }
        }.start()
    }

    /**
     * 取消倒计时（保留后续自动启动的能力）
     */
    private fun cancelCountDown() {
        countDownTimer?.cancel()
        countDownTimer = null
        // 恢复按钮文字
        binding.btnNext.text = getString(R.string.guide_btn_next)
    }

    /**
     * 停止所有倒计时，并禁用后续自动倒计时
     */
    private fun stopAllCountDown() {
        cancelCountDown()
        isCountDownEnabled = false // 禁用后续自动启动倒计时
    }

    private fun execAction_1() {
        loadInterstitial(call = {
            val next = {
                ActivityUtils.finishActivity(LanguageActivity::class.java)
                SPUtils.getInstance().put("isGuide", true,true)
                startActivity(Intent(this, BusinessWorkspaceActivity::class.java))
                finish()
            }
            next.invoke()
        })
    }

    private fun execAction_2(count: Int) {
        val indicatorLayout = binding.indicator
        indicatorLayout.removeAllViews()
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(8, 0, 8, 0)
        for (i in 0 until count) {
            val dot = ImageView(this)
            dot.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.indicator_inactive_dot
                )
            )
            dot.layoutParams = params
            indicatorLayout.addView(dot)
        }
    }

    private fun execAction_3(index: Int) {
        val indicatorLayout = binding.indicator
        for (i in 0 until indicatorLayout.childCount) {
            val imageView = indicatorLayout.getChildAt(i) as ImageView
            imageView.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (i == index) R.drawable.indicator_active_dot else R.drawable.indicator_inactive_dot
                )
            )
        }
    }

    override fun initWindowPadding() {
        findViewById<ViewGroup>(R.id.main)?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    private fun execLoad_4(position:Int) {

    }

    private fun execAction_6() {mField_2 = ConvertUtils.dp2px(50f)
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY) &&
                    abs(diffX) > mField_2 &&
                    abs(velocityX) > 100) {

                    if (diffX < 0 && binding.vpGuide.currentItem == pages.lastIndex) {
                        // 在最后一页检测到左滑
                        execAction_1 ()
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun initObserve() {}
    override fun initTag(): String {
        return TAG
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCountDown()
    }
}
