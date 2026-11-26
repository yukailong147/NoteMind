package com.team.classroomnotes

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.team.classroomnotes.data.NoteStore
import com.team.classroomnotes.data.ReviewOutcome
import com.team.classroomnotes.data.ReviewQueue
import com.team.classroomnotes.data.ReviewQuestion
import com.team.classroomnotes.data.ReviewRecord
import com.team.classroomnotes.data.ReviewStore

class ReviewActivity : AppCompatActivity() {
    private lateinit var noteStore: NoteStore
    private lateinit var reviewStore: ReviewStore
    private var queue: List<ReviewQuestion> = emptyList()
    private var currentIndex = 0
    private var againCount = 0

    private lateinit var progressText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var emptyStateText: TextView
    private lateinit var questionSection: LinearLayout
    private lateinit var courseText: TextView
    private lateinit var noteTitleText: TextView
    private lateinit var questionText: TextView
    private lateinit var showAnswerButton: Button
    private lateinit var answerPanel: LinearLayout
    private lateinit var summaryText: TextView
    private lateinit var keyPointsContainer: LinearLayout
    private lateinit var sourceText: TextView
    private lateinit var ratingButtons: LinearLayout
    private lateinit var completionPanel: LinearLayout
    private lateinit var completionMessageText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        noteStore = NoteStore(this)
        reviewStore = ReviewStore(this)
        bindViews()

        findViewById<Button>(R.id.reviewBackButton).setOnClickListener { finish() }
        showAnswerButton.setOnClickListener { revealAnswer() }
        findViewById<Button>(R.id.reviewAgainButton).setOnClickListener {
            recordOutcome(ReviewOutcome.AGAIN)
        }
        findViewById<Button>(R.id.reviewKnownButton).setOnClickListener {
            recordOutcome(ReviewOutcome.KNOWN)
        }
        findViewById<Button>(R.id.reviewRestartButton).setOnClickListener {
            startSession()
        }
        findViewById<Button>(R.id.reviewHomeButton).setOnClickListener { finish() }

        startSession()
    }

    private fun bindViews() {
        progressText = findViewById(R.id.reviewProgressText)
        scrollView = findViewById(R.id.reviewScroll)
        emptyStateText = findViewById(R.id.reviewEmptyState)
        questionSection = findViewById(R.id.reviewQuestionSection)
        courseText = findViewById(R.id.reviewCourseText)
        noteTitleText = findViewById(R.id.reviewNoteTitleText)
        questionText = findViewById(R.id.reviewQuestionText)
        showAnswerButton = findViewById(R.id.reviewShowAnswerButton)
        answerPanel = findViewById(R.id.reviewAnswerPanel)
        summaryText = findViewById(R.id.reviewSummaryText)
        keyPointsContainer = findViewById(R.id.reviewKeyPointsContainer)
        sourceText = findViewById(R.id.reviewSourceText)
        ratingButtons = findViewById(R.id.reviewRatingButtons)
        completionPanel = findViewById(R.id.reviewCompletionPanel)
        completionMessageText = findViewById(R.id.reviewCompletionMessage)
    }

    private fun startSession() {
        queue = ReviewQueue.build(noteStore.loadAll(), reviewStore.loadAll())
        currentIndex = 0
        againCount = 0
        completionPanel.visibility = View.GONE

        if (queue.isEmpty()) {
            progressText.text = ""
            emptyStateText.visibility = View.VISIBLE
            questionSection.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            questionSection.visibility = View.VISIBLE
            renderQuestion()
        }
    }

    private fun renderQuestion() {
        val question = queue[currentIndex]
        progressText.text = getString(
            R.string.review_progress,
            currentIndex + 1,
            queue.size
        )
        courseText.text = question.courseName
        noteTitleText.text = question.noteTitle
        questionText.text = question.questionText
        summaryText.text = question.summary.ifBlank {
            getString(R.string.review_summary_missing)
        }
        renderKeyPoints(question)
        sourceText.text = buildSourceText(question)

        answerPanel.visibility = View.GONE
        ratingButtons.visibility = View.GONE
        showAnswerButton.visibility = View.VISIBLE
        questionSection.visibility = View.VISIBLE
        completionPanel.visibility = View.GONE
        scrollView.post { scrollView.fullScroll(View.FOCUS_UP) }
    }

    private fun renderKeyPoints(question: ReviewQuestion) {
        keyPointsContainer.removeAllViews()
        if (question.keyPoints.isEmpty()) {
            keyPointsContainer.addView(paragraph(getString(R.string.review_key_points_missing)))
            return
        }

        question.keyPoints.forEach { point ->
            keyPointsContainer.addView(paragraph("•  $point"))
        }
    }

    private fun paragraph(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(getColor(R.color.text_primary))
            textSize = 14f
            setLineSpacing(0f, 1.25f)
            setPadding(0, 5, 0, 5)
        }
    }

    private fun buildSourceText(question: ReviewQuestion): String {
        val bookSource = listOf(question.bookName, question.chapter)
            .filter(String::isNotBlank)
            .joinToString(" · ")
        return if (bookSource.isBlank()) {
            getString(R.string.review_only_note_source, question.noteTitle)
        } else {
            getString(R.string.review_source_prefix, bookSource)
        }
    }

    private fun revealAnswer() {
        showAnswerButton.visibility = View.GONE
        answerPanel.visibility = View.VISIBLE
        ratingButtons.visibility = View.VISIBLE
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun recordOutcome(outcome: ReviewOutcome) {
        val question = queue[currentIndex]
        reviewStore.save(
            ReviewRecord(
                questionId = question.id,
                noteId = question.noteId,
                questionText = question.questionText,
                outcome = outcome,
                reviewedAt = System.currentTimeMillis()
            )
        )
        if (outcome == ReviewOutcome.AGAIN) {
            againCount += 1
        }

        currentIndex += 1
        if (currentIndex >= queue.size) {
            showCompletion()
        } else {
            renderQuestion()
        }
    }

    private fun showCompletion() {
        questionSection.visibility = View.GONE
        emptyStateText.visibility = View.GONE
        completionPanel.visibility = View.VISIBLE
        progressText.text = getString(R.string.review_complete_title)
        completionMessageText.text = getString(
            R.string.review_complete_message,
            queue.size,
            againCount
        )
        scrollView.post { scrollView.fullScroll(View.FOCUS_UP) }
    }
}
