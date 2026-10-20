package dev.gloria.apkextractor.extract

import android.net.Uri
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import dev.gloria.apkextractor.output.OutputStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractWorkerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun run(packageName: String): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<ExtractWorker>(context)
            .setInputData(workDataOf(ExtractWorker.KEY_PACKAGE to packageName))
            .build()
            .doWork()
    }

    @Test
    fun extractsInstalledPackageToDownloads() {
        val result = run(context.packageName)

        result as ListenableWorker.Result.Success
        val uri: Uri = result.outputData.getString(ExtractWorker.KEY_URI)!!.toUri()
        assertNotNull(context.contentResolver.openInputStream(uri))
        assertTrue(result.outputData.getString(ExtractWorker.KEY_NAME)!!.endsWith(".apk"))
        OutputStore(context).deleteQuietly(uri)
    }

    @Test
    fun unknownPackageFailsWithReason() {
        val result = run("com.example.not.installed")

        result as ListenableWorker.Result.Failure
        assertEquals(true, result.outputData.getString(ExtractWorker.KEY_REASON)?.isNotBlank())
    }
}
