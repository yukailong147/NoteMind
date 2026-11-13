package com.team.classroomnotes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.team.classroomnotes.data.NoteItem
import com.team.classroomnotes.data.NoteStore

class EditNoteActivity : AppCompatActivity() {
    private lateinit var store: NoteStore
    private lateinit var note: NoteItem
    private lateinit var courseInput: EditText
    private lateinit var titleInput: EditText
    private lateinit var summaryInput: EditText
    private lateinit var keyPointsInput: EditText
    private lateinit var questionsInput: EditText
    private lateinit var uncertainInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        store = NoteStore(this)
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        val loaded = noteId?.let(store::findById)
        if (loaded == null) {
            finish()
            return
        }
        note = loaded

        courseInput = findViewById(R.id.editCourseInput)
        titleInput = findViewById(R.id.editTitleInput)
        summaryInput = findViewById(R.id.editSummaryInput)
        keyPointsInput = findViewById(R.id.editKeyPointsInput)
        questionsInput = findViewById(R.id.editQuestionsInput)
        uncertainInput = findViewById(R.id.editUncertainInput)

        courseInput.setText(note.courseName)
        titleInput.setText(note.title)
        summaryInput.setText(note.summary)
        keyPointsInput.setText(note.keyPoints.joinToString("\n"))
        questionsInput.setText(note.questions.joinToString("\n"))
        uncertainInput.setText(note.uncertain.joinToString("\n"))

        findViewById<Button>(R.id.saveEditButton).setOnClickListener { save() }
        findViewById<Button>(R.id.cancelEditButton).setOnClickListener { finish() }
    }

    private fun save() {
        val title = titleInput.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
            return
        }

        val updated = note.copy(
            courseName = courseInput.text.toString().trim()
                .ifBlank { getString(R.string.default_course) },
            title = title,
            summary = summaryInput.text.toString().trim(),
            keyPoints = linesOf(keyPointsInput),
            questions = linesOf(questionsInput),
            uncertain = linesOf(uncertainInput)
        )
        store.update(updated)
        setResult(RESULT_OK)
        Toast.makeText(this, R.string.note_updated, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun linesOf(input: EditText): List<String> {
        return input.text.toString()
            .lineSequence()
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    companion object {
        private const val EXTRA_NOTE_ID = "extra_note_id"

        fun createIntent(context: Context, noteId: String): Intent {
            return Intent(context, EditNoteActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
            }
        }
    }
}
