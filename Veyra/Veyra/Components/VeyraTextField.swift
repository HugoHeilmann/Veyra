import SwiftUI

struct VeyraTextField: View {
    @Binding var text: String
    let placeholder: String
    
    var body: some View {
        ZStack(alignment: .leading) {
            if text.isEmpty {
                Text(placeholder)
                    .foregroundColor(Color(VeyraTheme.Colors.veyraLightGray))
                    .padding( .horizontal, 12)
                    .padding(.vertical, 10)
            }
            
            TextField("", text: $text)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .foregroundColor(.white)
        }
        .frame(height: 50)
        .background(Color(VeyraTheme.Colors.veyraDarkGray))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color(VeyraTheme.Colors.veyraLightGray))
        )
        .cornerRadius(10)
    }
}
