package com.sajatpenzugyek.app.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajatpenzugyek.app.PenzugyekApp
import com.sajatpenzugyek.app.domain.model.Account
import com.sajatpenzugyek.app.domain.model.AccountType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AccountsViewModel : ViewModel() {

    private val app = PenzugyekApp.instance
    private val accountRepo = app.accountRepository

    val accounts: StateFlow<List<Account>> = accountRepo.getAllAccountsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addAccount(name: String, institution: String, type: AccountType, openingBalanceMinor: Long) {
        viewModelScope.launch {
            val acc = Account(
                id = UUID.randomUUID().toString(),
                name = name,
                institution = institution,
                type = type,
                currency = "HUF",
                openingBalanceMinor = openingBalanceMinor,
                currentBalanceMinor = openingBalanceMinor
            )
            accountRepo.insertAccount(acc)
        }
    }
}
