package dev.gloria.apkextractor.install

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ApkInstallerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val installer = ApkInstaller(context)

    private fun sessionCount() = context.packageManager.packageInstaller.mySessions.size

    private fun sessionCountSettlesAt(expected: Int): Boolean {
        repeat(30) {
            if (sessionCount() == expected) return true
            Thread.sleep(100)
        }
        return false
    }

    @Test
    fun failureWhileWritingReturnsFailureAndAbandonsSession() = runBlocking {
        val before = sessionCount()

        val result = installer.install(listOf(ApkEntry("a.apk", { throw IllegalStateException("boom") }, 10)))

        assertTrue(result is InstallResult.Failure)
        assertTrue(sessionCountSettlesAt(before))
    }

    @Test
    fun cancellingInstallAbandonsItsSession() = runBlocking {
        val before = sessionCount()
        val started = CompletableDeferred<Unit>()
        val interrupted = CompletableDeferred<Unit>()
        val blocking = ApkEntry(
            "a.apk",
            {
                started.complete(Unit)
                try {
                    Thread.sleep(60_000)
                } catch (e: InterruptedException) {
                    interrupted.complete(Unit)
                    throw e
                }
                ByteArrayInputStream(ByteArray(0))
            },
            10,
        )

        val job = launch(Dispatchers.Default) { installer.install(listOf(blocking)) }
        started.await()
        withTimeout(5_000) { job.cancelAndJoin() }
        withTimeout(5_000) { interrupted.await() }

        assertTrue(sessionCountSettlesAt(before))
    }
}
