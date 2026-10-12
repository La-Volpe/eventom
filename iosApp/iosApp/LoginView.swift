import SwiftUI
import Shared

/// Republishes `AuthViewModel` state through the shared concrete
/// `observe(onChange:)` API (the StateFlow would arrive type-erased in Swift).
final class AuthBridge: ObservableObject {
    let viewModel: AuthViewModel

    @Published var state: AuthUiState

    private var observation: UiObservation?

    init(viewModel: AuthViewModel = SharedKoin.shared.authViewModel()) {
        self.viewModel = viewModel
        self.state = AuthUiState.Idle.shared
    }

    func startObserving() {
        guard observation == nil else { return }
        observation = viewModel.observe(onChange: { [weak self] newState in
            self?.state = newState
        })
    }

    func stopObserving() {
        observation?.stop()
        observation = nil
    }
}

/// Mock login (M2→IOS-2), Eventim look. Success routing lives in the root
/// composition; this view only reports via [onSuccess]/[onBack].
struct LoginView: View {
    @StateObject private var authBridge = AuthBridge()

    @State private var email = ""
    @State private var password = ""

    let onSuccess: () -> Void
    let onBack: () -> Void

    private var isLoading: Bool { authBridge.state is AuthUiState.Loading }
    private var errorMessage: String? {
        ((authBridge.state as? AuthUiState.Error))?.message
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Button("Back") { onBack() }
                    .buttonStyle(.bordered)
                    .disabled(isLoading)
                Spacer()
            }
            .padding(.top, 8)

            ScrollView(.vertical) {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer()
                        .frame(height: 24)

                    EventomWordmark()
                .frame(maxWidth: .infinity, alignment: .center)
            Text("Sign in to see your events")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .center)

            Spacer().frame(height: 32)

            TextField("E-mail", text: $email)
                .textInputAutocapitalization(.never)
                .keyboardType(.emailAddress)
                .autocorrectionDisabled()
                .padding(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(Color.primary.opacity(0.25), lineWidth: 1)
                )
                .disabled(isLoading)

            Spacer().frame(height: 12)

            SecureField("Password", text: $password)
                .padding(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(Color.primary.opacity(0.25), lineWidth: 1)
                )
                .disabled(isLoading)

            if let errorMessage {
                Spacer().frame(height: 10)
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }

            Spacer().frame(height: 24)

            Button(action: {
                authBridge.viewModel.signIn(email: email, password: password)
            }) {
                HStack {
                    if isLoading {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Text("Sign in")
                            .fontWeight(.semibold)
                    }
                }
                .frame(maxWidth: .infinity, minHeight: 52)
            }
            .background(EvenTomColors.ticketBlue)
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .disabled(isLoading || email.trimmingCharacters(in: .whitespaces).isEmpty || password.isEmpty)

                    Spacer().frame(height: 14)
                    Text("Demo account: demo@evantom.dev · demo1234")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)

                    // Clearance so the CTA scrolls clear of the keyboard.
                    Spacer().frame(height: 240)
                }
            }
            .scrollDismissesKeyboard(.immediately)
        }
        .padding(.horizontal, 20)
        .background(EvenTomColors.brightBackground.ignoresSafeArea())
        .onAppear { authBridge.startObserving() }
        .onDisappear { authBridge.stopObserving() }
        // Success → route (root-side reset keeps one owner, as on Android):
        // bridge the class type to a String so iOS 16's Equatable onChange works.
        .onChange(of: String(describing: type(of: authBridge.state))) { _ in
            if authBridge.state is AuthUiState.Success {
                onSuccess()
            }
        }
    }
}

#Preview {
    LoginView(onSuccess: {}, onBack: {})
}