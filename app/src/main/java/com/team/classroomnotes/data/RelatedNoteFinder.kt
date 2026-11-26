package com.team.classroomnotes.data

data class RelatedNoteMatch(
    val note: NoteItem,
    val score: Int,
    val matchedKeywords: List<String>
)

object RelatedNoteFinder {
    fun find(
        current: NoteItem,
        candidates: List<NoteItem>,
        limit: Int = 10
    ): List<RelatedNoteMatch> {
        val currentTerms = searchableTerms(current)
        val hasCurrentLocation = current.bookName.isNotBlank() && current.chapter.isNotBlank()

        return candidates
            .asSequence()
            .filter { it.id != current.id }
            .filter { it.status == NoteStatus.SUCCESS }
            .mapNotNull { candidate ->
                val candidateTerms = searchableTerms(candidate)
                val matched = currentTerms.filter { currentTerm ->
                    candidateTerms.any { candidateTerm ->
                        termsMatch(currentTerm, candidateTerm)
                    }
                }.distinct()

                val sameChapter = hasCurrentLocation &&
                    current.bookName.equals(candidate.bookName, ignoreCase = true) &&
                    current.chapter.equals(candidate.chapter, ignoreCase = true)
                val sameCourse = current.courseName.equals(candidate.courseName, ignoreCase = true)

                val score = matched.size * 3 +
                    if (sameChapter) 4 else 0 +
                    if (sameCourse) 1 else 0

                if (score <= 0 || (matched.isEmpty() && !sameChapter)) {
                    null
                } else {
                    RelatedNoteMatch(
                        note = candidate,
                        score = score,
                        matchedKeywords = matched.take(5)
                    )
                }
            }
            .sortedWith(
                compareByDescending<RelatedNoteMatch> { it.score }
                    .thenByDescending { it.note.createdAt }
            )
            .take(limit)
            .toList()
    }

    private fun searchableTerms(note: NoteItem): List<String> {
        val raw = if (note.keywords.isNotEmpty()) {
            note.keywords
        } else {
            listOf(note.title) + note.keyPoints.take(3)
        }

        return raw
            .flatMap { it.split(TERM_SEPARATOR) }
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 }
            .distinct()
    }

    private fun termsMatch(left: String, right: String): Boolean {
        if (left == right) return true
        if (left.length < 2 || right.length < 2) return false
        return left.contains(right) || right.contains(left)
    }

    private val TERM_SEPARATOR = Regex("[\\s,，。；;、:：()（）\\[\\]【】\\n]+")
}
