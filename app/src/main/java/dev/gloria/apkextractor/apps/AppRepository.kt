package dev.gloria.apkextractor.apps

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

interface AppRepository {
    fun loadApps(): List<InstalledApp>
}

class PackageManagerAppRepository(private val packageManager: PackageManager) : AppRepository {

    override fun loadApps(): List<InstalledApp> =
        packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0)).mapNotNull { pkg ->
            val info = pkg.applicationInfo ?: return@mapNotNull null
            InstalledApp(
                packageName = pkg.packageName,
                label = info.loadLabel(packageManager).toString(),
                isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                sourceDir = info.sourceDir,
                splitSourceDirs = info.splitSourceDirs?.toList().orEmpty(),
                versionName = pkg.versionName,
            )
        }
}
