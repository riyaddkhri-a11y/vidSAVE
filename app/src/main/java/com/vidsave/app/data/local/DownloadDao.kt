package com.vidsave.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vidsave.app.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_history ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM download_history WHERE downloadId = :downloadId LIMIT 1")
    suspend fun getDownloadByDownloadId(downloadId: Long): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(downloadItem: DownloadItem): Long

    @Query("UPDATE download_history SET status = :status, localFilePath = :filePath WHERE downloadId = :downloadId")
    suspend fun updateDownloadStatus(downloadId: Long, status: String, filePath: String?)

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM download_history")
    suspend fun clearAllDownloads()
}
