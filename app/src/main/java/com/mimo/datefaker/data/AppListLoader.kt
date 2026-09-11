package com.mimo.datefaker.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)

object AppListLoader {

    fun load(pm: PackageManager, includeSystem: Boolean = false): List<AppInfo> {
        @Suppress("DEPRECATION")
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.asSequence()
            .filter { it.packageName != ModuleConfig.MODULE_PACKAGE }
            .filter { includeSystem || (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(it).toString(),
                    isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
