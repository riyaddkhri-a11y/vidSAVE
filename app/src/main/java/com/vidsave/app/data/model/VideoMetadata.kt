package com.vidsave.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoMetadata(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "authorAvatarUrl") val authorAvatarUrl: String? = null,
    @Json(name = "coverUrl") val coverUrl: String? = null,
    @Json(name = "durationSeconds") val durationSeconds: Int? = null,
    @Json(name = "downloadUrl") val downloadUrl: String,
    @Json(name = "watermarkFree") val watermarkFree: Boolean = false,
    @Json(name = "quality") val quality: String? = "1080p",
    @Json(name = "format") val format: String? = "MP4",
    @Json(name = "sizeBytes") val sizeBytes: Long? = null,
    @Json(name = "originalUrl") val originalUrl: String
)
