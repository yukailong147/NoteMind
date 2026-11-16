package com.team.classroomnotes.ai

import com.team.classroomnotes.data.ChatMessage
import com.team.classroomnotes.data.NoteItem

data class ChatReply(
    val content: String,
    val providerName: String,
    val modelName: String,
    val latencyMs: Long
)

interface AiChatClient {
    fun reply(
        note: NoteItem,
        history: List<ChatMessage>,
        modelId: String,
        callback: (Result<ChatReply>) -> Unit
    )

    fun summarize(
        note: NoteItem,
        history: List<ChatMessage>,
        modelId: String,
        callback: (Result<GeneratedNote>) -> Unit
    )
}

