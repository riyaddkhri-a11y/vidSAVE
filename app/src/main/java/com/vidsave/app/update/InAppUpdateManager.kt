package com.vidsave.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.vidsave.app.data.model.UpdateMetadata
import com.vidsave.app.data.repository.UpdateRepository
import com.vidsave.app.domain.ApkVerifier
import com.vidsave.app.domain.Sha256Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val metadata: UpdateMetadata) : UpdateStatus()
    object NoUpdate : UpdateStatus()
    data class Downloading(val progressPercentage: Int, val bytesRead: Long, val totalBytes: Long) : UpdateStatus()
    data class VerificationFailed(val reason: String) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class InAppUpdateManager(private val context: Context) {

    private val updateRepository = UpdateRepository(context)
    private val httpClient = OkHttpClient()

    val currentVersionName: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

    val currentVersionCode: Long
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }

    /**
     * Checks if a newer version is available.
     */
    suspend fun checkForUpdates(
        forceCheck: Boolean = false,
        metadataUrl: String = "https://raw.githubusercontent.com/VidSave/VidSave/main/update-metadata.json"
    ): Result<UpdateMetadata?> = withContext(Dispatchers.IO) {
        val result = updateRepository.checkForUpdates(metadataUrl = metadataUrl, forceCheck = forceCheck)
        
        if (result.isSuccess) {
            val metadata = result.getOrNull()
            if (metadata != null && metadata.versionCode > currentVersionCode) {
                Result.success(metadata)
            } else {
                Result.success(null) // Up to date
            }
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error checking updates."))
        }
    }

    /**
     * Downloads update APK file with real progress updates and verifies integrity.
     */
    fun downloadAndVerifyApk(metadata: UpdateMetadata): Flow<UpdateStatus> = flow {
        emit(UpdateStatus.Downloading(0, 0, 0))

        val updateDir = File(context.cacheDir, "updates")
        if (!updateDir.exists()) {
            updateDir.mkdirs()
        }

        val apkFile = File(updateDir, "VidSave_v${metadata.version}.apk")

        try {
            val request = Request.Builder().url(metadata.downloadUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                emit(UpdateStatus.Error("Failed to download APK from GitHub Release: ${response.code}"))
                return@flow
            }

            val body = response.body!!
            val totalBytes = body.contentLength()
            var bytesRead = 0L

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        val progress = if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt() else 0
                        emit(UpdateStatus.Downloading(progress, bytesRead, totalBytes))
                    }
                    output.flush()
                }
            }

            // Perform Security Verification
            val verification = ApkVerifier.verifyApk(
                context = context,
                apkFile = apkFile,
                expectedPackageName = metadata.packageName,
                expectedSha256 = metadata.sha256,
                currentVersionCode = currentVersionCode,
                expectedCertificateSha256 = metadata.signingCertificateSha256
            )

            if (!verification.isValid) {
                apkFile.delete() // Cleanup corrupted file
                emit(UpdateStatus.VerificationFailed(verification.reason))
            } else {
                emit(UpdateStatus.ReadyToInstall(apkFile))
            }

        } catch (e: Exception) {
            if (apkFile.exists()) apkFile.delete()
            emit(UpdateStatus.Error("Download interrupted or failed: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Launches the Android package installer for the verified APK file using FileProvider.
     */
    fun installApk(apkFile: File): Boolean {
        if (!apkFile.exists()) return false

        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
