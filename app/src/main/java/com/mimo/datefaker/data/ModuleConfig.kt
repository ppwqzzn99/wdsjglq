package com.mimo.datefaker.data

/**
 * 模块配置。通过 XSharedPreferences 在目标进程与模块进程之间共享。
 * PREF_NAME 必须与 UI 写入、Hook 读取保持一致。
 */
data class ModuleConfig(
    val enabled: Boolean = false,
    val targetPackage: String = "",
    /** 0 = 固定日期; 1 = 偏移天数 */
    val mode: Int = MODE_FIXED,
    /** 固定模式下的目标 epoch millis（本地时区解释） */
    val fixedMillis: Long = 0L,
    /** 偏移模式下的天数，可为负 */
    val offsetDays: Int = 0,
    val spoofTimezone: Boolean = false,
    val timezoneId: String = "Asia/Shanghai",
    val spoofBuildTime: Boolean = true,
) {
    companion object {
        const val PREF_NAME = "date_spoofer_config"
        const val MODULE_PACKAGE = "com.mimo.datefaker"

        const val MODE_FIXED = 0
        const val MODE_OFFSET = 1

        const val KEY_ENABLED = "enabled"
        const val KEY_TARGET_PACKAGE = "target_package"
        const val KEY_MODE = "mode"
        const val KEY_FIXED_MILLIS = "fixed_millis"
        const val KEY_OFFSET_DAYS = "offset_days"
        const val KEY_SPOOF_TZ = "spoof_timezone"
        const val KEY_TZ_ID = "timezone_id"
        const val KEY_SPOOF_BUILD_TIME = "spoof_build_time"
    }
}
