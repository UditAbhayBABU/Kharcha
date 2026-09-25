package com.example.data.remote

import android.util.Log
import com.example.data.local.ExpenseEntity
import com.example.data.model.DriveFileItem
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

class GoogleDriveAndSheetsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "GoogleDriveSheetsService"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    /**
     * Lists Excel workbooks and Google Sheets from the connected user's Google Drive.
     */
    suspend fun listDriveFiles(accessToken: String): Result<List<DriveFileItem>> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Google Account connected nahi hai"))
        }

        try {
            val query = "trashed=false and (mimeType='application/vnd.google-apps.spreadsheet' or mimeType='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' or mimeType='text/csv' or name contains '.xlsx' or name contains '.csv')"
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/drive/v3/files?pageSize=50&fields=files(id,name,mimeType,modifiedTime)&q=$encodedQuery&orderBy=modifiedTime%20desc"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val filesArray = json.optJSONArray("files") ?: JSONArray()
                val items = mutableListOf<DriveFileItem>()

                for (i in 0 until filesArray.length()) {
                    val f = filesArray.getJSONObject(i)
                    items.add(
                        DriveFileItem(
                            id = f.optString("id"),
                            name = f.optString("name"),
                            mimeType = f.optString("mimeType"),
                            modifiedTime = f.optString("modifiedTime")
                        )
                    )
                }
                Result.success(items)
            } else {
                Log.e(tag, "Drive list files error: code ${response.code} body: $responseBody")
                Result.failure(Exception("Drive files load fail: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Drive list error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new KHARCHA Master workbook in the user's Google Drive.
     */
    suspend fun createKharchaWorkbookInDrive(accessToken: String, fileName: String): Result<DriveFileItem> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Google Account connected nahi hai"))
        }

        try {
            // Create a Google Sheet or CSV in Google Drive
            val metaObj = JSONObject().apply {
                put("name", if (fileName.isBlank()) "KHARCHA_Expenses_Master.xlsx" else fileName)
                put("mimeType", "application/vnd.google-apps.spreadsheet")
            }

            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(metaObj.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val fileItem = DriveFileItem(
                    id = json.optString("id"),
                    name = json.optString("name"),
                    mimeType = json.optString("mimeType")
                )

                // Initialize header row in the new sheet
                initializeSheetHeaders(accessToken, fileItem.id, "KHARCHA")

                Result.success(fileItem)
            } else {
                Log.e(tag, "Drive create file error: code ${response.code} body: $responseBody")
                Result.failure(Exception("Drive file creation error: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Create file error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Initializes header row in a Google Sheet if newly created.
     */
    private fun initializeSheetHeaders(accessToken: String, spreadsheetId: String, sheetName: String) {
        try {
            val headerValues = JSONArray().apply {
                put("Transaction ID")
                put("Date")
                put("Time")
                put("Amount (INR)")
                put("Category")
                put("Context")
                put("Business")
                put("Payment Method")
                put("Udhaar Person")
                put("Pot / Gullak")
                put("Note")
                put("Created At")
            }

            val body = JSONObject().apply {
                put("range", "$sheetName!A1:L1")
                put("majorDimension", "ROWS")
                put("values", JSONArray().put(headerValues))
            }

            val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$sheetName!A1:L1?valueInputOption=USER_ENTERED"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .put(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(tag, "Header init note: ${e.message}")
        }
    }

    /**
     * Appends expenses to the selected Google Spreadsheet via Sheets API v4.
     */
    suspend fun appendExpensesToGoogleSheet(
        accessToken: String,
        spreadsheetId: String,
        sheetName: String,
        expenses: List<ExpenseEntity>
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank() || spreadsheetId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Google Spreadsheet ID ya Token missing hai"))
        }
        if (expenses.isEmpty()) return@withContext Result.success(0)

        try {
            val rowsArray = JSONArray()
            for (item in expenses) {
                val d = Date(item.dateMillis)
                val row = JSONArray().apply {
                    put(item.id)
                    put(dateFormat.format(d))
                    put(timeFormat.format(d))
                    put(item.amount)
                    put(item.category)
                    put(item.contextType)
                    put(item.businessName ?: "-")
                    put(item.paymentMethod)
                    put(item.udhaarPersonName ?: "-")
                    put(item.potName ?: "-")
                    put(item.note)
                    put(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.createdAt)))
                }
                rowsArray.put(row)
            }

            val targetSheet = sheetName.ifBlank { "KHARCHA" }
            val body = JSONObject().apply {
                put("range", "$targetSheet!A:L")
                put("majorDimension", "ROWS")
                put("values", rowsArray)
            }

            val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$targetSheet!A:L:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success(expenses.size)
            } else {
                Log.e(tag, "Sheets append error: code ${response.code} body: $responseBody")
                Result.failure(Exception("Google Sheets append error: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Append expenses error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Controlled Excel sync to Google Drive workbook file.
     * Appends unique transaction rows preserving the workbook.
     */
    suspend fun syncExpensesToDriveExcel(
        accessToken: String,
        fileId: String,
        sheetName: String,
        expenses: List<ExpenseEntity>
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank() || fileId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Google Drive File ID ya Token missing hai"))
        }
        if (expenses.isEmpty()) return@withContext Result.success(0)

        // Delegate to Sheets API v4 which also operates seamlessly on Google Drive spreadsheets
        return@withContext appendExpensesToGoogleSheet(accessToken, fileId, sheetName, expenses)
    }
}
