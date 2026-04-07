package com.documentpro.office.business.fileviewer.utils

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.UserHandle
import android.provider.MediaStore
import com.documentpro.office.business.fileviewer.ad.BusinessPointLog
import com.documentpro.office.business.fileviewer.ui.clean.CleanItem
import com.documentpro.office.business.fileviewer.ui.clean.CleanType
import java.io.File

object BusinessCleanUtils {
    private val STANDARD_SIZES_GB = listOf(16, 32, 64, 128, 256, 512, 1024, 2048)
    private const val GB: Long = 1_073_741_824L // 1024^3
    private const val TOLERANCE = 0.05 // 5% 误差

    // 图片文件扩展名
    private val IMAGE_EXTENSIONS = setOf(
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".heic", ".heif"
    )

    // 文档文件扩展名
    private val DOCUMENT_EXTENSIONS = setOf(
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
        ".txt", ".rtf", ".csv", ".xml", ".json"
    )

    /**
     * 获取主存储+系统分区总空间（字节），自动匹配最接近的标准容量
     * @return 标准容量（字节）
     */
    fun getDeviceTotalStorage(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val systemStat = StatFs(Environment.getRootDirectory().absolutePath)
        val realTotal = stat.totalBytes + systemStat.totalBytes

        val realGB = realTotal.toDouble() / GB
        // 找到差值最小的标准容量
        val matched = STANDARD_SIZES_GB.minByOrNull { std -> kotlin.math.abs(realGB - std) } ?: realGB
        return matched.toLong() * GB
    }

    /**
     * 获取主存储+系统分区已用空间（字节）
     * @return 主存储+系统分区已用空间（字节）
     */
    fun getDeviceUsedStorage(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val systemStat = StatFs(Environment.getRootDirectory().absolutePath)
        val used = stat.totalBytes - stat.availableBytes
        val systemUsed = systemStat.totalBytes - systemStat.availableBytes
        return used + systemUsed
    }

    /**
     * 扫描垃圾文件
     */
    fun scanJunkFiles(context: Context, contentResolver: ContentResolver, onFileFound: (CleanItem) -> Unit) {
        // 扫描下载目录
        scanDirectory(contentResolver, "%/Download/%", onFileFound)
        
        // 扫描图片目录
//        scanDirectory(contentResolver, "%/DCIM/%", onFileFound)
//        scanDirectory(contentResolver, "%/Pictures/%", onFileFound)
        
        // 扫描文档目录
        scanDirectory(contentResolver, "%/Documents/%", onFileFound)
        
        // 扫描已卸载应用的残留数据
        scanUninstalledAppData(context, onFileFound)
    }
    
