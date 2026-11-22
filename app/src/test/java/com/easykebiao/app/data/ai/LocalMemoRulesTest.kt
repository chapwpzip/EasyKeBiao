package com.easykebiao.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 本地规则兜底 —— 没配 key / 模型抽风时，用户仍然能拿到"整理过的东西"。
 *
 * 口径：宁可不猜，也不要瞎猜。识别不出的字段一律 null，交给用户自己补。
 */
class LocalMemoRulesTest {

    private val today = LocalDate.of(2026, 11, 22)

    @Test
    fun first_short_line_becomes_title_and_rest_becomes_content() {
        val draft = LocalMemoRules.organize("测绘学概论实验安排\n实验三 全站仪导线测量，综合实验中心315\n分组：第4组", today)
        assertEquals("测绘学概论实验安排", draft.title)
        assertTrue(draft.content.contains("全站仪"))
        assertEquals(MemoDraft.Source.LOCAL, draft.source)
    }

    @Test
    fun single_short_line_leaves_content_empty() {
        val draft = LocalMemoRules.organize("取快递", today)
        assertEquals("取快递", draft.title)
        assertEquals("", draft.content)
    }

    @Test
    fun long_first_line_is_truncated_for_title_but_kept_in_content() {
        val long = "明天下午三点在综合实验中心三楼开班会，请各位同学带上笔记本与校园卡准时参加，迟到者需向辅导员说明原因"
        val draft = LocalMemoRules.organize(long, today)
        assertTrue("标题要截断", draft.title.length <= 28)
        assertTrue("正文保留全文", draft.content.length >= long.length - 30)
    }

    @Test
    fun relative_dates_are_resolved_against_today() {
        assertEquals(today.plusDays(1), LocalMemoRules.findDate("明天交报告", today))
        assertEquals(today.plusDays(2), LocalMemoRules.findDate("后天考试", today))
        assertEquals(today, LocalMemoRules.findDate("今天截止", today))
        assertEquals(LocalDate.of(2026, 12, 5), LocalMemoRules.findDate("12月5日交实验报告", today))
        assertEquals(LocalDate.of(2026, 11, 30), LocalMemoRules.findDate("11-30 前提交", today))
        assertNull("没有日期线索就不猜", LocalMemoRules.findDate("复习第三章", today))
    }

    @Test
    fun times_are_normalised() {
        assertEquals("15:30", LocalMemoRules.findTime("15:30 开会"))
        assertEquals("15:00", LocalMemoRules.findTime("下午 15点 开会"))
        assertEquals("09:00", LocalMemoRules.findTime("9点起床"))
        assertNull(LocalMemoRules.findTime("随便什么时候"))
    }

    @Test
    fun blank_input_yields_blank_draft_without_crashing() {
        val draft = LocalMemoRules.organize("   ", today)
        assertEquals("", draft.title)
        assertEquals("", draft.content)
    }

    @Test
    fun ai_json_is_parsed_and_falls_back_to_rules_for_missing_fields() {
        val raw = """
            ```json
            {"title":"高数期中复习","content":"第一章 极限与连续\n第二章 导数","dueDate":null,"time":null}
            ```
        """.trimIndent()
        val draft = AiMemoClient.parseContent(raw, today)
        assertEquals("高数期中复习", draft!!.title)
        assertEquals(MemoDraft.Source.AI, draft.source)
        assertNull("模型说 null 就是 null", draft.dueDate)
    }

    @Test
    fun non_json_model_output_is_rejected() {
        assertNull(AiMemoClient.parseContent("抱歉，我无法完成这个请求。", today))
        assertNull(AiMemoClient.parseContent("{\"content\":\"没有标题\"}", today))
    }

    @Test
    fun endpoint_normalisation_covers_common_base_urls() {
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            AiMemoClient.endpoint("https://api.deepseek.com")
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            AiMemoClient.endpoint("https://api.deepseek.com/v1/")
        )
        assertEquals(
            "https://x.test/chat/completions",
            AiMemoClient.endpoint("https://x.test/chat/completions")
        )
    }
}
