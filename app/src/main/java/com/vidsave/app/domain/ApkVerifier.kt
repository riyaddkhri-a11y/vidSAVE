package com.vidsave.app.domain

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

object ApkVerifier {

    data class VerificationResult(
        val isValid: Boolean,
        val reason: String = "",
        val packageName: String? = null,
        val versionCode: Long = 0,
        val versionName: String? = null
    )

    /**
     * Verifies an downloaded APK file against expected parameters:
     * - Package Name matches expected (com.vidsave.app)
     * - VersionCode is strictly greater than current installed version
     * - SHA-256 hash matches expected metadata hash (if provided)
     * - Signing certificate signature matches current app signature (if check enabled)
     */
    fun verifyApk(
        context: Context,
        apkFile: File,
        expectedPackageName: String = "com.vidsave.app",
        expectedSha256: String? = null,
        currentVersionCode: Long = 1L,
        expectedCertificateSha256: String? = null
    ): VerificationResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return VerificationResult(isValid = false, reason = "APK file does not exist or is empty.")
        }

        // 1. Verify SHA-256 checksum if provided
        if (!expectedSha256.isNullOrBlank()) {
            val matchesChecksum = Sha256Util.verifyChecksum(apkFile, expectedSha256)
            if (!matchesChecksum) {
                return VerificationResult(
                    isValid = false,
                    reason = "APK SHA-256 checksum mismatch. File may be corrupted or modified."
                )
            }
        }

        // 2. Parse package info from APK
        val pm = context.packageManager
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNATURES.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
        }

        if (packageInfo == null) {
            return VerificationResult(isValid = false, reason = "Unable to parse APK package information.")
        }

        val parsedPackageName = packageInfo.packageName
        val parsedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        val parsedVersionName = packageInfo.versionName

        // 3. Verify Package Name
        if (parsedPackageName != expectedPackageName) {
            return VerificationResult(
                isValid = false,
                reason = "APK package name ($parsedPackageName) does not match expected package name ($expectedPackageName).",
                packageName = parsedPackageName,
                versionCode = parsedVersionCode,
                versionName = parsedVersionName
            )
        }

        // 4. Verify VersionCode progression (must be greater than installed)
        if (parsedVersionCode <= currentVersionCode) {
            return VerificationResult(
                isValid = false,
                reason = "APK versionCode ($parsedVersionCode) is not newer than current installed versionCode ($currentVersionCode).",
                packageName = parsedPackageName,
                versionCode = parsedVersionCode,
                versionName = parsedVersionName
            )
        }

        // 5. Verify Signing Certificate if expected signature is provided
        if (!expectedCertificateSha256.isNullOrBlank()) {
            val certSha256 = extractApkSigningCertificateSha256(packageInfo)
            val cleanExpected = expectedCertificateSha256.trim().replace(":", "")
            if (certSha256 != null && !certSha256.equals(cleanExpected, ignoreCase = true)) {
                return VerificationResult(
                    isValid = false,
                    reason = "APK signing certificate mismatch with metadata signature.",
                    packageName = parsedPackageName,
                    versionCode = parsedVersionCode,
                    versionName = parsedVersionName
                )
            }
        }

        // 6. Verify Signing Certificate matches current installed app signature
        val installedCertSha256 = getInstalledAppCertificateSha256(context)
        val downloadedCertSha256 = extractApkSigningCertificateSha256(packageInfo)
        if (installedCertSha256 != null && downloadedCertSha256 != null) {
            if (!installedCertSha256.equals(downloadedCertSha256, ignoreCase = true)) {
                return VerificationResult(
                    isValid = false,
                    reason = "SIGNATURE_MISMATCH: Installed app signature differs from update signature.",
                    packageName = parsedPackageName,
                    versionCode = parsedVersionCode,
                    versionName = parsedVersionName
                )
            }
        }

        return VerificationResult(
            isValid = true,
            reason = "APK verification successful.",
            packageName = parsedPackageName,
            versionCode = parsedVersionCode,
            versionName = parsedVersionName
        )
    }

    fun getInstalledAppCertificateSha256(context: Context): String? {
        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNATURES.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            extractApkSigningCertificateSha256(packageInfo)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractApkSigningCertificateSha256(packageInfo: android.content.pm.PackageInfo): String? {
        return try {
            val signatures = @Suppress("DEPRECATION") packageInfo.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                val certBytes = signatures[0].toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(certBytes)
                val sb = StringBuilder()
                for (b in digest) {
                    sb.append(String.format("%02x", b))
                }
                sb.toString()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
