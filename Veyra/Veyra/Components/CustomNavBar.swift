import SwiftUI

struct CustomNavBar: View {
    @Binding var selectedTab: Int
    
    var body: some View {
        HStack {
            tabButton(index: 0,
                      title: "Ma musique",
                      systemImage: "music.note")
            
            tabButton(index: 1,
                      title: "Playlists",
                      systemImage: "list.bullet.rectangle.portrait")
            
            tabButton(index: 2,
                      title: "Queue",
                      systemImage: "music.note.list")
            
            tabButton(index: 3,
                      title: "Télécharger",
                      systemImage: "arrow.down.circle.fill")
        }
        .padding(.vertical, 20)
        .padding(.horizontal, 16)
        .frame(maxWidth: UIScreen.main.bounds.width * 0.95)
        .background(Color(VeyraTheme.Colors.veyraMediumGray))
        .clipShape(RoundedCorner(radius: 18, corners: [.topLeft, .topRight]))
        .shadow(radius: 4)
    }
    
    @ViewBuilder
    private func tabButton(index: Int, title: String, systemImage: String) -> some View {
        let isSelected = selectedTab == index
        
        Button {
            selectedTab = index
        } label: {
            VStack(spacing: 4) {
                Image(systemName: systemImage)
                    .font(.system(size: 20))
                Text(title)
                    .font(.caption2)
            }
            .foregroundColor(
                Color(isSelected ? VeyraTheme.Colors.veyraGreen : VeyraTheme.Colors.veyraLightGray)
            )
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}
