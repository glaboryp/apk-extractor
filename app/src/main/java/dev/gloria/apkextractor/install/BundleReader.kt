package dev.gloria.apkextractor.install

import java.io.InputStream
import java.util.zip.ZipInputStream

data class ApkEntry(val name: String, val open: () -> InputStream, val size: Long)

sealed interface BundleContents {
    data class Apks(val entries: List<ApkEntry>, val hasObb: Boolean) : BundleContents
    data class Invalid(val reason: String) : BundleContents
}

fun readBundle(open: () -> InputStream, fileName: String): BundleContents {
    if (fileName.endsWith(".apk", ignoreCase = true)) {
        return BundleContents.Apks(listOf(ApkEntry(fileName, open, size = -1)), hasObb = false)
    }

    val entries = mutableListOf<ApkEntry>()
    var hasObb = false
    ZipInputStream(open()).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name
            if (isUnsafe(name)) return BundleContents.Invalid("El paquete contiene una ruta no permitida: $name")
            when {
                name.startsWith("Android/obb/") -> hasObb = true
                !entry.isDirectory && '/' !in name && name.endsWith(".apk", ignoreCase = true) ->
                    entries += ApkEntry(name, { openEntry(open, name) }, entry.size)
            }
        }
    }
    if (entries.isEmpty()) return BundleContents.Invalid("El archivo no contiene ningún APK")
    return BundleContents.Apks(entries, hasObb)
}

private fun isUnsafe(name: String): Boolean =
    name.startsWith("/") || name.contains('\\') || name.split('/').any { it == ".." }

private fun openEntry(open: () -> InputStream, name: String): InputStream {
    val zip = ZipInputStream(open())
    while (true) {
        val entry = zip.nextEntry
        if (entry == null) {
            zip.close()
            throw java.io.FileNotFoundException(name)
        }
        if (entry.name == name) return zip
    }
}
