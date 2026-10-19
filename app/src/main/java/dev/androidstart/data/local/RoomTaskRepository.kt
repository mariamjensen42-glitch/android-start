package dev.androidstart.data.local

import dev.androidstart.data.TaskRepository
import dev.androidstart.data.model.Task
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room 实现。它只做两件事：调 DAO、把实体映射成领域模型。
 *
 * `@Inject constructor` + `@Singleton` 让 Hilt 能直接把它绑到 [TaskRepository] 上
 * （绑定写在 `di/RepositoryModule.kt`）——不需要为它单独写一个 @Provides。
 *
 * 关于映射的线程：`map` 在收集者上下文执行，也就是主线程。现在每行只做一次对象拷贝，
 * 比"切到 IO 再切回来"便宜；等映射变重（解析、排序大列表）再加 `flowOn(Dispatchers.Default)`，
 * 别现在就为没有的负载付调度开销。
 */
@Singleton
class RoomTaskRepository @Inject constructor(
    private val dao: TaskDao,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        dao.observeAll().map { entities -> entities.map(TaskEntity::toDomain) }

    override fun observeTask(id: Long): Flow<Task?> =
        dao.observeById(id).map { entity -> entity?.toDomain() }

    override suspend fun add(title: String): Long {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "任务标题不能为空" }
        return dao.insert(TaskEntity(title = trimmed))
    }

    override suspend fun setDone(id: Long, done: Boolean) {
        dao.setDone(id = id, done = done)
    }

    override suspend fun delete(id: Long) {
        // 单语句不需要 withTransaction：它只多一次线程跳转。
        // 以后改成多表写（比如同时删子任务）时必须包起来，那时再加。
        dao.deleteById(id)
    }
}

// --- 映射 ------------------------------------------------------------------
// 映射函数放在数据层，UI 层拿到的永远是领域模型。

private fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    done = done,
)
