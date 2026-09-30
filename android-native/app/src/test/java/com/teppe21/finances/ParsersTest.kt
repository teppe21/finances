package com.teppe21.finances

import com.teppe21.finances.domain.model.PaymentMethod
import com.teppe21.finances.domain.model.RawNotification
import com.teppe21.finances.domain.model.TransactionDirection
import com.teppe21.finances.native.csv.CsvParser
import com.teppe21.finances.native.notification.GenericBankParser
import com.teppe21.finances.native.notification.GoogleWalletParser
import com.teppe21.finances.native.notification.OtpParser
import com.teppe21.finances.native.notification.RevolutParser
import com.teppe21.finances.native.notification.extractAmountAndCurrency
import com.teppe21.finances.native.receipt.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.Instant

class ParsersTest {

    @Test
    fun testReceiptParserHungarian() {
        val ocr = """
            SPAR MAGYARORSZAG KFT.
            1052 BUDAPEST DEAK TER 1.
            2026.03.28 14:30
            PILOS TEJ 399 Ft
            KENYER 1KG 850 Ft
            FIZETENDO: 1 249 Ft
            KESZPENZ: 2 000 Ft
            VISSZAJARO: 751 Ft
            AFA TARTALOM: 60 Ft
        """.trimIndent()

        val result = ReceiptParser.parse(ocr)
        assertEquals("Spar", result.merchant)
        assertEquals(124900L, result.totalMinor) // 1 249 Ft in minor units
        assertEquals("HUF", result.currency)
        assertEquals(PaymentMethod.CASH, result.paymentMethod)
        assertEquals(6000L, result.taxMinor)
        assertEquals(2, result.items.size)
        assertTrue(result.confidence > 0.8f)
    }

    @Test
    fun testRevolutRealNotificationCoopWithBalance() {
        // Real device notification:
        // Title: "Coop"
        // Text: "You spent HUF804\nHUF balance: HUF42,285.97"
        val notif = RawNotification(
            packageName = "com.revolut.revolut",
            applicationLabel = "Revolut",
            title = "Coop",
            text = "You spent HUF804\nHUF balance: HUF42,285.97",
            postedAt = Instant.now()
        )

        val parser = RevolutParser()
        assertTrue(parser.canHandle(notif))

        val tx = parser.parse(notif)
        assertNotNull("Genuine Revolut expense must be parsed successfully", tx)
        assertEquals("Coop", tx!!.merchant)
        assertEquals(-80400L, tx.amountMinor) // Exactly 804 HUF, NOT the balance 42,285.97!
        assertEquals("HUF", tx.currency)
        assertEquals(TransactionDirection.EXPENSE, tx.direction)
        assertEquals("Revolut", tx.suggestedAccountName)
        assertTrue(tx.confidence >= 0.9f)
    }

    @Test
    fun testRevolutPromoReferralNotificationIsDiscarded() {
        // Real device promotional notification:
        // Title: "7 days left to earn"
        // Text: "Get HUF22,500 for each eligible friend you refer by October 6. T&Cs apply. Tap t..."
        val notif = RawNotification(
            packageName = "com.revolut.revolut",
            applicationLabel = "Revolut",
            title = "7 days left to earn",
            text = "Get HUF22,500 for each eligible friend you refer by October 6. T&Cs apply. Tap t...",
            postedAt = Instant.now()
        )

        val parser = RevolutParser()
        val tx = parser.parse(notif)
        assertNull("Promotional and referral alerts must NEVER create fake transactions", tx)
    }

    @Test
    fun testRevolutMarketingCashbackBonusIsDiscarded() {
        val notif = RawNotification(
            packageName = "com.revolut.revolut",
            applicationLabel = "Revolut",
            title = "Special Offer",
            text = "Invite friends to earn 15,000 HUF bonus reward before campaign ends.",
            postedAt = Instant.now()
        )

        val parser = RevolutParser()
        val tx = parser.parse(notif)
        assertNull("Marketing campaigns must be discarded", tx)
    }

