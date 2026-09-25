package com.example.ui.screens.udhaar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.UdhaarEntry
import com.example.data.model.UdhaarParty
import com.example.ui.components.KharchaCard
import com.example.ui.components.TactileButton
import com.example.ui.components.TactileChip
import com.example.ui.theme.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarScreen(
    userId: String,
    parties: List<UdhaarParty>,
    onAddParty: (UdhaarParty) -> Unit,
    onRecordTransaction: (partyId: String, amount: Double, isRepayment: Boolean, note: String) -> Unit,
    onGetPartyEntries: (partyId: String) -> Flow<List<UdhaarEntry>>,
    onDeleteParty: (String) -> Unit
) {
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var selectedPartyForLedger by remember { mutableStateOf<UdhaarParty?>(null) }
    var showTransactionDialogForParty by remember { mutableStateOf<UdhaarParty?>(null) }

    val totalGiven = parties.sumOf { it.totalGiven }
    val totalReceived = parties.sumOf { it.totalReceived }
    val totalOutstanding = totalGiven - totalReceived

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Udhaar Khata",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 19.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showAddPartyDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Naya Party Jodo",
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
            // OUTSTANDING SUMMARY CARD
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Bazar me Kul Udhaar Baki",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹${totalOutstanding.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalOutstanding > 0) OrangeWarning else EmeraldCash
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Kul Diya (Given)", fontSize = 11.sp, color = TextMuted)
                            Text(text = "₹${totalGiven.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Wapas Mila (Received)", fontSize = 11.sp, color = TextMuted)
                            Text(text = "₹${totalReceived.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldCash)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Parties / Log (${parties.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                TextButton(onClick = { showAddPartyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Naya Naam Jodo", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (parties.isEmpty()) {
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
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Koi Udhaar Khata nahi hai.",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(parties, key = { it.id }) { party ->
                        UdhaarPartyCard(
                            party = party,
                            onOpenLedger = { selectedPartyForLedger = party },
                            onOpenTransactionDialog = { showTransactionDialogForParty = party }
                        )
                    }
                }
            }
        }

        // ADD PARTY DIALOG
        if (showAddPartyDialog) {
            AddUdhaarPartyDialog(
                onDismiss = { showAddPartyDialog = false },
                onAdd = { newParty ->
                    onAddParty(newParty)
                    showAddPartyDialog = false
                },
                userId = userId
            )
        }

        // TRANSACTION DIALOG (GAVE / RECEIVED)
        showTransactionDialogForParty?.let { party ->
            RecordUdhaarEntryDialog(
                party = party,
                onDismiss = { showTransactionDialogForParty = null },
                onConfirm = { amount, isRepayment, note ->
                    onRecordTransaction(party.id, amount, isRepayment, note)
                    showTransactionDialogForParty = null
                }
            )
        }

        // FULL PARTY LEDGER DIALOG / SHEET
        selectedPartyForLedger?.let { party ->
            val entriesFlow = remember(party.id) { onGetPartyEntries(party.id) }
            val entries by entriesFlow.collectAsState(initial = emptyList())

            UdhaarLedgerSheet(
                party = party,
                entries = entries,
                onDismiss = { selectedPartyForLedger = null },
                onRecordTransaction = { amount, isRepayment, note ->
                    onRecordTransaction(party.id, amount, isRepayment, note)
                },
                onDeleteParty = {
                    onDeleteParty(party.id)
                    selectedPartyForLedger = null
                }
            )
        }
    }
}

