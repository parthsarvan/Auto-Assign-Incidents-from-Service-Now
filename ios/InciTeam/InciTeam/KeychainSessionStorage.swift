import Foundation
import Security

protocol SessionStorage {
    func loadSession() throws -> StoredSession?
    func saveSession(_ session: StoredSession) throws
    func clearSession() throws
}

struct KeychainSessionStorage: SessionStorage {
    private let service = "com.inciteam.mobile"
    private let account = "session"

    func loadSession() throws -> StoredSession? {
        var query = baseQuery()
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        if status == errSecItemNotFound {
            return nil
        }
        guard status == errSecSuccess, let data = result as? Data else {
            throw KeychainSessionStorageError.unhandledStatus(status)
        }

        return try JSONDecoder().decode(StoredSession.self, from: data)
    }

    func saveSession(_ session: StoredSession) throws {
        let data = try JSONEncoder().encode(session)
        try clearSession()

        var query = baseQuery()
        query[kSecValueData as String] = data
        query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainSessionStorageError.unhandledStatus(status)
        }
    }

    func clearSession() throws {
        let status = SecItemDelete(baseQuery() as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainSessionStorageError.unhandledStatus(status)
        }
    }

    private func baseQuery() -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }
}

struct PreviewSessionStorage: SessionStorage {
    func loadSession() throws -> StoredSession? {
        nil
    }

    func saveSession(_ session: StoredSession) throws {}

    func clearSession() throws {}
}

enum KeychainSessionStorageError: LocalizedError {
    case unhandledStatus(OSStatus)

    var errorDescription: String? {
        switch self {
        case let .unhandledStatus(status):
            return "Keychain returned status \(status)."
        }
    }
}
