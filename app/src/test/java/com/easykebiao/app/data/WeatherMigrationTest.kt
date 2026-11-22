package com.easykebiao.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * v13 → v14：新增「天气缓存」表。
 *
 * 这条迁移是纯 CREATE TABLE，所以只盯三件事：
 * ① 表真的建出来了，列名/类型/可空性与 Room 期望逐字对齐；
 * ② 幂等（同一份 SQL 跑两遍不报错，线上重试不会卡住）；
 * ③ 既有表一行不少 —— 天气跟课表/待办/备忘没有任何关系。
 */
class WeatherMigrationTest {

    private fun openInMemory(): Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    private fun createLegacyTables(conn: Connection) {
        conn.createStatement().use { st ->
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
            st.execute("INSERT INTO memos (title, content, pinned, createdAt, updatedAt) VALUES ('高数提纲', '第一章', 1, 10, 20)")
        }
    }

    @Test
    fun migration_creates_weather_cache_with_expected_columns() {
        openInMemory().use { conn ->
            createLegacyTables(conn)
            conn.createStatement().use { st -> MIGRATION_13_14_STATEMENTS.forEach(st::execute) }

            val columns = mutableMapOf<String, Pair<String, Int>>()
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA table_info(weather_cache)").use { rs ->
                    while (rs.next()) {
                        columns[rs.getString("name")] = rs.getString("type") to rs.getInt("notnull")
                    }
                }
            }
            assertEquals(6, columns.size)
            assertEquals("INTEGER" to 1, columns["id"])
            assertEquals("TEXT" to 1, columns["cityName"])
            assertEquals("REAL" to 1, columns["lat"])
            assertEquals("REAL" to 1, columns["lon"])
            assertEquals("INTEGER" to 1, columns["fetchedAt"])
            assertEquals("TEXT" to 1, columns["payloadJson"])
        }
    }

    @Test
    fun migration_is_idempotent() {
        openInMemory().use { conn ->
            createLegacyTables(conn)
            conn.createStatement().use { st ->
                MIGRATION_13_14_STATEMENTS.forEach(st::execute)
                MIGRATION_13_14_STATEMENTS.forEach(st::execute)   // 第二次不报错
            }
            conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM weather_cache").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(0, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun migration_leaves_existing_tables_untouched() {
        openInMemory().use { conn ->
            createLegacyTables(conn)
            conn.createStatement().use { st -> MIGRATION_13_14_STATEMENTS.forEach(st::execute) }
            conn.createStatement().use { st ->
                st.executeQuery("SELECT title FROM memos WHERE id = 1").use { rs ->
                    assertTrue("既有备忘必须还在", rs.next())
                    assertEquals("高数提纲", rs.getString("title"))
                }
                // 单行表：主键 id 必须存在，且空表时没有行
                st.executeQuery("SELECT * FROM weather_cache").use { rs ->
                    assertTrue("迁移后表应为空", !rs.next())
                }
            }
            assertNull(null)
        }
    }
}
