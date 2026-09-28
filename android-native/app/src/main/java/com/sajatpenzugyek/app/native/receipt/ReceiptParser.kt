package com.sajatpenzugyek.app.native.receipt

import com.sajatpenzugyek.app.core.utils.TextNormalizer
import com.sajatpenzugyek.app.domain.model.PaymentMethod
import com.sajatpenzugyek.app.domain.model.ReceiptItem
import com.sajatpenzugyek.app.domain.model.ReceiptScan
import java.time.LocalDate
import java.util.UUID
import java.util.regex.Pattern

object ReceiptParser {

    private val HU_TOTAL_KEYWORDS = listOf(
        "fizetendo", "vegosszeg", "osszesen", "total", "grand total", "amount due", "fizetett"
    )
    private val HU_TAX_KEYWORDS = listOf("afa", "afa tartalom", "vat", "tax")
    private val HU_CASH_KEYWORDS = listOf("keszpenz", "keszp.", "cash", "kp")
    private val HU_CARD_KEYWORDS = listOf("bankkartya", "kartya", "card", "visa", "mastercard", "pos")

    private val KNOWN_CHAINS = listOf(
        "lidl", "aldi", "spar", "interspar", "tesco", "auchan", "penny", "coop", "cba",
        "prima", "mol", "shell", "omv", "orlen", "dm", "rossmann", "muller", "ikea",
        "decathlon", "obi", "praktiker", "bauhaus", "benu", "kulcs patika", "gyogyszertar",
        "mcdonalds", "burger king", "kfc", "subway", "starbucks"
    )

    fun parse(rawText: String, imageUri: String? = null): ReceiptScan {
        val normalized = rawText
            .replace("\r\n", "\n")
            .replace("fizetend0", "fizetendo", ignoreCase = true)
            .replace("vegosszeg", "vegosszeg", ignoreCase = true)

        val lines = normalized.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val merchant = extractMerchant(lines)
        val (totalMinor, currency) = extractTotal(lines)
        val taxMinor = extractTax(lines)
        val paymentMethod = extractPaymentMethod(normalized)
        val date = extractDate(lines)
        val items = extractItems(lines)

        var confidence = 0.0f
        if (totalMinor != null && totalMinor > 0L) confidence += 0.45f
        if (!merchant.isNullOrBlank()) confidence += 0.25f
        if (date != null) confidence += 0.15f
        if (paymentMethod != PaymentMethod.UNKNOWN) confidence += 0.10f
        if (items.isNotEmpty()) confidence += 0.05f

        return ReceiptScan(
            id = UUID.randomUUID().toString(),
            imageUri = imageUri,
            merchant = merchant,
            date = date ?: LocalDate.now(),
            totalMinor = totalMinor,
            subtotalMinor = null,
            taxMinor = taxMinor,
            currency = currency,
            paymentMethod = paymentMethod,
            items = items,
            rawOcrText = rawText,
            confidence = confidence.coerceIn(0.1f, 1.0f)
        )
    }

    private fun extractMerchant(lines: List<String>): String? {
        // 1. Check known chains in first 8 lines
        for (i in 0 until minOf(lines.size, 8)) {
            val norm = TextNormalizer.removeDiacritics(lines[i]).lowercase()
            for (chain in KNOWN_CHAINS) {
                if (norm.contains(chain)) {
                    return chain.replaceFirstChar { it.uppercase() }
                }
            }
        }

        // 2. Fallback to clean top line
        val skip = listOf("nyugta", "szamla", "penztar", "egyszerusitett", "adoszam", "blokk", "receipt")
        for (i in 0 until minOf(lines.size, 4)) {
            val raw = lines[i]
            val norm = TextNormalizer.removeDiacritics(raw).lowercase()
            if (raw.length > 2 && skip.none { norm.contains(it) }) {
                return TextNormalizer.cleanMerchantName(raw)
            }
        }

        return null
    }

