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

        PdfAppInitializer.setLauncherExistsCallback {
            ActivityUtils.isActivityExistsInStack(BusinessWorkspaceActivity::class.java)
        }
        PdfAppInitializer.setLauncherRestartCallback { context ->
            context.startActivity(
                Intent(context, BusinessWorkspaceActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
        }
        PdfAppInitializer.onCreate(this)
    }
}
