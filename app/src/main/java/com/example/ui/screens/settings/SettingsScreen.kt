package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import com.example.data.model.Business
import com.example.data.model.CategoryItem
import com.example.data.model.UserProfile
import com.example.ui.components.ExcelWarningDialog
import com.example.ui.components.TactileButton
import com.example.ui.components.TactileChip
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    businesses: List<Business>,
    categories: List<CategoryItem>,
    isPinLockEnabled: Boolean,
    pendingExcelCount: Int,
    pendingSheetsCount: Int,
    onNavigateToBusinesses: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToSheetsSync: () -> Unit,
    onNavigateToExcelSync: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onSignOut: () -> Unit,
    onExportCsv: () -> Unit
) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Khata",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 19.sp
                    )
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // USER PROFILE CARD
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGlow, RoundedCornerShape(18.dp)),
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.2f))
                                .border(1.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userProfile?.displayName?.ifBlank { "Kharcha User" } ?: "Kharcha User",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = userProfile?.email?.ifBlank { "Logged in" } ?: "Logged in",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            if (userProfile?.role == "admin") {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "★ Admin Access Enabled",
                                    fontSize = 10.sp,
                                    color = AmberVibrant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // CORE BUSINESS & CATEGORY SECTION
            item {
                Text(
                    text = "Khata & Categories",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                ) {
                    SettingsItem(
                        icon = Icons.Default.Business,
                        iconTint = BlueBank,
                        title = "Business Khata",
                        subtitle = "${businesses.size} configured businesses",
                        onClick = onNavigateToBusinesses
                    )
                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
                    SettingsItem(
                        icon = Icons.Default.Category,
                        iconTint = GoldPrimary,
                        title = "Categories Manage Karo",
                        subtitle = "${categories.size} categories available",
                        onClick = onNavigateToCategories
                    )
                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
                    SettingsItem(
                        icon = Icons.Default.PieChart,
                        iconTint = PurpleCard,
                        title = "Monthly Budgets",
                        subtitle = if (userProfile?.isBudgetFeatureEnabled == true) "Chalu hai" else "Band hai (Tap to enable)",
                        onClick = onNavigateToBudgets
                    )
                }
            }

            // MIRRORS & CLOUD EXCEL / SHEETS SECTION
            item {
                Text(
                    text = "Cloud Sync & Mirrors",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                ) {
                    SettingsItem(
                        icon = Icons.Default.TableChart,
                        iconTint = EmeraldCash,
                        title = "Google Sheets Mirror",
                        subtitle = if (pendingSheetsCount > 0) "$pendingSheetsCount pending records" else "Sabhi synced",
                        onClick = onNavigateToSheetsSync
                    )
                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
                    SettingsItem(
                        icon = Icons.Default.Description,
                        iconTint = EmeraldCash,
                        title = "Excel Synchronization",
                        subtitle = if (pendingExcelCount > 0) "$pendingExcelCount unsynced records" else "Up to date",
                        onClick = onNavigateToExcelSync
                    )
                }
            }

            // APP SECURITY / PIN LOCK
            item {
                Text(
                    text = "Suraksha & Privacy",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                ) {
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        iconTint = if (isPinLockEnabled) GoldPrimary else TextMuted,
                        title = "App Lock (4-Digit PIN)",
                        subtitle = if (isPinLockEnabled) "PIN lock chalu hai (Quick Add ko PIN nahi chahiye)" else "Band hai",
                        onClick = { showPinDialog = true }
                    )
                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
                    SettingsItem(
                        icon = Icons.Default.FileDownload,
                        iconTint = GoldLight,
                        title = "Backup & Export (CSV)",
                        subtitle = "Kharche ka pura data file me save karo",
                        onClick = onExportCsv
                    )
                }
            }

            // ADMIN PANEL (IF APPLICABLE)
            if (userProfile?.role == "admin") {
                item {
                    Text(
                        text = "Admin Controls",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    ) {
                        SettingsItem(
                            icon = Icons.Default.AdminPanelSettings,
                            iconTint = AmberVibrant,
                            title = "Admin Dashboard",
                            subtitle = "Users, metrics and system stats",
                            onClick = onNavigateToAdmin
                        )
                    }
                }
            }

            // LOGOUT BUTTON
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = RedExpense),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout / Account Se Niklein", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showPinDialog) {
            PinSetupDialog(
                isCurrentlyEnabled = isPinLockEnabled,
                onDismiss = { showPinDialog = false },
                onSavePin = { pin ->
                    onSetPin(pin)
                    showPinDialog = false
                },
                onDisable = {
                    onDisablePin()
                    showPinDialog = false
                }
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun PinSetupDialog(
    isCurrentlyEnabled: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit,
    onDisable: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = if (isCurrentlyEnabled) "PIN Lock Badlein ya Hatayein" else "4-Digit PIN Lock Lagayein",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Dhyan rahe: Quick Add bina PIN ke turant khulega taaki jaldi kharcha jud sake.",
                    fontSize = 12.sp,
                    color = GoldLight
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("4-Digit PIN Daalo", color = TextMuted) },
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
                onClick = { if (pin.length == 4) onSavePin(pin) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                enabled = pin.length == 4
            ) {
                Text("Set PIN", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (isCurrentlyEnabled) {
                TextButton(onClick = onDisable) {
                    Text("PIN Hatayein", color = RedExpense)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Ruko", color = TextSecondary) }
            }
        }
    )
}
