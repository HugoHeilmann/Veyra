import SwiftUI

struct MainTabView: View {
    @State private var selectedTab = 0
    @StateObject private var playerManager = PlayerManager.shared
    
    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                NavigationStack {
                    MusicListView().navigationBarHidden(true)
                }
                .tag(0)
                
                NavigationStack {
                    PlaylistsView().navigationBarHidden(true)
                }
                .tag(1)
                
                NavigationStack {
                    QueueView().navigationBarHidden(true)
                }
                .tag(2)
                
                NavigationStack {
                    DownloadView().navigationBarHidden(true)
                }
                .tag(3)
            }
            .onAppear {
                UITabBar.appearance().isHidden = true
            }
            
            CustomNavBar(selectedTab: $selectedTab)
                .ignoresSafeArea(edges: .bottom)
        }
        .background(Color(VeyraTheme.Colors.veyraDarkGray).ignoresSafeArea())
        .environmentObject(playerManager)
    }
}

struct SettingsView: View {
    var body: some View {
        Text("Parametres de Veyra")
    }
}
