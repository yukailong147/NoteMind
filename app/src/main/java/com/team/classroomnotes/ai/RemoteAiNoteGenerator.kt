package com.team.classroomnotes.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.team.classroomnotes.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class RemoteAiNoteGenerator : AiNoteGenerator {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun generate(
        courseName: String,
        imageFile: File,
        callback: (Result<GeneratedNote>) -> Unit
    ) {
        executor.execute {
            val startedAt = System.currentTimeMillis()
            val result = runCatching {
                requestModel(courseName, imageFile).copy(
                    providerName = if (BuildConfig.AI_ENDPOINT.contains("deepseek", ignoreCase = true)) {
                        "DeepSeek"
                    } else {
                        "OpenAI Compatible"
                    },
                    modelName = BuildConfig.AI_MODEL,
                    latencyMs = System.currentTimeMillis() - startedAt
                )
            }
            mainHandler.post { callback(result) }
        }
    }

    private fun requestModel(courseName: String, imageFile: File): GeneratedNote {
        val imageBase64 = encodeImage(imageFile)
        val endpoint = URL(BuildConfig.AI_ENDPOINT)
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${BuildConfig.AI_API_KEY}")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        val payload = buildPayload(courseName, imageBase64)
        connection.outputStream.use { output ->
            output.write(payload.toString().toByteArray(Charsets.UTF_8))
        }

        val statusCode = connection.responseCode
        val responseBody = (
            if (statusCode in 200..299) connection.inputStream else connection.errorStream
            )?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (statusCode !in 200..299) {
            throw IOException("AI 接口返回 HTTP $statusCode：${responseBody.take(300)}")
        }

        val content = extractAssistantContent(responseBody)
        return parseGeneratedNote(content)
    }

    private fun buildPayload(courseName: String, imageBase64: String): JSONObject {
        val systemPrompt = """
            你是课堂笔记整理助手。请阅读用户提供的板书图片。
            只输出一个 JSON 对象，不要输出 Markdown 代码块或额外解释。
            JSON 必须包含以下字段：
            title: 字符串，简洁标题；
            summary: 字符串，80 至 180 字；
            key_points: 字符串数组，3 至 6 个知识点；
            questions: 字符串数组，1 至 4 个待复习问题；
            keywords: 字符串数组，3 至 8 个检索关键词；
            uncertain: 字符串数组，列出无法确认的文字、公式或图形。
            不要补写图片中不存在的内容。看不清的内容必须放入 uncertain。
        """.trimIndent()

        val userContent = JSONArray()
            .put(
                JSONObject()
                    .put("type", "text")
                    .put("text", "课程名称：$courseName。请整理这张课堂板书。")
            )
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put(
                        "image_url",
                        JSONObject().put(
                            "url",
                            "data:image/jpeg;base64,$imageBase64"
                        )
                    )
            )

        return JSONObject()
            .put("model", BuildConfig.AI_MODEL)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userContent))
            )
            .put("temperature", 0.2)
            .put("max_tokens", 1200)
            .put(
                "response_format",
                JSONObject().put("type", "json_object")
            )
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
                    val part = content.optJSONObject(index)
                    val text = part?.optString("text").orEmpty()
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
        if (start < 0 || end <= start) {
            throw IOException("AI 没有返回 JSON 对象")
        }

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

    private fun encodeImage(file: File): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > 1600 ||
            bounds.outHeight / sampleSize > 1600
        ) {
            sampleSize *= 2
        }

        val bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: throw IOException("无法读取板书图片")

        val compressed = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, compressed)
        bitmap.recycle()
        return Base64.encodeToString(compressed.toByteArray(), Base64.NO_WRAP)
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

    companion object {
        private val executor = Executors.newFixedThreadPool(2)
    }
}

