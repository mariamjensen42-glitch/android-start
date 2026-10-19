pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 依赖仓库只在这里声明。模块里再写 repositories {} 会直接构建失败——
    // 这样"某个模块偷偷用了别的仓库"这种问题不可能发生。
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "android-start"
include(":app")
// 性能模块：生成 Baseline Profile + 跑 macrobenchmark。不产出可安装的 App，
// 只在手动触发时才需要设备（见 README 的《性能》一节）。
include(":benchmarks")
