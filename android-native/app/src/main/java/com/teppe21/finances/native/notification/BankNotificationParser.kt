package com.teppe21.finances.native.notification

import com.teppe21.finances.core.utils.TextNormalizer
import com.teppe21.finances.domain.model.AccountType
import com.teppe21.finances.domain.model.ParsedNotificationTransaction
import com.teppe21.finances.domain.model.RawNotification
import com.teppe21.finances.domain.model.TransactionDirection
import java.time.LocalDate
import java.time.ZoneId
import java.util.regex.Pattern

interface BankNotificationParser {
    val bankName: String
    val supportedPackages: List<String>
    fun canHandle(notification: RawNotification): Boolean
    fun parse(notification: RawNotification): ParsedNotificationTransaction?
}

// 1. REVOLUT PARSER
class RevolutParser : BankNotificationParser {
    override val bankName = "Revolut"
    override val supportedPackages = listOf("com.revolut.revolut")

    override fun canHandle(notification: RawNotification): Boolean {
        if (supportedPackages.contains(notification.packageName)) return true
        val label = notification.applicationLabel?.lowercase() ?: ""
        return label.contains("revolut")
    }

    override fun parse(notification: RawNotification): ParsedNotificationTransaction? {
        val title = notification.title ?: ""
        val text = "${notification.text ?: ""} ${notification.bigText ?: ""}".trim()
        val full = "$title\n$text".trim()

        if (full.isBlank()) return null
        if (full.contains("biztonsági kód") || full.contains("security code") || full.contains("új funkció")) {
            return null
        }

        val extracted = extractAmountAndCurrency(full) ?: return null

        var direction = TransactionDirection.EXPENSE
        val lower = full.lowercase()
        val isTransfer = lower.contains("transfer") || lower.contains("transferred") ||
            lower.contains("átutalás") || lower.contains("utalás") || lower.contains("átutalt") ||
            lower.contains("pénzt küldtél") || lower.contains("elküldtél") || lower.contains("sent to") ||
            lower.contains("you sent") || lower.contains("átvezetés")

        if (lower.contains("refund") || lower.contains("visszatérítés")) {
            direction = TransactionDirection.REFUND
        } else if (isTransfer) {
            direction = TransactionDirection.TRANSFER
        } else if (lower.contains("received") || lower.contains("kapott") || lower.contains("érkezett") || lower.contains("jóváírás")) {
            direction = TransactionDirection.INCOME
        }

        val huMatch = Regex("(?:következőnek:?|itt:?|elfogadóhely:?|részére:?|nevére:?|küldve:?)\\s*([^,.\n]+)", RegexOption.IGNORE_CASE).find(full)
        val enMatch = Regex("(?:to|at|sent to)\\s+([A-Za-z0-9\\s&'-]+?)(?:\\s+with|\\s+from|\\s+for|\\.|$)", RegexOption.IGNORE_CASE).find(full)

        val merchant = if (huMatch != null) {
            TextNormalizer.cleanMerchantName(huMatch.groupValues[1])
        } else if (enMatch != null) {
            TextNormalizer.cleanMerchantName(enMatch.groupValues[1])
        } else if (title.isNotBlank() && !title.lowercase().contains("revolut") && !title.lowercase().contains("fizetés") && !title.lowercase().contains("utalás")) {
            TextNormalizer.cleanMerchantName(title)
        } else {
            if (isTransfer) "Transfer" else "Revolut Partner"
        }

        val date = notification.postedAt.atZone(ZoneId.systemDefault()).toLocalDate()

        return ParsedNotificationTransaction(
            amountMinor = if (direction == TransactionDirection.EXPENSE) -kotlin.math.abs(extracted.amountMinor) else kotlin.math.abs(extracted.amountMinor),
            currency = extracted.currency,
            direction = direction,
            merchant = merchant,
            description = full.take(120),
            date = date,
            valueDate = notification.postedAt,
            suggestedAccountType = AccountType.BANK,
            suggestedAccountName = "Revolut",
            confidence = if (merchant != "Revolut Partner") 0.95f else 0.85f,
            rawTextExcerpt = full
        )
    }
}

// 2. OTP BANK PARSER
class OtpParser : BankNotificationParser {
    override val bankName = "OTP Bank"
    override val supportedPackages = listOf(
        "hu.otpbank.smartbank",
        "hu.otpbank.mobilbank",
        "hu.otpbank.simple"
    )

    override fun canHandle(notification: RawNotification): Boolean {
        if (supportedPackages.contains(notification.packageName)) return true
        val label = notification.applicationLabel?.lowercase() ?: ""
        return label.contains("otp") || label.contains("simple")
    }

