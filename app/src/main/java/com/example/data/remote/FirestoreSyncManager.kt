package com.example.data.remote

import android.util.Log
import com.example.data.local.ExpenseEntity
import com.example.data.local.KharchaDao
import com.example.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val tag = "FirestoreSyncManager"

    suspend fun syncPendingExpenses(userId: String, dao: KharchaDao): Result<Int> {
        if (userId.isBlank() || userId == "guest_user") return Result.success(0)
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            ?: return Result.success(0)
        if (authUser.uid != userId) return Result.success(0)

        return try {
            val pendingExpenses = dao.getUnsyncedExpenses(userId)
            var syncedCount = 0

            for (entity in pendingExpenses) {
                val docRef = firestore.collection("users")
                    .document(userId)
                    .collection("expenses")
                    .document(entity.id)

                if (entity.syncState == SyncState.PENDING_DELETE.name) {
                    docRef.delete().await()
                    dao.deleteExpenseById(entity.id)
                } else {
                    val map = hashMapOf(
                        "id" to entity.id,
                        "userId" to entity.userId,
                        "amount" to entity.amount,
                        "dateMillis" to entity.dateMillis,
                        "category" to entity.category,
                        "note" to entity.note,
                        "paymentMethod" to entity.paymentMethod,
                        "contextType" to entity.contextType,
                        "businessId" to (entity.businessId ?: ""),
                        "businessName" to (entity.businessName ?: ""),
                        "potId" to (entity.potId ?: ""),
                        "potName" to (entity.potName ?: ""),
                        "udhaarPersonId" to (entity.udhaarPersonId ?: ""),
                        "udhaarPersonName" to (entity.udhaarPersonName ?: ""),
                        "receiptUrl" to (entity.receiptUrl ?: ""),
                        "sheetsSynced" to entity.sheetsSynced,
                        "excelSynced" to entity.excelSynced,
                        "createdAt" to entity.createdAt,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    docRef.set(map, SetOptions.merge()).await()
                    dao.updateExpense(entity.copy(syncState = SyncState.SYNCED.name))
                    syncedCount++
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e(tag, "Expense sync error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun pullRemoteExpenses(userId: String, dao: KharchaDao): Result<Int> {
        if (userId.isBlank() || userId == "guest_user") return Result.success(0)
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            ?: return Result.success(0)
        if (authUser.uid != userId) return Result.success(0)

        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .get()
                .await()

            val remoteEntities = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val amount = doc.getDouble("amount") ?: 0.0
                val dateMillis = doc.getLong("dateMillis") ?: System.currentTimeMillis()
                val category = doc.getString("category") ?: "Other"
                val note = doc.getString("note") ?: ""
                val paymentMethod = doc.getString("paymentMethod") ?: "UPI"
                val contextType = doc.getString("contextType") ?: "PERSONAL"
                val businessId = doc.getString("businessId").takeIf { !it.isNullOrBlank() }
                val businessName = doc.getString("businessName").takeIf { !it.isNullOrBlank() }
                val potId = doc.getString("potId").takeIf { !it.isNullOrBlank() }
                val potName = doc.getString("potName").takeIf { !it.isNullOrBlank() }
                val udhaarPersonId = doc.getString("udhaarPersonId").takeIf { !it.isNullOrBlank() }
                val udhaarPersonName = doc.getString("udhaarPersonName").takeIf { !it.isNullOrBlank() }
                val receiptUrl = doc.getString("receiptUrl").takeIf { !it.isNullOrBlank() }
                val createdAt = doc.getLong("createdAt") ?: dateMillis
                val sheetsSynced = doc.getBoolean("sheetsSynced") ?: false
                val excelSynced = doc.getBoolean("excelSynced") ?: false

                ExpenseEntity(
                    id = id,
                    userId = userId,
                    amount = amount,
                    dateMillis = dateMillis,
                    category = category,
                    note = note,
                    paymentMethod = paymentMethod,
                    contextType = contextType,
                    businessId = businessId,
                    businessName = businessName,
                    potId = potId,
                    potName = potName,
                    udhaarPersonId = udhaarPersonId,
                    udhaarPersonName = udhaarPersonName,
                    receiptUrl = receiptUrl,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis(),
                    syncState = SyncState.SYNCED.name,
                    sheetsSynced = sheetsSynced,
                    excelSynced = excelSynced
                )
            }

            dao.insertExpenses(remoteEntities)
            Result.success(remoteEntities.size)
        } catch (e: Exception) {
            Log.e(tag, "Pull remote expenses error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncPots(userId: String, dao: KharchaDao) {
        if (userId.isBlank() || userId == "guest_user" || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("pots")
                .get()
                .await()

            for (doc in snapshot.documents) {
                val pot = Pot(
                    id = doc.getString("id") ?: doc.id,
                    userId = userId,
                    name = doc.getString("name") ?: "Gullak",
                    targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                    currentBalance = doc.getDouble("currentBalance") ?: 0.0,
                    totalDeposited = doc.getDouble("totalDeposited") ?: 0.0,
                    totalSpent = doc.getDouble("totalSpent") ?: 0.0,
                    note = doc.getString("note") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    syncState = SyncState.SYNCED
                )
                dao.insertPot(com.example.data.local.PotEntity.fromDomain(pot))
            }
        } catch (e: Exception) {
            Log.w(tag, "Sync pots note: ${e.message}")
        }
    }

    suspend fun savePotToCloud(pot: Pot) {
        if (pot.userId.isBlank() || pot.userId == "guest_user" || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        try {
            firestore.collection("users")
                .document(pot.userId)
                .collection("pots")
                .document(pot.id)
                .set(
                    hashMapOf(
                        "id" to pot.id,
                        "userId" to pot.userId,
                        "name" to pot.name,
                        "targetAmount" to pot.targetAmount,
                        "currentBalance" to pot.currentBalance,
                        "totalDeposited" to pot.totalDeposited,
                        "totalSpent" to pot.totalSpent,
                        "note" to pot.note,
                        "createdAt" to pot.createdAt
                    ),
                    SetOptions.merge()
                ).await()
        } catch (e: Exception) {
            Log.w(tag, "Save pot note: ${e.message}")
        }
    }

    suspend fun syncBusinesses(userId: String, dao: KharchaDao) {
        if (userId.isBlank() || userId == "guest_user" || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("businesses")
                .get()
                .await()

            for (doc in snapshot.documents) {
                val biz = Business(
                    id = doc.getString("id") ?: doc.id,
                    userId = userId,
                    name = doc.getString("name") ?: "",
                    gstOrRegNo = doc.getString("gstOrRegNo") ?: "",
                    note = doc.getString("note") ?: "",
                    colorHex = doc.getString("colorHex") ?: "#3B82F6",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    syncState = SyncState.SYNCED
                )
                dao.insertBusiness(com.example.data.local.BusinessEntity.fromDomain(biz))
            }
        } catch (e: Exception) {
            Log.e(tag, "Sync businesses error: ${e.message}")
        }
    }

    suspend fun saveBusinessToCloud(business: Business) {
        if (business.userId.isBlank() || business.userId == "guest_user" || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        try {
            firestore.collection("users")
                .document(business.userId)
                .collection("businesses")
                .document(business.id)
                .set(
                    hashMapOf(
                        "id" to business.id,
                        "userId" to business.userId,
                        "name" to business.name,
                        "gstOrRegNo" to business.gstOrRegNo,
                        "note" to business.note,
                        "colorHex" to business.colorHex,
                        "createdAt" to business.createdAt
                    ),
                    SetOptions.merge()
                ).await()
        } catch (e: Exception) {
            Log.w(tag, "Save business note: ${e.message}")
        }
    }

    suspend fun syncUdhaar(userId: String, dao: KharchaDao) {
        if (userId.isBlank() || userId == "guest_user" || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("udhaar_parties")
                .get()
                .await()

            for (doc in snapshot.documents) {
                val party = UdhaarParty(
                    id = doc.getString("id") ?: doc.id,
                    userId = userId,
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    note = doc.getString("note") ?: "",
                    totalGiven = doc.getDouble("totalGiven") ?: 0.0,
                    totalReceived = doc.getDouble("totalReceived") ?: 0.0,
                    expectedReturnDateMillis = doc.getLong("expectedReturnDateMillis"),
                    reminderEnabled = doc.getBoolean("reminderEnabled") ?: true,
                    reminderIntervalDays = doc.getLong("reminderIntervalDays")?.toInt() ?: 7,
                    lastReminderSentMillis = doc.getLong("lastReminderSentMillis") ?: 0L,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    syncState = SyncState.SYNCED
                )
                dao.insertUdhaarParty(com.example.data.local.UdhaarPartyEntity.fromDomain(party))
            }
        } catch (e: Exception) {
            Log.w(tag, "Sync udhaar note: ${e.message}")
        }
    }

    suspend fun saveUdhaarPartyToCloud(party: UdhaarParty) {
        if (party.userId.isBlank() || party.userId == "guest_user" || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        try {
            firestore.collection("users")
                .document(party.userId)
                .collection("udhaar_parties")
                .document(party.id)
                .set(
                    hashMapOf(
                        "id" to party.id,
                        "userId" to party.userId,
                        "name" to party.name,
                        "phone" to party.phone,
                        "note" to party.note,
                        "totalGiven" to party.totalGiven,
                        "totalReceived" to party.totalReceived,
                        "expectedReturnDateMillis" to (party.expectedReturnDateMillis ?: 0L),
                        "reminderEnabled" to party.reminderEnabled,
                        "reminderIntervalDays" to party.reminderIntervalDays,
                        "lastReminderSentMillis" to party.lastReminderSentMillis,
                        "createdAt" to party.createdAt
                    ),
                    SetOptions.merge()
                ).await()
        } catch (e: Exception) {
            Log.w(tag, "Save udhaar party note: ${e.message}")
        }
    }

    suspend fun saveUdhaarEntryToCloud(entry: UdhaarEntry) {
        if (entry.userId.isBlank() || entry.userId == "guest_user" || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        try {
            firestore.collection("users")
                .document(entry.userId)
                .collection("udhaar_entries")
                .document(entry.id)
                .set(
                    hashMapOf(
                        "id" to entry.id,
                        "partyId" to entry.partyId,
                        "userId" to entry.userId,
                        "amount" to entry.amount,
                        "isRepayment" to entry.isRepayment,
                        "dateMillis" to entry.dateMillis,
                        "note" to entry.note
                    ),
                    SetOptions.merge()
                ).await()
        } catch (e: Exception) {
            Log.e(tag, "Save udhaar entry error: ${e.message}")
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        if (userId.isBlank() || userId == "guest_user") return null
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return null
        if (authUser.uid != userId) return null

        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (!doc.exists()) return null
            UserProfile(
                uid = userId,
                email = doc.getString("email") ?: "",
                displayName = doc.getString("displayName") ?: "",
                role = doc.getString("role") ?: "user",
                isBudgetFeatureEnabled = doc.getBoolean("isBudgetFeatureEnabled") ?: false,
                sheetsUrl = doc.getString("sheetsUrl") ?: "",
                sheetsAutoSync = doc.getBoolean("sheetsAutoSync") ?: true,
                excelPreferredSyncTime = doc.getString("excelPreferredSyncTime") ?: "02:00",
                lastExcelSyncMillis = doc.getLong("lastExcelSyncMillis") ?: 0L,
                lastSheetsSyncMillis = doc.getLong("lastSheetsSyncMillis") ?: 0L,
                pinHash = doc.getString("pinHash") ?: "",
                isPinLockEnabled = doc.getBoolean("isPinLockEnabled") ?: false,
                googleAccountEmail = doc.getString("googleAccountEmail") ?: "",
                googleAccountName = doc.getString("googleAccountName") ?: "",
                googleAccessToken = doc.getString("googleAccessToken") ?: "",
                sheetsSpreadsheetId = doc.getString("sheetsSpreadsheetId") ?: "",
                sheetsSpreadsheetName = doc.getString("sheetsSpreadsheetName") ?: "",
                sheetsWorksheetName = doc.getString("sheetsWorksheetName") ?: "KHARCHA",
                excelWorkbookId = doc.getString("excelWorkbookId") ?: "",
                excelWorkbookName = doc.getString("excelWorkbookName") ?: "",
                excelWorksheetName = doc.getString("excelWorksheetName") ?: "KHARCHA"
            )
        } catch (e: Exception) {
            Log.w(tag, "Get user profile note: ${e.message}")
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        if (profile.uid.isBlank() || profile.uid == "guest_user") return
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        if (authUser.uid != profile.uid) return

        try {
            firestore.collection("users").document(profile.uid)
                .set(
                    hashMapOf(
                        "uid" to profile.uid,
                        "email" to profile.email,
                        "displayName" to profile.displayName,
                        "role" to profile.role,
                        "isBudgetFeatureEnabled" to profile.isBudgetFeatureEnabled,
                        "sheetsUrl" to profile.sheetsUrl,
                        "sheetsAutoSync" to profile.sheetsAutoSync,
                        "excelPreferredSyncTime" to profile.excelPreferredSyncTime,
                        "lastExcelSyncMillis" to profile.lastExcelSyncMillis,
                        "lastSheetsSyncMillis" to profile.lastSheetsSyncMillis,
                        "pinHash" to profile.pinHash,
                        "isPinLockEnabled" to profile.isPinLockEnabled,
                        "googleAccountEmail" to profile.googleAccountEmail,
                        "googleAccountName" to profile.googleAccountName,
                        "googleAccessToken" to profile.googleAccessToken,
                        "sheetsSpreadsheetId" to profile.sheetsSpreadsheetId,
                        "sheetsSpreadsheetName" to profile.sheetsSpreadsheetName,
                        "sheetsWorksheetName" to profile.sheetsWorksheetName,
                        "excelWorkbookId" to profile.excelWorkbookId,
                        "excelWorkbookName" to profile.excelWorkbookName,
                        "excelWorksheetName" to profile.excelWorksheetName,
                        "lastActiveMillis" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
        } catch (e: Exception) {
            Log.w(tag, "Save user profile note: ${e.message}")
        }
    }

    suspend fun markExpenseSheetsSyncedInFirestore(userId: String, expenseId: String) {
        try {
            firestore.collection("users").document(userId)
                .collection("expenses").document(expenseId)
                .update("sheetsSynced", true).await()
        } catch (e: Exception) {
            Log.e(tag, "Mark sheets synced in firestore error: ${e.message}")
        }
    }

    suspend fun markExpenseExcelSyncedInFirestore(userId: String, expenseId: String) {
        try {
            firestore.collection("users").document(userId)
                .collection("expenses").document(expenseId)
                .update("excelSynced", true).await()
        } catch (e: Exception) {
            Log.e(tag, "Mark excel synced in firestore error: ${e.message}")
        }
    }

    // Admin queries
    suspend fun getAllUsersForAdmin(): List<UserProfile> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: doc.id
                val email = doc.getString("email") ?: ""
                UserProfile(
                    uid = uid,
                    email = email,
                    displayName = doc.getString("displayName") ?: "",
                    role = doc.getString("role") ?: "user",
                    isBudgetFeatureEnabled = doc.getBoolean("isBudgetFeatureEnabled") ?: false,
                    sheetsUrl = doc.getString("sheetsUrl") ?: "",
                    sheetsAutoSync = doc.getBoolean("sheetsAutoSync") ?: true,
                    excelPreferredSyncTime = doc.getString("excelPreferredSyncTime") ?: "02:00",
                    lastExcelSyncMillis = doc.getLong("lastExcelSyncMillis") ?: 0L,
                    pinHash = "",
                    isPinLockEnabled = doc.getBoolean("isPinLockEnabled") ?: false
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
