import SwiftUI

struct MusicListContent: View {
    let selectedTab: Int
    let musics: [Music]
    
    @ViewBuilder
    var body: some View {
        switch selectedTab {
        case 0: // Chansons
            SongsTabView(musics: musics)
              
        case 1: // Artistes
            VStack(spacing: 16) {
                AddArtistOrAlbum(isArtist: true)
                Text("Artist")
                    .foregroundColor(.white)
                    .font(.system(size: 18, weight: .semibold))
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
                
        case 2: // Albums
            VStack(spacing: 16) {
                AddArtistOrAlbum(isArtist: false)
                Text("Album")
                    .foregroundColor(.white)
                    .font(.system(size: 18, weight: .semibold))
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
                
        default:
            EmptyView()
        }
    }
}
