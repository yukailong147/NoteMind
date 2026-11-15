package com.team.classroomnotes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NoteStore(context: Context) {
    private val notesFile = File(context.filesDir, "notes.json")
    private val pendingDeletionFile = File(context.filesDir, "pending_deletion.json")

    @Synchronized
    fun loadAll(): List<NoteItem> {
        if (!notesFile.exists()) return emptyList()

        return runCatching {
            val root = JSONArray(notesFile.readText())
            buildList {
                for (index in 0 until root.length()) {
                    runCatching {
                        add(root.getJSONObject(index).toNoteItem())
                    }
                }
            }.sortedByDescending { it.createdAt }
        }.getOrElse { emptyList() }
    }

    @Synchronized
    fun add(note: NoteItem): List<NoteItem> {
        val notes = loadAll().filterNot { it.id == note.id }.toMutableList()
        notes.add(0, note)
        writeAll(notes)
        return notes
    }

    @Synchronized
    fun update(note: NoteItem): NoteItem {
        val notes = loadAll().toMutableList()
        val index = notes.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            notes[index] = note
        } else {
            notes.add(0, note)
        }
        writeAll(notes)
        return note
    }

    @Synchronized
    fun delete(noteId: String): NoteItem? {
        val notes = loadAll().toMutableList()
        val removed = notes.firstOrNull { it.id == noteId } ?: return null
        notes.removeAll { it.id == noteId }
        writeAll(notes)
        pendingDeletionFile.writeText(removed.toJson().toString())
        return removed
    }

    @Synchronized
    fun loadPendingDeletion(): NoteItem? {
        if (!pendingDeletionFile.exists()) return null
        return runCatching {
            JSONObject(pendingDeletionFile.readText()).toNoteItem()
        }.getOrNull()
    }

    @Synchronized
    fun clearPendingDeletion() {
        runCatching { pendingDeletionFile.delete() }
    }

    @Synchronized
    fun findById(id: String): NoteItem? = loadAll().firstOrNull { it.id == id }

    private fun writeAll(notes: List<NoteItem>) {
        notesFile.parentFile?.mkdirs()
        val root = JSONArray()
        notes.forEach { root.put(it.toJson()) }
        notesFile.writeText(root.toString())
    }

    private fun NoteItem.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("courseName", courseName)
        put("title", title)
        put("summary", summary)
        put("keyPoints", keyPoints.toJsonArray())
        put("questions", questions.toJsonArray())
        put("keywords", keywords.toJsonArray())
        put("uncertain", uncertain.toJsonArray())
        put("imagePath", imagePath)
        put("createdAt", createdAt)
        put("isMock", isMock)
        put("status", status.name)
        put("errorMessage", errorMessage)
        put("providerName", providerName)
        put("modelName", modelName)
        put("latencyMs", latencyMs)
        put("sourceType", sourceType)
        put("sourceNoteId", sourceNoteId)
    }

    private fun JSONObject.toNoteItem(): NoteItem = NoteItem(
        id = getString("id"),
        courseName = getString("courseName"),
        title = getString("title"),
        summary = getString("summary"),
        keyPoints = getJSONArray("keyPoints").toStringList(),
        questions = getJSONArray("questions").toStringList(),
        keywords = optJSONArray("keywords")?.toStringList() ?: emptyList(),
        uncertain = optJSONArray("uncertain")?.toStringList() ?: emptyList(),
        imagePath = getString("imagePath"),
        createdAt = getLong("createdAt"),
        isMock = optBoolean("isMock", false),
        status = NoteStatus.fromStorage(optString("status").takeIf { it.isNotBlank() }),
        errorMessage = optString("errorMessage", ""),
        providerName = optString("providerName", ""),
        modelName = optString("modelName", ""),
        latencyMs = optLong("latencyMs", 0),
        sourceType = optString("sourceType", "photo"),
        sourceNoteId = optString("sourceNoteId", "")
    )

    private fun List<String>.toJsonArray(): JSONArray = JSONArray().also { array ->
        forEach { array.put(it) }
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) {
            add(getString(index))
        }
    }
}

