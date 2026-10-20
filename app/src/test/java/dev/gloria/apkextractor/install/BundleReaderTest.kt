package dev.gloria.apkextractor.install

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BundleReaderTest {

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun read(bytes: ByteArray, name: String) = readBundle({ ByteArrayInputStream(bytes) }, name)

    @Test
    fun `plain apk yields one entry`() {
        val bytes = byteArrayOf(1, 2, 3)

        val result = read(bytes, "game.apk") as BundleContents.Apks

        assertEquals(listOf("game.apk"), result.entries.map { it.name })
        assertArrayEquals(bytes, result.entries.single().open().readBytes())
        assertFalse(result.hasObb)
    }

    @Test
    fun `apks zip yields all root apks`() {
        val base = ByteArray(300) { 1 }
        val config = ByteArray(200) { 2 }
        val bundle = zip(
            "base.apk" to base,
            "toc.pb" to byteArrayOf(9),
            "split_config.en.apk" to config,
        )

        val result = read(bundle, "game.apks") as BundleContents.Apks

        assertEquals(listOf("base.apk", "split_config.en.apk"), result.entries.map { it.name })
        assertArrayEquals(config, result.entries[1].open().readBytes())
        assertArrayEquals(base, result.entries[0].open().readBytes())
    }

    @Test
    fun `xapk with obb folder sets hasObb`() {
        val bundle = zip(
            "manifest.json" to byteArrayOf(1),
            "com.game.apk" to byteArrayOf(2),
            "Android/obb/com.game/main.1.com.game.obb" to byteArrayOf(3),
        )

        val result = read(bundle, "game.xapk") as BundleContents.Apks

        assertEquals(listOf("com.game.apk"), result.entries.map { it.name })
        assertTrue(result.hasObb)
    }

    @Test
    fun `zip without apk entries is Invalid`() {
        val result = read(zip("readme.txt" to byteArrayOf(1)), "stuff.zip")

        assertTrue(result is BundleContents.Invalid)
    }

    @Test
    fun `entry with parent directory traversal is Invalid`() {
        val result = read(zip("../evil.apk" to byteArrayOf(1), "base.apk" to byteArrayOf(2)), "game.apks")

        assertTrue(result is BundleContents.Invalid)
    }

    @Test
    fun `file that is not a zip is Invalid`() {
        val result = read(byteArrayOf(1, 2, 3, 4), "game.apks")

        assertTrue(result is BundleContents.Invalid)
    }
}
