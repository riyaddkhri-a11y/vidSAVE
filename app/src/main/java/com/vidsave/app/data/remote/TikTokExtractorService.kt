package com.vidsave.app.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class ExtractRequest(
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class ExtractResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "authorAvatarUrl") val authorAvatarUrl: String? = null,
    @Json(name = "coverUrl") val coverUrl: String? = null,
    @Json(name = "durationSeconds") val durationSeconds: Int? = null,
    @Json(name = "downloadUrl") val downloadUrl: String? = null,
    @Json(name = "watermarkFree") val watermarkFree: Boolean = false,
    @Json(name = "quality") val quality: String? = "1080p",
    @Json(name = "format") val format: String? = "MP4",
    @Json(name = "sizeBytes") val sizeBytes: Long? = null,
    @Json(name = "error") val error: String? = null
)

interface TikTokExtractorService {

    @POST
    suspend fun extractVideo(
        @Url customUrl: String,
        @Body request: ExtractRequest
    ): Response<ExtractResponse>

    @GET
    suspend fun extractVideoGet(
        @Url customUrl: String,
        @Query("url") url: String
    ): Response<ExtractResponse>
}

@JsonClass(generateAdapter = true)
data class TikWmResponse(
    @Json(name = "code") val code: Int? = -1,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "data") val data: TikWmData? = null
)

@JsonClass(generateAdapter = true)
data class TikWmData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "play") val play: String? = null,
    @Json(name = "wmplay") val wmplay: String? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "author") val author: TikWmAuthor? = null
)

@JsonClass(generateAdapter = true)
data class TikWmAuthor(
    @Json(name = "unique_id") val uniqueId: String? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "avatar") val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class TikTokOEmbedResponse(
    @Json(name = "version") val version: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "author_url") val authorUrl: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)
