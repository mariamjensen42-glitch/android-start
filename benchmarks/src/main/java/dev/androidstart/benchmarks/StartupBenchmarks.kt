package dev.androidstart.benchmarks

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 冷启动测量。
 *
 * 这是"尺子"，不是"优化"：它不改 App 的任何行为，只产出数字。
 * 行业惯例的顺序是 **先量再优化**——没有基线，你无法判断加的 baseline profile
 * 到底带来了什么，也无法在以后发现回归。
 *
 * 三种 CompilationMode 的用法（决定测什么）：
 * - `CompilationMode.None()`：完全未编译，等于"刚装完第一次启动"，最接近 baseline profile 的战场
 * - `CompilationMode.Partial()`：只有 baseline profile 走 AOT，其余 JIT ——**最常用**，默认就它
 * - `CompilationMode.Full()`：全量 AOT，等于"用了几天之后"的理想态
 *
 * 想把 profile 的收益量出来，就同一个测试跑两遍：
 * 一次 `None()`，一次 `Partial()`，对比 `timeToInitialDisplay` 的 P50。
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) {
        pressHome()
        startActivityAndWait()
    }

    /**
     * 同一件事，但关掉 AOT 编译：用来量"baseline profile 到底救了多少"。
     *
     * 两个测试的数字放在一起看，差值就是 profile 的收益。差值很小（<5%）时，
     * 就该考虑是不是 profile 覆盖面不对，而不是继续堆迭代次数。
     */
    @Test
    fun startupColdWithoutProfile() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
