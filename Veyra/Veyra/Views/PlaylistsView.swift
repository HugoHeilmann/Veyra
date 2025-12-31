import SwiftUI

struct PlaylistsView: View {
    // Mocks
    private let playlists: [Playlist] = [
        Playlist(name: "Favoris", musics: []),
        Playlist(name: "Workout", musics: [Music(title: "test")])
    ]
    
    var body: some View {
        ZStack {
            Color(VeyraTheme.Colors.veyraDarkGray).ignoresSafeArea()
            
            ZStack {
                VStack(spacing: 16) {
                    Header(text: "Playlists")
                    
                    Text("Organise tes musiques en playlists")
                        .foregroundColor(Color(VeyraTheme.Colors.veyraLightGray))
                        .font(.system(size: 14))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                    
                    // Liste
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(playlists, id: \.name) { playlist in
                                PlaylistRowItem(playlist: playlist)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 4)
                    }
                    
                    Spacer()
                }
                .frame(maxHeight: .infinity, alignment: .top)
                
                // Bouton flottant
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: {
                            //TODO create new playlist
                        }) {
                            Image(systemName: "plus")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.black)
                                .padding(18)
                        }
                        .background(Color(VeyraTheme.Colors.veyraGreen))
                        .cornerRadius(12)
                        .shadow(radius: 6)
                        .padding(.trailing, 24)
                        .padding(.bottom, 64)
                    }
                }
            }
        }
    }
}
