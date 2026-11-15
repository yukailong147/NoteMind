package com.team.classroomnotes.ai

import com.team.classroomnotes.BuildConfig

object AiChatClientFactory {
    fun create(): AiChatClient {
        val configured =
            BuildConfig.AI_ENDPOINT.isNotBlank() &&
                BuildConfig.AI_MODEL.isNotBlank() &&
                BuildConfig.AI_API_KEY.isNotBlank()

        return when {
            configured -> RemoteAiChatClient()
            else -> MockAiChatClient()
        }
    }
}
