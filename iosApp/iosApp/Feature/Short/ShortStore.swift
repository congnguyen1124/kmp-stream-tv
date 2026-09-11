import Combine
import Foundation
import Shared

@MainActor
final class ShortStore: ObservableObject {
    @Published private(set) var state: ShortUiState

    private let viewModel: ShortViewModel
    private var observation: Observation?

    init(initialId: String? = nil) {
        let viewModel = SharedDependencies().shortViewModel()
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
        if let initialId {
            viewModel.selectById(id: initialId)
        }
    }

    func select(id: String) {
        guard let index = state.items.firstIndex(where: { $0.id == id }) else { return }
        viewModel.select(index: Int32(index))
    }

    func retry() {
        viewModel.reload()
    }

    func toggleLike(_ item: ShortItemUiModel) {
        viewModel.toggleLike(id: item.id)
    }

    func toggleFollow(_ item: ShortItemUiModel) {
        viewModel.toggleFollow(providerId: item.providerId)
    }

    deinit {
        observation?.cancel()
        viewModel.dispose()
    }
}
