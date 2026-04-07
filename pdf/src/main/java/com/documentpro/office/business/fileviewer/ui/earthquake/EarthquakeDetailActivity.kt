package com.documentpro.office.business.fileviewer.ui.earthquake

import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.blankj.utilcode.util.StringUtils
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.base.BaseActivity
import com.documentpro.office.business.fileviewer.databinding.ActivityEarthquakeDetailBinding
import com.documentpro.office.business.fileviewer.utils.loadInterstitial
import com.documentpro.office.business.fileviewer.utils.loadNative
import com.google.gson.Gson
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import io.docview.push.builder.LANDING_NOTIFICATION_EARTHQUAKE_DATA
import io.docview.push.earthquake.EarthquakeInfo
import com.android.common.bill.ui.NativeAdStyleType

class EarthquakeDetailActivity : BaseActivity<ActivityEarthquakeDetailBinding, EarthquakeDetailModel>() {

    private val gson = Gson()

    override fun initBinding(): ActivityEarthquakeDetailBinding {
        return ActivityEarthquakeDetailBinding.inflate(layoutInflater)
    }

    override fun initModel(): EarthquakeDetailModel {
        return viewModels<EarthquakeDetailModel>().value
    }

    override fun finish() {
        loadInterstitial {
            super.finish()
        }
    }

    override fun initView() {
        // 从Intent获取地震数据
        val earthquakeJson = intent.getStringExtra(LANDING_NOTIFICATION_EARTHQUAKE_DATA)
        val earthquake = if (earthquakeJson != null) {
            try {
                gson.fromJson(earthquakeJson, EarthquakeInfo::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }

        loadNative(binding.adViewContainer, styleType = NativeAdStyleType.LARGE)

        // 设置Toolbar
        useDefaultToolbar(binding.toolbar,"")

        // 从Intent获取数据并显示
        if (earthquake != null) {
            binding.ivBg.setImageResource(when(earthquake.alert){
                "green"-> R.mipmap.bg_earth_q_4
                "yellow"-> R.mipmap.bg_earth_q_2
                "orange"-> R.mipmap.bg_earth_q_3
                "red"-> R.mipmap.bg_earth_q_1
                else -> R.mipmap.bg_earth_q_4
            })
            displayEarthquakeData(
                magnitude = earthquake.magnitude.toString(),
                location = earthquake.place.orEmpty(),
                focalDepth = "${earthquake.depth}"+ "KM",
                tsunamiThreat = if (earthquake.hasTsunami) "Yes" else "No",
                usgsWarning = earthquake.alert.orEmpty().uppercase(),
                originTime = earthquake.time,
                magnitudeType = earthquake.magType.orEmpty(),
                dataStatus = earthquake.status.orEmpty()
            )
        }
    }

    override fun initObserve() {
        // 可以在这里添加LiveData观察者
    }

    override fun initTag(): String {
        return "EarthquakeDetailActivity"
    }

    /**
     * 显示地震数据
     */
    private fun displayEarthquakeData(
        magnitude: String,
        location: String,
        focalDepth: String,
        tsunamiThreat: String,
        usgsWarning: String,
        originTime: String,
        magnitudeType: String,
        dataStatus: String
    ) {
        binding.tvMagnitude.text = magnitude
        binding.tvMagnitudeUnit.text = "m"
        binding.tvLocation.text = location
        binding.tvFocalDepthValue.text = focalDepth
        binding.tvTsunamiThreatValue.text = tsunamiThreat
        binding.tvUsgsWarningValue.text = usgsWarning
        binding.tvOriginTimeValue.text = originTime
        binding.tvMagnitudeTypeValue.text = magnitudeType
        binding.tvDataStatusValue.text = dataStatus
    }

    override fun initWindowPadding() {
        binding.main?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }
}

