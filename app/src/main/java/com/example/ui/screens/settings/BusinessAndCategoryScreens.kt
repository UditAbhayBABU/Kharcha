package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Business
import com.example.data.model.CategoryItem
import com.example.ui.components.TactileButton
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessManagementScreen(
    userId: String,
    businesses: List<Business>,
    onAddBusiness: (Business) -> Unit,
    onDeleteBusiness: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Business Khata Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Naya Business", tint = GoldPrimary)
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
                .padding(16.dp)
        ) {
            Text(
                text = "Aapke Registered Business Khate",
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (businesses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Text("Abhi koi business nahi juda hai.", color = TextSecondary)
                        TactileButton(
                            text = "Naya Business Jodo",
                            onClick = { showAddDialog = true },
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(businesses, key = { it.id }) { biz ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                            color = SurfaceCard,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(BlueBank.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = BlueBank)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = biz.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                        if (biz.gstOrRegNo.isNotBlank()) {
                                            Text(text = "GST/Reg: ${biz.gstOrRegNo}", fontSize = 11.sp, color = TextMuted)
                                        }
                                    }
                                }
                                IconButton(onClick = { onDeleteBusiness(biz.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddBusinessDialog(
                userId = userId,
                onDismiss = { showAddDialog = false },
                onAdd = {
                    onAddBusiness(it)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddBusinessDialog(
    userId: String,
    onDismiss: () -> Unit,
    onAdd: (Business) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gstOrRegNo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Naya Business Jodo", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Business Ka Naam", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BlueBank
                    )
                )
                OutlinedTextField(
                    value = gstOrRegNo,
                    onValueChange = { gstOrRegNo = it },
                    label = { Text("GST ya Reg Number (Optional)", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BlueBank
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val biz = Business(
                            id = "biz_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                            userId = userId,
                            name = name.trim(),
                            gstOrRegNo = gstOrRegNo.trim(),
                            note = note.trim()
                        )
                        onAdd(biz)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BlueBank, contentColor = TextPrimary),
                enabled = name.isNotBlank()
            ) {
                Text("Jodo", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ruko", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    userId: String,
    categories: List<CategoryItem>,
    onAddCategory: (CategoryItem) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Categories Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nayi Category", tint = GoldPrimary)
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
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
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
                            Text(text = cat.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 15.sp)
                            if (!cat.isDefault) {
                                IconButton(onClick = { onDeleteCategory(cat.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            var catName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = SurfaceElevated,
                title = { Text("Nayi Category Jodo", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Ka Naam", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (catName.isNotBlank()) {
                                onAddCategory(
                                    CategoryItem(
                                        id = "cat_${System.currentTimeMillis()}",
                                        userId = userId,
                                        name = catName.trim()
                                    )
                                )
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = TextOnGold),
                        enabled = catName.isNotBlank()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Ruko", color = TextSecondary) }
                }
            )
        }
    }
}
