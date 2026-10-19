package dev.androidstart.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.androidstart.feature.taskdetail.TaskDetailEffect
import dev.androidstart.feature.taskdetail.TaskDetailScreen
import dev.androidstart.feature.taskdetail.TaskDetailViewModel
import dev.androidstart.feature.tasks.TaskListScreen
import dev.androidstart.feature.tasks.TaskListViewModel
import kotlinx.serialization.Serializable

/**
 * 导航路由。**唯一**的页面清单就在这里。
 *
 * 为什么用 `@Serializable` 的 data class/object 而不是字符串路径：
 * 传参靠构造函数，编译期就能查错；`rememberNavBackStack` 也靠序列化把返回栈
 * 存进 savedInstanceState，进程被回收后能原样恢复。
 */
@Serializable
data object TaskListRoute : NavKey

@Serializable
data class TaskDetailRoute(val taskId: Long) : NavKey

/**
 * 全应用的导航宿主，也是**唯一**可以调用 `hiltViewModel()` 的地方。
 *
 * 加页面的三个动作，缺一不可：
 * 1. 上面加一个 `@Serializable` 路由；
 * 2. 下面 `entryProvider` 里加一个 `entry`；
 * 3. 在发起方 `backStack.add(...)`。
 */
@Composable
fun AppNavDisplay(modifier: Modifier = Modifier) {
    // 返回栈跨配置变更与进程死亡自动恢复（因为 key 都可序列化）。
    val backStack = rememberNavBackStack(TaskListRoute)
    val activity = LocalActivity.current

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        // 系统返回 / 返回手势。
        // 根页面**不能**直接 removeLastOrNull()：那会把栈清空，页面变一片空白。
        // 交给系统退出（finish）才是用户预期。
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            } else {
                activity?.finish()
            }
        },
        // 这两个装饰器缺一不可：
        // - SaveableStateHolder：每个页面的 rememberSaveable 各存各的；
        // - ViewModelStore：每个 NavEntry 一份 ViewModel，返回时状态还在。
        //   少了它 hiltViewModel() 拿到的会是整个返回栈共享的实例。
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<TaskListRoute> {
                val viewModel: TaskListViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                TaskListScreen(
                    uiState = uiState,
                    onIntent = viewModel::onIntent,
                    onTaskClick = { taskId -> backStack.add(TaskDetailRoute(taskId)) },
                )
            }

            entry<TaskDetailRoute> { route ->
                // 需要运行时参数的 ViewModel：用 assisted factory 把 taskId 递进去。
                val viewModel = hiltViewModel<TaskDetailViewModel, TaskDetailViewModel.Factory>(
                    creationCallback = { factory -> factory.create(route.taskId) },
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // ViewModel 只会说"这件事发生了"，怎么响应由导航层决定。
                // 一次性副作用在导航层收口，Screen 不需要知道删除意味着要离开。
                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            TaskDetailEffect.TaskDeleted -> backStack.removeLastOrNull()
                        }
                    }
                }

                TaskDetailScreen(
                    uiState = uiState,
                    onIntent = viewModel::onIntent,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
