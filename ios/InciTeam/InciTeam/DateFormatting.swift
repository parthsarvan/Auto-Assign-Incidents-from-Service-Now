import Foundation

enum DateFormatting {
    static let dayNames: [(value: String, short: String, long: String)] = [
        ("MONDAY", "Mon", "Monday"),
        ("TUESDAY", "Tue", "Tuesday"),
        ("WEDNESDAY", "Wed", "Wednesday"),
        ("THURSDAY", "Thu", "Thursday"),
        ("FRIDAY", "Fri", "Friday"),
        ("SATURDAY", "Sat", "Saturday"),
        ("SUNDAY", "Sun", "Sunday")
    ]

    private static let isoDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    private static let displayDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEE, MMM d"
        return formatter
    }()

    private static let displayDateTimeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEE, MMM d, h:mm a"
        return formatter
    }()

    private static let shortDateTimeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, h:mm a"
        return formatter
    }()

    private static let isoInstantFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    private static let isoInstantFallbackFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    private static func hasExplicitTimeZone(_ value: String) -> Bool {
        if value.hasSuffix("Z") {
            return true
        }
        guard let timeSeparator = value.firstIndex(of: "T") else {
            return false
        }
        let timeComponent = value[value.index(after: timeSeparator)...]
        return timeComponent.contains("+") || timeComponent.contains("-")
    }

    static func isoDate(_ date: Date) -> String {
        isoDateFormatter.string(from: date)
    }

    static func date(from isoDate: String) -> Date? {
        isoDateFormatter.date(from: isoDate)
    }

    static func displayDate(_ isoDate: String) -> String {
        guard let date = date(from: isoDate) else {
            return isoDate
        }
        return displayDateFormatter.string(from: date)
    }

    static func displayDate(_ date: Date) -> String {
        displayDateFormatter.string(from: date)
    }

    static func isoInstant(_ date: Date) -> String {
        isoInstantFallbackFormatter.string(from: date)
    }

    static func instantDate(from value: String?) -> Date? {
        guard let value, !value.isEmpty else {
            return nil
        }
        if let date = isoInstantFormatter.date(from: value) {
            return date
        }
        if let date = isoInstantFallbackFormatter.date(from: value) {
            return date
        }
        if !hasExplicitTimeZone(value), value.contains("T") {
            let utcValue = "\(value)Z"
            if let date = isoInstantFormatter.date(from: utcValue) {
                return date
            }
            if let date = isoInstantFallbackFormatter.date(from: utcValue) {
                return date
            }
        }
        return nil
    }

    static func displayDateTime(_ value: String?) -> String {
        guard let date = instantDate(from: value) else {
            return value ?? "-"
        }
        return displayDateTimeFormatter.string(from: date)
    }

    static func shortDateTime(_ value: String?) -> String {
        guard let date = instantDate(from: value) else {
            return value ?? "-"
        }
        return shortDateTimeFormatter.string(from: date)
    }

    static func duration(start: String?, end: String?) -> String {
        guard let startDate = instantDate(from: start), let endDate = instantDate(from: end) else {
            return "-"
        }

        let minutes = max(Int(endDate.timeIntervalSince(startDate) / 60), 0)
        if minutes < 60 {
            return "\(minutes) min"
        }

        let hours = minutes / 60
        let remainingMinutes = minutes % 60
        if hours < 24 {
            return "\(hours)h \(remainingMinutes)m"
        }

        let days = hours / 24
        let remainingHours = hours % 24
        return "\(days)d \(remainingHours)h"
    }

    static func days(starting startDate: Date, count: Int) -> [Date] {
        (0..<count).compactMap { offset in
            Calendar.current.date(byAdding: .day, value: offset, to: startDate)
        }
    }

    static func dayCount(startDate: String, endDate: String) -> Int? {
        guard let start = date(from: startDate), let end = date(from: endDate) else {
            return nil
        }
        let days = Calendar.current.dateComponents([.day], from: start, to: end).day ?? 0
        return max(days + 1, 1)
    }

    static func formatCoverageDays(_ value: String?) -> String {
        let days = parseCoverageDays(value)
        let normalized = Set(days)
        if normalized == Set(dayNames.map(\.value)) {
            return "Every day"
        }
        if normalized == Set(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]) {
            return "Weekdays"
        }
        if normalized == Set(["SATURDAY", "SUNDAY"]) {
            return "Weekend"
        }
        return days.map { day in
            dayNames.first(where: { $0.value == day })?.short ?? day
        }
        .joined(separator: ", ")
    }

    static func parseCoverageDays(_ value: String?) -> [String] {
        guard let value, !value.isEmpty else {
            return dayNames.map(\.value)
        }
        return value
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() }
            .filter { !$0.isEmpty }
    }

    static func isoDateFromTimestamp(_ value: String) -> String? {
        if let date = ISO8601DateFormatter().date(from: value) {
            return isoDate(date)
        }
        return String(value.prefix(10)).isEmpty ? nil : String(value.prefix(10))
    }
}
