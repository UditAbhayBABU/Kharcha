package com.example.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.*
import com.example.ui.components.KharchaCard
import com.example.ui.components.TactileChip
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    expenses: List<Expense>,
    budgets: List<CategoryBudget>,
    isBudgetFeatureEnabled: Boolean,
    pots: List<Pot>,
    udhaarParties: List<UdhaarParty>
) {
    var selectedTimePeriod by remember { mutableStateOf("THIS_MONTH") } // THIS_MONTH, TODAY, ALL_TIME

    val now = remember { Calendar.getInstance() }
    val currentMonth = now.get(Calendar.MONTH)
    val currentYear = now.get(Calendar.YEAR)

    // Filter expenses by period
    val periodExpenses = remember(expenses, selectedTimePeriod) {
        val cal = Calendar.getInstance()
        when (selectedTimePeriod) {
            "TODAY" -> {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                expenses.filter { it.dateMillis >= startOfDay }
            }
            "THIS_MONTH" -> {
                expenses.filter {
                    cal.timeInMillis = it.dateMillis
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
            }
            else -> expenses
        }
    }

    val totalSpending = periodExpenses.sumOf { it.amount }

    // Breakdown by Category
    val categoryBreakdown = remember(periodExpenses) {
        periodExpenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Breakdown by Payment Method
    val paymentBreakdown = remember(periodExpenses) {
        periodExpenses.groupBy { it.paymentMethod }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Personal vs Business Breakdown
    val personalTotal = remember(periodExpenses) {
        periodExpenses.filter { it.contextType == ContextType.PERSONAL }.sumOf { it.amount }
    }
    val businessTotal = remember(periodExpenses) {
        periodExpenses.filter { it.contextType == ContextType.BUSINESS }.sumOf { it.amount }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kharcha Hisab & Analysis",
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
            // PERIOD FILTER CHIPS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TactileChip(
                        text = "Aaj (Today)",
                        selected = selectedTimePeriod == "TODAY",
                        onClick = { selectedTimePeriod = "TODAY" },
                        modifier = Modifier.weight(1f)
                    )
                    TactileChip(
                        text = "Is Mahine",
                        selected = selectedTimePeriod == "THIS_MONTH",
                        onClick = { selectedTimePeriod = "THIS_MONTH" },
                        modifier = Modifier.weight(1f)
                    )
                    TactileChip(
                        text = "Kul (All Time)",
                        selected = selectedTimePeriod == "ALL_TIME",
                        onClick = { selectedTimePeriod = "ALL_TIME" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // TOTAL SPENT HERO CARD
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGlow, RoundedCornerShape(18.dp)),
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (selectedTimePeriod == "THIS_MONTH") "Is Mahine Ka Kul Kharcha" else if (selectedTimePeriod == "TODAY") "Aaj Ka Kharcha" else "Kul Kharcha",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₹${totalSpending.toInt()}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary
                        )

                        // Personal vs Business split
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Personal: ₹${personalTotal.toInt()}",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Business: ₹${businessTotal.toInt()}",
                                fontSize = 13.sp,
                                color = BlueBank,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // MONTHLY BUDGET WARNING / STATUS (IF ENABLED)
            if (isBudgetFeatureEnabled && budgets.isNotEmpty()) {
                item {
                    Text(
                        text = "Monthly Budgets Status",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        budgets.filter { it.isEnabled }.forEach { budget ->
                            val spent = categoryBreakdown.find { it.first == budget.categoryName }?.second ?: 0.0
                            val pct = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat() else 0f
                            val isExceeded = spent >= budget.monthlyLimit

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isExceeded) RedExpense else BorderSubtle, RoundedCornerShape(14.dp)),
                                color = SurfaceCard,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = budget.categoryName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                        Text(
                                            text = "₹${spent.toInt()} / ₹${budget.monthlyLimit.toInt()}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = if (isExceeded) RedExpense else TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { pct.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (isExceeded) RedExpense else GoldPrimary,
                                        trackColor = SurfaceDark
                                    )
                                    if (isExceeded) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "“Iss mahine already bhot kharcha ho gaya MALIKK”",
                                            fontSize = 11.sp,
                                            color = OrangeWarning,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SPENDING BY CATEGORY BREAKDOWN
            item {
                Text(
                    text = "Category ke Anusar Kharcha",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (categoryBreakdown.isEmpty()) {
                    Text("Is samay me koi kharcha nahi mila.", color = TextMuted, fontSize = 12.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categoryBreakdown.forEach { (catName, amount) ->
                            val percentage = if (totalSpending > 0) ((amount / totalSpending) * 100).toInt() else 0
                            val progress = if (totalSpending > 0) (amount / totalSpending).toFloat() else 0f

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                                color = SurfaceCard,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = catName, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                                        Text(text = "₹${amount.toInt()} ($percentage%)", fontWeight = FontWeight.Bold, color = GoldLight, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = GoldPrimary,
                                        trackColor = SurfaceDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PAYMENT METHODS BREAKDOWN
            item {
                Text(
                    text = "Payment Method (Kisse Diye)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentBreakdown.forEach { (method, amount) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                            color = SurfaceCard,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = method, fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "₹${amount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
