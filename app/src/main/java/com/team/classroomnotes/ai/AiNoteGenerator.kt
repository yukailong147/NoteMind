package com.team.classroomnotes.ai

import java.io.File

interface AiNoteGenerator {
    fun generate(
        courseName: String,
        imageFile: File,
        callback: (Result<GeneratedNote>) -> Unit
    )
}

