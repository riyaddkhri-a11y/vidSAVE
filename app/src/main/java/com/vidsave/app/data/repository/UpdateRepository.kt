package com.vidsave.app.data.repository

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vidsave.app.data.model.UpdateMetadata
import com.vidsave.app.data.remote.UpdateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class UpdateRepository(private val context: Context) {

    private val PREFS_NAME = "vidsave_update_prefs"
    private val KEY_LAST_CHECK = "last_update_check_time"
    private val KEY_CACHED_METADATA = "cached_update_metadata"
    private val CACHE_DURATION_MS = 60 * 60 * 1000L // 1 hour

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val updateApiService = retrofit.create(UpdateApiService::class.java)

    /**
     * Checks for updates from GitHub Release metadata URL.
     * @param metadataUrl URL to update-metadata.json file or API endpoint
     * @param forceCheck If true, bypasses 1-hour cache
     */
    suspend fun checkForUpdates(
        metadataUrl: String = "https://raw.githubusercontent.com/riyaddkhri-a11y/vidSAVE/main/update-metadata.json",
        forceCheck: Boolean = false
    ): Result<UpdateMetadata?> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
        val currentTime = System.currentTimeMillis()

        if (!forceCheck && (currentTime - lastCheck < CACHE_DURATION_MS)) {
            val cachedJson = prefs.getString(KEY_CACHED_METADATA, null)
            if (!cachedJson.isNullOrBlank()) {
                try {
                    val adapter = moshi.adapter(UpdateMetadata::class.java)
                    val metadata = adapter.fromJson(cachedJson)
                    if (metadata != null) {
                        return@withContext Result.success(metadata)
                    }
                } catch (e: Exception) {
                    // Fallthrough to fetch fresh
                }
            }
        }

        try {
            val response = updateApiService.fetchUpdateMetadata(metadataUrl)
            if (response.isSuccessful && response.body() != null) {
                val metadata = response.body()!!
                
                // Cache metadata
                val adapter = moshi.adapter(UpdateMetadata::class.java)
                val jsonString = adapter.toJson(metadata)
                
                prefs.edit()
                    .putLong(KEY_LAST_CHECK, currentTime)
                    .putString(KEY_CACHED_METADATA, jsonString)
                    .apply()

                return@withContext Result.success(metadata)
            } else {
                return@withContext Result.failure(
                    Exception("Failed to fetch update metadata from server: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}
