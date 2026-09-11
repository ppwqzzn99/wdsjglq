package com.mimo.datefaker.hook

import com.mimo.datefaker.data.ModuleConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * 伪造 android.os.Build.TIME。
 */
object BuildTimeHook {

    fun install(classLoader: ClassLoader?) {
        try {
            val buildClass = XposedHelpers.findClass("android.os.Build", classLoader)
            val field = XposedHelpers.findField(buildClass, "TIME")
            field.isAccessible = true
            // 字段写一次即可；偏移模式下后续可读 getTime()
            val cfg = ConfigProvider.load()
            val fake = ConfigProvider.spoofedNow(cfg)
            if (fake != null) {
                field.setLong(null, fake)
                XposedBridge.log("[DateSpoofer] Build.TIME -> $fake")
            }
        } catch (t: Throwable) {
            XposedBridge.log("[DateSpoofer] set Build.TIME failed: $t")
        }

        try {
            val buildClass = XposedHelpers.findClass("android.os.Build", classLoader)
            XposedHelpers.findAndHookMethod(
                buildClass,
                "getTime",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!ConfigProvider.enterHook()) return
                        try {
                            val fake = ConfigProvider.spoofedNow(ConfigProvider.load()) ?: return
                            param.result = fake
                        } finally {
                            ConfigProvider.exitHook()
                        }
                    }
                }
            )
        } catch (_: Throwable) {
        }
    }
}
