package dev.androidstart

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用级组合根。
 *
 * `@HiltAndroidApp` 会生成 `Hilt_AndroidStartApp`，它替你建立整个依赖图。
 * 所以这个类里**不需要写任何装配代码**——依赖在哪提供，看 `di/` 下的两个模块：
 * - `DatabaseModule`：Room 数据库与 DAO
 * - `RepositoryModule`：接口 → 实现（全应用唯一一处"选实现"的地方）
 */
@HiltAndroidApp
class AndroidStartApp : Application()
