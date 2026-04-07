package com.documentpro.office.business.fileviewer.ad

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.corekit.core.controller.ChannelUserController
import net.corekit.core.ext.DataStoreStringDelegate
import net.corekit.core.utils.ConfigRemoteManager

/**
 * 主页广告位配置管理器
 * 单个 JSON 配置文件，内部区分 paid_user_tier / organic_user_tier
 * 支持本地 assets 默认配置 + 远程拉取覆盖
 */
@SuppressLint("StaticFieldLeak")
object HomePlacementManager {

    private const val TAG = "HomePlacementManager"
    private const val CONFIG_FILE = "home_placement_config.json"

    // Remote Config Key
    const val KEY_HOME_PLACEMENT_JSON = "launcher_homePlacementJson"

    private var homePlacementJsonFromRemote by DataStoreStringDelegate("homePlacementJsonRemote", "")

    @Volatile
    private var config: HomePlacementConfig? = null

    /**
     * 初始化配置
     */
    fun initialize(context: Context) {
        try {
            initializeWithLocalConfig(context)
            fetchRemoteConfig()
            Log.d(TAG, "HomePlacement 配置初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "HomePlacement 配置初始化失败", e)
        }
    }

    /**
     * 异步获取远程配置
     */
    private fun fetchRemoteConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val remoteJson = ConfigRemoteManager.getString(KEY_HOME_PLACEMENT_JSON, "")
                if (!remoteJson.isNullOrEmpty()) {
                    config = parseConfig(remoteJson)
                    homePlacementJsonFromRemote = remoteJson
                    Log.d(TAG, "远程 HomePlacement 配置更新成功")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取远程 HomePlacement 配置异常", e)
            }
        }
    }

    /**
     * 使用本地配置初始化
     */
    private fun initializeWithLocalConfig(context: Context) {
        val json = homePlacementJsonFromRemote.orEmpty().takeIf { it.isNotEmpty() }
            ?: context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
        config = parseConfig(json)
    }

    private fun parseConfig(jsonString: String): HomePlacementConfig {
        return try {
            Gson().fromJson(jsonString, HomePlacementConfig::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "解析 HomePlacement 配置失败", e)
            HomePlacementConfig()
        }
    }

    /**
     * 获取当前渠道的 HomePlacement 配置
     */
    fun getHomePlacement(): HomePlacementData {
        return try {
            val cfg = config ?: HomePlacementConfig()
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> cfg.organicUserTier
                ChannelUserController.UserChannelType.PAID -> cfg.paidUserTier
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取用户渠道失败，使用默认配置", e)
            config?.organicUserTier ?: HomePlacementData()
        }
    }
}
