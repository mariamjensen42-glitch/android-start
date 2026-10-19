package dev.androidstart.data

import dev.androidstart.data.model.Task
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * 内存实现：起步工程的默认数据源，也是所有测试的默认假实现。
 *
 * 它**故意不是**玩具：读写都是线程安全的（[MutableStateFlow.update] 内部是 CAS 循环），
 * 语义与真实实现一致，所以 ViewModel 的单测可以一直用它，不需要 mock。
 *
 * 换成 Room 时保留这个类——它是"没有数据库也能跑"的保底路径，
 * 也是 instrumentation 测试里注入内存数据的入口。
 */
class InMemoryTaskRepository(
    initialTasks: List<Task> = emptyList(),
) : TaskRepository {

    private val tasks = MutableStateFlow(initialTasks)
    private val nextId = AtomicLong((initialTasks.maxOfOrNull { it.id } ?: 0L) + 1L)

    override fun observeTasks(): Flow<List<Task>> = tasks.asStateFlow()

    override fun observeTask(id: Long): Flow<Task?> =
        tasks.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun add(title: String): Long {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "任务标题不能为空" }

        val id = nextId.getAndIncrement()
        tasks.update { current -> current + Task(id = id, title = trimmed) }
        return id
    }

    override suspend fun setDone(id: Long, done: Boolean) {
        tasks.update { current ->
            current.map { task -> if (task.id == id) task.copy(done = done) else task }
        }
    }

    override suspend fun delete(id: Long) {
        tasks.update { current -> current.filterNot { it.id == id } }
    }
}
