package com.sajatpenzugyek.app.core.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {
    private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

    fun format(date: LocalDate): String {
        return date.format(DISPLAY_FORMATTER)
    }
}
