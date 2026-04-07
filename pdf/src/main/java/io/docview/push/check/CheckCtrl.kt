package io.docview.push.check

object CheckCtrl {
    enum class NotificationType(val string: String) {
        UNLOCK("unlock"),
        BACKGROUND("background"),
        KEEPALIVE("keepalive"),
        FCM("fcm"),
        RESIDENT("resident"),
    }
}
