plugins {
    alias(libs.plugins.android.application)
    // Kotlin 编译由 AGP 9 内建，这里只需要 Compose 编译器与序列化两个插件。
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // Room 与 Hilt 的代码生成都走 KSP。
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Room 的结构快照导出目录。
 *
 * 导出的 `schemas/<version>.json` 是**迁移的对照物**：改了实体（表结构）之后，
 * Room 拿旧快照跟新结构比，缺迁移就在**编译期**报错，而不是等用户装上之后崩在运行时。
 * 所以 `app/schemas/` 要跟着代码一起提交，不要 gitignore。
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "dev.androidstart"

    // AGP 9 的写法：compileSdk 用块，支持 minor 版本。
    // 37.1 是下限而不是偏好：Compose BOM 2026.09 里的 ui-test 要求 minCompileSdk=37。
    // targetSdk 仍留在 36——"用新 SDK 编译"和"声明适配新行为"是两件事，分开推进。
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.androidstart"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // AGP 9 用 optimization 取代 isMinifyEnabled / isShrinkResources。
            // 打 release 包前务必装机冒烟一次：R8 之后二进制与源码不同形，
            // @Serializable 路由、Room 的生成代码、Hilt 的注入点最容易在这里翻车。
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // 用 Kotlin 2.x 内建的 Compose 编译器时，只留这一个开关。
    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // 警告不拦构建，错误必须拦：CI 里 lint 挂了就是真的有错。
        abortOnError = true
        warningsAsErrors = false
        // 只检查本模块源码，不递归检查依赖库（慢，且不是我们能修的）。
        checkDependencies = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    // 让每个 NavEntry 拿到独立的 ViewModelStore，详情页才能"一个 key 一个 ViewModel"。
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Hilt：运行时 + KSP 编译器 + Nav3 场景下的 hiltViewModel()。
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Room：运行时 + Flow 支持 + KSP 编译器。
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.serialization.json)

    // Compose 预览/检查工具只进 debug，不污染 release 体积。
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Room 的 DAO 测试要跑协程（in-memory 数据库 + Flow）。
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
