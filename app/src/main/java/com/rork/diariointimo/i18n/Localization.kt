package com.rork.diariointimo.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/**
 * Supported interface languages. [AUTO] follows the device configuration.
 * A new language is added by appending one entry here plus its [AppStrings] file.
 */
enum class AppLanguage(val id: String, val tag: String?, val nativeName: String?) {
    AUTO("auto", null, null),
    ES("es", "es", "Español"),
    EN("en", "en", "English"),
    PT("pt", "pt", "Português"),
    FR("fr", "fr", "Français"),
    IT("it", "it", "Italiano"),
    DE("de", "de", "Deutsch");

    companion object {
        fun fromId(id: String?): AppLanguage = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

private val tables: Map<String, AppStrings> = mapOf(
    "es" to StringsEs,
    "en" to StringsEn,
    "pt" to StringsPt,
    "fr" to StringsFr,
    "it" to StringsIt,
    "de" to StringsDe
)

/** Resolves the language actually used, taking the device locale into account. */
fun resolveLanguage(selected: AppLanguage, deviceTag: String): AppLanguage {
    if (selected != AppLanguage.AUTO) return selected
    val primary = deviceTag.lowercase(Locale.ROOT).substringBefore('-')
    return AppLanguage.entries.firstOrNull { it.tag == primary } ?: AppLanguage.EN
}

fun stringsFor(language: AppLanguage): AppStrings =
    tables[language.tag] ?: StringsEn

fun localeFor(language: AppLanguage, deviceLocale: Locale): Locale =
    language.tag?.let { Locale(it) } ?: deviceLocale

val LocalStrings = staticCompositionLocalOf { StringsEn }
val LocalAppLocale = staticCompositionLocalOf { Locale.ENGLISH }
