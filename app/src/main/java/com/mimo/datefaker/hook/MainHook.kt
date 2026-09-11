package com.mimo.datefaker.hook

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.mimo.datefaker.data.ModuleConfig

/**
 * LSPosed / Xposed 入口。
 * Android 16 + LSPosed 仍完整支持经典 de.robv.android.xposed API。
 */
class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    private var modulePath: String = ""

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
        XposedBridge.log("$TAG zygote init, modulePath=$modulePath")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 不 hook 自己，避免模块界面显示假时间
        if (lpparam.packageName == ModuleConfig.MODULE_PACKAGE) return

        val config = ConfigProvider.load(force = true)
        if (!config.enabled) return
        if (config.targetPackage.isBlank()) return
        if (lpparam.packageName != config.targetPackage) return

        XposedBridge.log("$TAG hooking ${lpparam.packageName} mode=${config.mode}")

        SystemTimeHook.install()
        BuildTimeHook.install(lpparam.classLoader)
        TimezoneHook.install(lpparam.classLoader)
        SystemPropertyHook.install(lpparam.classLoader)
    }

    companion object {
        private const val TAG = "[DateSpoofer]"
    }
}
