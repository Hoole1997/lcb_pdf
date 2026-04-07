package io.docview.push.check

object CheckCtrl {
    @JvmStatic
    var debugZeroInterval: Boolean = false

    enum class NotificationType(val string: String) {
        UNLOCK("unlock"),
        BACKGROUND("background"),
        KEEPALIVE("keepalive"),
        FCM("fcm"),
        RESIDENT("resident"),
        EARTHQUAKE("earthquake"),
    }
}
