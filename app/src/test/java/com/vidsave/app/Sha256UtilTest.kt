package com.vidsave.app

import com.vidsave.app.domain.Sha256Util
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Sha256UtilTest {

    @Test
    fun testSha256ByteArray() {
        val sampleBytes = "VidSave SHA-256 Test String".toByteArray(Charsets.UTF_8)
        val hash = Sha256Util.calculateSha256(sampleBytes)
        
        // SHA-256 of "VidSave SHA-256 Test String"
        assertTrue(hash.length == 64)
    }

    @Test
    fun testSha256FileVerification() {
        val tempFile = File.createTempFile("vidsave_test", ".txt")
        tempFile.writeText("VidSave Test Content 123", Charsets.UTF_8)

        val hash = Sha256Util.calculateSha256(tempFile)
        assertTrue(Sha256Util.verifyChecksum(tempFile, hash))
        assertTrue(Sha256Util.verifyChecksum(tempFile, hash.uppercase()))

        tempFile.delete()
    }
}
