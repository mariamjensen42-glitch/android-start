/**
 * 性能模块。
 *
 * `com.android.test`：本模块不产出可安装的 App，它是一个**测试 APK**，
 * 靠 [targetProjectPath] 把 `:app` 一起编出来再装到设备上驱动它。
 *
 * 两种测试活在同一个模块里（这也是 Google 官方参考实现 nowinandroid 的形态）：
 * - `BaselineProfileGenerator`：走关键路径，采集出 `baseline-prof.txt`
 * - `StartupBenchmarks`：量冷启动，用来判断 profile 到底有没有用
 *
 * 为什么必须单独一个模块，而不是塞进 `app/src/androidTest`：
 * 两者都要跑在**接近生产**的包上（不可调试、跑过 R8），而 androidTest 只能跑 debug 包。
 */
plugins {
    // **不能写版本号**：`com.android.test` 与 `com.android.application` 是同一个 AGP 构件，
    // 版本已由根项目带进 classpath，这里再写会报 "already on the classpath"。
    id("com.android.test")
    // 提供 `baselineProfile { managedDevices ... }` 配置与 generateBaselineProfile 任务。
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "dev.androidstart.benchmarks"

    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // macrobenchmark 的底线是 API 23；这里取 28 是因为强制编译模式（CompilationMode）
        // 依赖 profileable 语义，28 以下行为不稳定，而 GMD 本身就是 34。
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    /**
     * 被测应用就是本仓库的 `:app`，且测的是它的 **release** 变体
     * （不可调试 + R8 优化过，才有性能意义）。
     */
    targetProjectPath = ":app"
    // 让测试进程自己观测被测包，报告里才能把两边分开。
    experimentalProperties["android.experimental.self-instrumenting"] = true

    /**
     * Gradle Managed Device：由 Gradle 自己起停模拟器，**不需要手动插真机**，
     * 本地和 CI 跑出来的环境一致。
     *
     * 这里的安全牌是 `google_apis`（不是 `google_apis_playstore`）：
     * Play Store 镜像不允许 macrobenchmark 需要的那些 shell 能力，跑不起来。
     * 换 apiLevel 前先确认 `$ANDROID_HOME/system-images/android-<api>/` 里有对应镜像。
     */
    testOptions.managedDevices.localDevices {
        create("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "google_apis"
        }
    }
}

/**
 * Baseline Profile 生成用哪台设备。
 *
 * `useConnectedDevices = false`：不碰你插着的手机，一律用上面的 GMD——
 * 否则"在谁的手机上生成的 profile"会变成一个没人能复现的变量。
 */
baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.junit)
    // 生成 profile 时要按资源 id 定位控件、走真实交互路径。
    implementation(libs.androidx.test.uiautomator)
}
