package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.*

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val dateMillis: Long,
    val category: String,
    val note: String,
    val paymentMethod: String,
    val contextType: String,
    val businessId: String?,
    val businessName: String?,
    val potId: String?,
    val potName: String?,
    val udhaarPersonId: String?,
    val udhaarPersonName: String?,
    val receiptUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: String,
    val sheetsSynced: Boolean,
    val excelSynced: Boolean
) {
    fun toDomain(): Expense = Expense(
        id = id,
        userId = userId,
        amount = amount,
        dateMillis = dateMillis,
        category = category,
        note = note,
        paymentMethod = paymentMethod,
        contextType = if (contextType == ContextType.BUSINESS.name) ContextType.BUSINESS else ContextType.PERSONAL,
        businessId = businessId,
        businessName = businessName,
        potId = potId,
        potName = potName,
        udhaarPersonId = udhaarPersonId,
        udhaarPersonName = udhaarPersonName,
        receiptUrl = receiptUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncState = SyncState.valueOf(syncState),
        sheetsSynced = sheetsSynced,
        excelSynced = excelSynced
    )

    companion object {
        fun fromDomain(domain: Expense): ExpenseEntity = ExpenseEntity(
            id = domain.id,
            userId = domain.userId,
            amount = domain.amount,
            dateMillis = domain.dateMillis,
            category = domain.category,
            note = domain.note,
            paymentMethod = domain.paymentMethod,
            contextType = domain.contextType.name,
            businessId = domain.businessId,
            businessName = domain.businessName,
            potId = domain.potId,
            potName = domain.potName,
            udhaarPersonId = domain.udhaarPersonId,
            udhaarPersonName = domain.udhaarPersonName,
            receiptUrl = domain.receiptUrl,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            syncState = domain.syncState.name,
            sheetsSynced = domain.sheetsSynced,
            excelSynced = domain.excelSynced
        )
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean,
    val syncState: String
) {
    fun toDomain(): CategoryItem = CategoryItem(
        id = id,
        userId = userId,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isDefault = isDefault,
        syncState = SyncState.valueOf(syncState)
    )

    companion object {
        fun fromDomain(domain: CategoryItem): CategoryEntity = CategoryEntity(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            iconName = domain.iconName,
            colorHex = domain.colorHex,
            isDefault = domain.isDefault,
            syncState = domain.syncState.name
        )
    }
}

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val gstOrRegNo: String,
    val note: String,
    val colorHex: String,
    val createdAt: Long,
    val syncState: String
) {
    fun toDomain(): Business = Business(
        id = id,
        userId = userId,
        name = name,
        gstOrRegNo = gstOrRegNo,
        note = note,
        colorHex = colorHex,
        createdAt = createdAt,
        syncState = SyncState.valueOf(syncState)
    )

    companion object {
        fun fromDomain(domain: Business): BusinessEntity = BusinessEntity(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            gstOrRegNo = domain.gstOrRegNo,
            note = domain.note,
            colorHex = domain.colorHex,
            createdAt = domain.createdAt,
            syncState = domain.syncState.name
        )
    }
}

@Entity(tableName = "pots")
data class PotEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val currentBalance: Double,
    val totalDeposited: Double,
    val totalSpent: Double,
    val note: String,
    val createdAt: Long,
    val syncState: String
) {
    fun toDomain(): Pot = Pot(
        id = id,
        userId = userId,
        name = name,
        targetAmount = targetAmount,
        currentBalance = currentBalance,
        totalDeposited = totalDeposited,
        totalSpent = totalSpent,
        note = note,
        createdAt = createdAt,
        syncState = SyncState.valueOf(syncState)
    )

    companion object {
        fun fromDomain(domain: Pot): PotEntity = PotEntity(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            targetAmount = domain.targetAmount,
            currentBalance = domain.currentBalance,
            totalDeposited = domain.totalDeposited,
            totalSpent = domain.totalSpent,
            note = domain.note,
            createdAt = domain.createdAt,
            syncState = domain.syncState.name
        )
    }
}

