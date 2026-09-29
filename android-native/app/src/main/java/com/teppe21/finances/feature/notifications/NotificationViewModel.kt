package com.teppe21.finances.feature.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teppe21.finances.FinancesApp
import com.teppe21.finances.domain.model.NotificationEvent
import com.teppe21.finances.domain.model.RawNotification
import com.teppe21.finances.native.notification.NotificationHelper
import com.teppe21.finances.native.notification.OtpParser
import com.teppe21.finances.native.notification.RevolutParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class NotificationUiState(
    val isPermissionGranted: Boolean = false,
    val pendingEvents: List<NotificationEvent> = emptyList(),
    val accounts: List<com.teppe21.finances.domain.model.Account> = emptyList(),
    val bankMappings: Map<String, String> = emptyMap(),
    val monitoredApps: List<String> = listOf("OTP Bank", "Revolut", "Erste Bank", "MBH Bank", "Wise")
)

class NotificationViewModel : ViewModel() {

    private val app = FinancesApp.instance
    private val notifRepo = app.notificationRepository
    private val ingestUseCase = app.ingestTransactionUseCase
    private val accountRepo = app.accountRepository
    private val prefsRepo = app.preferencesRepository

    private val _isPermissionGranted = MutableStateFlow(false)
    private val _bankMappings = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        loadBankMappings()
    }

    private fun loadBankMappings() {
        viewModelScope.launch {
            val supportedBanks = listOf("OTP Bank", "Revolut", "Erste Bank", "MBH Bank", "Wise")
            val map = mutableMapOf<String, String>()
            supportedBanks.forEach { bank ->
                prefsRepo.getBankMapping(bank)?.let { accId ->
                    map[bank] = accId
                }
            }
            _bankMappings.value = map
        }
    }

    fun updateBankMapping(bankName: String, accountId: String) {
        viewModelScope.launch {
            prefsRepo.setBankMapping(bankName, accountId)
            _bankMappings.value = _bankMappings.value + (bankName to accountId)
        }
    }

    val uiState: StateFlow<NotificationUiState> = combine(
        _isPermissionGranted,
        notifRepo.getPendingNotificationsFlow(),
        accountRepo.getAllAccountsFlow(),
        _bankMappings
    ) { granted, pending, accounts, mappings ->
        NotificationUiState(
            isPermissionGranted = granted,
            pendingEvents = pending,
            accounts = accounts,
            bankMappings = mappings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationUiState()
    )

    fun checkPermission(context: Context) {
        _isPermissionGranted.value = NotificationHelper.isNotificationAccessGranted(context)
    }

    fun openSettings(context: Context) {
        NotificationHelper.openNotificationListenerSettings(context)
    }

    fun approveEvent(event: NotificationEvent) {
        viewModelScope.launch {
            val updated = event.copy(processed = true)
            notifRepo.updateNotification(updated)
        }
    }

    fun dismissEvent(event: NotificationEvent) {
        viewModelScope.launch {
            val updated = event.copy(processed = true)
            notifRepo.updateNotification(updated)
        }
    }

    fun simulateTestNotification(type: String = "otp") {
        viewModelScope.launch {
            val raw = when (type) {
                "revolut" -> RawNotification(
                    packageName = "com.revolut.revolut",
                    applicationLabel = "Revolut",
                    title = "Fizetés a következőnek: LIDL",
                    text = "Elköltöttél 14 500 Ft-ot itt: LIDL.",
                    postedAt = Instant.now()
                )
                "revolut_transfer" -> RawNotification(
                    packageName = "com.revolut.revolut",
                    applicationLabel = "Revolut",
                    title = "Pénzküldés",
                    text = "Pénzt küldtél Kovács János részére. Összeg: 10 000 Ft.",
                    postedAt = Instant.now()
                )
                else -> RawNotification(
                    packageName = "hu.otpbank.smartbank",
                    applicationLabel = "OTP SmartBank",
                    title = "Sikeres kártyás vásárlás",
                    text = "Összeg: -4.500 Ft. Hely: SPAR BUDAPEST. Kártya: *1234.",
                    postedAt = Instant.now()
                )
            }

            val parser = if (type.startsWith("revolut")) RevolutParser() else OtpParser()
            val parsed = parser.parse(raw)

            if (parsed != null) {
                val accounts = accountRepo.getAllAccounts()
                val accId = accounts.firstOrNull { it.name.contains(parser.bankName, ignoreCase = true) }?.id
                    ?: accounts.firstOrNull()?.id ?: "acc_otp"

                ingestUseCase.execute(
                    com.teppe21.finances.domain.usecase.IngestionInput(
                        accountId = accId,
                        date = parsed.date,
                        amountMinor = parsed.amountMinor,
                        currency = parsed.currency,
                        direction = parsed.direction,
                        description = parsed.description,
                        merchant = parsed.merchant,
                        source = com.teppe21.finances.domain.model.TransactionSource.NOTIFICATION,
                        sourceAppPackage = raw.packageName,
                        confidence = parsed.confidence
                    )
                )
            }
        }
    }
}
