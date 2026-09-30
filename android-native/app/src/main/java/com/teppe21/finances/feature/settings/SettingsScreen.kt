package com.teppe21.finances.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teppe21.finances.R
import com.teppe21.finances.core.theme.Rose500
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val prefs by viewModel.preferences.collectAsState()
    val isSyncing by viewModel.isSyncingRates.collectAsState()
    val lastSync by viewModel.lastRatesSync.collectAsState()
    val currentRates by viewModel.currentRates.collectAsState()

    var showEditRatesDialog by remember { mutableStateOf(false) }

    // PIN & Lock States
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showDisableLockDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var disablePinInput by remember { mutableStateOf("") }
    var disablePinError by remember { mutableStateOf<String?>(null) }

    // Delete All Data State
    var showDeleteAllDialog by remember { mutableStateOf(false) }

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
                        listOf("HUF", "EUR", "USD", "GBP", "CHF").forEach { curr ->
                            FilterChip(
                                selected = prefs.currency == curr,
                                onClick = { viewModel.setCurrency(curr) },
                                label = { Text(curr) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic FX Rates Sync",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (prefs.autoSyncFxRates) {
                                    "Periodic sync from European Central Bank via frankfurter.app"
                                } else {
                                    "Air-gapped offline mode: no network requests will be made"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.autoSyncFxRates,
                            onCheckedChange = { viewModel.setAutoSyncFxRates(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Exchange Rates Cache",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (lastSync != null) "Last updated: ${com.teppe21.finances.core.utils.DateFormatter.format(lastSync!!.atZone(java.time.ZoneId.systemDefault()).toLocalDate())}" else "Default rates cached",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showEditRatesDialog = true },
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Edit Rates", style = MaterialTheme.typography.labelMedium)
                            }

                            IconButton(
                                onClick = { viewModel.refreshExchangeRates() },
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Protect your financial data with an on-device biometric challenge or salted 4-digit PIN code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.appLockType == "off",
                            onClick = {
                                if (prefs.appLockType != "off") {
                                    disablePinInput = ""
                                    disablePinError = null
                                    showDisableLockDialog = true
                                }
                            },
                            label = { Text(stringResource(R.string.app_lock_off)) }
                        )
                        FilterChip(
                            selected = prefs.appLockType == "pin",
                            onClick = {
                                pinInput = ""
                                pinConfirmInput = ""
                                pinError = null
                                showPinSetupDialog = true
                            },
                            label = { Text(stringResource(R.string.app_lock_pin)) }
                        )
                        FilterChip(
                            selected = prefs.appLockType == "biometric",
                            onClick = {
                                val biometricManager = BiometricManager.from(context)
                                val canAuth = biometricManager.canAuthenticate(
                                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                                )
                                if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                                    if (prefs.pinHash.isBlank()) {
                                        // PIN required first as fallback
                                        pinInput = ""
                                        pinConfirmInput = ""
                                        pinError = null
                                        showPinSetupDialog = true
                                        Toast.makeText(context, "Please set a PIN first as backup for biometrics", Toast.LENGTH_LONG).show()
                                    } else {
                                        viewModel.setBiometricLock()
                                        Toast.makeText(context, "Biometric lock activated", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Biometric authentication is not available on this device", Toast.LENGTH_LONG).show()
                                }
                            },
                            label = { Text("Biometric") }
                        )
                    }
                }
            }
        }

        // 5. Data Management & Privacy
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Load portfolio demo data for demonstration, or wipe all financial data from this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.loadSampleData()
                                Toast.makeText(context, "Demo data loaded", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load Demo")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.clearSampleData()
                                Toast.makeText(context, "Demo data cleared", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Demo")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All Financial Data")
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
                        text = "Privacy Guarantee:\nAll accounts, transactions, balances, and receipts are stored 100% locally in on-device SQLite. Zero personal or financial telemetry is ever transmitted over the network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://teppe21.github.io/finances/privacy-policy.html")
                            )
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Full Privacy Policy")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("Set 4-Digit Security PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a 4-digit numeric PIN to protect your app on launch and resume.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text("Enter 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pinConfirmInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirmInput = it },
                        label = { Text("Confirm 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            pinError = "PIN must be exactly 4 digits"
                            return@Button
                        }
                        if (pinInput != pinConfirmInput) {
                            pinError = "PINs do not match"
                            return@Button
                        }
                        viewModel.setPinLock(pinInput)
                        showPinSetupDialog = false
                        Toast.makeText(context, "PIN protection enabled", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Disable Lock Dialog
    if (showDisableLockDialog) {
        AlertDialog(
            onDismissRequest = { showDisableLockDialog = false },
            title = { Text("Enter PIN to Disable Lock", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Please verify your 4-digit PIN to turn off app lock security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = disablePinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) disablePinInput = it },
                        label = { Text("Current 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (disablePinError != null) {
                        Text(
                            text = disablePinError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val isValid = viewModel.verifyPin(disablePinInput)
                            if (isValid) {
                                viewModel.disableLock()
                                showDisableLockDialog = false
                                Toast.makeText(context, "App lock disabled", Toast.LENGTH_SHORT).show()
                            } else {
                                disablePinError = "Incorrect PIN code"
                            }
                        }
                    }
                ) {
                    Text("Verify & Turn Off")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableLockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete All Data Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Financial Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will permanently wipe all transactions, accounts, custom categories, category rules, and receipts stored on this device. All accounts will be reset to a clean 0 balance. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData {
                            Toast.makeText(context, "All financial data has been wiped", Toast.LENGTH_LONG).show()
                        }
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual Rates Editor Dialog
    if (showEditRatesDialog) {
        var hufRate by remember { mutableStateOf(currentRates["HUF"]?.toPlainString() ?: "395.0") }
        var usdRate by remember { mutableStateOf(currentRates["USD"]?.toPlainString() ?: "1.08") }
        var gbpRate by remember { mutableStateOf(currentRates["GBP"]?.toPlainString() ?: "0.85") }
        var chfRate by remember { mutableStateOf(currentRates["CHF"]?.toPlainString() ?: "0.95") }

        AlertDialog(
            onDismissRequest = { showEditRatesDialog = false },
            title = { Text("Manual Exchange Rates (Base: 1 EUR)", fontWeight = FontWeight.Bold) },
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
