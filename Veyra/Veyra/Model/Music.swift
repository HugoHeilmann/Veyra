import Foundation

struct Music: Identifiable, Hashable, Codable {
    let id: UUID
    let title: String
    let artist: String?
    let album: String?
    let coverPath: String?
    let duration: TimeInterval?
    let fileURL: URL?
    
    init(
        title: String,
        artist: String = "Unknown artist",
        album: String = "Unknown album",
        coverPath: String = "",
        duration: TimeInterval? = nil,
        fileURL: URL? = nil
    ) {
        self.id = UUID()
        self.title = title
        self.artist = artist
        self.album = album
        self.coverPath = coverPath
        self.duration = duration
        self.fileURL = fileURL
    }
}
