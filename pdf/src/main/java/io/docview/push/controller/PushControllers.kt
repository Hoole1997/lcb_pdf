package io.docview.push.controller

import android.content.Intent
import io.docview.push.builder.LANDING_NOTIFICATION_ACTION
import io.docview.push.builder.LANDING_NOTIFICATION_CONTENT
import io.docview.push.builder.LANDING_NOTIFICATION_FROM
import io.docview.push.builder.LANDING_NOTIFICATION_TITLE

object BgNotiInterceptController {
    @JvmStatic
    fun markNextIntercept() = Unit

    @JvmStatic
    fun clearIntercept() = Unit
}

object LandingCtrl {
    @JvmStatic
    fun isFromNotification(intent: Intent?): Boolean {
        return intent?.hasExtra(LANDING_NOTIFICATION_FROM) == true ||
            intent?.hasExtra(LANDING_NOTIFICATION_ACTION) == true
    }

    @JvmStatic
    fun clearNotificationParameters(intent: Intent?) {
        intent?.removeExtra(LANDING_NOTIFICATION_ACTION)
        intent?.removeExtra(LANDING_NOTIFICATION_CONTENT)
        intent?.removeExtra(LANDING_NOTIFICATION_FROM)
        intent?.removeExtra(LANDING_NOTIFICATION_TITLE)
    }
}

class NotificationOverlayController(
    private val activity: android.app.Activity,
    private val onClick: (OverlayContent) -> Unit
) {
    data class OverlayContent(val actionType: Int = 0)

    fun show() = Unit

    fun destroy() = Unit

    companion object {
        @JvmStatic
        fun setShowCallback(callback: (() -> Unit)?) = Unit
    }
}

object TriggerCtrl {
    @JvmStatic
    fun stopRepeatNotification() = Unit
}
