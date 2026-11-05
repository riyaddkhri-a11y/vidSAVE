package com.vidsave.app.data.repository

import com.vidsave.app.data.local.DownloadDao
import com.vidsave.app.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {

    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    suspend fun getDownloadByDownloadId(downloadId: Long): DownloadItem? {
        return downloadDao.getDownloadByDownloadId(downloadId)
    }

    suspend fun insertDownload(item: DownloadItem): Long {
        return downloadDao.insertDownload(item)
    }

    suspend fun updateDownloadStatus(downloadId: Long, status: String, filePath: String?) {
        downloadDao.updateDownloadStatus(downloadId, status, filePath)
    }

    suspend fun deleteDownload(id: Long) {
        downloadDao.deleteDownloadById(id)
    }

    suspend fun clearHistory() {
        downloadDao.clearAllDownloads()
    }
}
