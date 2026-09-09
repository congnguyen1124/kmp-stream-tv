import Foundation
import Shared

@MainActor
final class HomeStore: ObservableObject {
    @Published private(set) var state: HomeUiState

    private let viewModel: HomeViewModel
    private var observation: Observation?

    init() {
        let viewModel = SharedDependencies().homeViewModel()
        self.viewModel = viewModel
        self.state = viewModel.currentState
        self.observation = viewModel.observe { [weak self] state in
            guard let self else { return }
            if Thread.isMainThread {
                self.state = state
            } else {
                DispatchQueue.main.async { self.state = state }
            }
        }
    }

    func retry() {
        viewModel.loadHome()
    }

    deinit {
        observation?.cancel()
        viewModel.dispose()
    }
}
