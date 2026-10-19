package dev.androidstart

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `viewModelScope` 用的是 `Dispatchers.Main`，JVM 单测里没有主线程，
 * 不替换就会抛 "Module with the Main dispatcher had failed to initialize"。
 *
 * 默认用 [UnconfinedTestDispatcher]：协程立即执行，测试里不用到处 `advanceUntilIdle()`。
 * 需要精确控制执行顺序时，构造时传入 `StandardTestDispatcher()`。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
