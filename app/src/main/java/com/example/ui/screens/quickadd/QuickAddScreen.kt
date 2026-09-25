package com.example.ui.screens.quickadd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(
    userId: String,
    categories: List<CategoryItem>,
    businesses: List<Business>,
    pots: List<Pot>,
    udhaarParties: List<UdhaarParty>,
    budgets: List<CategoryBudget>,
    isBudgetFeatureEnabled: Boolean,
    onSaveExpense: (Expense) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    var amountString by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Khana & Peena") }
    var selectedContext by remember { mutableStateOf(ContextType.PERSONAL) }
    var selectedBusinessId by remember { mutableStateOf<String?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.UPI.label) }

    // Secondary / Optional Fields
    var showMoreOptions by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var selectedPotId by remember { mutableStateOf<String?>(null) }
    var selectedUdhaarPartyId by remember { mutableStateOf<String?>(null) }

    // Humorous Budget Alert State
    var showBudgetWarningDialog by remember { mutableStateOf(false) }
    var exceededBudgetName by remember { mutableStateOf("") }
    var exceededBudgetLimit by remember { mutableStateOf(0.0) }
    var exceededCurrentSpent by remember { mutableStateOf(0.0) }

    // Keep selectedCategory valid if categories change
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && categories.none { it.name == selectedCategory }) {
            selectedCategory = categories.first().name
        }
    }

    val amountValue = amountString.toDoubleOrNull() ?: 0.0

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
                                .background(GoldPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Jaldi Kharcha Jodo",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // 1. AMOUNT DISPLAY (Bold Tactile Card)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, BorderGlow, RoundedCornerShape(20.dp)),
                color = SurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Kitna Kharch Hua?",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (amountString.isEmpty()) "0" else amountString,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (amountString.isEmpty()) TextMuted else TextPrimary,
                            maxLines = 1
                        )
                    }

                    // Fast Preset Quick Amount Chips (+50, +100, +200, +500, +2000)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50, 100, 200, 500, 1000, 2000).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .clickable {
                                        val current = amountString.toDoubleOrNull() ?: 0.0
                                        amountString = (current + preset).toInt().toString()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+₹$preset",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberVibrant
                                )
                            }
                        }
                    }
                }
            }

            // 2. CONTEXT SELECTOR (Personal vs Business)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TactileChip(
                    text = "Personal",
                    selected = selectedContext == ContextType.PERSONAL,
                    onClick = {
                        selectedContext = ContextType.PERSONAL
                        selectedBusinessId = null
                    },
                    icon = Icons.Default.Person,
                    accentColor = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (businesses.isNotEmpty()) {
                    TactileChip(
                        text = if (selectedBusinessId != null) {
                            businesses.find { it.id == selectedBusinessId }?.name ?: "Business"
                        } else "Business",
                        selected = selectedContext == ContextType.BUSINESS,
                        onClick = {
                            selectedContext = ContextType.BUSINESS
                            if (selectedBusinessId == null) {
                                selectedBusinessId = businesses.firstOrNull()?.id
                            }
                        },
                        icon = Icons.Default.Business,
                        accentColor = BlueBank,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // If Business selected and multiple exist, show business switcher chips
            if (selectedContext == ContextType.BUSINESS && businesses.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    businesses.forEach { biz ->
                        val isSelected = selectedBusinessId == biz.id
                        TactileChip(
                            text = biz.name,
                            selected = isSelected,
                            onClick = { selectedBusinessId = biz.id },
                            accentColor = BlueBank
                        )
                    }
                }
            }

            // 3. CATEGORY SELECTION (Fast Scrollable Chips)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        TactileChip(
                            text = cat.name,
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = cat.name },
                            accentColor = GoldPrimary
                        )
                    }
                }
            }

            // 4. NUMBER PAD FOR INSTANT ENTRY
            TactileNumberPad(
                onDigitClick = { digit ->
                    if (digit == "." && amountString.contains(".")) return@TactileNumberPad
                    if (amountString.length < 8) {
                        amountString += digit
                    }
                },
                onBackspace = {
                    if (amountString.isNotEmpty()) {
                        amountString = amountString.dropLast(1)
                    }
                },
                onClear = {
                    amountString = ""
                }
            )

            // 5. SECONDARY / OPTIONAL ACCORDION ("Aur Details? Note, Pot, Udhaar")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showMoreOptions = !showMoreOptions }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showMoreOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showMoreOptions) "Kam options dikhao" else "Note, Pot ya Udhaar jodo (Optional)",
                        fontSize = 13.sp,
                        color = GoldLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AnimatedVisibility(visible = showMoreOptions, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Payment Method
                    Text(
                        text = "Payment Kisse Kiya?",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMethod.values().forEach { method ->
                            TactileChip(
                                text = method.label,
                                selected = selectedPaymentMethod == method.label,
                                onClick = { selectedPaymentMethod = method.label },
                                accentColor = EmeraldCash
                            )
                        }
                    }

                    // Optional Note
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note / Comment (Optional)", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceElevated,
                            unfocusedContainerColor = SurfaceElevated
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Optional Pot (Virtual Gullak)
                    if (pots.isNotEmpty()) {
                        Text(
                            text = "Kisi Gullak / Pot se kharch karna hai?",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TactileChip(
                                text = "None",
                                selected = selectedPotId == null,
                                onClick = { selectedPotId = null },
                                accentColor = GoldPrimary
                            )
                            pots.forEach { pot ->
                                TactileChip(
                                    text = "${pot.name} (₹${pot.currentBalance.toInt()})",
                                    selected = selectedPotId == pot.id,
                                    onClick = { selectedPotId = pot.id },
                                    accentColor = EmeraldCash
                                )
                            }
                        }
                    }

                    // Optional Udhaar Link
                    if (udhaarParties.isNotEmpty()) {
                        Text(
                            text = "Yeh kisi ko Udhaar diya hai?",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TactileChip(
                                text = "Nahi",
                                selected = selectedUdhaarPartyId == null,
                                onClick = { selectedUdhaarPartyId = null },
                                accentColor = GoldPrimary
                            )
                            udhaarParties.forEach { party ->
                                TactileChip(
                                    text = party.name,
                                    selected = selectedUdhaarPartyId == party.id,
                                    onClick = { selectedUdhaarPartyId = party.id },
                                    accentColor = OrangeWarning
                                )
                            }
                        }
                    }
                }
            }

            // 6. SAVE EXPENSE BUTTON (Big, Bold, 1-Tap)
            TactileButton(
                text = "Kharcha Jodo  ₹${if (amountValue > 0) amountString else "0"}",
                icon = Icons.Default.Check,
                onClick = {
                    if (amountValue <= 0) return@TactileButton

                    val selectedBiz = businesses.find { it.id == selectedBusinessId }
                    val selectedPot = pots.find { it.id == selectedPotId }
                    val selectedUdhaar = udhaarParties.find { it.id == selectedUdhaarPartyId }

                    // Check monthly budget if enabled
                    if (isBudgetFeatureEnabled) {
                        val budget = budgets.find { it.categoryName == selectedCategory && it.isEnabled }
                        if (budget != null && amountValue >= budget.monthlyLimit) {
                            exceededBudgetName = selectedCategory
                            exceededBudgetLimit = budget.monthlyLimit
                            exceededCurrentSpent = amountValue
                            showBudgetWarningDialog = true
                        }
                    }

                    val expense = Expense(
                        id = "kh_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                        userId = userId,
                        amount = amountValue,
                        dateMillis = System.currentTimeMillis(),
                        category = selectedCategory,
                        note = note.trim(),
                        paymentMethod = selectedPaymentMethod,
                        contextType = selectedContext,
                        businessId = if (selectedContext == ContextType.BUSINESS) selectedBusinessId else null,
                        businessName = if (selectedContext == ContextType.BUSINESS) selectedBiz?.name else null,
                        potId = selectedPotId,
                        potName = selectedPot?.name,
                        udhaarPersonId = selectedUdhaarPartyId,
                        udhaarPersonName = selectedUdhaar?.name
                    )

                    onSaveExpense(expense)
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Kharcha ₹${expense.amount.toInt()} jud gaya!",
                            actionLabel = "Peeche jao",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onNavigateBack()
                        }
                    }

                    // Reset for next entry
                    amountString = ""
                    note = ""
                    selectedPotId = null
                    selectedUdhaarPartyId = null
                },
                enabled = amountValue > 0,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Humorous Budget Alert Dialog
        if (showBudgetWarningDialog) {
            HumorousBudgetDialog(
                categoryName = exceededBudgetName,
                limit = exceededBudgetLimit,
                currentSpent = exceededCurrentSpent,
                onDismiss = { showBudgetWarningDialog = false }
            )
        }
    }
}