@Composable
fun UdhaarPartyCard(
    party: UdhaarParty,
    onOpenLedger: () -> Unit,
    onOpenTransactionDialog: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val balance = party.outstandingBalance

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenLedger),
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
                        Text(
                            text = party.name.take(1).uppercase(),
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = party.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        if (party.phone.isNotBlank()) {
                            Text(text = party.phone, fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${balance.toInt()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance > 0) OrangeWarning else EmeraldCash
                    )
                    Text(
                        text = if (balance > 0) "Lena Hai" else if (balance < 0) "Dena Hai" else "Barabar",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balance > 0) OrangeWarning else EmeraldCash
                    )
                }
            }

            // Reminders / Expected Return Date badges
            if (party.expectedReturnDateMillis != null && party.expectedReturnDateMillis > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Wapsi Date: ${dateFormat.format(Date(party.expectedReturnDateMillis))}",
                        fontSize = 11.sp,
                        color = GoldLight
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (party.reminderEnabled) {
                        Text(
                            text = "Reminder har ${party.reminderIntervalDays} din",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenTransactionDialog,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ / - Entry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenLedger,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary.copy(alpha = 0.2f), contentColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Khata Dekho", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddUdhaarPartyDialog(
    onDismiss: () -> Unit,
    onAdd: (UdhaarParty) -> Unit,
    userId: String
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var reminderIntervalDays by remember { mutableStateOf(7) }
    var reminderEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Naya Udhaar Khata Jodo", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Party / Vyakti Ka Naam", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (Optional)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = GoldPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reminder Chalu Rakhein?", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                    )
                }

                if (reminderEnabled) {
                    Text("Reminder Kitne Din Me:", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 7, 15, 30).forEach { days ->
                            TactileChip(
                                text = "$days Din",
                                selected = reminderIntervalDays == days,
                                onClick = { reminderIntervalDays = days },
                                accentColor = GoldPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val party = UdhaarParty(
                            id = "party_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                            userId = userId,
                            name = name.trim(),
                            phone = phone.trim(),
                            note = note.trim(),
                            reminderEnabled = reminderEnabled,
                            reminderIntervalDays = reminderIntervalDays
                        )
                        onAdd(party)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                enabled = name.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Radd Karo", color = TextSecondary) }
        }
    )
}

@Composable
fun RecordUdhaarEntryDialog(
    party: UdhaarParty,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, isRepayment: Boolean, note: String) -> Unit
) {
    var amountString by remember { mutableStateOf("") }
    var isRepayment by remember { mutableStateOf(false) } // false = Udhaar Diya, true = Wapas Mila
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = "${party.name} - Entry Jodo",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toggle Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TactileChip(
                        text = "Maine Diya (Udhaar)",
                        selected = !isRepayment,
                        onClick = { isRepayment = false },
                        accentColor = OrangeWarning,
                        modifier = Modifier.weight(1f)
                    )
                    TactileChip(
                        text = "Wapas Mila (Vasooli)",
                        selected = isRepayment,
                        onClick = { isRepayment = true },
                        accentColor = EmeraldCash,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("Amount (₹)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = if (isRepayment) EmeraldCash else OrangeWarning
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Vivaran / Note (Optional)", color = TextMuted) },
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
                    val amt = amountString.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt, isRepayment, note.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRepayment) EmeraldCash else OrangeWarning,
                    contentColor = TextPrimary
                ),
                enabled = (amountString.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ruko", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarLedgerSheet(
    party: UdhaarParty,
    entries: List<UdhaarEntry>,
    onDismiss: () -> Unit,
    onRecordTransaction: (amount: Double, isRepayment: Boolean, note: String) -> Unit,
    onDeleteParty: () -> Unit
) {
    var showEntryDialog by remember { mutableStateOf(false) }
    var showDeletePartyConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderMedium) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxHeight(0.85f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = party.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Baki Balance: ₹${party.outstandingBalance.toInt()}",
                        fontSize = 13.sp,
                        color = if (party.outstandingBalance > 0) OrangeWarning else EmeraldCash,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { showDeletePartyConfirm = true }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = RedExpense)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TactileButton(
                text = "+ / - Entry Jodo",
                onClick = { showEntryDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Pura Len-Den History (${entries.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Koi len-den record nahi hai.", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { item ->
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
                                Column {
                                    Text(
                                        text = if (item.isRepayment) "Wapas Mila (Received)" else "Maine Diya (Given)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isRepayment) EmeraldCash else OrangeWarning
                                    )
                                    Text(
                                        text = dateFormat.format(Date(item.dateMillis)),
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    if (item.note.isNotBlank()) {
                                        Text(text = item.note, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                Text(
                                    text = "${if (item.isRepayment) "-" else "+"}₹${item.amount.toInt()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.isRepayment) EmeraldCash else OrangeWarning
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEntryDialog) {
        RecordUdhaarEntryDialog(
            party = party,
            onDismiss = { showEntryDialog = false },
            onConfirm = { amount, isRepayment, note ->
                onRecordTransaction(amount, isRepayment, note)
                showEntryDialog = false
            }
        )
    }

    if (showDeletePartyConfirm) {
        AlertDialog(
            onDismissRequest = { showDeletePartyConfirm = false },
            containerColor = SurfaceElevated,
            title = { Text("${party.name} ka Khata Hatana Hai?", color = TextPrimary) },
            text = { Text("Is party ka pura ledger delete ho jayega.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeletePartyConfirm = false
                    onDeleteParty()
                }) {
                    Text("Delete Karo", color = RedExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePartyConfirm = false }) { Text("Ruko", color = TextSecondary) }
            }
        )
    }
}
