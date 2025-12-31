//
//  VeyraApp.swift
//  Veyra
//
//  Created by user290650 on 12/9/25.
//

import SwiftUI

@main
struct VeyraApp: App {
    init() {
        UITabBar.appearance().backgroundColor = VeyraTheme.Colors.veyraMediumGray
        UITabBar.appearance().unselectedItemTintColor = VeyraTheme.Colors.veyraLightGray
    }
    
    var body: some Scene {
        WindowGroup {
            MainTabView()
                .background(Color(VeyraTheme.Colors.veyraDarkGray))
                .ignoresSafeArea()
        }
    }
}
