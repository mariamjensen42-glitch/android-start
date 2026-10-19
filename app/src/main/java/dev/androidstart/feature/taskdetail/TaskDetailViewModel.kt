package dev.androidstart.feature.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.androidstart.data.TaskRepository
import dev.androidstart.data.model.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 详情页状态。`task == null && !isLoading` 表示"这个 id 已经不存在了"，
 * UI 据此显示一句人话，而不是空白页或崩溃。
 */
data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
)

/**
 * 详情页 ViewModel。
 *
 * 它需要一个**运行时才知道的参数**（taskId），而 Hilt 是在编译期建图的，
 * 所以用 Assisted Injection：`repository` 由 Hilt 给，`taskId` 由调用方传。
 *
 * 另一种常见做法是 `SavedStateHandle.get<Long>("taskId")`，
 * 但它要求导航参数先被塞进 SavedStateHandle——Nav3 的路由是类型安全的 data class，
 * 走 assisted injection 更直接，也不会出现"key 拼错了只在运行时炸"的问题。
 */
@HiltViewModel(assistedFactory = TaskDetailViewModel.Factory::class)
class TaskDetailViewModel @AssistedInject constructor(
    private val repository: TaskRepository,
    @Assisted private val taskId: Long,
) : ViewModel() {

    val uiState: StateFlow<TaskDetailUiState> = repository.observeTask(taskId)
        .map { task -> TaskDetailUiState(task = task, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TaskDetailUiState(),
        )

    fun setDone(done: Boolean) {
        viewModelScope.launch { repository.setDone(taskId, done) }
    }

    fun delete() {
        viewModelScope.launch { repository.delete(taskId) }
    }

    /**
     * Hilt 用这个工厂创建 ViewModel；调用点在 `AppNavigation` 里：
     * `hiltViewModel<TaskDetailViewModel, TaskDetailViewModel.Factory>(creationCallback = { it.create(route.taskId) })`
     */
    @AssistedFactory
    interface Factory {
        fun create(taskId: Long): TaskDetailViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
