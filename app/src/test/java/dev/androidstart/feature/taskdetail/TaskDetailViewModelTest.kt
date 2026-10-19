package dev.androidstart.feature.taskdetail

import app.cash.turbine.test
import dev.androidstart.MainDispatcherRule
import dev.androidstart.data.InMemoryTaskRepository
import dev.androidstart.data.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun repositoryWith(vararg tasks: Task) = InMemoryTaskRepository(tasks.toList())

    @Test
    fun `starts in the loading state before anyone collects`() {
        val viewModel = TaskDetailViewModel(repositoryWith(Task(id = 1, title = "a")), taskId = 1)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `exposes the requested task`() = runTest {
        val viewModel = TaskDetailViewModel(repositoryWith(Task(id = 1, title = "写个详情页")), taskId = 1)

        val loaded = viewModel.uiState.first { !it.isLoading }

        assertEquals("写个详情页", loaded.task?.title)
        assertFalse(loaded.isLoading)
    }

    @Test
    fun `a missing task ends up as null instead of blowing up`() = runTest {
        val viewModel = TaskDetailViewModel(InMemoryTaskRepository(), taskId = 42)

        val loaded = viewModel.uiState.first { !it.isLoading }

        assertNull(loaded.task)
    }

    @Test
    fun `SetDone intent persists through the repository`() = runTest {
        val repository = repositoryWith(Task(id = 1, title = "a", done = false))
        val viewModel = TaskDetailViewModel(repository, taskId = 1)

        viewModel.onIntent(TaskDetailIntent.SetDone(done = true))
        advanceUntilIdle()

        assertTrue(repository.observeTask(1).first()!!.done)
    }

    @Test
    fun `Delete intent removes the task and emits an effect exactly once`() = runTest {
        val repository = repositoryWith(Task(id = 1, title = "a"))
        val viewModel = TaskDetailViewModel(repository, taskId = 1)

        viewModel.effects.test {
            viewModel.onIntent(TaskDetailIntent.Delete)
            advanceUntilIdle()

            // 效果是"每个事件只投递一次"——这正是用 Channel 而不是把
            // "已删除"塞进 UiState 的理由（后者会在旋屏/重订阅时重放）。
            assertEquals(TaskDetailEffect.TaskDeleted, awaitItem())

            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(repository.observeTask(1).first())
    }
}
