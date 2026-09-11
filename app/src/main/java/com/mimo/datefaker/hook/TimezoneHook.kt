package com.mimo.datefaker.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.TimeZone

/**
 * 伪造默认时区。开启后目标 App 的 TimeZone.getDefault() 返回指定 ID。
 */
object TimezoneHook {

    fun install(classLoader: ClassLoader?) {
        try {
            XposedHelpers.findAndHookMethod(
                TimeZone::class.java,
                "getDefault",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!ConfigProvider.enterHook()) return
                        try {
                            val current = ConfigProvider.load()
                            if (!current.enabled || !current.spoofTimezone) return
                            param.result = TimeZone.getTimeZone(current.timezoneId)
                        } catch (_: Throwable) {
                        } finally {
                            ConfigProvider.exitHook()
                        }
                    }
                }
            )
            XposedBridge.log("[DateSpoofer] TimezoneHook installed")
        } catch (t: Throwable) {
            XposedBridge.log("[DateSpoofer] hook TimeZone.getDefault failed: $t")
        }
    }
}
