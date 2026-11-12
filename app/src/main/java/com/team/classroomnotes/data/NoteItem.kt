package com.team.classroomnotes.data

data class NoteItem(
    val id: String,
    val courseName: String,
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
    val questions: List<String>,
    val imagePath: String,
    val createdAt: Long,
    val isMock: Boolean
)

