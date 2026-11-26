package com.team.classroomnotes.data

enum class ReviewOutcome {
    AGAIN,
    KNOWN
}

data class ReviewRecord(
    val questionId: String,
    val noteId: String,
    val questionText: String,
    val outcome: ReviewOutcome,
    val reviewedAt: Long
)

data class ReviewQuestion(
    val id: String,
    val noteId: String,
    val noteTitle: String,
    val courseName: String,
    val questionText: String,
    val summary: String,
    val keyPoints: List<String>,
    val bookName: String,
    val chapter: String
)
