package dev.androidstart.feature.tasks

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import dev.androidstart.data.model.Task
import dev.androidstart.ui.theme.AndroidStartTheme

/**
 * 列表页的 Preview。
 *
 * 单独一个文件的原因很实际：预览代码占屏幕文件的三分之一，却不跟任何生产逻辑一起变。
 * 放在一起时，读 UI 的人要滚过几十行假数据才看到下一个 Composable。
 *
 * 预览**直接喂状态、不走 ViewModel**：这样以后状态变多、依赖变复杂，预览也不会被拖垮。
 */

private val PreviewTasks = listOf(
    Task(id = 1, title = "接一个真实的数据源（Room / 网络）", done = false),
    Task(id = 2, title = "把列表拆成独立 feature 模块", done = true),
    Task(id = 3, title = "接上 CI", done = false),
)

@Preview(name = "列表 · 亮色", showBackground = true)
@Composable
private fun TaskListScreenPreview() {
    AndroidStartTheme(dynamicColor = false) {
        TaskListScreen(
            uiState = TaskListUiState(tasks = PreviewTasks, isLoading = false),
            onIntent = {},
            onTaskClick = {},
        )
    }
}

@Preview(name = "列表 · 暗色", showBackground = true)
@Composable
private fun TaskListScreenDarkPreview() {
    AndroidStartTheme(darkTheme = true, dynamicColor = false) {
        TaskListScreen(
            uiState = TaskListUiState(tasks = PreviewTasks, isLoading = false),
            onIntent = {},
            onTaskClick = {},
        )
    }
}

@Preview(name = "空状态", showBackground = true)
@Composable
private fun TaskListScreenEmptyPreview() {
    AndroidStartTheme(dynamicColor = false) {
        TaskListScreen(
            uiState = TaskListUiState(tasks = emptyList(), isLoading = false),
            onIntent = {},
            onTaskClick = {},
        )
    }
}
