package com.sajatpenzugyek.app.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sajatpenzugyek.app.R
import com.sajatpenzugyek.app.core.theme.Blue500
import com.sajatpenzugyek.app.core.theme.CategoryColorProvider
import com.sajatpenzugyek.app.core.theme.Emerald500
import com.sajatpenzugyek.app.core.theme.Rose500
import com.sajatpenzugyek.app.core.utils.CurrencyFormatter
import com.sajatpenzugyek.app.domain.model.Transaction
import com.sajatpenzugyek.app.domain.model.TransactionDirection
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddModal by remember { mutableStateOf(false) }
    var showSortModal by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = state.selectedPeriod == "this_month",
                        onClick = { viewModel.setPeriod("this_month") },
                        label = { Text(stringResource(R.string.filter_this_month)) }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedPeriod == "last_month",
                        onClick = { viewModel.setPeriod("last_month") },
                        label = { Text(stringResource(R.string.filter_last_month)) }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedPeriod == "3_months",
                        onClick = { viewModel.setPeriod("3_months") },
                        label = { Text(stringResource(R.string.filter_3_months)) }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedPeriod == "all",
                        onClick = { viewModel.setPeriod("all") },
                        label = { Text(stringResource(R.string.filter_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = state.onlyRecurring,
                        onClick = { viewModel.toggleRecurring() },
                        leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(stringResource(R.string.filter_recurring)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sort & Count Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = true,
                    onClick = { showSortModal = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort_by),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        val sortLabel = when (state.sortOrder) {
                            TransactionSortOrder.NEWEST -> stringResource(R.string.sort_newest)
                            TransactionSortOrder.OLDEST -> stringResource(R.string.sort_oldest)
                            TransactionSortOrder.AMOUNT_DESC -> stringResource(R.string.sort_amount_desc)
                            TransactionSortOrder.AMOUNT_ASC -> stringResource(R.string.sort_amount_asc)
                            TransactionSortOrder.MERCHANT_ASC -> stringResource(R.string.sort_merchant_asc)
                            TransactionSortOrder.MERCHANT_DESC -> stringResource(R.string.sort_merchant_desc)
                        }
                        Text(sortLabel, style = MaterialTheme.typography.labelSmall)
                    }
                )

                Text(
                    text = "${state.filteredTransactions.size} tranzakció",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Transactions List
            if (state.filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_transactions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.filteredTransactions, key = { it.id }) { tx ->
                        TransactionCard(
                            transaction = tx,
                            onClick = { viewModel.selectTransaction(tx) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }

        // Floating Action Button for New Transaction
        FloatingActionButton(
            onClick = { showAddModal = true },
            containerColor = Blue500,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_transaction))
        }

        // Sort Options Bottom Sheet
        if (showSortModal) {
            ModalBottomSheet(
                onDismissRequest = { showSortModal = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sort_by),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val sortOptions = listOf(
                        TransactionSortOrder.NEWEST to R.string.sort_newest,
                        TransactionSortOrder.OLDEST to R.string.sort_oldest,
                        TransactionSortOrder.AMOUNT_DESC to R.string.sort_amount_desc,
                        TransactionSortOrder.AMOUNT_ASC to R.string.sort_amount_asc,
                        TransactionSortOrder.MERCHANT_ASC to R.string.sort_merchant_asc,
                        TransactionSortOrder.MERCHANT_DESC to R.string.sort_merchant_desc
                    )
                    sortOptions.forEach { (order, stringRes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setSortOrder(order)
                                    showSortModal = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(stringRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (state.sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                color = if (state.sortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (state.sortOrder == order) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

        // Transaction Detail Bottom Sheet
        if (state.selectedTransaction != null) {
            val selected = state.selectedTransaction!!
            val sheetState = rememberModalBottomSheetState()

            ModalBottomSheet(
                onDismissRequest = { viewModel.selectTransaction(null) },
                sheetState = sheetState
            ) {
                TransactionDetailContent(
                    transaction = selected,
                    onDelete = { transactionToDelete = selected },
                    onClose = { viewModel.selectTransaction(null) }
                )
            }
        }

        // Add Transaction Modal
        if (showAddModal) {
            AddTransactionDialog(
                accounts = state.accounts,
                categories = state.categories,
                onDismiss = { showAddModal = false },
                onConfirm = { accId, amtMinor, dir, desc, merch, catId, date ->
                    viewModel.addManualTransaction(accId, amtMinor, dir, desc, merch, catId, date)
                    showAddModal = false
                }
            )
        }

        // Delete Confirmation Dialog
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text(stringResource(R.string.delete_confirm_title)) },
                text = { Text(stringResource(R.string.delete_confirm_msg)) },
                confirmButton = {
                    Button(
                        onClick = {
                            transactionToDelete?.let { viewModel.deleteTransaction(it) }
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isIncome = transaction.direction == TransactionDirection.INCOME ||
        (transaction.direction == TransactionDirection.REFUND && transaction.amountMinor > 0)
    val isTransfer = transaction.direction == TransactionDirection.TRANSFER
    val catColor = CategoryColorProvider.getColor(transaction.categoryId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isTransfer -> Color(0xFF6366F1).copy(alpha = 0.15f)
                        isIncome -> Emerald500.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isTransfer -> Icons.Default.SwapHoriz
                                isIncome -> Icons.Default.ShoppingBag
                                else -> Icons.Default.ShoppingBag
                            },
                            contentDescription = null,
                            tint = when {
                                isTransfer -> Color(0xFF6366F1)
                                isIncome -> Emerald500
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(catColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = transaction.merchant?.ifBlank { null } ?: transaction.description,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.date} • ${transaction.source.name.lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = CurrencyFormatter.formatSigned(transaction.amountMinor, transaction.currency),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = when {
                    isTransfer -> Color(0xFF6366F1)
                    isIncome -> Emerald500
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun TransactionDetailContent(
    transaction: Transaction,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = transaction.merchant ?: transaction.description,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }

        Text(
            text = CurrencyFormatter.formatSigned(transaction.amountMinor, transaction.currency),
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (transaction.amountMinor > 0) Emerald500 else MaterialTheme.colorScheme.onSurface
        )

        DetailRow(label = stringResource(R.string.date), value = transaction.date.toString())
        DetailRow(label = stringResource(R.string.category), value = transaction.categoryId ?: "other")
        DetailRow(label = stringResource(R.string.source), value = transaction.source.name)
        if (!transaction.notes.isNullOrBlank()) {
            DetailRow(label = stringResource(R.string.note), value = transaction.notes)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = Rose500),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.delete))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AddTransactionDialog(
    accounts: List<com.sajatpenzugyek.app.domain.model.Account>,
    categories: List<com.sajatpenzugyek.app.domain.model.Category>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: String, amountMinor: Long, direction: TransactionDirection, description: String, merchant: String?, categoryId: String?, date: LocalDate) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "acc_otp") }
    var selectedCategoryId by remember { mutableStateOf("food") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_transaction)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isExpense = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpense) Rose500 else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.expenses))
                    }
                    Button(
                        onClick = { isExpense = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isExpense) Emerald500 else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.income))
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.amount) + " (Ft)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val context = LocalContext.current
                Text(
                    text = stringResource(R.string.category),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.getDisplayName(context), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text(stringResource(R.string.merchant)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text(stringResource(R.string.note)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val major = amountText.toLongOrNull() ?: 0L
                    val minor = major * 100L
                    val dir = if (isExpense) TransactionDirection.EXPENSE else TransactionDirection.INCOME
                    val desc = descriptionText.ifBlank { merchantText.ifBlank { "Kézi tétel" } }
                    onConfirm(selectedAccountId, if (isExpense) -minor else minor, dir, desc, merchantText.ifBlank { null }, selectedCategoryId, LocalDate.now())
                },
                enabled = amountText.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
