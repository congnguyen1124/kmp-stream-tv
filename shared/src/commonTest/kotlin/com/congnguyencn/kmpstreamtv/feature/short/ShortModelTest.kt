package com.congnguyencn.kmpstreamtv.feature.short

import com.congnguyencn.kmpstreamtv.feature.short.data.repository.DummyShortRepository
import com.congnguyencn.kmpstreamtv.feature.short.data.source.ShortDummyDataSource
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortUiMapper
import com.congnguyencn.kmpstreamtv.feature.short.presentation.ShortViewModel
import com.congnguyencn.kmpstreamtv.feature.short.presentation.StoryGroupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShortModelTest {
    private val repository = DummyShortRepository(ShortDummyDataSource())

    @Test
    fun cataloguePagesWithoutDuplicates() =
        runTest {
            val first = repository.getShorts(page = 0, pageSize = 4)
            val second = repository.getShorts(page = 1, pageSize = 4)

            assertEquals(4, first.items.size)
            assertEquals(4, second.items.size)
            assertTrue(first.hasNextPage)
            assertFalse(second.hasNextPage)
            assertEquals(8, (first.items + second.items).distinctBy { it.id }.size)
        }

    @Test
    fun shortViewModelLoadsMoreAndSharesProviderFollowState() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val viewModel = ShortViewModel(repository, ShortUiMapper())
                advanceUntilIdle()
                assertEquals(4, viewModel.currentState.items.size)

                val firstItem = viewModel.currentState.items.first()
                val providerId = firstItem.providerId
                viewModel.toggleFollow(providerId)
                viewModel.toggleLike(firstItem.id)
                viewModel.addComment(firstItem.id, "Great short")
                assertTrue(
                    viewModel.currentState.items
                        .first()
                        .isLiked,
                )
                assertEquals(
                    firstItem.likeCount + 1,
                    viewModel.currentState.items
                        .first()
                        .likeCount,
                )
                assertEquals(
                    firstItem.commentCount + 1,
                    viewModel.currentState.items
                        .first()
                        .commentCount,
                )
                assertEquals(listOf("Great short"), viewModel.commentsFor(firstItem.id))

                viewModel.reload()
                advanceUntilIdle()
                assertEquals(
                    firstItem.commentCount + 1,
                    viewModel.currentState.items
                        .first()
                        .commentCount,
                )

                viewModel.loadMore()
                advanceUntilIdle()
                assertEquals(8, viewModel.currentState.items.size)

                assertTrue(
                    viewModel.currentState.items
                        .filter { it.providerId == providerId }
                        .all { it.isFollowingProvider },
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun storyGroupStartsAtRequestedItemAndStopsAtItsEdges() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val viewModel = StoryGroupViewModel(repository, ShortUiMapper())
                viewModel.load("short-football")
                advanceUntilIdle()

                assertEquals("short-football", viewModel.currentState.items[viewModel.currentState.activeIndex].id)
                assertTrue(viewModel.moveToPrevious())
                assertFalse(viewModel.moveToPrevious())
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun selectingAnUnloadedIdContinuesPagingUntilItIsActive() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val viewModel = ShortViewModel(repository, ShortUiMapper())
                viewModel.selectById("short-festival")
                advanceUntilIdle()

                assertEquals(8, viewModel.currentState.items.size)
                assertEquals(
                    "short-festival",
                    viewModel.currentState.items[viewModel.currentState.activeIndex].id,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }
}
