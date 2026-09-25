package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.ExcelSyncManager
import com.example.data.remote.ExcelSyncResult
import com.example.data.remote.FirestoreSyncManager
import com.example.data.remote.GoogleDriveAndSheetsService
import com.example.data.remote.GoogleSheetsMirrorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class KharchaRepository(
    private val context: Context,
    private val dao: KharchaDao = KharchaDatabase.getInstance(context).kharchaDao(),
    private val firestoreSync: FirestoreSyncManager = FirestoreSyncManager(),
    private val sheetsMirror: GoogleSheetsMirrorService = GoogleSheetsMirrorService(),
    private val driveService: GoogleDriveAndSheetsService = GoogleDriveAndSheetsService(),
    private val excelSync: ExcelSyncManager = ExcelSyncManager(context)
) {
    private val repoScope = CoroutineScope(Dispatchers.IO)

    // Default categories seeded if none exist
    private val defaultCategories = listOf(
        CategoryItem("cat_food", "", "Khana & Peena", "restaurant", "#F59E0B", true),
        CategoryItem("cat_travel", "", "Safar & Petrol", "commute", "#3B82F6", true),
        CategoryItem("cat_bills", "", "Bills & Recharge", "receipt", "#EC4899", true),
        CategoryItem("cat_kirana", "", "Kirana & Ration", "shopping_cart", "#10B981", true),
        CategoryItem("cat_shopping", "", "Shopping", "shopping_bag", "#8B5CF6", true),
        CategoryItem("cat_office", "", "Office & Kaam", "business_center", "#64748B", true),
        CategoryItem("cat_health", "", "Dawai & Doctor", "medical_services", "#EF4444", true),
        CategoryItem("cat_other", "", "Anya Kharcha", "more_horiz", "#6B7280", true)
    )

    fun seedDefaultCategories(userId: String) {
        repoScope.launch {
            defaultCategories.forEach { cat ->
                dao.insertCategory(CategoryEntity.fromDomain(cat.copy(userId = userId)))
            }
        }
    }

    // EXPENSES
    fun getExpenses(userId: String): Flow<List<Expense>> {
        return dao.getAllExpenses(userId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addExpense(
        expense: Expense,
        sheetsUrl: String = "",
        autoSyncSheets: Boolean = true,
        googleAccessToken: String = "",
        sheetsSpreadsheetId: String = "",
        sheetsWorksheetName: String = "KHARCHA"
    ) {
        // 1. Immediately write to Room local DB (instant, zero delay, offline-first)
        val entity = ExpenseEntity.fromDomain(expense.copy(syncState = SyncState.PENDING_INSERT))
        dao.insertExpense(entity)

        // 2. If associated with a Pot, deduct from Pot
        if (!expense.potId.isNullOrBlank()) {
            val pot = dao.getPotById(expense.potId)
            if (pot != null) {
                val updatedPot = pot.copy(
                    currentBalance = (pot.currentBalance - expense.amount).coerceAtLeast(0.0),
                    totalSpent = pot.totalSpent + expense.amount
                )
                dao.insertPot(updatedPot)
                repoScope.launch { firestoreSync.savePotToCloud(updatedPot.toDomain()) }
            }
        }

        // 3. If associated with Udhaar party, update party ledger
        if (!expense.udhaarPersonId.isNullOrBlank()) {
            val party = dao.getUdhaarPartyById(expense.udhaarPersonId)
            if (party != null) {
                val updatedParty = party.copy(totalGiven = party.totalGiven + expense.amount)
                dao.insertUdhaarParty(updatedParty)
                val entry = UdhaarEntry(
                    id = "ud_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                    partyId = expense.udhaarPersonId,
                    userId = expense.userId,
                    amount = expense.amount,
                    isRepayment = false,
                    dateMillis = expense.dateMillis,
                    note = expense.note
                )
                dao.insertUdhaarEntry(UdhaarEntryEntity.fromDomain(entry))
                repoScope.launch {
                    firestoreSync.saveUdhaarPartyToCloud(updatedParty.toDomain())
                    firestoreSync.saveUdhaarEntryToCloud(entry)
                }
            }
        }

        // 4. Asynchronously sync to Firestore and Sheets mirror
        // A failure in Google Sheets or Excel must NEVER cause loss of the original expense
        repoScope.launch {
            try {
                firestoreSync.syncPendingExpenses(expense.userId, dao)
                if (autoSyncSheets) {
                    if (googleAccessToken.isNotBlank() && sheetsSpreadsheetId.isNotBlank()) {
                        val pending = dao.getPendingSheetsExpenses(expense.userId)
                        if (pending.isNotEmpty()) {
                            val res = driveService.appendExpensesToGoogleSheet(
                                googleAccessToken,
                                sheetsSpreadsheetId,
                                sheetsWorksheetName,
                                pending
                            )
                            if (res.isSuccess) {
                                pending.forEach { dao.markExpenseSheetsSynced(it.id) }
                            }
                        }
                    } else if (sheetsUrl.isNotBlank()) {
                        sheetsMirror.mirrorPendingExpenses(expense.userId, sheetsUrl, dao)
                    }
                }
            } catch (e: Exception) {
                Log.e("KharchaRepo", "Background sync non-blocking note: ${e.message}")
            }
        }
    }

    suspend fun deleteExpense(id: String, userId: String) {
        dao.markExpensePendingDelete(id)
        repoScope.launch {
            firestoreSync.syncPendingExpenses(userId, dao)
        }
    }

    suspend fun updateExpense(expense: Expense) {
        val entity = ExpenseEntity.fromDomain(expense.copy(syncState = SyncState.PENDING_UPDATE))
        dao.updateExpense(entity)
        repoScope.launch {
            firestoreSync.syncPendingExpenses(expense.userId, dao)
        }
    }

    // CATEGORIES
    fun getCategories(userId: String): Flow<List<CategoryItem>> {
        return dao.getAllCategories(userId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addCategory(category: CategoryItem) {
        dao.insertCategory(CategoryEntity.fromDomain(category))
    }

    suspend fun deleteCategory(id: String) {
        dao.deleteCategoryById(id)
    }

    // BUSINESSES
    fun getBusinesses(userId: String): Flow<List<Business>> {
        return dao.getAllBusinesses(userId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addBusiness(business: Business) {
        dao.insertBusiness(BusinessEntity.fromDomain(business))
        repoScope.launch { firestoreSync.saveBusinessToCloud(business) }
    }

    suspend fun deleteBusiness(id: String) {
        dao.deleteBusinessById(id)
    }

    // POTS
    fun getPots(userId: String): Flow<List<Pot>> {
        return dao.getAllPots(userId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addPot(pot: Pot) {
        dao.insertPot(PotEntity.fromDomain(pot))
        repoScope.launch { firestoreSync.savePotToCloud(pot) }
    }

    suspend fun depositToPot(potId: String, amount: Double) {
        val pot = dao.getPotById(potId) ?: return
        val updated = pot.copy(
            currentBalance = pot.currentBalance + amount,
            totalDeposited = pot.totalDeposited + amount
        )
        dao.insertPot(updated)
        repoScope.launch { firestoreSync.savePotToCloud(updated.toDomain()) }
    }

    suspend fun deletePot(id: String) {
        dao.deletePotById(id)
    }

    // UDHAAR
    fun getUdhaarParties(userId: String): Flow<List<UdhaarParty>> {
        return dao.getAllUdhaarParties(userId).map { list -> list.map { it.toDomain() } }
    }

    fun getUdhaarEntries(partyId: String): Flow<List<UdhaarEntry>> {
        return dao.getEntriesForParty(partyId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addUdhaarParty(party: UdhaarParty) {
        dao.insertUdhaarParty(UdhaarPartyEntity.fromDomain(party))
        repoScope.launch { firestoreSync.saveUdhaarPartyToCloud(party) }
    }

    suspend fun recordUdhaarTransaction(partyId: String, amount: Double, isRepayment: Boolean, note: String, userId: String) {
        val party = dao.getUdhaarPartyById(partyId) ?: return
        val updatedParty = if (isRepayment) {
            party.copy(totalReceived = party.totalReceived + amount)
        } else {
            party.copy(totalGiven = party.totalGiven + amount)
        }
        dao.insertUdhaarParty(updatedParty)

        val entry = UdhaarEntry(
            id = "ud_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
            partyId = partyId,
            userId = userId,
            amount = amount,
            isRepayment = isRepayment,
            dateMillis = System.currentTimeMillis(),
            note = note
        )
        dao.insertUdhaarEntry(UdhaarEntryEntity.fromDomain(entry))

        repoScope.launch {
            firestoreSync.saveUdhaarPartyToCloud(updatedParty.toDomain())
            firestoreSync.saveUdhaarEntryToCloud(entry)
        }
    }

    suspend fun deleteUdhaarParty(id: String) {
        dao.deleteUdhaarPartyById(id)
    }

    // BUDGETS
    fun getBudgets(userId: String): Flow<List<CategoryBudget>> {
        return dao.getAllBudgets(userId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun saveBudget(budget: CategoryBudget) {
        dao.insertBudget(CategoryBudgetEntity.fromDomain(budget))
    }

    suspend fun deleteBudget(categoryName: String, userId: String) {
        dao.deleteBudget(categoryName, userId)
    }

    // SYNC OPERATIONS
    suspend fun syncAllWithCloud(userId: String): Result<String> {
        return try {
            val pushResult = firestoreSync.syncPendingExpenses(userId, dao)
            val pullResult = firestoreSync.pullRemoteExpenses(userId, dao)
            firestoreSync.syncPots(userId, dao)
            firestoreSync.syncBusinesses(userId, dao)
            firestoreSync.syncUdhaar(userId, dao)

            val pushed = pushResult.getOrDefault(0)
            val pulled = pullResult.getOrDefault(0)
            Result.success("Sync safal raha! Cloud se $pulled kharche mile, $pushed bheje gaye.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncGoogleSheets(userId: String, webhookUrl: String): Result<Int> {
        return sheetsMirror.mirrorPendingExpenses(userId, webhookUrl, dao)
    }

    suspend fun syncGoogleSheetsDirect(
        userId: String,
        accessToken: String,
        spreadsheetId: String,
        sheetName: String = "KHARCHA"
    ): Result<Int> {
        val pending = dao.getPendingSheetsExpenses(userId)
        if (pending.isEmpty()) return Result.success(0)
        val res = driveService.appendExpensesToGoogleSheet(accessToken, spreadsheetId, sheetName, pending)
        if (res.isSuccess) {
            pending.forEach { dao.markExpenseSheetsSynced(it.id) }
        }
        return res
    }

    suspend fun listGoogleDriveFiles(accessToken: String): Result<List<DriveFileItem>> {
        return driveService.listDriveFiles(accessToken)
    }

    suspend fun createKharchaWorkbookInDrive(accessToken: String, fileName: String): Result<DriveFileItem> {
        return driveService.createKharchaWorkbookInDrive(accessToken, fileName)
    }

    suspend fun syncExcel(userId: String): Result<ExcelSyncResult> {
        return excelSync.performExcelSync(userId, dao)
    }

    suspend fun syncExcelWithGoogleDrive(
        userId: String,
        accessToken: String,
        fileId: String,
        sheetName: String = "KHARCHA"
    ): Result<ExcelSyncResult> {
        val pending = dao.getPendingExcelExpenses(userId)
        if (pending.isEmpty()) {
            return Result.success(
                ExcelSyncResult(
                    syncedCount = 0,
                    totalPending = 0,
                    message = "Sabhi transactions already Excel me synced hain."
                )
            )
        }

        if (accessToken.isNotBlank() && fileId.isNotBlank()) {
            val driveRes = driveService.syncExpensesToDriveExcel(accessToken, fileId, sheetName, pending)
            if (driveRes.isSuccess) {
                pending.forEach { dao.markExpenseExcelSynced(it.id) }
                // Also write to local backup workbook
                excelSync.performExcelSync(userId, dao)
                return Result.success(
                    ExcelSyncResult(
                        syncedCount = pending.size,
                        totalPending = 0,
                        message = "${pending.size} transactions Google Drive Excel workbook me jod diye gaye."
                    )
                )
            } else {
                Log.w("KharchaRepo", "Drive sync fallback: ${driveRes.exceptionOrNull()?.message}")
            }
        }

        return excelSync.performExcelSync(userId, dao)
    }

    suspend fun getPendingExcelCount(userId: String): Int {
        return dao.getPendingExcelExpenses(userId).size
    }

    suspend fun getPendingSheetsCount(userId: String): Int {
        return dao.getPendingSheetsExpenses(userId).size
    }
}
