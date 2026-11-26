package com.team.classroomnotes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.team.classroomnotes.data.NoteItem
import com.team.classroomnotes.data.NoteStore
import com.team.classroomnotes.data.RelatedNoteFinder
import com.team.classroomnotes.ui.bindStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RelatedNotesActivity : AppCompatActivity() {
    private lateinit var store: NoteStore
    private lateinit var currentNote: NoteItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_related_notes)

        store = NoteStore(this)
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        val loaded = noteId?.let(store::findById)
        if (loaded == null) {
            finish()
            return
        }
        currentNote = loaded

        findViewById<Button>(R.id.relatedBackButton).setOnClickListener { finish() }
        renderMatches()
    }

    private fun renderMatches() {
        val matches = RelatedNoteFinder.find(currentNote, store.loadAll())
        findViewById<TextView>(R.id.relatedContextText).text = getString(
            R.string.related_notes_context,
            currentNote.title,
            matches.size
        )

        val container = findViewById<LinearLayout>(R.id.relatedNotesContainer)
        val empty = findViewById<TextView>(R.id.relatedEmptyText)
        container.removeAllViews()

        if (matches.isEmpty()) {
            empty.visibility = View.VISIBLE
            container.addView(empty)
            return
        }

        empty.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        matches.forEach { match ->
            val view = inflater.inflate(R.layout.item_note, container, false)
            view.findViewById<TextView>(R.id.noteCourseText).text = match.note.courseName
            view.findViewById<TextView>(R.id.noteTitleText).text = match.note.title
            view.findViewById<TextView>(R.id.noteMetaText).text =
                if (match.matchedKeywords.isNotEmpty()) {
                    getString(
                        R.string.related_match_keywords,
                        match.matchedKeywords.joinToString("、")
                    )
                } else {
                    getString(R.string.related_match_chapter)
                }
            view.findViewById<TextView>(R.id.noteDateText).text =
                SimpleDateFormat("MM 月 dd 日", Locale.CHINA)
                    .format(Date(match.note.createdAt))
            view.findViewById<TextView>(R.id.statusBadgeText).apply {
                visibility = View.VISIBLE
                bindStatus(match.note.status, match.note.isMock)
            }
            view.setOnClickListener {
                startActivity(NoteDetailActivity.createIntent(this, match.note.id))
            }
            container.addView(view)
        }
    }

    companion object {
        private const val EXTRA_NOTE_ID = "extra_note_id"

        fun createIntent(context: Context, noteId: String): Intent {
            return Intent(context, RelatedNotesActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
            }
        }
    }
}
