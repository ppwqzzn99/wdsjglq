package com.mimo.datefaker.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 伪造系统属性中与构建日期相关的项。
 */
object SystemPropertyHook {

    private val DATE_KEYS = setOf(
        "ro.build.date",
        "ro.build.date.utc",
        "ro.system.build.date",
        "ro.system.build.date.utc",
    )

    fun install(classLoader: ClassLoader?) {
        try {
            val spClass = XposedHelpers.findClass("android.os.SystemProperties", classLoader)
            XposedHelpers.findAndHookMethod(
                spClass,
                "get",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        if (key !in DATE_KEYS) return
                        if (!ConfigProvider.enterHook()) return
                        try {
                            val current = ConfigProvider.load()
                            if (!current.enabled || !current.spoofBuildTime) return
                            val fake = ConfigProvider.spoofedNow(current) ?: return

                            if (key.endsWith(".utc")) {
                                param.result = (fake / 1000L).toString()
                            } else {
                                val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
                                param.result = sdf.format(Date(fake))
                            }
                        } catch (_: Throwable) {
                        } finally {
                            ConfigProvider.exitHook()
                        }
                    }
                }
            )
            XposedBridge.log("[DateSpoofer] SystemPropertyHook installed")
        } catch (t: Throwable) {
            XposedBridge.log("[DateSpoofer] hook SystemProperties failed: $t")
        }
    }
}
