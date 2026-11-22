package com.easykebiao.app.widget

import com.easykebiao.app.data.entity.CourseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 「本周课表」桌面组件 ↔ 应用内课表 的**几何一致性**契约（2026-11-20 用户报障）。
 *
 * 用户报障原文：小组件里周二「高等数学 I」在应用内是 3-5 节，组件里 Y 轴偏上（疑似从
 * 第 1 节渲染）、跨节高度没撑满，文字被压成「综合实…」，圆角与透明度也和 App 不一致。
 *
 * 根因是组件侧有三套"自己一套"的换算，全部在这里钉死：
 *  - BUG-1 行数只取"课程末节次" → 13 节课表最后一节没课就只画 12 行（全表被拉高/漂移）；
 *  - BUG-2 行下标用 `startNode - 1` 硬减 → 节次节点号不连续时整列课块错位；
 *  - BUG-3 ownTime（真实分钟）课也按整格吸附 → 起点/高度与 App 的分数行映射对不上。
 *
 * JVM-safe：只测纯函数（位图绘制管线由编译 + 源码审查兜底，与
 * WidgetDegradationLadderTest 同口径）。
 */
class WeekGridGeometryParityTest {

    private companion object {
        /** 13 节课表（用户真机课表的形状：08:20 起、含 18:30/20:50 晚课） */
        const val TIME_JSON_13 = """[
            {"node":1,"start":"08:20","end":"09:00"},
            {"node":2,"start":"09:00","end":"09:40"},
            {"node":3,"start":"09:50","end":"10:30"},
            {"node":4,"start":"10:40","end":"11:20"},
            {"node":5,"start":"11:25","end":"12:05"},
            {"node":6,"start":"14:00","end":"14:40"},
            {"node":7,"start":"14:45","end":"15:25"},
            {"node":8,"start":"15:35","end":"16:15"},
            {"node":9,"start":"16:20","end":"17:00"},
            {"node":10,"start":"18:30","end":"19:10"},
            {"node":11,"start":"19:15","end":"19:55"},
            {"node":12,"start":"20:05","end":"20:45"},
            {"node":13,"start":"20:50","end":"21:30"}
        ]"""
    }

    private fun course(
        id: Long,
        day: Int,
        startNode: Int,
        step: Int,
        ownTime: Boolean = false,
        startTime: String = "",
        endTime: String = ""
    ) = CourseEntity(
        id = id, groupId = "g$id", tableId = 1L, courseName = "课$id",
        day = day, startNode = startNode, step = step,
        startWeek = 1, endWeek = 20, color = "blue",
        ownTime = ownTime, isIrregularTime = ownTime,
        startTime = startTime, endTime = endTime
    )

    private fun week(courses: List<CourseEntity>, timeJson: String = TIME_JSON_13) = WeekData(
        days = (1..7).map { dow ->
            DayData(
                date = LocalDate.now().plusDays((dow - LocalDate.now().dayOfWeek.value).toLong()),
                dayOfWeek = dow,
                courses = courses.filter { it.day == dow },
                timeJson = timeJson
            )
        },
        hasTable = true
    )

    // ── BUG-1: 行数必须与 App 同口径（= 课表全部节次），不能只看课程末节次 ──

    @Test
    fun `row count follows the period table not the last course`() {
        // 课程只用到第 12 节，课表有 13 节 → 组件必须仍是 13 行（App 也是 13 行）
        val data = week(listOf(course(1, 1, 3, 3), course(2, 6, 8, 1)))
        assertEquals(13, WeekGridWidgetProvider.weekGridMaxNode(data))
    }

    @Test
    fun `row count grows when a course exceeds the period table`() {
        // 脏数据: 课程末节次超出 timeJson → 仍要能画出来（取较大值, 不裁剪课程）
        val data = week(listOf(course(1, 1, 11, 4)))
        assertEquals(14, WeekGridWidgetProvider.weekGridMaxNode(data))
    }

    // ── BUG-2: 行下标按 node 查表, 不做 startNode-1 ──

