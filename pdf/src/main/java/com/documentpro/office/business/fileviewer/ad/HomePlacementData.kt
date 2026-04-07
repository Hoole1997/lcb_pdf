package com.documentpro.office.business.fileviewer.ad

import com.google.gson.annotations.SerializedName

/**
 * 主页广告位完整配置（包含买量和自然量两个渠道）
 */
data class HomePlacementConfig(
    @SerializedName("paid_user_tier")
    val paidUserTier: HomePlacementData = HomePlacementData(),
    @SerializedName("organic_user_tier")
    val organicUserTier: HomePlacementData = HomePlacementData()
)

/**
 * 主页广告位配置数据类
 */
data class HomePlacementData(
    @SerializedName("drawer_close_interstitial_interval_seconds")
    val drawerCloseInterstitialIntervalSeconds: Int = 2147483647,
    @SerializedName("drawer_close_first_install_silent_seconds")
    val drawerCloseFirstInstallSilentSeconds: Int = 1800,
    @SerializedName("drawer_close_interstitial_daily_max_count")
    val drawerCloseInterstitialDailyMaxCount: Int = 20,
    @SerializedName("show_desktop_uninstall_entry")
    val showDesktopUninstallEntry: Boolean = true
)
