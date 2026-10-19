package dev.androidstart.data

import dev.androidstart.data.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 仓库层的单测：纯 JVM，不需要设备，几百毫秒跑完。
 *
 * 这一层值得写测试的原因不是"覆盖率好看"，而是仓库的语义（id 递增、trim、
 * 删不存在的 id 不炸）会被上层依赖，改坏了是静默的。
 */
class InMemoryTaskRepositoryTest {

    @Test
    fun `add trims the title and returns increasing ids`() = runTest {
        val repository = InMemoryTaskRepository()

        val firstId = repository.add("  写测试  ")
        val secondId = repository.add("再写一个")

        assertTrue("后加的任务 id 应该更大", secondId > firstId)
        assertEquals(
            listOf("写测试", "再写一个"),
            repository.observeTasks().first().map { it.title },
        )
    }

    @Test
    fun `add rejects a blank title`() {
        val repository = InMemoryTaskRepository()

        // 这个断言不需要 runTest：runBlocking 只是把挂起调用拉直好放进 assertThrows。
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.add("   ") }
        }
    }

    @Test
    fun `setDone only touches the target task`() = runTest {
        val repository = InMemoryTaskRepository(
            listOf(Task(id = 1, title = "a"), Task(id = 2, title = "b")),
        )

        repository.setDone(id = 2, done = true)

        val tasks = repository.observeTasks().first()
        assertEquals(false, tasks.first { it.id == 1L }.done)
        assertEquals(true, tasks.first { it.id == 2L }.done)
    }

    @Test
    fun `setDone on a missing id is a no-op`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 1, title = "a")))

        repository.setDone(id = 99, done = true)

        assertEquals(listOf(1L), repository.observeTasks().first().map { it.id })
    }

    @Test
    fun `delete removes the task and observeTask emits null`() = runTest {
        val repository = InMemoryTaskRepository(
            listOf(Task(id = 1, title = "a"), Task(id = 2, title = "b")),
        )

        repository.delete(id = 1)

        assertNull(repository.observeTask(1).first())
        assertEquals(listOf(2L), repository.observeTasks().first().map { it.id })
    }

    @Test
    fun `new ids continue after the seeded tasks`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = 7, title = "已有的")))

        assertEquals(8L, repository.add("新的"))
    }
}
