package com.team.classroomnotes.ai

data class GeneratedNote(
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
    val questions: List<String>,
    val keywords: List<String>,
    val uncertain: List<String>,
    val isMock: Boolean,
    val providerName: String = "",
    val modelName: String = "",
    val latencyMs: Long = 0
)

