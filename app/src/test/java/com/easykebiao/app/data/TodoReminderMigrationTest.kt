package com.easykebiao.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * v12 → v13：待办表加 `reminderAt`（闹钟提醒时刻）。
 *
 * 只做 ALTER TABLE ADD COLUMN，所以这里要盯三件事：
 * ① 新列类型是 `INTEGER` 且**可空**（Room 期望 Long? 与迁移 SQL 必须逐字对齐）；
 * ② 既有待办一行不少、内容不变，老数据的新列为 NULL；
 * ③ 原表的其它列定义没被重建打乱。
 */
class TodoReminderMigrationTest {

    /** 复刻 v12 时刻的 todos 表（与 MIGRATION_11_12_STATEMENTS 里那份逐字一致）。 */
    private fun createV12Schema(conn: Connection) {
        conn.createStatement().use { st ->
            st.execute(
                """
                CREATE TABLE todos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    dueDate INTEGER,
                    isCompleted INTEGER NOT NULL,
                    completedAt INTEGER,
                    isPinned INTEGER NOT NULL,
                    isFlagged INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            st.execute(
                """
                CREATE TABLE todo_steps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    todoId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    isDone INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    FOREIGN KEY(todoId) REFERENCES todos(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            st.execute("CREATE INDEX index_todo_steps_todoId ON todo_steps (todoId)")
            st.execute(
                "INSERT INTO todos (title, notes, dueDate, isCompleted, isPinned, isFlagged, sortOrder, createdAt, updatedAt) " +
                    "VALUES ('交实验报告', '周五前', 20324, 0, 1, 0, 0, 100, 200)"
            )
            st.execute("INSERT INTO todo_steps (todoId, title, isDone, sortOrder) VALUES (1, '写完', 0, 0)")
        }
    }

    private fun openInMemory(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @Test
    fun migration_adds_nullable_integer_reminder_column() {
        openInMemory().use { conn ->
            createV12Schema(conn)
            conn.createStatement().use { st -> MIGRATION_12_13_STATEMENTS.forEach(st::execute) }

            var type: String? = null
            var notNull = -1
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA table_info(todos)").use { rs ->
                    while (rs.next()) {
                        if (rs.getString("name") == "reminderAt") {
                            type = rs.getString("type")
                            notNull = rs.getInt("notnull")
                        }
                    }
                }
            }
            assertEquals("reminderAt must be INTEGER", "INTEGER", type)
            assertEquals("reminderAt must be nullable", 0, notNull)
        }
    }

    @Test
    fun migration_preserves_existing_todos_and_leaves_reminder_null() {
        openInMemory().use { conn ->
            createV12Schema(conn)
            conn.createStatement().use { st -> MIGRATION_12_13_STATEMENTS.forEach(st::execute) }

            conn.createStatement().use { st ->
                st.executeQuery("SELECT title, notes, isPinned, reminderAt FROM todos WHERE id = 1").use { rs ->
                    assertTrue("既有待办必须还在", rs.next())
                    assertEquals("交实验报告", rs.getString("title"))
                    assertEquals("周五前", rs.getString("notes"))
                    assertEquals(1, rs.getInt("isPinned"))
                    rs.getLong("reminderAt")
                    assertTrue("老数据的新列应为 NULL", rs.wasNull())
                }
                st.executeQuery("SELECT COUNT(*) FROM todo_steps").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun migration_keeps_other_columns_untouched() {
        openInMemory().use { conn ->
            createV12Schema(conn)
            conn.createStatement().use { st -> MIGRATION_12_13_STATEMENTS.forEach(st::execute) }

            val columns = mutableMapOf<String, Pair<String, Int>>()
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA table_info(todos)").use { rs ->
                    while (rs.next()) {
                        columns[rs.getString("name")] = rs.getString("type") to rs.getInt("notnull")
                    }
                }
            }
            assertEquals(12, columns.size)
            assertEquals("INTEGER" to 1, columns["id"])
            assertEquals("TEXT" to 1, columns["title"])
            assertEquals("INTEGER" to 0, columns["dueDate"])
            assertEquals("INTEGER" to 0, columns["completedAt"])
            assertNull(columns["bogusColumn"])
        }
    }
}
