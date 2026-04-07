package io.docview.push.builder

import android.content.Context
import android.content.Intent
import com.documentpro.office.business.fileviewer.ui.main.BusinessWorkspaceActivity

const val LANDING_NOTIFICATION_ACTION = "landing_notification_action"
const val LANDING_NOTIFICATION_CONTENT = "landing_notification_content"
const val LANDING_NOTIFICATION_FROM = "landing_notification_from"
const val LANDING_NOTIFICATION_TITLE = "landing_notification_title"
const val LANDING_NOTIFICATION_EARTHQUAKE_DATA = "landing_notification_earthquake_data"

fun entryPointIntent(context: Context): Intent {
    return Intent(context, BusinessWorkspaceActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
