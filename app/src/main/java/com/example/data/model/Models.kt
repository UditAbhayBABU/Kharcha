package com.example.data.model

enum class SyncState {
    SYNCED,
    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE
}

enum class ContextType {
    PERSONAL,
    BUSINESS
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card"),
    BANK_TRANSFER("Bank Transfer"),
    OTHER("Other")
}

data class Expense(
    val id: String,
    val userId: String = "",
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val category: String,
    val note: String = "",
    val paymentMethod: String = PaymentMethod.UPI.label,
    val contextType: ContextType = ContextType.PERSONAL,
    val businessId: String? = null,
    val businessName: String? = null,
    val potId: String? = null,
    val potName: String? = null,
    val udhaarPersonId: String? = null,
    val udhaarPersonName: String? = null,
    val receiptUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING_INSERT,
    val sheetsSynced: Boolean = false,
    val excelSynced: Boolean = false
)

data class CategoryItem(
    val id: String,
    val userId: String = "",
    val name: String,
    val iconName: String = "category",
    val colorHex: String = "#F59E0B",
    val isDefault: Boolean = false,
    val syncState: SyncState = SyncState.SYNCED
)

data class Business(
    val id: String,
    val userId: String = "",
    val name: String,
    val gstOrRegNo: String = "",
    val note: String = "",
    val colorHex: String = "#3B82F6",
    val createdAt: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING_INSERT
)

data class Pot(
    val id: String,
    val userId: String = "",
    val name: String,
    val targetAmount: Double = 0.0,
    val currentBalance: Double = 0.0,
    val totalDeposited: Double = 0.0,
    val totalSpent: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING_INSERT
)

data class UdhaarParty(
    val id: String,
    val userId: String = "",
    val name: String,
    val phone: String = "",
    val note: String = "",
    val totalGiven: Double = 0.0,
    val totalReceived: Double = 0.0,
    val expectedReturnDateMillis: Long? = null,
    val reminderEnabled: Boolean = true,
    val reminderIntervalDays: Int = 7,
    val lastReminderSentMillis: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING_INSERT
) {
    val outstandingBalance: Double
        get() = totalGiven - totalReceived
}

data class UdhaarEntry(
    val id: String,
    val partyId: String,
    val userId: String = "",
    val amount: Double,
    val isRepayment: Boolean = false, // false = Given (Kharcha/Udhaar), true = Received (Wapas mila)
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val syncState: SyncState = SyncState.PENDING_INSERT
)

data class RecurringExpense(
    val id: String,
    val userId: String = "",
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY, YEARLY
    val startDateMillis: Long,
    val endDateMillis: Long? = null,
    val nextDueMillis: Long,
    val paymentMethod: String = PaymentMethod.UPI.label,
    val contextType: ContextType = ContextType.PERSONAL,
    val businessId: String? = null,
    val potId: String? = null,
    val note: String = "",
    val isActive: Boolean = true,
    val lastExecutedDateKey: String = "",
    val syncState: SyncState = SyncState.PENDING_INSERT
)

data class CategoryBudget(
    val categoryName: String,
    val userId: String = "",
    val monthlyLimit: Double,
    val isEnabled: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_INSERT
)

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String = "",
    val role: String = "user", // "admin" or "user"
    val isBudgetFeatureEnabled: Boolean = false,
    val sheetsUrl: String = "",
    val sheetsAutoSync: Boolean = true,
    val excelPreferredSyncTime: String = "02:00", // 24-hr time
    val lastExcelSyncMillis: Long = 0L,
    val pinHash: String = "",
    val isPinLockEnabled: Boolean = false
)
