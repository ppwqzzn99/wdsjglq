package com.mimo.datefaker.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * 备用配置读取通道。
 * 当 XSharedPreferences 在个别 ROM 上读不到时，Hook 侧可通过 ContentResolver 查询。
 */
class ConfigContentProvider : ContentProvider() {

    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, "config", CODE_CONFIG)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (matcher.match(uri) != CODE_CONFIG) return null
        val ctx = context ?: return null
        val c = ConfigStore.read(ctx)

        val cursor = MatrixCursor(
            arrayOf(
                ModuleConfig.KEY_ENABLED,
                ModuleConfig.KEY_TARGET_PACKAGE,
                ModuleConfig.KEY_MODE,
                ModuleConfig.KEY_FIXED_MILLIS,
                ModuleConfig.KEY_OFFSET_DAYS,
                ModuleConfig.KEY_SPOOF_TZ,
                ModuleConfig.KEY_TZ_ID,
                ModuleConfig.KEY_SPOOF_BUILD_TIME,
            )
        )
        cursor.addRow(
            arrayOf(
                if (c.enabled) 1 else 0,
                c.targetPackage,
                c.mode,
                c.fixedMillis,
                c.offsetDays,
                if (c.spoofTimezone) 1 else 0,
                c.timezoneId,
                if (c.spoofBuildTime) 1 else 0,
            )
        )
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.$AUTHORITY.config"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        const val AUTHORITY = "com.mimo.datefaker.config"
        const val CODE_CONFIG = 1
        val URI: Uri = Uri.parse("content://$AUTHORITY/config")
    }
}
