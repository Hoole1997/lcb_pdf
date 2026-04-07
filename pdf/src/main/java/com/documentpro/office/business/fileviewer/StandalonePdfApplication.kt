package com.documentpro.office.business.fileviewer

import android.app.Application
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.ActivityUtils
import com.documentpro.office.business.fileviewer.ui.main.BusinessWorkspaceActivity

class StandalonePdfApplication : Application() {

    override fun attachBaseContext(base: Context) {
        PdfAppInitializer.attachBaseContext(
            base = base,
            isLocalFlavor = BuildConfig.FLAVOR.contains("local", ignoreCase = true)
        )
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        PdfAppInitializer.onCreate(this)
    }
}
