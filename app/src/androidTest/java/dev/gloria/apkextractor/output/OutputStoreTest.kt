package dev.gloria.apkextractor.output

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutputStoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = OutputStore(context)
    private val created = mutableListOf<Uri>()

    private fun save(name: String, bytes: ByteArray): Uri =
        store.saveToDownloads(name, "application/vnd.android.package-archive") { it.write(bytes) }
            .also { created += it }

    private fun read(uri: Uri): ByteArray? =
        runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()

    @After
    fun cleanUp() {
        created.forEach(store::deleteQuietly)
    }

    @Test
    fun saveToDownloadsWritesTheBytes() {
        val bytes = ByteArray(5000) { (it % 200).toByte() }
        val uri = save("apkextractor_test_write.apk", bytes)
        assertArrayEquals(bytes, read(uri))
    }

    @Test
    fun secondSaveWithSameNameDoesNotReplaceTheFirst() {
        val first = save("apkextractor_test_same.apk", byteArrayOf(1, 2, 3))
        val second = save("apkextractor_test_same.apk", byteArrayOf(9, 9))
        assertNotEquals(first, second)
        assertArrayEquals(byteArrayOf(1, 2, 3), read(first))
        assertArrayEquals(byteArrayOf(9, 9), read(second))
    }

    @Test
    fun deleteQuietlyRemovesTheEntry() {
        val uri = save("apkextractor_test_delete.apk", byteArrayOf(1))
        store.deleteQuietly(uri)
        assertNull(read(uri))
    }

    @Test
    fun failedWriteLeavesNoEntryBehind() {
        val before = downloadsCount("apkextractor_test_fail.apk")
        runCatching {
            store.saveToDownloads("apkextractor_test_fail.apk", "application/octet-stream") {
                throw IllegalStateException("boom")
            }
        }
        assertEquals(before, downloadsCount("apkextractor_test_fail.apk"))
    }

    @Test
    fun shareIntentSendsTheUriWithReadPermission() {
        val uri = save("apkextractor_test_share.apk", byteArrayOf(1))
        val intent = store.shareIntent(uri, "application/vnd.android.package-archive")
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun downloadsCount(name: String): Int =
        context.contentResolver.query(
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(android.provider.MediaStore.Downloads._ID),
            "${android.provider.MediaStore.Downloads.DISPLAY_NAME}=?",
            arrayOf(name),
            null,
        )?.use { it.count } ?: 0
}