    /**
     * 扫描已卸载应用的残留数据
     * @param context 上下文
     * @param onFileFound 找到文件时的回调
     */
    fun scanUninstalledAppData(context: Context, onFileFound: (CleanItem) -> Unit) {
        try {
            // 获取所有已安装应用的包名列表
            val installedPackages = getInstalledPackageNames(context)
            
            // 扫描 /Android/data 目录
            scanUninstalledAppFolder(
                File(Environment.getExternalStorageDirectory(), "Android/data"),
                installedPackages,
                onFileFound
            )
            
            // 扫描 /Android/obb 目录
            scanUninstalledAppFolder(
                File(Environment.getExternalStorageDirectory(), "Android/obb"),
                installedPackages,
                onFileFound
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取所有已安装应用的包名集合
     */
    private fun getInstalledPackageNames(context: Context): Set<String> {
        return try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            installedApps.map { it.packageName }.toSet()
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }
    
    /**
     * 扫描指定目录下已卸载应用的文件夹
     * @param directory 要扫描的目录（如 /Android/data 或 /Android/obb）
     * @param installedPackages 已安装应用的包名集合
     * @param onFileFound 找到文件时的回调
     */
    private fun scanUninstalledAppFolder(
        directory: File,
        installedPackages: Set<String>,
        onFileFound: (CleanItem) -> Unit
    ) {
        if (!directory.exists() || !directory.isDirectory) {
            return
        }
        
        try {
            val subFolders = directory.listFiles { file ->
                file.isDirectory
            } ?: return
            
            for (folder in subFolders) {
                val folderName = folder.name
                
                // 检查文件夹名（通常是包名）是否对应已安装的应用
                if (!installedPackages.contains(folderName)) {
                    // 这是已卸载应用的残留数据
                    val size = calculateFolderSize(folder)
                    
                    // 生成一个唯一ID（使用路径的hashCode）
                    val id = folder.absolutePath.hashCode().toLong()
                    
                    onFileFound(
                        CleanItem(
                            id = id,
                            name = folderName,
                            path = folder.absolutePath,
                            size = size,
                            type = CleanType.JUNK
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 计算文件夹大小
     * @param folder 文件夹
     * @return 文件夹总大小（字节）
     */
    private fun calculateFolderSize(folder: File): Long {
        var size = 0L
        try {
            folder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
        } catch (e: Exception) {
            // 权限问题或其他异常时，返回0
            e.printStackTrace()
        }
        return size
    }

    private fun scanDirectory(contentResolver: ContentResolver, pathPattern: String, onFileFound: (CleanItem) -> Unit) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf(pathPattern)
        val cursor = try {
            contentResolver.query(uri, projection, selection, selectionArgs, null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: continue
                val path = it.getString(pathIndex) ?: continue
                val size = it.getLong(sizeIndex)
                val mimeType = it.getString(mimeIndex)

                if (isJunkFile(path)) {
                    try {
                        onFileFound(CleanItem(id, name, path, size, CleanType.JUNK))
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                        BusinessPointLog.logEvent("Exception", mapOf("reason" to "BusinessCleanUtils scanDirectory null"))
                    }
                }
            }
        }
    }

    /**
     * 扫描过时APK
     */
    fun scanObsoleteApks(contentResolver: ContentResolver, onFileFound: (CleanItem) -> Unit) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?) AND ${MediaStore.Files.FileColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf(
            "application/vnd.android.package-archive",
            "%.apk",
            "%/Download/%"
        )
        val cursor = try {
            contentResolver.query(uri, projection, selection, selectionArgs, null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: continue
                val path = it.getString(pathIndex) ?: continue
                val size = it.getLong(sizeIndex)
                val mimeType = it.getString(mimeIndex)

                if (isObsoleteApk(path)) {
                    onFileFound(CleanItem(id, name, path, size, CleanType.OBSOLETE_APK))
                }
            }
        }
    }

    /**
     * 扫描临时文件
     */
    fun scanTempFiles(contentResolver: ContentResolver, onFileFound: (CleanItem) -> Unit) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/temp/%", "%/cache/%")
        val cursor = try {
            contentResolver.query(uri, projection, selection, selectionArgs, null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: continue
                val path = it.getString(pathIndex) ?: continue
                val size = it.getLong(sizeIndex)
                val mimeType = it.getString(mimeIndex)

                if (isTempFile(path)) {
                    onFileFound(CleanItem(id, name, path, size, CleanType.TEMP))
                }
            }
        }
    }

    /**
     * 扫描日志文件
     */
    fun scanLogFiles(contentResolver: ContentResolver, onFileFound: (CleanItem) -> Unit) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("%.log", "%/logs/%")
        val cursor = try {
            contentResolver.query(uri, projection, selection, selectionArgs, null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val pathIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: continue
                val path = it.getString(pathIndex) ?: continue
                val size = it.getLong(sizeIndex)
                val mimeType = it.getString(mimeIndex)

                if (isLogFile(path)) {
                    onFileFound(CleanItem(id, name, path, size, CleanType.LOG))
                }
            }
        }
    }

    private fun isJunkFile(path: String): Boolean {
        val file = File(path)
        val name = file.name.lowercase()
        
        // 检查是否是隐藏文件或文件夹
        if (file.name.startsWith(".")) {
            return true
        }
        
        // 检查是否是临时文件
        if (name.endsWith(".tmp") || name.endsWith(".temp")) {
            return true
        }
        
        // 检查是否是下载的临时文件
        if (name.endsWith(".crdownload") || name.endsWith(".part")) {
            return true
        }
        
        // 检查是否是缓存文件
        if (name.endsWith(".cache")) {
            return true
        }
        
        // 检查是否是空文件
        if (file.length() == 0L) {
            return true
        }

        // 检查是否是图片文件
//        if (IMAGE_EXTENSIONS.any { name.endsWith(it) }) {
//            // 检查是否是重复的图片（例如：IMG_20240101_1.jpg, IMG_20240101_2.jpg）
//            val baseName = name.substringBeforeLast("_")
//            val parentDir = file.parentFile
//            if (parentDir != null) {
//                val similarFiles = parentDir.listFiles { f ->
//                    f.name.lowercase().startsWith(baseName) &&
//                    IMAGE_EXTENSIONS.any { ext -> f.name.lowercase().endsWith(ext) }
//                }
//                if (similarFiles?.size ?: 0 > 1) {
//                    return true
//                }
//            }
//        }
//
//        // 检查是否是文档文件
//        if (DOCUMENT_EXTENSIONS.any { name.endsWith(it) }) {
//            // 检查是否是重复的文档（例如：document_1.pdf, document_2.pdf）
//            val baseName = name.substringBeforeLast("_")
//            val parentDir = file.parentFile
//            if (parentDir != null) {
//                val similarFiles = parentDir.listFiles { f ->
//                    f.name.lowercase().startsWith(baseName) &&
//                    DOCUMENT_EXTENSIONS.any { ext -> f.name.lowercase().endsWith(ext) }
//                }
//                if (similarFiles?.size ?: 0 > 1) {
//                    return true
//                }
//            }
//        }
        
        return false
    }

    private fun isObsoleteApk(path: String): Boolean {
        val file = File(path)
        // 检查是否是APK文件
        if (!file.name.endsWith(".apk")) {
            return false
        }
        
        //        // 检查文件是否超过30天未修改
        //        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        //        return file.lastModified() < thirtyDaysAgo
        return true
    }

    private fun isTempFile(path: String): Boolean {
        val file = File(path)
        // 检查是否是临时文件
        if (file.name.endsWith(".tmp") || file.name.endsWith(".temp")) {
            return true
        }
        
        // 检查是否是下载的临时文件
        if (file.name.endsWith(".crdownload") || file.name.endsWith(".part")) {
            return true
        }
        
        // 检查是否是缓存文件
        if (file.name.endsWith(".cache")) {
            return true
        }
        
        return false
    }

    private fun isLogFile(path: String): Boolean {
        val file = File(path)
        // 检查是否是日志文件
        if (file.name.endsWith(".log")) {
            return true
        }
        
        // 检查是否在logs目录下
        if (path.contains("/logs/")) {
            return true
        }
        
        return false
    }

    /**
     * 获取手机所有app占用存储大小
     * @param context Context
     * @return 所有应用占用的总存储大小（字节）
     */
    fun getAllAppsStorageSize(context: Context): Long {
        // 1) 若具备权限且为 O+，优先使用 StorageStatsManager 获取更准确的数据
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasUsageStatsPermission(context)) {
            try {
                var accurateTotal = 0L
                val pm = context.packageManager
                val storageStatsManager = context.getSystemService(StorageStatsManager::class.java)
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (app in apps) {
                    try {
                        val uuid = app.storageUuid
                        val user = UserHandle.getUserHandleForUid(app.uid)
                        val stats = storageStatsManager.queryStatsForPackage(uuid, app.packageName, user)
                        accurateTotal += stats.appBytes + stats.dataBytes + stats.cacheBytes
                    } catch (e: SecurityException) {
                        // 无权限时安全回退
                        accurateTotal += estimateApkBytes(app)
                    } catch (e: Exception) {
                        accurateTotal += estimateApkBytes(app)
                    }
                }
                return accurateTotal
            } catch (e: Exception) {
                // 出现异常则继续走近似方案
                e.printStackTrace()
            }
        }

        // 2) 近似方案（方案C）：APK体积 + 残差的一部分
        val contentResolver = context.contentResolver
        val videosSize = getAllVideosStorageSize(contentResolver)
        val imagesSize = getAllImagesStorageSize(contentResolver)
        val musicSize = getAllMusicStorageSize(contentResolver)

        val pm = context.packageManager
        val apps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList<ApplicationInfo>()
        }

        // APK 体积（含 split APK）。为避免过度扫描，这里不深入遍历 /Android/data，仅在 Android 10 及以下追加 OBB 目录体积
        var apkAndObbBytes = 0L
        for (app in apps) {
            apkAndObbBytes += estimateApkBytes(app)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                apkAndObbBytes += estimateObbBytes(app)
            }
        }

        val usedTotal = getDeviceUsedStorage()
        val residual = (usedTotal - videosSize - imagesSize - musicSize - apkAndObbBytes).coerceAtLeast(0L)
        val residualWeight = 0.6 // 可调系数，0.5~0.7 之间
        val approx = apkAndObbBytes + (residual * residualWeight).toLong()

        // 结果做边界保护
        return approx.coerceIn(0L, usedTotal)
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    private fun estimateApkBytes(appInfo: ApplicationInfo): Long {
        var size = 0L
        try {
            size += File(appInfo.sourceDir).length()
        } catch (_: Exception) { }
        try {
            val splits = appInfo.splitSourceDirs
            if (splits != null) {
                for (path in splits) {
                    size += try { File(path).length() } catch (_: Exception) { 0L }
                }
            }
        } catch (_: Exception) { }
        return size
    }

    private fun estimateObbBytes(appInfo: ApplicationInfo): Long {
        return try {
            val obbDir = File(Environment.getExternalStorageDirectory(), "Android/obb/${appInfo.packageName}")
            directorySizeShallow(obbDir)
        } catch (_: Exception) { 0L }
    }

    private fun directorySizeShallow(dir: File?): Long {
        if (dir == null || !dir.exists() || !dir.isDirectory) return 0L
        var total = 0L
        try {
            val files = dir.listFiles() ?: return 0L
            for (f in files) {
                total += if (f.isFile) f.length() else 0L
            }
        } catch (_: Exception) { }
        return total
    }

    /**
     * 获取手机所有video占用存储大小
     * @param contentResolver ContentResolver
     * @return 所有视频文件占用的总存储大小（字节）
     */
    fun getAllVideosStorageSize(contentResolver: ContentResolver): Long {
        var totalSize = 0L
        try {
            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Video.Media.SIZE)
            
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                while (cursor.moveToNext()) {
                    totalSize += cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return totalSize
    }

    /**
     * 获取手机所有image占用存储大小
     * @param contentResolver ContentResolver
     * @return 所有图片文件占用的总存储大小（字节）
     */
    fun getAllImagesStorageSize(contentResolver: ContentResolver): Long {
        var totalSize = 0L
        try {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Images.Media.SIZE)
            
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                while (cursor.moveToNext()) {
                    totalSize += cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return totalSize
    }

    /**
     * 获取手机所有music占用存储大小
     * @param contentResolver ContentResolver
     * @return 所有音乐文件占用的总存储大小（字节）
     */
    fun getAllMusicStorageSize(contentResolver: ContentResolver): Long {
        var totalSize = 0L
        try {
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Audio.Media.SIZE)
            
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
                    totalSize += cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return totalSize
    }
}