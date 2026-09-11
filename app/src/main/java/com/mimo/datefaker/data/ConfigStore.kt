package com.mimo.datefaker.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 模块 UI 侧配置读写。写入 SharedPreferences 后由 LSPosed/XSharedPreferences 被目标进程读取。
 */
object ConfigStore {

    fun prefs(context: Context): SharedPreferences {
        // MODE_WORLD_READABLE 在 N+ 上无效，但保留 makeWorldReadable 调用习惯；
        // 真正跨进程读取依赖 LSPosed 对 XSharedPreferences 的支持。
        @Suppress("DEPRECATION")
        @SuppressLint("WorldReadableFiles")
        return context.getSharedPreferences(ModuleConfig.PREF_NAME, Context.MODE_PRIVATE)
    }

    fun read(context: Context): ModuleConfig {
        val p = prefs(context)
        return ModuleConfig(
            enabled = p.getBoolean(ModuleConfig.KEY_ENABLED, false),
            targetPackage = p.getString(ModuleConfig.KEY_TARGET_PACKAGE, "") ?: "",
            mode = p.getInt(ModuleConfig.KEY_MODE, ModuleConfig.MODE_FIXED),
            fixedMillis = p.getLong(ModuleConfig.KEY_FIXED_MILLIS, defaultFixedMillis()),
            offsetDays = p.getInt(ModuleConfig.KEY_OFFSET_DAYS, 0),
            spoofTimezone = p.getBoolean(ModuleConfig.KEY_SPOOF_TZ, false),
            timezoneId = p.getString(ModuleConfig.KEY_TZ_ID, "Asia/Shanghai") ?: "Asia/Shanghai",
            spoofBuildTime = p.getBoolean(ModuleConfig.KEY_SPOOF_BUILD_TIME, true),
        )
    }

    fun write(context: Context, config: ModuleConfig) {
        prefs(context).edit()
            .putBoolean(ModuleConfig.KEY_ENABLED, config.enabled)
            .putString(ModuleConfig.KEY_TARGET_PACKAGE, config.targetPackage)
            .putInt(ModuleConfig.KEY_MODE, config.mode)
            .putLong(ModuleConfig.KEY_FIXED_MILLIS, config.fixedMillis)
            .putInt(ModuleConfig.KEY_OFFSET_DAYS, config.offsetDays)
            .putBoolean(ModuleConfig.KEY_SPOOF_TZ, config.spoofTimezone)
            .putString(ModuleConfig.KEY_TZ_ID, config.timezoneId)
            .putBoolean(ModuleConfig.KEY_SPOOF_BUILD_TIME, config.spoofBuildTime)
            .apply()
        // 尝试 makeWorldReadable（部分环境仍需要）
        try {
            val f = java.io.File(
                context.applicationInfo.dataDir,
                "shared_prefs/${ModuleConfig.PREF_NAME}.xml"
            )
            if (f.exists()) {
                f.setReadable(true, false)
                f.parentFile?.setReadable(true, false)
                f.parentFile?.setExecutable(true, false)
            }
        } catch (_: Throwable) {
        }
    }

    fun defaultFixedMillis(): Long {
        // 默认：今天 12:00
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 12)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun formatDateTime(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
    }

    fun formatDate(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    }

    fun computePreview(config: ModuleConfig, realNow: Long = System.currentTimeMillis()): Long {
        return when (config.mode) {
            ModuleConfig.MODE_FIXED -> if (config.fixedMillis > 0) config.fixedMillis else realNow
            ModuleConfig.MODE_OFFSET -> realNow + config.offsetDays * 86_400_000L
            else -> realNow
        }
    }
}
