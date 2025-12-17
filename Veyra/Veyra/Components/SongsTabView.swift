import SwiftUI

struct SongsTabView: View {
    let musics: [Music]
    
    private let indexTitles: [String] = ["#"] + (65...90).map { String(UnicodeScalar($0)!) } // # + A-Z
    
    var body: some View {
        VStack(spacing: 12) {
            PlayerButton(randomPlay: false)
                .padding(.horizontal, 16)
                .padding(.top, 8)
            
            ScrollViewReader { proxy in
                HStack(alignment: .top, spacing: 8) {
                    // Liste
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 10) {
                            ForEach(sections, id: \.key) { section in
                                // Header
                                Text(section.key)
                                    .foregroundColor(Color(VeyraTheme.Colors.veyraGreen))
                                    .font(.system(size: 14, weight: .semibold))
                                    .padding(.vertical, 6)
                                    .padding(.horizontal, 12)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .cornerRadius(10)
                                    .id(section.key)
                                
                                // Musiques
                                ForEach(section.items, id: \.id) { music in
                                    MusicRow(music: music)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .topLeading)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 12)
                        
                        Color.clear
                            .frame(height: 50)
                    }
                    
                    AlphabetScrollBar(titles: sectionKeys) { title in
                        proxy.scrollTo(title, anchor: .top)
                    }
                }
            }
        }
    }
    
    private var sections: [(key: String, items: [Music])] {
        let sorted = musics.sorted {
            $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending
        }
        
        var dict: [String: [Music]] = [:]
        for music in sorted {
            let key = sectionKey(for: music.title)
            dict[key, default: []].append(music)
        }
        
        return dict
            .filter { !$0.value.isEmpty }
            .map { (key: $0.key, items: $0.value) }
            .sorted { $0.key < $1.key }
    }
    
    private var sectionKeys: [String] {
        sections.map { $0.key }
    }
    
    private func sectionKey(for title: String) -> String {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let first = trimmed.first else { return "#" }
        
        let firstString = String(first)
        
        let normalized = firstString
            .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .uppercased()
        
        guard let scalar = normalized.unicodeScalars.first else { return "#" }
        let v = scalar.value
        return (v >= 65 && v <= 90) ? normalized : "#"
    }
}
