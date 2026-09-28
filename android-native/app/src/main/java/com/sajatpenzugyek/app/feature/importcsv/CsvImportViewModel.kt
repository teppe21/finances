package com.sajatpenzugyek.app.feature.importcsv

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajatpenzugyek.app.PenzugyekApp
import com.sajatpenzugyek.app.domain.model.Account
import com.sajatpenzugyek.app.domain.model.TransactionSource
import com.sajatpenzugyek.app.domain.usecase.DeduplicationStatus
import com.sajatpenzugyek.app.domain.usecase.IngestionInput
import com.sajatpenzugyek.app.native.csv.CsvParser
import com.sajatpenzugyek.app.native.csv.ParsedCsvRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CsvImportUiState(
    val fileName: String? = null,
    val isAnalyzing: Boolean = false,
    val isImporting: Boolean = false,
    val parsedRows: List<ParsedCsvRow> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: String? = null,
    val newCount: Int = 0,
    val duplicateCount: Int = 0,
    val importComplete: Boolean = false,
    val errorMessage: String? = null
)

class CsvImportViewModel : ViewModel() {

    private val app = PenzugyekApp.instance
    private val accountRepo = app.accountRepository
    private val txRepo = app.transactionRepository
    private val ingestUseCase = app.ingestTransactionUseCase
    private val dedupUseCase = app.deduplicateUseCase

    private val _uiState = MutableStateFlow(CsvImportUiState())
    val uiState: StateFlow<CsvImportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = accountRepo.getAllAccounts()
            _uiState.value = _uiState.value.copy(
                accounts = accounts,
                selectedAccountId = accounts.firstOrNull()?.id
            )
        }
    }

    fun setSelectedAccount(accountId: String) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId)
    }

    fun loadFromUri(context: Context, uri: Uri, fileName: String?) {
        _uiState.value = _uiState.value.copy(isAnalyzing = true, fileName = fileName, errorMessage = null)
        viewModelScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        CsvParser.parse(stream)
                    } ?: emptyList()
                }

                if (rows.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        errorMessage = "No valid transaction rows found in the CSV file."
                    )
                    return@launch
                }

                // Analyze duplicates against current database
                val existing = txRepo.getAllTransactions()
                var duplicates = 0
                var newItems = 0

                rows.forEach { row ->
                    val fp = dedupUseCase.generateFingerprint(
                        date = row.date,
                        amountMinor = row.amountMinor,
                        currency = row.currency,
                        description = row.description,
                        merchant = row.merchant
                    )
                    val result = dedupUseCase.execute(fp, row.date, row.amountMinor, row.currency, null, existing)
                    if (result.status == DeduplicationStatus.EXACT_DUPLICATE) {
                        duplicates++
                    } else {
                        newItems++
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    parsedRows = rows,
                    newCount = newItems,
                    duplicateCount = duplicates
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = e.message ?: "Error reading CSV file"
                )
            }
        }
    }

    fun executeImport(onSuccess: () -> Unit) {
        val rows = _uiState.value.parsedRows
        val accountId = _uiState.value.selectedAccountId ?: "acc_otp"

        _uiState.value = _uiState.value.copy(isImporting = true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                rows.forEach { row ->
                    ingestUseCase.execute(
                        IngestionInput(
                            accountId = accountId,
                            date = row.date,
                            amountMinor = row.amountMinor,
                            currency = row.currency,
                            direction = row.direction,
                            description = row.description,
                            merchant = row.merchant,
                            source = TransactionSource.CSV
                        )
                    )
                }
            }

            _uiState.value = _uiState.value.copy(isImporting = false, importComplete = true)
            onSuccess()
        }
    }
}
