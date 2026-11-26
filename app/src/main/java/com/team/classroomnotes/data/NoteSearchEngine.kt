package com.team.classroomnotes.data

enum class SearchField(val weight: Int) {
    TITLE(6),
    KEYWORDS(5),
    KEY_POINTS(4),
    QUESTIONS(3),
    CHAPTER(3),
    COURSE(2),
    BOOK(2),
    SUMMARY(2)
}

data class NoteSearchMatch(
    val note: NoteItem,
    val score: Int,
    val matchedFields: List<SearchField>
)

object NoteSearchEngine {
    fun search(query: String, notes: List<NoteItem>): List<NoteSearchMatch> {
        val tokens = query
            .lowercase()
            .split(QUERY_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (tokens.isEmpty()) return emptyList()

        return notes
            .asSequence()
            .filter { it.status == NoteStatus.SUCCESS }
            .map { note -> match(note, tokens) }
            .filter { it.score > 0 }
            .sortedWith(
                compareByDescending<NoteSearchMatch> { it.score }
                    .thenByDescending { it.note.createdAt }
            )
            .toList()
    }

    private fun match(note: NoteItem, tokens: List<String>): NoteSearchMatch {
        var score = 0
        val matchedFields = linkedSetOf<SearchField>()

        tokens.forEach { token ->
            fun check(field: SearchField, values: List<String>) {
                if (values.any { it.lowercase().contains(token) }) {
                    score += field.weight
                    matchedFields.add(field)
                }
            }

            check(SearchField.TITLE, listOf(note.title))
            check(SearchField.KEYWORDS, note.keywords)
            check(SearchField.KEY_POINTS, note.keyPoints)
            check(SearchField.QUESTIONS, note.questions)
            check(SearchField.CHAPTER, listOf(note.chapter))
            check(SearchField.COURSE, listOf(note.courseName))
            check(SearchField.BOOK, listOf(note.bookName))
            check(SearchField.SUMMARY, listOf(note.summary))
        }

        return NoteSearchMatch(
            note = note,
            score = score,
            matchedFields = matchedFields.toList()
        )
    }

    private val QUERY_SEPARATOR = Regex("[\\s,，。；;、:：]+")
}
