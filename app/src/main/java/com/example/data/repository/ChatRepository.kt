package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.db.ChatDao
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
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
        customModels: List<com.example.data.model.CustomAiModel> = emptyList()
    ): Result<String> {
        val customModel = customModels.find { it.id == selectedModel }
        if (customModel != null) {
            val key = customModel.apiKey.trim()
            if (key.isBlank()) {
                return Result.failure(Exception("API key untuk model kustom '${customModel.name}' belum diisi."))
            }
            return when (customModel.providerType.lowercase()) {
                "groq" -> sendToOpenAiCompatibleApi("https://api.groq.com/openai/v1/chat/completions", key, customModel.id, mode.systemPrompt, conversationHistory, userPrompt)
                "openrouter" -> sendToOpenAiCompatibleApi("https://openrouter.ai/api/v1/chat/completions", key, customModel.id, mode.systemPrompt, conversationHistory, userPrompt)
                "mistral" -> sendToOpenAiCompatibleApi("https://api.mistral.ai/v1/chat/completions", key, customModel.id, mode.systemPrompt, conversationHistory, userPrompt)
                "gemini" -> sendToGemini(userPrompt, conversationHistory, mode, key, customModel.id)
                else -> sendToOpenAiCompatibleApi("https://api.openai.com/v1/chat/completions", key, customModel.id, mode.systemPrompt, conversationHistory, userPrompt)
            }
        }

        return when {
            selectedModel.startsWith("pollinations/") -> {
                val realModel = selectedModel.removePrefix("pollinations/")
                sendToPollinationsApi(realModel, mode.systemPrompt, userPrompt)
            }
            selectedModel.startsWith("groq/") -> {
                val realModel = selectedModel.removePrefix("groq/")
                val apiKey = groqApiKey?.trim().orEmpty()
                if (apiKey.isBlank()) {
                    Result.failure(Exception("Groq API key belum dimasukkan. Masukkan Groq Key (gsk_...) di menu Settings."))
                } else {
                    sendToOpenAiCompatibleApi(
                        baseUrl = "https://api.groq.com/openai/v1/chat/completions",
                        apiKey = apiKey,
                        modelName = realModel,
                        systemPrompt = mode.systemPrompt,
                        conversationHistory = conversationHistory,
                        userPrompt = userPrompt
                    )
                }
            }
            selectedModel.startsWith("openrouter/") -> {
                val realModel = selectedModel.removePrefix("openrouter/")
                val apiKey = openRouterApiKey?.trim().orEmpty()
                if (apiKey.isBlank()) {
                    Result.failure(Exception("OpenRouter API key belum dimasukkan. Masukkan OpenRouter Key (sk-or-...) di menu Settings."))
                } else {
                    sendToOpenAiCompatibleApi(
                        baseUrl = "https://openrouter.ai/api/v1/chat/completions",
                        apiKey = apiKey,
                        modelName = realModel,
                        systemPrompt = mode.systemPrompt,
                        conversationHistory = conversationHistory,
                        userPrompt = userPrompt
                    )
                }
            }
            selectedModel.startsWith("mistral/") -> {
                val realModel = selectedModel.removePrefix("mistral/")
                val apiKey = mistralApiKey?.trim().orEmpty()
                if (apiKey.isBlank()) {
                    Result.failure(Exception("Mistral API key belum dimasukkan. Masukkan Mistral Key di menu Settings."))
                } else {
                    sendToOpenAiCompatibleApi(
                        baseUrl = "https://api.mistral.ai/v1/chat/completions",
                        apiKey = apiKey,
                        modelName = realModel,
                        systemPrompt = mode.systemPrompt,
                        conversationHistory = conversationHistory,
                        userPrompt = userPrompt
                    )
                }
            }
            else -> {
                // Default Gemini API handler
                val cleanModel = selectedModel.removePrefix("gemini/")
                sendToGemini(userPrompt, conversationHistory, mode, geminiApiKey, cleanModel)
            }
        }
    }

    suspend fun sendToGemini(
        userPrompt: String,
        conversationHistory: List<ChatMessageEntity>,
        mode: WormMode,
        customApiKey: String? = null,
        selectedModel: String = "gemini-3.5-flash"
    ): Result<String> {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
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

        // Add current user prompt
        contents.add(
            Content(
                role = "user",
                parts = listOf(Part(text = userPrompt))
            )
        )

        val systemInstructionContent = Content(
            parts = listOf(Part(text = mode.systemPrompt))
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

        return try {
            val response = GeminiClient.apiService.generateContent(
                model = selectedModel,
                apiKey = apiKey,
                request = request
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else if (response.error != null) {
                Result.failure(Exception("API Error (${response.error.code}): ${response.error.message}"))
            } else {
                Result.failure(Exception("Response was empty or filtered by safety systems."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendToOpenAiCompatibleApi(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        systemPrompt: String,
        conversationHistory: List<ChatMessageEntity>,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            
            // System prompt
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
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
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://github.com/mnfst/awesome-free-llm-apis")
                .addHeader("X-Title", "WormGPT Mobile")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful && bodyString.isNotBlank()) {
                    val json = JSONObject(bodyString)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val choice = choices.getJSONObject(0)
                        val messageObj = choice.optJSONObject("message")
                        val content = messageObj?.optString("content")
                        if (!content.isNullOrBlank()) {
                            return@withContext Result.success(content)
                        }
                    }
                    Result.failure(Exception("Respon model kosong dari $modelName."))
                } else {
                    Result.failure(Exception("HTTP Error ${response.code}: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendToPollinationsApi(
        modelName: String,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
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
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful && bodyString.isNotBlank()) {
                    Result.success(bodyString)
                } else {
                    Result.failure(Exception("Pollinations AI Error ${response.code}: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun String?.isNotBlank(): Boolean = this != null && this.trim().isNotEmpty()
}

