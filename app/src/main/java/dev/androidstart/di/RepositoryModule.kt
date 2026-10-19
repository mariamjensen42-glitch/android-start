package dev.androidstart.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.androidstart.data.TaskRepository
import dev.androidstart.data.local.RoomTaskRepository
import javax.inject.Singleton

/**
 * 仓库绑定：把"接口"接到"实现"上。
 *
 * 这就是**全应用唯一**一处"选哪个实现"的地方。换成网络仓库、缓存优先仓库，
 * 都只改这一个方法，ViewModel 和 UI 一行不动。
 *
 * 用 `@Binds` 而不是 `@Provides`：实现类自己有 `@Inject constructor` 时，
 * `@Binds` 只是登记一句"要接口就给这个类"，不产生任何工厂代码。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: RoomTaskRepository): TaskRepository
}
