package dev.androidstart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 数据库定义。加表 / 加字段：改这里和实体 → 提高 version → 写 Migration。
 *
 * `exportSchema = true` 是刻意的：schema JSON 会落在 `app/schemas/`，
 * 它让"忘了写迁移"变成编译期错误，而不是线上崩溃。
 */
@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AndroidStartDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
