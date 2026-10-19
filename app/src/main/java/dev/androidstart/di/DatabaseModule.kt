package dev.androidstart.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import androidx.room.migration.Migration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.androidstart.data.local.AndroidStartDatabase
import dev.androidstart.data.local.TaskDao
import javax.inject.Singleton

/**
 * 数据库的提供方式。
 *
 * 为什么数据库要 `@Singleton`：`RoomDatabase` 内部持有连接池，重复创建等于开了多个
 * 连接池，既费内存又会让 WAL 锁互相等。整个进程一份，这是硬要求。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AndroidStartDatabase {
        val builder = Room
            // 位置参数，不用命名参数：Room 是 Java 库，参数名不保证进字节码。
            .databaseBuilder(context, AndroidStartDatabase::class.java, DATABASE_NAME)
            .addMigrations(*MIGRATIONS)

        // 只允许**可调试包**在结构变化时重建库：开发期改表结构很频繁，不重建就得天天手写迁移。
        // 发布包走这里会抛异常（Room 默认行为），也就是"响亮地失败"——
        // 静默清空用户数据是比崩溃严重得多的故障：崩溃会有人报，数据没了不会。
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debuggable) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
        }

        return builder.build()
    }

    @Provides
    fun provideTaskDao(database: AndroidStartDatabase): TaskDao = database.taskDao()

    /**
     * 迁移清单。**唯一**登记处：加了 `Migration(1, 2)` 但忘了加到这里，
     * 发布包升级时会直接抛异常（这是故意的，比静默出错好）。
     *
     * 现在为空是因为还在 v1；第一次改表结构时这里会变成 `arrayOf(MIGRATION_1_2)`。
     */
    private val MIGRATIONS: Array<Migration> = emptyArray()

    private const val DATABASE_NAME = "android-start.db"
}
