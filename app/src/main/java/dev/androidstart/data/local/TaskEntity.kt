package dev.androidstart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 数据库实体。**只在这一层存在**——不要让 [androidx.room.Entity] 的类型漏到
 * ViewModel 或 UI（那就是把"数据怎么存"焊死进了表现层）。
 *
 * 加字段的流程：改这里 → 提高 `AndroidStartDatabase` 的 version → 写 Migration
 * → 跑构建让 Room 拿 `app/schemas/` 里的旧快照做校验（缺迁移会在编译期报错）。
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val done: Boolean = false,
    /**
     * 创建时间，用来给列表排序。**必须有确定的排序键**：
     * 没有 ORDER BY 的查询在 SQLite 里返回顺序不保证，UI 上表现为"任务偶尔乱跳"。
     */
    val createdAt: Long = System.currentTimeMillis(),
)
