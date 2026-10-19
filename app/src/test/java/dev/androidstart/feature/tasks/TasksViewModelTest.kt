package dev.androidstart.feature.tasks

import app.cash.turbine.test
import dev.androidstart.MainDispatcherRule
import dev.androidstart.data.InMemoryTaskRepository
import dev.androidstart.data.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * ViewModel 单测：没有 Robolectric、没有设备、没有 mock 框架。
 *
 * 能这么干净是因为 [TasksViewModel] 只依赖 [dev.androidstart.data.TaskRepository] 接口，
 * 而 [InMemoryTaskRepository] 本身就是个合格的假实现——
 * **不要**为了测试去引 mockk，先问一句"真实的实现能不能直接用"。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts loading then exposes the repository tasks`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "写个屏幕")))
        val viewModel = TasksViewModel(repository)

        viewModel.uiState.test {
            // 第一个值是 stateIn 的 initialValue：还没订阅到仓库时就是加载中。
            assertEquals(TasksUiState(), awaitItem())

            val loaded = awaitItem()
            assertEquals(listOf("写个屏幕"), loaded.tasks.map { it.title })
            assertFalse(loaded.isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addTask appends through the repository`() = runTest {
        val repository = InMemoryTaskRepository()
        val viewModel = TasksViewModel(repository)

        viewModel.uiState.test {
            skipItems(2) // 初始值 + 空列表

            viewModel.addTask("买牛奶")

            assertEquals(listOf("买牛奶"), awaitItem().tasks.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addTask ignores a blank title`() = runTest {
        val repository = InMemoryTaskRepository()
        val viewModel = TasksViewModel(repository)

        viewModel.addTask("   ")

        assertTrue(repository.observeTasks().first().isEmpty())
    }

    @Test
    fun `toggleTask flips the done flag`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "a", done = false)))
        val viewModel = TasksViewModel(repository)

        viewModel.uiState.test {
            skipItems(2)

            viewModel.toggleTask(1)

            assertTrue(awaitItem().tasks.single().done)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleTask on an unknown id does nothing`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "a", done = false)))
        val viewModel = TasksViewModel(repository)

        viewModel.uiState.test {
            skipItems(2)

            viewModel.toggleTask(99)

            expectNoEvents()
            assertEquals(false, repository.observeTasks().first().single().done)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
