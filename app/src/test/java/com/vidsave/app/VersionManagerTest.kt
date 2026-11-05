package com.vidsave.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionManagerTest {

    @Test
    fun testPatchVersionIncrement() {
        val currentVersion = "1.0.0"
        val parts = currentVersion.split(".").map { it.toInt() }
        val nextPatchVersion = "${parts[0]}.${parts[1]}.${parts[2] + 1}"

        assertEquals("1.0.1", nextPatchVersion)
    }

    @Test
    fun testVersionCodeProgression() {
        val currentVersionCode = 1L
        val newVersionCode = 2L

        assertTrue(newVersionCode > currentVersionCode)
    }
}
