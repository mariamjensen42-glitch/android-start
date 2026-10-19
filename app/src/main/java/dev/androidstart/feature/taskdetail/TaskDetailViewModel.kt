package dev.androidstart.feature.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.androidstart.data.TaskRepository
import dev.androidstart.data.model.Task
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
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

/** 详情页能收到的全部用户意图。 */
sealed interface TaskDetailIntent {
    data class SetDone(val done: Boolean) : TaskDetailIntent

    /** 删除当前任务。删完要离开这个页面，见 [TaskDetailEffect]。 */
    data object Delete : TaskDetailIntent
}

/**
 * 一次性的**副作用**（effect）：不是状态，不该放进 [TaskDetailUiState]。
 *
 * 为什么不用 UiState 表达"刚被删除了"：状态会被重放。旋屏、进程恢复、
 * 重订阅都会让"已删除 = true"再出现一次，于是页面被连续弹两次——
 * 这就是经典的"一次性事件当状态用"的坑。
 *
 * 用 `Channel` 而不是 `SharedFlow`：Channel 是队列语义，**每个事件只投递一次**，
 * 且在没有订阅者时缓冲（`Channel.BUFFERED`），不会丢。
 */
sealed interface TaskDetailEffect {
    data object TaskDeleted : TaskDetailEffect
}

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

    private val _effects = Channel<TaskDetailEffect>(capacity = Channel.BUFFERED)

    /**
     * 由导航层（`AppNavigation`）收集并决定怎么响应——ViewModel 不碰导航。
     * 这样"删完返回上一页"是导航层的决定，而不是写死在业务逻辑里。
     */
    val effects: Flow<TaskDetailEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: TaskDetailIntent) {
        when (intent) {
            is TaskDetailIntent.SetDone -> setDone(intent.done)
            TaskDetailIntent.Delete -> delete()
        }
    }

    private fun setDone(done: Boolean) {
        viewModelScope.launch { repository.setDone(taskId, done) }
    }

    private fun delete() {
        viewModelScope.launch {
            repository.delete(taskId)
            _effects.send(TaskDetailEffect.TaskDeleted)
        }
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
