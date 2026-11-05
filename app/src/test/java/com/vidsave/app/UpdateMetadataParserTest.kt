package com.vidsave.app

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vidsave.app.data.model.UpdateMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UpdateMetadataParserTest {

    @Test
    fun testParseUpdateMetadataJson() {
        val json = """
            {
              "version": "1.0.1",
              "versionCode": 2,
              "releaseName": "VidSave 1.0.1",
              "releaseNotes": "Performance improvements and bug fixes.",
              "downloadUrl": "https://github.com/VidSave/VidSave/releases/download/v1.0.1/app-release.apk",
              "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
              "packageName": "com.vidsave.app",
              "mandatory": false
            }
        """.trimIndent()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val adapter = moshi.adapter(UpdateMetadata::class.java)
        val metadata = adapter.fromJson(json)

        assertNotNull(metadata)
        assertEquals("1.0.1", metadata?.version)
        assertEquals(2, metadata?.versionCode)
        assertEquals("com.vidsave.app", metadata?.packageName)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", metadata?.sha256)
    }
}
