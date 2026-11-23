package com.team.classroomnotes.data

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReviewStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun savedRecord_canBeReadAfterCreatingANewStoreForTheSameFile() {
        val file = temporaryFolder.newFile("review_progress.txt")
        val record = ReviewRecord(
            questionId = "tree-question",
            noteId = "tree-note",
            questionText = "如果左子树为空，下一步访问谁？",
            outcome = ReviewOutcome.AGAIN,
            reviewedAt = 1_700_000_000_000
        )

        ReviewStore(file).save(record)
        val result = ReviewStore(file).loadAll()

        assertEquals(listOf(record), result)
    }
}
