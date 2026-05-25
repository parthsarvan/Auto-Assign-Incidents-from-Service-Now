import SwiftUI

enum InciTeamFeature: String, CaseIterable, Identifiable, Hashable {
    case roster
    case schedule
    case teamMembers
    case leaves
    case breaks
    case configurationItems
    case ciUserMapping
    case summary
    case logs
    case diagnostics
    case accountSettings
    case userAccess

    var id: String {
        rawValue
    }

    var title: String {
        switch self {
        case .roster:
            return "Roster"
        case .schedule:
            return "Schedule"
        case .teamMembers:
            return "Team Members"
        case .leaves:
            return "Leaves"
        case .breaks:
            return "Breaks"
        case .configurationItems:
            return "CI"
        case .ciUserMapping:
            return "CI User Mapping"
        case .summary:
            return "Summary"
        case .logs:
            return "Logs"
        case .diagnostics:
            return "Diagnostics"
        case .accountSettings:
            return "Account"
        case .userAccess:
            return "User Access"
        }
    }

    var subtitle: String {
        switch self {
        case .roster:
            return "On-shift people"
        case .schedule:
            return "Coverage calendar"
        case .teamMembers:
            return "Team directory"
        case .leaves:
            return "Planned absences"
        case .breaks:
            return "Active breaks"
        case .configurationItems:
            return "Supported systems"
        case .ciUserMapping:
            return "Routing ownership"
        case .summary:
            return "Operational health"
        case .logs:
            return "Assignment timeline"
        case .diagnostics:
            return "Routing checks"
        case .accountSettings:
            return "Profile and deletion"
        case .userAccess:
            return "Roles and teams"
        }
    }

    var systemImage: String {
        switch self {
        case .roster:
            return "person.3.sequence.fill"
        case .schedule:
            return "calendar"
        case .teamMembers:
            return "person.2.fill"
        case .leaves:
            return "figure.walk.departure"
        case .breaks:
            return "cup.and.saucer.fill"
        case .configurationItems:
            return "server.rack"
        case .ciUserMapping:
            return "point.3.connected.trianglepath.dotted"
        case .summary:
            return "gauge.with.dots.needle.bottom.50percent"
        case .logs:
            return "list.bullet.rectangle.portrait.fill"
        case .diagnostics:
            return "stethoscope"
        case .accountSettings:
            return "person.crop.circle.badge.exclamationmark"
        case .userAccess:
            return "person.badge.key.fill"
        }
    }

    var tone: FeatureTone {
        switch self {
        case .roster, .schedule, .summary, .logs:
            return .blue
        case .teamMembers, .configurationItems, .ciUserMapping:
            return .slate
        case .leaves:
            return .orange
        case .breaks:
            return .green
        case .diagnostics:
            return .purple
        case .accountSettings, .userAccess:
            return .red
        }
    }
}

struct InciTeamFeatureSection: Identifiable {
    let title: String
    let systemImage: String
    let features: [InciTeamFeature]

    var id: String {
        title
    }

    static let all: [InciTeamFeatureSection] = [
        InciTeamFeatureSection(
            title: "Roster and Schedule",
            systemImage: "calendar.badge.clock",
            features: [.roster, .schedule]
        ),
        InciTeamFeatureSection(
            title: "People",
            systemImage: "person.2.fill",
            features: [.teamMembers]
        ),
        InciTeamFeatureSection(
            title: "Availability",
            systemImage: "person.crop.circle.badge.clock",
            features: [.leaves, .breaks]
        ),
        InciTeamFeatureSection(
            title: "CI Routing",
            systemImage: "arrow.triangle.branch",
            features: [.configurationItems, .ciUserMapping]
        ),
        InciTeamFeatureSection(
            title: "Operations",
            systemImage: "waveform.path.ecg.rectangle",
            features: [.summary, .logs, .diagnostics]
        ),
        InciTeamFeatureSection(
            title: "Access",
            systemImage: "lock.shield.fill",
            features: [.accountSettings, .userAccess]
        )
    ]
}
