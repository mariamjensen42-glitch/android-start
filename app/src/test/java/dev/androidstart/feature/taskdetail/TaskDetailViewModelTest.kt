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

    /**
     * 回归测试：删除按钮连点两次，只能退一页。
     *
     * 这是 MVI 改造**引入**过的一个真 bug：`Delete` 变成 effect 之后不再天然幂等，
     * 两次点击 → 两个 effect → 导航层连 pop 两次 → 栈被清空。
     * 修法在 ViewModel（`deleteRequested` 幂等）与 Channel（CONFLATED）两处，
     * 这个测试锁住前者。
     */
    @Test
    fun `Delete intent twice still emits exactly one effect`() = runTest {
        val repository = repositoryWith(Task(id = 1, title = "a"))
        val viewModel = TaskDetailViewModel(repository, taskId = 1)

        viewModel.effects.test {
            viewModel.onIntent(TaskDetailIntent.Delete)
            viewModel.onIntent(TaskDetailIntent.Delete) // 用户连点
            advanceUntilIdle()

            assertEquals(TaskDetailEffect.TaskDeleted, awaitItem())
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * 删除后 Room 会先发出 `null`。UI 不能在那一帧渲染"任务不存在"，
     * 否则用户会看到一闪而过的错误页。这里锁住"删除中保留最后已知内容"的行为。
     *
     * 用 turbine 而不是 `first {}`：后者订阅完就退订，`WhileSubscribed` 的停止计时器会在
     * `advanceUntilIdle()` 里把上游停掉，断言就落到"过期状态"上了。
     */
    @Test
    fun `task stays visible while the delete is in flight`() = runTest {
        val viewModel = TaskDetailViewModel(repositoryWith(Task(id = 1, title = "a")), taskId = 1)

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals("a", state.task?.title)

            viewModel.onIntent(TaskDetailIntent.Delete)
            advanceUntilIdle()

            val afterDelete = expectMostRecentItem()
            assertEquals("a", afterDelete.task?.title)
            assertTrue(afterDelete.isDeleting)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
