package dev.gloria.apkextractor.apps

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

interface AppRepository {
    fun loadApps(): List<InstalledApp>
}

class PackageManagerAppRepository(private val packageManager: PackageManager) : AppRepository {

    override fun loadApps(): List<InstalledApp> =
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA).map { info ->
            InstalledApp(
                packageName = info.packageName,
                label = info.loadLabel(packageManager).toString(),
                isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                sourceDir = info.sourceDir,
                splitSourceDirs = info.splitSourceDirs?.toList().orEmpty(),
            )
        }
}
