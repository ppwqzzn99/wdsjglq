# DateSpoofer — LSPosed 日期伪装模块

对**指定应用**伪装手机系统日期 / 时间，支持 Android 16（API 36）与 LSPosed。

## 功能

| 能力 | 说明 |
|------|------|
| 目标应用 | 只 Hook 你在作用域中勾选的那一个包名 |
| 固定日期 | 始终返回你设定的 `yyyy-MM-dd HH:mm` |
| 偏移天数 | 在真实时间上整体 +N / -N 天（相对时间仍连续） |
| 系统时间 API | `System.currentTimeMillis`、`new Date()`、`Calendar.getTimeInMillis` |
| 构建时间 | `Build.TIME`、`ro.build.date` / `ro.build.date.utc` |
| 时区（可选） | `TimeZone.getDefault()` |

> 绝大多数 Java 日期 API（`Instant` / `LocalDate` / `SimpleDateFormat` 等）底层都依赖  
> `System.currentTimeMillis()`，因此 Hook 这一条即可覆盖绝大多数 App。

## 工程结构

```
DateSpoofer/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/xposed_init          # Xposed 入口类名
│       ├── java/com/mimo/datefaker/
│       │   ├── data/                   # 配置与应用列表
│       │   ├── hook/                   # MainHook + 各 Hook
│       │   └── ui/                     # 设置界面
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 环境要求

- JDK 17+
- Android Studio Ladybug+（或等价 AGP 8.13）
- compileSdk / targetSdk = **36**（Android 16）
- minSdk = 27
- 设备已安装 **LSPosed**（Zygisk 或 Riru 版本均可，建议最新）

## 编译

1. 用 Android Studio 打开 `DateSpoofer/` 目录  
2. 等待 Gradle Sync 完成  
3. `Build → Build App Bundle(s) / APK(s) → Build APK(s)`  
   或命令行：

```powershell
cd DateSpoofer
.\gradlew.bat assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

> 首次可能需要生成 Gradle Wrapper：  
> `gradle wrapper --gradle-version 8.14.3`  
> 或直接用 Android Studio 打开，它会自动补齐 wrapper。

## 安装与启用

1. 将 APK 安装到手机  
2. 打开 **LSPosed 管理器 → 模块 → 日期伪装 → 启用**  
3. 在模块的 **作用域** 中勾选你要伪装的目标应用（必须与 App 内选的一致）  
4. 打开「日期伪装」App：  
   - 打开总开关  
   - 选择目标应用  
   - 设置固定日期或偏移天数  
   - 点「保存」  
5. **强行停止**目标应用后重新打开，Hook 即生效

## 使用建议

- **固定日期**：适合绕过活动截止、试用到期、签到日期等  
- **偏移天数**：适合需要时间连续流动、但整体前移/后移的场景  
- 若目标 App 使用了 native 层 `clock_gettime` / NTP 校时，本模块（Java 层）无法覆盖，需额外 native hook  
- 系统设置里的「自动确定日期时间」不会被本模块修改；它只影响目标 App 进程内读到的值

## 技术说明

### 入口

`assets/xposed_init` → `com.mimo.datefaker.hook.MainHook`  
实现 `IXposedHookLoadPackage` + `IXposedHookZygoteInit`

### 配置共享

模块 UI 写入 `SharedPreferences("date_spoofer_config")`，  
目标进程通过 `XSharedPreferences` 读取（LSPosed 对此有跨进程支持）。

### 主要 Hook

| 类 | 方法 | 策略 |
|----|------|------|
| `java.lang.System` | `currentTimeMillis` | `beforeHook` 替换返回值 |
| `java.util.Date` | 构造 | `afterHook` 重写 `time` |
| `java.util.Calendar` | `getTimeInMillis` | 固定模式替换 |
| `android.os.Build` | 静态字段 `TIME` | 直接改写 |
| `android.os.SystemProperties` | `get` | 拦截 `ro.build.date*` |
| `java.util.TimeZone` | `getDefault` | 可选时区伪装 |

### 安全与边界

- 不 Hook 模块自身包名，避免界面显示假时间  
- 配置读取失败时默认 `enabled=false`，避免误伤  
- 仅影响目标 App 进程，不改系统全局时钟

## 免责声明

本模块仅供学习、调试与合法测试使用。请遵守目标应用的服务条款与当地法律法规。
