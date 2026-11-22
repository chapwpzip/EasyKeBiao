package com.easykebiao.app.data.ai

import java.time.LocalDate

/**
 * 「AI 整理」的产出：一条待确认的备忘草稿。
 *
 * 注意它**不是**实体 —— 只是弹窗里的预填内容，用户点"填入编辑框"才会变成真正的备忘。
 * 模型出错、网络断、没配 key，都不影响编辑框里用户自己写的东西。
 */
data class MemoDraft(
    val title: String,
    val content: String,
    /** 从文字里识别出的日期（"明天""11月25日"），识别不出就是 null。 */
    val dueDate: LocalDate? = null,
    /** 识别出的时刻（"下午3点""15:30"），识别不出就是 null。 */
    val time: String? = null,
    /** 来源：ai = 模型整理，local = 本地规则，用于在 UI 上如实标注。 */
    val source: Source = Source.LOCAL
) {
    enum class Source { AI, LOCAL }
}

/**
 * 本地规则解析 —— 没有 key 或模型失败时的兜底，也是"完全离线可用"的保证。
 *
 * 只做三件简单的事，不做 NLP：
 *  1. 首行当标题（超长截断），其余当正文；
 *  2. 找"今天/明天/后天/周X/MM月DD日/MM-DD"当日期；
 *  3. 找"HH:mm / H点"当时刻；行首是 -、•、1. 的当要点保留原样。
 */
object LocalMemoRules {

    private val datePatterns = listOf(
        Regex("""(\d{4})-(\d{1,2})-(\d{1,2})"""),
        Regex("""(\d{1,2})月(\d{1,2})[日号]"""),
        Regex("""(\d{1,2})-(\d{1,2})(?!\d)""")
    )
    private val timePattern = Regex("""(\d{1,2})[:：](\d{2})""")
    private val hourPattern = Regex("""(\d{1,2})\s*[点时]""")

    fun organize(raw: String, today: LocalDate = LocalDate.now()): MemoDraft {
        val text = raw.replace("\r", "").trim()
        if (text.isEmpty()) return MemoDraft("", "", source = MemoDraft.Source.LOCAL)

        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val firstLine = lines.firstOrNull().orEmpty()

        // 标题：首行；如果首行太长（>28 字）就取前 28 字，且整段都留在正文里
        val title = if (firstLine.length <= 28) firstLine else firstLine.take(28)
        val content = if (lines.size <= 1 && firstLine.length <= 28) {
            ""                                     // 只有一行短句，正文留空，用户自己补
        } else {
            lines.drop(if (firstLine.length <= 28) 1 else 0).joinToString("\n")
        }

        return MemoDraft(
            title = title,
            content = content.ifBlank { if (lines.size > 1) text else "" },
            dueDate = findDate(text, today),
            time = findTime(text),
            source = MemoDraft.Source.LOCAL
        )
    }

    internal fun findDate(text: String, today: LocalDate): LocalDate? {
        when {
            text.contains("后天") -> return today.plusDays(2)
            text.contains("明天") || text.contains("明日") -> return today.plusDays(1)
            text.contains("今天") || text.contains("今日") -> return today
        }
        datePatterns.forEach { p ->
            val m = p.find(text) ?: return@forEach
            val g = m.groupValues
            val date = when (g.size) {
                4 -> runCatching { LocalDate.of(g[1].toInt(), g[2].toInt(), g[3].toInt()) }.getOrNull()
                else -> runCatching {
                    val y = today.year
                    val d = LocalDate.of(y, g[1].toInt(), g[2].toInt())
                    // 已经过去很久的月日视为明年
                    if (d.plusDays(180).isBefore(today)) d.plusYears(1) else d
                }.getOrNull()
            }
            if (date != null) return date
        }
        return null
    }

    internal fun findTime(text: String): String? {
        timePattern.find(text)?.let { m ->
            val h = m.groupValues[1].toIntOrNull() ?: return@let
            val min = m.groupValues[2].toIntOrNull() ?: return@let
            if (h in 0..23 && min in 0..59) return String.format("%02d:%02d", h, min)
        }
        hourPattern.find(text)?.let { m ->
            val h = m.groupValues[1].toIntOrNull() ?: return@let
            if (h in 0..23) return String.format("%02d:00", h)
        }
        return null
    }
}
