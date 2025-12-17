import Foundation
import AVFoundation
import Combine

final class PlayerManager: ObservableObject {
    static let shared = PlayerManager()
    
    @Published private(set) var currentMusic: Music?
    @Published private(set) var isPlaying: Bool = false
    
    private var player: AVAudioPlayer?
    
    private init() {
        configureAudioSession()
    }
    
    private func configureAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("AudioSession error: \(error)")
        }
    }
    
    func play(music: Music) {
        currentMusic = music
        isPlaying = true
        
        // Plus tard : si music.fileURL != nil -> cree un AVAudioSession
        // do {
        //      guard let url = music.fileURL else { return }
        //      player = try AVAudioPlayer(contentsOf: url)
        //      player?.play()
        //      isPlaying = true
        // } cstch {
        //      print("Failed to play: \(error)")
        // }
    }
    
    func togglePlayPause() {
        guard currentMusic != nil else { return }
        isPlaying.toggle()
        // + tard : player?.play() / player?.pause
    }
    
    func stop() {
        player?.stop()
        player = nil
        isPlaying = false
        currentMusic = nil
    }
}
