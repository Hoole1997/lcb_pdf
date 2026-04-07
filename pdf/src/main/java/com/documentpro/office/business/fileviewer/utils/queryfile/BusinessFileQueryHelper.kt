package com.documentpro.office.business.fileviewer.utils.queryfile

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.Utils
import com.documentpro.office.business.fileviewer.utils.BusinessPdfUtils
import java.io.File
import java.util.HashSet

fun notifySystemToScan() {
    FileUtils.notifySystemToScan(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
    FileUtils.notifySystemToScan(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath)
    FileUtils.notifySystemToScan(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath)
    FileUtils.notifySystemToScan(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath)
}

fun queryFiles(mimeType: String): List<BusinessFileInfo> {
    val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    val picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
    val musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
    val documentsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath

    // 组合查询条件
    val selection = "(${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.DATA} LIKE ?) AND " +
            "(${MediaStore.Files.FileColumns.MIME_TYPE} = ?)" // MIME 类型条件

    // 组合查询参数
    val selectionArgs = arrayOf(
        "$downloadDirectory/%",  // 查询下载目录及其子目录下的文件
        "$picturesDirectory/%",  // 查询图片目录及其子目录下的文件
        "$musicDirectory/%",     // 查询音乐目录及其子目录下的文件
        "$documentsDirectory/%", // 查询文档目录及其子目录下的文件
        "%",                     // 查询其他所有公共目录下的文件
        mimeType
    )

    val files = mutableListOf<BusinessFileInfo>()
    val projection = arrayOf(
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.DATE_ADDED
    )

    // 查询所有文件，根据 MIME 类型筛选
    val cursor: Cursor? = try {
        Utils.getApp().contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            null
        )
    } catch (e: Exception) {
        // 某些设备可能没有外部存储卷，返回空列表
        e.printStackTrace()
        null
    }

    cursor?.use {
        val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val dataIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
        val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateModifiedIndex =
            it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
        val dateCreatedIndex =
            it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED) // 取决于 API 版本
        val isReadable = true // 可以通过 File 类来检查
        val isWritable = true // 可以通过 File 类来检查
        val isHidden = false // 可以通过 File 类来检查

        while (it.moveToNext()) {
            val fileName = it.getString(nameIndex)
            val filePath = it.getString(dataIndex)
            val mimeType = it.getString(mimeIndex)
            
            // 检查必要字段是否为 null，如果为 null 则跳过这条记录
            if (filePath.isNullOrEmpty() || fileName.isNullOrEmpty() || mimeType.isNullOrEmpty()) {
                continue
            }
            
            val fileSize = it.getLong(sizeIndex)
            val dateModified = it.getLong(dateModifiedIndex)
            val dateCreated = it.getLong(dateCreatedIndex) // 如果支持获取
            val extension = filePath.substringAfterLast('.', "") // 获取文件扩展名
            val checksum: String? = null // 如果需要，可以计算或获取
            var isLocked = false
            if (mimeType == MIME_TYPE_PDF) {
                isLocked = BusinessPdfUtils.isPdfEncrypted(filePath)
            }
            
            files.add(
                BusinessFileInfo(
                    name = fileName,
                    path = filePath,
                    type = mimeType,
                    size = fileSize,
                    dateModified = dateModified,
                    dateCreated = dateCreated,
                    extension = extension,
                    isReadable = isReadable,
                    isWritable = isWritable,
                    isHidden = isHidden,
                    checksum = checksum,
                    isLocked = isLocked
                ).apply {
//                        LogUtils.d(GsonUtils.toJson(this))
                }
            )
        }
    }

    // 新增：扫描APP私有目录下的文件
    if (mimeType == MIME_TYPE_PDF ||
        mimeType == MIME_TYPE_WORD ||
        mimeType == MIME_TYPE_TEXT ||
        MIME_TYPE_EXCEL.contains(mimeType) ||
        MIME_TYPE_PPT.contains(mimeType)) {
        // 根据MIME类型确定要查找的文件扩展名
        val fileExtensions = when (mimeType) {
            MIME_TYPE_PDF -> listOf(".pdf")
            MIME_TYPE_WORD -> listOf(".doc", ".docx")
            MIME_TYPE_TEXT -> listOf(".txt")
            else -> {
                if (MIME_TYPE_EXCEL.contains(mimeType)) {
                    listOf(".xlsx")
                } else if (MIME_TYPE_PPT.contains(mimeType)) {
                    listOf(".pptx")
                } else {
                    emptyList()
                }
            }
        }
        
        // 要扫描的目录列表
        val dirsToScan = listOf(
            Utils.getApp().filesDir,  // 根目录
            File(Utils.getApp().filesDir, "doc")  // doc子目录
        )
        
        // 遍历每个目录扫描文件
        for (dir in dirsToScan) {
            if (!dir.exists() || !dir.isDirectory) continue
            
            dir.listFiles { file ->
                file.isFile && fileExtensions.any { ext -> file.name.endsWith(ext, ignoreCase = true) }
            }?.forEach { file ->
                val extension = file.name.substringAfterLast('.', "")
                val isLocked = if (mimeType == MIME_TYPE_PDF) BusinessPdfUtils.isPdfEncrypted(file.absolutePath) else false
                
                files.add(
                    BusinessFileInfo(
                        name = file.name,
                        path = file.absolutePath,
                        type = mimeType,
                        size = file.length(),
                        dateModified = file.lastModified()/1000,
                        dateCreated = file.lastModified()/1000,
                        extension = extension,
                        isReadable = file.canRead(),
                        isWritable = file.canWrite(),
                        isHidden = file.isHidden,
                        checksum = null,
                        isLocked = isLocked
                    )
                )
            }
        }
    }

    return files
}

