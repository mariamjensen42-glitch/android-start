# Android Start

一个**安卓原生**（Kotlin + Jetpack Compose）起步工程，把 [vibestart](https://github.com/VinkyDev/vibestart) 的那套
"组合 → 验证 → 可维护" 思路搬到了 Android 原生栈上：版本只有一处来源、分层的边界写死在文档里、
所有检查一条命令能跑、AI 编码助手有一份明确的施工规范。

> vibestart 本身是全栈 TypeScript 脚手架，移动端只有 Expo（React Native），**不覆盖** Kotlin/Compose。
> 这个工程补的就是那块空白。

## 里面有什么

| 关注点 | 选择 | 为什么 |
|---|---|---|
| 语言 / UI | Kotlin 2.3.20 + Jetpack Compose（Material 3） | 原生唯一的主流答案 |
| 构建 | AGP 9.3.3 + Gradle 9.5，版本全在 `gradle/libs.versions.toml` | AGP 9 内建 Kotlin 编译，不再需要 `kotlin-android` 插件 |
| 导航 | Navigation 3（`navigation3-runtime` / `-ui`） | 类型安全路由，返回栈可序列化，跨进程死亡能恢复 |
| 状态 | ViewModel + `StateFlow<UiState>` + `collectAsStateWithLifecycle` | 单向数据流，UI 只画状态 |
| 依赖注入 | 手动 DI（`AppContainer`） | 起步阶段没有代码生成、没有注解处理，构建快；要换 Hilt 时改动面已收敛到一处 |
| 数据 | `TaskRepository` 接口 + 内存实现 | 契约先行，换 Room / 网络时 UI 与 ViewModel 不动 |
| 测试 | JUnit4 + `kotlinx-coroutines-test` + Turbine + Compose UI Test | 单测纯 JVM，几百毫秒；UI 测试一条命令 |
| 质量门 | Android Lint（`abortOnError`）+ 单测 + assemble | `./gradlew check` 一把梭 |

minSdk 24 / targetSdk 36 / compileSdk 37.1（compileSdk 是下限，被 Compose BOM 2026.09 的
`ui-test` 的 `minCompileSdk=37` 顶着；targetSdk 单独推进，两者不必同步）。

## 5 分钟跑起来

要求：JDK 17+（本机 JDK 21 即可）、Android SDK（`ANDROID_HOME` 或 `local.properties` 里的 `sdk.dir`）。

```bash
# 1. 编译 debug 包
./gradlew assembleDebug

# 2. 装到已连接的设备/模拟器并启动
./gradlew installDebug
adb shell am start -n dev.androidstart/.MainActivity

# 3. 三条验收命令，提交前必须绿
./gradlew assembleDebug testDebugUnitTest lint
```

用 Android Studio 直接 `Open` 这个目录即可，同步不需要任何额外配置。

跑设备上的 UI 测试（需要一个模拟器或真机）：

```bash
./gradlew connectedDebugAndroidTest
```

## 目录结构

```
app/src/main/java/dev/androidstart/
├── AndroidStartApp.kt        Application + 组合根
├── MainActivity.kt           唯一 Activity，只做 setContent
├── di/AppContainer.kt        依赖图（现在是 1 个仓库）
├── data/
│   ├── TaskRepository.kt     契约：Flow 读、suspend 写
│   ├── InMemoryTaskRepository.kt
│   └── model/Task.kt         领域模型（≠ 数据库实体 ≠ 网络 DTO）
├── navigation/AppNavigation.kt   路由清单 + Nav3 宿主
├── feature/
│   ├── tasks/               列表页：ViewModel + Screen
│   └── taskdetail/          详情页：ViewModel + Screen
└── ui/theme/Theme.kt        唯一的主题入口
```

测试同构：`app/src/test/...`（JVM 单测）、`app/src/androidTest/...`（Compose UI 测试）。

## 已经替你做的决定（以及代价）

- **单模块**。一个 `:app`，按包分层。等某个 feature 的编译时间或团队边界真的疼了再拆模块——
  提前拆只会让每个改动横跨 4 个 `build.gradle.kts`。
- **手动 DI 而不是 Hilt**。代价是没有编译期作用域校验；收益是零注解处理、同步和构建都快。
  依赖数超过几十个、或者需要"登录态作用域"时再换（换法见 `AGENTS.md`）。
- **内存数据源**。所以现在**杀掉进程数据就没了**——这是刻意的：先让分层和测试成立，
  再换真数据源，一步只改一件事。
- **没有引 ktlint / detekt / spotless**。它们都要额外的插件依赖与版本兼容维护；
  当前的风格约束由 `.editorconfig` + 代码评审 + Android Lint 承担。
  真要上格式检查，加 `ktlint-gradle` 一条依赖即可，别同时上三个。
- **图标是纯矢量**（`res/mipmap-anydpi*`），仓库里没有二进制 PNG。
  要上架时用 Android Studio 的 *Image Asset* 生成各密度 PNG 即可。

## 接下来最常做的五件事

| 想做的事 | 改哪里 |
|---|---|
| 换成 SQLite 持久化 | 加 `data/local/`（Room 实体 + DAO）→ 写 `RoomTaskRepository` 实现 `TaskRepository` → 改 `AppContainer` 一行 |
| 接后端 | 加 `data/remote/`（Ktor client 或 Retrofit）+ DTO → mapper 转 `Task` → 组合进新的仓库实现 |
| 加一个页面 | `navigation/` 加路由 + `entryProvider` 加 `entry`，再写 `feature/xxx/` 的 ViewModel + Screen |
| 换 Hilt | 加 Hilt 依赖 → `@HiltAndroidApp` / `@AndroidEntryPoint` → 删 `AppContainer` → ViewModel 加 `@HiltViewModel` |
| 上架前 | 配签名（别把密钥写进仓库）、生成 PNG 图标、跑一次 `assembleRelease` 并装机冒烟 |

具体到"文件级"的施工规范看 [AGENTS.md](AGENTS.md)——那份文档同时是给人和给 AI 编码助手看的。

## 改名清单

把 `dev.androidstart` / `android-start` 换成你自己的，一共 5 处：

1. `settings.gradle.kts` 的 `rootProject.name`
2. `app/build.gradle.kts` 的 `namespace` 与 `applicationId`
3. `app/src/main/java/dev/androidstart/` 目录名与所有 `package` 声明
4. `app/src/main/res/values/strings.xml` 的 `app_name`
5. `app/src/main/res/values/colors.xml` 的图标底色（想换品牌色时）

改完跑 `./gradlew assembleDebug testDebugUnitTest lint`，绿了就是改干净了。

## 已知限制

- 没有 Network / 数据库层，数据不持久化（见上面"已经替你做的决定"）。
- 没有 Baseline Profile、没有 Macrobenchmark 模块。
- 没做平板/折叠屏的双栏布局：Navigation 3 的 Scenes API 是后续接入点。
- CI 里的设备测试是手动触发（模拟器启动慢，每次 PR 都跑性价比不高）。
