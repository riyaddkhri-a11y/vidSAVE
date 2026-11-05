package com.vidsave.app.domain

import java.io.File
import java.io.FileInputStream

object Mp4FileValidator {

    /**
     * Validates that a file exists, is non-empty, and possesses valid MP4 file signature ('ftyp' box).
     * Rejects HTML pages, JSON error responses, thumbnails, and corrupted payloads.
     */
    fun isValidMp4File(file: File): Boolean {
        if (!file.exists() || file.length() < 512) {
            return false
        }

        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(64)
                val bytesRead = fis.read(header)
                if (bytesRead < 12) return false

                // Check for 'ftyp' magic bytes at offset 4..7
                val isFtyp = header[4] == 'f'.code.toByte() &&
                             header[5] == 't'.code.toByte() &&
                             header[6] == 'y'.code.toByte() &&
                             header[7] == 'p'.code.toByte()

                // Reject HTML or JSON text payloads
                val firstChars = String(header, 0, minOf(bytesRead, 32), Charsets.UTF_8).lowercase()
                val isTextPayload = firstChars.contains("html") ||
                                    firstChars.contains("<!doc") ||
                                    firstChars.contains("{\n") ||
                                    firstChars.contains("{\"") ||
                                    firstChars.contains("<xml")

                isFtyp && !isTextPayload
            }
        } catch (e: Exception) {
            false
        }
    }
}
