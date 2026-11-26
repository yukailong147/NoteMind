package com.team.classroomnotes.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewQueueTest {
    @Test
    fun build_collectsQuestionsOnlyFromSuccessfulNotes() {
        val notes = listOf(
            note(
                id = "success",
                status = NoteStatus.SUCCESS,
                questions = listOf("什么是中序遍历？")
            ),
            note(
                id = "draft",
                status = NoteStatus.DRAFT,
                questions = listOf("草稿中的问题")
            ),
            note(
                id = "failed",
                status = NoteStatus.FAILED,
                questions = listOf("失败笔记中的问题")
            )
        )

        val result = ReviewQueue.build(notes, records = emptyList())

        assertEquals(listOf("success"), result.map { it.noteId })
    }

    @Test
    fun build_prioritizesAgainQuestionsThenNewQuestionsThenKnownQuestions() {
        val note = note(
            id = "tree",
            status = NoteStatus.SUCCESS,
            questions = listOf("已经掌握的问题", "没有复习的问题", "需要再看看的问题")
        )
        val records = listOf(
            reviewRecord("tree", "已经掌握的问题", ReviewOutcome.KNOWN, 30),
            reviewRecord("tree", "需要再看看的问题", ReviewOutcome.AGAIN, 20)
        )

        val result = ReviewQueue.build(listOf(note), records)

        assertEquals(
            listOf("需要再看看的问题", "没有复习的问题", "已经掌握的问题"),
            result.map { it.questionText }
        )
    }

    @Test
    fun questionId_usesANoteScopedStableDigest() {
        val result = ReviewQueue.questionId("tree", "什么是中序遍历？")

        assertEquals("5ebfc40399760b892287f829", result)
    }

    @Test
    fun build_ignoresBlankQuestionsAndTrimsQuestionText() {
        val note = note(
            id = "tree",
            status = NoteStatus.SUCCESS,
            questions = listOf("   ", "  什么是中序遍历？  ")
        )

        val result = ReviewQueue.build(listOf(note), emptyList())

        assertEquals(listOf("什么是中序遍历？"), result.map { it.questionText })
    }

    @Test
    fun build_returnsEmptyWhenSuccessfulNotesHaveNoQuestions() {
        val note = note(
            id = "tree",
            status = NoteStatus.SUCCESS,
            questions = emptyList()
        )

        val result = ReviewQueue.build(listOf(note), emptyList())

        assertTrue(result.isEmpty())
    }

    private fun note(
        id: String,
        status: NoteStatus,
        questions: List<String>
    ): NoteItem {
        return NoteItem(
            id = id,
            courseName = "数据结构",
            title = "二叉树课堂笔记",
            summary = "中序遍历的顺序是左、根、右。",
            keyPoints = listOf("先遍历左子树", "再访问根节点"),
            questions = questions,
            keywords = emptyList(),
            uncertain = emptyList(),
            imagePath = "",
            createdAt = 0,
            isMock = false,
            status = status,
            errorMessage = ""
        )
    }

    private fun reviewRecord(
        noteId: String,
        questionText: String,
        outcome: ReviewOutcome,
        reviewedAt: Long
    ): ReviewRecord {
        return ReviewRecord(
            questionId = ReviewQueue.questionId(noteId, questionText),
            noteId = noteId,
            questionText = questionText,
            outcome = outcome,
            reviewedAt = reviewedAt
        )
    }
}
