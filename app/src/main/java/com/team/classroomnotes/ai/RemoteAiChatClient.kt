package com.team.classroomnotes.ai

import android.os.Handler
import android.os.Looper
import com.team.classroomnotes.BuildConfig
import com.team.classroomnotes.data.ChatMessage
import com.team.classroomnotes.data.NoteItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class RemoteAiChatClient : AiChatClient {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun reply(
        note: NoteItem,
        history: List<ChatMessage>,
        callback: (Result<ChatReply>) -> Unit
    ) {
        executor.execute {
            val startedAt = System.currentTimeMillis()
            val result = runCatching {
                val systemPrompt = buildNoteContext(note) +
                    "\n你是课堂学习助手。只依据笔记上下文和对话回答；" +
                    "无法确定时明确说明，不要编造。回答应简洁，可分步骤或给例子。"
                val content = requestCompletion(
                    systemPrompt = systemPrompt,
                    history = history,
                    jsonOutput = false
                )
                ChatReply(
                    content = content,
                    providerName = providerName(),
                    modelName = BuildConfig.AI_MODEL,
                    latencyMs = System.currentTimeMillis() - startedAt
                )
            }
            mainHandler.post { callback(result) }
        }
    }

    override fun summarize(
        note: NoteItem,
        history: List<ChatMessage>,
        callback: (Result<GeneratedNote>) -> Unit
    ) {
        executor.execute {
            val startedAt = System.currentTimeMillis()
            val result = runCatching {
                val systemPrompt = """
                    你是课堂笔记整理助手。请把给定的一条原始笔记和后续 AI 对话整理成一条新的复习笔记。
                    只输出一个 JSON 对象，不要输出 Markdown 或额外解释。
                    JSON 字段：
                    title: 字符串；
                    summary: 80 至 180 字；
                    key_points: 字符串数组；
                    questions: 字符串数组；
                    keywords: 字符串数组；
                    uncertain: 字符串数组。
                    不要编造原笔记和对话中不存在的事实。
                """.trimIndent()

                val transcript = history.joinToString("\n") { message ->
                    val speaker = if (message.role == ChatMessage.ROLE_USER) "用户" else "AI"
                    "$speaker：${message.content}"
                }
                val userContent = buildNoteContext(note) +
                    "\n\n以下是后续对话：\n$transcript"
                val content = requestCompletion(
                    systemPrompt = systemPrompt,
                    history = listOf(
                        ChatMessage(
                            role = ChatMessage.ROLE_USER,
                            content = userContent,
                            createdAt = System.currentTimeMillis()
                        )
                    ),
                    jsonOutput = true
                )
                parseGeneratedNote(content).copy(
                    isMock = false,
                    providerName = providerName(),
                    modelName = BuildConfig.AI_MODEL,
                    latencyMs = System.currentTimeMillis() - startedAt
                )
            }
            mainHandler.post { callback(result) }
        }
    }

    private fun requestCompletion(
        systemPrompt: String,
        history: List<ChatMessage>,
        jsonOutput: Boolean
    ): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
        history.takeLast(20).forEach { message ->
            messages.put(
                JSONObject()
                    .put("role", message.role)
                    .put("content", message.content)
            )
        }

        val payload = JSONObject()
            .put("model", BuildConfig.AI_MODEL)
            .put("messages", messages)
            .put("temperature", 0.3)
            .put("max_tokens", 1400)
        if (jsonOutput) {
            payload.put("response_format", JSONObject().put("type", "json_object"))
        }

        val connection = (URL(BuildConfig.AI_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${BuildConfig.AI_API_KEY}")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        try {
            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            val statusCode = connection.responseCode
            val responseBody = (
                if (statusCode in 200..299) connection.inputStream else connection.errorStream
                )?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (statusCode !in 200..299) {
                throw IOException("AI 接口返回 HTTP $statusCode：${responseBody.take(300)}")
            }
            return extractAssistantContent(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildNoteContext(note: NoteItem): String {
        return buildString {
            appendLine("课程：${note.courseName}")
            appendLine("笔记标题：${note.title}")
            appendLine("摘要：${note.summary}")
            appendLine("知识点：${note.keyPoints.joinToString("；")}")
            appendLine("待复习问题：${note.questions.joinToString("；")}")
            append("检索关键词：${note.keywords.joinToString("；")}")
        }
    }

    private fun extractAssistantContent(responseBody: String): String {
        val root = JSONObject(responseBody)
        val choice = root.optJSONArray("choices")?.optJSONObject(0)
            ?: throw IOException("AI 返回内容缺少 choices")
        val message = choice.optJSONObject("message")
            ?: throw IOException("AI 返回内容缺少 message")
        val content = message.opt("content")

        return when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (index in 0 until content.length()) {
                    val text = content.optJSONObject(index)?.optString("text").orEmpty()
                    if (text.isNotBlank()) append(text)
                }
            }
            else -> throw IOException("AI 返回内容格式不受支持")
        }.ifBlank {
            throw IOException("AI 返回内容为空")
        }
    }

    private fun parseGeneratedNote(content: String): GeneratedNote {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) throw IOException("AI 没有返回 JSON 对象")

        val json = JSONObject(content.substring(start, end + 1))
        val title = json.optString("title").trim()
        val summary = json.optString("summary").trim()
        if (title.isBlank() || summary.isBlank()) {
            throw IOException("AI 返回的标题或摘要为空")
        }

        return GeneratedNote(
            title = title,
            summary = summary,
            keyPoints = json.optJSONArray("key_points").toStringList(),
            questions = json.optJSONArray("questions").toStringList(),
            keywords = json.optJSONArray("keywords").toStringList(),
            uncertain = json.optJSONArray("uncertain").toStringList(),
            isMock = false
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val value = optString(index).trim()
                if (value.isNotEmpty()) add(value)
            }
        }
    }

    private fun providerName(): String {
        return if (BuildConfig.AI_ENDPOINT.contains("deepseek", ignoreCase = true)) {
            "DeepSeek"
        } else {
            "OpenAI Compatible"
        }
    }

    companion object {
        private val executor = Executors.newFixedThreadPool(2)
    }
}

