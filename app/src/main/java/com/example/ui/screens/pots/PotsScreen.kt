package com.example.ui.screens.pots

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
import com.example.data.model.Pot
import com.example.ui.components.KharchaCard
import com.example.ui.components.TactileButton
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotsScreen(
    userId: String,
    pots: List<Pot>,
    onAddPot: (Pot) -> Unit,
    onDepositToPot: (potId: String, amount: Double) -> Unit,
    onDeletePot: (String) -> Unit
) {
    var showCreatePotDialog by remember { mutableStateOf(false) }
    var selectedPotForDeposit by remember { mutableStateOf<Pot?>(null) }

    val totalPotBalance = pots.sumOf { it.currentBalance }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gullak / Pots",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 19.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showCreatePotDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Naya Gullak",
                            tint = GoldPrimary
                        )
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
                .padding(horizontal = 16.dp)
        ) {
            // TOTAL POTS BALANCE CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGlow, RoundedCornerShape(18.dp)),
                color = SurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Gullak me Jama Kul Rashi",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹${totalPotBalance.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPrimary
                    )
                    Text(
                        text = "Pots virtual allocation hain (Emergency fund, Safar, Naya phone aadi)",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aapke Gullak (${pots.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                TextButton(onClick = { showCreatePotDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Naya Pot Banao", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (pots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Koi Gullak / Pot nahi banaya abhi tak.",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "'Naya Pot Banao' par tap karke target set karein.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(pots, key = { it.id }) { pot ->
                        PotCard(
                            pot = pot,
                            onDeposit = { selectedPotForDeposit = pot },
                            onDelete = { onDeletePot(pot.id) }
                        )
                    }
                }
            }
        }

        // CREATE POT DIALOG
        if (showCreatePotDialog) {
            CreatePotDialog(
                userId = userId,
                onDismiss = { showCreatePotDialog = false },
                onConfirm = { pot ->
                    onAddPot(pot)
                    showCreatePotDialog = false
                }
            )
        }

        // DEPOSIT TO POT DIALOG
        selectedPotForDeposit?.let { pot ->
            DepositPotDialog(
                pot = pot,
                onDismiss = { selectedPotForDeposit = null },
                onConfirm = { amount ->
                    onDepositToPot(pot.id, amount)
                    selectedPotForDeposit = null
                }
            )
        }
    }
}

@Composable
fun PotCard(
    pot: Pot,
    onDeposit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val progress = if (pot.targetAmount > 0) {
        (pot.currentBalance / pot.targetAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                            .border(1.dp, BorderMedium, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = pot.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        if (pot.targetAmount > 0) {
                            Text(
                                text = "Target: ₹${pot.targetAmount.toInt()}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${pot.currentBalance.toInt()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldCash
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (pot.targetAmount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GoldPrimary,
                    trackColor = SurfaceDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}% Target Pura",
                        fontSize = 10.sp,
                        color = GoldLight
                    )
                    Text(
                        text = "Kharch Hua: ₹${pot.totalSpent.toInt()}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onDeposit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = GoldPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Is Gullak Me Paise Daalo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SurfaceElevated,
            title = { Text("${pot.name} Gullak Hatayein?", color = TextPrimary) },
            text = { Text("Is pot ka record delete ho jayega.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete Karo", color = RedExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Ruko", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun CreatePotDialog(
    userId: String,
    onDismiss: () -> Unit,
    onConfirm: (Pot) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var initialDepositStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Naya Gullak / Pot Banao", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Gullak Ka Naam (e.g. Emergency, Trip)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary
                    )
                )

                OutlinedTextField(
                    value = targetAmountStr,
                    onValueChange = { targetAmountStr = it },
                    label = { Text("Target Amount (₹) (Optional)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary
                    )
                )

                OutlinedTextField(
                    value = initialDepositStr,
                    onValueChange = { initialDepositStr = it },
                    label = { Text("Pehle Se Kitna Jama Hai? (₹) (Optional)", color = TextMuted) },
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
                    if (name.isNotBlank()) {
                        val target = targetAmountStr.toDoubleOrNull() ?: 0.0
                        val initial = initialDepositStr.toDoubleOrNull() ?: 0.0
                        val pot = Pot(
                            id = "pot_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                            userId = userId,
                            name = name.trim(),
                            targetAmount = target,
                            currentBalance = initial,
                            totalDeposited = initial
                        )
                        onConfirm(pot)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                enabled = name.isNotBlank()
            ) {
                Text("Banao", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Radd Karo", color = TextSecondary) }
        }
    )
}

@Composable
fun DepositPotDialog(
    pot: Pot,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("${pot.name} me Jama Karo", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Abhi Balance: ₹${pot.currentBalance.toInt()}",
                    fontSize = 13.sp,
                    color = GoldLight
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Kitne Rupee Jama Kar Rahe Ho?", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldCash
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldCash, contentColor = TextPrimary),
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Jama Karo", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ruko", color = TextSecondary) }
        }
    )
}
