import SwiftUI

struct QueueView: View {
    
    var musics = [
        Music(title: "10 Things I Hate About You", artist: "Leah Kate"),
        Music(title: "1987", artist: "Calogero"),
        Music(title: "7 Rings", artist: "Ariana Grande"),
    ]
    
    var body: some View {
        ZStack {
            Color(VeyraTheme.Colors.veyraDarkGray).ignoresSafeArea()
            
            VStack(spacing: 16) {
                Header(text: "File de lecture")
                
                if !musics.isEmpty {
                    PlayerButton(randomPlay: false)
                    
                    VStack(alignment: .leading, spacing: 12) {
                        
                        Text("À suivre")
                            .foregroundColor(.white)
                            .font(.system(size: 18, weight: .semibold))
                        
                        Rectangle()
                            .fill(Color(VeyraTheme.Colors.veyraLightGray))
                            .frame(height: 1)
                        
                        ScrollView {
                            VStack(spacing: 8) {
                                ForEach(musics) { music in
                                    HStack(spacing: 12) {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(music.title)
                                                .foregroundColor(.white)
                                                .font(.system(size: 16, weight: .semibold))
                                                .lineLimit(1)
                                            
                                            Text(music.artist ?? "Unknown artist")
                                                .foregroundColor(Color(VeyraTheme.Colors.veyraLightGray))
                                                .font(.system(size: 12))
                                                .lineLimit(1)
                                            
                                            Text(music.album ?? "Unknown album")
                                                .foregroundColor(Color(VeyraTheme.Colors.veyraLightGray))
                                                .font(.system(size: 12))
                                                .lineLimit(1)
                                        }
                                        
                                        Spacer()
                                        
                                        Image(systemName: "line.3.horizontal")
                                            .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                                            .font(.system(size: 18))
                                    }
                                    .padding(12)
                                    .background(Color(VeyraTheme.Colors.veyraMediumGray))
                                    .cornerRadius(16)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .top)
                        }
                        .frame(maxHeight: .infinity)
                    }
                    .padding(16)
                    .background(Color(VeyraTheme.Colors.veyraMediumDarkGray))
                    .cornerRadius(16)
                    .padding(.horizontal, 12)
                    
                    Button(action: {
                        // TODO ACTION VIDER LA FILE
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "trash")
                            Text("Vider la file")
                                .font(.system(size: 15, weight: .semibold))
                        }
                        .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                        .padding(.vertical, 10)
                        .frame(maxWidth: .infinity)
                    }
                    .overlay(
                        RoundedRectangle(cornerRadius: 50)
                            .stroke(Color(VeyraTheme.Colors.veyraLightGray))
                    )
                    .padding(.top, 4)
                    .padding(.horizontal, 12)
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "text.badge.plus")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 70, height: 70)
                            .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                            .padding(.bottom, 12)
                        
                        
                        Text("Aucun morceau dans la file de lecture")
                            .foregroundColor(.white)
                            .font(.system(size: 18, weight: .semibold))
                        
                        Text("Ajoutez des morceaux a partir de votre bibliotheque")
                            .foregroundColor(Color(VeyraTheme.Colors.veyraLightGray))
                            .font(.system(size: 14))
                            .padding(.horizontal, 32)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                    
                    Spacer()
                }
                Spacer(minLength: 0)
            }
            .frame(maxHeight: .infinity, alignment: .top)
        }
        .safeAreaInset(edge: .bottom) {
            Color.clear
                .frame(height: 50)
        }
    }
}
