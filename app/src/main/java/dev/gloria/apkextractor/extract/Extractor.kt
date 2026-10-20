package dev.gloria.apkextractor.extract

import android.net.Uri
import dev.gloria.apkextractor.apps.InstalledApp
import dev.gloria.apkextractor.output.OutputStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

const val APK_MIME = "application/vnd.android.package-archive"
const val BUNDLE_MIME = "application/octet-stream"

sealed interface ExtractResult {
    data class Success(val uri: Uri, val displayName: String, val mime: String) : ExtractResult
    data class Failure(val reason: String) : ExtractResult
}

class Extractor(
    private val store: OutputStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun extract(app: InstalledApp, onProgress: (done: Long, total: Long) -> Unit): ExtractResult =
        when (val resolved = resolveApk(app)) {
            is ResolvedApk.Unreadable -> ExtractResult.Failure(resolved.reason)
            is ResolvedApk.Single ->
                save(baseName(app) + ".apk", APK_MIME) { copyFile(resolved.file, it, onProgress) }
            is ResolvedApk.Split ->
                save(baseName(app) + ".apks", BUNDLE_MIME) { bundleSplits(resolved.files, it, onProgress) }
        }

    private suspend fun save(
        displayName: String,
        mime: String,
        write: (java.io.OutputStream) -> Unit,
    ): ExtractResult = runInterruptible(ioDispatcher) {
        try {
            ExtractResult.Success(store.saveToDownloads(displayName, mime, write), displayName, mime)
        } catch (e: java.io.IOException) {
            ExtractResult.Failure(e.message ?: "Error al guardar el archivo")
        }
    }

    private fun baseName(app: InstalledApp): String {
        val label = app.label.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim().ifEmpty { app.packageName }
        return app.versionName?.let { "${label}_$it" } ?: label
    }
}
