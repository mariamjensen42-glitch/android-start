package dev.androidstart.data.model

/**
 * 领域模型。注意它**不是**持久化实体，也不是网络 DTO：
 * - 数据库实体（Room Entity）加 @Entity 后放在 data/local 里；
 * - 接口返回的 JSON 结构放在 data/remote 里；
 * 两者都通过 mapper 转成这个类型，UI 层永远只认它。
 *
 * 这样换后端、换表结构时，改的是 mapper，不是整个 App。
 */
data class Task(
    val id: Long,
    val title: String,
    val done: Boolean = false,
)
