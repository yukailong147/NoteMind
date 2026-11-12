package com.team.classroomnotes

import android.content.ActivityNotFoundException
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.team.classroomnotes.data.NoteItem
import com.team.classroomnotes.data.NoteStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var courseInput: EditText
    private lateinit var captureButton: Button
    private lateinit var sampleButton: Button
    private lateinit var statusText: TextView
    private lateinit var noteCountText: TextView
    private lateinit var emptyStateText: TextView
    private lateinit var notesContainer: LinearLayout
    private lateinit var store: NoteStore

    private var pendingPhotoFile: File? = null
    private var pendingCourseName: String = ""

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        handleCameraResult(success)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = NoteStore(this)
        courseInput = findViewById(R.id.courseInput)
        captureButton = findViewById(R.id.captureButton)
        sampleButton = findViewById(R.id.sampleButton)
        statusText = findViewById(R.id.statusText)
        noteCountText = findViewById(R.id.noteCountText)
        emptyStateText = findViewById(R.id.emptyStateText)
        notesContainer = findViewById(R.id.notesContainer)

        courseInput.setText(getString(R.string.default_course))
        captureButton.setOnClickListener { openSystemCamera() }
        sampleButton.setOnClickListener { createSampleCapture() }

        renderNotes()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) {
            renderNotes()
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

        saveMockNote(persistentPhoto, pendingCourseName.ifBlank { currentCourseName() })
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

        saveMockNote(photoFile, currentCourseName())
    }

    private fun saveMockNote(photoFile: File, courseName: String) {
        captureButton.isEnabled = false
        sampleButton.isEnabled = false
        showMessage(getString(R.string.processing_mock))

        Handler(Looper.getMainLooper()).postDelayed({
            val now = System.currentTimeMillis()
            val title = "第 ${store.loadAll().size + 1} 次课堂记录"
            val note = NoteItem(
                id = UUID.randomUUID().toString(),
                courseName = courseName,
                title = title,
                summary = "板书照片已经成功保存。本版使用固定模拟数据验证笔记创建、本地保存和重新读取流程。",
                keyPoints = listOf(
                    "中序遍历的访问顺序是左子树、根节点、右子树",
                    "递归处理子树时要明确终止条件",
                    "原始板书照片会随笔记一起保存在手机本地"
                ),
                questions = listOf(
                    "如果左子树为空，下一步应该访问哪个节点？",
                    "如何用代码表示二叉树的终止条件？"
                ),
                imagePath = photoFile.absolutePath,
                createdAt = now,
                isMock = true
            )

            store.add(note)
            captureButton.isEnabled = true
            sampleButton.isEnabled = true
            statusText.text = getString(R.string.note_saved)
            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show()
            renderNotes()
        }, 700)
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
            itemView.findViewById<TextView>(R.id.noteMetaText).text = getString(
                R.string.note_meta,
                note.keyPoints.size,
                note.questions.size
            )
            itemView.findViewById<TextView>(R.id.noteDateText).text =
                formatDate(note.createdAt)
            itemView.findViewById<TextView>(R.id.mockBadgeText).visibility =
                if (note.isMock) View.VISIBLE else View.GONE
            itemView.setOnClickListener {
                startActivity(
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
}

