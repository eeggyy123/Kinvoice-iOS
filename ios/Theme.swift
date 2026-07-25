import SwiftUI

enum KinTheme {
    static let ink = Color(red: 0.12, green: 0.14, blue: 0.15)
    static let secondary = Color(red: 0.38, green: 0.40, blue: 0.39)
    static let accent = Color(red: 0.70, green: 0.28, blue: 0.18)
    static let paper = Color(red: 0.97, green: 0.98, blue: 0.97)
    static let line = Color.black.opacity(0.10)
}

extension View {
    func kinPage() -> some View { self.background(KinTheme.paper.ignoresSafeArea()).tint(KinTheme.accent) }
}
