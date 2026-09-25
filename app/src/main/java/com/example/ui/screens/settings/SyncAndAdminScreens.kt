package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryBudget
import com.example.data.model.CategoryItem
import com.example.data.model.UserProfile
import com.example.ui.components.ExcelWarningDialog
import com.example.ui.components.TactileButton
import com.example.ui.components.TactileChip
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    userId: String,
    isFeatureEnabled: Boolean,
    categories: List<CategoryItem>,
    budgets: List<CategoryBudget>,
    onToggleFeature: (Boolean) -> Unit,
    onSaveBudget: (CategoryBudget) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Monthly Budgets Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Enable / Disable Switch Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
                color = SurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Budgets Chalu Karein?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isFeatureEnabled) "Chalu hai (Limit paar hone par alert aayega)" else "Band hai (Default)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isFeatureEnabled,
                        onCheckedChange = onToggleFeature,
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                    )
                }
            }

            if (isFeatureEnabled) {
                Text(
                    text = "Category ki Monthly Limit Set Karein (₹)",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val currentBudget = budgets.find { it.categoryName == cat.name }
                        var limitStr by remember(currentBudget) {
                            mutableStateOf(currentBudget?.monthlyLimit?.toInt()?.toString() ?: "")
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                            color = SurfaceCard,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = limitStr,
                                    onValueChange = {
                                        limitStr = it
                                        val num = it.toDoubleOrNull()
                                        if (num != null && num > 0) {
                                            onSaveBudget(
                                                CategoryBudget(
                                                    categoryName = cat.name,
                                                    userId = userId,
                                                    monthlyLimit = num,
                                                    isEnabled = true
                                                )
                                            )
                                        }
                                    },
                                    placeholder = { Text("0", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.width(110.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = GoldPrimary,
                                        unfocusedBorderColor = BorderSubtle
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetsSyncScreen(
    userProfile: UserProfile?,
    pendingCount: Int,
    onSaveSheetsUrl: (String, Boolean) -> Unit,
    onSyncNow: () -> Unit,
    isSyncing: Boolean,
    onNavigateBack: () -> Unit
) {
    var url by remember { mutableStateOf(userProfile?.sheetsUrl ?: "") }
    var autoSync by remember { mutableStateOf(userProfile?.sheetsAutoSync ?: true) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Google Sheets Mirror", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGlow, RoundedCornerShape(16.dp)),
                color = SurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "KHARCHA → Firestore → Google Sheets Mirror", fontWeight = FontWeight.Bold, color = GoldLight, fontSize = 13.sp)
                    Text(
                        text = "Google Sheets sirf ek backup/mirror destination hai. Primary database hamesha Firestore rehta hai.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Pending to mirror: $pendingCount transactions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingCount > 0) OrangeWarning else EmeraldCash
                    )
                }
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Google Apps Script / Webhook URL", color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = GoldPrimary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Har kharche par Auto-Mirror karein?", color = TextPrimary, fontSize = 13.sp)
                Switch(
                    checked = autoSync,
                    onCheckedChange = { autoSync = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                )
            }

            TactileButton(
                text = "Settings Save Karo",
                onClick = { onSaveSheetsUrl(url.trim(), autoSync) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            TactileButton(
                text = if (isSyncing) "Mirroring..." else "Sync Abhi Karo (Pending: $pendingCount)",
                onClick = onSyncNow,
                isLoading = isSyncing,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelSyncScreen(
    userProfile: UserProfile?,
    pendingCount: Int,
    onSavePreferredTime: (String) -> Unit,
    onTriggerExcelSync: () -> Unit,
    isSyncing: Boolean,
    onNavigateBack: () -> Unit
) {
    var selectedTime by remember { mutableStateOf(userProfile?.excelPreferredSyncTime ?: "02:00") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Excel Synchronization", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGlow, RoundedCornerShape(16.dp)),
                color = SurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Firestore MASTER → Controlled Excel Sync → Google Drive", fontWeight = FontWeight.Bold, color = GoldLight, fontSize = 13.sp)
                    Text(
                        text = "Excel synchronization har kharche par nahi hota. Yeh 24 ghante me 1 baar chalta hai ya jab aap manually 'UPDATE NOW' dabayein.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    val lastSync = userProfile?.lastExcelSyncMillis ?: 0L
                    Text(
                        text = if (lastSync > 0) "Aakhri Sync: ${dateFormat.format(Date(lastSync))}" else "Abhi tak sync nahi hua",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Text(
                        text = "Pending Records: $pendingCount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingCount > 0) OrangeWarning else EmeraldCash
                    )
                }
            }

            Text("Automatic 24-Hour Sync Ka Samay Chunein:", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("01:00", "02:00", "03:00", "04:00").forEach { time ->
                    TactileChip(
                        text = "$time AM",
                        selected = selectedTime == time,
                        onClick = {
                            selectedTime = time
                            onSavePreferredTime(time)
                        },
                        accentColor = GoldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TactileButton(
                text = "UPDATE NOW",
                onClick = { showConfirmDialog = true },
                isLoading = isSyncing,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showConfirmDialog) {
            ExcelWarningDialog(
                onConfirm = {
                    showConfirmDialog = false
                    onTriggerExcelSync()
                },
                onDismiss = { showConfirmDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    adminUsers: List<UserProfile>,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGlow, RoundedCornerShape(16.dp)),
                color = SurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "System Information", fontWeight = FontWeight.Bold, color = GoldLight)
                    Text(text = "App: KHARCHA (Native Android)", color = TextPrimary, fontSize = 13.sp)
                    Text(text = "Primary DB: Cloud Firestore (MASTER)", color = TextPrimary, fontSize = 13.sp)
                    Text(text = "Registered Users Count: ${adminUsers.size}", color = EmeraldCash, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(text = "Users List", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 14.sp)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(adminUsers, key = { it.uid }) { u ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                        color = SurfaceCard,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = u.displayName.ifBlank { "User" }, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = u.email, color = TextSecondary, fontSize = 12.sp)
                            Text(text = "Role: ${u.role}", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
