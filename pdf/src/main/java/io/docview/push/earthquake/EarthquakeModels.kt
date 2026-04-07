package io.docview.push.earthquake

data class EarthquakeInfo(
    val alert: String? = null,
    val magnitude: Double = 0.0,
    val place: String? = null,
    val depth: Double = 0.0,
    val hasTsunami: Boolean = false,
    val time: String = "",
    val magType: String? = null,
    val status: String? = null,
)

object EarthquakeController {
    @JvmStatic
    fun start() = Unit
}
