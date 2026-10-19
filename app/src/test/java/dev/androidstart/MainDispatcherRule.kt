package dev.androidstart

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `viewModelScope` 用的是 `Dispatchers.Main`，JVM 单测里没有主线程，
 * 不替换就会抛 "Module with the Main dispatcher had failed to initialize"。
 *
 * 默认用 [StandardTestDispatcher] 而不是 `UnconfinedTestDispatcher`，这不是风格问题：
 * unconfined 会让 `stateIn(WhileSubscribed)` **在订阅的一瞬间同步跑完上游**，
 * 于是"先收到初始值、再收到加载完成"这个顺序不再是确定的——
 * 测试里一旦依赖"第几个事件"，就会随机红。
 *
 * 代价：动作类断言后面要 `advanceUntilIdle()`。这比"偶发的红"便宜得多。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
