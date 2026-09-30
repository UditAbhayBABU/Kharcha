package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LedgerType(val label: String) {
    CATEGORY("Category Khata"),
    BUSINESS("Business Khata"),
    POT("Gullak Passbook"),
    UDHAAR_PARTY("Udhaar Ledger")
}

data class LedgerTarget(
    val type: LedgerType,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val colorHex: String? = null,
    val icon: ImageVector? = null,
    val targetAmount: Double? = null,
    val currentBalance: Double? = null
)

/**
 * Universal Ledger Row Item for clean tabular representation.
 */
data class LedgerRowItem(
    val id: String,
    val dateMillis: Long,
    val title: String,
    val note: String,
    val tag: String,
    val paymentMethod: String,
    val amount: Double,
    val isCredit: Boolean, // true = Credit (+), false = Debit (-)
    val runningBalance: Double,
    val originalExpense: Expense? = null,
    val originalUdhaarEntry: UdhaarEntry? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalLedgerSheet(
    target: LedgerTarget,
    allExpenses: List<Expense>,
    udhaarEntries: List<UdhaarEntry> = emptyList(),
    onDismiss: () -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onDeleteUdhaarEntry: ((UdhaarEntry) -> Unit)? = null,
    onAddDepositToPot: ((potId: String, amount: Double) -> Unit)? = null,
    onAddUdhaarEntry: ((partyId: String, amount: Double, isRepayment: Boolean, note: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, THIS_MONTH, TODAY
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var udhaarEntryToDelete by remember { mutableStateOf<UdhaarEntry?>(null) }
    var showDepositDialog by remember { mutableStateOf(false) }
    var showUdhaarEntryDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val shortDateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    // Build chronological ledger rows
    val ledgerRows = remember(target, allExpenses, udhaarEntries, searchQuery, selectedFilter) {
        val now = java.util.Calendar.getInstance()
        val curMonth = now.get(java.util.Calendar.MONTH)
        val curYear = now.get(java.util.Calendar.YEAR)
        val startOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        when (target.type) {
            LedgerType.CATEGORY -> {
                val matched = allExpenses.filter {
                    it.category.equals(target.title, ignoreCase = true) || it.category == target.id
                }.sortedBy { it.dateMillis } // Ascending to calculate running total

                var runningTotal = 0.0
                matched.map { exp ->
                    runningTotal += exp.amount
                    LedgerRowItem(
                        id = exp.id,
                        dateMillis = exp.dateMillis,
                        title = exp.category,
                        note = exp.note.ifBlank { if (exp.businessName != null) "Business: ${exp.businessName}" else "Expense" },
                        tag = if (exp.contextType == ContextType.BUSINESS) "Business" else "Personal",
                        paymentMethod = exp.paymentMethod,
                        amount = exp.amount,
                        isCredit = false,
                        runningBalance = runningTotal,
                        originalExpense = exp
                    )
                }
            }

            LedgerType.BUSINESS -> {
                val matched = allExpenses.filter {
                    it.businessId == target.id ||
                            it.businessName?.equals(target.title, ignoreCase = true) == true ||
                            (it.contextType == ContextType.BUSINESS && target.id == "all_business")
                }.sortedBy { it.dateMillis }

                var runningTotal = 0.0
                matched.map { exp ->
                    runningTotal += exp.amount
                    LedgerRowItem(
                        id = exp.id,
                        dateMillis = exp.dateMillis,
                        title = exp.category,
                        note = exp.note.ifBlank { "Business kharcha" },
                        tag = exp.businessName ?: target.title,
                        paymentMethod = exp.paymentMethod,
                        amount = exp.amount,
                        isCredit = false,
                        runningBalance = runningTotal,
                        originalExpense = exp
                    )
                }
            }

            LedgerType.POT -> {
                val matched = allExpenses.filter {
                    it.potId == target.id || it.potName?.equals(target.title, ignoreCase = true) == true
                }.sortedBy { it.dateMillis }

                var runningTotal = 0.0
                matched.map { exp ->
                    runningTotal += exp.amount
                    LedgerRowItem(
                        id = exp.id,
                        dateMillis = exp.dateMillis,
                        title = exp.category,
                        note = exp.note.ifBlank { "Pot se kharcha kiya" },
                        tag = "Gullak Spend",
                        paymentMethod = exp.paymentMethod,
                        amount = exp.amount,
                        isCredit = false, // Spends deduct from pot
                        runningBalance = runningTotal,
                        originalExpense = exp
                    )
                }
            }

            LedgerType.UDHAAR_PARTY -> {
                val matched = udhaarEntries.filter { it.partyId == target.id }.sortedBy { it.dateMillis }
                var netBalance = 0.0
                matched.map { entry ->
                    if (entry.isRepayment) {
                        netBalance -= entry.amount // Received repayment reduces outstanding
                    } else {
                        netBalance += entry.amount // Giving increases outstanding
                    }
                    LedgerRowItem(
                        id = entry.id,
                        dateMillis = entry.dateMillis,
                        title = if (entry.isRepayment) "Wapas Mila (Credit)" else "Maine Diya (Debit)",
                        note = entry.note.ifBlank { if (entry.isRepayment) "Vasooli / Repayment" else "Udhaar Diya" },
                        tag = if (entry.isRepayment) "Received" else "Given",
                        paymentMethod = "Ledger",
                        amount = entry.amount,
                        isCredit = entry.isRepayment,
                        runningBalance = netBalance,
                        originalUdhaarEntry = entry
                    )
                }
            }
        }.reversed().filter { item ->
            // Filter by search
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                item.title.lowercase().contains(q) ||
                        item.note.lowercase().contains(q) ||
                        item.paymentMethod.lowercase().contains(q) ||
                        item.amount.toString().contains(q)
            }
            // Filter by period
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = item.dateMillis }
            val matchesFilter = when (selectedFilter) {
                "TODAY" -> item.dateMillis >= startOfToday
                "THIS_MONTH" -> cal.get(java.util.Calendar.MONTH) == curMonth && cal.get(java.util.Calendar.YEAR) == curYear
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val totalDebit = ledgerRows.filter { !it.isCredit }.sumOf { it.amount }
    val totalCredit = ledgerRows.filter { it.isCredit }.sumOf { it.amount }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderMedium) },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        ProvideSafeTextToolbar {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
            // 1. TOP HEADER & TITLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(GoldMetallicRimBrush)
                            .border(BorderStroke(1.2.dp, Color.White.copy(alpha = 0.6f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = target.icon ?: when (target.type) {
                                LedgerType.CATEGORY -> Icons.Default.Category
                                LedgerType.BUSINESS -> Icons.Default.Business
                                LedgerType.POT -> Icons.Default.Savings
                                LedgerType.UDHAAR_PARTY -> Icons.Default.AccountBalanceWallet
                            },
                            contentDescription = null,
                            tint = TextOnGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = target.title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = SurfaceElevated,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, BorderSubtle)
                            ) {
                                Text(
                                    text = target.type.label,
                                    fontSize = 10.sp,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = target.subtitle ?: "${ledgerRows.size} len-den records",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                Row {
                    IconButton(onClick = {
                        exportLedgerCsv(context, target.title, ledgerRows, dateFormat)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share CSV",
                            tint = GoldLight
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Band Karein",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. HERO FINANCIAL SUMMARY CARDS
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.2.dp, MetallicRimBrush), RoundedCornerShape(18.dp)),
                color = SurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (target.type) {
                        LedgerType.POT -> {
                            Column {
                                Text("Gullak me Baki Rashi", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                Text("₹${(target.currentBalance ?: 0.0).toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = GoldPrimary)
                                if ((target.targetAmount ?: 0.0) > 0) {
                                    Text("Target: ₹${target.targetAmount!!.toInt()}", fontSize = 11.sp, color = EmeraldCash)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Kul Spent: ₹${totalDebit.toInt()}", fontSize = 12.sp, color = RedExpense, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (onAddDepositToPot != null) {
                                    Button(
                                        onClick = { showDepositDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldCash),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Deposit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        LedgerType.UDHAAR_PARTY -> {
                            Column {
                                Text("Baki Balance", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                val bal = (target.currentBalance ?: (totalDebit - totalCredit))
                                Text("₹${bal.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (bal > 0) OrangeWarning else EmeraldCash)
                                Text(if (bal > 0) "Vasooli Baki Hai" else "Chukta / Settled", fontSize = 11.sp, color = if (bal > 0) OrangeWarning else EmeraldCash)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Diya: ₹${totalDebit.toInt()}", fontSize = 11.sp, color = OrangeWarning)
                                Text("Mila: ₹${totalCredit.toInt()}", fontSize = 11.sp, color = EmeraldCash)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (onAddUdhaarEntry != null) {
                                    Button(
                                        onClick = { showUdhaarEntryDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("+ / - Entry", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextOnGold)
                                    }
                                }
                            }
                        }

                        else -> {
                            Column {
                                Text("Kul Kharcha (Total)", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                Text("₹${totalDebit.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = RedExpense)
                                Text("${ledgerRows.size} transactions total", fontSize = 11.sp, color = GoldLight)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val avg = if (ledgerRows.isNotEmpty()) totalDebit / ledgerRows.size else 0.0
                                Text("Ausat (Average)", fontSize = 11.sp, color = TextMuted)
                                Text("₹${avg.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. SEARCH & PERIOD FILTER ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search note ya amount...", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                // Period Chips
                TactileChip(
                    text = "Sabhi",
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                TactileChip(
                    text = "Is Mahine",
                    selected = selectedFilter == "THIS_MONTH",
                    onClick = { selectedFilter = "THIS_MONTH" }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. TABULAR PASSBOOK / STATEMENT HEADER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceElevated.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TARIKH / VIVARAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                    Text("AMOUNT (DR / CR) & ACTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. LEDGER ROWS (TABULAR PASSBOOK LIST)
            if (ledgerRows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Text("Is ledger me koi record nahi mila.", fontSize = 13.sp, color = TextSecondary)
                        if (searchQuery.isNotBlank() || selectedFilter != "ALL") {
                            TextButton(onClick = { searchQuery = ""; selectedFilter = "ALL" }) {
                                Text("Filter Hatayein", color = GoldPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp)
                ) {
                    items(ledgerRows, key = { it.id }) { row ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(14.dp)),
                            color = SurfaceCard,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = row.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = SurfaceElevated,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = row.paymentMethod,
                                                fontSize = 9.sp,
                                                color = GoldLight,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = shortDateFormat.format(Date(row.dateMillis)),
                                        fontSize = 10.5.sp,
                                        color = TextMuted
                                    )

                                    if (row.note.isNotBlank()) {
                                        Text(
                                            text = row.note,
                                            fontSize = 11.5.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Right Amount & Delete Action
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (row.isCredit) "+" else "-"}₹${row.amount.toInt()}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (row.isCredit) EmeraldCash else RedExpense
                                        )
                                        Text(
                                            text = "Bal: ₹${row.runningBalance.toInt()}",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }

                                    // Direct Database Delete Button with Rollback
                                    IconButton(
                                        onClick = {
                                            if (row.originalExpense != null) {
                                                expenseToDelete = row.originalExpense
                                            } else if (row.originalUdhaarEntry != null) {
                                                udhaarEntryToDelete = row.originalUdhaarEntry
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Record",
                                            tint = RedExpense.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // CONFIRM DELETE EXPENSE DIALOG (WITH ROLLBACK WARNING)
    expenseToDelete?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            containerColor = SurfaceElevated,
            title = {
                Text("Is Kharcha Record Ko Delete Karein?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "₹${exp.amount.toInt()} (${exp.category}) permanently delete ho jayega.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    if (!exp.potId.isNullOrBlank()) {
                        Text(
                            text = "✓ Gullak Balance (+₹${exp.amount.toInt()}) wapas restore ho jayega.",
                            color = EmeraldCash,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!exp.udhaarPersonId.isNullOrBlank()) {
                        Text(
                            text = "✓ Udhaar party ledger se yeh entry minus hokar balance normal ho jayega.",
                            color = OrangeWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = exp
                        expenseToDelete = null
                        onDeleteExpense(toDel)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpense)
                ) {
                    Text("Delete & Rollback", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Ruko", color = TextSecondary)
                }
            }
        )
    }

    // CONFIRM DELETE UDHAAR ENTRY DIALOG
    udhaarEntryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { udhaarEntryToDelete = null },
            containerColor = SurfaceElevated,
            title = {
                Text("Is Udhaar Entry Ko Delete Karein?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "₹${entry.amount.toInt()} ki yeh entry delete hone par party ka balance automatically rollback hokar adjust ho jayega.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = entry
                        udhaarEntryToDelete = null
                        onDeleteUdhaarEntry?.invoke(toDel)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpense)
                ) {
                    Text("Delete Karo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { udhaarEntryToDelete = null }) {
                    Text("Ruko", color = TextSecondary)
                }
            }
        )
    }

    // POT DEPOSIT DIALOG
    if (showDepositDialog && onAddDepositToPot != null) {
        var depositAmountString by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("${target.title} me Jama Karein", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gullak me bachayi hui rashi jodein:", fontSize = 12.sp, color = TextMuted)
                    OutlinedTextField(
                        value = depositAmountString,
                        onValueChange = { depositAmountString = it },
                        label = { Text("Amount (₹)", color = TextMuted) },
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
                        val amt = depositAmountString.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onAddDepositToPot(target.id, amt)
                            showDepositDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCash),
                    enabled = (depositAmountString.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Jama Karo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) { Text("Ruko", color = TextSecondary) }
            }
        )
    }

    // UDHAAR ENTRY DIALOG
    if (showUdhaarEntryDialog && onAddUdhaarEntry != null) {
        var udhaarAmtString by remember { mutableStateOf("") }
        var isRepayment by remember { mutableStateOf(false) }
        var noteString by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showUdhaarEntryDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("${target.title} - Entry Jodo", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TactileChip(
                            text = "Maine Diya",
                            selected = !isRepayment,
                            onClick = { isRepayment = false },
                            accentColor = OrangeWarning,
                            modifier = Modifier.weight(1f)
                        )
                        TactileChip(
                            text = "Wapas Mila",
                            selected = isRepayment,
                            onClick = { isRepayment = true },
                            accentColor = EmeraldCash,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = udhaarAmtString,
                        onValueChange = { udhaarAmtString = it },
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
                        value = noteString,
                        onValueChange = { noteString = it },
                        label = { Text("Note / Vivaran (Optional)", color = TextMuted) },
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
                        val amt = udhaarAmtString.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onAddUdhaarEntry(target.id, amt, isRepayment, noteString.trim())
                            showUdhaarEntryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRepayment) EmeraldCash else OrangeWarning),
                    enabled = (udhaarAmtString.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Save Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUdhaarEntryDialog = false }) { Text("Ruko", color = TextSecondary) }
            }
        )
    }
}

private fun exportLedgerCsv(
    context: Context,
    ledgerTitle: String,
    rows: List<LedgerRowItem>,
    dateFormat: SimpleDateFormat
) {
    try {
        val safeName = ledgerTitle.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val csvFile = File(context.cacheDir, "LEDGER_${safeName}_${System.currentTimeMillis()}.csv")
        val writer = FileWriter(csvFile)
        writer.append("ID,Date,Title,Note,Type,PaymentMethod,Amount,RunningBalance\n")
        rows.forEach { r ->
            writer.append("\"${r.id}\",")
            writer.append("\"${dateFormat.format(Date(r.dateMillis))}\",")
            writer.append("\"${r.title}\",")
            writer.append("\"${r.note.replace("\"", "\"\"")}\",")
            writer.append("\"${if (r.isCredit) "CREDIT" else "DEBIT"}\",")
            writer.append("\"${r.paymentMethod}\",")
            writer.append("${if (r.isCredit) r.amount else -r.amount},")
            writer.append("${r.runningBalance}\n")
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
        context.startActivity(Intent.createChooser(shareIntent, "$ledgerTitle Ka CSV Export Share Karein"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