    override fun parse(notification: RawNotification): ParsedNotificationTransaction? {
        val title = notification.title ?: ""
        val text = "${notification.text ?: ""} ${notification.bigText ?: ""}".trim()
        val full = "$title\n$text".trim()

        if (full.isBlank()) return null
        if (full.contains("belépési kód") || full.contains("SMS kód") || full.contains("jóváhagyás")) {
            return null
        }

        val extracted = extractAmountAndCurrency(full) ?: return null

        var direction = TransactionDirection.EXPENSE
        val lower = full.lowercase()
        if (lower.contains("jóváírás") || lower.contains("jovairas") || lower.contains("beérkező") || lower.contains("munkabér")) {
            direction = TransactionDirection.INCOME
        } else if (lower.contains("átutalás") && !lower.contains("sikeres fizetés")) {
            direction = TransactionDirection.TRANSFER
        }

        val placeMatch = Regex("(?:Hely|Partner|Elfogadóhely):\\s*([^,.\n]+)", RegexOption.IGNORE_CASE).find(full)
        val dashMatch = Regex("[-–]\\s*([A-Za-z0-9\\s&'-]+?)(?:\\s*,|\\s*Kártya|\\.|$)", RegexOption.IGNORE_CASE).find(full)

        val merchant = if (placeMatch != null) {
            TextNormalizer.cleanMerchantName(placeMatch.groupValues[1])
        } else if (dashMatch != null && !dashMatch.groupValues[1].lowercase().contains("ft")) {
            TextNormalizer.cleanMerchantName(dashMatch.groupValues[1])
        } else {
            "OTP Transaction"
        }

        val cardMatch = Regex("kártya:?\\s*\\*?(\\d{4})", RegexOption.IGNORE_CASE).find(full)
        val cardLast4 = cardMatch?.groupValues?.get(1)

        val date = notification.postedAt.atZone(ZoneId.systemDefault()).toLocalDate()

        return ParsedNotificationTransaction(
            amountMinor = if (direction == TransactionDirection.EXPENSE) -kotlin.math.abs(extracted.amountMinor) else kotlin.math.abs(extracted.amountMinor),
            currency = extracted.currency,
            direction = direction,
            merchant = merchant,
            description = full.take(120),
            date = date,
            valueDate = notification.postedAt,
            suggestedAccountType = AccountType.BANK,
            suggestedAccountName = "OTP Current Account",
            cardLast4 = cardLast4,
            confidence = if (merchant != "OTP Transaction") 0.95f else 0.85f,
            rawTextExcerpt = full
        )
    }
}

// 3. ERSTE, MBH, WISE, GENERIC BANK PARSERS
class ErsteParser : BankNotificationParser {
    override val bankName = "Erste Bank"
    override val supportedPackages = listOf("hu.erstebank.george.hungary", "hu.erstebank.mobilebank")

    override fun canHandle(notification: RawNotification): Boolean {
        if (supportedPackages.contains(notification.packageName)) return true
        return (notification.applicationLabel?.lowercase() ?: "").contains("erste") ||
            (notification.title?.lowercase() ?: "").contains("george")
    }

    override fun parse(notification: RawNotification): ParsedNotificationTransaction? {
        val full = "${notification.title ?: ""} ${notification.text ?: ""} ${notification.bigText ?: ""}".trim()
        val extracted = extractAmountAndCurrency(full) ?: return null
        val date = notification.postedAt.atZone(ZoneId.systemDefault()).toLocalDate()

        return ParsedNotificationTransaction(
            amountMinor = -kotlin.math.abs(extracted.amountMinor),
            currency = extracted.currency,
            direction = TransactionDirection.EXPENSE,
            merchant = "Erste Transaction",
            description = full.take(120),
            date = date,
            valueDate = notification.postedAt,
            suggestedAccountType = AccountType.BANK,
            suggestedAccountName = "Erste Bank",
            confidence = 0.85f,
            rawTextExcerpt = full
        )
    }
}

class MbhParser : BankNotificationParser {
    override val bankName = "MBH Bank"
    override val supportedPackages = listOf("hu.takarek.mobilbank", "hu.mbhbank.app")

    override fun canHandle(notification: RawNotification): Boolean {
        if (supportedPackages.contains(notification.packageName)) return true
        return (notification.applicationLabel?.lowercase() ?: "").contains("mbh")
    }

    override fun parse(notification: RawNotification): ParsedNotificationTransaction? {
        val full = "${notification.title ?: ""} ${notification.text ?: ""} ${notification.bigText ?: ""}".trim()
        val extracted = extractAmountAndCurrency(full) ?: return null
        val date = notification.postedAt.atZone(ZoneId.systemDefault()).toLocalDate()

        return ParsedNotificationTransaction(
            amountMinor = -kotlin.math.abs(extracted.amountMinor),
            currency = extracted.currency,
            direction = TransactionDirection.EXPENSE,
            merchant = "MBH Transaction",
            description = full.take(120),
            date = date,
            valueDate = notification.postedAt,
            suggestedAccountType = AccountType.BANK,
            suggestedAccountName = "MBH Bank",
            confidence = 0.85f,
            rawTextExcerpt = full
        )
    }
}

