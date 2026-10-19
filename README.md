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
| 依赖注入 | **Hilt 2.59.2**（KSP，不用 kapt） | 生产项目的事实标准；作用域、测试替换都有标准解法 |
| 持久化 | **Room 2.8.5**（KSP + 导出 schema） | 编译期校验 SQL 与结构变更，忘了写迁移会编译失败 |
| 测试 | JUnit4 + `kotlinx-coroutines-test` + Turbine + Compose UI Test + Room DAO 仪器测试 | 单测纯 JVM 几百毫秒；SQL 与 UI 交互各有对应的层去测 |
| 性能 | Baseline Profile + Macrobenchmark，都在 `:benchmarks` 模块（`androidx.baselineprofile` / `benchmark-macro-junit4` **1.5.0**） | 首帧收益明确；生成与测量同一模块，手动触发，不拖累日常构建 |
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

跑设备上的测试（Compose UI 测试 + Room DAO 测试，需要一个模拟器或真机）：

```bash
./gradlew connectedDebugAndroidTest
```

## 性能：Baseline Profile 与 Macrobenchmark

两件事都在 `:benchmarks` 模块里，且**都要设备**，所以都不进日常构建。

```bash
# 1. 生成 baseline profile（自己起一台 GMD 模拟器，不用插真机）
./gradlew :app:generateBaselineProfile
#    产物：app/src/release/generated/baselineProfiles/baseline-prof.txt —— **要提交**

# 2. 量冷启动（Partial = 有 profile，None = 无 profile，两者差值就是收益）
./gradlew :benchmarks:connectedReleaseAndroidTest
```

**什么时候必须重跑生成**：首屏相关的代码变了（主题、DI 图、首屏 Compose 树、首屏数据源）。
不重跑不会报错，只会让 profile 悄悄失效——收益衰减是静默的，这是这套东西唯一真正会"烂掉"的地方。

两个刻意的决定：

- **`automaticGenerationDuringBuild = false`**。否则 `assembleRelease` 会要求一台设备，
  "我只是想打个包"变成"先起模拟器"。生成只在你明确要它时发生。
- **CI 里只跑生成、不跑 macrobenchmark**。CI 的虚拟化设备数字噪声大，看趋势得用真机；
  把噪声放进 CI 只会让人学会忽略它。那条 workflow 是手动触发的（`workflow_dispatch`）。

## 目录结构

```
app/src/main/java/dev/androidstart/
├── AndroidStartApp.kt        @HiltAndroidApp，依赖图入口
├── MainActivity.kt           唯一 Activity，@AndroidEntryPoint，只做 setContent
├── di/
│   ├── DatabaseModule.kt     Room 数据库与 DAO 的提供方式
│   └── RepositoryModule.kt   接口 → 实现（全应用唯一一处选实现的地方）
├── data/
│   ├── TaskRepository.kt     契约：Flow 读、suspend 写
│   ├── InMemoryTaskRepository.kt  测试与预览用的内存实现
│   ├── model/Task.kt         领域模型（≠ 数据库实体 ≠ 网络 DTO）
│   └── local/
│       ├── TaskEntity.kt     @Entity
│       ├── TaskDao.kt        SQL 只出现在这里
│       ├── RoomTaskRepository.kt  实现 TaskRepository + 实体→领域映射
│       └── AndroidStartDatabase.kt
├── navigation/AppNavigation.kt   路由清单 + Nav3 宿主（唯一调用 hiltViewModel 的地方）
├── feature/
│   ├── tasks/               列表页：@HiltViewModel + Screen
│   └── taskdetail/          详情页：@HiltViewModel + assisted factory + Screen
└── ui/theme/Theme.kt        唯一的主题入口

app/schemas/                 Room 结构快照，**要提交**

benchmarks/                  性能模块（com.android.test，不产出可安装的 App）
├── BaselineProfileGenerator.kt   走关键路径，采集 baseline-prof.txt
└── StartupBenchmarks.kt          量冷启动：Partial（有 profile）vs None（无 profile）
```

测试同构：`app/src/test/...`（JVM 单测）、`app/src/androidTest/...`（Compose UI + DAO 测试）。

## 已经替你做的决定（以及代价）

- **单模块**。一个 `:app`，按包分层。等某个 feature 的编译时间或团队边界真的疼了再拆模块——
  提前拆只会让每个改动横跨 4 个 `build.gradle.kts`。
- **Hilt 但不上 `@HiltAndroidTest`**。现在没有需要替换依赖的仪器测试；等真的需要
  （比如注入假仓库跑端到端）再加 `hilt-android-testing` 和 `HiltTestApplication`，
  别现在就把测试基础设施铺开。
- **详情页走 assisted injection 传 `taskId`**，而不是 `SavedStateHandle.get("taskId")`。
  代价是多一个 `@AssistedFactory` 接口；收益是参数类型安全，不会出现"key 拼错只在运行时炸"。
