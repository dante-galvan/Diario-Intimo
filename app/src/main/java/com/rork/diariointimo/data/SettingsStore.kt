package com.rork.diariointimo.data

import android.content.Context
import com.rork.diariointimo.i18n.AppLanguage

/** Auto-lock delay options, expressed in minutes (0 means immediately). */
enum class AutoLockDelay(val minutes: Int) {
    IMMEDIATE(0),
    ONE(1),
    FIVE(5),
    FIFTEEN(15),
    NEVER(-1);

    val millis: Long get() = if (minutes <= 0) 0L else minutes * 60_000L

    companion object {
        fun fromMinutes(value: Int): AutoLockDelay =
            entries.firstOrNull { it.minutes == value } ?: ONE
    }
}

/** Appearance options: the warm daylight paper, «Noche de tinta», or the system. */
enum class AppearanceMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromId(value: String?): AppearanceMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** Non-sensitive preferences: language, appearance and auto-lock behaviour. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("diary_settings", Context.MODE_PRIVATE)

    var language: AppLanguage
        get() = AppLanguage.fromId(prefs.getString(KEY_LANGUAGE, AppLanguage.AUTO.id))
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value.id).apply()

    var appearance: AppearanceMode
        get() = AppearanceMode.fromId(prefs.getString(KEY_APPEARANCE, null))
        set(value) = prefs.edit().putString(KEY_APPEARANCE, value.name).apply()

    var autoLock: AutoLockDelay
        get() = AutoLockDelay.fromMinutes(prefs.getInt(KEY_AUTO_LOCK, AutoLockDelay.ONE.minutes))
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK, value.minutes).apply()

    /** Whether the diary may be opened with device biometrics. Entry-only: the password always works. */
    var biometryEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRY, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRY, value).apply()

    /**
     * The user's decision about password protection: null while undecided
     * (first run), "password" once a password exists, "none" when the diary
     * is deliberately kept open.
     */
    var protectionChoice: String?
        get() = prefs.getString(KEY_PROTECTION, null)
        set(value) = prefs.edit().putString(KEY_PROTECTION, value).apply()

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_APPEARANCE = "appearance"
        const val KEY_AUTO_LOCK = "auto_lock"
        const val KEY_BIOMETRY = "biometry_enabled"
        const val KEY_PROTECTION = "protection_choice"
    }
}
