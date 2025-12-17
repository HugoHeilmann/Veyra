import SwiftUI

struct MusicRow: View {
    let music: Music
    
    var body: some View {
        HStack(spacing: 6) {
            VStack(alignment: .leading, spacing: 4) {
                Text(music.title)
                    .foregroundColor(.white)
                    .font(.system(size: 16, weight: .semibold))
                    .lineLimit(1)
                    .truncationMode(.tail)
                
                Text(music.artist ?? "Unknown Artist")
                    .foregroundColor(Color(VeyraTheme.Colors.veyraLightGray))
                    .font(.system(size: 12))
                    .lineLimit(1)
                    .truncationMode(.tail)
                
                Text(music.album ?? "Unknown Album")
                    .foregroundColor(Color(VeyraTheme.Colors.veyraLightGray))
                    .font(.system(size: 12))
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            
            Spacer()
            
            Image(systemName: "plus.circle")
                .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                .font(.system(size: 24))
                .padding(.trailing, 10)
                        
            Image(systemName: "pencil")
                .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                .font(.system(size: 24, weight: .bold))
                .padding(.trailing, 10)
            
            coverView
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(Color(VeyraTheme.Colors.veyraMediumDarkGray))
        .cornerRadius(14)
    }
    
    private var coverView: some View {
        Group {
            if let path = music.coverPath,
               let uiImage = UIImage(contentsOfFile: path) {
                Image(uiImage: uiImage)
                    .resizable()
            } else {
                Image("default_cover")
                    .resizable()
            }
        }
        .scaledToFit()
        .frame(width: 54, height: 54)
        .cornerRadius(6)
        .clipped()
    }
}
