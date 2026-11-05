package com.vidsave.app.domain

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object Sha256Util {

    /**
     * Calculates the SHA-256 hash of a file.
     */
    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return bytesToHex(digest.digest())
    }

    /**
     * Calculates the SHA-256 hash of a ByteArray.
     */
    fun calculateSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return bytesToHex(digest.digest(bytes))
    }

    /**
     * Verifies if a file matches the expected SHA-256 hash string.
     */
    fun verifyChecksum(file: File, expectedHash: String): Boolean {
        if (!file.exists()) return false
        val calculated = calculateSha256(file)
        return calculated.equals(expectedHash.trim(), ignoreCase = true)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