fun queryPdfFiles(sortType: BusinessSortType = BusinessSortType.MODIFIED_DESC): List<BusinessFileInfo> {
    return sortFileList(queryFiles(MIME_TYPE_PDF), sortType)
}

/**
 * 只查询APP私有目录下的Demo文件（不需要存储权限）
 * @param queryType 查询类型，如 "All", "PDF", "WORD" 等
 * @param sortType 排序类型
 * @return Demo文件列表
 */
fun queryDemoFiles(queryType: String = "All", sortType: BusinessSortType = BusinessSortType.MODIFIED_DESC): List<BusinessFileInfo> {
    val files = mutableListOf<BusinessFileInfo>()
    
    // Demo文件扩展名和对应的MIME类型及文件类型
    val demoFileTypes = listOf(
        Triple(listOf(".pdf"), MIME_TYPE_PDF, BusinessFileType.PDF.name),
        Triple(listOf(".doc", ".docx"), MIME_TYPE_WORD, BusinessFileType.WORD.name),
        Triple(listOf(".xls", ".xlsx"), MIME_TYPE_EXCEL[1], BusinessFileType.EXCEL.name),
        Triple(listOf(".ppt", ".pptx"), MIME_TYPE_PPT[1], BusinessFileType.PPT.name),
        Triple(listOf(".txt"), MIME_TYPE_TEXT, BusinessFileType.TXT.name)
    )
    
    // 根据queryType过滤要查找的文件类型
    val filteredFileTypes = if (queryType == "All") {
        demoFileTypes
    } else {
        demoFileTypes.filter { it.third == queryType }
    }
    
    // 扫描APP私有目录
    val docDir = File(Utils.getApp().filesDir, "doc")
    if (!docDir.exists() || !docDir.isDirectory) {
        return files
    }
    
    // 获取所有符合条件的扩展名
    val allowedExtensions = filteredFileTypes.flatMap { it.first }
    
    docDir.listFiles { file ->
        file.isFile && file.name.startsWith("Demo", ignoreCase = true) &&
            allowedExtensions.any { ext -> file.name.endsWith(ext, ignoreCase = true) }
    }?.forEach { file ->
        val extension = file.name.substringAfterLast('.', "")
        val matchedType = demoFileTypes.find { (extensions, _, _) ->
            extensions.any { ext -> file.name.endsWith(ext, ignoreCase = true) }
        }
        val mimeType = matchedType?.second ?: "application/octet-stream"
        
        val isLocked = if (mimeType == MIME_TYPE_PDF) {
            BusinessPdfUtils.isPdfEncrypted(file.absolutePath)
        } else {
            false
        }
        
        files.add(
            BusinessFileInfo(
                name = file.name,
                path = file.absolutePath,
                type = mimeType,
                size = file.length(),
                dateModified = file.lastModified() / 1000,
                dateCreated = file.lastModified() / 1000,
                extension = extension,
                isReadable = file.canRead(),
                isWritable = file.canWrite(),
                isHidden = file.isHidden,
                checksum = null,
                isLocked = isLocked
            )
        )
    }
    
    // 排序后确保PDF文件始终在第一个
    val sortedFiles = sortFileList(files, sortType)
    val pdfFiles = sortedFiles.filter { it.name.endsWith(".pdf", ignoreCase = true) }
    val otherFiles = sortedFiles.filter { !it.name.endsWith(".pdf", ignoreCase = true) }
    return pdfFiles + otherFiles
}

