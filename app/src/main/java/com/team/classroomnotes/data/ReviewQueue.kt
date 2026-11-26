package com.team.classroomnotes.data

import java.security.MessageDigest

object ReviewQueue {
    fun build(
        notes: List<NoteItem>,
        records: List<ReviewRecord>
    ): List<ReviewQuestion> {
        val latestRecords = records
            .groupBy { it.questionId }
            .mapValues { (_, values) -> values.maxBy { it.reviewedAt } }

        return notes
            .asSequence()
            .filter { it.status == NoteStatus.SUCCESS }
            .flatMap { note ->
                note.questions.asSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .map { question -> note.toReviewQuestion(question) }
            }
            .sortedWith(
                compareBy<ReviewQuestion> {
                    when (latestRecords[it.id]?.outcome) {
                        ReviewOutcome.AGAIN -> 0
                        null -> 1
                        ReviewOutcome.KNOWN -> 2
                    }
                }
            )
            .toList()
    }

    private fun NoteItem.toReviewQuestion(question: String): ReviewQuestion {
        return ReviewQuestion(
            id = questionId(id, question),
            noteId = id,
            noteTitle = title,
            courseName = courseName,
            questionText = question,
            summary = summary,
            keyPoints = keyPoints,
            bookName = bookName,
            chapter = chapter
        )
    }

    fun questionId(noteId: String, questionText: String): String {
        val source = "$noteId\u0000${questionText.trim()}".toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256")
            .digest(source)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
            .take(ID_LENGTH)
    }

    private const val ID_LENGTH = 24
}
