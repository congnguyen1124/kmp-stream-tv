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

    func selectById(_ id: String) {
        viewModel.selectById(id: id)
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

    /// `ShortViewModel.addComment` trims and rejects blank text, then raises the shared count.
    func addComment(_ comment: String, to item: ShortItemUiModel) {
        viewModel.addComment(id: item.id, comment: comment)
    }

    /// `ShortViewModel.commentsFor`
    func comments(for item: ShortItemUiModel) -> [String] {
        viewModel.commentsFor(id: item.id)
    }

    /// Every loaded short by the same provider, which is what the provider sheet lists.
    func shorts(byProviderOf item: ShortItemUiModel) -> [ShortItemUiModel] {
        state.items.filter { $0.providerId == item.providerId }
    }

    /// The first loaded title or provider match, as the Android search dialog resolves it.
    func firstMatch(_ query: String) -> ShortItemUiModel? {
        let normalized = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else { return nil }
        return state.items.first { item in
            item.title.localizedCaseInsensitiveContains(normalized) ||
                item.providerName.localizedCaseInsensitiveContains(normalized)
        }
    }

    /// `NOT_INTERESTED` advances to the next loaded item, or stays put on the last one.
    func itemAfter(_ item: ShortItemUiModel) -> ShortItemUiModel? {
        guard let index = state.items.firstIndex(where: { $0.id == item.id }) else { return nil }
        let next = min(index + 1, state.items.count - 1)
        guard next != index else { return nil }
        return state.items[next]
    }

    deinit {
        observation?.cancel()
        viewModel.dispose()
    }
}
