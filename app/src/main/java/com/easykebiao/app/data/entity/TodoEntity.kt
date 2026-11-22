package com.easykebiao.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待办（备忘录栏目里的「待办」段）。
 *
 * 字段刻意保持精简 —— 只留真的会用到的那几个，避免变成另一个 Cresto：
 * 标题 / 备注 / 截止日 / 完成 / 置顶 / 重要标记 / 提醒时刻 / 排序 / 时间戳。
 *
 * [dueDate] 存 epochDay（LocalDate.toEpochDay()），只有日期粒度、不带时刻；
 * [reminderAt] 是「闹钟」用的绝对时刻（epoch millis，可空），与 [dueDate] 独立 ——
 * 没设截止日也可以设提醒。两者都不写 defaultValue、不加索引，这样 Room 期望的建表 / 加列
 * 与 [com.easykebiao.app.data.MIGRATION_11_12_STATEMENTS] / [com.easykebiao.app.data.MIGRATION_12_13_STATEMENTS]
 * 能逐字对上，运行时迁移校验才不会炸。
 */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "notes") val notes: String = "",
    @ColumnInfo(name = "dueDate") val dueDate: Long? = null,
    @ColumnInfo(name = "reminderAt") val reminderAt: Long? = null,
    /** 附图文件名（0.15.0）：图片本体在 filesDir/memo_images 下，这里只存名字 */
    @ColumnInfo(name = "imagePath") val imagePath: String? = null,
    /** 分类：todo / activity / exam / homework / other */
    @ColumnInfo(name = "category", defaultValue = "todo") val category: String = "todo",
    /** 优先级：0 无 / 1 低 / 2 中 / 3 高 */
    @ColumnInfo(name = "priority", defaultValue = "0") val priority: Int = 0,
    /** 地点（自由文本，可空串） */
    @ColumnInfo(name = "place", defaultValue = "") val place: String = "",
    @ColumnInfo(name = "isCompleted") val isCompleted: Boolean = false,
    @ColumnInfo(name = "completedAt") val completedAt: Long? = null,
    @ColumnInfo(name = "isPinned") val isPinned: Boolean = false,
    @ColumnInfo(name = "isFlagged") val isFlagged: Boolean = false,
    @ColumnInfo(name = "sortOrder") val sortOrder: Int = 0,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long
)
