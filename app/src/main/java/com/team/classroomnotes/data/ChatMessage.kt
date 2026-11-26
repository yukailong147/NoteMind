package com.team.classroomnotes.data

data class ChatMessage(
    val role: String,
    val content: String,
    val createdAt: Long
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}

