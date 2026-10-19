package dev.androidstart.feature.taskdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.androidstart.R
import dev.androidstart.data.model.Task
import dev.androidstart.ui.theme.AndroidStartTheme

/**
 * 任务详情页。同样只画状态、不问数据：状态进来，意图出去。
 *
 * 删除是 Intent（`TaskDetailIntent.Delete`），"删完返回"是 ViewModel 通过
 * `effects` 通知导航层的结果——这里不做"删掉 + 后退"的拼接，
 * 因为那样等于让 UI 知道删除会离开页面这件事。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    uiState: TaskDetailUiState,
    onIntent: (TaskDetailIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.task_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (uiState.task != null) {
                        IconButton(
                            onClick = { onIntent(TaskDetailIntent.Delete) },
                            // 删除中禁用：否则用户以为没点上，连点两次会触发两次删除意图
                            // （ViewModel 也做了幂等，这里是第二道防线，同时给用户明确的视觉反馈）。
                            enabled = !uiState.isDeleting,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val task = uiState.task
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                task == null -> Text(
                    text = stringResource(R.string.task_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )

                else -> TaskContent(
                    task = task,
                    onToggleDone = { done -> onIntent(TaskDetailIntent.SetDone(done)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TaskContent(
    task: Task,
    onToggleDone: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineSmall,
        )

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.done, onCheckedChange = onToggleDone)
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(
                    if (task.done) R.string.task_status_done else R.string.task_status_open,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Text(
            text = stringResource(R.string.task_detail_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- 预览 ------------------------------------------------------------------

@Preview(name = "详情 · 亮色", showBackground = true)
@Composable
private fun TaskDetailScreenPreview() {
    AndroidStartTheme(dynamicColor = false) {
        TaskDetailScreen(
            uiState = TaskDetailUiState(
                task = Task(id = 1, title = "接一个真实的数据源", done = false),
                isLoading = false,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "详情 · 已删除", showBackground = true)
@Composable
private fun TaskDetailScreenMissingPreview() {
    AndroidStartTheme(dynamicColor = false) {
        TaskDetailScreen(
            uiState = TaskDetailUiState(task = null, isLoading = false),
            onIntent = {},
            onBack = {},
        )
    }
}
