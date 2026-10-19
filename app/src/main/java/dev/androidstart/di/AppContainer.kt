package dev.androidstart.di

import dev.androidstart.data.InMemoryTaskRepository
import dev.androidstart.data.TaskRepository

/**
 * 手动依赖注入容器：一个进程一份，跟着 [dev.androidstart.AndroidStartApp] 活。
 *
 * 规则：
 * - 只暴露**接口**（[TaskRepository]），实现类不出容器。以后换成网络/数据库实现，
 *   调用点一行都不用改。
 * - 构造函数里不做 IO。所有耗时初始化都放到首次使用的位置（或明确的 warmup），
 *   否则冷启动会替所有人付出代价。
 */
class AppContainer {

    /**
     * 演示用的内存实现，进程结束数据就没了。
     *
     * 换成 Room / DataStore / 远端 API 的顺序见 AGENTS.md 的《换数据源》一节：
     * 先加实现，再改这一行，测试不用动。
     */
    val taskRepository: TaskRepository = InMemoryTaskRepository()
}