@Entity(tableName = "udhaar_parties")
data class UdhaarPartyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val note: String,
    val totalGiven: Double,
    val totalReceived: Double,
    val expectedReturnDateMillis: Long?,
    val reminderEnabled: Boolean,
    val reminderIntervalDays: Int,
    val lastReminderSentMillis: Long,
    val createdAt: Long,
    val syncState: String
) {
    fun toDomain(): UdhaarParty = UdhaarParty(
        id = id,
        userId = userId,
        name = name,
        phone = phone,
        note = note,
        totalGiven = totalGiven,
        totalReceived = totalReceived,
        expectedReturnDateMillis = expectedReturnDateMillis,
        reminderEnabled = reminderEnabled,
        reminderIntervalDays = reminderIntervalDays,
        lastReminderSentMillis = lastReminderSentMillis,
        createdAt = createdAt,
        syncState = SyncState.valueOf(syncState)
    )

    companion object {
        fun fromDomain(domain: UdhaarParty): UdhaarPartyEntity = UdhaarPartyEntity(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            phone = domain.phone,
            note = domain.note,
            totalGiven = domain.totalGiven,
            totalReceived = domain.totalReceived,
            expectedReturnDateMillis = domain.expectedReturnDateMillis,
            reminderEnabled = domain.reminderEnabled,
            reminderIntervalDays = domain.reminderIntervalDays,
            lastReminderSentMillis = domain.lastReminderSentMillis,
            createdAt = domain.createdAt,
            syncState = domain.syncState.name
        )
    }
}

@Entity(tableName = "udhaar_entries")
data class UdhaarEntryEntity(
    @PrimaryKey val id: String,
    val partyId: String,
    val userId: String,
    val amount: Double,
    val isRepayment: Boolean,
    val dateMillis: Long,
    val note: String,
    val syncState: String
) {
    fun toDomain(): UdhaarEntry = UdhaarEntry(
        id = id,
        partyId = partyId,
        userId = userId,
        amount = amount,
        isRepayment = isRepayment,
        dateMillis = dateMillis,
        note = note,
        syncState = SyncState.valueOf(syncState)
    )

    companion object {
        fun fromDomain(domain: UdhaarEntry): UdhaarEntryEntity = UdhaarEntryEntity(
            id = domain.id,
            partyId = domain.partyId,
            userId = domain.userId,
            amount = domain.amount,
            isRepayment = domain.isRepayment,
            dateMillis = domain.dateMillis,
            note = domain.note,
            syncState = domain.syncState.name
        )
    }
}

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: String,
    val startDateMillis: Long,
    val endDateMillis: Long?,
    val nextDueMillis: Long,
    val paymentMethod: String,
    val contextType: String,
    val businessId: String?,
    val potId: String?,
    val note: String,
    val isActive: Boolean,
    val lastExecutedDateKey: String,
    val syncState: String
) {
    fun toDomain(): RecurringExpense = RecurringExpense(
        id = id,
        userId = userId,
        title = title,
        amount = amount,
        category = category,
        frequency = frequency,
        startDateMillis = startDateMillis,
        endDateMillis = endDateMillis,
        nextDueMillis = nextDueMillis,
        paymentMethod = paymentMethod,
        contextType = if (contextType == ContextType.BUSINESS.name) ContextType.BUSINESS else ContextType.PERSONAL,
        businessId = businessId,
        potId = potId,
        note = note,
        isActive = isActive,
        lastExecutedDateKey = lastExecutedDateKey,
        syncState = SyncState.valueOf(syncState)
    )

    companion object {
        fun fromDomain(domain: RecurringExpense): RecurringExpenseEntity = RecurringExpenseEntity(
            id = domain.id,
            userId = domain.userId,
            title = domain.title,
            amount = domain.amount,
            category = domain.category,
            frequency = domain.frequency,
            startDateMillis = domain.startDateMillis,
            endDateMillis = domain.endDateMillis,
            nextDueMillis = domain.nextDueMillis,
            paymentMethod = domain.paymentMethod,
            contextType = domain.contextType.name,
            businessId = domain.businessId,
            potId = domain.potId,
            note = domain.note,
            isActive = domain.isActive,
            lastExecutedDateKey = domain.lastExecutedDateKey,
            syncState = domain.syncState.name
        )
    }
}

@Entity(tableName = "category_budgets", primaryKeys = ["categoryName", "userId"])
data class CategoryBudgetEntity(
    val categoryName: String,
    val userId: String,
    val monthlyLimit: Double,
    val isEnabled: Boolean,
    val syncState: String
) {
    fun toDomain(): CategoryBudget = CategoryBudget(
        categoryName = categoryName,
        userId = userId,
        monthlyLimit = monthlyLimit,
        isEnabled = isEnabled,
        syncState = SyncState.valueOf(syncState)
    )

    companion object {
        fun fromDomain(domain: CategoryBudget): CategoryBudgetEntity = CategoryBudgetEntity(
            categoryName = domain.categoryName,
            userId = domain.userId,
            monthlyLimit = domain.monthlyLimit,
            isEnabled = domain.isEnabled,
            syncState = domain.syncState.name
        )
    }
}
