package com.rork.diariointimo.i18n

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.DayOfWeek
import java.util.Locale

/** Locale aware date helpers. User content is never touched by these. */
object DateFormats {
    fun longDate(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    fun mediumDate(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    fun monthYear(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    fun shortMonth(date: LocalDate, locale: Locale): String =
        date.month.getDisplayName(TextStyle.SHORT, locale).uppercase(locale).take(4)

    fun weekdayInitial(day: DayOfWeek, locale: Locale): String =
        day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale).take(2)

    fun weekdayFull(date: LocalDate, locale: Locale): String =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
