package com.imsx3d.classy.ui.screen.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imsx3d.classy.SleepyApp
import com.imsx3d.classy.data.entity.MemoEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 备忘录的唯一状态持有者。依赖直接取自 [SleepyApp]（全 App 无 Hilt），
 * 数据只在 Room 里，页面不需要任何本地缓存。
 */
class MemoViewModel : ViewModel() {

    private val dao = SleepyApp.get().database.memoDao()

    val memos: StateFlow<List<MemoEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** memoId 为 null = 新建；否则覆盖既有行。返回受影响的行数（新建为自增 id）。 */
    fun save(memoId: Long?, title: String, content: String, pinned: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = memoId?.let { dao.getById(it) }
            dao.upsert(
                MemoEntity(
                    id = existing?.id ?: 0L,
                    title = title,
                    content = content,
                    pinned = pinned,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            )
        }
    }

    fun setPinned(memo: MemoEntity, pinned: Boolean) {
        viewModelScope.launch { dao.setPinned(memo.id, pinned) }
    }

    fun delete(memo: MemoEntity) {
        viewModelScope.launch { dao.deleteById(memo.id) }
    }
}
