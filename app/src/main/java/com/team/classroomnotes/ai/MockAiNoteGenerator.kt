package com.team.classroomnotes.ai

import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.IOException

class MockAiNoteGenerator : AiNoteGenerator {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var simulatedFailureConsumed = false

    override fun generate(
        courseName: String,
        imageFile: File,
        callback: (Result<GeneratedNote>) -> Unit
    ) {
        mainHandler.postDelayed({
            if (courseName.contains("模拟失败") && !simulatedFailureConsumed) {
                simulatedFailureConsumed = true
                callback(
                    Result.failure(
                        IOException("这是用于测试重试流程的模拟网络错误")
                    )
                )
                return@postDelayed
            }

            callback(
                Result.success(
                    GeneratedNote(
                        title = "二叉树的中序遍历",
                        summary = "板书围绕二叉树中序遍历展开。当前结果由模拟服务生成，用于验证状态、保存和重试流程。",
                        keyPoints = listOf(
                            "访问顺序为左子树、根节点、右子树",
                            "递归处理子树时要明确终止条件",
                            "原始板书照片会随笔记一起保存在手机本地"
                        ),
                        questions = listOf(
                            "如果左子树为空，下一步应该访问哪个节点？",
                            "如何用代码表示二叉树的终止条件？"
                        ),
                        keywords = listOf("二叉树", "中序遍历", "递归"),
                        uncertain = listOf("当前图片内容尚未接入真实视觉模型"),
                        isMock = true
                    )
                )
            )
        }, 900)
    }
}

