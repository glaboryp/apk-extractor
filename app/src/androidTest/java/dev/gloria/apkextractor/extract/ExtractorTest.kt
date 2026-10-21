package dev.gloria.apkextractor.extract

import android.net.Uri
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import dev.gloria.apkextractor.apps.InstalledApp
import dev.gloria.apkextractor.output.OutputStore
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipInputStream

class ExtractorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = OutputStore(context)
    private val extractor = Extractor(store)
    private val created = mutableListOf<Uri>()
    private val tempFiles = mutableListOf<File>()

    private fun tempApk(name: String, size: Int): File =
        File(context.cacheDir, name).apply {
            writeBytes(ByteArray(size) { (it % 199).toByte() })
            tempFiles += this
        }

    private fun app(label: String, base: File, splits: List<File> = emptyList()) =
        InstalledApp("com.example.fake", label, false, base.path, splits.map { it.path }, "1.0")

    private fun downloadsCount(prefix: String): Int =
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
            arrayOf("$prefix%"),
            null,
        )?.use { it.count } ?: 0

    @After
    fun cleanUp() {
        created.forEach(store::deleteQuietly)
        tempFiles.forEach(File::delete)
    }

    @Test
    fun singleApkAppIsExtractedAsPlainApk() = runBlocking {
        val base = File(context.applicationInfo.sourceDir)
        val result = extractor.extract(app("ApkExtractorSingleTest", base)) { _, _ -> }

        result as ExtractResult.Success
        created += result.uri
        assertEquals("ApkExtractorSingleTest_1.0.apk", result.displayName.substringBefore(" ("))
        val saved = context.contentResolver.openInputStream(result.uri)!!.use { it.readBytes() }
        assertArrayEquals(base.readBytes(), saved)
    }

    @Test
    fun appWithSplitsIsBundledIntoApks() = runBlocking {
        val base = tempApk("base.apk", 3000)
        val split = tempApk("split_config.en.apk", 2000)
        val result = extractor.extract(app("ApkExtractorSplitTest", base, listOf(split))) { _, _ -> }

        result as ExtractResult.Success
        created += result.uri
        assertTrue(result.displayName.startsWith("ApkExtractorSplitTest_1.0.apks"))
        val names = ZipInputStream(context.contentResolver.openInputStream(result.uri)!!).use { zip ->
            generateSequence { zip.nextEntry?.name }.toList()
        }
        assertEquals(listOf("base.apk", "split_config.en.apk"), names)
    }

    @Test
    fun unreadableAppFailsAndLeavesNoFile() = runBlocking {
        val missing = File(context.cacheDir, "missing_base.apk")
        val result = extractor.extract(app("ApkExtractorUnreadableTest", missing)) { _, _ -> }

        assertTrue(result is ExtractResult.Failure)
        assertEquals(0, downloadsCount("ApkExtractorUnreadableTest"))
    }

    @Test
    fun cancelledExtractionDeletesPartialOutput() = runBlocking {
        val base = tempApk("big_base.apk", 20_000_000)
        val split = tempApk("big_split.apk", 20_000_000)
        lateinit var job: Job
        job = launch(Dispatchers.Default, start = CoroutineStart.LAZY) {
            extractor.extract(app("ApkExtractorCancelTest", base, listOf(split))) { done, _ ->
                if (done > 1_000_000) job.cancel()
            }
        }
        job.start()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(0, downloadsCount("ApkExtractorCancelTest"))
    }
}
