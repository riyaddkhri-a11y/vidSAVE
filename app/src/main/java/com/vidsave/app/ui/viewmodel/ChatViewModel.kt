package com.vidsave.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidsave.app.data.local.VidSaveDatabase
import com.vidsave.app.data.model.ChatMessage
import com.vidsave.app.data.model.DownloadItem
import com.vidsave.app.data.model.MessageStatus
import com.vidsave.app.data.model.SenderType
import com.vidsave.app.data.model.VideoMetadata
import com.vidsave.app.data.repository.DownloadRepository
import com.vidsave.app.data.repository.VideoExtractorRepository
import com.vidsave.app.domain.TikTokUrlValidator
import com.vidsave.app.download.VidSaveDownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isExtracting: Boolean = false,
    val apiEndpointUrl: String = "https://api.vidsave.app/v1/extract",
    val toastMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val extractorRepository = VideoExtractorRepository(application)
    private val downloadDao = VidSaveDatabase.getInstance(application).downloadDao()
    private val downloadRepository = DownloadRepository(downloadDao)
    private val downloadManager = VidSaveDownloadManager(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Welcome message initialization
        val welcomeMsg = ChatMessage(
            sender = SenderType.ASSISTANT,
            text = "Welcome to VidSave! 🎬\n\nPaste any TikTok video link here, and I'll extract the highest available quality for preview and fast download."
        )
        _uiState.update { it.copy(messages = listOf(welcomeMsg)) }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun setApiEndpointUrl(url: String) {
        _uiState.update { it.copy(apiEndpointUrl = url) }
    }

    fun sendUrl(url: String? = null) {
        val targetUrl = url ?: _uiState.value.inputText.trim()
        if (targetUrl.isBlank()) return

        val userMessage = ChatMessage(
            sender = SenderType.USER,
            text = targetUrl
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isExtracting = true
            )
        }

        if (!TikTokUrlValidator.isValidTikTokUrl(targetUrl)) {
            val errorMsg = ChatMessage(
                sender = SenderType.ASSISTANT,
                text = "Invalid TikTok URL. Please paste a valid link (e.g., https://vt.tiktok.com/... or https://www.tiktok.com/@user/video/...)",
                status = MessageStatus.ERROR,
                errorMessage = "Invalid link format"
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + errorMsg,
                    isExtracting = false
                )
            }
            return
        }

        val extractingMessageId = UUID.randomUUID().toString()
        val loadingMessage = ChatMessage(
            id = extractingMessageId,
            sender = SenderType.ASSISTANT,
            text = "Processing video link and extracting renditions...",
            status = MessageStatus.EXTRACTING
        )

        _uiState.update { it.copy(messages = it.messages + loadingMessage) }

        viewModelScope.launch {
            val result = extractorRepository.extractVideo(
                url = targetUrl,
                apiEndpointUrl = _uiState.value.apiEndpointUrl
            )

            _uiState.update { state ->
                val updatedList = state.messages.filter { it.id != extractingMessageId }
                state.copy(messages = updatedList, isExtracting = false)
            }

            if (result.isSuccess) {
                val metadata = result.getOrNull()!!
                val resultMessage = ChatMessage(
                    sender = SenderType.ASSISTANT,
                    text = "Video extracted successfully! Choose quality and download below.",
                    status = MessageStatus.SUCCESS,
                    metadata = metadata
                )
                _uiState.update { it.copy(messages = it.messages + resultMessage) }
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage
                    ?: "Extraction service unavailable."
                val failureMessage = ChatMessage(
                    sender = SenderType.ASSISTANT,
                    text = "Unable to extract video details:\n$errorMsg",
                    status = MessageStatus.ERROR,
                    errorMessage = errorMsg
                )
                _uiState.update { it.copy(messages = it.messages + failureMessage) }
            }
        }
    }

    fun startDownload(messageId: String, metadata: VideoMetadata) {
        viewModelScope.launch {
            val downloadId = downloadManager.startDownload(metadata)

            if (downloadId != -1L) {
                val downloadItem = DownloadItem(
                    downloadId = downloadId,
                    title = metadata.title ?: "TikTok Video",
                    author = metadata.author ?: "@creator",
                    videoUrl = metadata.originalUrl,
                    downloadUrl = metadata.downloadUrl,
                    thumbnailUrl = metadata.coverUrl ?: "",
                    localFilePath = null,
                    isWatermarkFree = metadata.watermarkFree,
                    quality = metadata.quality ?: "HD",
                    status = "DOWNLOADING"
                )

                downloadRepository.insertDownload(downloadItem)

                _uiState.update { state ->
                    val updated = state.messages.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(
                                downloadId = downloadId,
                                downloadState = "DOWNLOADING",
                                downloadProgress = 0
                            )
                        } else msg
                    }
                    state.copy(messages = updated)
                }

                // Track progress
                downloadManager.queryProgress(downloadId).collect { info ->
                    _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.downloadId == downloadId) {
                                msg.copy(
                                    downloadProgress = info.progressPercentage,
                                    downloadState = info.status
                                )
                            } else msg
                        }
                        state.copy(messages = updated)
                    }

                    downloadRepository.updateDownloadStatus(
                        downloadId = downloadId,
                        status = info.status,
                        filePath = info.localUri
                    )
                }
            } else {
                _uiState.update { it.copy(toastMessage = "Failed to start download task.") }
            }
        }
    }

    fun clearChat() {
        val welcomeMsg = ChatMessage(
            sender = SenderType.ASSISTANT,
            text = "Chat history cleared. Send a new TikTok video link to extract and download."
        )
        _uiState.update { it.copy(messages = listOf(welcomeMsg)) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
