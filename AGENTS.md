# AGENTS.md

给**人和 AI 编码助手**共用的施工规范。改动这个仓库之前先读它；
读完还拿不准的，按"保守、可逆、小步"的原则做，并在 PR 描述里写清假设。

---

## 1. 30 秒速查

```bash
./gradlew assembleDebug             # 编译
./gradlew testDebugUnitTest         # JVM 单测（秒级）
./gradlew lint                      # Android Lint（abortOnError=true）
./gradlew connectedDebugAndroidTest # 设备 UI 测试（需模拟器/真机）
./gradlew check                     # 上面那些的合集
```

**Definition of Done**：`./gradlew assembleDebug testDebugUnitTest lint` 三条全绿，
且新增行为有对应的测试。没有测试的新逻辑要么补上，要么在 PR 里说明为什么测不了。

---

## 2. 五条铁律

1. **UI 只画状态，不问数据。**
   `feature/*/XxxScreen.kt` 里不允许出现 `viewModel()`、`Repository`、`Context` 取数据、
   协程 `launch`。Screen 的签名永远是 `(uiState, on事件...)`。
   这样 Preview 和 Compose UI 测试才能不依赖 DI 地渲染它。

2. **依赖从构造函数进来。**
   需要什么在构造参数里写出来，不要在函数体里 `AppContainer.xxx` 或 `application as XxxApp`。
   唯一的例外是 `MainActivity`（组合根，从 `application` 取容器）和 `AndroidStartApp`（创建容器）。

3. **状态只有一个出口。**
   每屏一个 `data class XxxUiState`，由 `StateFlow` 暴露，UI 用 `collectAsStateWithLifecycle()` 收。
   不要在 Composable 里 `remember` 业务状态，也不要让 UI 自己推断"加载中还是空"——那属于 UiState。

4. **读用 Flow，写用 suspend。**
   仓库接口不接受回调，不返回一次性快照给 UI。数据变了 UI 自己重组，没有"手动刷新"这种东西。

5. **先问真实现能不能当假实现。**
   测试优先用 `InMemoryTaskRepository` 这类真实的内存实现。要引 mock 框架之前先说服自己
   为什么真实现不够——多数时候是够的。

---

## 3. 代码地图：改什么去哪个文件

| 要改的东西 | 唯一该动的地方 |
|---|---|
| 依赖版本 | `gradle/libs.versions.toml`（构建脚本里出现硬编码版本号就是错） |
| 新增依赖 | 先加到 catalog，再在 `app/build.gradle.kts` 引用 |
| 配色 / 字体 / 圆角 | `ui/theme/Theme.kt` |
| 用户可见文案 | `res/values/strings.xml`（Compose 里用 `stringResource`，禁止硬编码中文/英文） |
| 图标 | `res/drawable/*.xml`（矢量）或 `res/mipmap-anydpi*`（自适应图标） |
| 页面清单 / 传参 | `navigation/AppNavigation.kt` |
| 业务规则 | 对应 feature 的 `ViewModel` |
| 数据怎么存 / 从哪来 | `data/` 下的实现类 + `di/AppContainer.kt` 一行 |
| 权限、Activity 配置 | `app/src/main/AndroidManifest.xml` |
| 构建开关（minify、lint、compileSdk） | `app/build.gradle.kts` |

**一处修改只散落在一处**——这是这个仓库最想守住的性质。如果你发现改一个东西要动 3 个文件，
先停下来想清楚是不是抽象放错了位置，而不是硬改。

---

## 4. 分层与依赖方向

```
feature/*  ──▶  data (接口)  ◀──  data/*Repository 实现
    │                                  ▲
    └──▶  ui/theme                    di/AppContainer（唯一装配点）
```

允许：
- `feature` → `data`（接口）、`ui.theme`、`navigation` 里的路由定义
- `data` → `data.model`、`java.util.concurrent`、`kotlinx.coroutines`
- `di` → `data`

禁止：
- `data` → `feature` / `ui`（数据层不认识 UI）
- `feature` 之间互相 import（列表页不许直接调详情页的 ViewModel；要共享就下沉到 `data` 或新的 `core` 包）
- 任何包 → `di.AppContainer`，除 `MainActivity` 与 `AndroidStartApp`
- `feature/*Screen.kt` 里出现 `androidx.lifecycle.viewmodel.compose.viewModel`

判断方法：删掉整个 `feature` 目录，`data` 还能编译吗？应该能。

---

## 5. 标准施工流程

### 加一个页面
1. `navigation/AppNavigation.kt` 加 `@Serializable data class XxxRoute(...) : NavKey`
2. 同文件 `entryProvider` 里加 `entry<XxxRoute> { route -> ... }`，用 `viewModel(factory = XxxViewModel.factory(...))` 拿 ViewModel
3. 新建 `feature/xxx/XxxViewModel.kt`：`XxxUiState` + `StateFlow` + 事件函数 + `companion object { fun factory(...) }`
4. 新建 `feature/xxx/XxxScreen.kt`：只接 `uiState` 和 lambda；文件末尾至少一个 `@Preview`
5. 测试：`XxxViewModelTest`（用内存仓库）；Screen 有一个 UI 测试覆盖关键交互

