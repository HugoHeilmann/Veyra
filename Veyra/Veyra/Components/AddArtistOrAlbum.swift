import SwiftUI

struct AddArtistOrAlbum: View {
    let isArtist: Bool
    
    var body: some View {
        HStack {
            Text("Creer un nouvel " + (isArtist ? "artiste" : "album"))
                .foregroundColor(.white)
                .font(.system(size: 16, weight: .semibold))
            
            Spacer()
            
            ZStack {
                Capsule()
                    .fill(Color(VeyraTheme.Colors.veyraGreen))
                    .frame(width: 70, height: 30)
                
                Image(systemName: "plus")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Color(VeyraTheme.Colors.veyraDarkGray))
            }
        }
        .padding(.vertical, 12)
    }
}
