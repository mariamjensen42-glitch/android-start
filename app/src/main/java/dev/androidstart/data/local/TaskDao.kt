package dev.androidstart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO：SQL 只出现在这里。
 *
 * 两个约定：
 * - 读操作返回 [Flow]，Room 会在表变化时自动重新查询并推送，调用方不需要"刷新"。
 * - 写操作是 `suspend`，Room 自己保证不在主线程执行，不需要额外 `withContext`。
 */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<TaskEntity?>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
