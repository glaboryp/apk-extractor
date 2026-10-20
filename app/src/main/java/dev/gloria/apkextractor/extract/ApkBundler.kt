package dev.gloria.apkextractor.extract

import java.io.File
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.cancellation.CancellationException

private const val BUFFER_SIZE = 64 * 1024

fun bundleSplits(
    files: List<File>,
    out: OutputStream,
    onProgress: (bytesWritten: Long, total: Long) -> Unit,
) {
    val total = files.sumOf { it.length() }
    var written = 0L
    val buffer = ByteArray(BUFFER_SIZE)
    val zip = ZipOutputStream(out).apply { setLevel(Deflater.NO_COMPRESSION) }
    for (file in files) {
        zip.putNextEntry(ZipEntry(file.name))
        file.inputStream().use { input ->
            while (true) {
                if (Thread.currentThread().isInterrupted) throw CancellationException("Extracción cancelada")
                val read = input.read(buffer)
                if (read < 0) break
                zip.write(buffer, 0, read)
                written += read
                onProgress(written, total)
            }
        }
        zip.closeEntry()
    }
    zip.finish()
}
