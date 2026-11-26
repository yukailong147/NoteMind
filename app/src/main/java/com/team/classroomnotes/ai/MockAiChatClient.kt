package com.team.classroomnotes.ai

import android.os.Handler
import android.os.Looper
import com.team.classroomnotes.data.ChatMessage
import com.team.classroomnotes.data.NoteItem
import java.io.IOException

class MockAiChatClient : AiChatClient {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var failureConsumed = false

    override fun reply(
        note: NoteItem,
        history: List<ChatMessage>,
        modelId: String,
        callback: (Result<ChatReply>) -> Unit
    ) {
        val question = history.lastOrNull { it.role == ChatMessage.ROLE_USER }?.content.orEmpty()
        mainHandler.postDelayed({
            if (question.contains("模拟失败") && !failureConsumed) {
                failureConsumed = true
                callback(Result.failure(IOException("这是用于测试对话重试的模拟错误")))
                return@postDelayed
            }

            callback(
                Result.success(
                    ChatReply(
                        content = "这是围绕《${note.title}》的模拟回答。你的问题是：$question\n\n当前未配置 API Key，因此暂不调用真实模型。下一步可以追问知识点、例题或让 AI 设计一道复习题。",
                        providerName = "Mock",
                        modelName = "mock-$modelId",
                        latencyMs = 600
                    )
                )
            )
        }, 600)
    }

    override fun summarize(
        note: NoteItem,
        history: List<ChatMessage>,
        modelId: String,
        callback: (Result<GeneratedNote>) -> Unit
    ) {
        val asked = history
            .filter { it.role == ChatMessage.ROLE_USER }
            .takeLast(3)
            .map { it.content }
            .joinToString("；")

        mainHandler.postDelayed({
            callback(
                Result.success(
                    GeneratedNote(
                        title = "AI 讨论：${note.title}",
                        summary = "本次对话围绕《${note.title}》展开，讨论的问题包括：$asked。模拟整理器已把对话归纳为要点和待复习问题。",
                        keyPoints = note.keyPoints.take(3) + "通过连续追问补充了笔记的理解线索",
                        questions = note.questions.take(2) + "你能否不看原文复述本次讨论的结论？",
                        keywords = note.keywords.take(5),
                        uncertain = listOf("模拟总结仅用于验证对话归档流程"),
                        isMock = true,
                        providerName = "Mock",
                        modelName = "mock-$modelId-summary",
                        latencyMs = 700
                    )
                )
            )
        }, 700)
    }
}

