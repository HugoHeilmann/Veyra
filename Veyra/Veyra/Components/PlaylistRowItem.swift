import SwiftUI

struct PlaylistRowItem: View {
    let playlist: Playlist
    
    var onPlay: () -> Void = {}
    var onEdit: () -> Void = {}
    var onDelete: () -> Void = {}
    
    var body: some View {
        HStack(spacing: 12) {
            VStack(spacing: 4) {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(playlist.name)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                
                let plural = if playlist.musics.count == 1 {
                    "musique"
                } else {
                    "musiques"
                }
                Text("\(playlist.musics.count) \(plural)")
                    .font(.system(size: 13))
                    .foregroundColor(Color(VeyraTheme.Colors.veyraLightGray))
            }
            
            Spacer()
            
            HStack(spacing: 12) {
                Button(action: onPlay) {
                    Image(systemName: "play")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                }
                .buttonStyle(.plain)
                .padding(.trailing, 10)
                
                Button(action: onEdit) {
                    Image(systemName: "pencil")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                }
                .buttonStyle(.plain)
                .padding(.trailing, 10)
                
                Button(action: onDelete) {
                    Image(systemName: "trash")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Color(VeyraTheme.Colors.veyraRed))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(12)
        .background(Color(VeyraTheme.Colors.veyraMediumGray))
        .cornerRadius(12)
    }
}
