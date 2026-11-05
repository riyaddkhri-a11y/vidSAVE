package com.vidsave.app

import com.vidsave.app.domain.TikTokUrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TikTokUrlValidatorTest {

    @Test
    fun testValidTikTokUrls() {
        assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://vt.tiktok.com/ZSj9Xy123/"))
        assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://www.tiktok.com/@username/video/7123456789012345678"))
        assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://vm.tiktok.com/ZMJx123/"))
        assertTrue(TikTokUrlValidator.isValidTikTokUrl("Check out this video https://vt.tiktok.com/ZSj9Xy123/"))
    }

    @Test
    fun testInvalidUrls() {
        assertFalse(TikTokUrlValidator.isValidTikTokUrl("https://google.com"))
        assertFalse(TikTokUrlValidator.isValidTikTokUrl("https://youtube.com/watch?v=12345"))
        assertFalse(TikTokUrlValidator.isValidTikTokUrl("not a url at all"))
        assertFalse(TikTokUrlValidator.isValidTikTokUrl(""))
    }

    @Test
    fun testUrlExtractionAndNormalization() {
        val rawMessage = "Hey check out https://vt.tiktok.com/ZSj9Xy123/?share_item_id=123"
        val extracted = TikTokUrlValidator.extractUrlFromText(rawMessage)
        assertEquals("https://vt.tiktok.com/ZSj9Xy123/?share_item_id=123", extracted)

        val normalized = TikTokUrlValidator.normalizeUrl("https://www.tiktok.com/@user/video/7123456789?is_from_webapp=1&sender_device=pc")
        assertEquals("https://www.tiktok.com/@user/video/7123456789", normalized)
    }
}
