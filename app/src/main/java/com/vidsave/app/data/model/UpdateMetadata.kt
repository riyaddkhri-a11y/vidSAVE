package com.vidsave.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateMetadata(
    @Json(name = "version") val version: String,
    @Json(name = "versionCode") val versionCode: Int,
    @Json(name = "releaseName") val releaseName: String,
    @Json(name = "releaseNotes") val releaseNotes: String,
    @Json(name = "downloadUrl") val downloadUrl: String,
    @Json(name = "sha256") val sha256: String,
    @Json(name = "packageName") val packageName: String,
    @Json(name = "mandatory") val mandatory: Boolean = false,
    @Json(name = "signingCertificateSha256") val signingCertificateSha256: String? = null
)
