package com.teppe21.finances

import com.teppe21.finances.domain.model.PaymentMethod
import com.teppe21.finances.domain.model.RawNotification
import com.teppe21.finances.domain.model.TransactionDirection
import com.teppe21.finances.native.csv.CsvParser
import com.teppe21.finances.native.notification.OtpParser
import com.teppe21.finances.native.notification.RevolutParser
import com.teppe21.finances.native.receipt.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun testRevolutNotificationParser() {
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
        org.junit.Assert.assertNull("Security 2FA SMS codes must be discarded", tx)
    }
}

