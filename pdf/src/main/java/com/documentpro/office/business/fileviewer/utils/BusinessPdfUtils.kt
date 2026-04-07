package com.documentpro.office.business.fileviewer.utils

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.pdf.EncryptionConstants
import com.itextpdf.kernel.pdf.ReaderProperties
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.File
import java.io.InputStream
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.layout.element.Image
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.documentpro.office.business.fileviewer.ui.pdf.model.BusinessPdfPageInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.BusinessFileInfo
import com.documentpro.office.business.fileviewer.utils.queryfile.fileInfoFromPath
import java.io.FileOutputStream
import java.util.*

object BusinessPdfUtils {
    /**
     * 判断PDF文件是否加密
     */
    fun isPdfEncrypted(filePath: String): Boolean {
        return try {
            val reader = PdfReader(filePath)
            val pdfDoc = PdfDocument(reader)
            val encrypted = pdfDoc.reader.isEncrypted
            pdfDoc.close()
            encrypted
        } catch (e: Exception) {
            true // 打开失败也认为加密
        }
    }
    
    /**
     * 判断PDF文件是否加锁（isPdfEncrypted的别名）
     * @param filePath PDF文件路径
     * @return 是否加锁
     */
    fun isPdfLocked(filePath: String): Boolean = isPdfEncrypted(filePath)

