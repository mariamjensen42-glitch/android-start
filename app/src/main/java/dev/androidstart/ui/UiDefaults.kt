package dev.androidstart.ui

/**
 * 跨屏幕共享的 UI 常量。
 *
 * 为什么单独放一处：`stateIn(WhileSubscribed(5000))` 里的 5000 是**策略**而不是语法糖——
 * 它决定"页面离开后多久停止收集数据源"，直接影响后台耗电与返回时的首帧。
 * 两个 ViewModel 各写一份时，改一处忘一处就会变成"列表停了、详情没停"这种难查的问题。
 */
internal const val STATE_STOP_TIMEOUT_MILLIS = 5_000L