    @Test
    fun testGoogleWalletRealNotification75SzAbcWithCard() {
        // Real device notification:
        // Title: "75. SZ. ABC ÁRUHÁZ"
        // Text: "HUF804.00 with Revolut Mastercard ••1413"
        val notif = RawNotification(
            packageName = "com.google.android.apps.walletnfcrel",
            applicationLabel = "Google Wallet",
            title = "75. SZ. ABC ÁRUHÁZ",
            text = "HUF804.00 with Revolut Mastercard ••1413",
            postedAt = Instant.now()
        )

        val parser = GoogleWalletParser()
        assertTrue(parser.canHandle(notif))

        val tx = parser.parse(notif)
        assertNotNull("Google Wallet contactless transaction must be parsed", tx)
        assertEquals("75. SZ. ABC ÁRUHÁZ", tx!!.merchant)
        assertEquals(-80400L, tx.amountMinor)
        assertEquals("HUF", tx.currency)
        assertEquals(TransactionDirection.EXPENSE, tx.direction)
        assertEquals("Revolut", tx.suggestedAccountName)
        assertEquals("1413", tx.cardLast4)
    }

    @Test
    fun testGoogleWalletEuroPayment() {
        val notif = RawNotification(
            packageName = "com.google.android.apps.walletnfcrel",
            applicationLabel = "Google Wallet",
            title = "SPAR Express",
            text = "12.50 EUR with Erste Visa ••5678",
            postedAt = Instant.now()
        )

        val parser = GoogleWalletParser()
        val tx = parser.parse(notif)
        assertNotNull(tx)
        assertEquals("SPAR Express", tx!!.merchant)
        assertEquals(-1250L, tx.amountMinor)
        assertEquals("EUR", tx.currency)
        assertEquals("Erste Bank", tx.suggestedAccountName)
        assertEquals("5678", tx.cardLast4)
    }

    @Test
    fun testRevolutStandardHungarianExpense() {
        val notif = RawNotification(
            packageName = "com.revolut.revolut",
            applicationLabel = "Revolut",
            title = "Fizetés a következőnek: LIDL",
            text = "Elköltöttél 14 500 Ft-ot itt: LIDL.",
            postedAt = Instant.now()
        )

        val parser = RevolutParser()
        assertTrue(parser.canHandle(notif))

        val tx = parser.parse(notif)
        assertNotNull(tx)
        assertEquals("LIDL", tx!!.merchant)
        assertEquals(-1450000L, tx.amountMinor)
        assertEquals("HUF", tx.currency)
        assertEquals(TransactionDirection.EXPENSE, tx.direction)
    }

    @Test
    fun testOtpNotificationParser() {
        val notif = RawNotification(
            packageName = "hu.otpbank.smartbank",
            applicationLabel = "OTP SmartBank",
            title = "Sikeres kártyás vásárlás",
            text = "Összeg: -4.500 Ft. Hely: McDonald's. Kártya: *1234.",
            postedAt = Instant.now()
        )

        val parser = OtpParser()
        assertTrue(parser.canHandle(notif))

        val tx = parser.parse(notif)
        assertNotNull(tx)
        assertEquals("McDonald's", tx!!.merchant)
        assertEquals(-450000L, tx.amountMinor)
        assertEquals("HUF", tx.currency)
        assertEquals("1234", tx.cardLast4)
        assertEquals(TransactionDirection.EXPENSE, tx.direction)
    }

    @Test
    fun testOtpIgnoresSecurityAndSmsCodes() {
        val notif = RawNotification(
            packageName = "hu.otpbank.smartbank",
            applicationLabel = "OTP SmartBank",
            title = "OTP Bank belépési kód",
            text = "Az Ön belépési kódja: 981245. Ne adja ki senkinek!",
            postedAt = Instant.now()
        )

        val parser = OtpParser()
        val tx = parser.parse(notif)
        assertNull("Security 2FA SMS codes must be discarded", tx)
    }

    @Test
    fun testGenericBankParserRejectsThirdPartyNonBankApps() {
        val parser = GenericBankParser()

        val whatsappNotif = RawNotification(
            packageName = "com.whatsapp",
            title = "John Doe",
            text = "Sikeres fizetés történt 5000 Ft",
            postedAt = Instant.now()
        )
        assertFalse("GenericBankParser must never handle arbitrary messaging apps", parser.canHandle(whatsappNotif))

        val facebookNotif = RawNotification(
            packageName = "com.facebook.orca",
            title = "Messenger",
            text = "Vásárlás megerősítve",
            postedAt = Instant.now()
        )
        assertFalse(parser.canHandle(facebookNotif))

        val gmailNotif = RawNotification(
            packageName = "com.google.android.gm",
            title = "Receipt",
            text = "Fizetési bizonylat 10000 Ft",
            postedAt = Instant.now()
        )
        assertFalse(parser.canHandle(gmailNotif))
    }

