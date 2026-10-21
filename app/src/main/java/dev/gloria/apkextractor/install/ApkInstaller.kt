package dev.gloria.apkextractor.install

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import dev.gloria.apkextractor.extract.copyInterruptibly
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

sealed interface InstallResult {
    data object Success : InstallResult
    data class Failure(val message: String) : InstallResult
}

private val pendingResults = ConcurrentHashMap<Int, CompletableDeferred<InstallResult>>()

class ApkInstaller(private val context: Context) {

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    suspend fun install(entries: List<ApkEntry>): InstallResult {
        val installer = context.packageManager.packageInstaller
        var sessionId = -1
        try {
            sessionId = installer.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))
            val result = CompletableDeferred<InstallResult>()
            pendingResults[sessionId] = result
            runInterruptible(Dispatchers.IO) {
                installer.openSession(sessionId).use { session ->
                    for (entry in entries) {
                        entry.open().use { input ->
                            session.openWrite(entry.name, 0, entry.size).use { out ->
                                copyInterruptibly(input, out)
                                session.fsync(out)
                            }
                        }
                    }
                    session.commit(statusIntentSender(sessionId))
                }
            }
            return result.await()
        } catch (e: CancellationException) {
            abandon(installer, sessionId)
            throw e
        } catch (e: Exception) {
            abandon(installer, sessionId)
            return InstallResult.Failure(e.message ?: "Error al instalar")
        } finally {
            pendingResults.remove(sessionId)
        }
    }

    private fun abandon(installer: PackageInstaller, sessionId: Int) {
        if (sessionId >= 0) runCatching { installer.abandonSession(sessionId) }
    }

    private fun statusIntentSender(sessionId: Int) = PendingIntent.getBroadcast(
        context,
        sessionId,
        Intent(context, InstallStatusReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    ).intentSender
}

class InstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> pendingResults[sessionId]?.complete(InstallResult.Success)
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Error $status"
                pendingResults[sessionId]?.complete(InstallResult.Failure(message))
            }
        }
    }
}
