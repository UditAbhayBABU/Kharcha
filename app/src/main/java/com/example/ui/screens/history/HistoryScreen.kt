package com.example.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SortOption(val label: String) {
    NEWEST("Naye Pehle"),
    OLDEST("Purane Pehle"),
    HIGHEST("Jyada Amount"),
    LOWEST("Kam Amount")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    expenses: List<Expense>,
    categories: List<CategoryItem>,
    businesses: List<Business>,
    onDeleteExpense: (Expense) -> Unit,
    onRestoreExpense: (Expense) -> Unit,
    onSyncNow: () -> Unit,
    isSyncing: Boolean,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedContextFilter by remember { mutableStateOf<ContextType?>(null) }
    var selectedPaymentFilter by remember { mutableStateOf<String?>(null) }
    var currentSort by remember { mutableStateOf(SortOption.NEWEST) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    // Filter and Sort logic
    val filteredExpenses = remember(
        expenses,
        searchQuery,
        selectedCategoryFilter,
        selectedContextFilter,
        selectedPaymentFilter,
        currentSort
    ) {
        var list = expenses

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.category.lowercase().contains(q) ||
                        it.note.lowercase().contains(q) ||
                        (it.businessName?.lowercase()?.contains(q) == true) ||
                        (it.udhaarPersonName?.lowercase()?.contains(q) == true) ||
                        it.amount.toString().contains(q)
            }
        }

        if (selectedCategoryFilter != null) {
            list = list.filter { it.category == selectedCategoryFilter }
        }

        if (selectedContextFilter != null) {
            list = list.filter { it.contextType == selectedContextFilter }
        }

        if (selectedPaymentFilter != null) {
            list = list.filter { it.paymentMethod == selectedPaymentFilter }
        }

        when (currentSort) {
            SortOption.NEWEST -> list.sortedByDescending { it.dateMillis }
            SortOption.OLDEST -> list.sortedBy { it.dateMillis }
            SortOption.HIGHEST -> list.sortedByDescending { it.amount }
            SortOption.LOWEST -> list.sortedBy { it.amount }
        }
    }

    val totalAmount = filteredExpenses.sumOf { it.amount }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kharche ka Hisab",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${filteredExpenses.size} Kharche • Total ₹${totalAmount.toInt()}",
                            fontSize = 12.sp,
                            color = GoldLight
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSyncNow, enabled = !isSyncing) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = GoldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync",
                                tint = EmeraldCash
                            )
                        }
                    }
                    IconButton(onClick = { showFilterSheet = !showFilterSheet }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = if (selectedCategoryFilter != null || selectedContextFilter != null || selectedPaymentFilter != null) GoldPrimary else TextSecondary
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
            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search karo (Khana, Petrol, ₹500, Note...)", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated
                ),
                singleLine = true
            )

            // FILTER & SORT CHIP ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sort Toggle Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable {
                            val nextIndex = (currentSort.ordinal + 1) % SortOption.values().size
                            currentSort = SortOption.values()[nextIndex]
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentSort.label,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Context Filter (All / Personal / Business)
                listOf(null to "Sabhi", ContextType.PERSONAL to "Personal", ContextType.BUSINESS to "Business").forEach { (type, label) ->
                    val isSel = selectedContextFilter == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) GoldPrimary.copy(alpha = 0.2f) else SurfaceElevated)
                            .border(1.dp, if (isSel) GoldPrimary else BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { selectedContextFilter = type }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = if (isSel) GoldPrimary else TextSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // LIST OF EXPENSES
            if (filteredExpenses.isEmpty()) {
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Koi kharcha nahi mila!",
                            fontSize = 15.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Neeche '+' dabakar naya kharcha jodo.",
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
                    items(filteredExpenses, key = { it.id }) { item ->
                        ExpenseListItem(
                            expense = item,
                            dateFormat = dateFormat,
                            onDelete = {
                                onDeleteExpense(item)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Kharcha hata diya gaya (₹${item.amount.toInt()})",
                                        actionLabel = "Wapas Lao",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onRestoreExpense(item)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseListItem(
    expense: Expense,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .border(1.dp, BorderMedium, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.category,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Context badge (Business / Personal)
                    if (expense.contextType == ContextType.BUSINESS) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BlueBank.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = expense.businessName ?: "Business",
                                fontSize = 10.sp,
                                color = BlueBank,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(expense.dateMillis)),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(text = " • ", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = expense.paymentMethod,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    if (!expense.potName.isNullOrBlank()) {
                        Text(text = " • ", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = "Gullak: ${expense.potName}",
                            fontSize = 11.sp,
                            color = EmeraldCash
                        )
                    }
                }

                if (expense.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.note,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount and Delete
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${expense.amount.toInt()}",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync status indicator
                    Icon(
                        imageVector = if (expense.syncState == SyncState.SYNCED) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = "Sync Status",
                        tint = if (expense.syncState == SyncState.SYNCED) EmeraldCash else OrangeWarning,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SurfaceElevated,
            title = { Text("Kharcha Hatana Hai?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("₹${expense.amount.toInt()} ka yeh kharcha delete ho jayega.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete Karo", color = RedExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Nahi", color = TextSecondary)
                }
            }
        )
    }
}
