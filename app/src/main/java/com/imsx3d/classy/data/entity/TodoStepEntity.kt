package com.imsx3d.classy.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 待办下的步骤（Cresto 叫 SubTodo）。父待办删除时级联删掉。
 *
 * [todoId] 上必须有索引 —— 否则 Room 会给一条 "foreign key without index" 警告，
 * 而且级联删除在 Android 上要先扫全表。
 */
@Entity(
    tableName = "todo_steps",
    foreignKeys = [
        ForeignKey(
            entity = TodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["todoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("todoId")]
)
data class TodoStepEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "todoId") val todoId: Long,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "isDone") val isDone: Boolean = false,
    @ColumnInfo(name = "sortOrder") val sortOrder: Int = 0
)
