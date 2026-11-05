package com.vidsave.app.data.repository

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vidsave.app.data.model.VideoMetadata
import com.vidsave.app.data.remote.ExtractRequest
import com.vidsave.app.data.remote.TikTokExtractorService
import com.vidsave.app.data.remote.TikWmResponse
import com.vidsave.app.data.remote.TikTokOEmbedResponse
import com.vidsave.app.domain.TikTokUrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class VideoExtractorRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.vidsave.app/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(TikTokExtractorService::class.java)

    /**
     * Resolves redirected short links (vt.tiktok.com, vm.tiktok.com) to canonical TikTok URLs.
     */
    suspend fun resolveUrl(inputUrl: String): String = withContext(Dispatchers.IO) {
        val extractedUrl = TikTokUrlValidator.extractUrlFromText(inputUrl) ?: inputUrl
        return@withContext try {
            val req = Request.Builder()
                .url(extractedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            val resp = okHttpClient.newCall(req).execute()
            val finalUrl = resp.request.url.toString()
            resp.close()
            finalUrl
        } catch (e: Exception) {
            extractedUrl
        }
    }

    /**
     * Extracts TikTok video metadata for a specific TikTok URL.
     */
    suspend fun extractVideo(
        url: String,
        apiEndpointUrl: String = "https://api.vidsave.app/v1/extract"
    ): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        if (!TikTokUrlValidator.isValidTikTokUrl(url)) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid TikTok video URL provided.")
            )
        }

        val resolvedUrl = resolveUrl(url)

        // 1. Try configured backend API first if provided and custom
        if (apiEndpointUrl.isNotBlank() && apiEndpointUrl.startsWith("http") && !apiEndpointUrl.contains("api.vidsave.app")) {
            try {
                val response = apiService.extractVideo(
                    customUrl = apiEndpointUrl,
                    request = ExtractRequest(url = resolvedUrl)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && !body.downloadUrl.isNullOrBlank()) {
                        val videoMeta = VideoMetadata(
                            id = body.id ?: "tik_${System.currentTimeMillis()}",
                            title = body.title ?: "TikTok Video",
                            author = body.author ?: "@creator",
                            authorAvatarUrl = body.authorAvatarUrl,
                            coverUrl = body.coverUrl,
                            durationSeconds = body.durationSeconds ?: 15,
                            downloadUrl = body.downloadUrl,
                            watermarkFree = body.watermarkFree,
                            quality = body.quality ?: "1080p HD",
                            format = body.format ?: "MP4",
                            sizeBytes = body.sizeBytes,
                            originalUrl = resolvedUrl
                        )
                        return@withContext Result.success(videoMeta)
                    } else if (body?.error != null) {
                        return@withContext Result.failure(Exception(body.error))
                    }
                }
            } catch (e: Exception) {
                // Configured endpoint failed, fallback to public extractor
            }
        }

        // 2. Perform direct TikWM API extraction
        try {
            val tikWmResult = extractViaTikWm(resolvedUrl)
            if (tikWmResult != null) {
                return@withContext Result.success(tikWmResult)
            }
        } catch (e: Exception) {
            // TikWM failed, continue to fallback
        }

        // 3. Official TikTok oEmbed metadata lookup fallback
        try {
            val oembedResult = extractViaOEmbed(resolvedUrl)
            if (oembedResult != null) {
                return@withContext Result.success(oembedResult)
            }
        } catch (e: Exception) {
            // Fallback failed
        }

        return@withContext Result.failure(
            Exception("Unable to extract direct video stream for this TikTok post. Please verify the URL or check the backend API server in Settings.")
        )
    }

    /**
     * Queries TikWM public API for direct MP4 stream extraction.
     */
    private fun extractViaTikWm(tiktokUrl: String): VideoMetadata? {
        return try {
            val formBody = FormBody.Builder()
                .add("url", tiktokUrl)
                .build()

            val req = Request.Builder()
                .url("https://www.tikwm.com/api/")
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful || resp.body == null) return null

            val jsonString = resp.body!!.string()
            val adapter = moshi.adapter(TikWmResponse::class.java)
            val tikWmResp = adapter.fromJson(jsonString) ?: return null

            if (tikWmResp.code == 0 && tikWmResp.data != null) {
                val data = tikWmResp.data
                val directVideoUrl = data.play.takeIf { !it.isNullOrBlank() }
                    ?: data.wmplay.takeIf { !it.isNullOrBlank() }
                    ?: return null

                val authorName = data.author?.uniqueId?.let { "@$it" } ?: "@creator"
                val fullPlayUrl = if (directVideoUrl.startsWith("http")) directVideoUrl else "https://www.tikwm.com$directVideoUrl"

                VideoMetadata(
                    id = data.id ?: System.currentTimeMillis().toString(),
                    title = data.title?.takeIf { it.isNotBlank() } ?: "TikTok Video",
                    author = authorName,
                    authorAvatarUrl = data.author?.avatar,
                    coverUrl = data.cover,
                    durationSeconds = data.duration ?: 15,
                    downloadUrl = fullPlayUrl,
                    watermarkFree = !data.play.isNullOrBlank(),
                    quality = "1080p HD",
                    format = "MP4",
                    sizeBytes = null,
                    originalUrl = tiktokUrl
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fallback lookup using TikTok official oEmbed API.
     */
    private fun extractViaOEmbed(tiktokUrl: String): VideoMetadata? {
        return try {
            val oembedUrl = "https://www.tiktok.com/oembed?url=$tiktokUrl"
            val req = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful || resp.body == null) return null

            val jsonString = resp.body!!.string()
            val adapter = moshi.adapter(TikTokOEmbedResponse::class.java)
            val oembed = adapter.fromJson(jsonString) ?: return null

            if (!oembed.title.isNullOrBlank()) {
                val videoId = Regex("/video/(\\d+)").find(tiktokUrl)?.groupValues?.get(1)
                    ?: System.currentTimeMillis().toString()

                VideoMetadata(
                    id = videoId,
                    title = oembed.title ?: "TikTok Video",
                    author = oembed.authorName?.let { "@$it" } ?: "@creator",
                    authorAvatarUrl = null,
                    coverUrl = oembed.thumbnailUrl,
                    durationSeconds = 15,
                    downloadUrl = tiktokUrl, // Note: oEmbed does not provide raw video stream
                    watermarkFree = false,
                    quality = "720p HD",
                    format = "MP4",
                    sizeBytes = null,
                    originalUrl = tiktokUrl
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
