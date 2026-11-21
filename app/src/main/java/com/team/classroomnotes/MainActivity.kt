package com.team.classroomnotes

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.team.classroomnotes.data.NoteItem
import com.team.classroomnotes.data.NoteStore
import com.team.classroomnotes.data.NoteStatus
import com.team.classroomnotes.ui.bindStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var courseInput: EditText
    private lateinit var captureButton: Button
    private lateinit var pickImageButton: Button
    private lateinit var sampleButton: Button
    private lateinit var statusText: TextView
    private lateinit var noteCountText: TextView
    private lateinit var searchNotesButton: Button
    private lateinit var emptyStateText: TextView
    private lateinit var notesContainer: LinearLayout
    private lateinit var undoBar: LinearLayout
    private lateinit var undoMessageText: TextView
    private lateinit var undoButton: Button
    private lateinit var store: NoteStore

    private var pendingPhotoFile: File? = null
    private var pendingCourseName: String = ""
    private var pendingDeletedNote: NoteItem? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var finalizeDeleteRunnable: Runnable? = null

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        handleCameraResult(success)
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { imageUri ->
        handlePickedImage(imageUri)
    }

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val deleted = result.resultCode == Activity.RESULT_OK &&
            result.data?.getBooleanExtra(NoteDetailActivity.EXTRA_DELETED_NOTE, false) == true
        if (deleted) {
            showUndoFromStorage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = NoteStore(this)
        courseInput = findViewById(R.id.courseInput)
        captureButton = findViewById(R.id.captureButton)
        pickImageButton = findViewById(R.id.pickImageButton)
        sampleButton = findViewById(R.id.sampleButton)
        statusText = findViewById(R.id.statusText)
        noteCountText = findViewById(R.id.noteCountText)
        searchNotesButton = findViewById(R.id.searchNotesButton)
        emptyStateText = findViewById(R.id.emptyStateText)
        notesContainer = findViewById(R.id.notesContainer)
        undoBar = findViewById(R.id.undoBar)
        undoMessageText = findViewById(R.id.undoMessageText)
        undoButton = findViewById(R.id.undoButton)

        courseInput.setText(getString(R.string.default_course))
        captureButton.setOnClickListener { openSystemCamera() }
        pickImageButton.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        sampleButton.setOnClickListener { createSampleCapture() }
        searchNotesButton.setOnClickListener {
            startActivity(Intent(this, NoteSearchActivity::class.java))
        }
        undoButton.setOnClickListener { undoDelete() }

        renderNotes()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) {
            renderNotes()
            showUndoFromStorage()
        }
    }

    private fun openSystemCamera() {
        val courseName = currentCourseName()
        val cameraDirectory = File(cacheDir, "camera").apply { mkdirs() }
        val tempFile = File.createTempFile("capture_", ".jpg", cameraDirectory)
        val photoUri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            tempFile
        )

        pendingPhotoFile?.delete()
        pendingPhotoFile = tempFile
        pendingCourseName = courseName

        try {
            takePicture.launch(photoUri)
        } catch (_: ActivityNotFoundException) {
            pendingPhotoFile?.delete()
            pendingPhotoFile = null
            showMessage(getString(R.string.camera_unavailable))
        }
    }

    private fun handleCameraResult(success: Boolean) {
        val tempFile = pendingPhotoFile
        pendingPhotoFile = null

        if (!success || tempFile == null || !tempFile.exists() || tempFile.length() == 0L) {
            tempFile?.delete()
            showMessage(getString(R.string.capture_cancelled))
            return
        }

        val persistentPhoto = copyPhotoToAppStorage(tempFile)
        tempFile.delete()
        if (persistentPhoto == null) {
            showMessage(getString(R.string.capture_failed))
            return
        }

        createDraftAndOpen(
            persistentPhoto,
            pendingCourseName.ifBlank { currentCourseName() }
        )
    }

    private fun handlePickedImage(imageUri: Uri?) {
        if (imageUri == null) return

        val persistentPhoto = copyPickedImageToAppStorage(imageUri)
        if (persistentPhoto == null) {
            showMessage(getString(R.string.pick_image_failed))
            return
        }

        createDraftAndOpen(persistentPhoto, currentCourseName())
    }

    private fun createSampleCapture() {
        val photoDirectory = File(filesDir, "photos").apply { mkdirs() }
        val photoFile = File(photoDirectory, "sample_${System.currentTimeMillis()}.jpg")
        val bitmap = Bitmap.createBitmap(1080, 720, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(250, 250, 247))

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(23, 33, 43)
            textSize = 58f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(36, 67, 92)
            textSize = 42f
            typeface = Typeface.DEFAULT
        }

        canvas.drawText("二叉树的中序遍历", 72f, 145f, titlePaint)
        canvas.drawText("访问顺序：左  →  根  →  右", 72f, 285f, bodyPaint)
        canvas.drawText("先递归左子树", 72f, 385f, bodyPaint)
        canvas.drawText("再访问根节点", 72f, 465f, bodyPaint)
        canvas.drawText("最后递归右子树", 72f, 545f, bodyPaint)
        canvas.drawText("疑问：如果左子树为空，下一步访问谁？", 72f, 650f, bodyPaint)

        FileOutputStream(photoFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        bitmap.recycle()

        createDraftAndOpen(photoFile, currentCourseName())
    }

    private fun createDraftAndOpen(photoFile: File, courseName: String) {
        val note = NoteItem(
            id = UUID.randomUUID().toString(),
            courseName = courseName,
            title = getString(R.string.draft_title),
            summary = "",
            keyPoints = emptyList(),
            questions = emptyList(),
            keywords = emptyList(),
            uncertain = emptyList(),
            imagePath = photoFile.absolutePath,
            createdAt = System.currentTimeMillis(),
            isMock = false,
            status = NoteStatus.DRAFT,
            errorMessage = ""
        )

        store.add(note)
        renderNotes()
        Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show()
        detailLauncher.launch(
            NoteDetailActivity.createIntent(
                context = this,
                noteId = note.id,
                autoGenerate = true
            )
        )
    }

    private fun copyPhotoToAppStorage(source: File): File? = runCatching {
        val photoDirectory = File(filesDir, "photos").apply { mkdirs() }
        val destination = File(photoDirectory, "${UUID.randomUUID()}.jpg")
        source.inputStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destination
    }.getOrNull()

    private fun copyPickedImageToAppStorage(uri: Uri): File? = runCatching {
        val photoDirectory = File(filesDir, "photos").apply { mkdirs() }
        val destination = File(photoDirectory, "${UUID.randomUUID()}.jpg")
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法打开所选图片" }
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destination
    }.getOrNull()

    private fun renderNotes() {
        val notes = store.loadAll()
        notesContainer.removeAllViews()

        noteCountText.text = getString(R.string.note_count, notes.size)
        if (notes.isEmpty()) {
            notesContainer.addView(emptyStateText)
            emptyStateText.visibility = View.VISIBLE
            return
        }

        emptyStateText.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        notes.forEach { note ->
            val itemView = inflater.inflate(R.layout.item_note, notesContainer, false)
            itemView.findViewById<TextView>(R.id.noteCourseText).text = note.courseName
            itemView.findViewById<TextView>(R.id.noteTitleText).text = note.title
            itemView.findViewById<TextView>(R.id.noteMetaText).text =
                if (note.status == NoteStatus.SUCCESS) {
                    getString(
                        R.string.note_meta,
                        note.keyPoints.size,
                        note.questions.size
                    )
                } else {
                    getString(R.string.status_draft)
                }
            itemView.findViewById<TextView>(R.id.noteDateText).text =
                formatDate(note.createdAt)
            itemView.findViewById<TextView>(R.id.statusBadgeText).apply {
                visibility = View.VISIBLE
                bindStatus(note.status, note.isMock)
            }
            itemView.setOnClickListener {
                detailLauncher.launch(
                    NoteDetailActivity.createIntent(this, note.id)
                )
            }
            notesContainer.addView(itemView)
        }
    }

    private fun currentCourseName(): String {
        return courseInput.text.toString().trim()
            .ifBlank { getString(R.string.default_course) }
            .also { courseInput.setText(it) }
    }

    private fun showMessage(message: String) {
        statusText.text = message
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("MM 月 dd 日 HH:mm", Locale.CHINA)
            .format(Date(timestamp))
    }

    private fun showUndoFromStorage() {
        val deleted = store.loadPendingDeletion() ?: return
        if (undoBar.visibility == View.VISIBLE && pendingDeletedNote?.id == deleted.id) return
        finalizePendingDelete()
        pendingDeletedNote = deleted
        undoMessageText.text = getString(R.string.note_deleted)
        undoBar.visibility = View.VISIBLE
        undoBar.bringToFront()
        renderNotes()
        Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show()

        val runnable = Runnable {
            finalizePendingDelete()
            undoBar.visibility = View.GONE
        }
        finalizeDeleteRunnable = runnable
        mainHandler.postDelayed(runnable, UNDO_DURATION_MS)
    }

    private fun undoDelete() {
        finalizeDeleteRunnable?.let(mainHandler::removeCallbacks)
        finalizeDeleteRunnable = null
        pendingDeletedNote?.let(store::add)
        pendingDeletedNote = null
        store.clearPendingDeletion()
        undoBar.visibility = View.GONE
        renderNotes()
        Toast.makeText(this, R.string.note_restored, Toast.LENGTH_SHORT).show()
    }

    private fun finalizePendingDelete() {
        finalizeDeleteRunnable?.let(mainHandler::removeCallbacks)
        finalizeDeleteRunnable = null
        pendingDeletedNote?.let { note ->
            runCatching { File(note.imagePath).delete() }
        }
        val deletedId = pendingDeletedNote?.id
        pendingDeletedNote = null
        if (deletedId != null && store.loadPendingDeletion()?.id == deletedId) {
            store.clearPendingDeletion()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            finalizePendingDelete()
        }
        super.onDestroy()
    }

    companion object {
        private const val UNDO_DURATION_MS = 12_000L
    }
}

