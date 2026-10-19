package dev.androidstart.benchmarks

/**
 * 被测应用的包名，**唯一**一处。
 *
 * 改包名（applicationId）时只要改这里，两个测试都会跟着走。
 * 它和 `app/build.gradle.kts` 里的 `applicationId` 不一致时，
 * 报错会很难懂（"target package not found"），所以宁可显式写在这里。
 */
internal const val TARGET_PACKAGE = "dev.androidstart"
