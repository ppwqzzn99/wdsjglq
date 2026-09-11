package com.mimo.datefaker.hook

import com.mimo.datefaker.data.ModuleConfig
import de.robv.android.xposed.XSharedPreferences

/**
 * 在目标 App 进程中读取模块配置，并提供无递归的真实时间源。
 *
 * 注意：System.currentTimeMillis 被 Hook 后，任何对它的调用都会重入。
 * 因此提供 [realNow]：重入时直接放行原生方法，拿到真实墙钟。
 */
object ConfigProvider {

    @Volatile
    private var cache: ModuleConfig? = null

    @Volatile
    private var lastLoadAt: Long = 0L

    private const val CACHE_TTL_MS = 2_000L

    /** 重入保护：为 true 时 Hook 直接放行，让原生 currentTimeMillis 执行 */
    private val reentrant = ThreadLocal.withInitial { false }

    fun load(force: Boolean = false): ModuleConfig {
        val now = realNow()
        val cached = cache
        if (!force && cached != null && now - lastLoadAt < CACHE_TTL_MS) {
            return cached
        }

        val config = try {
            loadFromPrefs() ?: loadFromProvider()
        } catch (_: Throwable) {
            ModuleConfig(enabled = false)
        }

        cache = config
        lastLoadAt = now
        return config
    }

    private fun loadFromPrefs(): ModuleConfig? {
        return try {
            val prefs = XSharedPreferences(ModuleConfig.MODULE_PACKAGE, ModuleConfig.PREF_NAME)
            prefs.makeWorldReadable()
            prefs.reload()
            ModuleConfig(
                enabled = prefs.getBoolean(ModuleConfig.KEY_ENABLED, false),
                targetPackage = prefs.getString(ModuleConfig.KEY_TARGET_PACKAGE, "") ?: "",
                mode = prefs.getInt(ModuleConfig.KEY_MODE, ModuleConfig.MODE_FIXED),
                fixedMillis = prefs.getLong(ModuleConfig.KEY_FIXED_MILLIS, 0L),
                offsetDays = prefs.getInt(ModuleConfig.KEY_OFFSET_DAYS, 0),
                spoofTimezone = prefs.getBoolean(ModuleConfig.KEY_SPOOF_TZ, false),
                timezoneId = prefs.getString(ModuleConfig.KEY_TZ_ID, "Asia/Shanghai") ?: "Asia/Shanghai",
                spoofBuildTime = prefs.getBoolean(ModuleConfig.KEY_SPOOF_BUILD_TIME, true),
            )
        } catch (_: Throwable) {
            null
        }
    }

    /** XSharedPreferences 失败时的备用通道（通过 ContentProvider） */
    private fun loadFromProvider(): ModuleConfig {
        return try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApp = activityThread
                .getMethod("currentApplication")
                .invoke(null) as? android.app.Application
            val cr = currentApp?.contentResolver ?: return ModuleConfig(enabled = false)
            val uri = android.net.Uri.parse("content://com.mimo.datefaker.config/config")
            cr.query(uri, null, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return ModuleConfig(enabled = false)
                ModuleConfig(
                    enabled = c.getInt(0) == 1,
                    targetPackage = c.getString(1) ?: "",
                    mode = c.getInt(2),
                    fixedMillis = c.getLong(3),
                    offsetDays = c.getInt(4),
                    spoofTimezone = c.getInt(5) == 1,
                    timezoneId = c.getString(6) ?: "Asia/Shanghai",
                    spoofBuildTime = c.getInt(7) == 1,
                )
            } ?: ModuleConfig(enabled = false)
        } catch (_: Throwable) {
            ModuleConfig(enabled = false)
        }
    }

    /**
     * 获取真实墙钟时间（绕过自身 Hook 重入）。
     * Hook 安装前 / 非重入场景同样可用。
     */
    fun realNow(): Long {
        val was = reentrant.get()
        reentrant.set(true)
        return try {
            System.currentTimeMillis()
        } finally {
            reentrant.set(was)
        }
    }

    fun enterHook(): Boolean {
        if (reentrant.get()) return false
        reentrant.set(true)
        return true
    }

    fun exitHook() {
        reentrant.set(false)
    }

    /**
     * 计算当前应对外展示的 epoch millis。
     * @return null 表示不伪装
     */
    fun spoofedNow(config: ModuleConfig, realNow: Long = realNow()): Long? {
        if (!config.enabled) return null
        return when (config.mode) {
            ModuleConfig.MODE_FIXED -> {
                if (config.fixedMillis > 0L) config.fixedMillis else null
            }
            ModuleConfig.MODE_OFFSET -> {
                if (config.offsetDays == 0) null
                else realNow + config.offsetDays * 86_400_000L
            }
            else -> null
        }
    }
}
