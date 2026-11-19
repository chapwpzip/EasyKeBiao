package com.imsx3d.classy.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户手写备忘录。
 *
 * 与课表数据完全独立：不引用 time_tables / courses，只借用同一个数据库文件
 * （sleepy.db），因此导入、切表、删表都不会影响它。列表排序见 [com.imsx3d.classy.data.dao.MemoDao.observeAll]
 * —— 置顶优先，其次按最近修改。
 */
@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "content") val content: String = "",
    @ColumnInfo(name = "pinned") val pinned: Boolean = false,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
)
