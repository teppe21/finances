package com.sajatpenzugyek.app

import com.sajatpenzugyek.app.domain.model.PaymentMethod
import com.sajatpenzugyek.app.domain.model.RawNotification
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import com.sajatpenzugyek.app.native.csv.CsvParser
import com.sajatpenzugyek.app.native.notification.OtpParser
import com.sajatpenzugyek.app.native.notification.RevolutParser
import com.sajatpenzugyek.app.native.receipt.ReceiptParser
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
}
