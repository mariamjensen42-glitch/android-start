package dev.androidstart.benchmarks

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 生成 Baseline Profile 的"剧本"。
 *
 * 它做的事：把 App 装到 GMD 上 → 按下面写的顺序走一遍 → 采集出
 * `app/src/release/generated/baselineProfiles/baseline-prof.txt`。
 * 这个文件**要提交**：它是打进包里的东西，不是构建产物。
 *
 * 写这段代码的唯一原则：**走用户真的会走的路**。
 * 覆盖到冷启动第一帧需要的那批代码就够了，别把每个页面都塞进来——
 * profile 越大，安装期 AOT 编译越久，收益反而被吃掉。
 *
 * 改了首屏相关代码（主题、DI 图、首屏 Compose 树）之后要重跑，
 * 否则 profile 记录的是旧的类/方法，收益会悄悄衰减。
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
        // 从桌面开始，保证是"冷启动"而不是"从最近任务恢复"。
        pressHome()
        startActivityAndWait()

        // 这里可以继续走关键路径，例如：
        //   device.findObject(By.text("新建任务")).click()
        //   device.wait(Until.hasObject(By.text("任务")), 5_000)
        // 每一步都会把触达的代码纳入 profile。现在不写，是因为
        // 首屏渲染完就够了；等真实功能进来再按需补。
        //
        // 不要在这里自己 killProcess()：进程的生命周期由 collect 管，
        // 手动杀会打断它按迭代收敛 profile 的逻辑。
    }
}
