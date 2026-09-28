package com.sajatpenzugyek.app.data.local.database

import androidx.room.TypeConverter
import com.sajatpenzugyek.app.domain.model.AccountType
import com.sajatpenzugyek.app.domain.model.NotificationParseStatus
import com.sajatpenzugyek.app.domain.model.PaymentMethod
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import com.sajatpenzugyek.app.domain.model.TransactionSource
import java.time.Instant
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromDirection(value: TransactionDirection?): String? = value?.name

    @TypeConverter
    fun toDirection(value: String?): TransactionDirection? =
        value?.let { runCatching { TransactionDirection.valueOf(it) }.getOrDefault(TransactionDirection.UNKNOWN) }

    @TypeConverter
    fun fromSource(value: TransactionSource?): String? = value?.name

    @TypeConverter
    fun toSource(value: String?): TransactionSource? =
        value?.let { runCatching { TransactionSource.valueOf(it) }.getOrDefault(TransactionSource.MANUAL) }

    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? =
        value?.let { runCatching { AccountType.valueOf(it) }.getOrDefault(AccountType.BANK) }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? =
        value?.let { runCatching { PaymentMethod.valueOf(it) }.getOrDefault(PaymentMethod.UNKNOWN) }

    @TypeConverter
    fun fromNotificationParseStatus(value: NotificationParseStatus?): String? = value?.name

    @TypeConverter
    fun toNotificationParseStatus(value: String?): NotificationParseStatus? =
        value?.let { runCatching { NotificationParseStatus.valueOf(it) }.getOrDefault(NotificationParseStatus.NEEDS_REVIEW) }
}
