package dev.androidstart.feature.tasks

import dev.androidstart.MainDispatcherRule
import dev.androidstart.data.InMemoryTaskRepository
import dev.androidstart.data.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
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
 *
 * 断言写法的约定（吃过亏才这么写）：**不数的第几个事件**。
 * `stateIn(WhileSubscribed)` 会发几个中间值取决于调度器实现细节，
 * 依赖它写出来的测试会在换调度器/升库时随机红。所以要断言就断言
 * "最终状态"或"仓库收到的写入"。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts in the loading state before anyone collects`() {
        val viewModel = TasksViewModel(InMemoryTaskRepository())

        // 还没人订阅 → WhileSubscribed 没启动上游 → 还是 initialValue。
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.tasks.isEmpty())
    }

    @Test
    fun `exposes the repository tasks once collected`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "写个屏幕")))
        val viewModel = TasksViewModel(repository)

        val loaded = viewModel.uiState.first { !it.isLoading }

        assertEquals(listOf("写个屏幕"), loaded.tasks.map { it.title })
        assertFalse(loaded.isLoading)
    }

    @Test
    fun `AddTask intent appends through the repository`() = runTest {
        val repository = InMemoryTaskRepository()
        val viewModel = TasksViewModel(repository)

        viewModel.onIntent(TasksIntent.AddTask("买牛奶"))
        advanceUntilIdle()

        assertEquals(listOf("买牛奶"), repository.observeTasks().first().map { it.title })
    }

    @Test
    fun `AddTask intent ignores a blank title`() = runTest {
        val repository = InMemoryTaskRepository()
        val viewModel = TasksViewModel(repository)

        viewModel.onIntent(TasksIntent.AddTask("   "))
        advanceUntilIdle()

        assertTrue(repository.observeTasks().first().isEmpty())
    }

    @Test
    fun `ToggleTask intent flips the done flag`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "a", done = false)))
        val viewModel = TasksViewModel(repository)
        // toggle 读的是 uiState.value，所以先把状态加载出来，模拟"用户看到了列表"。
        viewModel.uiState.first { !it.isLoading }

        viewModel.onIntent(TasksIntent.ToggleTask(id = 1))
        advanceUntilIdle()

        assertTrue(repository.observeTasks().first().single().done)
    }

    @Test
    fun `ToggleTask intent on an unknown id does nothing`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "a", done = false)))
        val viewModel = TasksViewModel(repository)
        viewModel.uiState.first { !it.isLoading }

        viewModel.onIntent(TasksIntent.ToggleTask(id = 99))
        advanceUntilIdle()

        assertFalse(repository.observeTasks().first().single().done)
    }

    @Test
    fun `uiState follows repository changes`() = runTest {
        val repository = InMemoryTaskRepository()
        val viewModel = TasksViewModel(repository)

        viewModel.onIntent(TasksIntent.AddTask("a"))
        advanceUntilIdle()
        repository.add("b")

        val state = viewModel.uiState.first { it.tasks.size == 2 }
        assertEquals(listOf("a", "b"), state.tasks.map { it.title })
    }
}
