package com.vidsave.app.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.vidsave.app.data.model.DownloadItem
import com.vidsave.app.data.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class VidSaveDownloadManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    data class DownloadProgressInfo(
        val downloadId: Long,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercentage: Int,
        val status: String,
        val localUri: String? = null
    )

    /**
     * Enqueues a video download task using Android DownloadManager.
     */
    fun startDownload(metadata: VideoMetadata): Long {
        val sanitizedTitle = (metadata.title ?: "video")
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            .take(50)
        val fileName = "VidSave_${sanitizedTitle}_${System.currentTimeMillis()}.${metadata.format?.lowercase() ?: "mp4"}"

        val request = DownloadManager.Request(Uri.parse(metadata.downloadUrl)).apply {
            setTitle(metadata.title ?: "VidSave Download")
            setDescription("Downloading TikTok video (${metadata.quality ?: "HD"})")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VidSave/$fileName")
            setAllowedOverRoaming(true)
        }

        return try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Queries the current progress and status of a download ID.
     */
    fun queryProgress(downloadId: Long): Flow<DownloadProgressInfo> = flow {
        var isFinished = false

        while (!isFinished) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                val bytesDownloaded = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                val totalBytes = if (totalBytesIndex >= 0) cursor.getLong(totalBytesIndex) else 0L
                val statusInt = if (statusIndex >= 0) cursor.getInt(statusIndex) else DownloadManager.STATUS_FAILED
                val localUri = if (localUriIndex >= 0) cursor.getString(localUriIndex) else null

                val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0

                val statusString = when (statusInt) {
                    DownloadManager.STATUS_PENDING -> "PENDING"
                    DownloadManager.STATUS_RUNNING -> "DOWNLOADING"
                    DownloadManager.STATUS_PAUSED -> "PAUSED"
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        isFinished = true
                        "COMPLETED"
                    }
                    DownloadManager.STATUS_FAILED -> {
                        isFinished = true
                        "FAILED"
                    }
                    else -> "UNKNOWN"
                }

                emit(
                    DownloadProgressInfo(
                        downloadId = downloadId,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        progressPercentage = progress,
                        status = statusString,
                        localUri = localUri
                    )
                )
            } else {
                isFinished = true
                emit(
                    DownloadProgressInfo(
                        downloadId = downloadId,
                        bytesDownloaded = 0,
                        totalBytes = 0,
                        progressPercentage = 0,
                        status = "FAILED"
                    )
                )
            }

            cursor?.close()

            if (!isFinished) {
                kotlinx.coroutines.delay(500)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Cancels an ongoing download task.
     */
    fun cancelDownload(downloadId: Long): Int {
        return downloadManager.remove(downloadId)
    }
}
