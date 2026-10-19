package dev.androidstart

import android.app.Application
import dev.androidstart.di.AppContainer

/**
 * 应用级组合根。
 *
 * 依赖图只有一处创建点（这里），其余代码一律通过构造参数拿依赖。
 * 这样"谁依赖谁"在代码里看得见，测试里换成假实现也不需要任何魔法。
 *
 * 什么时候该换成 Hilt：依赖超过几十个、出现作用域需求（登录态、按功能分模块）时。
 * 换的时候只改 AppContainer 和调用点，ViewModel 的构造签名不用动——这是刻意的设计。
 */
class AndroidStartApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
