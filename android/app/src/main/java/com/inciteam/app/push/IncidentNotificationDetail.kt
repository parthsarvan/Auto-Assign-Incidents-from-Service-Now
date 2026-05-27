package com.inciteam.app.push

import android.content.Intent

data class IncidentNotificationDetail(
    val incidentNumber: String,
    val title: String,
    val priority: String,
    val configurationItem: String
) {
    companion object {
        fun fromIntent(intent: Intent?): IncidentNotificationDetail? {
            val extras = intent?.extras ?: return null
            val incidentNumber = extras.getString("incidentNumber")?.trim().orEmpty()
            if (incidentNumber.isBlank()) {
                return null
            }

            return IncidentNotificationDetail(
                incidentNumber = incidentNumber,
                title = extras.getString("title")?.trim().orEmpty().ifBlank { "Incident assigned to you" },
                priority = extras.getString("priority")?.trim().orEmpty().ifBlank { "Not provided" },
                configurationItem = listOfNotNull(
                    extras.getString("ci"),
                    extras.getString("configurationItem")
                )
                    .firstOrNull { it.isNotBlank() }
                    ?.trim()
                    .orEmpty()
                    .ifBlank { "Not provided" }
            )
        }
    }
}
