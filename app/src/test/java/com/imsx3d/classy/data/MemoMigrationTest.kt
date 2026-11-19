package com.imsx3d.classy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * v10 → v11 memos schema and data-preservation contract.
 *
 * 备忘录是纯新增表, 迁移只允许 CREATE TABLE — 一旦带上 INDEX/默认值而 Entity 没声明,
 * Room 打开库时的 identityHash/schema 校验就会直接抛 IllegalStateException,
 * 用户看到的是"课表全没了"。所以这里既验列形态, 也验"没有多余索引"。
 */
class MemoMigrationTest {

    private fun createV10Schema(conn: Connection) {
        conn.createStatement().use { st ->
            st.execute(
                """
                CREATE TABLE time_tables (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    startDate TEXT NOT NULL,
                    maxWeek INTEGER NOT NULL DEFAULT 20,
                    nodesPerDay INTEGER NOT NULL DEFAULT 12,
                    timeJson TEXT NOT NULL,
                    color TEXT NOT NULL DEFAULT '#FF6750A4',
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    smartConfigJson TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            st.execute("INSERT INTO time_tables (name, startDate, timeJson, createdAt) VALUES ('旧课表', '2026-09-01', '{}', 11)")
        }
    }

    private fun openInMemory(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @Test
    fun migration_creates_memo_table_with_required_columns() {
        openInMemory().use { conn ->
            createV10Schema(conn)
            conn.createStatement().use { st -> MIGRATION_10_11_STATEMENTS.forEach(st::execute) }

            val columns = mutableMapOf<String, Pair<String, Int>>()
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA table_info(memos)").use { rs ->
                    while (rs.next()) {
                        columns[rs.getString("name")] = rs.getString("type") to rs.getInt("notnull")
                    }
                }
            }
            assertEquals("memos 必须有 id 列", "INTEGER", columns["id"]?.first)
            assertEquals("id 必须 NOT NULL(主键)", 1, columns["id"]?.second)
            assertEquals("TEXT", columns["title"]?.first)
            assertEquals(1, columns["title"]?.second)
            assertEquals("TEXT", columns["content"]?.first)
            assertEquals(1, columns["content"]?.second)
            assertEquals("INTEGER", columns["pinned"]?.first)
            assertEquals(1, columns["pinned"]?.second)
            assertEquals("INTEGER", columns["createdAt"]?.first)
            assertEquals(1, columns["createdAt"]?.second)
            assertEquals("INTEGER", columns["updatedAt"]?.first)
            assertEquals(1, columns["updatedAt"]?.second)
            assertEquals("memos 列数固定为 6", 6, columns.size)
            assertNotNull(columns["id"])
        }
    }

    @Test
    fun migration_creates_no_index_room_does_not_expect() {
        openInMemory().use { conn ->
            createV10Schema(conn)
            conn.createStatement().use { st -> MIGRATION_10_11_STATEMENTS.forEach(st::execute) }
            val indices = mutableListOf<String>()
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA index_list(memos)").use { rs ->
                    while (rs.next()) indices += rs.getString("name")
                }
            }
            assertEquals(
                "memos 不得有 Room Entity 未声明的索引(否则 schema 校验失败, 库打不开)",
                emptyList<String>(),
                indices
            )
        }
    }

    @Test
    fun migration_preserves_existing_timetable_rows() {
        openInMemory().use { conn ->
            createV10Schema(conn)
            conn.createStatement().use { st -> MIGRATION_10_11_STATEMENTS.forEach(st::execute) }
            conn.createStatement().use { st ->
                st.executeQuery("SELECT name FROM time_tables WHERE id = 1").use { rs ->
                    assertTrue(rs.next())
                    assertEquals("旧课表", rs.getString("name"))
                }
            }
        }
    }

    @Test
    fun migration_is_idempotent_and_accepts_memo_insert() {
        openInMemory().use { conn ->
            createV10Schema(conn)
            // 重复执行必须是空操作(CREATE TABLE IF NOT EXISTS), 否则迁移重试会把已升级的用户打挂
            conn.createStatement().use { st -> MIGRATION_10_11_STATEMENTS.forEach(st::execute) }
            conn.createStatement().use { st -> MIGRATION_10_11_STATEMENTS.forEach(st::execute) }
            conn.createStatement().use { st ->
                st.execute("INSERT INTO memos (title, content, pinned, createdAt, updatedAt) VALUES ('第一条', '正文', 1, 100, 100)")
                st.executeQuery("SELECT id, pinned FROM memos").use { rs ->
                    assertTrue(rs.next())
                    assertEquals("自增主键应从 1 开始", 1L, rs.getLong("id"))
                    assertEquals(1, rs.getInt("pinned"))
                }
            }
        }
    }
}
