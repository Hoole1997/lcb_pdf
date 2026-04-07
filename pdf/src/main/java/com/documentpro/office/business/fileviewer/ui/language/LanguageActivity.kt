package com.documentpro.office.business.fileviewer.ui.language

import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.SPUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityLanguageBinding
import com.documentpro.office.business.fileviewer.ui.main.BusinessMainModel
import com.documentpro.office.business.fileviewer.ui.main.BusinessWorkspaceActivity
import com.documentpro.office.business.fileviewer.ui.splash.GuideActivity
import com.documentpro.office.business.fileviewer.utils.BusinessSPConfig
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.loadNative
import net.corekit.core.utils.BusinessLanguageController
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import kotlinx.coroutines.launch
import net.corekit.core.ext.isDefaultLauncher
import com.android.common.bill.ads.ext.AdShowExt
import com.android.common.bill.ui.NativeAdStyleType
import com.documentpro.office.business.fileviewer.PdfAppInitializer
import com.documentpro.office.business.fileviewer.dialog.LanguageSettingLoadingDialog
import com.documentpro.office.business.fileviewer.ui.setting.BusinessSettingActivity
import kotlinx.coroutines.delay
import net.corekit.core.report.ReportDataManager


class LanguageActivity : BaseActivity<ActivityLanguageBinding, BusinessMainModel>() {

    companion object {
        var pinCurrentLang = true
        private const val TAG = "LanguageActivity"
        fun launch(context: Context, fromSetting: Boolean) {
            context.startActivity(Intent().apply {
                setClass(context, LanguageActivity::class.java)
                putExtra("fromSetting", fromSetting)
            })
        }
    }

    private lateinit var languageAdapter: BusinessLanguageAdapter
    private lateinit var languageList: MutableList<BusinessLanguageItem>

    private var initLanguage = false
    private var initGuide = false

    private val currentLanguageCode: String
        get() = BusinessLanguageController.getInstance().getAliens()

    private val fromSetting: Boolean
        get() = intent.getBooleanExtra("fromSetting",false)

    private var countDownTimer: CountDownTimer? = null
    private val countDownDuration = 3000L // 3秒
    private val countDownInterval = 1000L // 1秒更新一次


    override fun initBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(layoutInflater)
    }

    override fun initModel(): BusinessMainModel {
        return viewModels<BusinessMainModel>().value
    }

    override fun useDefaultToolbar(toolbar: Toolbar, title: String) {
        super.useDefaultToolbar(toolbar, title)
        if (!initLanguage || !initGuide) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
        ImmersionBar.with(this).statusBarDarkFont(true).hideBar(BarHide.FLAG_HIDE_NAVIGATION_BAR)
            .titleBar(binding.toolbar).init()
    }

    override fun initView() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if(fromSetting){
                    confirmLanguageSelection()
                }
            }
        })
        binding.btnNext.setOnClickListener {
            cancelCountDown()
            confirmLanguageSelection()
        }
        useDefaultToolbar(binding.toolbar, "")
        initLanguage = BusinessSPConfig.isInitLanguage()
        initGuide = SPUtils.getInstance().getBoolean("isGuide", false)

        languageList = execAction_2()

        languageAdapter = BusinessLanguageAdapter(languageList){
            // 点击语言项时的回调，可以在这里处理实时预览等
            // 语言选择只在点击确认按钮后生效
            pinCurrentLang = false
        }
        binding.rvLanguage.apply {
            layoutManager = LinearLayoutManager(this@LanguageActivity)
            adapter = languageAdapter
        }
        languageAdapter.setDefaultLanguage(currentLanguageCode)

        loadNative(container = binding.adsContainer)

        execLoad_4()

        // 启动倒计时
//        Handler(Looper.getMainLooper()).postDelayed({
//            startCountDown()
//        },300)
    }

    /**
     * 启动倒计时
     */
    private fun startCountDown() {

        if(fromSetting)return

        cancelCountDown() // 先取消之前的倒计时

        val baseText = getString(R.string.language_btn_done)
        // 先显示3
        binding.btnNext.text = "$baseText (3)"

        countDownTimer = object : CountDownTimer(countDownDuration, countDownInterval) {
            override fun onTick(millisUntilFinished: Long) {
                // 向上取整：(millisUntilFinished + 999) / 1000
                // 这样2000ms显示2，1000ms显示1
                val seconds = ((millisUntilFinished + 999) / 1000).toInt()
                binding.btnNext.text = "$baseText ($seconds)"
            }

            override fun onFinish() {
                binding.btnNext.text = baseText
                // 倒计时结束，自动执行点击事件
                confirmLanguageSelection()
            }
        }.start()
    }

    /**
     * 重置倒计时
     */
    private fun resetCountDown() {
        startCountDown()
    }

    /**
     * 取消倒计时
     */
    private fun cancelCountDown() {
        countDownTimer?.cancel()
        countDownTimer = null
        binding.btnNext.text = getString(R.string.language_btn_done)
    }

    private fun execAction_2(): MutableList<BusinessLanguageItem> {
        val rtlLanguages = setOf(
            BusinessLanguageController.ARABIC,
            BusinessLanguageController.PERSIAN,
            BusinessLanguageController.ITALIAN,
            BusinessLanguageController.DANISH,
            BusinessLanguageController.TURKISH,
            BusinessLanguageController.SWEDISH,
        )

        val allLanguages = BusinessLanguageController.getInstance().getAllLanguages()
        val languages = mutableListOf<BusinessLanguageItem>()
        allLanguages.forEach { (code, displayName) ->
            if (!rtlLanguages.contains(code)) {
                languages.add(BusinessLanguageItem(code, displayName))
            }
        }

        // 将当前语言移到第一位
        if(pinCurrentLang){
            val currentLanguageItem = languages.find { it.code == currentLanguageCode }
            if (currentLanguageItem != null) {
                languages.remove(currentLanguageItem)
                languages.add(0, currentLanguageItem)
            }
        }

        return languages
    }


    /**
     * 确认语言选择
     */
    private fun confirmLanguageSelection() {
        lifecycleScope.launch {
            try {
                loadInterstitial(
                    call = {
                    changeLang()

                        if(!fromSetting){
                        BusinessPointLog.logEvent("Guide", mapOf("Guide" to 2))
                        val isGuide = SPUtils.getInstance().getBoolean("isGuide", true)
                        if (isGuide) {
                            ActivityUtils.startActivity(BusinessWorkspaceActivity::class.java)
                        } else {
                            ActivityUtils.startActivity(GuideActivity::class.java)
                        }
                    }
                    finish()
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun changeLang() {
        ReportDataManager.reportData("language_confirm",mapOf())
        // 检查 languageAdapter 是否已初始化
        if (!::languageAdapter.isInitialized) {
            return
        }

        val currentLanguageCode = BusinessLanguageController.getInstance().getAliens()
        val selectedLanguageItem = languageAdapter.getSelectedLanguage()

        // 没有设置过语言 或者 有设置过，但是选择的语言和缓存的不一样
        if ( (selectedLanguageItem != null && selectedLanguageItem.code != currentLanguageCode)) {
            BusinessLanguageController.getInstance().apply(selectedLanguageItem.code)
            PdfAppInitializer.restartLauncher(this@LanguageActivity)
            ActivityUtils.finishActivity(BusinessSettingActivity::class.java)
            finish()
        }
    }



    override fun initObserve() {
        // No specific observers needed for this activity yet
    }

    override fun initTag(): String {
        return TAG
    }

    private fun execLoad_4() {
    }


    override fun onDestroy() {
        super.onDestroy()
        cancelCountDown()
    }
}
