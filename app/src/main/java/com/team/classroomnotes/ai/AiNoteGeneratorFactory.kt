package com.team.classroomnotes.ai

import com.team.classroomnotes.BuildConfig

object AiNoteGeneratorFactory {
    fun create(): AiNoteGenerator {
        val configured =
            BuildConfig.AI_ENDPOINT.isNotBlank() &&
                BuildConfig.AI_IMAGE_MODEL.isNotBlank() &&
                BuildConfig.AI_API_KEY.isNotBlank()

        return if (configured) {
            RemoteAiNoteGenerator()
        } else {
            MockAiNoteGenerator()
        }
    }
}

