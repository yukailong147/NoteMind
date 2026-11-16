package com.team.classroomnotes.ai

object ChatModelCatalog {
    const val FLASH = "deepseek-flash"
    const val PRO = "deepseek-v4-pro"

    val availableModels = listOf(FLASH, PRO)

    fun normalize(modelId: String?): String {
        return modelId?.takeIf { it in availableModels } ?: FLASH
    }
}

