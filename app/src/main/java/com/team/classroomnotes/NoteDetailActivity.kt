package com.team.classroomnotes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.team.classroomnotes.data.NoteItem
import com.team.classroomnotes.data.NoteStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        val note = noteId?.let { NoteStore(this).findById(it) }
        if (note == null) {
            finish()
            return
        }

        findViewById<TextView>(R.id.backButton).setOnClickListener { finish() }
        bind(note)
    }

    private fun bind(note: NoteItem) {
        findViewById<TextView>(R.id.detailCourseText).text = note.courseName
        findViewById<TextView>(R.id.detailTitleText).text = note.title
        findViewById<TextView>(R.id.detailDateText).text = SimpleDateFormat(
            "yyyy 年 MM 月 dd 日 HH:mm",
            Locale.CHINA
        ).format(Date(note.createdAt))
        findViewById<TextView>(R.id.detailSummaryText).text = note.summary

        val imageView = findViewById<ImageView>(R.id.detailImage)
        loadScaledBitmap(note.imagePath)?.let(imageView::setImageBitmap)

        val keyPointContainer = findViewById<LinearLayout>(R.id.keyPointsContainer)
        note.keyPoints.forEach { addBullet(keyPointContainer, it) }

        val questionContainer = findViewById<LinearLayout>(R.id.questionsContainer)
        note.questions.forEach { addBullet(questionContainer, it) }
    }

    private fun addBullet(container: LinearLayout, text: String) {
        val paragraph = TextView(this).apply {
            this.text = "•  $text"
            setTextColor(getColor(R.color.text_primary))
            textSize = 14f
            setLineSpacing(0f, 1.25f)
            setPadding(0, 4, 0, 4)
        }
        container.addView(paragraph)
    }

    private fun loadScaledBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 1800 || bounds.outHeight / sampleSize > 1800) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        return BitmapFactory.decodeFile(path, options)
    }

    companion object {
        private const val EXTRA_NOTE_ID = "extra_note_id"

        fun createIntent(context: Context, noteId: String): Intent {
            return Intent(context, NoteDetailActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
            }
        }
    }
}

