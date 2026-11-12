package com.team.classroomnotes.data

data class NoteItem(
    val id: String,
    val courseName: String,
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
    val questions: List<String>,
    val keywords: List<String>,
    val uncertain: List<String>,
    val imagePath: String,
    val createdAt: Long,
    val isMock: Boolean,
    val status: NoteStatus,
    val errorMessage: String
)

enum class NoteStatus {
    DRAFT,
    PROCESSING,
    SUCCESS,
    FAILED;

    companion object {
        fun fromStorage(value: String?): NoteStatus {
            return entries.firstOrNull { it.name == value } ?: SUCCESS
        }
    }
}

