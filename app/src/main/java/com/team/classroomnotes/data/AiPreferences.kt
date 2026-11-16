package com.team.classroomnotes.data

import android.content.Context
import com.team.classroomnotes.ai.ChatModelCatalog

class AiPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        "ai_preferences",
        Context.MODE_PRIVATE
    )

    fun getChatModelId(): String {
        return ChatModelCatalog.normalize(
            preferences.getString(KEY_CHAT_MODEL, ChatModelCatalog.FLASH)
        )
    }

    fun setChatModelId(modelId: String) {
        preferences.edit()
            .putString(KEY_CHAT_MODEL, ChatModelCatalog.normalize(modelId))
            .apply()
    }

    companion object {
        private const val KEY_CHAT_MODEL = "chat_model"
    }
}
