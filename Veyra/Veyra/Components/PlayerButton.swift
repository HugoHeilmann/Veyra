import SwiftUI

struct PlayerButton: View {
    let randomPlay: Bool
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(randomPlay ? "Lecture aleatoire" : "Lecture ordonnee")
                    .foregroundColor(.white)
                    .font(.system(size: 16, weight: .semibold))
            }
            
            Spacer()
            
            ZStack {
                Capsule()
                    .fill(Color(VeyraTheme.Colors.veyraGreen))
                    .frame(width: 70, height: 30)
                
                Image(systemName: "play.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Color(VeyraTheme.Colors.veyraDarkGray))
            }
        }
        .padding(.vertical, 12)
    }
}
