package dev.gloria.apkextractor.output

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

class OutputStore(private val context: Context) {

    private val resolver get() = context.contentResolver

    fun saveToDownloads(displayName: String, mime: String, write: (OutputStream) -> Unit): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
            "No se pudo crear el archivo en Descargas"
        }
        try {
            checkNotNull(resolver.openOutputStream(uri)).use(write)
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Throwable) {
            deleteQuietly(uri)
            throw e
        }
        return uri
    }

    fun deleteQuietly(uri: Uri) {
        runCatching { resolver.delete(uri, null, null) }
    }

    fun shareIntent(uri: Uri, mime: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}
