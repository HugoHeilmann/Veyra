import Foundation

final class MusicLibraryService {
    static let shared = MusicLibraryService()
    
    private init() {}
    
    func loadAllMusic() async -> [Music] {
        
        try? await Task.sleep(nanoseconds: 300_000_000)
        
        return [
            // A–Z classiques
            Music(title: "Alpha", artist: "Artist 1", album: "Album 1"),
            Music(title: "Bravo", artist: "Artist 2", album: "Album 2"),
            Music(title: "Charlie", artist: "Artist 3", album: "Album 3"),
            Music(title: "Delta", artist: "Artist 4", album: "Album 4"),
            Music(title: "Echo", artist: "Artist 5", album: "Album 5"),
            Music(title: "Foxtrot", artist: "Artist 6", album: "Album 6"),
            Music(title: "Golf", artist: "Artist 7", album: "Album 7"),
            Music(title: "Hotel", artist: "Artist 8", album: "Album 8"),
            Music(title: "India", artist: "Artist 9", album: "Album 9"),
            Music(title: "Juliet", artist: "Artist 10", album: "Album 10"),
            Music(title: "Kilo", artist: "Artist 11", album: "Album 11"),
            Music(title: "Lima", artist: "Artist 12", album: "Album 12"),
            Music(title: "Mike", artist: "Artist 13", album: "Album 13"),
            Music(title: "November", artist: "Artist 14", album: "Album 14"),
            Music(title: "Oscar", artist: "Artist 15", album: "Album 15"),
            Music(title: "Papa", artist: "Artist 16", album: "Album 16"),
            Music(title: "Quebec", artist: "Artist 17", album: "Album 17"),
            Music(title: "Romeo", artist: "Artist 18", album: "Album 18"),
            Music(title: "Sierra", artist: "Artist 19", album: "Album 19"),
            Music(title: "Tango", artist: "Artist 20", album: "Album 20"),
            Music(title: "Uniform", artist: "Artist 21", album: "Album 21"),
            Music(title: "Victor", artist: "Artist 22", album: "Album 22"),
            Music(title: "Whiskey", artist: "Artist 23", album: "Album 23"),
            Music(title: "X-Ray", artist: "Artist 24", album: "Album 24"),
            Music(title: "Yankee", artist: "Artist 25", album: "Album 25"),
            Music(title: "Zulu", artist: "Artist 26", album: "Album 26"),

            // Accents (doivent aller dans # avec ton implémentation actuelle)
            Music(title: "Été", artist: "French Artist", album: "Saisons"),
            Music(title: "À demain", artist: "French Artist", album: "Paroles"),
            Music(title: "Ça ira", artist: "French Artist", album: "Révolution"),
            Music(title: "Österreich", artist: "German Artist", album: "Europa"),
            Music(title: "Über allen Gipfeln", artist: "German Artist", album: "Natur"),

            // Chiffres / symboles (=> #)
            Music(title: "10 Things I Hate About You", artist: "Kids Band", album: "Jeux"),
            Music(title: "!Exclamation", artist: "Punk Band", album: "Loud"),
            Music(title: "#Hashtag", artist: "Social Artist", album: "Trends"),
            Music(title: "@Home", artist: "Indie Artist", album: "Stay")
        ]
    }
}
