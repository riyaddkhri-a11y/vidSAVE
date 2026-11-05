package com.vidsave.app

import com.vidsave.app.domain.TikTokUrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoExtractorRepositoryTest {

    @Test
    fun testTikTokUrlValidation() {
        assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://www.tiktok.com/@mrbeast/video/7325200000000000000"))
        assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://vt.tiktok.com/ZSj9Xy123/"))
        assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://vm.tiktok.com/ZSj9Xy123/"))
    }

    @Test
    fun testUrlExtractionFromText() {
        val text = "Check out this TikTok video https://vt.tiktok.com/ZSj9Xy123/ awesome clip!"
        val extracted = TikTokUrlValidator.extractUrlFromText(text)
        assertEquals("https://vt.tiktok.com/ZSj9Xy123/", extracted)
    }
}
