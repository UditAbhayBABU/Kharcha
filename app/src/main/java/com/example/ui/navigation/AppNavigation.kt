package com.example.ui.navigation

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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

    val userId = currentUser?.uid ?: ""

    // Seed default categories for this user once authenticated
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

    val authGoogleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data != null) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    authLoading = true
                    authError = null
                    coroutineScope.launch {
                        val res = authRepo.signInWithGoogleCredential(idToken)
                        authLoading = false
                        res.onFailure { authError = "Google login fail: ${it.localizedMessage}" }
                    }
                } else {
                    authError = "Google token nahi mila"
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                authLoading = false
                authError = when (e.statusCode) {
                    10 -> "Google Developer Error (10): Google Play Services SHA-1 config missing. Kripya Email/Password se login karein."
                    12500 -> "Google Sign-in (12500): Device par Google Play account login nahi hai. Kripya Email/Password use karein."
                    12501 -> "Google sign-in cancel kiya gaya."
                    else -> "Google login error (${e.statusCode}): ${e.localizedMessage}"
                }
            } catch (e: Exception) {
                authLoading = false
                authError = e.message
            }
        }
    }

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
            onGoogleSignIn = {
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                    .requestIdToken("853064744273-u0fj1ttrgva2mvhoa0896csogv1ja55t.apps.googleusercontent.com")
                    .requestEmail()
                    .requestProfile()
                    .build()
                val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                authGoogleSignInLauncher.launch(client.signInIntent)
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
        val getScreenRank: (Screen) -> Int = { screen ->
            when (screen) {
                Screen.History -> 0
                Screen.Udhaar -> 1
                Screen.QuickAdd -> 2
                Screen.Pots -> 3
                Screen.Analysis -> 4
                Screen.Settings -> 5
                else -> 6
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val fromRank = getScreenRank(initialState)
                    val toRank = getScreenRank(targetState)
                    val isForward = toRank >= fromRank

                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * (if (isForward) 0.16f else -0.16f)).toInt() },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                     scaleIn(
                         initialScale = 0.97f,
                         animationSpec = spring(
                             dampingRatio = Spring.DampingRatioLowBouncy,
                             stiffness = Spring.StiffnessMediumLow
                         )
                     )).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> (fullWidth * (if (isForward) -0.16f else 0.16f)).toInt() },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
                        scaleOut(targetScale = 0.99f, animationSpec = tween(160))
                    )
                },
                label = "screen_makkhan_transition"
            ) { targetScreen ->
                when (targetScreen) {
                Screen.QuickAdd -> {
                    BackHandler { currentScreen = Screen.History }
                    QuickAddScreen(
                        userId = userId,
                        categories = categories,
                        businesses = businesses,
                        pots = pots,
                        udhaarParties = udhaarParties,
                        budgets = budgets,
                        allExpenses = expenses,
                        isBudgetFeatureEnabled = userProfile?.isBudgetFeatureEnabled == true,
                        onSaveExpense = { expense ->
                            coroutineScope.launch {
                                kharchaRepo.addExpense(
                                    expense = expense,
                                    sheetsUrl = userProfile?.sheetsUrl ?: "",
                                    autoSyncSheets = userProfile?.sheetsAutoSync ?: true,
                                    googleAccessToken = userProfile?.googleAccessToken ?: "",
                                    sheetsSpreadsheetId = userProfile?.sheetsSpreadsheetId ?: "",
                                    sheetsWorksheetName = userProfile?.sheetsWorksheetName ?: "KHARCHA"
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
                        onNavigateToGoogleDriveAndSheets = { currentScreen = Screen.GoogleDriveAndSheets },
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

                Screen.GoogleDriveAndSheets -> {
                    BackHandler { currentScreen = Screen.Settings }
                    GoogleDriveAndSheetsScreen(
                        userProfile = userProfile,
                        pendingExcelCount = pendingExcelCount,
                        pendingSheetsCount = pendingSheetsCount,
                        onConnectGoogleAccount = { email, displayName, token ->
                            coroutineScope.launch {
                                authRepo.connectGoogleDriveAccount(email, displayName, token)
                            }
                        },
                        onDisconnectGoogleAccount = {
                            coroutineScope.launch {
                                authRepo.disconnectGoogleDriveAccount()
                            }
                        },
                        onSelectDriveFile = { fileId, fileName ->
                            coroutineScope.launch {
                                authRepo.updateSelectedDriveExcel(fileId, fileName)
                            }
                        },
                        onSelectSheetsSpreadsheet = { id, name ->
                            coroutineScope.launch {
                                authRepo.updateSelectedSheetsId(id, name)
                            }
                        },
                        onSaveSheetsWebhookUrl = { url, auto ->
                            coroutineScope.launch {
                                authRepo.updateSheetsUrl(url, auto)
                            }
                        },
                        onSaveExcelSyncTime = { time ->
                            coroutineScope.launch {
                                authRepo.updateExcelSyncPreference(time)
                            }
                        },
                        onFetchDriveFiles = { token ->
                            kharchaRepo.listGoogleDriveFiles(token)
                        },
                        onCreateDriveWorkbook = { token, name ->
                            kharchaRepo.createKharchaWorkbookInDrive(token, name)
                        },
                        onTriggerSheetsSync = {
                            isSyncing = true
                            coroutineScope.launch {
                                val token = userProfile?.googleAccessToken.orEmpty()
                                val sheetId = userProfile?.sheetsSpreadsheetId.orEmpty()
                                val res = if (token.isNotBlank() && sheetId.isNotBlank()) {
                                    kharchaRepo.syncGoogleSheetsDirect(userId, token, sheetId)
                                } else {
                                    kharchaRepo.syncGoogleSheets(userId, userProfile?.sheetsUrl.orEmpty())
                                }
                                isSyncing = false
                                res.onSuccess {
                                    Toast.makeText(context, "$it records Google Sheets me sync ho gaye!", Toast.LENGTH_SHORT).show()
                                }
                                res.onFailure {
                                    Toast.makeText(context, it.message ?: "Sheets sync error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onTriggerExcelSync = {
                            isSyncing = true
                            coroutineScope.launch {
                                val token = userProfile?.googleAccessToken.orEmpty()
                                val fileId = userProfile?.excelWorkbookId.orEmpty()
                                val res = kharchaRepo.syncExcelWithGoogleDrive(userId, token, fileId)
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
                        isSyncingSheets = isSyncing,
                        isSyncingExcel = isSyncing,
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
}

@Composable
fun KharchaBottomBar(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        color = Color(0xF505080F),
        tonalElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.2.dp, MetallicRimBrush),
                RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            )
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box {
            // Specular top rim shine for glossy glassmorphic depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                GlassRimTop,
                                Color.Transparent
                            )
                        )
                    )
            )

            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val navItems = listOf(
                    Triple(Screen.History, "Kharcha", Icons.Outlined.ReceiptLong),
                    Triple(Screen.Udhaar, "Udhaar", Icons.Outlined.SwapHoriz),
                    Triple(Screen.QuickAdd, "+ Jodo", Icons.Default.Add),
                    Triple(Screen.Pots, "Gullak", Icons.Outlined.Savings),
                    Triple(Screen.Analysis, "Hisab", Icons.Outlined.QueryStats),
                    Triple(Screen.Settings, "Settings", Icons.Outlined.Tune)
                )

                navItems.forEach { (screen, label, icon) ->
                    val isSelected = currentScreen == screen
                    val isQuickAdd = screen == Screen.QuickAdd

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.18f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "nav_icon_scale"
                    )

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(
                                if (isQuickAdd) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                            )
                            onSelectScreen(screen)
                        },
                        icon = {
                            if (isQuickAdd) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .scale(iconScale)
                                        .clip(CircleShape)
                                        .background(GoldMetallicRimBrush)
                                        .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = TextOnGold,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) GoldPrimary else TextSecondary,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .scale(iconScale)
                                )
                            }
                        },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected || isQuickAdd) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected || isQuickAdd) GoldLight else TextMuted
                                )
                                if (isSelected && !isQuickAdd) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(GoldPrimary)
                                    )
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = if (isQuickAdd) Color.Transparent else SurfaceElevated.copy(alpha = 0.5f)
                        )
                    )
                }
            }
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
