package com.vidsave.app.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.vidsave.app.data.model.VideoMetadata
import com.vidsave.app.domain.Mp4FileValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class VidSaveDownloadManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val idCounter = AtomicLong(System.currentTimeMillis())
    private val activeTasks = ConcurrentHashMap<Long, ActiveTask>()

    private data class ActiveTask(
        val downloadId: Long,
        val metadata: VideoMetadata,
        @Volatile var isCancelled: Boolean = false
    )

    data class DownloadProgressInfo(
        val downloadId: Long,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercentage: Int,
        val status: String,
        val localUri: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Enqueues a video download task.
     */
    fun startDownload(metadata: VideoMetadata): Long {
        val downloadId = idCounter.incrementAndGet()
        val task = ActiveTask(downloadId = downloadId, metadata = metadata)
        activeTasks[downloadId] = task
        return downloadId
    }

    /**
     * Executes video binary download, validates MP4 signature, and saves to Movies/VidSave/ via MediaStore.
     */
    fun queryProgress(downloadId: Long): Flow<DownloadProgressInfo> = flow {
        val task = activeTasks[downloadId]
        if (task == null) {
            emit(
                DownloadProgressInfo(
                    downloadId = downloadId,
                    bytesDownloaded = 0,
                    totalBytes = 0,
                    progressPercentage = 0,
                    status = "FAILED",
                    errorMessage = "Download task not found."
                )
            )
            return@flow
        }

        val metadata = task.metadata
        val sanitizedTitle = (metadata.title ?: "video")
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            .take(40)
        val fileName = "VidSave_${sanitizedTitle}_${System.currentTimeMillis()}.mp4"
        val tempFile = File(context.cacheDir, "temp_$fileName")

        try {
            emit(DownloadProgressInfo(downloadId, 0, 0, 0, "DOWNLOADING"))

            val req = Request.Builder()
                .url(metadata.downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful || resp.body == null) {
                emit(DownloadProgressInfo(downloadId, 0, 0, 0, "FAILED", errorMessage = "Server returned status ${resp.code}"))
                activeTasks.remove(downloadId)
                return@flow
            }

            val body = resp.body!!
            val totalBytes = body.contentLength()
            var bytesDownloaded = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16384)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (task.isCancelled) {
                            output.flush()
                            if (tempFile.exists()) tempFile.delete()
                            emit(DownloadProgressInfo(downloadId, bytesDownloaded, totalBytes, 0, "CANCELLED"))
                            activeTasks.remove(downloadId)
                            return@flow
                        }
                        output.write(buffer, 0, read)
                        bytesDownloaded += read
                        val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
                        emit(DownloadProgressInfo(downloadId, bytesDownloaded, totalBytes, progress, "DOWNLOADING"))
                    }
                    output.flush()
                }
            }

            // 1. Validate MP4 File Signature & Content
            if (!Mp4FileValidator.isValidMp4File(tempFile)) {
                if (tempFile.exists()) tempFile.delete()
                emit(DownloadProgressInfo(downloadId, 0, 0, 0, "FAILED", errorMessage = "Downloaded payload is not a valid MP4 video stream."))
                activeTasks.remove(downloadId)
                return@flow
            }

            // 2. Save Valid MP4 to Movies/VidSave/ via MediaStore
            val savedUri = saveToMoviesDirectory(context, tempFile, fileName)
            if (tempFile.exists()) tempFile.delete()

            if (savedUri != null) {
                emit(
                    DownloadProgressInfo(
                        downloadId = downloadId,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = bytesDownloaded,
                        progressPercentage = 100,
                        status = "COMPLETED",
                        localUri = savedUri.toString()
                    )
                )
            } else {
                emit(DownloadProgressInfo(downloadId, 0, 0, 0, "FAILED", errorMessage = "Failed to save video to Gallery."))
            }

        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            emit(DownloadProgressInfo(downloadId, 0, 0, 0, "FAILED", errorMessage = e.localizedMessage))
        } finally {
            activeTasks.remove(downloadId)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Saves a validated MP4 file to Movies/VidSave/ directory using MediaStore.
     */
    private fun saveToMoviesDirectory(context: Context, sourceFile: File, fileName: String): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VidSave")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    /**
     * Cancels an ongoing download task.
     */
    fun cancelDownload(downloadId: Long): Int {
        val task = activeTasks[downloadId]
        return if (task != null) {
            task.isCancelled = true
            1
        } else 0
    }
}
