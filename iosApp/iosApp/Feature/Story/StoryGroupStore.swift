import Combine
import Foundation
import Shared

@MainActor
final class StoryGroupStore: ObservableObject {
    @Published private(set) var state: StoryGroupUiState

    private let viewModel: StoryGroupViewModel
    private var observation: Observation?

    init(initialId: String) {
        let viewModel = SharedDependencies().storyGroupViewModel()
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
        viewModel.load(initialShortId: initialId)
    }

    func retry(initialId: String) {
        viewModel.load(initialShortId: initialId)
    }

    func moveToPrevious() -> Bool {
        viewModel.moveToPrevious()
    }

    func moveToNext() -> Bool {
        viewModel.moveToNext()
    }

    deinit {
        observation?.cancel()
        viewModel.dispose()
    }
}