    @Test
    fun `row index looks up the node instead of subtracting one`() {
        val nodes = listOf(1, 2, 4, 5, 9)   // 用户删过节点的课表
        assertEquals(0, WeekGridWidgetProvider.weekGridRowIndexOf(1, nodes))
        assertEquals(2, WeekGridWidgetProvider.weekGridRowIndexOf(4, nodes))
        assertEquals(4, WeekGridWidgetProvider.weekGridRowIndexOf(9, nodes))
        // 不在槽位表里的 node（数据脏）退回 node-1，绝不出现负数
        assertEquals(8, WeekGridWidgetProvider.weekGridRowIndexOf(9, listOf(1, 2, 4, 5)))
        assertEquals(0, WeekGridWidgetProvider.weekGridRowIndexOf(0, nodes))
    }

    @Test
    fun `contiguous thirteen node table maps node three to row index two`() {
        val nodes = (1..13).toList()
        assertEquals(2, WeekGridWidgetProvider.weekGridRowIndexOf(3, nodes))
        assertEquals(12, WeekGridWidgetProvider.weekGridRowIndexOf(13, nodes))
    }

    // ── BUG-3: 节次 → 像素的映射（单一事实来源）──

    @Test
    fun `period mapping is row height spacing plus one gap per period`() {
        val (top, h) = WeekGridWidgetProvider.weekGridBlockGeom(
            rowIndex = 2, step = 3, bodyTop = 100f, gapH = 4f, slotH = 40f
        )
        // 第 3 节顶端 = bodyTop + gap + 2 行
        assertEquals(100f + 4f + 2 * (40f + 4f), top, 0.001f)
        // 跨 3 节 = 3 个卡高 + 2 个空隙（块内无接缝）
        assertEquals(40f * 3 + 4f * 2, h, 0.001f)
    }

    @Test
    fun `irregular course maps by real minutes not by period grid`() {
        // 09:50-12:05 的课（ownTime）: 起始行 = 2.0（第 3 节），结束行 ≈ 5.0
        val (top, h) = WeekGridWidgetProvider.weekGridBlockGeom(
            rowIndex = 0, step = 1, bodyTop = 0f, gapH = 4f, slotH = 40f,
            frac = 2.0f to 5.0f
        )
        assertEquals(4f + 2f * 44f, top, 0.001f)
        assertEquals(3f * 44f - 4f, h, 0.001f)
    }

    @Test
    fun `irregular block never collapses below one readable row`() {
        // 5 分钟的极短课: 比例高度会趋近 0 → 保底一个 slotH, 否则课块不可见
        val (_, h) = WeekGridWidgetProvider.weekGridBlockGeom(
            rowIndex = 1, step = 1, bodyTop = 0f, gapH = 4f, slotH = 40f,
            frac = 1.0f to 1.02f
        )
        assertEquals(40f, h, 0.001f)
    }

    // ── 与 App 端 CourseTableView 的显式对照（同一课表, 两套算法必须给出同一行）──

    @Test
    fun `widget and app agree on the row of every course in a thirteen period week`() {
        val nodes = (1..13).toList()
        val slotH = 33f
        val gapH = 1.5f
        val bodyTop = 40f
        val courses = listOf(
            course(1, 2, 3, 3),   // 周二 高等数学 I: 3-5 节
            course(2, 4, 6, 4),   // 周四 自然地理学: 6-9 节
            course(3, 1, 1, 2)    // 周一 大学英语 I: 1-2 节
        )
        for (c in courses) {
            // App: slotIndexOf(node) = renderSlots.indexOfFirst { it.nodeStart == node }
            val appRow = nodes.indexOfFirst { it == c.startNode }
            // 组件: 同一张槽位表 → 必须同值
            val widgetRow = WeekGridWidgetProvider.weekGridRowIndexOf(c.startNode, nodes)
            assertEquals("node=${c.startNode} 行号必须一致", appRow, widgetRow)

            val (top, h) = WeekGridWidgetProvider.weekGridBlockGeom(
                rowIndex = widgetRow, step = c.step, bodyTop = bodyTop,
                gapH = gapH, slotH = slotH
            )
            assertEquals(bodyTop + gapH + appRow * (slotH + gapH), top, 0.001f)
            assertTrue("块高必须覆盖 step 节", h >= slotH * c.step)
        }
    }
}
