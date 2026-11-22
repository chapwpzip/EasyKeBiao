package com.easykebiao.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * v14 → v15：待办加「附图 / 分类 / 优先级 / 地点」，备忘加「分类 / 优先级 / 地点」。
 *
 * 全是 ADD COLUMN，所以只盯三件事：
 * ① 新列的类型与可空性和 Room 期望逐字对齐（待办 imagePath 可空，其余带默认值）；
 * ② 既有行不丢、新列的默认值符合预期（老的待办自动落到「待办」分类、优先级 0）；
 * ③ 原表的其他列定义没被重建打乱。
 */
class TodoImageMigrationTest {

    private fun openInMemory(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    private fun createV14Schema(conn: Connection) {
        conn.createStatement().use { st ->
            st.execute(
                """
                CREATE TABLE todos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    dueDate INTEGER,
                    reminderAt INTEGER,
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
                CREATE TABLE memos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    pinned INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            st.execute(
                "INSERT INTO todos (title, notes, isCompleted, isPinned, isFlagged, sortOrder, createdAt, updatedAt) " +
                    "VALUES ('交实验报告', '周五前', 0, 1, 0, 0, 100, 100)"
            )
            st.execute("INSERT INTO memos (title, content, pinned, createdAt, updatedAt) VALUES ('高数提纲', '第一章', 0, 10, 20)")
        }
    }

    private fun columns(conn: Connection, table: String): Map<String, Triple<String, Int, String?>> {
        val out = mutableMapOf<String, Triple<String, Int, String?>>()
        conn.createStatement().use { st ->
            st.executeQuery("PRAGMA table_info($table)").use { rs ->
                while (rs.next()) {
                    out[rs.getString("name")] = Triple(
                        rs.getString("type"),
                        rs.getInt("notnull"),
                        rs.getString("dflt_value")
                    )
                }
            }
        }
        return out
    }

    @Test
    fun migration_adds_todo_columns_with_expected_types() {
        openInMemory().use { conn ->
            createV14Schema(conn)
            conn.createStatement().use { st -> MIGRATION_14_15_STATEMENTS.forEach(st::execute) }

            val cols = columns(conn, "todos")
            assertEquals("TEXT" to 0, cols.getValue("imagePath").let { it.first to it.second })
            assertEquals("TEXT" to 1, cols.getValue("category").let { it.first to it.second })
            assertEquals("INTEGER" to 1, cols.getValue("priority").let { it.first to it.second })
            assertEquals("TEXT" to 1, cols.getValue("place").let { it.first to it.second })
        }
    }

    @Test
    fun migration_adds_memo_columns_with_expected_types() {
        openInMemory().use { conn ->
            createV14Schema(conn)
            conn.createStatement().use { st -> MIGRATION_14_15_STATEMENTS.forEach(st::execute) }

            val cols = columns(conn, "memos")
            assertEquals("TEXT" to 1, cols.getValue("category").let { it.first to it.second })
            assertEquals("INTEGER" to 1, cols.getValue("priority").let { it.first to it.second })
            assertEquals("TEXT" to 1, cols.getValue("place").let { it.first to it.second })
        }
    }

    @Test
    fun migration_keeps_existing_rows_and_applies_defaults() {
        openInMemory().use { conn ->
            createV14Schema(conn)
            conn.createStatement().use { st -> MIGRATION_14_15_STATEMENTS.forEach(st::execute) }

            conn.createStatement().use { st ->
                st.executeQuery("SELECT title, imagePath, category, priority, place FROM todos WHERE id = 1").use { rs ->
                    assertTrue("既有待办必须还在", rs.next())
                    assertEquals("交实验报告", rs.getString("title"))
                    assertNull("没附图的旧待办这一列应为 NULL", rs.getString("imagePath"))
                    assertEquals("todo", rs.getString("category"))
                    assertEquals(0, rs.getInt("priority"))
                    assertEquals("", rs.getString("place"))
                }
                st.executeQuery("SELECT title, category, priority, place FROM memos WHERE id = 1").use { rs ->
                    assertTrue("既有备忘必须还在", rs.next())
                    assertEquals("高数提纲", rs.getString("title"))
                    assertEquals("other", rs.getString("category"))
                    assertEquals(0, rs.getInt("priority"))
                    assertEquals("", rs.getString("place"))
                }
            }
        }
    }

    @Test
    fun migration_keeps_other_columns_untouched() {
        openInMemory().use { conn ->
            createV14Schema(conn)
            conn.createStatement().use { st -> MIGRATION_14_15_STATEMENTS.forEach(st::execute) }

            val cols = columns(conn, "todos")
            assertEquals(16, cols.size)
            // reminderAt 可空（没设提醒就是 NULL），updatedAt 必填 —— 迁移不该动它们
            assertEquals("INTEGER" to 0, cols.getValue("reminderAt").let { it.first to it.second })
            assertEquals("INTEGER" to 1, cols.getValue("updatedAt").let { it.first to it.second })
        }
    }
}
