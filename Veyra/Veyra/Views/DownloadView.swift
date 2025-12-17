import SwiftUI

struct DownloadView: View {
    @State private var youtubeURL: String = ""
    @State private var title: String = ""
    @State private var artist: String = ""
    @State private var album: String = ""
    
    @StateObject private var status = DownloadStatus()
    
    var body: some View {
        ZStack {
            Color(VeyraTheme.Colors.veyraDarkGray).ignoresSafeArea()
            
            VStack(spacing: 16) {
                
                Header(text: "Telechargement")
                
                VStack(spacing: 20) {
                    // input card
                    VStack(spacing: 12) {
                        Text("Informations du morceau")
                            .foregroundColor(.white)
                            .font(.system(size: 18, weight: .bold))
                            .frame(maxWidth: .infinity, alignment: .leading)
                        VeyraTextField(text: $youtubeURL, placeholder: "URL YouTube")
                        VeyraTextField(text: $title, placeholder: "Titre")
                        VeyraTextField(text: $artist, placeholder: "Artiste")
                        VeyraTextField(text: $album, placeholder: "Album")
                        
                        Button(action: {
                            // TODO ouvrir la selection de playlist
                        }) {
                            Text("Ajouter a une ou plusieurs playlists")
                                .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                                .font(.system(size: 14, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                        }
                        .background(Color.clear)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color(VeyraTheme.Colors.veyraLightGray), lineWidth: 1)
                        )
                        .padding(.top, 4)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity)
                    .background(Color(VeyraTheme.Colors.veyraMediumGray))
                    .cornerRadius(16)
                    
                    // downloadButton
                    Button(action: {
                        // TODO logique du telechargement
                    }) {
                        Text("Telecharger en MP3")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .background(Color(VeyraTheme.Colors.veyraGreen))
                    .cornerRadius(14)
                    
                    // statusBar
                    HStack {
                        Text(status.message)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.black)
                            .lineLimit(1)
                            .truncationMode(.tail)
                            .padding(.horizontal, 12)
                    }
                    .frame(height: 36)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(status.backgroundColor))
                    .cornerRadius(10)
                }
            }
            .frame(maxHeight: .infinity, alignment: .top)
        }
    }
}
