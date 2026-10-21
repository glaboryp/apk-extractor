package dev.gloria.apkextractor.extract

import dev.gloria.apkextractor.apps.InstalledApp
import java.io.File

sealed interface ResolvedApk {
    data class Single(val file: File) : ResolvedApk
    data class Split(val files: List<File>) : ResolvedApk
    data class Unreadable(val reason: String) : ResolvedApk
}

fun resolveApk(app: InstalledApp): ResolvedApk {
    val files = (listOf(app.sourceDir) + app.splitSourceDirs).map(::File)
    files.firstOrNull { !it.canRead() }?.let {
        return ResolvedApk.Unreadable("No se puede leer ${it.name}")
    }
    return if (files.size == 1) ResolvedApk.Single(files.first()) else ResolvedApk.Split(files)
}
