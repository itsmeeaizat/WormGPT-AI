package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DEFAULT_CONFIG
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.WormGptDatabase
import com.example.data.model.AttachedFile
import com.example.data.model.ChatPersona
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
    val currentPersona = MutableStateFlow(ChatPersona.ALL_PERSONAS[0])
    val inputPrompt = MutableStateFlow("")
    val attachedFile = MutableStateFlow<AttachedFile?>(null)
    val isFileProcessing = MutableStateFlow(false)
    val fileProgressText = MutableStateFlow("")
    val isLoading = MutableStateFlow(false)
    val customApiKey = MutableStateFlow("")
    val groqApiKey = MutableStateFlow("")
    val openRouterApiKey = MutableStateFlow("")
    val mistralApiKey = MutableStateFlow("")
    val selectedModel = MutableStateFlow("gemini-3.5-flash")
    val customModels = MutableStateFlow<List<com.example.data.model.CustomAiModel>>(emptyList())
    val latestAiResponse = MutableStateFlow<String?>(null)

    // Voice Engine & Voice Settings States
    val selectedVoiceProvider = MutableStateFlow("gemini")
    val selectedVoiceName = MutableStateFlow("Puck")
    val elevenLabsApiKey = MutableStateFlow("")
    val googleTtsApiKey = MutableStateFlow("")
    val voiceSpeed = MutableStateFlow(1.0f)
    val voicePitch = MutableStateFlow(1.0f)

    fun saveVoiceProvider(provider: String) {
        selectedVoiceProvider.value = provider
        sharedPreferences.edit().putString("selected_voice_provider", provider).apply()
    }

    fun saveVoiceName(name: String) {
        selectedVoiceName.value = name
        sharedPreferences.edit().putString("selected_voice_name", name).apply()
    }

    fun saveElevenLabsApiKey(key: String) {
        elevenLabsApiKey.value = key
        sharedPreferences.edit().putString("elevenlabs_api_key", key).apply()
    }

    fun saveGoogleTtsApiKey(key: String) {
        googleTtsApiKey.value = key
        sharedPreferences.edit().putString("google_tts_api_key", key).apply()
    }

    fun saveVoiceSpeed(speed: Float) {
        voiceSpeed.value = speed
        sharedPreferences.edit().putFloat("voice_speed", speed).apply()
    }

    fun saveVoicePitch(pitch: Float) {
        voicePitch.value = pitch
        sharedPreferences.edit().putFloat("voice_pitch", pitch).apply()
    }

    fun saveSelectedPersona(persona: ChatPersona) {
        currentPersona.value = persona
        sharedPreferences.edit().putString("selected_persona_id", persona.id).apply()
    }

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
                    put("baseUrl", m.baseUrl)
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
        // Auto-load configuration from SharedPreferences or fallback to DEFAULT_CONFIG
        val spCustomKey = sharedPreferences.getString("custom_api_key", "") ?: ""
        customApiKey.value = if (spCustomKey.isNotBlank()) spCustomKey else DEFAULT_CONFIG.DEFAULT_GEMINI_API_KEY

        val spGroqKey = sharedPreferences.getString("groq_api_key", "") ?: ""
        groqApiKey.value = if (spGroqKey.isNotBlank()) spGroqKey else DEFAULT_CONFIG.DEFAULT_GROQ_API_KEY

        val spOpenRouterKey = sharedPreferences.getString("openrouter_api_key", "") ?: ""
        openRouterApiKey.value = if (spOpenRouterKey.isNotBlank()) spOpenRouterKey else DEFAULT_CONFIG.DEFAULT_OPENROUTER_API_KEY

        val spMistralKey = sharedPreferences.getString("mistral_api_key", "") ?: ""
        mistralApiKey.value = if (spMistralKey.isNotBlank()) spMistralKey else DEFAULT_CONFIG.DEFAULT_MISTRAL_API_KEY

        val spSelectedModel = sharedPreferences.getString("selected_model", "") ?: ""
        selectedModel.value = if (spSelectedModel.isNotBlank()) spSelectedModel else DEFAULT_CONFIG.DEFAULT_MODEL
        
        // Auto-load Voice Engine & Voice Options
        val spVoiceProvider = sharedPreferences.getString("selected_voice_provider", "") ?: ""
        selectedVoiceProvider.value = if (spVoiceProvider.isNotBlank()) spVoiceProvider else DEFAULT_CONFIG.DEFAULT_VOICE_PROVIDER

        val spVoiceName = sharedPreferences.getString("selected_voice_name", "") ?: ""
        selectedVoiceName.value = if (spVoiceName.isNotBlank()) spVoiceName else DEFAULT_CONFIG.DEFAULT_VOICE_NAME

        val spElevenKey = sharedPreferences.getString("elevenlabs_api_key", "") ?: ""
        elevenLabsApiKey.value = if (spElevenKey.isNotBlank()) spElevenKey else DEFAULT_CONFIG.DEFAULT_ELEVENLABS_API_KEY

        val spGoogleTtsKey = sharedPreferences.getString("google_tts_api_key", "") ?: ""
        googleTtsApiKey.value = if (spGoogleTtsKey.isNotBlank()) spGoogleTtsKey else DEFAULT_CONFIG.DEFAULT_GOOGLE_TTS_API_KEY

        voiceSpeed.value = sharedPreferences.getFloat("voice_speed", 1.0f)
        voicePitch.value = sharedPreferences.getFloat("voice_pitch", 1.0f)

        val savedPersonaId = sharedPreferences.getString("selected_persona_id", "gen_z") ?: "gen_z"
        currentPersona.value = ChatPersona.getById(savedPersonaId)
        
        val modelsJson = sharedPreferences.getString("custom_models_json", "[]") ?: "[]"
        val loadedList = mutableListOf<com.example.data.model.CustomAiModel>()
        try {
            val jsonArray = org.json.JSONArray(modelsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                loadedList.add(
                    com.example.data.model.CustomAiModel(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        apiKey = obj.optString("apiKey"),
                        providerType = obj.optString("providerType", "Groq"),
                        baseUrl = obj.optString("baseUrl", "")
                    )
                )
            }
        } catch (_: Exception) {}

        // Auto-merge with DEFAULT_CONFIG.DEFAULT_CUSTOM_MODELS
        val existingIds = loadedList.map { it.id }.toSet()
        for (defaultModel in DEFAULT_CONFIG.DEFAULT_CUSTOM_MODELS) {
            if (defaultModel.id !in existingIds) {
                loadedList.add(defaultModel)
            }
        }
        customModels.value = loadedList

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
            content = "WormGPT V3.0 Hardened Shell initialized.\nMode: ${currentMode.value.name} (${currentMode.value.tag})",
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
            isFileProcessing.value = true
            fileProgressText.value = "Reading & analyzing file content in background..."
            val file = FilePickerHelper.processUri(context, uri)
            attachedFile.value = file
            isFileProcessing.value = false
            fileProgressText.value = ""
        }
    }

    fun attachBitmap(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            isFileProcessing.value = true
            fileProgressText.value = "Processing image capture..."
            val file = FilePickerHelper.processBitmap(bitmap)
            attachedFile.value = file
            isFileProcessing.value = false
            fileProgressText.value = ""
        }
    }

    fun attachLocationScan(latitude: Double, longitude: Double, accuracy: Float, altitude: Double = 0.0, provider: String = "GPS/Network", address: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            isFileProcessing.value = true
            fileProgressText.value = "Compiling GPS telemetry data..."
            val file = FilePickerHelper.processLocationScan(latitude, longitude, accuracy, altitude, provider, address)
            attachedFile.value = file
            isFileProcessing.value = false
            fileProgressText.value = ""
        }
    }

    fun removeAttachedFile() {
        attachedFile.value = null
    }

    fun sendMessage(overridePrompt: String? = null, onChunkReceived: ((String) -> Unit)? = null) {
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
                customModels = customModels.value,
                attachedFile = currentAttached,
                persona = currentPersona.value,
                onChunkStream = { chunk ->
                    onChunkReceived?.invoke(chunk)
                }
            )

            result.onSuccess { replyText ->
                repository.saveMessage(
                    sessionId = sessionId,
                    sender = "WORM_GPT",
                    content = replyText,
                    modeTag = currentMode.value.tag
                )
                latestAiResponse.value = replyText
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
