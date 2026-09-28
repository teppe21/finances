package com.sajatpenzugyek.app.feature.receipt

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajatpenzugyek.app.PenzugyekApp
import com.sajatpenzugyek.app.domain.model.Account
import com.sajatpenzugyek.app.domain.model.AccountType
import com.sajatpenzugyek.app.domain.model.Category
import com.sajatpenzugyek.app.domain.model.PaymentMethod
import com.sajatpenzugyek.app.domain.model.ReceiptScan
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import com.sajatpenzugyek.app.domain.model.TransactionSource
import com.sajatpenzugyek.app.domain.usecase.IngestionInput
import com.sajatpenzugyek.app.native.receipt.ReceiptOcrEngine
import com.sajatpenzugyek.app.native.receipt.ReceiptParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReceiptUiState(
    val isProcessing: Boolean = false,
    val scannedReceipt: ReceiptScan? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val errorMessage: String? = null
)

class ReceiptViewModel : ViewModel() {

    private val app = PenzugyekApp.instance
    private val receiptRepo = app.receiptRepository
    private val accountRepo = app.accountRepository
    private val catRepo = app.categoryRepository
    private val ingestUseCase = app.ingestTransactionUseCase

    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val accounts = accountRepo.getAllAccounts()
            val categories = catRepo.getAllCategories()
            _uiState.value = _uiState.value.copy(
                accounts = accounts,
                categories = categories
            )
        }
    }

    fun processBitmap(context: Context, bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val engine = ReceiptOcrEngine(context)
                val scan = engine.recognizeFromBitmap(bitmap)
                handleScanResult(scan)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.message ?: "OCR error"
                )
            }
        }
    }

    fun processUri(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val engine = ReceiptOcrEngine(context)
                val scan = engine.recognizeFromUri(uri)
                handleScanResult(scan)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.message ?: "OCR error"
                )
            }
        }
    }

    fun processDemoText(sampleText: String) {
        _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
        viewModelScope.launch {
            val scan = ReceiptParser.parse(sampleText)
            handleScanResult(scan)
        }
    }

    private fun handleScanResult(scan: ReceiptScan) {
        val accounts = _uiState.value.accounts
        // If cash detected, suggest Cash Wallet account!
        val suggestedAccount = if (scan.paymentMethod == PaymentMethod.CASH) {
            accounts.firstOrNull { it.type == AccountType.CASH } ?: accounts.firstOrNull()
        } else {
            accounts.firstOrNull { it.type == AccountType.BANK } ?: accounts.firstOrNull()
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = false,
            scannedReceipt = scan,
            selectedAccountId = suggestedAccount?.id,
            selectedCategoryId = "food" // default
        )
    }

    fun setSelectedAccount(accountId: String) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId)
    }

    fun setSelectedCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun clearScan() {
        _uiState.value = _uiState.value.copy(scannedReceipt = null, errorMessage = null)
    }

    fun confirmSave(
        merchant: String,
        amountMinor: Long,
        date: LocalDate,
        accountId: String,
        categoryId: String,
        currency: String = "HUF",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val scan = _uiState.value.scannedReceipt
            if (scan != null) {
                receiptRepo.insertReceipt(scan.copy(merchant = merchant, totalMinor = amountMinor, date = date, currency = currency))
            }

            ingestUseCase.execute(
                IngestionInput(
                    accountId = accountId,
                    date = date,
                    amountMinor = -kotlin.math.abs(amountMinor),
                    currency = currency,
                    direction = TransactionDirection.EXPENSE,
                    description = "$merchant (Receipt)",
                    merchant = merchant,
                    categoryId = categoryId,
                    source = TransactionSource.RECEIPT,
                    receiptId = scan?.id
                )
            )

            clearScan()
            onSuccess()
        }
    }
}
