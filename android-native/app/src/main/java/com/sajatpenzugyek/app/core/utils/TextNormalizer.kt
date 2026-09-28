package com.sajatpenzugyek.app.core.utils

import java.text.Normalizer
import java.util.regex.Pattern

object TextNormalizer {

    private val DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

    fun removeDiacritics(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("")
    }

    fun normalizeSearch(text: String?): String {
        return removeDiacritics(text)
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun cleanMerchantName(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var cleaned = raw.trim().trimStart(':', '-', '–', ' ', '\t')
            .replace(Regex("^(kft\\.?|zrt\\.?|bt\\.?|nyrt\\.?)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(kft\\.?|zrt\\.?|bt\\.?|nyrt\\.?)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(bolt|üzlet|aruhaz|áruház)\\s+", RegexOption.IGNORE_CASE), "")
            .trim().trimStart(':', '-', '–', ' ', '\t').trimEnd(':', '-', '–', ' ', '\t')

        return cleaned
    }
}