fun queryWordFiles(sortType: BusinessSortType = BusinessSortType.MODIFIED_DESC): List<BusinessFileInfo> {
    return sortFileList(queryFiles(MIME_TYPE_WORD), sortType)
}

fun queryExcelFiles(sortType: BusinessSortType = BusinessSortType.MODIFIED_DESC): List<BusinessFileInfo> {
    val files = mutableListOf<BusinessFileInfo>()
    val filePathSet = HashSet<String>() // 用于去重
    
    for (mimeType in MIME_TYPE_EXCEL) {
        queryFiles(mimeType).forEach { fileInfo ->
            // 使用文件路径作为唯一标识进行去重
            if (!filePathSet.contains(fileInfo.path)) {
                filePathSet.add(fileInfo.path)
                files.add(fileInfo)
            }
        }
    }
    return sortFileList(files, sortType)
}

fun queryTextFiles(sortType: BusinessSortType = BusinessSortType.MODIFIED_DESC): List<BusinessFileInfo> {
    return sortFileList(queryFiles(MIME_TYPE_TEXT), sortType)
}

fun queryImageFiles(sortType: BusinessSortType = BusinessSortType.MODIFIED_DESC): List<BusinessFileInfo> {
    val files = mutableListOf<BusinessFileInfo>()
    for (mimeType in MIME_TYPE_IMAGE) {
        files.addAll(queryFiles(mimeType))
    }
    return sortFileList(files, sortType)
}

fun queryPptFiles(sortType: BusinessSortType = BusinessSortType.MODIFIED_DESC): List<BusinessFileInfo> {
    val files = mutableListOf<BusinessFileInfo>()
    val filePathSet = HashSet<String>() // 用于去重
    
    for (mimeType in MIME_TYPE_PPT) {
        queryFiles(mimeType).forEach { fileInfo ->
            // 使用文件路径作为唯一标识进行去重
            if (!filePathSet.contains(fileInfo.path)) {
                filePathSet.add(fileInfo.path)
                files.add(fileInfo)
            }
        }
    }
    return sortFileList(files, sortType)
}

fun sortFileList(list: List<BusinessFileInfo>, sortType: BusinessSortType): List<BusinessFileInfo> {
    return when (sortType) {
        BusinessSortType.MODIFIED_DESC -> list.sortedByDescending { it.dateModified }
        BusinessSortType.MODIFIED_ASC -> list.sortedBy { it.dateModified }
        BusinessSortType.NAME_ASC -> list.sortedBy { it.name.lowercase() }
        BusinessSortType.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
        BusinessSortType.SIZE_DESC -> list.sortedByDescending { it.size }
        BusinessSortType.SIZE_ASC -> list.sortedBy { it.size }
    }
}

fun equalsFileType(mineType: String): BusinessFileType {
    return when (mineType) {
        MIME_TYPE_PDF -> BusinessFileType.PDF
        MIME_TYPE_WORD -> BusinessFileType.WORD
        in MIME_TYPE_EXCEL -> BusinessFileType.EXCEL
        MIME_TYPE_TEXT -> BusinessFileType.TXT
        in MIME_TYPE_IMAGE -> BusinessFileType.IMAGE
        in MIME_TYPE_PPT -> BusinessFileType.PPT
        else -> BusinessFileType.PDF
    }
}

const val MIME_TYPE_PDF = "application/pdf"
const val MIME_TYPE_WORD = "application/msword"
val MIME_TYPE_EXCEL = arrayOf(
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
)
const val MIME_TYPE_TEXT = "text/plain"
val MIME_TYPE_IMAGE = arrayOf(
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/bmp",
    "image/webp",
    "image/svg+xml"
)
val MIME_TYPE_PPT = arrayOf(
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.openxmlformats-officedocument.presentationml.template"
)

