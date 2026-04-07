package com.documentpro.office.business.fileviewer.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.documentpro.office.business.fileviewer.R
import com.documentpro.office.business.fileviewer.ui.splash.BusinessSplashScreenActivity

/**
 * 动态创建和管理应用快捷方式
 */
object BusinessShortcutManager {

    /**
     * 创建卸载快捷方式
     */
    fun createUninstallShortcut(context: Context): ShortcutInfo {
        val intent = Intent(context, BusinessSplashScreenActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            putExtra("from_shortcut", "uninstall")
            // 设置 flags 确保正确的启动行为
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return ShortcutInfo.Builder(context, "uninstall_shortcut")
            .setShortLabel(context.getString(R.string.shortcut_uninstall_short))
            .setLongLabel(context.getString(R.string.shortcut_uninstall_short))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_trash_outline))
            .setIntent(intent)
            .setCategories(setOf("android.shortcut.conversation"))
            .build()
    }

    /**
     * 根据 BuildConfig 创建 flavor 特定的快捷方式
     */
    fun createFlavorSpecificShortcuts(context: Context): List<ShortcutInfo> {
        val shortcuts = mutableListOf<ShortcutInfo>()

        // 基础卸载快捷方式
        shortcuts.add(createUninstallShortcut(context))

        return shortcuts
    }


    /**
     * 设置应用快捷方式
     */
    fun setAppShortcuts(context: Context) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            try {
                val shortcuts = createFlavorSpecificShortcuts(context)
                shortcutManager.dynamicShortcuts = shortcuts
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 清除所有动态快捷方式
     */
    fun clearAppShortcuts(context: Context) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        shortcutManager?.dynamicShortcuts?.clear()
    }
}
