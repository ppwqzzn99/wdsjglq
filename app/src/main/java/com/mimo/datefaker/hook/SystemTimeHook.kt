package com.mimo.datefaker.hook

import com.mimo.datefaker.data.ModuleConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.Calendar
import java.util.Date

/**
 * 核心时间伪装。
 *
 * 绝大多数 Java 层日期 API 最终都会走到 System.currentTimeMillis()：
 *   new Date() / Calendar / Instant.now() / LocalDate.now()
 * 因此优先 Hook System.currentTimeMillis，再补几条常见直读路径。
 *
 * 配置动态读取（ConfigProvider.load），保存后强停目标应用即可生效。
 */
object SystemTimeHook {

    fun install() {
        // 1) System.currentTimeMillis —— 最关键（带重入保护）
        try {
            XposedHelpers.findAndHookMethod(
                System::class.java,
                "currentTimeMillis",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!ConfigProvider.enterHook()) return
                        try {
                            val config = ConfigProvider.load()
                            val fake = ConfigProvider.spoofedNow(config)
                            if (fake != null) param.result = fake
                        } finally {
                            ConfigProvider.exitHook()
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[DateSpoofer] hook currentTimeMillis failed: $t")
        }

        // 2) new Date() —— 防御性覆盖
        try {
            XposedHelpers.findAndHookConstructor(
                Date::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!ConfigProvider.enterHook()) return
                        try {
                            val config = ConfigProvider.load()
                            val fake = ConfigProvider.spoofedNow(config) ?: return
                            (param.thisObject as Date).time = fake
                        } finally {
                            ConfigProvider.exitHook()
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[DateSpoofer] hook Date() failed: $t")
        }

        // 3) Calendar.getTimeInMillis —— 固定模式下直接替换
        try {
            XposedHelpers.findAndHookMethod(
                Calendar::class.java,
                "getTimeInMillis",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!ConfigProvider.enterHook()) return
                        try {
                            val current = ConfigProvider.load()
                            if (!current.enabled) return
                            if (current.mode != ModuleConfig.MODE_FIXED) return
                            val fake = ConfigProvider.spoofedNow(current) ?: return
                            param.result = fake
                        } finally {
                            ConfigProvider.exitHook()
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[DateSpoofer] hook Calendar.getTimeInMillis failed: $t")
        }

        XposedBridge.log("[DateSpoofer] SystemTimeHook installed")
    }
}
