import SwiftUI

final class DownloadStatus: ObservableObject {
    @Published var message: String = "⏳ En attente…"
    
    var backgroundColor: UIColor {
        guard let first = message.first else {
            return VeyraTheme.Colors.veyraLightGray
        }
        
        switch first {
        case "✅":
            return VeyraTheme.Colors.veyraGreen
        case "❌":
            return VeyraTheme.Colors.veyraRed
        default:
            return VeyraTheme.Colors.veyraLightGray
        }
    }
}
