package com.inciteam.app.ui

import androidx.compose.ui.graphics.Color

enum class FeatureTone(val color: Color) {
    Blue(Color(0xFF1859D1)),
    Green(Color(0xFF1F9D68)),
    Orange(Color(0xFFD48806)),
    Purple(Color(0xFF6750A4)),
    Red(Color(0xFFC74444)),
    Slate(Color(0xFF475569))
}

data class InciTeamFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val tone: FeatureTone
)

data class InciTeamFeatureSection(
    val title: String,
    val badge: String,
    val features: List<InciTeamFeature>
)

val InciTeamFeatureSections = listOf(
    InciTeamFeatureSection(
        title = "Roster and Schedule",
        badge = "RS",
        features = listOf(
            InciTeamFeature(
                id = "roster",
                title = "Roster",
                subtitle = "On-shift people",
                badge = "RO",
                tone = FeatureTone.Blue
            ),
            InciTeamFeature(
                id = "schedule",
                title = "Schedule",
                subtitle = "Coverage calendar",
                badge = "SC",
                tone = FeatureTone.Blue
            )
        )
    ),
    InciTeamFeatureSection(
        title = "People",
        badge = "PE",
        features = listOf(
            InciTeamFeature(
                id = "team-members",
                title = "Team Members",
                subtitle = "Team directory",
                badge = "TM",
                tone = FeatureTone.Slate
            )
        )
    ),
    InciTeamFeatureSection(
        title = "Availability",
        badge = "AV",
        features = listOf(
            InciTeamFeature(
                id = "leaves",
                title = "Leaves",
                subtitle = "Planned absences",
                badge = "LV",
                tone = FeatureTone.Orange
            ),
            InciTeamFeature(
                id = "breaks",
                title = "Breaks",
                subtitle = "Active breaks",
                badge = "BR",
                tone = FeatureTone.Green
            )
        )
    ),
    InciTeamFeatureSection(
        title = "CI Routing",
        badge = "CI",
        features = listOf(
            InciTeamFeature(
                id = "configuration-items",
                title = "CI",
                subtitle = "Supported systems",
                badge = "CI",
                tone = FeatureTone.Slate
            ),
            InciTeamFeature(
                id = "ci-user-mapping",
                title = "CI User Mapping",
                subtitle = "Routing ownership",
                badge = "CU",
                tone = FeatureTone.Slate
            )
        )
    ),
    InciTeamFeatureSection(
        title = "Operations",
        badge = "OP",
        features = listOf(
            InciTeamFeature(
                id = "summary",
                title = "Summary",
                subtitle = "Operational health",
                badge = "SU",
                tone = FeatureTone.Blue
            ),
            InciTeamFeature(
                id = "logs",
                title = "Logs",
                subtitle = "Assignment timeline",
                badge = "LG",
                tone = FeatureTone.Blue
            ),
            InciTeamFeature(
                id = "diagnostics",
                title = "Diagnostics",
                subtitle = "Routing checks",
                badge = "DX",
                tone = FeatureTone.Purple
            )
        )
    ),
    InciTeamFeatureSection(
        title = "Access",
        badge = "AC",
        features = listOf(
            InciTeamFeature(
                id = "account",
                title = "Account",
                subtitle = "Profile and deletion",
                badge = "ME",
                tone = FeatureTone.Red
            ),
            InciTeamFeature(
                id = "user-access",
                title = "User Access",
                subtitle = "Roles and teams",
                badge = "UA",
                tone = FeatureTone.Red
            )
        )
    )
)