    @Test
    fun testExtractAmountAndCurrencyPrefixAndSuffixFormats() {
        // Prefix currencies
        val p1 = extractAmountAndCurrency("You spent HUF804")
        assertNotNull(p1)
        assertEquals(80400L, p1!!.amountMinor)
        assertEquals("HUF", p1.currency)

        val p2 = extractAmountAndCurrency("Payment: HUF804.00 at merchant")
        assertNotNull(p2)
        assertEquals(80400L, p2!!.amountMinor)
        assertEquals("HUF", p2.currency)

        val p3 = extractAmountAndCurrency("Get HUF22,500 bonus")
        assertNotNull(p3)
        assertEquals(2250000L, p3!!.amountMinor)
        assertEquals("HUF", p3.currency)

        val p4 = extractAmountAndCurrency("Paid €12.50 to cafe")
        assertNotNull(p4)
        assertEquals(1250L, p4!!.amountMinor)
        assertEquals("EUR", p4.currency)

        val p5 = extractAmountAndCurrency("Spent $25.00 online")
        assertNotNull(p5)
        assertEquals(2500L, p5!!.amountMinor)
        assertEquals("USD", p5.currency)

        // Suffix currencies
        val s1 = extractAmountAndCurrency("14 500 Ft értékben")
        assertNotNull(s1)
        assertEquals(1450000L, s1!!.amountMinor)
        assertEquals("HUF", s1.currency)

        val s2 = extractAmountAndCurrency("Paid 45.50 EUR to GitHub")
        assertNotNull(s2)
        assertEquals(4550L, s2!!.amountMinor)
        assertEquals("EUR", s2.currency)

        // Balance line suppression
        val b1 = extractAmountAndCurrency("You spent HUF804\nHUF balance: HUF42,285.97")
        assertNotNull(b1)
        assertEquals(80400L, b1!!.amountMinor)
        assertEquals("HUF", b1.currency)
    }

    @Test
    fun testCsvParserWithEuropeanNumbersAndQuotes() {
        val csv = """
            "Dátum";"Leírás / Partner";"Összeg";"Pénznem"
            "2026-03-25";"Lidl Élelmiszer";"-14.500,50";"HUF"
            "2026-03-26";"Fizetés jóváírás";"650.000,00";"HUF"
        """.trimIndent()

        val stream = ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
        val rows = CsvParser.parse(stream)

        assertEquals(2, rows.size)

        assertEquals("Lidl Élelmiszer", rows[0].description)
        assertEquals(-1450050L, rows[0].amountMinor)
        assertEquals(TransactionDirection.EXPENSE, rows[0].direction)

        assertEquals("Fizetés jóváírás", rows[1].description)
        assertEquals(65000000L, rows[1].amountMinor)
        assertEquals(TransactionDirection.INCOME, rows[1].direction)
    }

    @Test
    fun testErsteNotificationParser() {
        val notif = RawNotification(
            packageName = "hu.erstebank.george.hungary",
            applicationLabel = "George",
            title = "Sikeres kártyás fizetés George",
            text = "Kártyás fizetés történt: 8.990 Ft értékben.",
            postedAt = Instant.now()
        )

        val parser = com.teppe21.finances.native.notification.ErsteParser()
        assertTrue(parser.canHandle(notif))

        val tx = parser.parse(notif)
        assertNotNull(tx)
        assertEquals(-899000L, tx!!.amountMinor)
        assertEquals("HUF", tx.currency)
    }

    @Test
    fun testMbhNotificationParser() {
        val notif = RawNotification(
            packageName = "hu.mbhbank.app",
            applicationLabel = "MBH Bank",
            title = "Kártyás tranzakció",
            text = "Vásárlás: -12.450 Ft",
            postedAt = Instant.now()
        )

        val parser = com.teppe21.finances.native.notification.MbhParser()
        assertTrue(parser.canHandle(notif))

        val tx = parser.parse(notif)
        assertNotNull(tx)
        assertEquals(-1245000L, tx!!.amountMinor)
        assertEquals("HUF", tx.currency)
    }

    @Test
    fun testWiseNotificationParser() {
        val notif = RawNotification(
            packageName = "com.transferwise.android",
            applicationLabel = "Wise",
            title = "You spent money with Wise",
            text = "You paid 45.50 EUR to GitHub",
            postedAt = Instant.now()
        )

        val parser = com.teppe21.finances.native.notification.WiseParser()
        assertTrue(parser.canHandle(notif))

        val tx = parser.parse(notif)
        assertNotNull(tx)
        assertEquals(-4550L, tx!!.amountMinor)
        assertEquals("EUR", tx.currency)
    }
}
