package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.WormGptDatabase
import com.example.data.model.AttachedFile
import com.example.data.model.WormMode
import com.example.data.repository.ChatRepository
import com.example.util.FilePickerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    
    private val sharedPreferences = application.getSharedPreferences("wormgpt_config", Context.MODE_PRIVATE)

    val currentMode = MutableStateFlow(WormMode.ALL_MODES[0])
    val inputPrompt = MutableStateFlow("")
    val attachedFile = MutableStateFlow<AttachedFile?>(null)
    val isLoading = MutableStateFlow(false)
    val customApiKey = MutableStateFlow("")
    val groqApiKey = MutableStateFlow("")
    val openRouterApiKey = MutableStateFlow("")
    val mistralApiKey = MutableStateFlow("")
    val selectedModel = MutableStateFlow("gemini-3.5-flash")
    val customModels = MutableStateFlow<List<com.example.data.model.CustomAiModel>>(emptyList())

    fun saveCustomApiKey(key: String) {
        customApiKey.value = key
        sharedPreferences.edit().putString("custom_api_key", key).apply()
    }

    fun saveGroqApiKey(key: String) {
        groqApiKey.value = key
        sharedPreferences.edit().putString("groq_api_key", key).apply()
    }

    fun saveOpenRouterApiKey(key: String) {
        openRouterApiKey.value = key
        sharedPreferences.edit().putString("openrouter_api_key", key).apply()
    }

    fun saveMistralApiKey(key: String) {
        mistralApiKey.value = key
        sharedPreferences.edit().putString("mistral_api_key", key).apply()
    }

    fun saveSelectedModel(model: String) {
        selectedModel.value = model
        sharedPreferences.edit().putString("selected_model", model).apply()
    }

    fun saveCustomModels(models: List<com.example.data.model.CustomAiModel>) {
        customModels.value = models
        try {
            val jsonArray = org.json.JSONArray()
            for (m in models) {
                val obj = org.json.JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("apiKey", m.apiKey)
                    put("providerType", m.providerType)
                }
                jsonArray.put(obj)
            }
            sharedPreferences.edit().putString("custom_models_json", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    val activeSessionId = MutableStateFlow<String?>(null)

    val allSessions: StateFlow<List<ChatSessionEntity>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<ChatMessageEntity>>

    init {
        // Auto-load configuration from SharedPreferences
        customApiKey.value = sharedPreferences.getString("custom_api_key", "") ?: ""
        groqApiKey.value = sharedPreferences.getString("groq_api_key", "") ?: ""
        openRouterApiKey.value = sharedPreferences.getString("openrouter_api_key", "") ?: ""
        mistralApiKey.value = sharedPreferences.getString("mistral_api_key", "") ?: ""
        selectedModel.value = sharedPreferences.getString("selected_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
        
        val modelsJson = sharedPreferences.getString("custom_models_json", "[]") ?: "[]"
        try {
            val jsonArray = org.json.JSONArray(modelsJson)
            val list = mutableListOf<com.example.data.model.CustomAiModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    com.example.data.model.CustomAiModel(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        apiKey = obj.getString("apiKey"),
                        providerType = obj.getString("providerType")
                    )
                )
            }
            customModels.value = list
        } catch (_: Exception) {}

        val db = WormGptDatabase.getDatabase(application)
        repository = ChatRepository(db.chatDao())

        allSessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        activeMessages = activeSessionId.flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize default session
        viewModelScope.launch {
            createNewSession()
        }
    }

    suspend fun createNewSession(): String {
        val newId = repository.createNewSession(currentMode.value)
        activeSessionId.value = newId
        
        // Add initial system message welcome
        repository.saveMessage(
            sessionId = newId,
            sender = "WORM_GPT",
            content = "[SYS_OVERRIDE_ACTIVE]\nWormGPT V3.0 Hardened Shell initialized.\nMode: ${currentMode.value.name} (${currentMode.value.tag})\nReady for repository scanning, security analysis, or custom queries.",
            modeTag = currentMode.value.tag
        )
        
        return newId
    }

    fun selectSession(sessionId: String) {
        activeSessionId.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (activeSessionId.value == sessionId) {
                val remaining = allSessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    activeSessionId.value = remaining.first().id
                } else {
                    createNewSession()
                }
            }
        }
    }

    fun switchMode(mode: WormMode) {
        currentMode.value = mode
    }

    fun attachFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = FilePickerHelper.processUri(context, uri)
            attachedFile.value = file
        }
    }

    fun attachBitmap(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = FilePickerHelper.processBitmap(bitmap)
            attachedFile.value = file
        }
    }

    fun attachLocationScan(latitude: Double, longitude: Double, accuracy: Float, altitude: Double = 0.0, provider: String = "GPS/Network", address: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val file = FilePickerHelper.processLocationScan(latitude, longitude, accuracy, altitude, provider, address)
            attachedFile.value = file
        }
    }

    fun removeAttachedFile() {
        attachedFile.value = null
    }

    fun sendMessage(overridePrompt: String? = null) {
        val rawUserPrompt = (overridePrompt ?: inputPrompt.value).trim()
        val currentAttached = attachedFile.value
        
        if ((rawUserPrompt.isBlank() && currentAttached == null) || isLoading.value) return

        val sessionId = activeSessionId.value ?: return

        val fullTextToSend = buildString {
            if (currentAttached != null) {
                appendLine("📎 [ATTACHMENT: ${currentAttached.name} (${currentAttached.formattedSize}) | Type: ${currentAttached.mimeType}]")
                appendLine("```${currentAttached.extension.ifBlank { "txt" }}")
                appendLine(currentAttached.contentPayload)
                appendLine("```")
                appendLine()
            }
            if (rawUserPrompt.isNotBlank()) {
                append(rawUserPrompt)
            } else {
                append("Please inspect and analyze this attached file for vulnerabilities, structural audit, or code enhancements.")
            }
        }

        inputPrompt.value = ""
        attachedFile.value = null
        isLoading.value = true

        viewModelScope.launch {
            // Save user message
            repository.saveMessage(
                sessionId = sessionId,
                sender = "USER",
                content = fullTextToSend,
                modeTag = currentMode.value.tag
            )

            val currentHistory = activeMessages.value.filter { !it.isError }

            val result = repository.sendToAiModel(
                userPrompt = fullTextToSend,
                conversationHistory = currentHistory,
                mode = currentMode.value,
                geminiApiKey = customApiKey.value,
                groqApiKey = groqApiKey.value,
                openRouterApiKey = openRouterApiKey.value,
                mistralApiKey = mistralApiKey.value,
                selectedModel = selectedModel.value,
                customModels = customModels.value
            )

            result.onSuccess { replyText ->
                repository.saveMessage(
                    sessionId = sessionId,
                    sender = "WORM_GPT",
                    content = replyText,
                    modeTag = currentMode.value.tag
                )
            }.onFailure { err ->
                repository.saveMessage(
                    sessionId = sessionId,
                    sender = "WORM_GPT",
                    content = "[SYS_ERROR] ${err.message ?: "Failed to generate AI output."}",
                    modeTag = currentMode.value.tag,
                    isError = true
                )
            }

            isLoading.value = false
        }
    }

    fun setQuickPrompt(promptText: String) {
        inputPrompt.value = promptText
    }
}
