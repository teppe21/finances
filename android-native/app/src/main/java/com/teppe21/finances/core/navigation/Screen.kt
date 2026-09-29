package com.teppe21.finances.core.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object Analytics : Screen("analytics")
    data object Accounts : Screen("accounts")
    data object More : Screen("more")
    data object ScanReceipt : Screen("scan_receipt")
    data object NotificationAutomation : Screen("notification_automation")
    data object CsvImport : Screen("csv_import")
    data object Budgets : Screen("budgets")
    data object Recurring : Screen("recurring")
    data object Categories : Screen("categories")
    data object Settings : Screen("settings")
}
