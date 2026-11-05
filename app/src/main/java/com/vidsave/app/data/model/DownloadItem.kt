package com.vidsave.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val downloadId: Long = -1L,
    val title: String,
    val author: String,
    val videoUrl: String,
    val downloadUrl: String,
    val thumbnailUrl: String,
    val localFilePath: String?,
    val isWatermarkFree: Boolean,
    val quality: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String // PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
)
