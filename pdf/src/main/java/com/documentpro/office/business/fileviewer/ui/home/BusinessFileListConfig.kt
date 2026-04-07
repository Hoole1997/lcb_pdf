package com.documentpro.office.business.fileviewer.ui.home

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileType

/**
 * BusinessFileListFragment 的配置参数
 * 统一管理所有片段参数，提高可读性和维护性
 */
@Parcelize
data class BusinessFileListConfig(
    /** 查询的文件类型 */
    val queryType: String = BusinessFileType.PDF.name,
    /** 数据源类型 */
    val dataSource: DataSource = DataSource.FILE_SYSTEM,
    /** 显示模式 */
    val displayMode: DisplayMode = DisplayMode.NORMAL,
    /** 锁定状态过滤 */
    val lockFilter: LockFilter = LockFilter.ALL
) : Parcelable {

    /**
     * 数据源枚举
     */
    enum class DataSource(val value: Int) {
        /** 文件系统数据源 */
        FILE_SYSTEM(0),
        /** 最近文件数据源 */
        RECENT(1),
        /** 收藏文件数据源 */
        FAVORITE(2);

        companion object {
            fun fromValue(value: Int): DataSource {
                return entries.find { it.value == value } ?: FILE_SYSTEM
            }
        }
    }

    /**
     * 显示模式枚举
     */
    enum class DisplayMode {
        /** 普通浏览模式 */
        NORMAL,
        /** 文件选择模式 */
        CHOOSE,
        /** 打印模式 */
        PRINT
    }

    /**
     * 锁定状态过滤枚举
     */
    enum class LockFilter(val value: Int) {
        /** 显示所有文件 */
        ALL(0),
        /** 只显示已锁定文件 */
        LOCKED_ONLY(1),
        /** 只显示未锁定文件 */
        UNLOCKED_ONLY(2);

        companion object {
            fun fromValue(value: Int): LockFilter {
                return entries.find { it.value == value } ?: ALL
            }
        }
    }

    /**
     * 便捷的状态检查方法
     */
    val isChooseMode: Boolean get() = displayMode == DisplayMode.CHOOSE
    val isPrintMode: Boolean get() = displayMode == DisplayMode.PRINT
    val isNormalMode: Boolean get() = displayMode == DisplayMode.NORMAL
    
    val isFileSystemSource: Boolean get() = dataSource == DataSource.FILE_SYSTEM
    val isRecentSource: Boolean get() = dataSource == DataSource.RECENT
    val isFavoriteSource: Boolean get() = dataSource == DataSource.FAVORITE
    
    val showAllFiles: Boolean get() = lockFilter == LockFilter.ALL
    val showLockedOnly: Boolean get() = lockFilter == LockFilter.LOCKED_ONLY
    val showUnlockedOnly: Boolean get() = lockFilter == LockFilter.UNLOCKED_ONLY

    companion object {
        /** Bundle key */
        const val BUNDLE_KEY = "file_list_config"

        /**
         * 创建普通浏览配置
         */
        fun normal(
            queryType: String = BusinessFileType.PDF.name,
            dataSource: DataSource = DataSource.FILE_SYSTEM
        ): BusinessFileListConfig {
            return BusinessFileListConfig(
                queryType = queryType,
                dataSource = dataSource,
                displayMode = DisplayMode.NORMAL,
                lockFilter = LockFilter.ALL
            )
        }

        /**
         * 创建文件选择配置
         */
        fun choose(
            queryType: String = BusinessFileType.PDF.name
        ): BusinessFileListConfig {
            return BusinessFileListConfig(
                queryType = queryType,
                dataSource = DataSource.FILE_SYSTEM,
                displayMode = DisplayMode.CHOOSE,
                lockFilter = LockFilter.ALL
            )
        }

        /**
         * 创建打印模式配置
         */
        fun print(): BusinessFileListConfig {
            return BusinessFileListConfig(
                queryType = BusinessFileType.PDF.name,
                dataSource = DataSource.FILE_SYSTEM,
                displayMode = DisplayMode.PRINT,
                lockFilter = LockFilter.ALL
            )
        }

        /**
         * 创建最近文件配置
         */
        fun recent(queryType: String = BusinessFileType.PDF.name): BusinessFileListConfig {
            return BusinessFileListConfig(
                queryType = queryType,
                dataSource = DataSource.RECENT,
                displayMode = DisplayMode.NORMAL,
                lockFilter = LockFilter.ALL
            )
        }

        /**
         * 创建收藏文件配置
         */
        fun favorite(queryType: String = BusinessFileType.PDF.name): BusinessFileListConfig {
            return BusinessFileListConfig(
                queryType = queryType,
                dataSource = DataSource.FAVORITE,
                displayMode = DisplayMode.NORMAL,
                lockFilter = LockFilter.ALL
            )
        }

        /**
         * 创建PDF锁定管理配置
         */
        fun pdfLock(lockFilter: LockFilter): BusinessFileListConfig {
            return BusinessFileListConfig(
                queryType = BusinessFileType.PDF.name,
                dataSource = DataSource.FILE_SYSTEM,
                displayMode = DisplayMode.NORMAL,
                lockFilter = lockFilter
            )
        }
    }
}

