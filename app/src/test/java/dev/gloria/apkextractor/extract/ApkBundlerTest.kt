package dev.gloria.apkextractor.extract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.coroutines.cancellation.CancellationException

class ApkBundlerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(name: String, size: Int): File =
        tmp.newFile(name).apply { writeBytes(ByteArray(size) { (it % 251).toByte() }) }

    private fun entries(zip: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zip)).use { input ->
            generateSequence { input.nextEntry }.forEach { result[it.name] = input.readBytes() }
        }
        return result
    }

    @Test
    fun `bundle contains one entry per file with original names`() {
        val base = file("base.apk", 1000)
        val config = file("split_config.en.apk", 500)
        val out = ByteArrayOutputStream()

        bundleSplits(listOf(base, config), out) { _, _ -> }

        val result = entries(out.toByteArray())
        assertEquals(listOf("base.apk", "split_config.en.apk"), result.keys.toList())
        assertTrue(base.readBytes().contentEquals(result.getValue("base.apk")))
        assertTrue(config.readBytes().contentEquals(result.getValue("split_config.en.apk")))
    }

    @Test
    fun `progress reaches total`() {
        val files = listOf(file("base.apk", 150_000), file("split.apk", 70_000))
        val reports = mutableListOf<Pair<Long, Long>>()

        bundleSplits(files, ByteArrayOutputStream()) { done, total -> reports += done to total }

        assertEquals(220_000L, reports.last().first)
        assertTrue(reports.all { it.second == 220_000L })
    }

    @Test
    fun `cancelling via thread interrupt throws CancellationException and stops writing`() {
        val big = file("base.apk", 1_000_000)
        val out = ByteArrayOutputStream()

        try {
            bundleSplits(listOf(big), out) { _, _ -> Thread.currentThread().interrupt() }
            fail("expected CancellationException")
        } catch (_: CancellationException) {
        } finally {
            Thread.interrupted()
        }

        assertTrue(out.size() < 1_000_000)
    }
}
