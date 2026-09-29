package com.teppe21.finances.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teppe21.finances.R
import java.math.BigDecimal

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val prefs by viewModel.preferences.collectAsState()
    val isSyncing by viewModel.isSyncingRates.collectAsState()
    val lastSync by viewModel.lastRatesSync.collectAsState()
    val currentRates by viewModel.currentRates.collectAsState()

    var showEditRatesDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // 1. Appearance / Theme
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.theme),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.theme == "dark",
                            onClick = { viewModel.setTheme("dark") },
                            label = { Text(stringResource(R.string.theme_dark)) }
                        )
                        FilterChip(
                            selected = prefs.theme == "light",
                            onClick = { viewModel.setTheme("light") },
                            label = { Text(stringResource(R.string.theme_light)) }
                        )
                        FilterChip(
                            selected = prefs.theme == "system",
                            onClick = { viewModel.setTheme("system") },
                            label = { Text(stringResource(R.string.theme_system)) }
                        )
                    }
                }
            }
        }

        // 2. Language
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = { Text("English") }
                        )
                    }
                }
            }
        }

        // 3. Currency & FX
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.currency == "HUF",
                            onClick = { viewModel.setCurrency("HUF") },
                            label = { Text("HUF (Ft)") }
                        )
                        FilterChip(
                            selected = prefs.currency == "EUR",
                            onClick = { viewModel.setCurrency("EUR") },
                            label = { Text("EUR (€)") }
                        )
                        FilterChip(
                            selected = prefs.currency == "USD",
                            onClick = { viewModel.setCurrency("USD") },
                            label = { Text("USD ($)") }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.currency == "GBP",
                            onClick = { viewModel.setCurrency("GBP") },
                            label = { Text("GBP (£)") }
                        )
                        FilterChip(
                            selected = prefs.currency == "CHF",
                            onClick = { viewModel.setCurrency("CHF") },
                            label = { Text("CHF (Fr)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live FX Auto-Sync Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Live FX Auto-Sync",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (prefs.autoSyncFxRates) "Fetches ECB market rates automatically" else "Disabled: 100% offline & air-gapped",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.autoSyncFxRates,
                            onCheckedChange = { viewModel.setAutoSyncFxRates(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status and action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val syncText = if (!prefs.autoSyncFxRates) {
                                "Air-gapped (Offline Only)"
                            } else if (isSyncing) {
                                "Updating real FX rates..."
                            } else if (lastSync != null) {
                                val formatted = java.time.format.DateTimeFormatter
                                    .ofPattern("yyyy-MM-dd HH:mm")
                                    .withZone(java.time.ZoneId.systemDefault())
                                    .format(lastSync)
                                "ECB / Frankfurter (live)\nUpdated: $formatted"
                            } else {
                                "ECB / Frankfurter (cached)"
                            }
                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showEditRatesDialog = true },
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("Edit Rates", style = MaterialTheme.typography.labelSmall)
                            }

                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.refreshExchangeRates() },
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    androidx.compose.material3.Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                                        contentDescription = "Refresh exchange rates"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. App Lock / Security
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.security),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.appLockType == "off",
                            onClick = { viewModel.setAppLock("off") },
                            label = { Text(stringResource(R.string.app_lock_off)) }
                        )
                        FilterChip(
                            selected = prefs.appLockType == "pin",
                            onClick = { viewModel.setAppLock("pin", "1234") },
                            label = { Text(stringResource(R.string.app_lock_pin)) }
                        )
                    }
                }
            }
        }

        // 5. About & Privacy
        // 5. Demo & Portfolio Data
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Demo & Portfolio Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Populate your accounts with representative portfolio demo balances. Fresh installs start with 0 balance by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.loadSampleData() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load Sample Data")
                    }
                }
            }
        }

        // 6. About & Privacy
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.version, "2.0.0 (Native Compose)"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.about_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.privacy_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.privacy_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Privacy & Air-Gap Guarantee:\nAll your accounts, transactions, balances, and receipts are stored 100% locally in on-device SQLite. Zero personal or financial data is ever transmitted over the network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }

    // Manual Rates Editor Dialog
    if (showEditRatesDialog) {
        var hufRate by remember { mutableStateOf(currentRates["HUF"]?.toPlainString() ?: "395.0") }
        var usdRate by remember { mutableStateOf(currentRates["USD"]?.toPlainString() ?: "1.08") }
        var gbpRate by remember { mutableStateOf(currentRates["GBP"]?.toPlainString() ?: "0.85") }
        var chfRate by remember { mutableStateOf(currentRates["CHF"]?.toPlainString() ?: "0.95") }

        AlertDialog(
            onDismissRequest = { showEditRatesDialog = false },
            title = { Text("Manual Exchange Rates (Base: 1 EUR)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Set custom exchange rates against 1 EUR. These rates will be saved locally in your database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = hufRate,
                        onValueChange = { hufRate = it },
                        label = { Text("HUF per EUR") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = usdRate,
                        onValueChange = { usdRate = it },
                        label = { Text("USD per EUR") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gbpRate,
                        onValueChange = { gbpRate = it },
                        label = { Text("GBP per EUR") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = chfRate,
                        onValueChange = { chfRate = it },
                        label = { Text("CHF per EUR") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        hufRate.toDoubleOrNull()?.let { viewModel.setManualRate("HUF", BigDecimal.valueOf(it)) }
                        usdRate.toDoubleOrNull()?.let { viewModel.setManualRate("USD", BigDecimal.valueOf(it)) }
                        gbpRate.toDoubleOrNull()?.let { viewModel.setManualRate("GBP", BigDecimal.valueOf(it)) }
                        chfRate.toDoubleOrNull()?.let { viewModel.setManualRate("CHF", BigDecimal.valueOf(it)) }
                        showEditRatesDialog = false
                    }
                ) {
                    Text("Save Rates")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditRatesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