    /**
     * 加密PDF文件
     * @param src 原PDF路径
     * @param dest 输出加密PDF路径
     * @param userPassword 用户密码
     * @param ownerPassword 拥有者密码
     */
    fun encryptPdf(src: String, dest: String, userPassword: String, ownerPassword: String): Boolean {
        return try {
            val reader = PdfReader(src)
            val writerProps = WriterProperties()
                .setStandardEncryption(
                    userPassword.toByteArray(),
                    ownerPassword.toByteArray(),
                    EncryptionConstants.ALLOW_PRINTING,
                    EncryptionConstants.ENCRYPTION_AES_128
                )
            val writer = PdfWriter(dest, writerProps)
            val pdfDoc = PdfDocument(reader, writer)
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 解密PDF文件
     * @param src 原PDF路径
     * @param dest 输出解密PDF路径
     * @param password 密码
     */
    fun decryptPdf(src: String, dest: String, password: String): Boolean {
        return try {
            val reader = PdfReader(src, ReaderProperties().setPassword(password.toByteArray()))
            val writer = PdfWriter(dest)
            val pdfDoc = PdfDocument(reader, writer)
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 创建一个简单的PDF文件
     * @param dest 输出PDF路径
     * @param content PDF内容
     */
    fun createPdf(dest: String, content: String): Boolean {
        return try {
            val writer = PdfWriter(dest)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)
            document.add(Paragraph(content))
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

    /**
     * 原地加密PDF文件（安全覆盖）
     */
    fun encryptPdfInPlace(src: String, userPassword: String, ownerPassword: String): Boolean {
        val tmpFile = src + ".tmp"
        val success = encryptPdf(src, tmpFile, userPassword, ownerPassword)
        val srcFile = File(src)
        val tmp = File(tmpFile)
        if (success) {
            if (srcFile.delete()) {
                val renameOk = tmp.renameTo(srcFile)
                if (!renameOk) {
                    tmp.delete()
                    return false
                }
                return true
            } else {
                tmp.delete()
                return false
            }
        }
        tmp.delete()
        return false
    }

    /**
     * 原地解密PDF文件（安全覆盖）
     */
    fun decryptPdfInPlace(src: String, password: String): Boolean {
        val tmpFile = src + ".tmp"
        val success = decryptPdf(src, tmpFile, password)
        val srcFile = File(src)
        val tmp = File(tmpFile)
        if (success) {
            if (srcFile.delete()) {
                val renameOk = tmp.renameTo(srcFile)
                if (!renameOk) {
                    tmp.delete()
                    return false
                }
                return true
            } else {
                tmp.delete()
                return false
            }
        }
        tmp.delete()
        return false
    }

    fun isPdfEncrypted(input: InputStream): Boolean {
        return try {
            val reader = PdfReader(input)
            val pdfDoc = PdfDocument(reader)
            val encrypted = pdfDoc.reader.isEncrypted
            pdfDoc.close()
            encrypted
        } catch (e: Exception) {
            true // 打开失败也认为加密
        }
    }

    /**
     * 多张图片合成PDF
     * @param dest 输出PDF路径
     * @param imagePaths 图片文件路径列表
     */
    fun createPdfFromImages(dest: String, imagePaths: List<String>): Boolean {
        return try {
            val writer = PdfWriter(dest)
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)
            for (path in imagePaths) {
                val imageData = ImageDataFactory.create(path)
                val image = Image(imageData)
                // 适配页面大小
                val pageSize = PageSize(image.imageWidth, image.imageHeight)
                pdfDoc.addNewPage(pageSize)
                image.setFixedPosition(pdfDoc.numberOfPages, 0f, 0f)
                document.add(image)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 将PDF文件从cache目录移动到filesDir下，返回新路径
     */
    fun movePdfToFilesDir(context: Context, cachePdfPath: String): String? {
        val cacheFile = File(cachePdfPath)
        if (!cacheFile.exists()) return null
        val destFile = File(context.filesDir, cacheFile.name)
        return try {
            cacheFile.copyTo(destFile, overwrite = true)
            cacheFile.delete()
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取PDF文件的页面数量
     */
    fun getPdfPageCount(filePath: String): Int {
        return try {
            val reader = PdfReader(filePath)
            val pdfDoc = PdfDocument(reader)
            val pageCount = pdfDoc.numberOfPages
            pdfDoc.close()
            pageCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 生成PDF页面缩略图
     * @param filePath PDF文件路径
     * @param pageIndex 页面索引（从0开始）
     * @param width 缩略图宽度
     * @param height 缩略图高度
     * @return 页面缩略图Bitmap
     */
    fun generatePageThumbnail(filePath: String, pageIndex: Int, width: Int, height: Int): Bitmap? {
        return try {
            val file = File(filePath)
            val parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            
            if (pageIndex >= 0 && pageIndex < pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                // 填充白色背景
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                // 渲染PDF页面到Bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                page.close()
                pdfRenderer.close()
                parcelFileDescriptor.close()
                
                bitmap
            } else {
                parcelFileDescriptor.close()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 生成错误占位图
            createErrorThumbnail(width, height)
        }
    }

    /**
     * 创建错误占位缩略图
     */
    private fun createErrorThumbnail(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.GRAY
            textAlign = Paint.Align.CENTER
            textSize = 24f
        }
        
        canvas.drawColor(Color.LTGRAY)
        canvas.drawText("PDF", width / 2f, height / 2f, paint)
        
        return bitmap
    }

    /**
     * 合并PDF页面
     * @param selectedPages 选中的页面列表
     * @return 是否成功
     */
    fun mergePdfPages(outputFileName: String?, selectedPages: List<BusinessPdfPageInfo>): BusinessFileInfo? {
        return try {
            if (selectedPages.isEmpty()) {
                null
            }
            
            // 按文件路径分组
            val pagesByFile = selectedPages.groupBy { it.filePath }
            
            // 生成输出文件名
            val finalOutputFileName = if (outputFileName == null) {
                "Merged_PDF_${System.currentTimeMillis()}.pdf"
            } else {
                outputFileName
            }
            val outputPath = "/storage/emulated/0/Download/$finalOutputFileName"
            
            val writer = PdfWriter(outputPath)
            val outputDoc = PdfDocument(writer)
            
            pagesByFile.forEach { (filePath, pages) ->
                val reader = PdfReader(filePath)
                val inputDoc = PdfDocument(reader)
                
                // 收集要复制的页面索引（转为1-based）
                val pageIndices = pages.sortedBy { it.pageIndex }
                    .map { it.pageIndex + 1 } // iText uses 1-based indexing
                    .filter { it <= inputDoc.numberOfPages }
                
                if (pageIndices.isNotEmpty()) {
                    // 使用copyPagesTo方法正确复制页面
                    inputDoc.copyPagesTo(pageIndices, outputDoc)
                }
                
                inputDoc.close()
            }
            
            outputDoc.close()
            fileInfoFromPath(outputPath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 拆分PDF页面
     * @param selectedPages 选中的页面列表
     * @return 是否成功
     */
    fun splitPdfPages(selectedPages: List<BusinessPdfPageInfo>): Boolean {
        return try {
            if (selectedPages.isEmpty()) {
                return false
            }
            
            // 按文件路径分组
            val pagesByFile = selectedPages.groupBy { it.filePath }
            
            pagesByFile.forEach { (filePath, pages) ->
                val reader = PdfReader(filePath)
                val inputDoc = PdfDocument(reader)
                
                pages.forEach { pageInfo ->
                    val outputFileName = "${pageInfo.fileName.replace(".pdf", "")}_Page_${pageInfo.pageNumber}_${System.currentTimeMillis()}.pdf"
                    val outputPath = "/storage/emulated/0/Download/$outputFileName"
                    
                    val writer = PdfWriter(outputPath)
                    val outputDoc = PdfDocument(writer)
                    
                    val pageIndex = pageInfo.pageIndex + 1 // iText uses 1-based indexing
                    if (pageIndex <= inputDoc.numberOfPages) {
                        // 使用copyPagesTo方法正确复制单个页面
                        inputDoc.copyPagesTo(listOf(pageIndex), outputDoc)
                    }
                    
                    outputDoc.close()
                }
                
                inputDoc.close()
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
} 