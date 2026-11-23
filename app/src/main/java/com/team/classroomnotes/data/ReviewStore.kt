package com.team.classroomnotes.data

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ReviewStore(private val file: File) {
    constructor(context: Context) : this(File(context.filesDir, "review_progress.txt"))

    @Synchronized
    fun loadAll(): List<ReviewRecord> {
        if (!file.exists()) return emptyList()

        return runCatching {
            file.readLines()
                .drop(1)
                .mapNotNull(::decodeLine)
        }.getOrElse { emptyList() }
    }

    @Synchronized
    fun save(record: ReviewRecord): List<ReviewRecord> {
        val records = loadAll()
            .filterNot { it.questionId == record.questionId }
            .plus(record)
        writeAll(records)
        return records
    }

    private fun writeAll(records: List<ReviewRecord>) {
        file.parentFile?.mkdirs()
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        temporaryFile.writeText(
            buildString {
                appendLine(HEADER)
                records.forEach { record ->
                    append(record.questionId)
                    append('\t')
                    append(record.noteId)
                    append('\t')
                    append(encodeText(record.questionText))
                    append('\t')
                    append(record.outcome.name)
                    append('\t')
                    appendLine(record.reviewedAt)
                }
            }
        )
        Files.move(
            temporaryFile.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    private fun decodeLine(line: String): ReviewRecord? {
        val parts = line.split('\t')
        if (parts.size != 5) return null

        return runCatching {
            ReviewRecord(
                questionId = parts[0],
                noteId = parts[1],
                questionText = decodeText(parts[2]),
                outcome = ReviewOutcome.valueOf(parts[3]),
                reviewedAt = parts[4].toLong()
            )
        }.getOrNull()
    }

    private fun encodeText(value: String): String {
        return value.toByteArray(Charsets.UTF_8)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun decodeText(value: String): String {
        val bytes = value.chunked(2)
            .map { pair -> pair.toInt(16).toByte() }
            .toByteArray()
        return String(bytes, Charsets.UTF_8)
    }

    companion object {
        private const val HEADER = "classroom-notes-review-v1"
    }
}
