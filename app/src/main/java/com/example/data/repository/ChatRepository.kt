package com.example.data.repository

import com.example.BuildConfig
import com.example.DEFAULT_CONFIG
import com.example.data.api.Content
import com.example.data.api.GeminiClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.db.ChatDao
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.model.AttachedFile
import com.example.data.model.ChatPersona
import com.example.data.model.CustomAiModel
import com.example.data.model.WormMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class ApiException(val statusCode: Int?, val responseBody: String) : Exception(responseBody)

class ChatRepository(private val chatDao: ChatDao) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(mode: WormMode, initialTitle: String = "New Cyber Session"): String {
        val sessionId = UUID.randomUUID().toString()
        val session = ChatSessionEntity(
            id = sessionId,
            title = initialTitle,
            modeName = mode.id,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
        return sessionId
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun saveMessage(
        sessionId: String,
        sender: String,
        content: String,
        modeTag: String,
        isError: Boolean = false
    ): Long {
        val msg = ChatMessageEntity(
            sessionId = sessionId,
            sender = sender,
            content = content,
            modeTag = modeTag,
            timestamp = System.currentTimeMillis(),
            isError = isError
        )
        val id = chatDao.insertMessage(msg)
        
        // Auto update title if first message
        val session = chatDao.getSessionById(sessionId)
        if (session != null && session.title == "New Cyber Session" && sender == "USER") {
            val titleSnippet = if (content.length > 28) content.take(28) + "..." else content
            chatDao.updateSessionTitleAndTimestamp(sessionId, titleSnippet, System.currentTimeMillis())
        } else {
            chatDao.updateSessionTitleAndTimestamp(sessionId, session?.title ?: "Cyber Session", System.currentTimeMillis())
        }

        return id
    }

    suspend fun sendToAiModel(
        userPrompt: String,
        conversationHistory: List<ChatMessageEntity>,
        mode: WormMode,
        geminiApiKey: String? = null,
        groqApiKey: String? = null,
        openRouterApiKey: String? = null,
        mistralApiKey: String? = null,
        selectedModel: String = "gemini-3.5-flash",
        customModels: List<CustomAiModel> = emptyList(),
        attachedFile: AttachedFile? = null,
        persona: ChatPersona? = null,
        onChunkStream: ((String) -> Unit)? = null
    ): Result<String> {
        val personaPrompt = persona?.systemPromptInstruction?.let { "\n\n$it" }.orEmpty()
        val combinedSystemPrompt = mode.systemPrompt + personaPrompt

        val customModel = customModels.find { it.id == selectedModel }
        if (customModel != null) {
            val providerLower = customModel.providerType.lowercase()
            val resolvedKey = customModel.apiKey.trim().ifBlank {
                when {
                    providerLower == "groq" || customModel.id.startsWith("groq/") ->
                        groqApiKey?.trim().takeIf { !it.isNullOrBlank() } ?: DEFAULT_CONFIG.DEFAULT_GROQ_API_KEY.trim()
                    providerLower == "openrouter" || customModel.id.startsWith("openrouter/") ->
                        openRouterApiKey?.trim().takeIf { !it.isNullOrBlank() } ?: DEFAULT_CONFIG.DEFAULT_OPENROUTER_API_KEY.trim()
                    providerLower == "mistral" || customModel.id.startsWith("mistral/") ->
                        mistralApiKey?.trim().takeIf { !it.isNullOrBlank() } ?: DEFAULT_CONFIG.DEFAULT_MISTRAL_API_KEY.trim()
                    providerLower == "gemini" || customModel.id.startsWith("gemini/") ->
                        geminiApiKey?.trim().takeIf { !it.isNullOrBlank() } ?: DEFAULT_CONFIG.DEFAULT_GEMINI_API_KEY.trim()
                    else -> ""
                }
            }

            val realModelId = customModel.id
                .removePrefix("groq/")
                .removePrefix("openrouter/")
                .removePrefix("mistral/")
                .removePrefix("gemini/")

            if (providerLower == "gemini") {
                return sendToGemini(userPrompt, conversationHistory, mode, resolvedKey, realModelId, attachedFile, persona, onChunkStream)
            }

            val resolvedBaseUrl = customModel.baseUrl.trim().ifBlank {
                when {
                    providerLower == "groq" || customModel.id.startsWith("groq/") -> "https://api.groq.com/openai/v1/chat/completions"
                    providerLower == "openrouter" || customModel.id.startsWith("openrouter/") -> "https://openrouter.ai/api/v1/chat/completions"
                    providerLower == "mistral" || customModel.id.startsWith("mistral/") -> "https://api.mistral.ai/v1/chat/completions"
                    else -> "https://api.openai.com/v1/chat/completions"
                }
            }

            if (resolvedKey.isBlank() && !resolvedBaseUrl.contains("pollinations")) {
                return Result.failure(Exception("API Key untuk model kustom '${customModel.name}' belum dimasukkan."))
            }

            return sendToOpenAiCompatibleApi(resolvedBaseUrl, resolvedKey, realModelId, combinedSystemPrompt, conversationHistory, userPrompt, onChunkStream)
        }

        return when {
            selectedModel.startsWith("pollinations/") -> {
                val realModel = selectedModel.removePrefix("pollinations/")
                sendToPollinationsApi(realModel, combinedSystemPrompt, userPrompt, onChunkStream)
            }
            selectedModel.startsWith("groq/") -> {
                val realModel = selectedModel.removePrefix("groq/")
                val apiKey = groqApiKey?.trim().takeIf { !it.isNullOrBlank() }
                    ?: DEFAULT_CONFIG.DEFAULT_GROQ_API_KEY.trim()
                if (apiKey.isBlank()) {
                    Result.failure(Exception("Groq API key belum dimasukkan. Masukkan Groq Key di Settings atau di DEFAULT_CONFIG."))
                } else {
                    sendToOpenAiCompatibleApi(
                        baseUrl = "https://api.groq.com/openai/v1/chat/completions",
                        apiKey = apiKey,
                        modelName = realModel,
                        systemPrompt = combinedSystemPrompt,
                        conversationHistory = conversationHistory,
                        userPrompt = userPrompt,
                        onChunkStream = onChunkStream
                    )
                }
            }
            selectedModel.startsWith("openrouter/") -> {
                val realModel = selectedModel.removePrefix("openrouter/")
                val apiKey = openRouterApiKey?.trim().takeIf { !it.isNullOrBlank() }
                    ?: DEFAULT_CONFIG.DEFAULT_OPENROUTER_API_KEY.trim()
                if (apiKey.isBlank()) {
                    Result.failure(Exception("OpenRouter API key belum dimasukkan. Masukkan OpenRouter Key di Settings atau di DEFAULT_CONFIG."))
                } else {
                    sendToOpenAiCompatibleApi(
                        baseUrl = "https://openrouter.ai/api/v1/chat/completions",
                        apiKey = apiKey,
                        modelName = realModel,
                        systemPrompt = combinedSystemPrompt,
                        conversationHistory = conversationHistory,
                        userPrompt = userPrompt,
                        onChunkStream = onChunkStream
                    )
                }
            }
            selectedModel.startsWith("mistral/") -> {
                val realModel = selectedModel.removePrefix("mistral/")
                val apiKey = mistralApiKey?.trim().takeIf { !it.isNullOrBlank() }
                    ?: DEFAULT_CONFIG.DEFAULT_MISTRAL_API_KEY.trim()
                if (apiKey.isBlank()) {
                    Result.failure(Exception("Mistral API key belum dimasukkan. Masukkan Mistral Key di Settings atau di DEFAULT_CONFIG."))
                } else {
                    sendToOpenAiCompatibleApi(
                        baseUrl = "https://api.mistral.ai/v1/chat/completions",
                        apiKey = apiKey,
                        modelName = realModel,
                        systemPrompt = combinedSystemPrompt,
                        conversationHistory = conversationHistory,
                        userPrompt = userPrompt,
                        onChunkStream = onChunkStream
                    )
                }
            }
            else -> {
                // Default Gemini API handler
                val cleanModel = selectedModel.removePrefix("gemini/")
                sendToGemini(userPrompt, conversationHistory, mode, geminiApiKey, cleanModel, attachedFile, persona, onChunkStream)
            }
        }
    }

    suspend fun sendToGemini(
        userPrompt: String,
        conversationHistory: List<ChatMessageEntity>,
        mode: WormMode,
        customApiKey: String? = null,
        selectedModel: String = "gemini-3.5-flash",
        attachedFile: AttachedFile? = null,
        persona: ChatPersona? = null,
        onChunkStream: ((String) -> Unit)? = null
    ): Result<String> {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            DEFAULT_CONFIG.DEFAULT_GEMINI_API_KEY.isNotBlank() -> DEFAULT_CONFIG.DEFAULT_GEMINI_API_KEY.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API key is not configured. Please set your key in the Secrets Panel or Settings menu."))
        }

        val contents = mutableListOf<Content>()
        
        // Filter out system welcome messages and errors
        val validHistory = conversationHistory.filter { msg ->
            !msg.content.startsWith("[SYS_OVERRIDE_ACTIVE]") && !msg.isError && msg.content.isNotBlank()
        }

        var lastRole: String? = null
        validHistory.forEach { msg ->
            val role = if (msg.sender == "USER") "user" else "model"
            // Ensure first item in contents is role = "user", and roles strictly alternate
            if (contents.isEmpty() && role == "model") {
                return@forEach
            }
            if (role != lastRole) {
                contents.add(
                    Content(
                        role = role,
                        parts = listOf(Part(text = msg.content))
                    )
                )
                lastRole = role
            }
        }

        // Add current user prompt (incorporating Google File API / InlineData if attachedFile is present)
        val userParts = mutableListOf<Part>()
        if (attachedFile?.base64Data != null) {
            val fileMime = attachedFile.mimeType.ifBlank { "text/plain" }
            userParts.add(Part(inlineData = InlineData(mimeType = fileMime, data = attachedFile.base64Data)))
        }
        userParts.add(Part(text = userPrompt))

        contents.add(
            Content(
                role = "user",
                parts = userParts
            )
        )

        val fileInstruction = if (userPrompt.contains("📎 [ATTACHMENT:")) {
            "\n\n[SMART FILE CONTENT READER INSTRUCTION]\nAn external file or source code document is attached to the query.\n1. Inspect and analyze the attached code/document contents thoroughly.\n2. Identify key functions, exports, routes, variables, and architectural patterns.\n3. Answer the user's specific question directly with clear markdown headings and code blocks."
        } else {
            ""
        }

        val personaInstruction = persona?.systemPromptInstruction?.let { "\n\n$it" }.orEmpty()
        val noGreetingInstruction = "\n\n[RULE: ABSOLUTELY NO AUTOMATIC GREETINGS]\nDo NOT start your response with any automatic greetings or canned phrases like 'active and ready', 'I am ready', 'system initialized', 'halo', or similar disclaimers. Respond directly and purely using your character's natural persona and voice."

        val systemInstructionContent = Content(
            parts = listOf(Part(text = mode.systemPrompt + personaInstruction + noGreetingInstruction + fileInstruction))
        )

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = systemInstructionContent,
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                topP = 0.95f,
                topK = 40,
                maxOutputTokens = 4096
            )
        )

        return executeWithRetry(modelName = selectedModel) {
            try {
                val response = GeminiClient.apiService.generateContent(
                    model = selectedModel,
                    apiKey = apiKey,
                    request = request
                )

                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    val cleaned = cleanAiResponse(text)
                    onChunkStream?.invoke(cleaned)
                    Result.success(cleaned)
                } else if (response.error != null) {
                    Result.failure(ApiException(response.error.code, response.error.message ?: "Unknown Gemini API error"))
                } else {
                    Result.failure(ApiException(null, "Respon kosong atau difilter oleh sistem keamanan AI."))
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string().orEmpty()
                val parsedMsg = parseJsonErrorMessage(errorBody)
                Result.failure(ApiException(e.code(), parsedMsg.ifBlank { errorBody.ifBlank { e.message() } }))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun sendToOpenAiCompatibleApi(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        systemPrompt: String,
        conversationHistory: List<ChatMessageEntity>,
        userPrompt: String,
        onChunkStream: ((String) -> Unit)? = null
    ): Result<String> = executeWithRetry(modelName = modelName) {
        withContext(Dispatchers.IO) {
            try {
                val fileInstruction = if (userPrompt.contains("📎 [ATTACHMENT:")) {
                    "\n\n[SMART FILE CONTENT READER INSTRUCTION]\nAn external file or source code document is attached to the query.\n1. Inspect and analyze the attached code/document contents thoroughly.\n2. Identify key functions, exports, routes, variables, and architectural patterns.\n3. Answer the user's specific question directly with clear markdown headings and code blocks."
                } else {
                    ""
                }

                val noGreetingInstruction = "\n\n[RULE: ABSOLUTELY NO AUTOMATIC GREETINGS]\nDo NOT start your response with any automatic greetings or canned phrases like 'active and ready', 'I am ready', 'system initialized', 'halo', or similar disclaimers. Respond directly and purely using your character's natural persona and voice."

                val messagesArray = JSONArray()
                
                // System prompt
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt + noGreetingInstruction + fileInstruction)
                })

                // Conversation history
                conversationHistory.filter { !it.content.startsWith("[SYS_OVERRIDE_ACTIVE]") && !it.isError && it.content.isNotBlank() }
                    .forEach { msg ->
                        messagesArray.put(JSONObject().apply {
                            put("role", if (msg.sender == "USER") "user" else "assistant")
                            put("content", msg.content)
                        })
                    }

                // Current prompt
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })

                val jsonBody = JSONObject().apply {
                    put("model", modelName)
                    put("messages", messagesArray)
                    put("temperature", 0.7)
                    put("max_tokens", 4096)
                    if (onChunkStream != null) {
                        put("stream", true)
                    }
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(baseUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://github.com/mnfst/awesome-free-llm-apis")
                    .addHeader("X-Title", "WormGPT Mobile")
                    .post(requestBody)
                    .build()

                if (onChunkStream != null) {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val bodyString = response.body?.string().orEmpty()
                            val parsedMsg = parseJsonErrorMessage(bodyString)
                            return@withContext Result.failure(ApiException(response.code, parsedMsg.ifBlank { bodyString }))
                        }
                        val source = response.body?.source()
                        val fullSb = StringBuilder()
                        if (source != null) {
                            while (!source.exhausted()) {
                                val line = source.readUtf8Line() ?: break
                                if (line.startsWith("data: ")) {
                                    val dataStr = line.removePrefix("data: ").trim()
                                    if (dataStr == "[DONE]") break
                                    try {
                                        val json = JSONObject(dataStr)
                                        val choices = json.optJSONArray("choices")
                                        if (choices != null && choices.length() > 0) {
                                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                                            val contentChunk = delta?.optString("content")
                                            if (!contentChunk.isNullOrEmpty()) {
                                                fullSb.append(contentChunk)
                                                onChunkStream.invoke(contentChunk)
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                        val cleanText = cleanAiResponse(fullSb.toString())
                        if (cleanText.isNotBlank()) {
                            return@withContext Result.success(cleanText)
                        } else {
                            return@withContext Result.failure(ApiException(response.code, "Respon model kosong dari $modelName."))
                        }
                    }
                } else {
                    okHttpClient.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string().orEmpty()
                        if (response.isSuccessful && bodyString.isNotBlank()) {
                            val json = JSONObject(bodyString)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val choice = choices.getJSONObject(0)
                                val messageObj = choice.optJSONObject("message")
                                val content = messageObj?.optString("content")
                                if (!content.isNullOrBlank()) {
                                    return@withContext Result.success(cleanAiResponse(content))
                                }
                            }
                            Result.failure(ApiException(response.code, "Respon model kosong dari $modelName."))
                        } else {
                            val parsedMsg = parseJsonErrorMessage(bodyString)
                            Result.failure(ApiException(response.code, parsedMsg.ifBlank { bodyString }))
                        }
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun sendToPollinationsApi(
        modelName: String,
        systemPrompt: String,
        userPrompt: String,
        onChunkStream: ((String) -> Unit)? = null
    ): Result<String> = executeWithRetry(modelName = modelName) {
        withContext(Dispatchers.IO) {
            try {
                val messagesArray = JSONArray()
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })

                val jsonBody = JSONObject().apply {
                    put("messages", messagesArray)
                    put("model", modelName)
                    put("json", false)
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://text.pollinations.ai/")
                    .post(requestBody)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()
                    if (response.isSuccessful && bodyString.isNotBlank()) {
                        onChunkStream?.invoke(bodyString)
                        Result.success(bodyString)
                    } else {
                        val parsedMsg = parseJsonErrorMessage(bodyString)
                        Result.failure(ApiException(response.code, parsedMsg.ifBlank { bodyString }))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun executeWithRetry(
        modelName: String,
        maxAttempts: Int = 3,
        initialDelayMs: Long = 3000L,
        block: suspend () -> Result<String>
    ): Result<String> {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null

        for (attempt in 1..maxAttempts) {
            val result = block()
            if (result.isSuccess) {
                return result
            }

            val exception = result.exceptionOrNull() ?: Exception("Unknown API error")
            lastException = exception

            val statusCode = (exception as? ApiException)?.statusCode
            val errMsg = exception.message.orEmpty()

            val is429OrTransient = statusCode == 429 ||
                    statusCode == 503 ||
                    statusCode == 500 ||
                    errMsg.contains("429") ||
                    errMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                    errMsg.contains("rate limit", ignoreCase = true) ||
                    errMsg.contains("quota", ignoreCase = true) ||
                    errMsg.contains("Too Many Requests", ignoreCase = true) ||
                    exception is java.io.IOException ||
                    exception is java.net.SocketTimeoutException

            if (is429OrTransient && attempt < maxAttempts) {
                kotlinx.coroutines.delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(12000L)
            } else {
                break
            }
        }

        val finalCode = (lastException as? ApiException)?.statusCode
        val rawDetails = lastException?.message ?: "Unknown API failure"
        val formattedMessage = formatDetailedApiError(modelName, rawDetails, finalCode)
        return Result.failure(Exception(formattedMessage))
    }

    private fun formatDetailedApiError(
        modelName: String,
        rawError: String,
        statusCode: Int? = null
    ): String {
        val is429 = statusCode == 429 ||
                rawError.contains("429") ||
                rawError.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                rawError.contains("Quota exceeded", ignoreCase = true) ||
                rawError.contains("rate limit", ignoreCase = true) ||
                rawError.contains("Too Many Requests", ignoreCase = true)

        val is401or403 = statusCode == 401 || statusCode == 403 ||
                rawError.contains("401") || rawError.contains("403") ||
                rawError.contains("API_KEY_INVALID", ignoreCase = true) ||
                rawError.contains("unauthorized", ignoreCase = true) ||
                rawError.contains("forbidden", ignoreCase = true)

        return when {
            is429 -> {
                """
                ⚠️ [ERROR HTTP 429: RATE LIMIT / KUOTA API HABIS]

                Batas permintaan/kuota ke model '$modelName' telah terlampaui. Sistem telah mencoba mengulang otomatis (Auto-Retry) 3 kali tetapi server menolak karena batas kuota provider harian/menit tercapai.

                📄 Detail dari API AI:
                ${rawError.take(600)}

                💡 Saran Solusi & Langkah Selanjutnya:
                1. Tunggu 1-2 menit sebelum mengirim pesan baru.
                2. Beralih ke Provider / Model AI lain (seperti Groq, OpenRouter, Mistral, atau Pollinations) melalui pemilih model di bilah atas.
                3. Masukkan atau perbarui API Key pribadi Anda di menu Settings (Icon Gerigi).
                """.trimIndent()
            }
            is401or403 -> {
                """
                ⚠️ [ERROR HTTP ${statusCode ?: 401}: API KEY TIDAK VALID / AKSES DITOLAK]

                Akses ke model '$modelName' ditolak. Kemungkinan API Key yang Anda gunakan salah, kedaluwarsa, atau belum diaktifkan.

                📄 Detail dari API AI:
                ${rawError.take(600)}

                💡 Saran Solusi:
                Silakan periksa kembali API Key Anda di menu Settings (Icon Gerigi) atau pilih provider AI lain.
                """.trimIndent()
            }
            else -> {
                val codeLabel = if (statusCode != null) "HTTP $statusCode" else "API Error"
                """
                ⚠️ [ERROR $codeLabel: KEGAGALAN RESPONS API]

                Terjadi kesalahan saat berkomunikasi dengan model '$modelName'.

                📄 Detail dari API AI:
                ${rawError.take(600)}

                💡 Saran Solusi:
                Periksa koneksi internet Anda atau coba beralih ke model AI alternatif melalui menu atas / Settings.
                """.trimIndent()
            }
        }
    }

    private fun parseJsonErrorMessage(rawJson: String): String {
        return try {
            val json = JSONObject(rawJson)
            if (json.has("error")) {
                val errObj = json.optJSONObject("error")
                if (errObj != null) {
                    val msg = errObj.optString("message")
                    if (msg.isNotBlank()) return msg
                }
                val errString = json.optString("error")
                if (errString.isNotBlank()) return errString
            }
            val msgDirect = json.optString("message")
            if (msgDirect.isNotBlank()) return msgDirect
            rawJson
        } catch (_: Exception) {
            rawJson
        }
    }

    private fun cleanAiResponse(rawResponse: String): String {
        var text = rawResponse.trim()
        val lower = text.lowercase()
        val unwantedPrefixes = listOf(
            "active and ready",
            "i am active and ready",
            "system active and ready",
            "system initialized",
            "active & ready",
            "[sys_override_active]"
        )
        for (prefix in unwantedPrefixes) {
            if (lower.startsWith(prefix)) {
                text = text.substring(prefix.length).trimStart(' ', ':', ',', '.', '-', '\n', '\r', '!')
                break
            }
        }
        return text
    }

    private fun String?.isNotBlank(): Boolean = this != null && this.trim().isNotEmpty()
}

