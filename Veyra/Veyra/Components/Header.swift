import SwiftUI

struct Header: View {
    @State var text: String
    
    var body: some View {
        ZStack {
            Color(VeyraTheme.Colors.veyraMediumGray)
            
            Text(text)
                .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                .font(.system(size: 24, weight: .semibold))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
        }
        .frame(height: 70)
        .frame(maxWidth: .infinity)
        .shadow(radius: 4)
    }
}
