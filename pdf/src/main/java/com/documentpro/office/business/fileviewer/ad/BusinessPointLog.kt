package com.documentpro.office.business.fileviewer.ad

import net.corekit.core.report.ReportDataManager

object BusinessPointLog {

    private const val TAG = "BusinessPointLog"

    fun logEvent(event: String, values: Map<String?, Any?>? = null) {
        val filteredValues = mutableMapOf<String, Any>()
        values?.forEach { (key, value) ->
            if (key != null && value != null) {
                filteredValues[key] = value
            }
        }
        ReportDataManager.reportData(event, filteredValues)
    }

    fun pushRequest(position: String) {
        logEvent(
            "Notific_Allow_Start", mapOf(
                "Notific_Allow_Position" to position,
            )
        )
    }

    fun pushResult(result: String, position: String) {
        logEvent(
            "Notific_Allow_Result", mapOf(
                "Result" to result,
                "Notific_Allow_Position" to position,
            )
        )
    }

}