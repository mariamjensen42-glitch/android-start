package dev.androidstart.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.androidstart.data.TaskRepository
import dev.androidstart.data.model.Task
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
 */
data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
) {
    val completedCount: Int get() = tasks.count { it.done }
}

/**
 * 列表页 ViewModel：**只做**状态编排，不碰 Context、不碰 Android 类，
 * 也不碰 Room 的实体类型（它只认识 [Task]）。
 *
 * `@HiltViewModel` 让 Hilt 负责创建它；构造参数只有一个接口，
 * 所以单测里 new 出来也不用任何容器（见 TasksViewModelTest）。
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    /**
     * 仓库的变化流 → UI 状态。
     *
     * `WhileSubscribed(5000)`：屏幕不可见 5 秒后停止收集，切回来立刻恢复。
     * 写 `SharingStarted.Eagerly` 会让后台的页面也一直在跑（也会一直占着 Room 的查询），
     * 是常见电耗来源。
     */
    val uiState: StateFlow<TasksUiState> = repository.observeTasks()
        .map { tasks -> TasksUiState(tasks = tasks, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TasksUiState(),
        )

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.add(title) }
    }

    fun toggleTask(id: Long) {
        // 从当前状态取，而不是再查一次仓库：多一次挂起往返就多一个竞态窗口。
        val task = uiState.value.tasks.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { repository.setDone(id, done = !task.done) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
