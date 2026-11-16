package com.team.classroomnotes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.team.classroomnotes.ai.AiNoteGenerator
import com.team.classroomnotes.ai.AiNoteGeneratorFactory
import com.team.classroomnotes.ai.GeneratedNote
import com.team.classroomnotes.data.NoteItem
import com.team.classroomnotes.data.NoteStatus
import com.team.classroomnotes.data.NoteStore
import com.team.classroomnotes.ui.bindStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDetailActivity : AppCompatActivity() {
    private lateinit var store: NoteStore
    private lateinit var generator: AiNoteGenerator
    private lateinit var note: NoteItem
    private var isGenerating = false

    private lateinit var statusText: TextView
    private lateinit var errorText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var generateButton: Button
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private lateinit var chatButton: Button
    private lateinit var relatedNotesButton: Button
    private lateinit var generationInfoText: TextView
    private lateinit var bookInfoText: TextView
    private lateinit var imageView: ImageView
    private lateinit var sourcePhotoTitle: TextView
    private lateinit var summaryTitle: TextView
    private lateinit var summaryText: TextView
    private lateinit var keyPointsTitle: TextView
    private lateinit var keyPointsContainer: LinearLayout
    private lateinit var questionsTitle: TextView
    private lateinit var questionsContainer: LinearLayout
    private lateinit var uncertainTitle: TextView
    private lateinit var uncertainContainer: LinearLayout
    private lateinit var detailNotice: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        store = NoteStore(this)
        generator = AiNoteGeneratorFactory.create()

        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        val loadedNote = noteId?.let(store::findById)
        if (loadedNote == null) {
            finish()
            return
        }
        note = loadedNote

        bindViews()
        findViewById<TextView>(R.id.backButton).setOnClickListener { finish() }
        generateButton.setOnClickListener { requestGeneration() }
        editButton.setOnClickListener {
            startActivity(EditNoteActivity.createIntent(this, note.id))
        }
        deleteButton.setOnClickListener { confirmDelete() }
        chatButton.setOnClickListener {
            startActivity(NoteChatActivity.createIntent(this, note.id))
        }
        relatedNotesButton.setOnClickListener {
            startActivity(RelatedNotesActivity.createIntent(this, note.id))
        }
        bind(note)

        if (intent.getBooleanExtra(EXTRA_AUTO_GENERATE, false) &&
            (note.status == NoteStatus.DRAFT || note.status == NoteStatus.FAILED)
        ) {
            generateNote()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized && ::note.isInitialized && !isGenerating) {
            store.findById(note.id)?.let(::bind)
        }
    }

    private fun bindViews() {
        statusText = findViewById(R.id.detailStatusText)
        errorText = findViewById(R.id.generationErrorText)
        progressBar = findViewById(R.id.generationProgress)
        generateButton = findViewById(R.id.generateButton)
        editButton = findViewById(R.id.editButton)
        deleteButton = findViewById(R.id.deleteButton)
        chatButton = findViewById(R.id.chatButton)
        relatedNotesButton = findViewById(R.id.relatedNotesButton)
        generationInfoText = findViewById(R.id.generationInfoText)
        bookInfoText = findViewById(R.id.bookInfoText)
        imageView = findViewById(R.id.detailImage)
        sourcePhotoTitle = findViewById(R.id.sourcePhotoTitleText)
        summaryTitle = findViewById(R.id.summaryTitleText)
        summaryText = findViewById(R.id.detailSummaryText)
        keyPointsTitle = findViewById(R.id.keyPointsTitleText)
        keyPointsContainer = findViewById(R.id.keyPointsContainer)
        questionsTitle = findViewById(R.id.questionsTitleText)
        questionsContainer = findViewById(R.id.questionsContainer)
        uncertainTitle = findViewById(R.id.uncertainTitleText)
        uncertainContainer = findViewById(R.id.uncertainContainer)
        detailNotice = findViewById(R.id.detailNoticeText)
    }

    private fun bind(current: NoteItem) {
        note = current
        findViewById<TextView>(R.id.detailCourseText).text = current.courseName
        findViewById<TextView>(R.id.detailTitleText).text = current.title
        findViewById<TextView>(R.id.detailDateText).text = SimpleDateFormat(
            "yyyy 年 MM 月 dd 日 HH:mm",
            Locale.CHINA
        ).format(Date(current.createdAt))

        statusText.bindStatus(current.status, current.isMock)
        if (current.providerName.isNotBlank() && current.modelName.isNotBlank()) {
            generationInfoText.text = getString(
                R.string.generation_info,
                current.providerName,
                current.modelName,
                current.latencyMs / 1000.0
            )
            generationInfoText.visibility = View.VISIBLE
        } else {
            generationInfoText.visibility = View.GONE
        }
        val bindingParts = buildList {
            if (current.bookName.isNotBlank()) add(current.bookName)
            if (current.chapter.isNotBlank()) add(current.chapter)
            if (current.pageNumber.isNotBlank()) add("第 ${current.pageNumber} 页")
        }
        if (bindingParts.isNotEmpty()) {
            bookInfoText.text = bindingParts.joinToString(" · ")
            bookInfoText.visibility = View.VISIBLE
        } else {
            bookInfoText.visibility = View.GONE
        }
        val hasImage = current.imagePath.isNotBlank() && File(current.imagePath).exists()
        sourcePhotoTitle.visibility = if (hasImage) View.VISIBLE else View.GONE
        imageView.visibility = if (hasImage) View.VISIBLE else View.GONE
        if (hasImage) {
            loadScaledBitmap(current.imagePath)?.let(imageView::setImageBitmap)
        } else {
            imageView.setImageDrawable(null)
        }

        errorText.visibility = if (
            current.status == NoteStatus.FAILED &&
            current.errorMessage.isNotBlank()
        ) {
            errorText.text = getString(R.string.ai_failure_prefix, current.errorMessage)
            View.VISIBLE
        } else {
            View.GONE
        }

        val hasResult =
            current.summary.isNotBlank() ||
                current.keyPoints.isNotEmpty() ||
                current.questions.isNotEmpty()
        chatButton.visibility = if (hasResult) View.VISIBLE else View.GONE
        relatedNotesButton.visibility = if (hasResult) View.VISIBLE else View.GONE
        summaryTitle.visibility = if (hasResult) View.VISIBLE else View.GONE
        summaryText.visibility = if (hasResult) View.VISIBLE else View.GONE
        summaryText.text = current.summary

        keyPointsTitle.visibility = if (hasResult) View.VISIBLE else View.GONE
        keyPointsContainer.visibility = if (hasResult) View.VISIBLE else View.GONE
        keyPointsContainer.removeAllViews()
        current.keyPoints.forEach { addBullet(keyPointsContainer, it) }

        questionsTitle.visibility = if (hasResult) View.VISIBLE else View.GONE
        questionsContainer.visibility = if (hasResult) View.VISIBLE else View.GONE
        questionsContainer.removeAllViews()
        current.questions.forEach { addBullet(questionsContainer, it) }

        val showUncertain = hasResult && current.uncertain.isNotEmpty()
        uncertainTitle.visibility = if (showUncertain) View.VISIBLE else View.GONE
        uncertainContainer.visibility = if (showUncertain) View.VISIBLE else View.GONE
        uncertainContainer.removeAllViews()
        current.uncertain.forEach { addBullet(uncertainContainer, it) }

        detailNotice.visibility = if (hasResult) View.VISIBLE else View.GONE
        updateGenerationControls(current.status)
    }

    private fun updateGenerationControls(status: NoteStatus) {
        val shouldShowButton = !isGenerating
        generateButton.visibility = if (shouldShowButton) View.VISIBLE else View.GONE
        generateButton.setText(
            when (status) {
                NoteStatus.DRAFT -> R.string.generate_note
                NoteStatus.FAILED -> R.string.retry_generate_note
                NoteStatus.SUCCESS -> R.string.regenerate_note
                NoteStatus.PROCESSING -> R.string.restart_generate_note
            }
        )
        progressBar.visibility =
            if (isGenerating || status == NoteStatus.PROCESSING) View.VISIBLE else View.GONE
        editButton.isEnabled = !isGenerating
        deleteButton.isEnabled = !isGenerating
        chatButton.isEnabled = !isGenerating
        relatedNotesButton.isEnabled = !isGenerating
    }

    private fun requestGeneration() {
        if (isGenerating) return
        if (note.status == NoteStatus.SUCCESS) {
            AlertDialog.Builder(this)
                .setTitle(R.string.regenerate_confirm_title)
                .setMessage(R.string.regenerate_confirm_message)
                .setPositiveButton(R.string.regenerate_note) { _, _ -> generateNote() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            generateNote()
        }
    }

    private fun generateNote() {
        if (isGenerating || note.status == NoteStatus.SUCCESS) return
        val imageFile = File(note.imagePath)
        if (!imageFile.exists()) {
            fail("原始板书图片不存在，无法重新整理")
            return
        }

        isGenerating = true
        note = store.update(
            note.copy(
                status = NoteStatus.PROCESSING,
                errorMessage = ""
            )
        )
        bind(note)

        generator.generate(note.courseName, imageFile) { result ->
            isGenerating = false
            result.fold(
                onSuccess = ::applyGeneratedNote,
                onFailure = { error ->
                    fail(error.message ?: "AI 整理失败")
                }
            )
        }
    }

    private fun applyGeneratedNote(generated: GeneratedNote) {
        note = store.update(
            note.copy(
                title = generated.title,
                summary = generated.summary,
                keyPoints = generated.keyPoints,
                questions = generated.questions,
                keywords = generated.keywords,
                uncertain = generated.uncertain,
                isMock = generated.isMock,
                status = NoteStatus.SUCCESS,
                errorMessage = "",
                providerName = generated.providerName,
                modelName = generated.modelName,
                latencyMs = generated.latencyMs
            )
        )
        if (!isFinishing && !isDestroyed) {
            bind(note)
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_note_title)
            .setMessage(R.string.delete_note_message)
            .setPositiveButton(R.string.delete_note) { _, _ ->
                val removed = store.delete(note.id)
                if (removed != null) {
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(EXTRA_DELETED_NOTE, true)
                    )
                }
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun fail(message: String) {
        isGenerating = false
        note = store.update(
            note.copy(
                status = NoteStatus.FAILED,
                errorMessage = message
            )
        )
        if (!isFinishing && !isDestroyed) {
            bind(note)
        }
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
        private const val EXTRA_AUTO_GENERATE = "extra_auto_generate"
        const val EXTRA_DELETED_NOTE = "extra_deleted_note"

        fun createIntent(
            context: Context,
            noteId: String,
            autoGenerate: Boolean = false
        ): Intent {
            return Intent(context, NoteDetailActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
                putExtra(EXTRA_AUTO_GENERATE, autoGenerate)
            }
        }
    }
}
