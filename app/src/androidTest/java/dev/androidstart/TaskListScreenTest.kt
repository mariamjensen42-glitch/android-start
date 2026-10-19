package dev.androidstart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.androidstart.data.model.Task
import dev.androidstart.feature.tasks.TaskListIntent
import dev.androidstart.feature.tasks.TaskListScreen
import dev.androidstart.feature.tasks.TaskListUiState
import dev.androidstart.ui.theme.AndroidStartTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI 测试：跑在设备/模拟器上（`./gradlew connectedDebugAndroidTest`）。
 *
 * 测的就是 MVI 契约本身：**状态进来、意图出去**。
 * 如果哪天有人把 Screen 改成自己去拿 ViewModel，这两个测试会先红。
 */
@RunWith(AndroidJUnit4::class)
class TaskListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun emptyState_isShown_whenThereAreNoTasks() {
        composeRule.setContent {
            AndroidStartTheme(dynamicColor = false) {
                TaskListScreen(
                    uiState = TaskListUiState(tasks = emptyList(), isLoading = false),
                    onIntent = {},
                    onTaskClick = {},
                )
            }
        }

        composeRule
            .onNodeWithText(context.getString(R.string.tasks_empty_title))
            .assertIsDisplayed()
    }

    @Test
    fun tappingARow_reportsItsId() {
        var clickedId: Long? = null

        composeRule.setContent {
            AndroidStartTheme(dynamicColor = false) {
                TaskListScreen(
                    uiState = TaskListUiState(
                        tasks = listOf(Task(id = 7, title = "被点的那一行")),
                        isLoading = false,
                    ),
                    onIntent = {},
                    onTaskClick = { clickedId = it },
                )
            }
        }

        composeRule.onNodeWithText("被点的那一行").performClick()

        composeRule.runOnIdle { assertEquals(7L, clickedId) }
    }

    @Test
    fun tappingTheCheckbox_emitsAToggleIntent() {
        var received: TaskListIntent? = null

        composeRule.setContent {
            AndroidStartTheme(dynamicColor = false) {
                TaskListScreen(
                    uiState = TaskListUiState(
                        tasks = listOf(Task(id = 7, title = "打勾的那一行", done = false)),
                        isLoading = false,
                    ),
                    onIntent = { received = it },
                    onTaskClick = {},
                )
            }
        }

        composeRule.onNode(isToggleable()).performClick()

        composeRule.runOnIdle { assertEquals(TaskListIntent.ToggleTask(id = 7), received) }
    }
}
