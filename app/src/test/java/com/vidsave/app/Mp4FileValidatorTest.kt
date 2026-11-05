package com.vidsave.app

import com.vidsave.app.domain.Mp4FileValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class Mp4FileValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testValidMp4HeaderRecognized() {
        val mp4File = tempFolder.newFile("test_video.mp4")
        // Construct minimal valid MP4 ftyp box header (size 0x00000020, ftypisom...)
        val header = ByteArray(600)
        header[0] = 0x00
        header[1] = 0x00
        header[2] = 0x00
        header[3] = 0x20
        header[4] = 'f'.code.toByte()
        header[5] = 't'.code.toByte()
        header[6] = 'y'.code.toByte()
        header[7] = 'p'.code.toByte()
        header[8] = 'i'.code.toByte()
        header[9] = 's'.code.toByte()
        header[10] = 'o'.code.toByte()
        header[11] = 'm'.code.toByte()

        mp4File.writeBytes(header)

        assertTrue(Mp4FileValidator.isValidMp4File(mp4File))
    }

    @Test
    fun testHtmlResponseRejected() {
        val htmlFile = tempFolder.newFile("test_page.html")
        val htmlContent = ("<!DOCTYPE html><html><head><title>Error</title></head><body>Access Denied</body></html>" + "a".repeat(600)).toByteArray()
        htmlFile.writeBytes(htmlContent)

        assertFalse(Mp4FileValidator.isValidMp4File(htmlFile))
    }

    @Test
    fun testJsonResponseRejected() {
        val jsonFile = tempFolder.newFile("test_error.json")
        val jsonContent = ("{\"error\": \"Video not found\", \"code\": 404}" + "a".repeat(600)).toByteArray()
        jsonFile.writeBytes(jsonContent)

        assertFalse(Mp4FileValidator.isValidMp4File(jsonFile))
    }

    @Test
    fun testEmptyFileRejected() {
        val emptyFile = tempFolder.newFile("empty.mp4")
        assertFalse(Mp4FileValidator.isValidMp4File(emptyFile))
    }
}
