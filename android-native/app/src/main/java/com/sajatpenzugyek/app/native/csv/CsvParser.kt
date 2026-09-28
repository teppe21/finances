package com.sajatpenzugyek.app.native.csv

import com.sajatpenzugyek.app.core.utils.TextNormalizer
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate

data class CsvColumnMapping(
    val dateCol: Int = 0,
    val descCol: Int = 1,
    val amountCol: Int? = null,
    val debitCol: Int? = null,
    val creditCol: Int? = null,
    val currencyCol: Int? = null,
    val categoryCol: Int? = null
)

data class ParsedCsvRow(
    val date: LocalDate,
    val description: String,
    val merchant: String?,
    val amountMinor: Long,
    val currency: String,
    val direction: TransactionDirection,
    val categorySuggestion: String?
)

object CsvParser {

    fun tokenizeLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++ // skip escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    fun detectDelimiter(content: String): Char {
        val sample = content.take(2048)
        val semicolons = sample.count { it == ';' }
        val commas = sample.count { it == ',' }
        val tabs = sample.count { it == '\t' }

        return when {
            semicolons > commas && semicolons > tabs -> ';'
            tabs > commas && tabs > semicolons -> '\t'
            else -> ','
        }
    }

    fun detectMapping(headers: List<String>): CsvColumnMapping {
        val norm = headers.map { TextNormalizer.removeDiacritics(it).lowercase().trim().replace("\"", "") }

        fun findIndex(vararg keywords: String): Int {
            return norm.indexOfFirst { col -> keywords.any { kw -> col.contains(kw) } }
        }

        val dateIdx = findIndex("datum", "date", "konyveles", "erteknap")
        val descIdx = findIndex("leiras", "description", "partner", "megjegyzes", "kozlemeny", "payee")
        val amountIdx = findIndex("osszeg", "amount", "ertek")
        val debitIdx = findIndex("terheles", "kiadas", "debit")
        val creditIdx = findIndex("jovairas", "bevetel", "credit")
        val currIdx = findIndex("penznem", "currency", "deviza")
        val catIdx = findIndex("kategoria", "category")

        return CsvColumnMapping(
            dateCol = if (dateIdx != -1) dateIdx else 0,
            descCol = if (descIdx != -1) descIdx else 1,
            amountCol = if (amountIdx != -1) amountIdx else null,
            debitCol = if (debitIdx != -1) debitIdx else null,
            creditCol = if (creditIdx != -1) creditIdx else null,
            currencyCol = if (currIdx != -1) currIdx else null,
            categoryCol = if (catIdx != -1) catIdx else null
        )
    }

    fun parse(inputStream: InputStream, mappingOverride: CsvColumnMapping? = null): List<ParsedCsvRow> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        var fullText = reader.readText()

        // Strip UTF-8 BOM
        if (fullText.startsWith("\uFEFF")) {
            fullText = fullText.substring(1)
        }

        val lines = fullText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return emptyList()

        val delimiter = detectDelimiter(fullText)
        val headers = tokenizeLine(lines[0], delimiter)
        val mapping = mappingOverride ?: detectMapping(headers)

        val rows = mutableListOf<ParsedCsvRow>()

        for (i in 1 until lines.size) {
            val cells = tokenizeLine(lines[i], delimiter)
            if (cells.size < 2) continue

            val rawDate = cells.getOrNull(mapping.dateCol) ?: ""
            val rawDesc = cells.getOrNull(mapping.descCol) ?: "Tranzakció"

            val date = parseDate(rawDate) ?: LocalDate.now()

            var amountMajor = 0.0
            var currency = "HUF"

            if (mapping.currencyCol != null) {
                val c = cells.getOrNull(mapping.currencyCol)?.uppercase()?.trim() ?: "HUF"
                if (listOf("HUF", "EUR", "USD").contains(c)) currency = c
            }

            if (mapping.debitCol != null && mapping.creditCol != null) {
                val debit = parseNumber(cells.getOrNull(mapping.debitCol))
                val credit = parseNumber(cells.getOrNull(mapping.creditCol))
                if (debit > 0) amountMajor = -debit
                else if (credit > 0) amountMajor = credit
            } else if (mapping.amountCol != null) {
                amountMajor = parseNumber(cells.getOrNull(mapping.amountCol))
            }

            val amountMinor = (amountMajor * 100).toLong()
            val direction = when {
                amountMajor > 0 -> TransactionDirection.INCOME
                amountMajor < 0 -> TransactionDirection.EXPENSE
                else -> TransactionDirection.ADJUSTMENT
            }

            val merchant = TextNormalizer.cleanMerchantName(rawDesc)
            val catSuggestion = if (mapping.categoryCol != null) cells.getOrNull(mapping.categoryCol) else null

            rows.add(
                ParsedCsvRow(
                    date = date,
                    description = rawDesc,
                    merchant = merchant.ifBlank { null },
                    amountMinor = amountMinor,
                    currency = currency,
                    direction = direction,
                    categorySuggestion = catSuggestion
                )
            )
        }

        return rows
    }

    private fun parseDate(raw: String): LocalDate? {
        val clean = raw.trim().replace("/", "-").replace(".", "-")
        val match = Regex("(20\\d{2})-(\\d{1,2})-(\\d{1,2})").find(clean)
        if (match != null) {
            val y = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            val d = match.groupValues[3].toInt()
            return LocalDate.of(y, m, d)
        }
        return null
    }

    private fun parseNumber(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val clean = raw.replace("\\s".toRegex(), "")
            .replace("Ft", "", ignoreCase = true)
            .replace("HUF", "", ignoreCase = true)
            .replace("EUR", "", ignoreCase = true)
            .replace("USD", "", ignoreCase = true)
            .replace("€", "")
            .replace("$", "")

        return if (clean.contains(",") && clean.contains(".")) {
            // E.g. 1.245,50 -> 1245.50
            clean.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        } else if (clean.contains(",")) {
            clean.replace(",", ".").toDoubleOrNull() ?: 0.0
        } else {
            clean.toDoubleOrNull() ?: 0.0
        }
    }
}
