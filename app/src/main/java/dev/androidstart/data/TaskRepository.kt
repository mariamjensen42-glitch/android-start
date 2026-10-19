package dev.androidstart.data

import dev.androidstart.data.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * 任务仓库的**契约**。
 *
 * 写法约定（这三条决定了以后换实现要不要动 UI）：
 * 1. 读一律返回 [Flow]，不返回一次性快照——数据变了 UI 自己重组，不需要"手动刷新"。
 * 2. 写一律是 `suspend`，调用方在协程里等结果，不做回调。
 * 3. 不抛异常给 UI 层靠 try/catch 兜：能预期的失败（网络、磁盘）在实现里转成
 *    领域结果；只有程序员错误（参数非法）才 `require`/`check` 直接崩。
 */
interface TaskRepository {

    /** 全部任务，按创建顺序。 */
    fun observeTasks(): Flow<List<Task>>

    /** 单个任务；被删掉之后会发出 `null`。 */
    fun observeTask(id: Long): Flow<Task?>

    /** 新建，返回新任务 id。空标题属于非法参数。 */
    suspend fun add(title: String): Long

    /** 改完成状态。id 不存在时静默忽略（例如两个页面同时操作）。 */
    suspend fun setDone(id: Long, done: Boolean)

    /** 删除。id 不存在时静默忽略。 */
    suspend fun delete(id: Long)
}
