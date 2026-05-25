import SwiftUI

struct LandingView: View {
    @Environment(\.openURL) private var openURL
    @State private var isShowingSignIn = false

    private let signUpURL = URL(string: "https://www.inciteam.com/signup")
    private let privacyURL = URL(string: "https://www.inciteam.com/privacy")

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                VStack(spacing: 28) {
                    Spacer()

                    VStack(spacing: 16) {
                        Image(systemName: "person.2.badge.gearshape.fill")
                            .font(.system(size: 58, weight: .semibold))
                            .foregroundStyle(InciTeamTheme.primary)
                            .accessibilityHidden(true)

                        VStack(spacing: 6) {
                            Text("InciTeam")
                                .font(.largeTitle.weight(.bold))

                            Text("Incident and Team Management platform for ServiceNow")
                                .font(.title3)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                        }
                    }

                    Spacer()

                    VStack(spacing: 12) {
                        Button {
                            isShowingSignIn = true
                        } label: {
                            Label("Sign In", systemImage: "person.crop.circle")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)

                        Button {
                            if let signUpURL {
                                openURL(signUpURL)
                            }
                        } label: {
                            Label("Sign Up", systemImage: "safari")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.large)

                        Button {
                            if let privacyURL {
                                openURL(privacyURL)
                            }
                        } label: {
                            Text("Privacy Policy")
                                .font(.footnote.weight(.semibold))
                        }
                        .buttonStyle(.plain)
                        .padding(.top, 4)
                    }
                }
                .padding(24)
            }
            .navigationBarTitleDisplayMode(.inline)
        }
        .sheet(isPresented: $isShowingSignIn) {
            SignInView()
        }
    }
}

#Preview {
    LandingView()
        .environment(SessionStore.previewSignedOut)
}
