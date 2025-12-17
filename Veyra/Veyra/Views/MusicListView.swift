import SwiftUI

struct MusicListView: View {
    @StateObject private var viewModel = MusicListViewModel()
    @EnvironmentObject private var playerManager: PlayerManager
    
    var body: some View {
        ZStack(alignment: .bottom) {
            Color(VeyraTheme.Colors.veyraDarkGray).ignoresSafeArea()
            
            VStack(spacing: 0) {
                Header(text: "Veyra")
                
                if viewModel.isLoading {
                    ProgressView("Chargement").frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = viewModel.errorMessage {
                    VStack(spacing: 12) {
                        Text(error).foregroundColor(.red)
                        Button("Retry") {
                            viewModel.loadMusics()
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.musics.isEmpty {
                    Text("Pas de musiques").foregroundColor(.secondary).frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    VStack(spacing: 12) {
                        // Search field
                        TextField(
                            "",
                            text: $viewModel.searchText,
                            prompt: Text("Rechercher...")
                                .foregroundColor(Color(VeyraTheme.Colors.veyraLightGray))
                        )
                            .padding(.horizontal, 12)
                            .padding(.vertical, 10)
                            .background(Color(VeyraTheme.Colors.veyraMediumGray))
                            .foregroundColor(.white)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color(VeyraTheme.Colors.veyraLightGray), lineWidth: 0)
                            )
                            .cornerRadius(12)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                        
                        // Tabs
                        CustomTabBar(selectedTab: $viewModel.selectedTab)
                        
                        MusicListContent(
                            selectedTab: viewModel.selectedTab,
                            musics: viewModel.musics
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
            }
            if let current = playerManager.currentMusic {
                PlayerBarView(music: current)
            }
        }
        .onAppear {
            viewModel.loadMusics()
        }
    }
}
