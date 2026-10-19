package dev.androidstart.feature.tasks

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.androidstart.data.TaskRepository
import dev.androidstart.data.model.Task
import dev.androidstart.ui.STATE_STOP_TIMEOUT_MILLIS
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 列表页的完整 UI 状态。
 *
 * 规则：屏幕需要的一切都在这里，不在 Composable 里拼装和推断。
 * 判断"加载中/空/有数据"是 UI 逻辑，写成一个 when 表达式而不是散落的条件。
 *
 * `@Immutable` 不是装饰：`List` 是接口，Compose 编译器无法证明它不可变，
 * 于是把整个 UiState 判为 unstable，导致**每次重组都跳过不了**这个屏幕。
 * 这里由我们保证不原地修改列表（仓库每次发出新实例），所以标注成立。
 * 数据量更大、需要结构共享时换成 `ImmutableList` + `persistentListOf()`。
 */
@Immutable
data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
) {
    val completedCount: Int get() = tasks.count { it.done }
}

/**
 * 列表页能收到的**全部**用户意图。
 *
 * 这就是 MVI 与"一堆回调 lambda"的实质区别：入口只有 [TaskListViewModel.onIntent] 一个，
 * 所以"用户在这个屏上能做什么"是一个可以一眼读完的清单，
 * 而不是散落在 Screen 签名上的若干函数类型。
 *
 * 加动作的流程是**编译器强制**的：这里加一个实现，`onIntent` 的 `when` 立刻报
 * "not exhaustive"，你不可能漏处理。
 */
sealed interface TaskListIntent {

    /** 新建一个任务；[title] 是用户输入的原始文本，去空格由数据层负责。 */
    data class AddTask(val title: String) : TaskListIntent

    /** 切换某一行的完成状态。 */
    data class ToggleTask(val id: Long) : TaskListIntent
}

/**
 * 列表页 ViewModel：**只做**状态编排，不碰 Context、不碰 Android 类，
 * 也不碰 Room 的实体类型（它只认识 [Task]）。
 *
 * 结构是标准 MVI：`Intent → 状态变更 → 新的 UiState`。
 * 这里没有单独的 reducer 抽象，因为状态变换只是"仓库的 Flow 映射一遍"——
 * 多一层 `reduce(state, intent)` 只会让同样的信息写两遍。
 * 什么时候该加 reducer：一个 intent 需要基于**旧状态**算出新状态、
 * 且这段计算有多个分支要单测时（那时把它抽成纯函数，单测不用起协程）。
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    /**
     * 仓库的变化流 → UI 状态。
     *
     * `WhileSubscribed(5000)`：屏幕不可见 5 秒后停止收集，切回来立刻恢复。
     * 写 `SharingStarted.Eagerly` 会让后台的页面也一直在跑（也会一直占着 Room 的查询），
     * 是常见电耗来源。
     */
    val uiState: StateFlow<TaskListUiState> = repository.observeTasks()
        .map { tasks -> TaskListUiState(tasks = tasks, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_STOP_TIMEOUT_MILLIS),
            initialValue = TaskListUiState(),
        )

    /** 唯一入口。事件从 Screen 进来，状态从 [uiState] 出去——单向，没有回头路。 */
    fun onIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.AddTask -> addTask(intent.title)
            is TaskListIntent.ToggleTask -> toggleTask(intent.id)
        }
    }

    private fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.add(title) }
    }

    private fun toggleTask(id: Long) {
        // 从当前状态取，而不是再查一次仓库：多一次挂起往返就多一个竞态窗口。
        // 状态还没加载出来时列表是空的，这里自然是 no-op——UI 那一刻也不会渲染出复选框。
        val task = uiState.value.tasks.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { repository.setDone(id, done = !task.done) }
    }
}
