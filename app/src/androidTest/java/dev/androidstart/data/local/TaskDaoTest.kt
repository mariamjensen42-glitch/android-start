package dev.androidstart.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO 的仪器测试：跑在真机/模拟器上，用**内存数据库**（进程结束即销毁，不污染设备）。
 *
 * 为什么不用 Robolectric 跑在 JVM 上：那是在模拟 SQLite，测的是"模拟器像不像 SQLite"，
 * 而不是"我们的 SQL 对不对"。DAO 这一层只有一条真 SQLite 才值得测。
 *
 * 这里**故意**只测三件事：自增 id、`ORDER BY` 真的生效、按 id 的写操作只影响那一行。
 * 其他都是 Room 自身的行为，测了也拦不住我们的 bug。
 */
@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: AndroidStartDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AndroidStartDatabase::class.java,
        ).build()
        dao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_assignsIncreasingIds_andListIsOrdered() = runTest {
        val first = dao.insert(TaskEntity(title = "先建的", createdAt = 1_000L))
        val second = dao.insert(TaskEntity(title = "后建的", createdAt = 2_000L))

        assertEquals(true, second > first)
        assertEquals(
            listOf("先建的", "后建的"),
            dao.observeAll().first().map { it.title },
        )
    }

    @Test
    fun setDone_onlyTouchesTheTargetRow() = runTest {
        val a = dao.insert(TaskEntity(title = "a", createdAt = 1L))
        val b = dao.insert(TaskEntity(title = "b", createdAt = 2L))

        dao.setDone(id = b, done = true)

        val rows = dao.observeAll().first()
        assertEquals(false, rows.first { it.id == a }.done)
        assertEquals(true, rows.first { it.id == b }.done)
    }

    @Test
    fun deleteById_removesOnlyThatRow() = runTest {
        val a = dao.insert(TaskEntity(title = "留下的", createdAt = 1L))
        val b = dao.insert(TaskEntity(title = "删掉的", createdAt = 2L))

        dao.deleteById(b)

        assertNull(dao.observeById(b).first())
        assertEquals(listOf(a), dao.observeAll().first().map { it.id })
    }
}
