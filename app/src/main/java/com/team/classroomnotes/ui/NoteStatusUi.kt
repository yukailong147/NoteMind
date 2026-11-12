package com.team.classroomnotes.ui

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.team.classroomnotes.R
import com.team.classroomnotes.data.NoteStatus

fun TextView.bindStatus(status: NoteStatus, isMock: Boolean) {
    when (status) {
        NoteStatus.DRAFT -> {
            setText(R.string.status_draft)
            setTextColor(context.getColor(R.color.navy))
            setBackgroundResource(R.drawable.bg_status_pending)
        }
        NoteStatus.PROCESSING -> {
            setText(R.string.status_processing)
            setTextColor(context.getColor(R.color.text_primary))
            setBackgroundResource(R.drawable.bg_status_processing)
        }
        NoteStatus.SUCCESS -> {
            setText(if (isMock) R.string.status_success_mock else R.string.status_success)
            setTextColor(context.getColor(R.color.text_primary))
            setBackgroundResource(R.drawable.bg_status_success)
        }
        NoteStatus.FAILED -> {
            setText(R.string.status_failed)
            setTextColor(context.getColor(R.color.text_primary))
            setBackgroundResource(R.drawable.bg_status_failed)
        }
    }
}