fun Fragment.pickPdfFile(onResult: (BusinessFileInfo?) -> Unit) {
    val launcher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            onResult(fileInfoFromUri(requireContext(), uri))
        } else {
            onResult(null)
        }
    }
    launcher.launch(arrayOf("application/pdf"))
}

fun Activity.pickPdfFile(onResult: (BusinessFileInfo?) -> Unit) {
    val launcher = (this as ActivityResultCaller).registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            onResult(fileInfoFromUri(this, uri))
        } else {
            onResult(null)
        }
    }
    launcher.launch(arrayOf("application/pdf"))
}

/**
 * 根据文件路径获取FileInfo对象
 * @param filePath 文件的完整路径
 * @return FileInfo对象，如果文件不存在或无法访问则返回null
 */
fun fileInfoFromPath(filePath: String): BusinessFileInfo? {
    return try {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            return null
        }
        
        val extension = file.name.substringAfterLast('.', "")
        val mimeType = getMimeTypeFromExtension(extension)
        val isLocked = if (mimeType == MIME_TYPE_PDF) {
            BusinessPdfUtils.isPdfEncrypted(filePath)
        } else {
            false
        }
        
        BusinessFileInfo(
            name = file.name,
            path = file.absolutePath,
            type = mimeType,
            size = file.length(),
            dateModified = file.lastModified() / 1000,
            dateCreated = file.lastModified() / 1000,
            extension = extension,
            isReadable = file.canRead(),
            isWritable = file.canWrite(),
            isHidden = file.isHidden,
            checksum = null,
            isLocked = isLocked
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 根据文件扩展名获取MIME类型
 * @param extension 文件扩展名（不含点号）
 * @return 对应的MIME类型
 */
private fun getMimeTypeFromExtension(extension: String): String {
    return when (extension.lowercase()) {
        "pdf" -> MIME_TYPE_PDF
        "doc", "docx" -> MIME_TYPE_WORD
        "xls" -> MIME_TYPE_EXCEL[0]
        "xlsx" -> MIME_TYPE_EXCEL[1]
        "txt" -> MIME_TYPE_TEXT
        "jpg", "jpeg" -> MIME_TYPE_IMAGE[0]
        "png" -> MIME_TYPE_IMAGE[1]
        "gif" -> MIME_TYPE_IMAGE[2]
        "bmp" -> MIME_TYPE_IMAGE[3]
        "webp" -> MIME_TYPE_IMAGE[4]
        "svg" -> MIME_TYPE_IMAGE[5]
        "ppt" -> MIME_TYPE_PPT[0]
        "pptx" -> MIME_TYPE_PPT[1]
        else -> "application/octet-stream" // 默认二进制类型
    }
}

fun fileInfoFromUri(context: Context, uri: Uri): BusinessFileInfo? {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            val name = if (nameIndex != -1) it.getString(nameIndex) else "unknown.pdf"
            val size = if (sizeIndex != -1) it.getLong(sizeIndex) else 0L
            val extension = name.substringAfterLast('.', "pdf")
            val isLocked = try {
                contentResolver.openInputStream(uri)?.use { input ->
                    BusinessPdfUtils.isPdfEncrypted(input)
                } ?: false
            } catch (e: Exception) {
                false
            }
            // 拷贝到cache目录，文件名用真实文件名
            val cacheFile = File(context.cacheDir, name)
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // 拷贝失败，降级用uri
                return BusinessFileInfo(
                    name = name,
                    path = uri.toString(),
                    type = "application/pdf",
                    size = size,
                    dateModified = System.currentTimeMillis(),
                    dateCreated = System.currentTimeMillis(),
                    extension = extension,
                    isReadable = true,
                    isWritable = false,
                    isHidden = false,
                    checksum = null,
                    isLocked = isLocked
                )
            }
            return BusinessFileInfo(
                name = name,
                path = cacheFile.absolutePath,
                type = "application/pdf",
                size = size,
                dateModified = System.currentTimeMillis(),
                dateCreated = System.currentTimeMillis(),
                extension = extension,
                isReadable = true,
                isWritable = false,
                isHidden = false,
                checksum = null,
                isLocked = isLocked
            )
        }
    }
    return null
}