class WiseParser : BankNotificationParser {
    override val bankName = "Wise"
    override val supportedPackages = listOf("com.transferwise.android")

    override fun canHandle(notification: RawNotification): Boolean {
        if (supportedPackages.contains(notification.packageName)) return true
        return (notification.applicationLabel?.lowercase() ?: "").contains("wise")
    }

    override fun parse(notification: RawNotification): ParsedNotificationTransaction? {
        val full = "${notification.title ?: ""} ${notification.text ?: ""} ${notification.bigText ?: ""}".trim()
        val extracted = extractAmountAndCurrency(full) ?: return null
        val date = notification.postedAt.atZone(ZoneId.systemDefault()).toLocalDate()

        return ParsedNotificationTransaction(
            amountMinor = -kotlin.math.abs(extracted.amountMinor),
            currency = extracted.currency,
            direction = TransactionDirection.EXPENSE,
            merchant = "Wise Partner",
            description = full.take(120),
            date = date,
            valueDate = notification.postedAt,
            suggestedAccountType = AccountType.BANK,
            suggestedAccountName = "Wise",
            confidence = 0.85f,
            rawTextExcerpt = full
        )
    }
}

class GenericBankParser : BankNotificationParser {
    override val bankName = "Generic Bank"
    override val supportedPackages = emptyList<String>()

    override fun canHandle(notification: RawNotification): Boolean {
        val full = "${notification.title ?: ""} ${notification.text ?: ""}".lowercase()
        return full.contains("fizetés") || full.contains("vásárlás") || full.contains("terhelés") || full.contains("jóváírás")
    }

    override fun parse(notification: RawNotification): ParsedNotificationTransaction? {
        val full = "${notification.title ?: ""} ${notification.text ?: ""} ${notification.bigText ?: ""}".trim()
        val extracted = extractAmountAndCurrency(full) ?: return null
        val date = notification.postedAt.atZone(ZoneId.systemDefault()).toLocalDate()

        return ParsedNotificationTransaction(
            amountMinor = -kotlin.math.abs(extracted.amountMinor),
            currency = extracted.currency,
            direction = TransactionDirection.EXPENSE,
            merchant = "Bank Transaction",
            description = full.take(120),
            date = date,
            valueDate = notification.postedAt,
            suggestedAccountType = AccountType.BANK,
            confidence = 0.70f,
            rawTextExcerpt = full
        )
    }
}

// Amount & Currency Extraction Helper
data class ExtractedAmount(val amountMinor: Long, val currency: String)

private fun parseMajorAmount(clean: String): Double? {
    if (clean.contains(",") && clean.contains(".")) {
        val lastComma = clean.lastIndexOf(',')
        val lastDot = clean.lastIndexOf('.')
        return if (lastComma > lastDot) {
            clean.replace(".", "").replace(",", ".").toDoubleOrNull()
        } else {
            clean.replace(",", "").toDoubleOrNull()
        }
    } else if (clean.contains(",")) {
        val parts = clean.split(",")
        return if (parts.size == 2 && parts[1].length <= 2) {
            clean.replace(",", ".").toDoubleOrNull()
        } else {
            clean.replace(",", "").toDoubleOrNull()
        }
    } else if (clean.contains(".")) {
        val parts = clean.split(".")
        return if (parts.size == 2 && parts[1].length <= 2) {
            clean.toDoubleOrNull()
        } else {
            clean.replace(".", "").toDoubleOrNull()
        }
    } else {
        return clean.toDoubleOrNull()
    }
}

fun extractAmountAndCurrency(text: String, defaultCurrency: String = "HUF"): ExtractedAmount? {
    // Pattern 1: e.g. "14 500 Ft", "3.450 HUF", "12,99 EUR", "25.50 USD", "45.50 EUR"
    val pattern = Pattern.compile("([+-]?\\s*\\d{1,3}(?:[.,\\s]\\d{3})*(?:[.,]\\d{1,2})?|[+-]?\\s*\\d+(?:[.,]\\d{1,2})?)\\s*(Ft|HUF|EUR|USD|GBP|CHF|€|\\$|£)", Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(text)

    if (matcher.find()) {
        val group1 = matcher.group(1) ?: return null
        val group2 = matcher.group(2) ?: return null
        val clean = group1.replace("\\s".toRegex(), "")

        val major = parseMajorAmount(clean) ?: return null

        val rawCurr = group2.uppercase()
        val currency = when (rawCurr) {
            "FT", "HUF" -> "HUF"
            "EUR", "€" -> "EUR"
            "USD", "$" -> "USD"
            "GBP", "£" -> "GBP"
            "CHF" -> "CHF"
            else -> defaultCurrency
        }

        val minor = (major * 100).toLong()
        return ExtractedAmount(minor, currency)
    }


    return null
}