    private fun extractTotal(lines: List<String>): Pair<Long?, String> {
        var currency = "HUF"

        for (i in lines.indices) {
            val line = lines[i]
            val norm = TextNormalizer.removeDiacritics(line).lowercase()

            if (HU_TOTAL_KEYWORDS.any { norm.contains(it) }) {
                // Try current line
                var extracted = parseAmountInLine(line)
                // If not found, try next line
                if (extracted == null && i + 1 < lines.size) {
                    extracted = parseAmountInLine(lines[i + 1])
                }

                if (extracted != null) {
                    return Pair(extracted.first, extracted.second)
                }
            }
        }

        // Fallback: Largest reasonable amount
        var maxMinor = 0L
        for (line in lines) {
            val extracted = parseAmountInLine(line)
            if (extracted != null && extracted.first > maxMinor) {
                maxMinor = extracted.first
                currency = extracted.second
            }
        }

        return Pair(if (maxMinor > 0L) maxMinor else null, currency)
    }

    private fun extractPaymentMethod(text: String): PaymentMethod {
        val norm = TextNormalizer.removeDiacritics(text).lowercase()
        val hasCash = HU_CASH_KEYWORDS.any { norm.contains(it) }
        val hasCard = HU_CARD_KEYWORDS.any { norm.contains(it) }

        return when {
            hasCash && !hasCard -> PaymentMethod.CASH
            hasCard && !hasCash -> PaymentMethod.CARD
            else -> PaymentMethod.UNKNOWN
        }
    }

    private fun extractTax(lines: List<String>): Long? {
        for (line in lines) {
            val norm = TextNormalizer.removeDiacritics(line).lowercase()
            if (HU_TAX_KEYWORDS.any { norm.contains(it) }) {
                val extracted = parseAmountInLine(line)
                if (extracted != null) return extracted.first
            }
        }
        return null
    }

    private fun extractDate(lines: List<String>): LocalDate? {
        val regex = Regex("(20\\d{2})[./-](\\d{1,2})[./-](\\d{1,2})")
        for (line in lines) {
            val match = regex.find(line)
            if (match != null) {
                return try {
                    val y = match.groupValues[1].toInt()
                    val m = match.groupValues[2].toInt()
                    val d = match.groupValues[3].toInt()
                    LocalDate.of(y, m, d)
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    private fun extractItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        val regex = Regex("^([A-Za-zÁÉÍÓÖŐÚÜŰáéíóöőúüű0-9\\s/.,%-]{3,40})\\s+(\\d{1,3}(?:[.,\\s]\\d{3})*(?:[.,]\\d{1,2})?)\\s*(?:Ft|HUF)?$", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val match = regex.find(line)
            if (match != null) {
                val name = match.groupValues[1].trim()
                val norm = TextNormalizer.removeDiacritics(name).lowercase()

                if (HU_TOTAL_KEYWORDS.any { norm.contains(it) } ||
                    HU_TAX_KEYWORDS.any { norm.contains(it) } ||
                    norm.contains("nyugta")
                ) {
                    continue
                }

                val priceRaw = match.groupValues[2].replace("\\s".toRegex(), "").replace(".", "").replace(",", ".")
                val priceMajor = priceRaw.toDoubleOrNull() ?: 0.0
                if (priceMajor > 0.0) {
                    items.add(
                        ReceiptItem(
                            name = name,
                            totalPriceMinor = (priceMajor * 100).toLong()
                        )
                    )
                }
            }
        }

        return items
    }

    private fun parseAmountInLine(line: String): Pair<Long, String>? {
        val pattern = Pattern.compile("(\\d{1,3}(?:[.,\\s]\\d{3})*(?:[.,]\\d{1,2})?)\\s*(Ft|HUF|EUR|USD|€|\\$)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(line)

        if (matcher.find()) {
            val group1 = matcher.group(1) ?: return null
            val rawNum = group1.replace("\\s".toRegex(), "").replace(".", "").replace(",", ".")
            val rawCurr = matcher.group(2)?.uppercase() ?: "HUF"

            val curr = when (rawCurr) {
                "EUR", "€" -> "EUR"
                "USD", "$" -> "USD"
                else -> "HUF"
            }

            val major = rawNum.toDoubleOrNull() ?: return null
            val minor = (major * 100).toLong()
            return Pair(minor, curr)
        }
        return null
    }
}
