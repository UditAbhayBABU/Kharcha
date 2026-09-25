package com.example.ui.navigation

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.KharchaApplication
import com.example.data.model.*
import com.example.ui.screens.analysis.AnalysisScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.lock.PinLockScreen
import com.example.ui.screens.pots.PotsScreen
import com.example.ui.screens.quickadd.QuickAddScreen
import com.example.ui.screens.settings.*
import com.example.ui.screens.udhaar.UdhaarScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    initialShortcutAction: String? = null
) {
    val context = LocalContext.current
    val app = KharchaApplication.instance
    val authRepo = app.authRepository
    val kharchaRepo = app.kharchaRepository
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by authRepo.currentUser.collectAsStateWithLifecycle()
    val userProfile by authRepo.userProfile.collectAsStateWithLifecycle()
    val isAppUnlocked by authRepo.isAppUnlocked.collectAsStateWithLifecycle()

    val userId = currentUser?.uid ?: "guest_user"

    // Seed default categories for this user if first time
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            kharchaRepo.seedDefaultCategories(userId)
            authRepo.loadUserProfile(userId)
        }
    }

    // Reactive streams from local Room DB
    val expenses by kharchaRepo.getExpenses(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by kharchaRepo.getCategories(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val businesses by kharchaRepo.getBusinesses(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val pots by kharchaRepo.getPots(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val udhaarParties by kharchaRepo.getUdhaarParties(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    val budgets by kharchaRepo.getBudgets(userId).collectAsStateWithLifecycle(initialValue = emptyList())

    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (initialShortcutAction == "quick_add") Screen.QuickAdd
            else if (initialShortcutAction == "udhaar") Screen.Udhaar
            else Screen.History
        )
    }

    var isSyncing by remember { mutableStateOf(false) }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var pendingExcelCount by remember { mutableStateOf(0) }
    var pendingSheetsCount by remember { mutableStateOf(0) }
    var adminUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    // Periodic pending counts check
    LaunchedEffect(expenses) {
        pendingExcelCount = kharchaRepo.getPendingExcelCount(userId)
        pendingSheetsCount = kharchaRepo.getPendingSheetsCount(userId)
    }

    // Handle App Lock:
    // User requirement: "Quick Add must NOT require PIN/biometric, because its purpose is instant expense recording."
    val isPinRequired = authRepo.isPinLockEnabled() && !isAppUnlocked && currentScreen != Screen.QuickAdd

    // If user is not authenticated, show AuthScreen (Email/Pass or Google)
    if (currentUser == null) {
        AuthScreen(
            onSignIn = { email, pass ->
                authLoading = true
                authError = null
                coroutineScope.launch {
                    val result = authRepo.signInWithEmail(email, pass)
                    authLoading = false
                    result.onFailure { authError = it.localizedMessage ?: "Login me samasya aayi" }
                }
            },
            onSignUp = { email, pass, name ->
                authLoading = true
                authError = null
                coroutineScope.launch {
                    val result = authRepo.signUpWithEmail(email, pass, name)
                    authLoading = false
                    result.onFailure { authError = it.localizedMessage ?: "Registration fail hua" }
                }
            },
            onForgotPassword = { email ->
                coroutineScope.launch {
                    authRepo.sendPasswordResetEmail(email)
                    Toast.makeText(context, "Password reset link bhej diya gaya!", Toast.LENGTH_LONG).show()
                }
            },
            isLoading = authLoading,
            errorMessage = authError
        )
        return
    }

    // If PIN lock is active and user tries to access a protected screen, show PIN Lock Screen
    if (isPinRequired) {
        PinLockScreen(
            onUnlockSuccess = { authRepo.unlockAppForSession() },
            onVerifyPin = { authRepo.verifyPin(it) },
            onOpenQuickAdd = { currentScreen = Screen.QuickAdd }
        )
        return
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen in listOf(Screen.History, Screen.QuickAdd, Screen.Udhaar, Screen.Pots, Screen.Analysis, Screen.Settings)) {
                KharchaBottomBar(
                    currentScreen = currentScreen,
                    onSelectScreen = { currentScreen = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.QuickAdd -> {
                    BackHandler { currentScreen = Screen.History }
                    QuickAddScreen(
                        userId = userId,
                        categories = categories,
                        businesses = businesses,
                        pots = pots,
                        udhaarParties = udhaarParties,
                        budgets = budgets,
                        isBudgetFeatureEnabled = userProfile?.isBudgetFeatureEnabled == true,
                        onSaveExpense = { expense ->
                            coroutineScope.launch {
                                kharchaRepo.addExpense(
                                    expense = expense,
                                    sheetsUrl = userProfile?.sheetsUrl ?: "",
                                    autoSyncSheets = userProfile?.sheetsAutoSync ?: true
                                )
                            }
                        },
                        onNavigateBack = { currentScreen = Screen.History },
                        snackbarHostState = snackbarHostState
                    )
                }

                Screen.History -> {
                    HistoryScreen(
                        expenses = expenses,
                        categories = categories,
                        businesses = businesses,
                        onDeleteExpense = { exp ->
                            coroutineScope.launch { kharchaRepo.deleteExpense(exp.id, userId) }
                        },
                        onRestoreExpense = { exp ->
                            coroutineScope.launch { kharchaRepo.addExpense(exp) }
                        },
                        onSyncNow = {
                            isSyncing = true
                            coroutineScope.launch {
                                val res = kharchaRepo.syncAllWithCloud(userId)
                                isSyncing = false
                                Toast.makeText(context, res.getOrDefault("Sync complete"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        isSyncing = isSyncing,
                        snackbarHostState = snackbarHostState
                    )
                }

                Screen.Udhaar -> {
                    BackHandler { currentScreen = Screen.History }
                    UdhaarScreen(
                        userId = userId,
                        parties = udhaarParties,
                        onAddParty = { party -> coroutineScope.launch { kharchaRepo.addUdhaarParty(party) } },
                        onRecordTransaction = { partyId, amount, isRepayment, note ->
                            coroutineScope.launch {
                                kharchaRepo.recordUdhaarTransaction(partyId, amount, isRepayment, note, userId)
                            }
                        },
                        onGetPartyEntries = { partyId -> kharchaRepo.getUdhaarEntries(partyId) },
                        onDeleteParty = { partyId -> coroutineScope.launch { kharchaRepo.deleteUdhaarParty(partyId) } }
                    )
                }

                Screen.Pots -> {
                    BackHandler { currentScreen = Screen.History }
                    PotsScreen(
                        userId = userId,
                        pots = pots,
                        onAddPot = { pot -> coroutineScope.launch { kharchaRepo.addPot(pot) } },
                        onDepositToPot = { potId, amount -> coroutineScope.launch { kharchaRepo.depositToPot(potId, amount) } },
                        onDeletePot = { potId -> coroutineScope.launch { kharchaRepo.deletePot(potId) } }
                    )
                }

                Screen.Analysis -> {
                    BackHandler { currentScreen = Screen.History }
                    AnalysisScreen(
                        expenses = expenses,
                        budgets = budgets,
                        isBudgetFeatureEnabled = userProfile?.isBudgetFeatureEnabled == true,
                        pots = pots,
                        udhaarParties = udhaarParties
                    )
                }

                Screen.Settings -> {
                    BackHandler { currentScreen = Screen.History }
                    SettingsScreen(
                        userProfile = userProfile,
                        businesses = businesses,
                        categories = categories,
                        isPinLockEnabled = authRepo.isPinLockEnabled(),
                        pendingExcelCount = pendingExcelCount,
                        pendingSheetsCount = pendingSheetsCount,
                        onNavigateToBusinesses = { currentScreen = Screen.BusinessManagement },
                        onNavigateToCategories = { currentScreen = Screen.CategoryManagement },
                        onNavigateToBudgets = { currentScreen = Screen.BudgetSettings },
                        onNavigateToSheetsSync = { currentScreen = Screen.SheetsSync },
                        onNavigateToExcelSync = { currentScreen = Screen.ExcelSync },
                        onNavigateToAdmin = {
                            coroutineScope.launch {
                                adminUsers = com.example.data.remote.FirestoreSyncManager().getAllUsersForAdmin()
                                currentScreen = Screen.AdminPanel
                            }
                        },
                        onSetPin = { pin ->
                            authRepo.setPin(pin)
                            Toast.makeText(context, "PIN Set ho gaya!", Toast.LENGTH_SHORT).show()
                        },
                        onDisablePin = {
                            authRepo.disablePinLock()
                            Toast.makeText(context, "PIN Lock band ho gaya", Toast.LENGTH_SHORT).show()
                        },
                        onSignOut = {
                            authRepo.signOut()
                        },
                        onExportCsv = {
                            exportExpensesCsv(context, expenses)
                        }
                    )
                }

                Screen.BusinessManagement -> {
                    BackHandler { currentScreen = Screen.Settings }
                    BusinessManagementScreen(
                        userId = userId,
                        businesses = businesses,
                        onAddBusiness = { coroutineScope.launch { kharchaRepo.addBusiness(it) } },
                        onDeleteBusiness = { coroutineScope.launch { kharchaRepo.deleteBusiness(it) } },
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }

                Screen.CategoryManagement -> {
                    BackHandler { currentScreen = Screen.Settings }
                    CategoryManagementScreen(
                        userId = userId,
                        categories = categories,
                        onAddCategory = { coroutineScope.launch { kharchaRepo.addCategory(it) } },
                        onDeleteCategory = { coroutineScope.launch { kharchaRepo.deleteCategory(it) } },
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }

                Screen.BudgetSettings -> {
                    BackHandler { currentScreen = Screen.Settings }
                    BudgetSettingsScreen(
                        userId = userId,
                        isFeatureEnabled = userProfile?.isBudgetFeatureEnabled == true,
                        categories = categories,
                        budgets = budgets,
                        onToggleFeature = { enabled ->
                            coroutineScope.launch { authRepo.updateBudgetFeatureEnabled(enabled) }
                        },
                        onSaveBudget = { budget ->
                            coroutineScope.launch { kharchaRepo.saveBudget(budget) }
                        },
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }

                Screen.SheetsSync -> {
                    BackHandler { currentScreen = Screen.Settings }
                    SheetsSyncScreen(
                        userProfile = userProfile,
                        pendingCount = pendingSheetsCount,
                        onSaveSheetsUrl = { url, auto ->
                            coroutineScope.launch {
                                authRepo.updateSheetsUrl(url, auto)
                                Toast.makeText(context, "Sheets settings save ho gayi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSyncNow = {
                            isSyncing = true
                            coroutineScope.launch {
                                val res = kharchaRepo.syncGoogleSheets(userId, userProfile?.sheetsUrl ?: "")
                                isSyncing = false
                                res.onSuccess {
                                    Toast.makeText(context, "$it records Google Sheets me sync ho gaye!", Toast.LENGTH_SHORT).show()
                                }
                                res.onFailure {
                                    Toast.makeText(context, it.message ?: "Sheets sync error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        isSyncing = isSyncing,
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }

                Screen.ExcelSync -> {
                    BackHandler { currentScreen = Screen.Settings }
                    ExcelSyncScreen(
                        userProfile = userProfile,
                        pendingCount = pendingExcelCount,
                        onSavePreferredTime = { time ->
                            coroutineScope.launch { authRepo.updateExcelSyncPreference(time) }
                        },
                        onTriggerExcelSync = {
                            isSyncing = true
                            coroutineScope.launch {
                                val res = kharchaRepo.syncExcel(userId)
                                isSyncing = false
                                res.onSuccess {
                                    authRepo.recordExcelSyncCompleted()
                                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                                }
                                res.onFailure {
                                    Toast.makeText(context, it.message ?: "Excel sync fail hua", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        isSyncing = isSyncing,
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }

                Screen.AdminPanel -> {
                    BackHandler { currentScreen = Screen.Settings }
                    AdminPanelScreen(
                        adminUsers = adminUsers,
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
fun KharchaBottomBar(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 6.dp,
        modifier = Modifier
            .border(
                BorderStroke(0.8.dp, BorderSubtle),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val navItems = listOf(
            Triple(Screen.History, "Kharcha", Icons.Default.ReceiptLong),
            Triple(Screen.Udhaar, "Udhaar", Icons.Default.Handshake),
            Triple(Screen.QuickAdd, "+ Jodo", Icons.Default.AddCircle),
            Triple(Screen.Pots, "Gullak", Icons.Default.Savings),
            Triple(Screen.Analysis, "Hisab", Icons.Default.Analytics),
            Triple(Screen.Settings, "Settings", Icons.Default.Settings)
        )

        navItems.forEach { (screen, label, icon) ->
            val isSelected = currentScreen == screen
            val isQuickAdd = screen == Screen.QuickAdd

            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectScreen(screen) },
                icon = {
                    if (isQuickAdd) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = TextOnGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) GoldPrimary else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected || isQuickAdd) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected || isQuickAdd) GoldLight else TextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (isQuickAdd) Color.Transparent else SurfaceElevated
                )
            )
        }
    }
}

fun exportExpensesCsv(context: Context, expenses: List<Expense>) {
    try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csvFile = File(context.cacheDir, "KHARCHA_Export_${System.currentTimeMillis()}.csv")
        val writer = FileWriter(csvFile)
        writer.append("ID,Date,Amount,Category,PaymentMethod,Context,Business,Pot,UdhaarPerson,Note\n")
        expenses.forEach { exp ->
            writer.append("\"${exp.id}\",")
            writer.append("\"${dateFormat.format(Date(exp.dateMillis))}\",")
            writer.append("${exp.amount},")
            writer.append("\"${exp.category}\",")
            writer.append("\"${exp.paymentMethod}\",")
            writer.append("\"${exp.contextType}\",")
            writer.append("\"${exp.businessName ?: ""}\",")
            writer.append("\"${exp.potName ?: ""}\",")
            writer.append("\"${exp.udhaarPersonName ?: ""}\",")
            writer.append("\"${exp.note.replace("\"", "\"\"")}\"\n")
        }
        writer.flush()
        writer.close()

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Kharche Ka CSV Share / Save Karein"))
    } catch (e: Exception) {
        Toast.makeText(context, "CSV Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
