package dev.androidstart.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.androidstart.di.AppContainer
import dev.androidstart.feature.taskdetail.TaskDetailScreen
import dev.androidstart.feature.taskdetail.TaskDetailViewModel
import dev.androidstart.feature.tasks.TasksScreen
import dev.androidstart.feature.tasks.TasksViewModel
import kotlinx.serialization.Serializable

/**
 * 导航路由。**唯一**的页面清单就在这里。
 *
 * 为什么用 `@Serializable` 的 data class/object 而不是字符串路径：
 * 传参靠构造函数，编译期就能查错；`rememberNavBackStack` 也靠序列化把返回栈
 * 存进 savedInstanceState，进程被回收后能原样恢复。
 */
@Serializable
data object TasksRoute : NavKey

@Serializable
data class TaskDetailRoute(val taskId: Long) : NavKey

/**
 * 全应用的导航宿主。
 *
 * 加页面的三个动作，缺一不可：
 * 1. 上面加一个 `@Serializable` 路由；
 * 2. 下面 `entryProvider` 里加一个 `entry`；
 * 3. 在发起方 `backStack.add(...)`。
 */
@Composable
fun AppNavDisplay(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    // 返回栈跨配置变更与进程死亡自动恢复（因为 key 都可序列化）。
    val backStack = rememberNavBackStack(TasksRoute)

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        // 系统返回 / 返回手势。
        onBack = { backStack.removeLastOrNull() },
        // 这两个装饰器缺一不可：
        // - SaveableStateHolder：每个页面的 rememberSaveable 各存各的；
        // - ViewModelStore：每个 NavEntry 一份 ViewModel，返回时状态还在。
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<TasksRoute> {
                val viewModel: TasksViewModel = viewModel(
                    factory = TasksViewModel.factory(container.taskRepository),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                TasksScreen(
                    uiState = uiState,
                    onAddTask = viewModel::addTask,
                    onToggleTask = viewModel::toggleTask,
                    onTaskClick = { taskId -> backStack.add(TaskDetailRoute(taskId)) },
                )
            }

            entry<TaskDetailRoute> { route ->
                val viewModel: TaskDetailViewModel = viewModel(
                    factory = TaskDetailViewModel.factory(container.taskRepository, route.taskId),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                TaskDetailScreen(
                    uiState = uiState,
                    onToggleDone = viewModel::setDone,
                    onDelete = {
                        viewModel.delete()
                        backStack.removeLastOrNull()
                    },
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
