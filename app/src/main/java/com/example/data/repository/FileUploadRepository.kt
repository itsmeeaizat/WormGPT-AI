package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.Content
import com.example.data.model.AttachedFile
import com.example.util.AiVoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FileUploadRepository(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Mengirim file ke backend Node.js (/api/upload) secara multipart form-data.
     * Endpoint ini membalas secara instan HTTP 202 Accepted untuk Optimistic UI.
     */
    suspend fun uploadFileToBackend(
        serverUrl: String = "http://10.0.2.2:3000/api/upload",
        file: AttachedFile,
        clientId: String,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val mime = file.mimeType.ifBlank { "application/octet-stream" }
            val fileBytes = file.contentPayload.toByteArray(Charsets.UTF_8)
            val fileRequestBody = fileBytes.toRequestBody(mime.toMediaTypeOrNull())

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, fileRequestBody)
                .addFormDataPart("clientId", clientId)
                .addFormDataPart("prompt", prompt)
                .build()

            val request = Request.Builder()
                .url(serverUrl)
                .post(multipartBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful || response.code == 202) {
                val json = JSONObject(responseBody)
                val dataObj = json.optJSONObject("data")
                val fileId = dataObj?.optString("fileId") ?: "file_${System.currentTimeMillis()}"
                Result.success(fileId)
            } else {
                Result.failure(Exception("HTTP Upload Failed with status code: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.w("FileUploadRepo", "Backend upload error (Fallback to Direct Gemini): ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Fallback Direct Gemini AI Stream Processing jika server Node.js lokal tidak dapat dijangkau.
     */
    suspend fun streamDirectGeminiFileResponse(
        file: AttachedFile,
        userPrompt: String,
        apiKey: String,
        onChunkStream: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val formattedPrompt = buildString {
                appendLine("📎 [FILE ATTACHMENT: ${file.name} (${file.formattedSize})]")
                appendLine("Format: ${file.mimeType} | Extension: .${file.extension}")
                appendLine("--- ISIDOKUMEN/TEKS TERHUBUNG ---")
                appendLine(file.contentPayload)
                appendLine("--- AKHIR DOKUMEN ---")
                appendLine()
                if (userPrompt.isNotBlank()) {
                    appendLine("Instruksi User: $userPrompt")
                } else {
                    appendLine("Tolong analisa, jelaskan, dan berikan poin-poin penting dari file diatas.")
                }
            }

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(Part(text = formattedPrompt))
                    )
                )
            )

            val modelName = "gemini-2.5-flash"
            val response = GeminiClient.apiService.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )

            val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "File berhasil diproses, namun tidak ada teks respon dari Gemini."

            // Simulasi token streaming chunk untuk respon cepat
            val words = fullText.split(" ")
            val sb = StringBuilder()
            for (word in words) {
                val chunk = "$word "
                sb.append(chunk)
                onChunkStream(chunk)
                kotlinx.coroutines.delay(18)
            }

            Result.success(fullText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
