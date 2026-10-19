package dev.androidstart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.androidstart.data.model.Task
import dev.androidstart.feature.tasks.TasksScreen
import dev.androidstart.feature.tasks.TasksUiState
import dev.androidstart.ui.theme.AndroidStartTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI 测试：跑在设备/模拟器上（`./gradlew connectedDebugAndroidTest`）。
 *
 * 只测两件容易被改坏的事：空状态真的显示了、点一行真的把 id 抛给了上层。
 * 文案从资源里取，改文案不会碎测试。
 */
@RunWith(AndroidJUnit4::class)
class TasksScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun emptyState_isShown_whenThereAreNoTasks() {
        composeRule.setContent {
            AndroidStartTheme(dynamicColor = false) {
                TasksScreen(
                    uiState = TasksUiState(tasks = emptyList(), isLoading = false),
                    onAddTask = {},
                    onToggleTask = {},
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
                TasksScreen(
                    uiState = TasksUiState(
                        tasks = listOf(Task(id = 7, title = "被点的那一行")),
                        isLoading = false,
                    ),
                    onAddTask = {},
                    onToggleTask = {},
                    onTaskClick = { clickedId = it },
                )
            }
        }

        composeRule.onNodeWithText("被点的那一行").performClick()

        composeRule.runOnIdle { assertEquals(7L, clickedId) }
    }
}
