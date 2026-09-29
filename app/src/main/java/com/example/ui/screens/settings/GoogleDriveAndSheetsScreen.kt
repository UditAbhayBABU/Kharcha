package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriveFileItem
import com.example.data.model.UserProfile
import com.example.ui.components.TactileButton
import com.example.ui.theme.*
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveAndSheetsScreen(
    userProfile: UserProfile?,
    pendingExcelCount: Int,
    pendingSheetsCount: Int,
    onConnectGoogleAccount: (email: String, displayName: String, token: String) -> Unit,
    onDisconnectGoogleAccount: () -> Unit,
    onSelectDriveFile: (fileId: String, fileName: String) -> Unit,
    onSelectSheetsSpreadsheet: (id: String, name: String) -> Unit,
    onSaveSheetsWebhookUrl: (url: String, autoSync: Boolean) -> Unit,
    onSaveExcelSyncTime: (time: String) -> Unit,
    onFetchDriveFiles: suspend (token: String) -> Result<List<DriveFileItem>>,
    onCreateDriveWorkbook: suspend (token: String, name: String) -> Result<DriveFileItem>,
    onTriggerSheetsSync: (currentWebhookUrl: String) -> Unit,
    onTriggerExcelSync: () -> Unit,
    onShareExportFile: (() -> Unit)? = null,
    isSyncingSheets: Boolean,
    isSyncingExcel: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var isConnecting by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showPasteSheetIdDialog by remember { mutableStateOf(false) }
    var pastedSheetInput by remember { mutableStateOf("") }
    var showAppsScriptDialog by remember { mutableStateOf(false) }
    var showManualConnectDialog by remember { mutableStateOf(false) }

    var webhookUrl by remember { mutableStateOf(userProfile?.sheetsUrl ?: "") }
    var autoSyncSheets by remember { mutableStateOf(userProfile?.sheetsAutoSync ?: true) }

    val isGoogleConnected = !userProfile?.googleAccountEmail.isNullOrBlank()
    val isWebhookConfigured = webhookUrl.trim().contains("script.google.com/macros/s/")

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("853064744273-u0fj1ttrgva2mvhoa0896csogv1ja55t.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("https://www.googleapis.com/auth/spreadsheets")
            )
            .build()
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val email = account.email ?: ""
                val displayName = account.displayName ?: ""

                isConnecting = true
                coroutineScope.launch {
                    val token = withContext(Dispatchers.IO) {
                        try {
                            val scopeStr = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/spreadsheets"
                            GoogleAuthUtil.getToken(
                                context,
                                account.account ?: android.accounts.Account(email, "com.google"),
                                scopeStr
                            )
                        } catch (e: Exception) {
                            account.idToken ?: ""
                        }
                    }
                    isConnecting = false
                    onConnectGoogleAccount(email, displayName, token)
                    Toast.makeText(context, "$email connect ho gaya", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isConnecting = false
                showManualConnectDialog = true
            }
        } else {
            isConnecting = false
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Google Sheets & Backup",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 1. PRIMARY CARD: GOOGLE SHEETS BACKUP (CORE USER NEED)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isWebhookConfigured) EmeraldCash.copy(alpha = 0.4f) else BorderSubtle),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header with status indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldCash.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = EmeraldCash,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Google Sheets Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (pendingSheetsCount > 0) "$pendingSheetsCount kharche sync hone baki hain" else "Sabhi kharche sync hain",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Apple-style status pill
                            Surface(
                                color = if (isWebhookConfigured) EmeraldCash.copy(alpha = 0.15f) else OrangeWarning.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (isWebhookConfigured) EmeraldCash.copy(alpha = 0.5f) else OrangeWarning.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (isWebhookConfigured) "Connected" else "Setup Needed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isWebhookConfigured) EmeraldCash else OrangeWarning,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.8.dp)

                        // Auto-Sync Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Sync Expenses",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Har naye kharche par turant sheet update hogi",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = autoSyncSheets,
                                onCheckedChange = {
                                    autoSyncSheets = it
                                    onSaveSheetsWebhookUrl(webhookUrl.trim(), it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                            )
                        }

                        // Web App URL Input
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Apps Script Web App URL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                TextButton(
                                    onClick = { showAppsScriptDialog = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = GoldLight
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Setup Kaise Karein?",
                                        fontSize = 11.sp,
                                        color = GoldLight,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = webhookUrl,
                                onValueChange = {
                                    webhookUrl = it
                                    onSaveSheetsWebhookUrl(it.trim(), autoSyncSheets)
                                },
                                placeholder = {
                                    Text(
                                        "https://script.google.com/macros/s/.../exec",
                                        color = TextMuted.copy(alpha = 0.4f),
                                        fontSize = 12.sp
                                    )
                                },
                                trailingIcon = {
                                    if (webhookUrl.isNotBlank()) {
                                        IconButton(onClick = {
                                            webhookUrl = ""
                                            onSaveSheetsWebhookUrl("", autoSyncSheets)
                                        }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            val clip = clipboardManager.getText()?.text.orEmpty().trim()
                                            if (clip.isNotBlank()) {
                                                webhookUrl = clip
                                                onSaveSheetsWebhookUrl(clip, autoSyncSheets)
                                                Toast.makeText(context, "URL paste ho gaya", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(
                                                Icons.Default.ContentPaste,
                                                contentDescription = "Paste",
                                                tint = GoldLight,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = if (webhookUrl.contains("docs.google.com/spreadsheets")) RedExpense else GoldPrimary,
                                    unfocusedBorderColor = if (webhookUrl.contains("docs.google.com/spreadsheets")) RedExpense.copy(alpha = 0.6f) else BorderSubtle
                                )
                            )

                            // Clear, minimal validation text
                            val trimmedUrl = webhookUrl.trim()
                            if (trimmedUrl.contains("docs.google.com/spreadsheets")) {
                                Text(
                                    text = "⚠️ Yeh Sheet ka browser link hai. Kripya Apps Script se 'Web App URL' paste karein.",
                                    color = RedExpense,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (trimmedUrl.contains("script.google.com/macros/s/")) {
                                Text(
                                    text = "✓ Valid Web App URL Connected",
                                    color = EmeraldCash,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Primary Sync Button
                        TactileButton(
                            text = if (isSyncingSheets) "Sync Ho Raha Hai..." else if (pendingSheetsCount > 0) "Sync Google Sheets Abhi ($pendingSheetsCount Pending)" else "Sync Google Sheets",
                            icon = Icons.Default.Sync,
                            onClick = {
                                onSaveSheetsWebhookUrl(webhookUrl.trim(), autoSyncSheets)
                                onTriggerSheetsSync(webhookUrl.trim())
                            },
                            isLoading = isSyncingSheets,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Secondary Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeSheetId = userProfile?.sheetsSpreadsheetId.orEmpty()
                            OutlinedButton(
                                onClick = {
                                    val targetUrl = if (activeSheetId.isNotBlank() && !activeSheetId.startsWith("sheet_")) {
                                        "https://docs.google.com/spreadsheets/d/$activeSheetId/edit"
                                    } else {
                                        "https://sheets.new"
                                    }
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Browser open nahi ho paya", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderMedium),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldCash)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Sheet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { showPasteSheetIdDialog = true },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderMedium),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Link Sheet ID", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 2. EXPORT & SHARE CARD
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShareExportFile?.invoke() },
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Backup (.CSV)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Google Drive, Files ya WhatsApp par share karein", fontSize = 11.sp, color = TextSecondary)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                    }
                }
            }

            // 3. GOOGLE ACCOUNT CARD (QUIET & MINIMAL)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Google Account",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            if (isGoogleConnected) {
                                TextButton(
                                    onClick = { showDisconnectDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Disconnect", color = RedExpense, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (isGoogleConnected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDark)
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = userProfile?.googleAccountName?.ifBlank { "Google User" } ?: "Google User",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = userProfile?.googleAccountEmail.orEmpty(),
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isConnecting = true
                                        val client = GoogleSignIn.getClient(context, gso)
                                        googleSignInLauncher.launch(client.signInIntent)
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BorderMedium)
                                ) {
                                    Text(
                                        if (isConnecting) "Connecting..." else "Sign in with Google",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showManualConnectDialog = true },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BorderMedium)
                                ) {
                                    Text(
                                        "Enter Email",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DISCONNECT DIALOG
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            containerColor = SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Account Disconnect?", fontWeight = FontWeight.Bold, color = RedExpense) },
            text = {
                Text(
                    text = "Kya aap ${userProfile?.googleAccountEmail} ko disconnect karna chahte hain?",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectDialog = false
                    onDisconnectGoogleAccount()
                    val client = GoogleSignIn.getClient(context, gso)
                    client.signOut()
                    Toast.makeText(context, "Account disconnect ho gaya", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Disconnect", color = RedExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // MANUAL CONNECT EMAIL DIALOG
    if (showManualConnectDialog) {
        var manualEmail by remember { mutableStateOf(userProfile?.email ?: "") }

        AlertDialog(
            onDismissRequest = { showManualConnectDialog = false },
            containerColor = SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Google Account Email", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Apna Google Account email dalein jisse hisab sync rahe:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = manualEmail,
                        onValueChange = { manualEmail = it },
                        label = { Text("Email", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualEmail.isNotBlank()) {
                            onConnectGoogleAccount(
                                manualEmail.trim(),
                                manualEmail.substringBefore("@"),
                                ""
                            )
                            showManualConnectDialog = false
                            Toast.makeText(context, "$manualEmail connect ho gaya!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualConnectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // PASTE SHEET ID DIALOG
    if (showPasteSheetIdDialog) {
        AlertDialog(
            onDismissRequest = { showPasteSheetIdDialog = false },
            containerColor = SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Google Sheet Link / ID", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Apni existing Google Spreadsheet ka URL ya Sheet ID paste karein:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = pastedSheetInput,
                        onValueChange = { pastedSheetInput = it },
                        placeholder = { Text("docs.google.com/spreadsheets/d/.../edit", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val raw = pastedSheetInput.trim()
                        val extractedId = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)").find(raw)?.groupValues?.get(1) ?: raw
                        if (extractedId.isNotBlank()) {
                            showPasteSheetIdDialog = false
                            onSelectSheetsSpreadsheet(extractedId, "KHARCHA Google Sheet")
                            onSelectDriveFile(extractedId, "KHARCHA Google Sheet")
                            Toast.makeText(context, "Spreadsheet link ho gayi!", Toast.LENGTH_SHORT).show()
                            onTriggerSheetsSync(webhookUrl.trim())
                        }
                    },
                    enabled = pastedSheetInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Link Sheet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteSheetIdDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // APPS SCRIPT SETUP GUIDE DIALOG (CLEAN, 4 STEPS, 1-TAP COPY)
    if (showAppsScriptDialog) {
        val scriptCode = """
function doGet(e) {
  return ContentService.createTextOutput(JSON.stringify({
    status: "KHARCHA Webhook is ACTIVE and READY!",
    timestamp: new Date().toISOString()
  })).setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheetName = data.sheetName || "KHARCHA";
    var sheet = ss.getSheetByName(sheetName);
    if (!sheet) {
      sheet = ss.insertSheet(sheetName);
      sheet.appendRow(["Transaction ID", "Date", "Time", "Amount (INR)", "Category", "Context", "Business", "Payment Method", "Udhaar Person", "Pot", "Note", "Created At"]);
      sheet.getRange(1, 1, 1, 12).setFontWeight("bold").setBackground("#F3F4F6");
    }
    var expenses = data.expenses || [];
    for (var i = 0; i < expenses.length; i++) {
      var x = expenses[i];
      sheet.appendRow([x.transactionId, x.date, x.time, x.amount, x.category, x.context, x.business, x.paymentMethod, x.udhaarPerson, x.pot, x.note, x.createdAt]);
    }
    return ContentService.createTextOutput(JSON.stringify({status: "success", count: expenses.length}))
      .setMimeType(ContentService.MimeType.JSON);
  } catch(err) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: err.toString()}))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showAppsScriptDialog = false },
            containerColor = SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = EmeraldCash, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Sheets Setup Guide", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("1. Apni Google Sheet kholein ➔ Extensions ➔ Apps Script.", color = TextPrimary, fontSize = 12.sp)
                            Text("2. Neeche diya code copy karke wahan paste karein aur Save karein.", color = TextPrimary, fontSize = 12.sp)
                            Text("3. Deploy ➔ New deployment ➔ Select type 'Web app'.", color = TextPrimary, fontSize = 12.sp)
                            Text("4. 'Who has access' me 'Anyone' chunein aur Deploy karein.", color = GoldLight, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("5. Jo Web app URL ('script.google.com/macros/s/.../exec') aayega, use yahan paste karein.", color = EmeraldCash, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
                    ) {
                        Text(
                            text = scriptCode,
                            color = GoldLight,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(scriptCode))
                        Toast.makeText(context, "Code clipboard me copy ho gaya!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCash, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Code", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppsScriptDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}
