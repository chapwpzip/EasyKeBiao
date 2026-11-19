package com.imsx3d.classy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.imsx3d.classy.data.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    /** 置顶优先，其次按最近修改 —— 用户翻备忘录总是先看刚写的那条。 */
    @Query("SELECT * FROM memos ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MemoEntity?

    /** 返回自增 id —— 新建时调用方不需要自己算主键。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memo: MemoEntity): Long

    @Query("UPDATE memos SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteById(id: Long)
}
