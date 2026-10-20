package dev.gloria.apkextractor.extract

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.cancellation.CancellationException

private const val BUFFER_SIZE = 64 * 1024

private class ProgressCopier(
    private val total: Long,
    private val onProgress: (bytesWritten: Long, total: Long) -> Unit,
) {
    private var written = 0L
    private val buffer = ByteArray(BUFFER_SIZE)

    fun copy(input: InputStream, out: OutputStream) {
        while (true) {
            if (Thread.currentThread().isInterrupted) throw CancellationException("Extracción cancelada")
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            written += read
            onProgress(written, total)
        }
    }
}

internal fun copyInterruptibly(input: InputStream, out: OutputStream) {
    ProgressCopier(-1) { _, _ -> }.copy(input, out)
}

fun copyFile(file: File, out: OutputStream, onProgress: (bytesWritten: Long, total: Long) -> Unit) {
    file.inputStream().use { ProgressCopier(file.length(), onProgress).copy(it, out) }
}

fun bundleSplits(
    files: List<File>,
    out: OutputStream,
    onProgress: (bytesWritten: Long, total: Long) -> Unit,
) {
    val copier = ProgressCopier(files.sumOf { it.length() }, onProgress)
    val zip = ZipOutputStream(out).apply { setLevel(Deflater.NO_COMPRESSION) }
    for (file in files) {
        zip.putNextEntry(ZipEntry(file.name))
        file.inputStream().use { copier.copy(it, zip) }
        zip.closeEntry()
    }
    zip.finish()
}
