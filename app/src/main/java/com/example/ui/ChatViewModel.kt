package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DEFAULT_CONFIG
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.NovaAiDatabase
import com.example.data.model.AttachedFile
import com.example.data.model.ChatPersona
import com.example.data.model.NovaMode
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import com.example.data.api.FileWebSocketClient
import com.example.data.api.FileWsEvent
import com.example.data.repository.FileUploadRepository

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    private val fileUploadRepository: FileUploadRepository = FileUploadRepository(application)
    private val fileWsClient: FileWebSocketClient = FileWebSocketClient()
    val clientId = "client_${System.currentTimeMillis()}"
    private var generateJob: Job? = null
    
    private val sharedPreferences = application.getSharedPreferences("novaai_config", Context.MODE_PRIVATE)

    val currentMode = MutableStateFlow(NovaMode.ALL_MODES[0])
    val allPersonas = MutableStateFlow<List<ChatPersona>>(ChatPersona.DEFAULT_PERSONAS)
    val currentPersona = MutableStateFlow(ChatPersona.DEFAULT_PERSONAS[0])
    val inputPrompt = MutableStateFlow("")
    val attachedFile = MutableStateFlow<AttachedFile?>(null)
    val isFileProcessing = MutableStateFlow(false)
    val fileProgressText = MutableStateFlow("")
    val isLoading = MutableStateFlow(false)
    val customApiKey = MutableStateFlow("")
    val groqApiKey = MutableStateFlow("")
    val openRouterApiKey = MutableStateFlow("")
    val mistralApiKey = MutableStateFlow("")
    val selectedModel = MutableStateFlow(DEFAULT_CONFIG.DEFAULT_MODEL)
    val customModels = MutableStateFlow<List<com.example.data.model.CustomAiModel>>(emptyList())
    val latestAiResponse = MutableStateFlow<String?>(null)

    // Voice Engine & Voice Settings States
    val selectedVoiceProvider = MutableStateFlow("gemini")
    val selectedVoiceName = MutableStateFlow("Puck")
    val elevenLabsApiKey = MutableStateFlow("")
    val googleTtsApiKey = MutableStateFlow("")
    val voiceSpeed = MutableStateFlow(1.0f)
    val voicePitch = MutableStateFlow(1.0f)

    // Reply / Quote Message State
    val replyingToMessage = MutableStateFlow<ChatMessageEntity?>(null)

    fun setReplyingTo(message: ChatMessageEntity?) {
        replyingToMessage.value = message
    }

    fun clearReplyingTo() {
        replyingToMessage.value = null
    }

    fun stopResponse() {
        generateJob?.cancel()
        generateJob = null
        fileWsClient.disconnect()
        isLoading.value = false
        fileProgressText.value = ""
    }

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

    fun addCustomPersona(name: String, description: String, promptInstruction: String, sampleEmoji: String) {
        val cleanName = name.trim()
        val cleanPrompt = promptInstruction.trim()
        if (cleanName.isBlank() || cleanPrompt.isBlank()) return
        
        val cleanTag = cleanName.uppercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }.take(12)
        val newId = "custom_${System.currentTimeMillis()}"
        val newPersona = ChatPersona(
            id = newId,
            name = cleanName,
            tag = if (cleanTag.isNotBlank()) cleanTag else "CUSTOM",
            description = if (description.isNotBlank()) description.trim() else "Persona kustom buatan pengguna.",
            sampleEmoji = if (sampleEmoji.isNotBlank()) sampleEmoji.trim() else "✨",
            systemPromptInstruction = cleanPrompt,
            isCustom = true
        )

        val currentCustoms = allPersonas.value.filter { it.isCustom }.toMutableList()
        currentCustoms.add(newPersona)
        saveCustomPersonasToSp(currentCustoms)

        val updatedAll = ChatPersona.DEFAULT_PERSONAS + currentCustoms
        allPersonas.value = updatedAll
        saveSelectedPersona(newPersona)
    }

    fun deleteCustomPersona(personaId: String) {
        val currentCustoms = allPersonas.value.filter { it.isCustom && it.id != personaId }
        saveCustomPersonasToSp(currentCustoms)
        val updatedAll = ChatPersona.DEFAULT_PERSONAS + currentCustoms
        allPersonas.value = updatedAll
        if (currentPersona.value.id == personaId) {
            saveSelectedPersona(updatedAll.firstOrNull() ?: ChatPersona.DEFAULT_PERSONAS[0])
        }
    }

    private fun saveCustomPersonasToSp(customList: List<ChatPersona>) {
        try {
            val jsonArray = org.json.JSONArray()
            for (p in customList) {
                val obj = org.json.JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("tag", p.tag)
                    put("description", p.description)
                    put("sampleEmoji", p.sampleEmoji)
                    put("systemPromptInstruction", p.systemPromptInstruction)
                }
                jsonArray.put(obj)
            }
            sharedPreferences.edit().putString("custom_personas_json", jsonArray.toString()).apply()
        } catch (_: Exception) {}
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

    fun applyCentralConfig(context: android.content.Context, chosenModel: String? = null) {
        val centralConfig = com.example.util.RawConfigManager.loadCentralConfig(context)
        val modelToUse = chosenModel ?: centralConfig.defaultModel
        
        saveSelectedModel(modelToUse)
        if (centralConfig.centralGeminiApiKey.isNotBlank()) {
            saveCustomApiKey(centralConfig.centralGeminiApiKey)
        }
        if (centralConfig.centralGroqApiKey.isNotBlank()) {
            saveGroqApiKey(centralConfig.centralGroqApiKey)
        }
        if (centralConfig.centralOpenRouterApiKey.isNotBlank()) {
            saveOpenRouterApiKey(centralConfig.centralOpenRouterApiKey)
        }
        if (centralConfig.centralMistralApiKey.isNotBlank()) {
            saveMistralApiKey(centralConfig.centralMistralApiKey)
        }

        // Merge and persist custom models from raw central config
        val centralCustomModels = centralConfig.toCustomAiModels()
        if (centralCustomModels.isNotEmpty()) {
            val currentList = customModels.value.toMutableList()
            for (cm in centralCustomModels) {
                val idx = currentList.indexOfFirst { it.id == cm.id }
                if (idx >= 0) {
                    currentList[idx] = cm
                } else {
                    currentList.add(cm)
                }
            }
            saveCustomModels(currentList)
        }
    }

    init {
        val rawConfig = com.example.util.RawConfigManager.loadCentralConfig(application.applicationContext)

        // Auto-load configuration from SharedPreferences or fallback to raw developer_config / DEFAULT_CONFIG
        val spCustomKey = sharedPreferences.getString("custom_api_key", "") ?: ""
        customApiKey.value = if (spCustomKey.isNotBlank()) spCustomKey else rawConfig.centralGeminiApiKey.ifBlank { DEFAULT_CONFIG.DEFAULT_GEMINI_API_KEY }

        val spGroqKey = sharedPreferences.getString("groq_api_key", "") ?: ""
        groqApiKey.value = if (spGroqKey.isNotBlank()) spGroqKey else rawConfig.centralGroqApiKey.ifBlank { DEFAULT_CONFIG.DEFAULT_GROQ_API_KEY }

        val spOpenRouterKey = sharedPreferences.getString("openrouter_api_key", "") ?: ""
        openRouterApiKey.value = if (spOpenRouterKey.isNotBlank()) spOpenRouterKey else rawConfig.centralOpenRouterApiKey.ifBlank { DEFAULT_CONFIG.DEFAULT_OPENROUTER_API_KEY }

        val spMistralKey = sharedPreferences.getString("mistral_api_key", "") ?: ""
        mistralApiKey.value = if (spMistralKey.isNotBlank()) spMistralKey else rawConfig.centralMistralApiKey.ifBlank { DEFAULT_CONFIG.DEFAULT_MISTRAL_API_KEY }

        val spSelectedModel = sharedPreferences.getString("selected_model", "") ?: ""
        selectedModel.value = if (spSelectedModel.isNotBlank()) spSelectedModel else rawConfig.defaultModel.ifBlank { DEFAULT_CONFIG.DEFAULT_MODEL }
        
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

        // Load Custom Personas
        val personasJson = sharedPreferences.getString("custom_personas_json", "[]") ?: "[]"
        val loadedPersonas = mutableListOf<ChatPersona>()
        try {
            val jsonArray = org.json.JSONArray(personasJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                loadedPersonas.add(
                    ChatPersona(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        tag = obj.optString("tag", "CUSTOM"),
                        description = obj.optString("description"),
                        sampleEmoji = obj.optString("sampleEmoji", "✨"),
                        systemPromptInstruction = obj.optString("systemPromptInstruction"),
                        isCustom = true
                    )
                )
            }
        } catch (_: Exception) {}

        val combinedPersonas = ChatPersona.DEFAULT_PERSONAS + loadedPersonas
        allPersonas.value = combinedPersonas

        val savedPersonaId = sharedPreferences.getString("selected_persona_id", "gen_z") ?: "gen_z"
        currentPersona.value = ChatPersona.getById(savedPersonaId, combinedPersonas)
        
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

        // Auto-merge with rawConfig custom models & DEFAULT_CONFIG.DEFAULT_CUSTOM_MODELS
        val existingIds = loadedList.map { it.id }.toSet()
        for (rawModel in rawConfig.toCustomAiModels()) {
            if (rawModel.id !in existingIds) {
                loadedList.add(rawModel)
            }
        }
        val existingIdsUpdated = loadedList.map { it.id }.toSet()
        for (defaultModel in DEFAULT_CONFIG.DEFAULT_CUSTOM_MODELS) {
            if (defaultModel.id !in existingIdsUpdated) {
                loadedList.add(defaultModel)
            }
        }
        customModels.value = loadedList

        val db = NovaAiDatabase.getDatabase(application)
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
        
        // Add initial welcome message
        repository.saveMessage(
            sessionId = newId,
            sender = "WORM_GPT",
            content = "Halo! Ada yang bisa saya bantu hari ini?",
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

    fun switchMode(mode: NovaMode) {
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
        val quotedMessage = replyingToMessage.value
        
        if ((rawUserPrompt.isBlank() && currentAttached == null && quotedMessage == null) || isLoading.value) return

        val sessionId = activeSessionId.value ?: return

        // 1. Teks murni pengguna yang akan disimpan di DB & ditampilkan di UI Bubble (tanpa string kutipan mentah)
        val userDisplayContent = buildString {
            if (currentAttached != null) {
                if (currentAttached.mimeType == "application/location-share") {
                    append(currentAttached.contentPayload)
                    if (rawUserPrompt.isNotBlank()) {
                        append("\n\n")
                        append(rawUserPrompt)
                    }
                } else {
                    appendLine("📎 [FILE UPLOAD: ${currentAttached.name} (${currentAttached.formattedSize})] - Memproses...")
                    appendLine("```${currentAttached.extension.ifBlank { "txt" }}")
                    appendLine(currentAttached.contentPayload)
                    appendLine("```")
                    appendLine()
                    if (rawUserPrompt.isNotBlank()) {
                        append(rawUserPrompt)
                    } else {
                        append("Tolong analisa file ${currentAttached.name} dan berikan ringkasan serta poin-poin penting.")
                    }
                }
            } else {
                if (rawUserPrompt.isNotBlank()) {
                    append(rawUserPrompt)
                } else if (quotedMessage != null) {
                    append("Tolong jelaskan lebih lanjut mengenai pesan yang saya kutip di atas.")
                }
            }
        }.trim()

        val replyId = quotedMessage?.id
        val replyTextSnippet = quotedMessage?.content?.take(200)
        val replySenderName = quotedMessage?.sender

        // 2. Teks prompt lengkap yang dikirim ke AI agar AI mengetahui konteks balasan
        val aiPrompt = if (quotedMessage != null && !replyTextSnippet.isNullOrBlank()) {
            "[Pesan yang dibalas (${replySenderName ?: "AI"}): \"${replyTextSnippet.replace("\n", " ")}\"]\n\n$userDisplayContent"
        } else if (currentAttached?.mimeType == "application/location-share") {
            "[PENGGUNA MEMBAGIKAN LOKASI GPS MEKA]\nAlamat: ${currentAttached.inspectionSummary ?: ""}\n$userDisplayContent"
        } else {
            userDisplayContent
        }

        inputPrompt.value = ""
        attachedFile.value = null
        replyingToMessage.value = null
        isLoading.value = true

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            try {
                // 1. Simpan pesan User secara instan ke database (Optimistic UI)
                repository.saveMessage(
                    sessionId = sessionId,
                    sender = "USER",
                    content = userDisplayContent,
                    modeTag = currentMode.value.tag,
                    replyToMessageId = replyId,
                    repliedText = replyTextSnippet,
                    repliedSender = replySenderName
                )

                val currentHistory = activeMessages.value.filter { !it.isError }

                // 2. Hubungkan WebSocket real-time client jika ada pemrosesan file
                if (currentAttached != null) {
                    fileWsClient.eventListener = { event ->
                        when (event) {
                            is FileWsEvent.FileQueued -> {
                                fileProgressText.value = event.message
                            }
                            is FileWsEvent.FileProcessing -> {
                                fileProgressText.value = event.message
                            }
                            is FileWsEvent.FileParsed -> {
                                fileProgressText.value = event.message
                            }
                            is FileWsEvent.AiStreamChunk -> {
                                onChunkReceived?.invoke(event.chunk)
                            }
                            is FileWsEvent.AiStreamComplete -> {
                                fileProgressText.value = ""
                            }
                            is FileWsEvent.FileError -> {
                                fileProgressText.value = event.errorMessage
                            }
                            else -> {}
                        }
                    }
                    fileWsClient.connect(clientId)
                }

                // 3. Kirim pesan ke Model AI / Upload Repository
                val result = if (currentAttached != null) {
                    // Coba upload ke backend / Queue dulu
                    val uploadRes = fileUploadRepository.uploadFileToBackend(
                        file = currentAttached,
                        clientId = clientId,
                        prompt = rawUserPrompt
                    )
                    if (uploadRes.isSuccess) {
                        // Berhasil dikirim ke queue backend
                        Result.success("File ${currentAttached.name} berhasil diunggah ke antrean server dan sedang diproses secara streaming real-time.")
                    } else {
                        // Direct Gemini Streaming Fallback
                        repository.sendToAiModel(
                            userPrompt = aiPrompt,
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
                    }
                } else {
                    // Normal chat message AI stream
                    repository.sendToAiModel(
                        userPrompt = aiPrompt,
                        conversationHistory = currentHistory,
                        mode = currentMode.value,
                        geminiApiKey = customApiKey.value,
                        groqApiKey = groqApiKey.value,
                        openRouterApiKey = openRouterApiKey.value,
                        mistralApiKey = mistralApiKey.value,
                        selectedModel = selectedModel.value,
                        customModels = customModels.value,
                        attachedFile = null,
                        persona = currentPersona.value,
                        onChunkStream = { chunk ->
                            onChunkReceived?.invoke(chunk)
                        }
                    )
                }

                result.onSuccess { replyText ->
                    repository.saveMessage(
                        sessionId = sessionId,
                        sender = "WORM_GPT",
                        content = replyText,
                        modeTag = currentMode.value.tag
                    )
                    latestAiResponse.value = replyText
                    com.example.util.NotificationHelper.sendAiResponseNotification(getApplication(), replyText)
                }.onFailure { err ->
                    if (err !is kotlinx.coroutines.CancellationException) {
                        repository.saveMessage(
                            sessionId = sessionId,
                            sender = "WORM_GPT",
                            content = "[SYS_ERROR] ${err.message ?: "Gagal memproses keluaran AI."}",
                            modeTag = currentMode.value.tag,
                            isError = true
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("ChatViewModel", "Generation cancelled by user")
            } finally {
                isLoading.value = false
                fileProgressText.value = ""
                generateJob = null
            }
        }
    }


    fun setQuickPrompt(promptText: String) {
        inputPrompt.value = promptText
    }
}
