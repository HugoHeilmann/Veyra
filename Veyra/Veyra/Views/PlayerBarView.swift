import SwiftUI

struct PlayerBarView: View {
    let music: Music
    @EnvironmentObject private var playerManager: PlayerManager

    var body: some View {
        VStack(spacing: 0) {
            Divider()
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(music.title)
                        .font(.subheadline)
                        .lineLimit(1)
                    Text(music.artist ?? "Unknown artist")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }

                Spacer()

                Button {
                    playerManager.togglePlayPause()
                } label: {
                    Image(systemName: playerManager.isPlaying ? "pause.fill" : "play.fill")
                        .font(.title2)
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
            .background(.ultraThinMaterial)
        }
    }
}
