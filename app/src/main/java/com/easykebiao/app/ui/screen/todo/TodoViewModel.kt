package com.easykebiao.app.ui.screen.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easykebiao.app.SleepyApp
import com.easykebiao.app.data.entity.TodoEntity
import com.easykebiao.app.data.entity.TodoStepEntity
import com.easykebiao.app.widget.TodoWidgetProvider
import com.easykebiao.app.widget.notification.TodoReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 待办的唯一状态源。字段少、操作就那几个：
 * 保存(新建/编辑，连带步骤与闹钟) / 勾完成 / 置顶 / 删除。
 */
class TodoViewModel : ViewModel() {

    private val dao = SleepyApp.get().database.todoDao()

    val todos: StateFlow<List<TodoEntity>> = dao.observeTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** todoId → 它的步骤（按 sortOrder 拍好）。 */
    val stepsByTodo: StateFlow<Map<Long, List<TodoStepEntity>>> = dao.observeSteps()
        .map { all -> all.groupBy { it.todoId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun save(
        existing: TodoEntity?,
        title: String,
        notes: String,
        dueDate: Long?,
        reminderAt: Long?,
        flagged: Boolean,
        imagePath: String? = null,
        category: String = "todo",
        priority: Int = 0,
        place: String = "",
        pinned: Boolean,
        steps: List<Pair<String, Boolean>>
    ) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val saved = if (existing == null) {
                TodoEntity(
                    title = title,
                    notes = notes,
                    dueDate = dueDate,
                    reminderAt = reminderAt,
                    imagePath = imagePath,
                    category = category,
                    priority = priority,
                    place = place,
                    isFlagged = flagged,
                    isPinned = pinned,
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                existing.copy(
                    title = title,
                    notes = notes,
                    dueDate = dueDate,
                    reminderAt = reminderAt,
                    imagePath = imagePath,
                    category = category,
                    priority = priority,
                    place = place,
                    isFlagged = flagged,
                    isPinned = pinned,
                    updatedAt = now
                )
            }
            val id = dao.upsert(saved)
            dao.clearSteps(id)
            val cleaned = steps.filter { it.first.isNotBlank() }
            if (cleaned.isNotEmpty()) {
                dao.insertSteps(
                    cleaned.mapIndexed { index, (text, done) ->
                        TodoStepEntity(todoId = id, title = text.trim(), isDone = done, sortOrder = index)
                    }
                )
            }
            scheduleReminder(dao.getById(id) ?: saved.copy(id = id))
            refreshWidget()
        }
    }

    fun toggleCompleted(todo: TodoEntity) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val completed = !todo.isCompleted
            dao.update(
                todo.copy(
                    isCompleted = completed,
                    completedAt = if (completed) now else null,
                    updatedAt = now
                )
            )
            // 完成就撤掉闹钟，别隔天还响；取消完成则按原时刻重新登记（已过期就不响）
            if (completed) {
                TodoReminderScheduler.cancel(SleepyApp.get(), todo.id)
                TodoReminderScheduler.dismissNotification(SleepyApp.get(), todo.id)
            } else {
                scheduleReminder(todo.copy(isCompleted = false))
            }
            refreshWidget()
        }
    }

    fun setPinned(todo: TodoEntity, pinned: Boolean) {
        viewModelScope.launch {
            dao.update(todo.copy(isPinned = pinned, updatedAt = System.currentTimeMillis()))
            refreshWidget()
        }
    }

    fun toggleStep(step: TodoStepEntity) {
        viewModelScope.launch { dao.setStepDone(step.id, !step.isDone) }
    }

    fun delete(todo: TodoEntity) {
        viewModelScope.launch {
            dao.delete(todo)
            TodoReminderScheduler.cancel(SleepyApp.get(), todo.id)
            TodoReminderScheduler.dismissNotification(SleepyApp.get(), todo.id)
            refreshWidget()
        }
    }

    private fun scheduleReminder(todo: TodoEntity) {
        runCatching { TodoReminderScheduler.schedule(SleepyApp.get(), todo) }
    }

    /** 数据一变就推一次桌面组件（没放置组件时内部直接返回）。 */
    private fun refreshWidget() {
        runCatching { TodoWidgetProvider.refresh(SleepyApp.get()) }
    }
}
