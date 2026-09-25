package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Auth : Screen("auth", "Login")
    object PinLock : Screen("pin_lock", "PIN Lock")
    object QuickAdd : Screen("quick_add", "Jaldi Jodo")
    object History : Screen("history", "Kharche")
    object Udhaar : Screen("udhaar", "Udhaar")
    object Pots : Screen("pots", "Gullak")
    object Analysis : Screen("analysis", "Hisab")
    object Settings : Screen("settings", "Settings")
    object BusinessManagement : Screen("manage_businesses", "Business Khata")
    object CategoryManagement : Screen("manage_categories", "Categories")
    object BudgetSettings : Screen("manage_budgets", "Monthly Budget")
    object SheetsSync : Screen("sheets_sync", "Google Sheets Mirror")
    object ExcelSync : Screen("excel_sync", "Excel Sync")
    object GoogleDriveAndSheets : Screen("google_drive_sheets", "Google Drive & Sheets")
    object AdminPanel : Screen("admin_panel", "Admin Panel")
}