### 加数据源（例如 Room）
1. `data/local/` 加实体、DAO、数据库类（需要 KSP 时在 catalog 加 `ksp` 插件，版本必须与 Kotlin 对齐）
2. 写 `RoomTaskRepository : TaskRepository`，**接口一行不改**
3. `AppContainer` 里把 `InMemoryTaskRepository()` 换成它
4. 保留内存实现：它是测试与"数据层挂掉时的保底路径"
5. 跑 `./gradlew testDebugUnitTest lint`；Room 的 schema 导出目录要跟着提交

### 加权限 / 系统能力
1. Manifest 声明权限
2. **必须**处理"用户拒绝"的路径（`ActivityResultContracts` + 拒绝后的降级 UI），不能只写成功分支
3. 在 `strings.xml` 补一句"为什么需要"的用户文案
4. 在本文件第 3 节的表格里补一行"这类改动改哪里"

### 拆模块
触发条件（满足任意一条再拆）：单模块编译 > 60s、两个 feature 由不同人长期并行、需要按 feature 动态交付。
拆法：`feature/xxx` → `:feature:xxx`，依赖方向仍然是 feature → core/data，禁止 feature 之间互相依赖。
`build-logic/` 里抽 convention plugin，**不要**在每个模块复制粘贴 40 行 `android {}`。

---

## 6. 代码风格（可判定，不靠感觉）

- 缩进 4 空格，行宽 ≤ 120，见 `.editorconfig`。文件末尾留一个换行。
- Kotlin 官方风格；`kotlin.code.style=official` 已设。
- 类名名词，函数名动词短语；`Boolean` 属性用 `is`/`has` 前缀（`isLoading`、`hasError`）。
- 一个 Composable 一个职责。超过 ~120 行的 Composable 拆成私有 Composable，**不要**拆成工具函数。
- Composable 私有成员放文件末尾，顺序：公开 Composable → 私有 Composable → Preview。
- 所有面向用户的字符串走 `stringResource`；`contentDescription` 必须给（纯装饰才用 `null`）。
- 日志：只在失败路径打，且带可搜的关键字。不要 `println`。
- 注释解释**为什么**，不解释**是什么**。看到"// 设置 title"这种就删掉。
- 错误处理：能预期的失败（IO、网络）在数据层转成领域结果；程序员错误（非法参数）用 `require`/`check` 直接崩。
  **不要**写空的 `catch {}`。

---

## 7. 测试规范

- **单测（`src/test`）**：ViewModel 的状态编排、仓库语义、纯函数。必须纯 JVM，不许 Robolectric。
- **UI 测试（`src/androidTest`）**：只测"改了容易静默坏掉"的交互。不为了覆盖率去点每一个按钮。
- 命名：`` `when something then expectation` `` 反引号可读句子，测的就是这句话。
- 每个断言都要有失败时能看懂的信息（`assertEquals("任务标题", expected, actual)`）。
- 测试里不 sleep、不用真延迟。需要控制时间就用 `runTest` 的虚拟时钟。

---

## 8. 版本与升级

- 唯一版本来源：`gradle/libs.versions.toml`。升级顺序：
  `agp` → `kotlin`（Kotlin / KSP / AGP 三者互相约束）→ `composeBom` → 其余。
- AGP 9 起 Kotlin 编译内建在 AGP 里：**不要**再应用 `org.jetbrains.kotlin.android`，
  `kotlinOptions {}` 已废弃，改用 `kotlin { compilerOptions { } }`。
- `minifyEnabled` / `shrinkResources` 也已废弃，改用 `optimization { enable = true }`。
- 升完必须：`./gradlew assembleDebug testDebugUnitTest lint`，再打一次 `assembleRelease` 并装机冒烟
  （R8 会把 `@Serializable` 路由、反射入口这类问题留到运行时才炸）。
- 每次升级单独一个提交，写清升了什么、为什么升、验证结果。不要混在功能提交里。

---

## 9. 禁止清单

- ❌ 在 Composable 里直接读仓库 / 起协程 / 拿 `Context` 做业务
- ❌ 在构建脚本里写死依赖版本（一律走 catalog）
- ❌ 第二个 `Activity`（页面切换一律走 Nav3 路由）
- ❌ `GlobalScope`、`runBlocking`（测试里除外）、`Thread.sleep`
- ❌ 空的 `catch {}`、`catch (e: Exception) { }`
- ❌ 把密钥、签名文件、`local.properties` 提交进仓库
- ❌ 为了"以后可能要用"提前加抽象（多一层 interface 之前先问：现在有两个实现吗）
- ❌ 同时引进两个做同一件事的库（两个 DI 框架、两个图片库、两个 JSON 库）
- ❌ 改完不跑 lint 就提交

---

## 10. 交付前检查清单

- [ ] `./gradlew assembleDebug testDebugUnitTest lint` 全绿
- [ ] 新增的 Composable 有 `@Preview`，且在深色模式下看过
- [ ] 新文案在 `strings.xml`，没有硬编码
- [ ] 新增行为有测试；测试名说清了"在什么情况下期望什么"
- [ ] 没引入新的依赖，或新依赖已经写进 catalog 并在 PR 里说明了理由
- [ ] `git diff` 里没有无关的格式化噪音
- [ ] 如果改了分层/约定，这份 AGENTS.md 同步更新了
