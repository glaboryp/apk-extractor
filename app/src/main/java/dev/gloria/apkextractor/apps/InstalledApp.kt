package dev.gloria.apkextractor.apps

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val sourceDir: String,
    val splitSourceDirs: List<String>,
    val versionName: String? = null,
)
