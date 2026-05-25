import Foundation
import UIKit

struct ProfileImageStorage {
    private let fileManager = FileManager.default

    func loadImageData(userId: Int64) -> Data? {
        try? Data(contentsOf: imageURL(userId: userId))
    }

    func saveImageData(_ data: Data, userId: Int64) throws -> Data {
        let normalizedData = normalizedJPEGData(from: data) ?? data
        let directoryURL = profileImagesDirectoryURL()
        try fileManager.createDirectory(at: directoryURL, withIntermediateDirectories: true)
        try normalizedData.write(to: imageURL(userId: userId), options: .atomic)
        return normalizedData
    }

    func deleteImageData(userId: Int64) {
        try? fileManager.removeItem(at: imageURL(userId: userId))
    }

    private func normalizedJPEGData(from data: Data) -> Data? {
        guard let image = UIImage(data: data) else {
            return nil
        }

        let maxSideLength: CGFloat = 512
        let longestSide = max(image.size.width, image.size.height)
        let scale = longestSide > maxSideLength ? maxSideLength / longestSide : 1
        let targetSize = CGSize(width: image.size.width * scale, height: image.size.height * scale)

        let renderer = UIGraphicsImageRenderer(size: targetSize)
        let renderedImage = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: targetSize))
        }
        return renderedImage.jpegData(compressionQuality: 0.86)
    }

    private func imageURL(userId: Int64) -> URL {
        profileImagesDirectoryURL().appendingPathComponent("profile-\(userId).jpg")
    }

    private func profileImagesDirectoryURL() -> URL {
        fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("ProfileImages", isDirectory: true)
    }
}
