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

    companion object {
        private val PROMO_PATTERNS = listOf(
            "refer", "referral", "eligible friend", "invite", "earn", "reward",
            "cashback", "bonus", "promotion", "campaign", "offer", "t&cs",
            "terms and conditions", "days left", "giveaway", "win", "ajánlj",
            "ajánlás", "barátodat", "pénzjutalom", "nyeremény", "hívj meg",
            "ingyenes kártya", "próbáld ki"
        )

        private val SECURITY_PATTERNS = listOf(
            "biztonsági kód", "security code", "új funkció", "verification code",
            "sms kód", "ellenőrző kód", "egyszer használatos", "one-time password",
            "passcode", "belépési kód", "hitelesítő kód"
        )

        private val TRANSACTION_SEMANTICS = listOf(
            "you spent", "elköltöttél", "fizettél", "paid", "card payment", "card purchase",
            "kártyás vásárlás", "with revolut", "transfer to", "sent to", "pénzt küldtél",
            "elküldtél", "cash withdrawal", "atm", "készpénzfelvétel", "sikeres fizetés",
            "received", "jóváírás", "refund", "visszatérítés", "fizetés a következőnek:"
        )
    }

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
        val lower = full.lowercase()

        // 1. Filter out security / OTP codes immediately
        if (SECURITY_PATTERNS.any { lower.contains(it) }) {
            return null
        }

        // 2. Filter out referral / promotional / marketing alerts immediately
        if (PROMO_PATTERNS.any { lower.contains(it) }) {
            return null
        }

        // 3. Require explicit financial transaction semantics
        val hasSemantic = TRANSACTION_SEMANTICS.any { lower.contains(it) }
        if (!hasSemantic) {
            return null
        }

        val extracted = extractAmountAndCurrency(full) ?: return null

        var direction = TransactionDirection.EXPENSE
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

// 2. GOOGLE WALLET PARSER (Contactless NFC payments)
class GoogleWalletParser : BankNotificationParser {
    override val bankName = "Google Wallet"
    override val supportedPackages = listOf(
        "com.google.android.apps.walletnfcrel",
        "com.google.android.apps.nbu.paisa.user"
    )

    override fun canHandle(notification: RawNotification): Boolean {
        if (supportedPackages.contains(notification.packageName)) return true
        val label = notification.applicationLabel?.lowercase() ?: ""
        return label.contains("wallet") || label.contains("google pay")
    }

    override fun parse(notification: RawNotification): ParsedNotificationTransaction? {
        val title = notification.title ?: ""
        val text = "${notification.text ?: ""} ${notification.bigText ?: ""}".trim()
        val full = "$title\n$text".trim()

        if (full.isBlank()) return null

        val extracted = extractAmountAndCurrency(full) ?: return null

        // Google Wallet displays the merchant in the notification title (e.g. "75. SZ. ABC ÁRUHÁZ")
        val merchant = if (title.isNotBlank() && !title.lowercase().contains("wallet") && !title.lowercase().contains("pay")) {
            TextNormalizer.cleanMerchantName(title)
        } else {
            "Contactless Payment"
        }

        // Extract card info e.g. "with Revolut Mastercard ••1413"
        val cardMatch = Regex("with\\s+(.+?)(?:\\s*[•*]+(\\d{4}))?$", RegexOption.IGNORE_CASE).find(text)
        val cardName = cardMatch?.groupValues?.get(1)?.trim() ?: ""
        val cardLast4 = cardMatch?.groupValues?.getOrNull(2)

        val suggestedAccount = when {
            cardName.contains("revolut", ignoreCase = true) -> "Revolut"
            cardName.contains("otp", ignoreCase = true) -> "OTP Current Account"
            cardName.contains("erste", ignoreCase = true) -> "Erste Bank"
            cardName.contains("mbh", ignoreCase = true) -> "MBH Bank"
            cardName.contains("wise", ignoreCase = true) -> "Wise"
            else -> "Google Wallet"
        }

        val date = notification.postedAt.atZone(ZoneId.systemDefault()).toLocalDate()

        return ParsedNotificationTransaction(
            amountMinor = -kotlin.math.abs(extracted.amountMinor),
            currency = extracted.currency,
            direction = TransactionDirection.EXPENSE,
            merchant = merchant,
            description = full.take(120),
            date = date,
            valueDate = notification.postedAt,
            suggestedAccountType = AccountType.BANK,
            suggestedAccountName = suggestedAccount,
            cardLast4 = cardLast4,
            confidence = 0.95f,
            rawTextExcerpt = full
        )
    }
}

// 3. OTP BANK PARSER
class OtpParser : BankNotificationParser {
    override val bankName = "OTP Bank"
    override val supportedPackages = listOf(
        "hu.otpbank.smartbank",
        "hu.otpbank.mobilbank",
        "hu.otpbank.simple"
    )

    companion object {
        private val SECURITY_PATTERNS = listOf(
            "belépési kód", "sms kód", "jóváhagyás", "biztonsági kód", "hitelesítő kód", "egyszer használatos"
        )
    }

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
        val lower = full.lowercase()

        if (SECURITY_PATTERNS.any { lower.contains(it) }) {
            return null
        }

        val extracted = extractAmountAndCurrency(full) ?: return null

