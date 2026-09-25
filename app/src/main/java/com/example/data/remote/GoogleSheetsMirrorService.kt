package com.example.data.remote

import android.util.Log
import com.example.data.local.KharchaDao
import com.example.data.model.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleSheetsMirrorService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "SheetsMirror"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    suspend fun mirrorPendingExpenses(
        userId: String,
        webhookUrl: String,
        dao: KharchaDao
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Google Sheets Webhook URL set nahi hai"))
        }

        try {
            val pending = dao.getPendingSheetsExpenses(userId)
            if (pending.isEmpty()) return@withContext Result.success(0)

            val rowsArray = JSONArray()
            for (item in pending) {
                val d = Date(item.dateMillis)
                val rowObj = JSONObject().apply {
                    put("transactionId", item.id)
                    put("date", dateFormat.format(d))
                    put("time", timeFormat.format(d))
                    put("amount", item.amount)
                    put("category", item.category)
                    put("context", item.contextType)
                    put("business", item.businessName ?: "-")
                    put("paymentMethod", item.paymentMethod)
                    put("udhaarPerson", item.udhaarPersonName ?: "-")
                    put("pot", item.potName ?: "-")
                    put("note", item.note)
                    put("createdAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.createdAt)))
                }
                rowsArray.put(rowObj)
            }

            val payload = JSONObject().apply {
                put("action", "appendExpenses")
                put("userId", userId)
                put("sheetName", "KHARCHA")
                put("expenses", rowsArray)
            }

            val request = Request.Builder()
                .url(webhookUrl)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                for (item in pending) {
                    dao.markExpenseSheetsSynced(item.id)
                }
                Result.success(pending.size)
            } else {
                val errorMsg = "Sheets sync response error: code ${response.code}"
                Log.e(tag, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "Google Sheets Mirror Error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
