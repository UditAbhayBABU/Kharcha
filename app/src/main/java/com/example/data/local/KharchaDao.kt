package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KharchaDao {

    // --- EXPENSES ---
    @Query("SELECT * FROM expenses WHERE userId = :userId AND syncState != 'PENDING_DELETE' ORDER BY dateMillis DESC")
    fun getAllExpenses(userId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE userId = :userId AND syncState != 'SYNCED'")
    suspend fun getUnsyncedExpenses(userId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND sheetsSynced = 0 AND syncState != 'PENDING_DELETE' ORDER BY dateMillis ASC")
    suspend fun getPendingSheetsExpenses(userId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND excelSynced = 0 AND syncState != 'PENDING_DELETE' ORDER BY dateMillis ASC")
    suspend fun getPendingExcelExpenses(userId: String): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("UPDATE expenses SET syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markExpensePendingDelete(id: String)

    @Query("UPDATE expenses SET sheetsSynced = 1 WHERE id = :id")
    suspend fun markExpenseSheetsSynced(id: String)

    @Query("UPDATE expenses SET excelSynced = 1 WHERE id = :id")
    suspend fun markExpenseExcelSynced(id: String)

    // --- CATEGORIES ---
    @Query("SELECT * FROM categories WHERE userId = :userId OR isDefault = 1 ORDER BY name ASC")
    fun getAllCategories(userId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    // --- BUSINESSES ---
    @Query("SELECT * FROM businesses WHERE userId = :userId ORDER BY createdAt ASC")
    fun getAllBusinesses(userId: String): Flow<List<BusinessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinesses(businesses: List<BusinessEntity>)

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusinessById(id: String)

    // --- POTS ---
    @Query("SELECT * FROM pots WHERE userId = :userId ORDER BY createdAt ASC")
    fun getAllPots(userId: String): Flow<List<PotEntity>>

    @Query("SELECT * FROM pots WHERE id = :id LIMIT 1")
    suspend fun getPotById(id: String): PotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPot(pot: PotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPots(pots: List<PotEntity>)

    @Query("DELETE FROM pots WHERE id = :id")
    suspend fun deletePotById(id: String)

    // --- UDHAAR PARTIES ---
    @Query("SELECT * FROM udhaar_parties WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllUdhaarParties(userId: String): Flow<List<UdhaarPartyEntity>>

    @Query("SELECT * FROM udhaar_parties WHERE id = :id LIMIT 1")
    suspend fun getUdhaarPartyById(id: String): UdhaarPartyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUdhaarParty(party: UdhaarPartyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUdhaarParties(parties: List<UdhaarPartyEntity>)

    @Query("DELETE FROM udhaar_parties WHERE id = :id")
    suspend fun deleteUdhaarPartyById(id: String)

    // --- UDHAAR ENTRIES ---
    @Query("SELECT * FROM udhaar_entries WHERE partyId = :partyId ORDER BY dateMillis DESC")
    fun getEntriesForParty(partyId: String): Flow<List<UdhaarEntryEntity>>

    @Query("SELECT * FROM udhaar_entries WHERE userId = :userId")
    fun getAllUdhaarEntries(userId: String): Flow<List<UdhaarEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUdhaarEntry(entry: UdhaarEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUdhaarEntries(entries: List<UdhaarEntryEntity>)

    @Query("DELETE FROM udhaar_entries WHERE id = :id")
    suspend fun deleteUdhaarEntryById(id: String)

    // --- RECURRING EXPENSES ---
    @Query("SELECT * FROM recurring_expenses WHERE userId = :userId ORDER BY nextDueMillis ASC")
    fun getAllRecurringExpenses(userId: String): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveRecurringExpenses(userId: String): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(recurring: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpenseById(id: String)

    // --- BUDGETS ---
    @Query("SELECT * FROM category_budgets WHERE userId = :userId")
    fun getAllBudgets(userId: String): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<CategoryBudgetEntity>)

    @Query("DELETE FROM category_budgets WHERE categoryName = :categoryName AND userId = :userId")
    suspend fun deleteBudget(categoryName: String, userId: String)

    // Clear all for user on logout if needed
    @Query("DELETE FROM expenses WHERE userId = :userId")
    suspend fun clearUserExpenses(userId: String)
}
