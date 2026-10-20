package dev.gloria.apkextractor.extract

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.gloria.apkextractor.R
import dev.gloria.apkextractor.apps.PackageManagerAppRepository
import dev.gloria.apkextractor.output.OutputStore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class ExtractWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val packageName = inputData.getString(KEY_PACKAGE) ?: return failure("Falta el paquete")
        val app = PackageManagerAppRepository(applicationContext.packageManager).loadApps()
            .firstOrNull { it.packageName == packageName }
            ?: return failure("La app $packageName no está instalada")

        setForegroundSafely(app.label, 0, 0)
        val done = AtomicLong()
        val total = AtomicLong()
        return coroutineScope {
            val reporter = launch {
                while (true) {
                    delay(PROGRESS_INTERVAL_MS)
                    setProgress(workDataOf(KEY_DONE to done.get(), KEY_TOTAL to total.get()))
                    setForegroundSafely(app.label, done.get(), total.get())
                }
            }
            try {
                when (val result = Extractor(OutputStore(applicationContext)).extract(app) { d, t ->
                    done.set(d)
                    total.set(t)
                }) {
                    is ExtractResult.Success -> Result.success(
                        workDataOf(
                            KEY_URI to result.uri.toString(),
                            KEY_NAME to result.displayName,
                            KEY_MIME to result.mime,
                        ),
                    )
                    is ExtractResult.Failure -> failure(result.reason)
                }
            } finally {
                reporter.cancel()
            }
        }
    }

    private fun failure(reason: String) = Result.failure(workDataOf(KEY_REASON to reason))

    private suspend fun setForegroundSafely(label: String, done: Long, total: Long) {
        runCatching { setForeground(foregroundInfo(label, done, total)) }
    }

    private fun foregroundInfo(label: String, done: Long, total: Long): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, applicationContext.getString(R.string.extract_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(applicationContext.getString(R.string.extracting, label))
            .setProgress(100, if (total > 0) (done * 100 / total).toInt() else 0, total == 0L)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        const val KEY_PACKAGE = "packageName"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_URI = "uri"
        const val KEY_NAME = "name"
        const val KEY_MIME = "mime"
        const val KEY_REASON = "reason"
        private const val CHANNEL_ID = "extract"
        private const val NOTIFICATION_ID = 1
        private const val PROGRESS_INTERVAL_MS = 300L
    }
}
