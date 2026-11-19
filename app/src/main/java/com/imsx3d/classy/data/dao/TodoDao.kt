package com.imsx3d.classy.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.imsx3d.classy.data.entity.TodoEntity
import com.imsx3d.classy.data.entity.TodoStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    /**
     * 全量取回，分组排序交给上层（列表很小，SQL 里塞一堆 CASE 反而更难读）：
     * 置顶 → 重要 → 未完成 → 截止日近的在前。
     */
    @Query("SELECT * FROM todos ORDER BY isPinned DESC, isFlagged DESC, isCompleted ASC, dueDate ASC, createdAt DESC")
    fun observeTodos(): Flow<List<TodoEntity>>

    /** 步骤一次全取（量小），上层按 todoId 分组。 */
    @Query("SELECT * FROM todo_steps ORDER BY sortOrder ASC, id ASC")
    fun observeSteps(): Flow<List<TodoStepEntity>>

    /** 组件用的同步一次读（位图渲染在后台线程，Room 的 suspend 会自己切线程）。 */
    @Query("SELECT * FROM todos ORDER BY isPinned DESC, isFlagged DESC, isCompleted ASC, dueDate ASC, createdAt DESC")
    suspend fun allOnce(): List<TodoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(todo: TodoEntity): Long

    /** 闹钟响、组件勾选、开机重排都要按 id 单独取一条。 */
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): TodoEntity?

    /** 开机后重排闹钟用：只关心还没完成、且设了提醒的。 */
    @Query("SELECT * FROM todos WHERE reminderAt IS NOT NULL AND isCompleted = 0")
    suspend fun pendingRemindersOnce(): List<TodoEntity>

    @Query("UPDATE todos SET isCompleted = :done, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, done: Boolean, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE todos SET reminderAt = :reminderAt WHERE id = :id")
    suspend fun setReminderAt(id: Long, reminderAt: Long?)

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Insert
    suspend fun insertSteps(steps: List<TodoStepEntity>)

    @Query("DELETE FROM todo_steps WHERE todoId = :todoId")
    suspend fun clearSteps(todoId: Long)

    @Query("UPDATE todo_steps SET isDone = :done WHERE id = :id")
    suspend fun setStepDone(id: Long, done: Boolean)
}
