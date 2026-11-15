package com.team.classroomnotes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChatStore(context: Context) {
    private val chatDirectory = File(context.filesDir, "chats")

    @Synchronized
    fun load(noteId: String): List<ChatMessage> {
        val file = fileFor(noteId)
        if (!file.exists()) return emptyList()

        return runCatching {
            val root = JSONArray(file.readText())
            buildList {
                for (index in 0 until root.length()) {
                    val item = root.getJSONObject(index)
                    add(
                        ChatMessage(
                            role = item.getString("role"),
                            content = item.getString("content"),
                            createdAt = item.getLong("createdAt")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    @Synchronized
    fun append(noteId: String, message: ChatMessage): List<ChatMessage> {
        val messages = load(noteId).toMutableList()
        messages.add(message)
        writeAll(noteId, messages)
        return messages
    }

    @Synchronized
    fun clear(noteId: String) {
        runCatching { fileFor(noteId).delete() }
    }

    private fun writeAll(noteId: String, messages: List<ChatMessage>) {
        chatDirectory.mkdirs()
        val root = JSONArray()
        messages.forEach { message ->
            root.put(
                JSONObject()
                    .put("role", message.role)
                    .put("content", message.content)
                    .put("createdAt", message.createdAt)
            )
        }
        fileFor(noteId).writeText(root.toString())
    }

    private fun fileFor(noteId: String): File {
        return File(chatDirectory, "$noteId.json")
    }
}

