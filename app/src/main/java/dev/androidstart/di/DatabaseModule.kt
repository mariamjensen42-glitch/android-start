package dev.androidstart.di

import android.content.Context
import androidx.room.Room
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
 *
 * 什么时候该把数据库拆成多实例：基本不用。真到了要隔离（例如离线缓存库、日志库）
 * 再说，那时再加 `@Named` 限定符。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AndroidStartDatabase = Room
        // 位置参数，不用命名参数：Room 是 Java 库，参数名不保证进字节码。
        .databaseBuilder(context, AndroidStartDatabase::class.java, DATABASE_NAME)
        // 起步阶段按"结构变了就重建"处理：还没有真实用户数据，写迁移是空转。
        // **上线前必须删掉这一行**，并补 Migration —— 否则升级会静默清空用户数据。
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    @Provides
    fun provideTaskDao(database: AndroidStartDatabase): TaskDao = database.taskDao()

    private const val DATABASE_NAME = "android-start.db"
}
