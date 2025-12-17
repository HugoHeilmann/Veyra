import SwiftUI

struct AlphabetScrollBar: View {
    let titles: [String]
    let onSelect: (String) -> Void
    
    var body: some View {
        VStack(alignment: .center, spacing: 2) {
            ForEach(titles, id: \.self) { title in
                Text(title)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 18, height: 14, alignment: .center)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onSelect(title)
                    }
            }
        }
        .padding(.vertical, 8)
        .frame(width: 28)
        .background(Color(VeyraTheme.Colors.veyraMediumGray))
        .cornerRadius(14)
        .padding(.trailing, 10)
        .padding(.top, 6)
    }
}
