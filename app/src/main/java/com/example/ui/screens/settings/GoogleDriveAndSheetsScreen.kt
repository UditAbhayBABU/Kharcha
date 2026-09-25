package com.example.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriveFileItem
import com.example.data.model.UserProfile
import com.example.ui.components.ExcelWarningDialog
import com.example.ui.components.TactileButton
import com.example.ui.components.TactileChip
import com.example.ui.theme.*
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onTriggerSheetsSync: () -> Unit,
    onTriggerExcelSync: () -> Unit,
    isSyncingSheets: Boolean,
    isSyncingExcel: Boolean,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    var isConnecting by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showExcelConfirmDialog by remember { mutableStateOf(false) }
    var showDriveFilePicker by remember { mutableStateOf(false) }
    var isLoadingFiles by remember { mutableStateOf(false) }
    var driveFilesList by remember { mutableStateOf<List<DriveFileItem>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newWorkbookName by remember { mutableStateOf("KHARCHA_Master_Workbook") }

    // Manual webhook and schedule settings
    var webhookUrl by remember { mutableStateOf(userProfile?.sheetsUrl ?: "") }
    var autoSyncSheets by remember { mutableStateOf(userProfile?.sheetsAutoSync ?: true) }
    var selectedExcelTime by remember { mutableStateOf(userProfile?.excelPreferredSyncTime ?: "02:00") }

    val isGoogleConnected = !userProfile?.googleAccountEmail.isNullOrBlank()

    // Google Sign-In options with Drive and Sheets scopes
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
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
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(Exception::class.java)
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
                    Toast.makeText(context, "$email safaltapoorvak connect ho gaya!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isConnecting = false
                Toast.makeText(context, "Google connection fail: ${e.message}", Toast.LENGTH_LONG).show()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isGoogleConnected) EmeraldCash else OrangeWarning)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Drive & Sheets",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 19.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Peeche",
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
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // 1. GOOGLE ACCOUNT CONNECTION CARD
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.2.dp, if (isGoogleConnected) BorderGlow else BorderSubtle, RoundedCornerShape(18.dp)),
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Google Account Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GoldLight
                            )
                            Surface(
                                color = if (isGoogleConnected) EmeraldCash.copy(alpha = 0.15f) else OrangeWarning.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isGoogleConnected) EmeraldCash else OrangeWarning
                                )
                            ) {
                                Text(
                                    text = if (isGoogleConnected) "CONNECTED" else "NOT CONNECTED",
                                    color = if (isGoogleConnected) EmeraldCash else OrangeWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
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
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userProfile?.googleAccountName?.ifBlank { "Google User" } ?: "Google User",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = userProfile?.googleAccountEmail ?: "",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isConnecting = true
                                        val client = GoogleSignIn.getClient(context, gso)
                                        client.signOut().addOnCompleteListener {
                                            googleSignInLauncher.launch(client.signInIntent)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change Account", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showDisconnectDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedExpense),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RedExpense.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Disconnect", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = "KHARCHA aapke Google Drive aur Google Sheets ke sath safely sync ho sakta hai. Aapka password kabhi store nahi kiya jata.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            TactileButton(
                                text = if (isConnecting) "Connecting..." else "Connect Google Account",
                                icon = Icons.Default.CloudQueue,
                                onClick = {
                                    isConnecting = true
                                    val client = GoogleSignIn.getClient(context, gso)
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                isLoading = isConnecting,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 2. GOOGLE DRIVE EXCEL WORKBOOK SELECTION
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = EmeraldCash, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google Drive Excel Workbook",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }

                        val selectedWorkbookName = userProfile?.excelWorkbookName.orEmpty()
                        val selectedWorkbookId = userProfile?.excelWorkbookId.orEmpty()

                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (selectedWorkbookName.isNotBlank()) selectedWorkbookName else "Koi file select nahi hai",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (selectedWorkbookName.isNotBlank()) TextPrimary else TextMuted
                                )
                                if (selectedWorkbookId.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Drive ID: $selectedWorkbookId",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (!isGoogleConnected) {
                                        Toast.makeText(context, "Pehle Google Account connect karein", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    showDriveFilePicker = true
                                    isLoadingFiles = true
                                    coroutineScope.launch {
                                        val res = onFetchDriveFiles(userProfile?.googleAccessToken ?: "")
                                        isLoadingFiles = false
                                        res.onSuccess { driveFilesList = it }
                                        res.onFailure {
                                            Toast.makeText(context, "Drive files read error: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select File", fontSize = 12.sp, color = TextPrimary)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isGoogleConnected) {
                                        Toast.makeText(context, "Pehle Google Account connect karein", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    showCreateDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldCash)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Naya Workbook", fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // 3. GOOGLE SHEETS MIRROR
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = EmeraldCash, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Google Sheets Mirror",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "Pending: $pendingSheetsCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingSheetsCount > 0) OrangeWarning else EmeraldCash
                            )
                        }

                        Text(
                            text = "Har kharche ka near-real-time backup mirror Google Sheets me chalta hai.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        // Webhook URL / Apps Script
                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { webhookUrl = it },
                            label = { Text("Webhook / Apps Script URL (Optional)", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Har kharche par Auto-Mirror karein?", color = TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = autoSyncSheets,
                                onCheckedChange = {
                                    autoSyncSheets = it
                                    onSaveSheetsWebhookUrl(webhookUrl.trim(), it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                            )
                        }

                        TactileButton(
                            text = if (isSyncingSheets) "Mirroring..." else "Sync Google Sheets Abhi",
                            icon = Icons.Default.Sync,
                            onClick = {
                                onSaveSheetsWebhookUrl(webhookUrl.trim(), autoSyncSheets)
                                onTriggerSheetsSync()
                            },
                            isLoading = isSyncingSheets,
                            isPrimary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. CONTROLLED EXCEL SYNCHRONIZATION
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGlow, RoundedCornerShape(18.dp)),
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Controlled Excel Sync",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "Pending: $pendingExcelCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingExcelCount > 0) OrangeWarning else EmeraldCash
                            )
                        }

                        Text(
                            text = "Excel synchronization 24 ghante me 1 baar safe time par hota hai, ya jab aap 'UPDATE NOW' dabayein.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        val lastSync = userProfile?.lastExcelSyncMillis ?: 0L
                        Text(
                            text = if (lastSync > 0) "Aakhri Sync: ${dateFormat.format(Date(lastSync))}" else "Abhi tak sync nahi hua",
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        Text("Automatic 24-Hour Safe Sync Ka Samay:", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("01:00", "02:00", "03:00", "04:00").forEach { time ->
                                TactileChip(
                                    text = "$time AM",
                                    selected = selectedExcelTime == time,
                                    onClick = {
                                        selectedExcelTime = time
                                        onSaveExcelSyncTime(time)
                                    },
                                    accentColor = GoldPrimary
                                )
                            }
                        }

                        TactileButton(
                            text = if (isSyncingExcel) "Updating Excel..." else "UPDATE NOW",
                            icon = Icons.Default.UploadFile,
                            onClick = { showExcelConfirmDialog = true },
                            isLoading = isSyncingExcel,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // DISCONNECT CONFIRMATION DIALOG
        if (showDisconnectDialog) {
            AlertDialog(
                onDismissRequest = { showDisconnectDialog = false },
                containerColor = SurfaceElevated,
                title = { Text("Google Account Disconnect?", fontWeight = FontWeight.Bold, color = RedExpense) },
                text = {
                    Text(
                        text = "Kya aap ${userProfile?.googleAccountEmail} ko KHARCHA se disconnect karna chahte hain? Aapke local aur Firestore kharche surakshit rahenge.",
                        color = TextPrimary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDisconnectDialog = false
                        onDisconnectGoogleAccount()
                        val client = GoogleSignIn.getClient(context, gso)
                        client.signOut()
                        Toast.makeText(context, "Google Account disconnect kar diya gaya", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Disconnect", color = RedExpense, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisconnectDialog = false }) {
                        Text("Ruko", color = TextSecondary)
                    }
                }
            )
        }

        // EXCEL SAFETY WARNING DIALOG
        if (showExcelConfirmDialog) {
            ExcelWarningDialog(
                onConfirm = {
                    showExcelConfirmDialog = false
                    onTriggerExcelSync()
                },
                onDismiss = { showExcelConfirmDialog = false }
            )
        }

        // CREATE NEW WORKBOOK DIALOG
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = SurfaceElevated,
                title = { Text("Naya Excel / Sheets Workbook", fontWeight = FontWeight.Bold, color = GoldLight) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Aapke Google Drive me naya KHARCHA spreadsheet banega jisme automatic columns setup honge.", color = TextSecondary, fontSize = 13.sp)
                        OutlinedTextField(
                            value = newWorkbookName,
                            onValueChange = { newWorkbookName = it },
                            label = { Text("File Ka Naam", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = newWorkbookName.trim().ifBlank { "KHARCHA_Master_Workbook" }
                        showCreateDialog = false
                        coroutineScope.launch {
                            val res = onCreateDriveWorkbook(userProfile?.googleAccessToken ?: "", name)
                            res.onSuccess {
                                onSelectDriveFile(it.id, it.name)
                                onSelectSheetsSpreadsheet(it.id, it.name)
                                Toast.makeText(context, "${it.name} Google Drive me ban gaya!", Toast.LENGTH_LONG).show()
                            }
                            res.onFailure {
                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Text("Banao", color = GoldPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // DRIVE FILE PICKER DIALOG
        if (showDriveFilePicker) {
            AlertDialog(
                onDismissRequest = { showDriveFilePicker = false },
                containerColor = SurfaceElevated,
                title = { Text("Google Drive Se File Chunein", fontWeight = FontWeight.Bold, color = GoldLight) },
                text = {
                    Box(modifier = Modifier.heightIn(min = 200.dp, max = 350.dp).fillMaxWidth()) {
                        if (isLoadingFiles) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = GoldPrimary)
                        } else if (driveFilesList.isEmpty()) {
                            Text("Google Drive me koi Excel ya Google Sheet file nahi mili.", modifier = Modifier.align(Alignment.Center), color = TextMuted)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(driveFilesList, key = { it.id }) { file ->
                                    Surface(
                                        color = SurfaceDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectDriveFile(file.id, file.name)
                                                onSelectSheetsSpreadsheet(file.id, file.name)
                                                showDriveFilePicker = false
                                                Toast.makeText(context, "${file.name} chuna gaya!", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (file.mimeType.contains("sheet") || file.name.endsWith(".xlsx")) Icons.Default.TableChart else Icons.Default.Description,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(file.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                                Text(file.mimeType.substringAfterLast("."), color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDriveFilePicker = false }) {
                        Text("Band Karein", color = TextSecondary)
                    }
                }
            )
        }
    }
}
