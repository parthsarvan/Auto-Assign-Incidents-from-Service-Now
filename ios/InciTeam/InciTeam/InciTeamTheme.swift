import SwiftUI
import UIKit

enum InciTeamTheme {
    static let primary = Color(red: 24 / 255, green: 89 / 255, blue: 209 / 255)
    static let primaryBright = Color(red: 55 / 255, green: 116 / 255, blue: 226 / 255)
    static let primaryDeep = adaptiveColor(
        light: UIColor(red: 17 / 255, green: 58 / 255, blue: 122 / 255, alpha: 1),
        dark: UIColor(red: 184 / 255, green: 211 / 255, blue: 1, alpha: 1)
    )
    static let ink = adaptiveColor(
        light: UIColor(red: 16 / 255, green: 35 / 255, blue: 61 / 255, alpha: 1),
        dark: UIColor(red: 235 / 255, green: 242 / 255, blue: 1, alpha: 1)
    )
    static let muted = adaptiveColor(
        light: UIColor(red: 95 / 255, green: 113 / 255, blue: 135 / 255, alpha: 1),
        dark: UIColor(red: 157 / 255, green: 174 / 255, blue: 199 / 255, alpha: 1)
    )
    static let border = adaptiveColor(
        light: UIColor(red: 216 / 255, green: 225 / 255, blue: 236 / 255, alpha: 1),
        dark: UIColor(red: 52 / 255, green: 72 / 255, blue: 104 / 255, alpha: 1)
    )
    static let card = adaptiveColor(
        light: UIColor.white.withAlphaComponent(0.94),
        dark: UIColor(red: 18 / 255, green: 31 / 255, blue: 56 / 255, alpha: 0.96)
    )
    static let row = adaptiveColor(
        light: UIColor(red: 248 / 255, green: 251 / 255, blue: 1, alpha: 1),
        dark: UIColor(red: 24 / 255, green: 40 / 255, blue: 71 / 255, alpha: 1)
    )

    static let background = LinearGradient(
        colors: [
            adaptiveColor(
                light: UIColor(red: 248 / 255, green: 251 / 255, blue: 1, alpha: 1),
                dark: UIColor(red: 8 / 255, green: 17 / 255, blue: 32 / 255, alpha: 1)
            ),
            adaptiveColor(
                light: UIColor(red: 239 / 255, green: 246 / 255, blue: 1, alpha: 1),
                dark: UIColor(red: 12 / 255, green: 25 / 255, blue: 48 / 255, alpha: 1)
            ),
            adaptiveColor(
                light: UIColor(red: 244 / 255, green: 247 / 255, blue: 251 / 255, alpha: 1),
                dark: UIColor(red: 9 / 255, green: 19 / 255, blue: 36 / 255, alpha: 1)
            )
        ],
        startPoint: .top,
        endPoint: .bottom
    )

    static let headerGradient = LinearGradient(
        colors: [
            primaryBright,
            primary,
            primaryDeep
        ],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    private static func adaptiveColor(light: UIColor, dark: UIColor) -> Color {
        Color(UIColor { traits in
            traits.userInterfaceStyle == .dark ? dark : light
        })
    }
}

enum FeatureTone: String {
    case blue
    case green
    case orange
    case purple
    case red
    case slate

    var color: Color {
        switch self {
        case .blue:
            return InciTeamTheme.primary
        case .green:
            return Color(red: 31 / 255, green: 157 / 255, blue: 104 / 255)
        case .orange:
            return Color(red: 212 / 255, green: 136 / 255, blue: 6 / 255)
        case .purple:
            return Color(red: 103 / 255, green: 80 / 255, blue: 164 / 255)
        case .red:
            return Color(red: 199 / 255, green: 68 / 255, blue: 68 / 255)
        case .slate:
            return Color(red: 71 / 255, green: 85 / 255, blue: 105 / 255)
        }
    }
}