- **数据库 `fallbackToDestructiveMigration`**。**上线前必须删掉这一行并补 Migration**，
  否则一次版本升级会静默清空用户数据。这是给"还没有真实用户"阶段留的方便。
- **release 用 debug 签名**。这样 clone 下来不用任何密钥就能装 release 变体，
  baseline profile 生成与 macrobenchmark 也都跑在它上面。
  **上架前必须换成正式签名**（见下面的清单）——否则你会发一个谁都签得出的包。
- **Baseline Profile 不自动生成**，见上一节。模板里带它是为了"该做的事已经接好线"，
  而不是让它变成构建的一环。
- **没有引 ktlint / detekt / spotless**。它们都要额外的插件依赖与版本兼容维护；
  当前的风格约束由 `.editorconfig` + 代码评审 + Android Lint 承担。
  真要上格式检查，加 `ktlint-gradle` 一条依赖即可，别同时上三个。
- **图标是纯矢量**（`res/mipmap-anydpi*`），仓库里没有二进制 PNG。
  要上架时用 Android Studio 的 *Image Asset* 生成各密度 PNG 即可。

## 接下来最常做的五件事

| 想做的事 | 改哪里 |
|---|---|
| 改表结构（加字段/加表） | 改 `TaskEntity` → `AndroidStartDatabase` 的 version +1 → 写 `Migration` → 构建（Room 会拿 `app/schemas/` 里的旧快照校验） |
| 换成网络数据源 | 加 `data/remote/`（Ktor/Retrofit + DTO + mapper）→ 写实现 `TaskRepository` 的离线优先仓库 → 改 `RepositoryModule` 一行 |
| 加一个页面 | `navigation/` 加路由 + `entry`，再写 `feature/xxx/` 的 `@HiltViewModel` + Screen |
| 给仪器测试加假的依赖 | 加 `hilt-android-testing` + `@HiltAndroidTest` + `HiltTestApplication` |
| 首屏改了想验证收益 | `./gradlew :app:generateBaselineProfile` 重新生成，再跑 `:benchmarks:connectedReleaseAndroidTest` 对比 Partial/None |
| 上架前 | 删掉 `fallbackToDestructiveMigration`、**把 release 换成正式签名**、配签名（密钥别进仓库）、生成 PNG 图标、跑 `assembleRelease` 并装机冒烟 |

具体到"文件级"的施工规范看 [AGENTS.md](AGENTS.md)——那份文档同时是给人和给 AI 编码助手看的。

## 版本这条链别乱动

`AGP → Kotlin → KSP → Hilt/Room` 是互相咬合的，`gradle/libs.versions.toml` 顶部有完整说明。三个要点：

- **Hilt 停在 2.59.2**：2.59 是第一个支持 AGP 9 的 Hilt 插件版本（2.58 及以前在 AGP 9 上直接报错）；
  2.60.x 把 `kotlin-stdlib` 下限抬到 2.3.21，会顶掉 Kotlin 2.3.20 → KSP 2.3.12 就配不上。
- **KSP 必须与 Kotlin 对齐**：KSP 2.3.12 的 pom 依赖 `kotlin-stdlib 2.3.20`。
- **`androidx.baselineprofile` 与 `benchmark-macro-junit4` 同一个版本号**（1.5.0），
  它们由同一个 androidx 项目发布；只升其中一个会出现插件与运行时不匹配。

## 改名清单

把 `dev.androidstart` / `android-start` 换成你自己的，一共 7 处：

1. `settings.gradle.kts` 的 `rootProject.name`
2. `app/build.gradle.kts` 的 `namespace` 与 `applicationId`
3. `benchmarks/build.gradle.kts` 的 `namespace`，以及 `benchmarks/.../TargetApp.kt` 的 `TARGET_PACKAGE`
4. `app/src/main/java/dev/androidstart/` 目录名与所有 `package` 声明
5. `app/src/main/res/values/strings.xml` 的 `app_name`
6. `app/src/main/res/values/colors.xml` 的图标底色（想换品牌色时）
7. `di/DatabaseModule.kt` 里的数据库文件名（如果要彻底换名）

改完跑 `./gradlew assembleDebug testDebugUnitTest lint`，绿了就是改干净了。
（`TARGET_PACKAGE` 与实际 `applicationId` 不一致时，性能测试会报"target package not found"。）

## 已知限制

- 没有 Network 层，也没有离线缓存策略（只有本地 Room）。
- Baseline Profile 里目前只覆盖到"首屏渲染完"，没有走新建/详情等二级路径——等真实功能进来再补。
- macrobenchmark 只有冷启动一项指标，没有帧率（`FrameTimingMetric`）与列表滑动场景。
- 没做平板/折叠屏的双栏布局：Navigation 3 的 Scenes API 是后续接入点。
- CI 里的设备测试与 profile 生成都是手动触发（模拟器启动慢，每次 PR 都跑性价比不高）。
