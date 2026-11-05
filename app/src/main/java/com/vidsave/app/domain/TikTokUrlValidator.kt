package com.vidsave.app.domain

import java.net.URI

object TikTokUrlValidator {

    private val TIKTOK_DOMAINS = listOf(
        "tiktok.com",
        "www.tiktok.com",
        "m.tiktok.com",
        "vt.tiktok.com",
        "vm.tiktok.com",
        "v.tiktok.com"
    )

    /**
     * Checks if the string contains a valid TikTok video URL.
     */
    fun isValidTikTokUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        
        val extractedUrl = extractUrlFromText(trimmed) ?: return false
        
        return try {
            val uri = URI(extractedUrl)
            val host = uri.host?.lowercase() ?: return false
            
            TIKTOK_DOMAINS.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extracts a URL from a text message (user might paste "Check this out https://vt.tiktok.com/...").
     */
    fun extractUrlFromText(text: String): String? {
        val words = text.split("\\s+".toRegex())
        for (word in words) {
            if (word.startsWith("http://") || word.startsWith("https://")) {
                if (isValidDomain(word)) {
                    return word
                }
            } else if (word.contains("tiktok.com")) {
                val fullUrl = "https://$word"
                if (isValidDomain(fullUrl)) {
                    return fullUrl
                }
            }
        }
        return null
    }

    /**
     * Normalizes a TikTok URL into a clean canonical string.
     */
    fun normalizeUrl(url: String): String {
        val extracted = extractUrlFromText(url) ?: url.trim()
        val uri = URI(extracted)
        val scheme = if (uri.scheme == null) "https" else uri.scheme
        val host = uri.host ?: ""
        val path = uri.path ?: ""
        val query = uri.query
        
        // Remove tracking query parameters like share_item_id, _r, etc., keeping clean URL
        return if (query != null && query.contains("is_from_webapp")) {
            "$scheme://$host$path"
        } else {
            extracted
        }
    }

    private fun isValidDomain(url: String): Boolean {
        return try {
            val host = URI(url).host?.lowercase() ?: return false
            TIKTOK_DOMAINS.any { host == it || host.endsWith(".$it") }
        } catch (e: Exception) {
            false
        }
    }
}