        var direction = TransactionDirection.EXPENSE
        if (lower.contains("jóváírás") || lower.contains("jovairas") || lower.contains("beérkező") || lower.contains("munkabér")) {
            direction = TransactionDirection.INCOME
        } else if (lower.contains("átutalás") && !lower.contains("sikeres fizetés")) {
            direction = TransactionDirection.TRANSFER
        }

        val placeMatch = Regex("(?:Hely|Partner|Elfogadóhely):\\s*([^,.\n]+)", RegexOption.IGNORE_CASE).find(full)
        val dashMatch = Regex("[-–]\\s*([A-Za-z0-9\\s&'-]+?)(?:\\s*,|\\s*Kártya|\\.|$)", RegexOption.IGNORE_CASE).find(full)

        val merchant = if (placeMatch != null) {
            TextNormalizer.cleanMerchantName(placeMatch.groupValues[1])
        } else if (dashMatch != null && !dashMatch.groupValues[1].lowercase().contains("ft") && !dashMatch.groupValues[1].lowercase().contains("huf")) {
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

// 4. ERSTE, MBH, WISE, GENERIC BANK PARSERS
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

// Restricted Generic Bank Parser: ONLY matches whitelisted Hungarian/European bank packages
class GenericBankParser : BankNotificationParser {
    override val bankName = "Generic Bank"
    override val supportedPackages = listOf(
        "hu.cib.mobilebank",
        "hu.raiffeisen.mobilebank",
        "hu.unicredit.mobilebank",
        "hu.granitbank.granitmobil"
    )

    override fun canHandle(notification: RawNotification): Boolean {
        // Strictly restricted to known financial packages only: NEVER match unknown third-party apps
        return supportedPackages.contains(notification.packageName)
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
            // Thousands separator e.g. 22,500
            clean.replace(",", "").toDoubleOrNull()
        }
    } else if (clean.contains(".")) {
        val parts = clean.split(".")
        return if (parts.size == 2 && parts[1].length <= 2) {
            clean.toDoubleOrNull()
        } else {
            // Thousands separator e.g. 14.500
            clean.replace(".", "").toDoubleOrNull()
        }
    } else {
        return clean.toDoubleOrNull()
    }
}

private fun normalizeCurrency(raw: String, defaultCurrency: String): String {
    return when (raw.uppercase()) {
        "FT", "HUF" -> "HUF"
        "EUR", "€" -> "EUR"
        "USD", "$" -> "USD"
        "GBP", "£" -> "GBP"
        "CHF" -> "CHF"
        else -> defaultCurrency
    }
}

fun extractAmountAndCurrency(text: String, defaultCurrency: String = "HUF"): ExtractedAmount? {
    if (text.isBlank()) return null

    // Filter out balance lines to prevent capturing balance as transaction amount
    val lines = text.lines()
    val nonBalanceLines = lines.filterNot { line ->
        val l = line.lowercase()
        l.contains("balance") || l.contains("egyenleg") || l.contains("elérhető") || l.contains("számlaegyenleg")
    }
    val contentToSearch = if (nonBalanceLines.isNotEmpty()) nonBalanceLines.joinToString("\n") else text

    // Pattern 1: Prefix Currency e.g. "HUF804", "HUF804.00", "HUF22,500", "€12.50", "$25.00", "£15.00", "Ft 14 500"
    val prefixPattern = Pattern.compile(
        "(?:(Ft|HUF|EUR|USD|GBP|CHF|€|\\$|£)\\s*([+-]?\\s*\\d{1,3}(?:[.,\\s]\\d{3})*(?:[.,]\\d{1,2})?|[+-]?\\s*\\d+(?:[.,]\\d{1,2})?))",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern 2: Suffix Currency e.g. "14 500 Ft", "804 HUF", "12,99 EUR", "25.50 USD", "45.50 EUR"
    val suffixPattern = Pattern.compile(
        "([+-]?\\s*\\d{1,3}(?:[.,\\s]\\d{3})*(?:[.,]\\d{1,2})?|[+-]?\\s*\\d+(?:[.,]\\d{1,2})?)\\s*(Ft|HUF|EUR|USD|GBP|CHF|€|\\$|£)",
        Pattern.CASE_INSENSITIVE
    )

    val prefixMatcher = prefixPattern.matcher(contentToSearch)
    if (prefixMatcher.find()) {
        val currStr = prefixMatcher.group(1) ?: ""
        val numStr = prefixMatcher.group(2) ?: ""
        val currency = normalizeCurrency(currStr, defaultCurrency)
        val major = parseMajorAmount(numStr.replace("\\s".toRegex(), ""))
        if (major != null) {
            val minor = Math.round(major * 100.0)
            return ExtractedAmount(minor, currency)
        }
    }

    val suffixMatcher = suffixPattern.matcher(contentToSearch)
    if (suffixMatcher.find()) {
        val numStr = suffixMatcher.group(1) ?: ""
        val currStr = suffixMatcher.group(2) ?: ""
        val currency = normalizeCurrency(currStr, defaultCurrency)
        val major = parseMajorAmount(numStr.replace("\\s".toRegex(), ""))
        if (major != null) {
            val minor = Math.round(major * 100.0)
            return ExtractedAmount(minor, currency)
        }
    }

    return null
}
