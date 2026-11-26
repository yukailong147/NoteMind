package com.team.classroomnotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.team.classroomnotes.data.NoteSearchEngine
import com.team.classroomnotes.data.NoteStore
import com.team.classroomnotes.data.SearchField
import com.team.classroomnotes.ui.bindStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteSearchActivity : AppCompatActivity() {
    private lateinit var store: NoteStore
    private lateinit var input: EditText
    private lateinit var statusText: TextView
    private lateinit var resultsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_search)

        store = NoteStore(this)
        input = findViewById(R.id.searchInput)
        statusText = findViewById(R.id.searchStatusText)
        resultsContainer = findViewById(R.id.searchResultsContainer)

        findViewById<Button>(R.id.searchBackButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.searchButton).setOnClickListener { performSearch() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = input.text.toString().trim()
        resultsContainer.removeAllViews()

        if (query.isEmpty()) {
            statusText.text = getString(R.string.search_start_hint)
            return
        }

        val matches = NoteSearchEngine.search(query, store.loadAll())
        if (matches.isEmpty()) {
            statusText.text = getString(R.string.search_no_results)
            return
        }

        statusText.text = getString(R.string.search_count, matches.size)
        val inflater = LayoutInflater.from(this)
        matches.forEach { match ->
            val view = inflater.inflate(R.layout.item_note, resultsContainer, false)
            view.findViewById<TextView>(R.id.noteCourseText).text = match.note.courseName
            view.findViewById<TextView>(R.id.noteTitleText).text = match.note.title
            view.findViewById<TextView>(R.id.noteMetaText).text = getString(
                R.string.search_match_fields,
                match.matchedFields.joinToString("、", transform = ::fieldLabel)
            )
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
            resultsContainer.addView(view)
        }
    }

    private fun fieldLabel(field: SearchField): String {
        val stringId = when (field) {
            SearchField.TITLE -> R.string.field_title
            SearchField.KEYWORDS -> R.string.field_keywords
            SearchField.KEY_POINTS -> R.string.field_key_points
            SearchField.QUESTIONS -> R.string.field_questions
            SearchField.CHAPTER -> R.string.field_chapter
            SearchField.COURSE -> R.string.field_course
            SearchField.BOOK -> R.string.field_book
            SearchField.SUMMARY -> R.string.field_summary
        }
        return getString(stringId)
    }
}
