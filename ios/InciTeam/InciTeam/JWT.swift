import Foundation

enum JWT {
    static func expirationDate(from token: String) -> Date? {
        let parts = token.split(separator: ".")
        guard parts.count >= 2 else {
            return nil
        }

        guard let payloadData = base64URLDecode(String(parts[1])),
              let payload = try? JSONSerialization.jsonObject(with: payloadData) as? [String: Any],
              let expiration = payload["exp"] as? TimeInterval else {
            return nil
        }

        return Date(timeIntervalSince1970: expiration)
    }

    private static func base64URLDecode(_ value: String) -> Data? {
        var base64 = value
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")

        while base64.count % 4 != 0 {
            base64.append("=")
        }

        return Data(base64Encoded: base64)
    }
}
