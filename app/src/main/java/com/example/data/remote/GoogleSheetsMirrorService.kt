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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
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
        val cleanUrl = webhookUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Google Sheets Webhook URL set nahi hai"))
        }

        // Detect if user pasted the Google Sheet browser link instead of the Apps Script Web App URL
        if (cleanUrl.contains("docs.google.com/spreadsheets")) {
            val errorMsg = "Aapne Google Sheet ka browser link daala hai! Kripya Apps Script me 'Deploy -> Manage deployments' se Web App URL (script.google.com/macros/s/.../exec) copy karke daalein."
            Log.e(tag, errorMsg)
            return@withContext Result.failure(IllegalArgumentException(errorMsg))
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
                .url(cleanUrl)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val finalUrl = response.request.url.toString()

            // If redirected to Google login, it means "Who has access" was not set to "Anyone"
            if (finalUrl.contains("accounts.google.com")) {
                val errorMsg = "Google ne permission block ki! Apps Script me 'Deploy -> Manage deployments' par jayein aur 'Who has access' ko 'Anyone' karein, tabhi sync hoga."
                Log.e(tag, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Google Apps Script Web App responds with 200 or 302 redirect
            if (response.isSuccessful || code in 200..399) {
                for (item in pending) {
                    dao.markExpenseSheetsSynced(item.id)
                }
                Result.success(pending.size)
            } else {
                val responseBody = response.body?.string() ?: ""
                val errorMsg = if (code == 405 || code == 404) {
                    "Webhook URL invalid hai (code $code). Kripya check karein ki Apps Script me 'Who has access: Anyone' chuna hai aur 'Web App URL' use kiya hai."
                } else {
                    "Sheets sync response error: code $code. $responseBody"
                }
                Log.e(tag, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "Google Sheets Mirror Error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
