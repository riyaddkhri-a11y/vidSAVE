package com.vidsave.app.data.model

import java.util.UUID

enum class SenderType {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class MessageStatus {
    SENDING,
    EXTRACTING,
    SUCCESS,
    ERROR
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: SenderType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SUCCESS,
    val metadata: VideoMetadata? = null,
    val downloadId: Long? = null,
    val downloadProgress: Int = 0, // 0 to 100
    val downloadState: String? = null, // PENDING, DOWNLOADING, COMPLETED, FAILED
    val errorMessage: String? = null
)
