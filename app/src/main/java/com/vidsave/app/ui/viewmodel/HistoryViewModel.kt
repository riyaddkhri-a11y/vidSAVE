package com.vidsave.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidsave.app.data.local.VidSaveDatabase
import com.vidsave.app.data.model.DownloadItem
import com.vidsave.app.data.repository.DownloadRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadDao = VidSaveDatabase.getInstance(application).downloadDao()
    private val downloadRepository = DownloadRepository(downloadDao)

    val downloads: StateFlow<List<DownloadItem>> = downloadRepository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            downloadRepository.clearHistory()
        }
    }
}
