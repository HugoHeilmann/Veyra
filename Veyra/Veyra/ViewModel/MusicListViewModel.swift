import Foundation

@MainActor
final class MusicListViewModel: ObservableObject {
    @Published var musics: [Music] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var searchText: String = ""
    @Published var selectedTab: Int = 0
    
    private let libraryService: MusicLibraryService
    
    init(libraryService: MusicLibraryService = .shared) {
        self.libraryService = libraryService
    }
    
    func tabTitle(for index: Int) -> String {
        switch index {
            case 0: return "Chansons"
            case 1: return "Artistes"
            case 2: return "Albums"
            default: return ""
        }
    }
    
    func loadMusics() {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil
        
        Task {
            let items = await libraryService.loadAllMusic()
            self.musics = items
            self.isLoading = false
        }
    }
}
