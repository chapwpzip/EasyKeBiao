package com.easykebiao.app.data.ai

import android.content.Context
import com.easykebiao.app.util.AppPrefs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * 「AI 整理」客户端 —— 走 OpenAI 兼容的 /chat/completions，key 由用户自己在设置页填。
 *
 * 三条硬规矩：
 *  1. **只发用户主动粘贴的那段文字**，不发课表、不发其他备忘；
 *  2. 返回必须是严格 JSON，解析不出来就当失败，交给本地规则兜底；
 *  3. 超时 15 秒、不重试（用户能再点一次，比默默排队好）。
 */
object AiMemoClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    internal fun endpoint(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        return when {
            base.endsWith("/chat/completions") -> base
            base.endsWith("/v1") -> "$base/chat/completions"
            else -> "$base/v1/chat/completions"
        }
    }

    internal fun buildBody(model: String, text: String, today: LocalDate): String {
        val system = """
            你是一个备忘整理助手。把用户给的一段话整理成一条备忘。
            只输出 JSON，不要 Markdown 代码块，不要解释。字段：
            {"title": "不超过 24 字的标题", "content": "整理后的正文，保留要点，可用换行", "dueDate": "YYYY-MM-DD 或 null", "time": "HH:mm 或 null"}
            今天的日期是 $today。识别不出日期/时间就填 null，**不要编造**。
        """.trimIndent()
        val payload = buildJsonObject {
            put("model", model)
            put("temperature", 0.2)
            putJsonArray("messages") {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                add(buildJsonObject { put("role", "user"); put("content", text) })
            }
        }
        return payload.toString()
    }

    /** 解析模型返回的 content（可能被 ```json 包着，也可能前后带话）。 */
    internal fun parseContent(content: String, today: LocalDate): MemoDraft? {
        val cleaned = content.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val obj = runCatching { json.parseToJsonElement(cleaned.substring(start, end + 1)).jsonObject }
            .getOrNull() ?: return null
        val title = obj["title"]?.jsonPrimitive?.contentOrNullSafe().orEmpty().trim()
        if (title.isEmpty()) return null
        val contentText = obj["content"]?.jsonPrimitive?.contentOrNullSafe().orEmpty()
        val due = obj["dueDate"]?.jsonPrimitive?.contentOrNullSafe()
            ?.takeIf { it.isNotBlank() && !it.equals("null", true) }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalMemoRules.findDate(contentText, today)
        val time = obj["time"]?.jsonPrimitive?.contentOrNullSafe()
            ?.takeIf { it.isNotBlank() && !it.equals("null", true) }
            ?: LocalMemoRules.findTime(contentText)
        return MemoDraft(
            title = title,
            content = contentText.trim(),
            dueDate = due,
            time = time,
            source = MemoDraft.Source.AI
        )
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        runCatching { content }.getOrNull()

    /**
     * 整理一段文字。成功返回 AI 草稿；任何失败都退回本地规则（并把 source 标成 LOCAL），
     * 让调用方永远有话可说 —— 不会出现"AI 转圈然后什么都没发生"。
     */
    suspend fun organize(context: Context, raw: String): MemoDraft {
        val text = raw.trim()
        if (text.isEmpty()) return MemoDraft("", "", source = MemoDraft.Source.LOCAL)
        val key = AppPrefs.getAiApiKey(context)
        if (key.isBlank()) return LocalMemoRules.organize(text)

        val today = LocalDate.now()
        val body = buildBody(AppPrefs.getAiModel(context), text, today)
        val remote = runCatching {
            postJson(
                url = endpoint(AppPrefs.getAiBaseUrl(context)),
                apiKey = key,
                body = body
            )
        }.getOrNull()
        if (remote == null) return LocalMemoRules.organize(text)

        val content = runCatching {
            json.parseToJsonElement(remote).jsonObject["choices"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.content
        }.getOrNull()
        if (content.isNullOrBlank()) return LocalMemoRules.organize(text)
        return parseContent(content, today) ?: LocalMemoRules.organize(text)
    }

    /** 连通性测试：用一句最短的请求确认 base/model/key 三者都对。 */
    suspend fun testConnection(context: Context): Result<String> = runCatching {
        val key = AppPrefs.getAiApiKey(context)
        if (key.isBlank()) error("API Key 为空")
        val body = buildBody(AppPrefs.getAiModel(context), "ping", LocalDate.now())
        val raw = postJson(endpoint(AppPrefs.getAiBaseUrl(context)), key, body)
        val content = json.parseToJsonElement(raw).jsonObject["choices"]?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
        content?.take(40) ?: "OK"
    }

    private fun postJson(url: String, apiKey: String, body: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TimeUnit.SECONDS.toMillis(15).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            if (code !in 200..299) throw IOException("HTTP $code: ${text.take(200)}")
            return text
        } finally {
            runCatching { conn.disconnect() }
        }
    }
}
