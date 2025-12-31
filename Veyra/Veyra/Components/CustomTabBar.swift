import SwiftUI

struct CustomTabBar: View {
    @Binding var selectedTab: Int
    
    private let tabs = ["Chansons", "Artistes", "Albums"]
    
    var body: some View {
        HStack(spacing: 8) {
            ForEach(tabs.indices, id: \.self) { index in
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        selectedTab = index
                    }
                } label: {
                    Text(tabs[index])
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(
                            selectedTab == index
                            ? Color(VeyraTheme.Colors.veyraGreen)
                            : .white
                        )
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(
                            Group {
                                if selectedTab == index {
                                    Color(VeyraTheme.Colors.veyraGreen)
                                        .opacity(0.25)
                                        .clipShape(RoundedRectangle(cornerRadius: 14))
                                } else {
                                    Color.clear
                                }
                            }
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 16)
    }
}
