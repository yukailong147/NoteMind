package com.team.classroomnotes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.team.classroomnotes.ai.AiChatClient
import com.team.classroomnotes.ai.AiChatClientFactory
import com.team.classroomnotes.ai.GeneratedNote
import com.team.classroomnotes.data.ChatMessage
import com.team.classroomnotes.data.ChatStore
import com.team.classroomnotes.data.NoteItem
import com.team.classroomnotes.data.NoteStatus
import com.team.classroomnotes.data.NoteStore
import java.util.UUID

class NoteChatActivity : AppCompatActivity() {
    private lateinit var noteStore: NoteStore
    private lateinit var chatStore: ChatStore
    private lateinit var client: AiChatClient
    private lateinit var note: NoteItem

    private lateinit var backButton: Button
    private lateinit var organizeButton: Button
    private lateinit var chatContextText: TextView
    private lateinit var chatScroll: ScrollView
    private lateinit var messagesContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var retryButton: Button
    private lateinit var input: EditText
    private lateinit var sendButton: Button

    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_chat)

        noteStore = NoteStore(this)
        chatStore = ChatStore(this)
        client = AiChatClientFactory.create()

        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        val loaded = noteId?.let(noteStore::findById)
        if (loaded == null) {
            finish()
            return
        }
        note = loaded

        bindViews()
        backButton.setOnClickListener { finish() }
        sendButton.setOnClickListener { sendMessage() }
        retryButton.setOnClickListener { retryLastQuestion() }
        organizeButton.setOnClickListener { confirmOrganize() }

        chatContextText.text = "${note.courseName} · ${note.title}"
        renderMessages()
    }

    private fun bindViews() {
        backButton = findViewById(R.id.chatBackButton)
        organizeButton = findViewById(R.id.organizeChatButton)
        chatContextText = findViewById(R.id.chatContextText)
        chatScroll = findViewById(R.id.chatScroll)
        messagesContainer = findViewById(R.id.chatMessagesContainer)
        statusText = findViewById(R.id.chatStatusText)
        progressBar = findViewById(R.id.chatProgress)
        retryButton = findViewById(R.id.retryChatButton)
        input = findViewById(R.id.chatInput)
        sendButton = findViewById(R.id.sendChatButton)
    }

    private fun sendMessage() {
        if (busy) return
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        chatStore.append(
            note.id,
            ChatMessage(
                role = ChatMessage.ROLE_USER,
                content = text,
                createdAt = System.currentTimeMillis()
            )
        )
        input.setText("")
        retryButton.visibility = View.GONE
        renderMessages()
        requestReply(chatStore.load(note.id))
    }

    private fun retryLastQuestion() {
        if (busy) return
        requestReply(chatStore.load(note.id))
    }

    private fun requestReply(history: List<ChatMessage>) {
        if (history.lastOrNull()?.role != ChatMessage.ROLE_USER) return
        setBusy(true, getString(R.string.chat_thinking))
        retryButton.visibility = View.GONE

        client.reply(note, history) { result ->
            result.fold(
                onSuccess = { reply ->
                    chatStore.append(
                        note.id,
                        ChatMessage(
                            role = ChatMessage.ROLE_ASSISTANT,
                            content = reply.content,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    if (!isFinishing && !isDestroyed) {
                        setBusy(false)
                        statusText.text = getString(
                            R.string.chat_reply_info,
                            reply.providerName,
                            reply.modelName,
                            reply.latencyMs / 1000.0
                        )
                        renderMessages()
                    }
                },
                onFailure = { error ->
                    if (!isFinishing && !isDestroyed) {
                        setBusy(false)
                        statusText.text = getString(R.string.chat_failed) +
                            "：" + (error.message ?: "")
                        statusText.visibility = View.VISIBLE
                        retryButton.visibility = View.VISIBLE
                    }
                }
            )
        }
    }

    private fun renderMessages() {
        val history = chatStore.load(note.id)
        messagesContainer.removeAllViews()
        if (history.isEmpty()) {
            val hint = TextView(this).apply {
                text = getString(R.string.chat_empty_hint)
                setTextColor(getColor(R.color.text_muted))
                textSize = 14f
                setLineSpacing(0f, 1.3f)
                setPadding(12, 24, 12, 24)
            }
            messagesContainer.addView(hint)
        } else {
            history.forEach { addMessageBubble(it) }
        }
        organizeButton.isEnabled = !busy && history.any {
            it.role == ChatMessage.ROLE_ASSISTANT
        }
        chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addMessageBubble(message: ChatMessage) {
        val isUser = message.role == ChatMessage.ROLE_USER
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(0, 6, 0, 6)
        }
        val bubble = TextView(this).apply {
            text = message.content
            setTextColor(getColor(if (isUser) R.color.white else R.color.text_primary))
            textSize = 14f
            setLineSpacing(0f, 1.25f)
            setPadding(14, 10, 14, 10)
            setBackgroundResource(if (isUser) {
                R.drawable.bg_chat_user
            } else {
                R.drawable.bg_chat_assistant
            })
            maxWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        }
        row.addView(bubble)
        messagesContainer.addView(row)
    }

    private fun setBusy(value: Boolean, message: String? = null) {
        busy = value
        input.isEnabled = !value
        sendButton.isEnabled = !value
        retryButton.isEnabled = !value
        organizeButton.isEnabled = !value && chatStore.load(note.id).any {
            it.role == ChatMessage.ROLE_ASSISTANT
        }
        progressBar.visibility = if (value) View.VISIBLE else View.GONE
        if (message != null) {
            statusText.text = message
            statusText.visibility = View.VISIBLE
        }
    }

    private fun confirmOrganize() {
        if (busy) return
        val history = chatStore.load(note.id)
        if (history.none { it.role == ChatMessage.ROLE_ASSISTANT }) {
            Toast.makeText(this, R.string.chat_not_ready, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.organize_chat_confirm_title)
            .setMessage(R.string.organize_chat_confirm_message)
            .setPositiveButton(R.string.organize_chat_note) { _, _ ->
                summarizeConversation(history)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun summarizeConversation(history: List<ChatMessage>) {
        setBusy(true, getString(R.string.chat_summarizing))
        client.summarize(note, history) { result ->
            result.fold(
                onSuccess = ::createChatNote,
                onFailure = { error ->
                    if (!isFinishing && !isDestroyed) {
                        setBusy(false)
                        statusText.text = getString(R.string.chat_summary_failed) +
                            "：" + (error.message ?: "")
                        statusText.visibility = View.VISIBLE
                    }
                }
            )
        }
    }

    private fun createChatNote(generated: GeneratedNote) {
        val newNote = NoteItem(
            id = UUID.randomUUID().toString(),
            courseName = note.courseName,
            title = generated.title,
            summary = generated.summary,
            keyPoints = generated.keyPoints,
            questions = generated.questions,
            keywords = generated.keywords,
            uncertain = generated.uncertain,
            imagePath = "",
            createdAt = System.currentTimeMillis(),
            isMock = generated.isMock,
            status = NoteStatus.SUCCESS,
            errorMessage = "",
            providerName = generated.providerName,
            modelName = generated.modelName,
            latencyMs = generated.latencyMs,
            sourceType = "chat",
            sourceNoteId = note.id
        )
        noteStore.add(newNote)

        if (!isFinishing && !isDestroyed) {
            Toast.makeText(this, R.string.chat_note_saved, Toast.LENGTH_SHORT).show()
            startActivity(NoteDetailActivity.createIntent(this, newNote.id))
            finish()
        }
    }

    companion object {
        private const val EXTRA_NOTE_ID = "extra_note_id"

        fun createIntent(context: Context, noteId: String): Intent {
            return Intent(context, NoteChatActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
            }
        }
    }
}
