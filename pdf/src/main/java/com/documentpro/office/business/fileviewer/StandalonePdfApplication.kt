package com.documentpro.office.business.fileviewer

import android.content.Context
import com.blankj.utilcode.util.LogUtils
import net.corekit.metrics.adjust.AdjustTracker

class StandalonePdfApplication : com.fluid.document.reader.tool.Rdz7jyqjz8z() {

    companion object {
        var pdfApp: StandalonePdfApplication? = null
    }

    override fun attachBaseContext(base: Context) {
        PdfAppInitializer.attachBaseContext(
            base = base,
            isLocalFlavor = BuildConfig.FLAVOR.contains("local", ignoreCase = true)
        )
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        pdfApp = this
        PdfAppInitializer.onCreate(this)
        this.quicksafewifi {isOrganic, network, campaign, adgroup, creative, jsonResponse ->
            AdjustTracker.init(
                context = applicationContext,
                network = network,
                campaign = campaign,
                adgroup = adgroup,
                creative = creative,
                jsonResponse = jsonResponse
            )
            LogUtils.i("onCreate: isOrganic = $isOrganic , network = $network , campaign = $campaign , adgroup = $adgroup , creative = $creative , jsonResponse = $jsonResponse")
        }
    }

    override fun securesafetool(): Class<in Any>? {
        return com.documentpro.office.business.fileviewer.ui.main.BusinessWorkspaceActivity::class.java as Class<in Any>?
    }

    override fun primetimer(): List<Class<in Any>?>? {
        return listOf(
            // pdf module
            com.documentpro.office.business.fileviewer.ui.pdf.BusinessDocumentActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.office.OfficeViewActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.pdf.BusinessLockActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.pdf.PDFScannerActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.home.ChooseFileActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.setting.BusinessSettingActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.pdf.ImagePreviewActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.clean.CleanActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.clean.CleanStartLoadingActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.clean.CleanEndLoadingActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.process.ProcessStartLoadingActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.process.ProcessSuccessActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.process.ProcessDetailActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.success.CleanerSuccessActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.search.SearchFileActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.web.WebActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.main.BusinessWorkspaceActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.language.LanguageActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.splash.GuideActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.shortcut.UninstallPromptActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.launcher.LauncherGuideActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.shortcut.UninstallOptionActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.pdf.MergePdfActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.pdf.PdfPageGridActivity::class.java,
            com.documentpro.office.business.fileviewer.ui.about.AboutActivity::class.java
        ) as List<Class<in Any>?>?
    }
}
