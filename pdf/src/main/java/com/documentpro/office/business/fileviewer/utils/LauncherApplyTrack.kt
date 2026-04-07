package com.documentpro.office.business.fileviewer.utils

import android.content.Context
import com.documentpro.office.business.fileviewer.dialog.DefaultAppDialog
import net.corekit.core.ext.DataStoreBoolDelegate
import net.corekit.core.ext.DataStoreStringDelegate
import net.corekit.core.ext.isDefaultLauncher
import net.corekit.core.report.ReportDataManager


object LauncherApplyTrack {

    var current by DataStoreStringDelegate("current_app_mode", "Settings")
    var hasSet by DataStoreBoolDelegate("launcher_has_set",false)

    fun First_Launcher(){
        current = "First_Launcher"
    }

    fun Second_Launcher(){
        current = "Second_Launcher"
    }

    fun Home_Launcher(){
        current = "Home_Launcher"
    }

    fun launcherMainAcTrack(context: Context){
        if(hasSet) return
        if(!context.isDefaultLauncher()) return
        hasSet = true
        ReportDataManager.reportData("Set_As_Default", mapOf("position" to current!!))
    }

    fun appMainAcTrack(context: Context){
        if(hasSet && !context.isDefaultLauncher()){
            ReportDataManager.reportData("Cancel_As_Default", mapOf())
            hasSet = false
            current = "Settings"
        }
    }
}
