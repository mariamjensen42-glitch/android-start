package dev.androidstart.feature.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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

class TaskDetailViewModel(
    private val repository: TaskRepository,
    private val taskId: Long,
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

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(repository: TaskRepository, taskId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TaskDetailViewModel(repository, taskId) }
            }
    }
}
