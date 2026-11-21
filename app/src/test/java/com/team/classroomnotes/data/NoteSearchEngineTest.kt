package com.team.classroomnotes.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSearchEngineTest {
    @Test
    fun search_matchesChineseTitle() {
        val notes = listOf(
            note(id = "tree", title = "二叉树的中序遍历"),
            note(id = "array", title = "数组与指针")
        )

        val result = NoteSearchEngine.search("二叉树", notes)

        assertEquals(listOf("tree"), result.map { it.note.id })
    }

    @Test
    fun search_matchesKeywordWhenTitleDoesNotContainQuery() {
        val notes = listOf(
            note(
                id = "tree",
                title = "第 3 次课堂记录",
                keywords = listOf("二叉树", "中序遍历")
            ),
            note(id = "array", title = "数组与指针")
        )

        val result = NoteSearchEngine.search("二叉树", notes)

        assertEquals(listOf("tree"), result.map { it.note.id })
    }

    @Test
    fun search_matchesTextbookChapter() {
        val notes = listOf(
            note(
                id = "tree",
                title = "第 3 次课堂记录",
                bookName = "数据结构教材",
                chapter = "第3章 树与二叉树"
            )
        )

        val result = NoteSearchEngine.search("第3章", notes)

        assertEquals(listOf("tree"), result.map { it.note.id })
    }

    @Test
    fun blankQuery_returnsEmptyResult() {
        val result = NoteSearchEngine.search(
            "   ",
            listOf(note(id = "tree", title = "二叉树"))
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun search_ignoresNotesThatAreNotSuccessfullyGenerated() {
        val notes = listOf(
            note(id = "draft", title = "二叉树草稿", status = NoteStatus.DRAFT),
            note(id = "processing", title = "二叉树处理中", status = NoteStatus.PROCESSING),
            note(id = "failed", title = "二叉树失败", status = NoteStatus.FAILED),
            note(id = "success", title = "二叉树完成", status = NoteStatus.SUCCESS)
        )

        val result = NoteSearchEngine.search("二叉树", notes)

        assertEquals(listOf("success"), result.map { it.note.id })
    }

    @Test
    fun titleMatch_ranksAboveSummaryMatch() {
        val notes = listOf(
            note(
                id = "summary",
                title = "课堂补充",
                summary = "这里讨论二叉树的中序遍历"
            ),
            note(
                id = "title",
                title = "二叉树中序遍历总结",
                summary = "课堂内容整理"
            )
        )

        val result = NoteSearchEngine.search("二叉树", notes)

        assertEquals(listOf("title", "summary"), result.map { it.note.id })
        assertTrue(result.first().score > result.last().score)
    }

    private fun note(
        id: String,
        title: String,
        summary: String = "",
        keywords: List<String> = emptyList(),
        bookName: String = "",
        chapter: String = "",
        status: NoteStatus = NoteStatus.SUCCESS
    ): NoteItem {
        return NoteItem(
            id = id,
            courseName = "数据结构",
            title = title,
            summary = summary,
            keyPoints = emptyList(),
            questions = emptyList(),
            keywords = keywords,
            uncertain = emptyList(),
            imagePath = "",
            createdAt = 0,
            isMock = false,
            status = status,
            errorMessage = "",
            bookName = bookName,
            chapter = chapter
        )
    }
}
