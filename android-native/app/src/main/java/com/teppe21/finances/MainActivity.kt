package com.teppe21.finances

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.teppe21.finances.core.navigation.Screen
import com.teppe21.finances.core.theme.PenzugyekTheme
import com.teppe21.finances.feature.accounts.AccountsScreen
import com.teppe21.finances.feature.analytics.AnalyticsScreen
import com.teppe21.finances.feature.categories.CategoriesScreen
import com.teppe21.finances.feature.dashboard.DashboardScreen
import com.teppe21.finances.feature.importcsv.CsvImportScreen
import com.teppe21.finances.feature.more.MoreScreen
import com.teppe21.finances.feature.notifications.NotificationAutomationScreen
import com.teppe21.finances.feature.receipt.ScanReceiptScreen
import com.teppe21.finances.feature.settings.SettingsScreen
import com.teppe21.finances.feature.subscreens.BudgetsScreen
import com.teppe21.finances.feature.subscreens.RecurringScreen
import com.teppe21.finances.feature.transactions.TransactionsScreen
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val isAppUnlocked = mutableStateOf(false)

    override fun attachBaseContext(newBase: android.content.Context) {
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(java.util.Locale.ENGLISH)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onStop() {
        super.onStop()
        // Re-lock the application when leaving to background
        isAppUnlocked.value = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val app = FinancesApp.instance
            val prefs by app.preferencesRepository.preferencesFlow.collectAsState(initial = null)

            val isDark = when (prefs?.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            PenzugyekTheme(darkTheme = isDark) {
                val lockType = prefs?.appLockType ?: "off"

                if (lockType != "off" && !isAppUnlocked.value) {
                    LockGateScreen(
                        lockType = lockType,
                        activity = this@MainActivity,
                        onUnlock = { isAppUnlocked.value = true },
                        verifyPin = { pin -> app.preferencesRepository.verifyPin(pin) }
                    )
                } else {
                    MainAppNavigation()
                }
            }
        }
    }
}

@Composable
fun LockGateScreen(
    lockType: String,
    activity: FragmentActivity,
    onUnlock: () -> Unit,
    verifyPin: suspend (String) -> Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var usePinFallback by remember { mutableStateOf(lockType == "pin") }

    val launchBiometric: () -> Unit = {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = androidx.biometric.BiometricPrompt(
            activity,
            executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    usePinFallback = true
                }
            }
        )

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Finances")
            .setSubtitle("Confirm your identity to view your finances")
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(lockType) {
        if (lockType == "biometric") {
            launchBiometric()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (lockType == "biometric" && !usePinFallback) Icons.Default.Fingerprint else Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Finances Secured",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (lockType == "biometric" && !usePinFallback) "Scan biometric credentials to proceed" else "Enter your 4-digit PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (lockType == "biometric" && !usePinFallback) {
                Button(
                    onClick = launchBiometric,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with Biometrics")
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = { usePinFallback = true }) {
                    Text("Enter PIN instead")
                }
            } else {
                OutlinedTextField(
                    value = pinText,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            pinText = input
                            errorMessage = null
                            if (input.length == 4) {
                                coroutineScope.launch {
                                    val valid = verifyPin(input)
                                    if (valid) {
                                        onUnlock()
                                    } else {
                                        errorMessage = "Incorrect PIN"
                                        pinText = ""
                                    }
                                }
                            }
                        }
                    },
                    label = { Text("4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.width(200.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (lockType == "biometric") {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { usePinFallback = false; launchBiometric() }) {
                        Text("Use Biometrics")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Transactions.route,
        Screen.Analytics.route,
        Screen.More.route
    )

    val isTopLevel = currentRoute in topLevelRoutes
    val hideBars = currentRoute == Screen.ScanReceipt.route

    Scaffold(
        topBar = {
            if (!hideBars) {
                TopAppBar(
                    title = {
                        Text(
                            text = getScreenTitle(currentRoute),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (!isTopLevel) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (!hideBars) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    val tabs = listOf(
                        Triple(Screen.Dashboard.route, stringResource(R.string.nav_home), Icons.Default.Home),
                        Triple(Screen.Transactions.route, stringResource(R.string.nav_transactions), Icons.AutoMirrored.Filled.ReceiptLong),
                        Triple(Screen.Analytics.route, stringResource(R.string.nav_analytics), Icons.Default.PieChart),
                        Triple(Screen.More.route, stringResource(R.string.nav_more), Icons.Default.Menu)
                    )

                    tabs.forEach { (route, title, icon) ->
                        val selected = currentRoute == route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (route == Screen.Dashboard.route) {
                                    if (currentRoute != Screen.Dashboard.route) {
                                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                                    }
                                } else if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(icon, contentDescription = title) },
                            label = { Text(title, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (hideBars) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            onNavigateToScanReceipt = { navController.navigate(Screen.ScanReceipt.route) },
                            onNavigateToImportCsv = { navController.navigate(Screen.CsvImport.route) },
                            onNavigateToNotifications = { navController.navigate(Screen.NotificationAutomation.route) },
                            onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                            onAddTransaction = { navController.navigate(Screen.Transactions.route) },
                            onSelectTransaction = { navController.navigate(Screen.Transactions.route) }
                        )
                    }

                    composable(Screen.Transactions.route) {
                        TransactionsScreen()
                    }

                    composable(Screen.Analytics.route) {
                        AnalyticsScreen()
                    }

                    composable(Screen.Accounts.route) {
                        AccountsScreen()
                    }

                    composable(Screen.More.route) {
                        MoreScreen(
                            onNavigateToNotifications = { navController.navigate(Screen.NotificationAutomation.route) },
                            onNavigateToCsvImport = { navController.navigate(Screen.CsvImport.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                            onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                            onNavigateToCategories = { navController.navigate(Screen.Categories.route) }
                        )
                    }

                    composable(Screen.ScanReceipt.route) {
                        ScanReceiptScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.NotificationAutomation.route) {
                        NotificationAutomationScreen()
                    }

                    composable(Screen.CsvImport.route) {
                        CsvImportScreen(
                            onImportFinished = {
                                navController.navigate(Screen.Transactions.route) {
                                    popUpTo(Screen.Dashboard.route)
                                }
                            }
                        )
                    }

                    composable(Screen.Budgets.route) {
                        BudgetsScreen()
                    }

                    composable(Screen.Recurring.route) {
                        RecurringScreen()
                    }

                    composable(Screen.Categories.route) {
                        CategoriesScreen()
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen()
                    }
                }
            }
        }
    )
}

@Composable
fun getScreenTitle(route: String?): String {
    return when (route) {
        Screen.Dashboard.route -> stringResource(R.string.app_name)
        Screen.Transactions.route -> stringResource(R.string.transactions_title)
        Screen.Analytics.route -> stringResource(R.string.analytics_title)
        Screen.Accounts.route -> stringResource(R.string.accounts_title)
        Screen.More.route -> stringResource(R.string.more_title)
        Screen.NotificationAutomation.route -> stringResource(R.string.notif_title)
        Screen.CsvImport.route -> stringResource(R.string.import_title)
        Screen.Budgets.route -> stringResource(R.string.budgets)
        Screen.Recurring.route -> stringResource(R.string.recurring)
        Screen.Categories.route -> stringResource(R.string.categories)
        Screen.Settings.route -> stringResource(R.string.settings)
        else -> stringResource(R.string.app_name)
    }
}
