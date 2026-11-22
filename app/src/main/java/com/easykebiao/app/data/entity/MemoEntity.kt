package com.easykebiao.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户手写备忘录。
 *
 * 与课表数据完全独立：不引用 time_tables / courses，只借用同一个数据库文件
 * （sleepy.db），因此导入、切表、删表都不会影响它。列表排序见 [com.easykebiao.app.data.dao.MemoDao.observeAll]
 * —— 置顶优先，其次按最近修改。
 */
@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "content") val content: String = "",
    @ColumnInfo(name = "pinned") val pinned: Boolean = false,
    /** 分类：todo / activity / exam / homework / other（与待办同一套色标） */
    @ColumnInfo(name = "category", defaultValue = "other") val category: String = "other",
    /** 优先级：0 无 / 1 低 / 2 中 / 3 高 */
    @ColumnInfo(name = "priority", defaultValue = "0") val priority: Int = 0,
    /** 地点（自由文本，可空串） */
    @ColumnInfo(name = "place", defaultValue = "") val place: String = "",
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
)
