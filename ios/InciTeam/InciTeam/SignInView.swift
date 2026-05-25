import SwiftUI

struct SignInView: View {
    @Environment(SessionStore.self) private var sessionStore
    @Environment(\.dismiss) private var dismiss

    @State private var username = ""
    @State private var password = ""
    @State private var errorMessage = ""
    @State private var isSigningIn = false

    private var canSubmit: Bool {
        !username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !password.isEmpty
            && !isSigningIn
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Username", text: $username)
                        .textContentType(.username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()

                    SecureField("Password", text: $password)
                        .textContentType(.password)
                        .submitLabel(.go)
                        .onSubmit {
                            submitIfPossible()
                        }
                }

                if !errorMessage.isEmpty {
                    Section {
                        Text(errorMessage)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    Button {
                        submitIfPossible()
                    } label: {
                        HStack {
                            Spacer()
                            if isSigningIn {
                                ProgressView()
                            } else {
                                Text("Sign In")
                            }
                            Spacer()
                        }
                    }
                    .disabled(!canSubmit)
                }
            }
            .navigationTitle("Sign In")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }

    private func submitIfPossible() {
        guard canSubmit else {
            return
        }

        Task {
            await signIn()
        }
    }

    private func signIn() async {
        isSigningIn = true
        errorMessage = ""

        do {
            try await sessionStore.signIn(username: username, password: password)
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }

        isSigningIn = false
    }
}

#Preview {
    SignInView()
        .environment(SessionStore.previewSignedOut)
}
