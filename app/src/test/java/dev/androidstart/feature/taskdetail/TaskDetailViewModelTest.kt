package dev.androidstart.feature.taskdetail

import app.cash.turbine.test
import dev.androidstart.MainDispatcherRule
import dev.androidstart.data.InMemoryTaskRepository
import dev.androidstart.data.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

    @Test
    fun `exposes the requested task`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "写个详情页")))
        val viewModel = TaskDetailViewModel(repository, taskId = 1)

        viewModel.uiState.test {
            assertTrue("订阅前应该是加载中", awaitItem().isLoading)

            val loaded = awaitItem()
            assertEquals("写个详情页", loaded.task?.title)
            assertFalse(loaded.isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a missing task ends up as null instead of blowing up`() = runTest {
        val repository = InMemoryTaskRepository()
        val viewModel = TaskDetailViewModel(repository, taskId = 42)

        viewModel.uiState.test {
            skipItems(1) // 初始值

            val loaded = awaitItem()
            assertNull(loaded.task)
            assertFalse(loaded.isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setDone persists through the repository`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "a", done = false)))
        val viewModel = TaskDetailViewModel(repository, taskId = 1)

        viewModel.setDone(done = true)

        assertTrue(repository.observeTask(1).first()!!.done)
    }

    @Test
    fun `delete removes the task`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "a")))
        val viewModel = TaskDetailViewModel(repository, taskId = 1)

        viewModel.delete()

        assertNull(repository.observeTask(1).first())
    }
}
