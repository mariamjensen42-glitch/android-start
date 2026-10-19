package dev.androidstart.data.local

import androidx.room.withTransaction
import dev.androidstart.data.TaskRepository
import dev.androidstart.data.model.Task
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room 实现。它只做三件事：调 DAO、把实体映射成领域模型、把多个写操作包进事务。
 *
 * `@Inject constructor` + `@Singleton` 让 Hilt 能直接把它绑到 [TaskRepository] 上
 * （绑定写在 `di/RepositoryModule.kt`）——不需要为它单独写一个 @Provides。
 */
@Singleton
class RoomTaskRepository @Inject constructor(
    private val database: AndroidStartDatabase,
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
        // 现在只有一张表，事务看起来多余；但它是"以后加关联表时不用回来改这里"的保险。
        // 一旦这里变成多语句写（比如同时删子任务），没有 withTransaction 就会出现半个状态。
        database.withTransaction { dao.deleteById(id) }
    }
}

// --- 映射 ------------------------------------------------------------------
// 映射函数放在数据层，UI 层拿到的永远是领域模型。

private fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    done = done,
)
