package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.model.ChatPersona
import com.example.data.model.CustomAiModel
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptRedAccent

// ChatGPT / Modern Light Palette Colors (Matches Homepage Theme)
private val ChatGptLightBg = Color(0xFFFFFFFF)
private val ChatGptCardBg = Color(0xFFF9FAFB)
private val ChatGptInputBg = Color(0xFFF3F4F6)
private val ChatGptSelectedCardBg = Color(0xFFECFDF5)
private val ChatGptBorder = Color(0xFFE5E7EB)
private val ChatGptEmerald = Color(0xFF10A37F)
private val ChatGptTextPrimary = Color(0xFF111827)
private val ChatGptTextSecondary = Color(0xFF4B5563)
private val ChatGptTextMuted = Color(0xFF9CA3AF)

@Composable
fun SettingsDialog(
    customApiKey: String,
    groqApiKey: String = "",
    openRouterApiKey: String = "",
    mistralApiKey: String = "",
    selectedModel: String,
    customModels: List<CustomAiModel> = emptyList(),
    currentPersona: ChatPersona = ChatPersona.DEFAULT_PERSONAS[0],
    allPersonas: List<ChatPersona> = ChatPersona.DEFAULT_PERSONAS,
    selectedVoiceProvider: String = "gemini",
    selectedVoiceName: String = "Puck",
    elevenLabsApiKey: String = "",
    googleTtsApiKey: String = "",
    onSaveApiKey: (String) -> Unit,
    onSaveGroqApiKey: (String) -> Unit = {},
    onSaveOpenRouterApiKey: (String) -> Unit = {},
    onSaveMistralApiKey: (String) -> Unit = {},
    onSaveModel: (String) -> Unit,
    onSaveCustomModels: (List<CustomAiModel>) -> Unit = {},
    onSavePersona: (ChatPersona) -> Unit = {},
    onAddCustomPersona: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteCustomPersona: (String) -> Unit = {},
    onSaveVoiceProvider: (String) -> Unit = {},
    onSaveVoiceName: (String) -> Unit = {},
    onSaveElevenLabsApiKey: (String) -> Unit = {},
    onSaveGoogleTtsApiKey: (String) -> Unit = {},
    onTestVoice: ((String, String, String, String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var geminiKeyText by remember { mutableStateOf(customApiKey) }
    var groqKeyText by remember { mutableStateOf(groqApiKey) }
    var openRouterKeyText by remember { mutableStateOf(openRouterApiKey) }
    var mistralKeyText by remember { mutableStateOf(mistralApiKey) }
    var currentSelectedModel by remember { mutableStateOf(selectedModel) }
    var customModelsList by remember { mutableStateOf(customModels) }
    var selectedPersona by remember { mutableStateOf(currentPersona) }

    // Custom Persona Builder State
    var isCreatingPersona by remember { mutableStateOf(false) }
    var newPersonaName by remember { mutableStateOf("") }
    var newPersonaDesc by remember { mutableStateOf("") }
    var newPersonaEmoji by remember { mutableStateOf("") }
    var newPersonaPrompt by remember { mutableStateOf("") }

    // Voice Settings States
    var voiceProvider by remember { mutableStateOf(selectedVoiceProvider) }
    var voiceName by remember { mutableStateOf(selectedVoiceName) }
    var elevenKeyText by remember { mutableStateOf(elevenLabsApiKey) }
    var googleTtsKeyText by remember { mutableStateOf(googleTtsApiKey) }

    var showOwnerLogin by remember { mutableStateOf(false) }
    var showOwnerSettings by remember { mutableStateOf(false) }
    var showCentralModelDialog by remember { mutableStateOf(false) }
    var ownerPasswordInput by remember { mutableStateOf("") }
    var ownerLoginError by remember { mutableStateOf(false) }

    // Custom Model Add/Edit Form Dialog State
    var showAddCustomModelDialog by remember { mutableStateOf(false) }
    var editingModelItem by remember { mutableStateOf<CustomAiModel?>(null) }
    var newModelId by remember { mutableStateOf("") }
    var newModelName by remember { mutableStateOf("") }
    var newModelKey by remember { mutableStateOf("") }
    var newModelBaseUrl by remember { mutableStateOf("") }
    var newModelProvider by remember { mutableStateOf("Groq") }
    var newCustomProviderName by remember { mutableStateOf("Custom") }

    val scrollState = rememberScrollState()

    fun openAddCustomModelForm(modelToEdit: CustomAiModel? = null) {
        editingModelItem = modelToEdit
        if (modelToEdit != null) {
            newModelId = modelToEdit.id
            newModelName = modelToEdit.name
            newModelKey = modelToEdit.apiKey
            newModelBaseUrl = modelToEdit.baseUrl
            val stdProviders = listOf("Groq", "OpenRouter", "Mistral", "Gemini", "OpenAI")
            if (stdProviders.any { it.equals(modelToEdit.providerType, ignoreCase = true) }) {
                newModelProvider = modelToEdit.providerType
                newCustomProviderName = "Custom"
            } else {
                newModelProvider = "Custom"
                newCustomProviderName = modelToEdit.providerType
            }
        } else {
            newModelId = ""
            newModelName = ""
            newModelKey = ""
            newModelBaseUrl = ""
            newModelProvider = "Groq"
            newCustomProviderName = "Custom"
        }
        showAddCustomModelDialog = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ChatGptLightBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Drag / Header handle bar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(ChatGptBorder)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Top Title & Dismiss Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ChatGptEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = ChatGptEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pengaturan Sistem AI",
                                color = ChatGptTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Konfigurasi Persona, Engine & API Key",
                                color = ChatGptTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ChatGptCardBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = ChatGptTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Owner Settings Protected Pill Button
                    Button(
                        onClick = { showOwnerLogin = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ChatGptCardBg),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, ChatGptBorder, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = ChatGptEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Owner Settings & Multi-Model Config",
                                    color = ChatGptTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "LOCKED",
                                color = ChatGptEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ChatGptEmerald.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secret status badge (Clean minimalist pill)
                    val isSecretInjected = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                    val isKeyActive = isSecretInjected || geminiKeyText.isNotBlank()
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ChatGptCardBg,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ChatGptBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isKeyActive) ChatGptEmerald else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isKeyActive) "Global Gemini API Key Aktif" else "Gemini Key Tambahan Belum Diisi (Opsional)",
                                color = if (isKeyActive) ChatGptTextPrimary else ChatGptTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section Title: Persona Selector
                    SectionHeader(title = "PERSONA & GAYA OBROLAN AI")
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allPersonas.forEach { persona ->
                            val isSelected = persona.id == selectedPersona.id
                            PersonaSelectionCard(
                                persona = persona,
                                isSelected = isSelected,
                                onSelect = {
                                    selectedPersona = persona
                                    onSavePersona(persona)
                                },
                                onDeleteCustom = { onDeleteCustomPersona(persona.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle Custom Persona Builder Button
                    Button(
                        onClick = { isCreatingPersona = !isCreatingPersona },
                        colors = ButtonDefaults.buttonColors(containerColor = ChatGptCardBg),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ChatGptBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isCreatingPersona) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            tint = ChatGptEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCreatingPersona) "Tutup Form Persona Kustom" else "+ Buat Persona Kustom Baru",
                            color = ChatGptEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isCreatingPersona) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ChatGptCardBg),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, ChatGptBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "FORM PERSONA KUSTOM",
                                    color = ChatGptTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                ApiKeyInputField(
                                    label = "Nama Persona",
                                    placeholder = "misal: Anak Kecil, Expert Coder, Dark Persona",
                                    value = newPersonaName,
                                    onValueChange = { newPersonaName = it }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                ApiKeyInputField(
                                    label = "Deskripsi Ciri Khas",
                                    placeholder = "Deskripsi singkat mengenai persona ini",
                                    value = newPersonaDesc,
                                    onValueChange = { newPersonaDesc = it }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                ApiKeyInputField(
                                    label = "Emoji Ekspresi",
                                    placeholder = "misal: ✨ 🤖 ⚡",
                                    value = newPersonaEmoji,
                                    onValueChange = { newPersonaEmoji = it }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "System Prompt & Gaya Bicara",
                                    color = ChatGptTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ChatGptInputBg)
                                        .border(0.5.dp, ChatGptBorder, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    BasicTextField(
                                        value = newPersonaPrompt,
                                        onValueChange = { newPersonaPrompt = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = ChatGptTextPrimary, fontSize = 12.sp),
                                        cursorBrush = SolidColor(ChatGptEmerald),
                                        decorationBox = { innerTextField ->
                                            if (newPersonaPrompt.isEmpty()) {
                                                Text(
                                                    text = "Jelaskan gaya bicaranya secara detail...",
                                                    color = ChatGptTextMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (newPersonaName.isNotBlank() && newPersonaPrompt.isNotBlank()) {
                                            onAddCustomPersona(newPersonaName, newPersonaDesc, newPersonaPrompt, newPersonaEmoji)
                                            newPersonaName = ""
                                            newPersonaDesc = ""
                                            newPersonaEmoji = ""
                                            newPersonaPrompt = ""
                                            isCreatingPersona = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald),
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Simpan Persona Kustom", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section Title: Central Server Configuration (Raw Config)
                    SectionHeader(title = "KONFIGURASI PUSAT SERVER (DEFAULT RAW)")
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ChatGptSelectedCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChatGptEmerald)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ChatGptEmerald.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ChatGptEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Konfigurasi Pusat Server (Raw Config)",
                                        color = ChatGptTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Muat Judul Model, Base URL Endpoint, Model ID, Provider Type, & API Key langsung dari pusat server",
                                        color = ChatGptTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val centralConfig = com.example.util.RawConfigManager.loadCentralConfig(context)
                                        if (centralConfig.centralGeminiApiKey.isNotBlank()) {
                                            geminiKeyText = centralConfig.centralGeminiApiKey
                                            onSaveApiKey(centralConfig.centralGeminiApiKey)
                                        }
                                        if (centralConfig.centralGroqApiKey.isNotBlank()) {
                                            groqKeyText = centralConfig.centralGroqApiKey
                                            onSaveGroqApiKey(centralConfig.centralGroqApiKey)
                                        }
                                        if (centralConfig.centralOpenRouterApiKey.isNotBlank()) {
                                            openRouterKeyText = centralConfig.centralOpenRouterApiKey
                                            onSaveOpenRouterApiKey(centralConfig.centralOpenRouterApiKey)
                                        }
                                        if (centralConfig.centralMistralApiKey.isNotBlank()) {
                                            mistralKeyText = centralConfig.centralMistralApiKey
                                            onSaveMistralApiKey(centralConfig.centralMistralApiKey)
                                        }

                                        val newCustomModels = centralConfig.toCustomAiModels()
                                        if (newCustomModels.isNotEmpty()) {
                                            val currentList = customModelsList.toMutableList()
                                            for (cm in newCustomModels) {
                                                val idx = currentList.indexOfFirst { it.id == cm.id }
                                                if (idx >= 0) currentList[idx] = cm else currentList.add(cm)
                                            }
                                            customModelsList = currentList
                                            onSaveCustomModels(currentList)
                                        }

                                        currentSelectedModel = centralConfig.defaultModel
                                        onSaveModel(centralConfig.defaultModel)

                                        android.widget.Toast.makeText(
                                            context,
                                            "Konfigurasi dari Pusat Berhasil Diterapkan ke Penyimpanan Lokal!",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald),
                                    shape = CircleShape,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Gunakan Konfigurasi dari Pusat",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                androidx.compose.material3.OutlinedButton(
                                    onClick = { showCentralModelDialog = true },
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ChatGptEmerald)
                                ) {
                                    Text(
                                        text = "Pilih Model...",
                                        color = ChatGptEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section Title: Model Engine Selection
                    SectionHeader(title = "ENGINE MODEL AI (OPSIONAL MAU GANTI MANUAL)")
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val builtInModels = listOf(
                            Triple("gemini-3.5-flash-lite", "gemini-3.5-flash-lite (Default)", "Google Ultra-Fast AI — Respon Kilat & Hemat Quota"),
                            Triple("gemini-3.6-flash", "gemini-3.6-flash", "Google Gemini 3.6 Flash — Generasi Terbaru"),
                            Triple("gemini-3.5-flash", "gemini-3.5-flash", "Google Fast AI (Standard)"),
                            Triple("gemini-3.1-pro-preview", "gemini-3.1-pro-preview", "Google Deep Reasoning & Coding AI"),
                            Triple("gemini-3.1-pro", "gemini-3.1-pro", "Google Gemini 3.1 Pro (Stable)"),
                            Triple("gemini-2.5-pro", "gemini-2.5-pro", "Google Gemini 2.5 Pro High Capacity"),
                            Triple("gemini-2.5-flash", "gemini-2.5-flash", "Google Gemini 2.5 Flash Speed"),
                            Triple("groq/llama-3.3-70b-versatile", "groq/llama-3.3-70b-versatile", "Groq Llama 3.3 (Perlu Groq API Key)"),
                            Triple("openrouter/anthropic/claude-3.5-sonnet", "openrouter/claude-3.5-sonnet", "OpenRouter Claude 3.5 (Perlu OpenRouter Key)"),
                            Triple("mistral/mistral-large-latest", "mistral/mistral-large-latest", "Mistral Large (Perlu Mistral Key)"),
                            Triple("pollinations/openai", "pollinations/openai (Free Global)", "Free fallback LLM engine (Tanpa API Key)")
                        )

                        builtInModels.forEach { (mId, mTitle, mSub) ->
                            ModelSelectionCard(
                                selected = currentSelectedModel == mId,
                                title = mTitle,
                                subtitle = mSub,
                                onClick = { currentSelectedModel = mId }
                            )
                        }

                        if (customModelsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "MODEL KUSTOM TAMBAHAN",
                                color = WormGptRedAccent,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            customModelsList.forEach { model ->
                                ModelSelectionCard(
                                    selected = currentSelectedModel == model.id,
                                    title = model.name,
                                    subtitle = "Custom ${model.providerType} (${model.id})",
                                    onClick = { currentSelectedModel = model.id },
                                    onEdit = { openAddCustomModelForm(model) },
                                    onDelete = {
                                        customModelsList = customModelsList.filter { it.id != model.id }
                                        onSaveCustomModels(customModelsList)
                                        if (currentSelectedModel == model.id) {
                                            currentSelectedModel = "gemini-3.5-flash-lite"
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { openAddCustomModelForm(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = ChatGptCardBg),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ChatGptBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = WormGptRedAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Tambah Model AI Baru",
                            color = ChatGptTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section Title: Voice Engine
                    SectionHeader(title = "ENGINE & VARIASI SUARA AI")
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ChatGptCardBg),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ChatGptBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "PILIH PROVIDER SUARA:",
                                color = ChatGptTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "gemini" to "Gemini",
                                    "openai" to "OpenAI",
                                    "elevenlabs" to "ElevenLabs",
                                    "google_cloud" to "G-Cloud"
                                ).forEach { (pKey, pLabel) ->
                                    val isSel = voiceProvider.equals(pKey, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .background(if (isSel) ChatGptEmerald else ChatGptInputBg)
                                            .border(0.5.dp, if (isSel) ChatGptEmerald else ChatGptBorder, CircleShape)
                                            .clickable {
                                                voiceProvider = pKey
                                                val firstVoice = com.example.util.AiVoiceCatalog.getVoicesForProvider(pKey).firstOrNull()?.id ?: ""
                                                voiceName = firstVoice
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pLabel,
                                            color = if (isSel) Color.White else ChatGptTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "DAFTAR SUARA (${voiceProvider.uppercase()}):",
                                color = ChatGptTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val availableVoices = com.example.util.AiVoiceCatalog.getVoicesForProvider(voiceProvider)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                availableVoices.forEach { voice ->
                                    val isSelectedVoice = voiceName.equals(voice.id, ignoreCase = true)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { voiceName = voice.id },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelectedVoice) ChatGptSelectedCardBg else ChatGptCardBg,
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.5.dp,
                                            if (isSelectedVoice) ChatGptEmerald else ChatGptBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelectedVoice,
                                                onClick = { voiceName = voice.id },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = ChatGptEmerald,
                                                    unselectedColor = ChatGptTextMuted
                                                ),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = voice.name,
                                                        color = ChatGptTextPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(ChatGptBorder)
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = if (voice.gender == "Male") "👨 Pria" else if (voice.gender == "Female") "👩 Wanita" else "🧑 Netral",
                                                            color = ChatGptTextSecondary,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = voice.description,
                                                    color = ChatGptTextSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (voiceProvider == "elevenlabs") {
                                Spacer(modifier = Modifier.height(10.dp))
                                ApiKeyInputField(
                                    label = "ElevenLabs API Key",
                                    placeholder = "xi-api-key...",
                                    value = elevenKeyText,
                                    isPassword = true,
                                    onValueChange = { elevenKeyText = it }
                                )
                            } else if (voiceProvider == "google_cloud") {
                                Spacer(modifier = Modifier.height(10.dp))
                                ApiKeyInputField(
                                    label = "Google Cloud TTS API Key",
                                    placeholder = "Google TTS API key...",
                                    value = googleTtsKeyText,
                                    isPassword = true,
                                    onValueChange = { googleTtsKeyText = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onTestVoice?.invoke(voiceProvider, voiceName, elevenKeyText, googleTtsKeyText)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald.copy(alpha = 0.15f)),
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = ChatGptEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Uji Suara Sekarang",
                                    color = ChatGptEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section Title: API Keys
                    SectionHeader(title = "KONFIGURASI API KEYS")
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ApiKeyInputField(
                            label = "Custom Gemini API Key",
                            placeholder = "AIzaSy...",
                            value = geminiKeyText,
                            onValueChange = { geminiKeyText = it }
                        )

                        ApiKeyInputField(
                            label = "Groq API Key (gsk_...)",
                            placeholder = "gsk_...",
                            value = groqKeyText,
                            onValueChange = { groqKeyText = it }
                        )

                        ApiKeyInputField(
                            label = "OpenRouter API Key (sk-or-...)",
                            placeholder = "sk-or-...",
                            value = openRouterKeyText,
                            onValueChange = { openRouterKeyText = it }
                        )

                        ApiKeyInputField(
                            label = "Mistral API Key",
                            placeholder = "Mistral API key...",
                            value = mistralKeyText,
                            onValueChange = { mistralKeyText = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Watermark Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = ChatGptCardBg,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ChatGptBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nova AI Modern Suite",
                                color = ChatGptTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Developed with ❤️ by Aizat",
                                color = ChatGptTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:aizatalamudinindonesia.plus@gmail.com")
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Nova AI Feedback & Support")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Gagal membuka email: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ChatGptInputBg),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, ChatGptBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = ChatGptEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send Feedback / Support",
                                    color = ChatGptTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar (Cancel & Save Pills)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ChatGptCardBg),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, ChatGptBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Batal", color = ChatGptTextSecondary, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            onSaveApiKey(geminiKeyText)
                            onSaveGroqApiKey(groqKeyText)
                            onSaveOpenRouterApiKey(openRouterKeyText)
                            onSaveMistralApiKey(mistralKeyText)
                            onSaveModel(currentSelectedModel)
                            onSaveCustomModels(customModelsList)
                            onSavePersona(selectedPersona)
                            onSaveVoiceProvider(voiceProvider)
                            onSaveVoiceName(voiceName)
                            onSaveElevenLabsApiKey(elevenKeyText)
                            onSaveGoogleTtsApiKey(googleTtsKeyText)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald),
                        shape = CircleShape,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text(text = "Simpan Pengaturan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dynamic Add / Edit Custom Model Dialog
    if (showAddCustomModelDialog) {
        Dialog(onDismissRequest = { showAddCustomModelDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ChatGptLightBg),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (editingModelItem == null) "Tambah Model AI Baru" else "Edit Model AI",
                        color = ChatGptTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Konfigurasi model AI kustom ke daftar pilihan utama.",
                        color = ChatGptTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ApiKeyInputField(
                        label = "Nama Model / Display Name *",
                        placeholder = "Contoh: Custom Llama 3.3",
                        value = newModelName,
                        onValueChange = { newModelName = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApiKeyInputField(
                        label = "Model ID / Endpoint *",
                        placeholder = "Contoh: groq/llama-3.3-70b",
                        value = newModelId,
                        onValueChange = { newModelId = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Provider Type *",
                        color = ChatGptTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Groq", "OpenRouter", "Mistral", "Gemini", "OpenAI", "Custom").forEach { prov ->
                            val selected = newModelProvider.equals(prov, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) ChatGptEmerald else ChatGptCardBg)
                                    .border(0.5.dp, if (selected) ChatGptEmerald else ChatGptBorder, CircleShape)
                                    .clickable { newModelProvider = prov }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(text = prov, color = if (selected) Color.White else ChatGptTextSecondary, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    if (newModelProvider.equals("Custom", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ApiKeyInputField(
                            label = "Nama Provider Kustom *",
                            placeholder = "Contoh: Blackbox, Together, Ollama, DeepSeek",
                            value = newCustomProviderName,
                            onValueChange = { newCustomProviderName = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        ApiKeyInputField(
                            label = "Custom Base URL / Endpoint URL *",
                            placeholder = "Contoh: https://api.blackbox.ai/v1/ atau http://localhost:11434/v1/",
                            value = newModelBaseUrl,
                            onValueChange = { newModelBaseUrl = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ApiKeyInputField(
                        label = "Nama Model / Display Name *",
                        placeholder = "Contoh: Custom Blackbox DeepSeek R1",
                        value = newModelName,
                        onValueChange = { newModelName = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApiKeyInputField(
                        label = "Model ID / Endpoint *",
                        placeholder = "Contoh: blackbox-deepseek-r1 / groq/llama-3.3-70b",
                        value = newModelId,
                        onValueChange = { newModelId = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApiKeyInputField(
                        label = "Custom Base URL / Endpoint URL",
                        placeholder = "https://api.blackbox.ai/v1/ atau http://localhost:11434/v1/",
                        value = newModelBaseUrl,
                        onValueChange = { newModelBaseUrl = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApiKeyInputField(
                        label = "Custom API Key (Opsional)",
                        placeholder = "API Key kustom (kosongkan jika publik/lokal)",
                        value = newModelKey,
                        onValueChange = { newModelKey = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddCustomModelDialog = false }) {
                            Text(text = "Batal", color = ChatGptTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newModelId.isNotBlank() && newModelName.isNotBlank()) {
                                    val finalProvider = if (newModelProvider.equals("Custom", ignoreCase = true)) {
                                        newCustomProviderName.trim().ifBlank { "Custom" }
                                    } else {
                                        newModelProvider.trim()
                                    }
                                    val updatedModel = CustomAiModel(
                                        id = newModelId.trim(),
                                        name = newModelName.trim(),
                                        apiKey = newModelKey.trim(),
                                        providerType = finalProvider,
                                        baseUrl = newModelBaseUrl.trim()
                                    )
                                    val newList = if (editingModelItem != null) {
                                        customModelsList.map { if (it.id == editingModelItem!!.id) updatedModel else it }
                                    } else {
                                        customModelsList.filter { it.id != updatedModel.id } + updatedModel
                                    }
                                    customModelsList = newList
                                    onSaveCustomModels(newList)
                                    currentSelectedModel = updatedModel.id
                                    onSaveModel(updatedModel.id)
                                    showAddCustomModelDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald),
                            shape = CircleShape
                        ) {
                            Text(text = "Simpan Model", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Owner Login Dialog
    if (showOwnerLogin) {
        Dialog(onDismissRequest = { showOwnerLogin = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ChatGptLightBg)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = ChatGptEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Owner Verification",
                            color = ChatGptTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Masukkan password owner untuk membuka Pengaturan Khusus Owner.",
                        color = ChatGptTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    ApiKeyInputField(
                        label = "Owner Password",
                        placeholder = "Masukkan password...",
                        value = ownerPasswordInput,
                        isPassword = true,
                        onValueChange = { ownerPasswordInput = it; ownerLoginError = false }
                    )
                    if (ownerLoginError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "❌ Password salah!",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showOwnerLogin = false }) {
                            Text(text = "Batal", color = ChatGptTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (ownerPasswordInput.trim() == "AizatDev123") {
                                    showOwnerLogin = false
                                    showOwnerSettings = true
                                    ownerPasswordInput = ""
                                    ownerLoginError = false
                                } else {
                                    ownerLoginError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald),
                            shape = CircleShape
                        ) {
                            Text(text = "Login Owner", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Owner Settings Dialog
    if (showOwnerSettings) {
        Dialog(onDismissRequest = { showOwnerSettings = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ChatGptLightBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "🛠 Owner Settings & Multi-Model Config",
                        color = ChatGptTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Protected Owner Mode — Synchronized API Keys & Custom Models",
                        color = ChatGptTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ChatGptSelectedCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChatGptEmerald)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(ChatGptEmerald.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ChatGptEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Gunakan API Key dari Pusat Server",
                                    color = ChatGptTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showCentralModelDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald),
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Gunakan Key & Model Server Pusat",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader(title = "SYNCHRONIZED API KEYS")
                    Spacer(modifier = Modifier.height(8.dp))
                    ApiKeyInputField("Gemini API Key", "AIzaSy...", geminiKeyText) { geminiKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("Groq API Key", "gsk_...", groqKeyText) { groqKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("OpenRouter API Key", "sk-or-...", openRouterKeyText) { openRouterKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("Mistral API Key", "Mistral key...", mistralKeyText) { mistralKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("ElevenLabs API Key", "xi-api-key...", elevenKeyText, isPassword = true) { elevenKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("Google Cloud TTS Key", "Google TTS key...", googleTtsKeyText, isPassword = true) { googleTtsKeyText = it }

                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader(title = "MULTIMODEL AI CONFIGURATION")
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ChatGptCardBg)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Tambah Provider & Model AI Kustom",
                                color = ChatGptTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Provider Type *",
                                color = ChatGptTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Groq", "OpenRouter", "Mistral", "Gemini", "OpenAI", "Custom").forEach { prov ->
                                    val selected = newModelProvider.equals(prov, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (selected) ChatGptEmerald else ChatGptCardBg)
                                            .border(0.5.dp, if (selected) ChatGptEmerald else ChatGptBorder, CircleShape)
                                            .clickable { newModelProvider = prov }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = prov,
                                            color = if (selected) Color.White else ChatGptTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            if (newModelProvider.equals("Custom", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                ApiKeyInputField("Nama Provider Kustom *", "Contoh: Blackbox, Together, Ollama", newCustomProviderName) { newCustomProviderName = it }
                                Spacer(modifier = Modifier.height(6.dp))
                                ApiKeyInputField("Base URL / Endpoint URL *", "Contoh: https://api.blackbox.ai/v1/ atau http://localhost:11434/v1/", newModelBaseUrl) { newModelBaseUrl = it }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            ApiKeyInputField("Display Name", "e.g. Custom Blackbox DeepSeek R1", newModelName) { newModelName = it }
                            Spacer(modifier = Modifier.height(6.dp))
                            ApiKeyInputField("Model ID / Endpoint", "e.g. blackbox-deepseek-r1 / groq/llama-3.1-8b", newModelId) { newModelId = it }
                            Spacer(modifier = Modifier.height(6.dp))
                            ApiKeyInputField("Base URL (Endpoint URL)", "https://api.blackbox.ai/v1/...", newModelBaseUrl) { newModelBaseUrl = it }
                            Spacer(modifier = Modifier.height(6.dp))
                            ApiKeyInputField("Model API Key (Opsional)", "API Key kustom...", newModelKey) { newModelKey = it }
                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (newModelId.isNotBlank() && newModelName.isNotBlank()) {
                                        val finalProvider = if (newModelProvider.equals("Custom", ignoreCase = true)) {
                                            newCustomProviderName.trim().ifBlank { "Custom" }
                                        } else {
                                            newModelProvider.trim()
                                        }
                                        val newModel = CustomAiModel(
                                            id = newModelId.trim(),
                                            name = newModelName.trim(),
                                            apiKey = newModelKey.trim(),
                                            providerType = finalProvider,
                                            baseUrl = newModelBaseUrl.trim()
                                        )
                                        customModelsList = customModelsList + newModel
                                        onSaveCustomModels(customModelsList)
                                        newModelId = ""
                                        newModelName = ""
                                        newModelKey = ""
                                        newModelBaseUrl = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald),
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "+ Simpan & Tambah Model Kustom", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onSaveApiKey(geminiKeyText)
                                onSaveGroqApiKey(groqKeyText)
                                onSaveOpenRouterApiKey(openRouterKeyText)
                                onSaveMistralApiKey(mistralKeyText)
                                onSaveCustomModels(customModelsList)
                                showOwnerSettings = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald),
                            shape = CircleShape
                        ) {
                            Text(text = "Apply Owner Settings", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showCentralModelDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val centralConfig = remember { com.example.util.RawConfigManager.loadCentralConfig(context) }
        var selectedCentralModelId by remember { mutableStateOf(centralConfig.defaultModel) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showCentralModelDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ChatGptLightBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ChatGptEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ChatGptEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Model AI Server Pusat",
                                color = ChatGptTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { showCentralModelDialog = false },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ChatGptInputBg)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ChatGptTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gunakan API Key bawaan dari file raw developer (res/raw/developer_config) & pilih model server yang ingin diaktifkan:",
                        color = ChatGptTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    centralConfig.availableCentralModels.forEach { centralModel ->
                        val isSelected = selectedCentralModelId == centralModel.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedCentralModelId = centralModel.id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ChatGptSelectedCardBg else ChatGptCardBg,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) ChatGptEmerald else ChatGptBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedCentralModelId = centralModel.id },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ChatGptEmerald,
                                        unselectedColor = ChatGptTextMuted
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = centralModel.name,
                                        color = ChatGptTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = centralModel.description,
                                        color = ChatGptTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (centralConfig.centralGeminiApiKey.isNotBlank()) {
                                geminiKeyText = centralConfig.centralGeminiApiKey
                                onSaveApiKey(centralConfig.centralGeminiApiKey)
                            }
                            if (centralConfig.centralGroqApiKey.isNotBlank()) {
                                groqKeyText = centralConfig.centralGroqApiKey
                                onSaveGroqApiKey(centralConfig.centralGroqApiKey)
                            }
                            if (centralConfig.centralOpenRouterApiKey.isNotBlank()) {
                                openRouterKeyText = centralConfig.centralOpenRouterApiKey
                                onSaveOpenRouterApiKey(centralConfig.centralOpenRouterApiKey)
                            }
                            if (centralConfig.centralMistralApiKey.isNotBlank()) {
                                mistralKeyText = centralConfig.centralMistralApiKey
                                onSaveMistralApiKey(centralConfig.centralMistralApiKey)
                            }

                            val newCustomModels = centralConfig.toCustomAiModels()
                            if (newCustomModels.isNotEmpty()) {
                                val currentList = customModelsList.toMutableList()
                                for (cm in newCustomModels) {
                                    val idx = currentList.indexOfFirst { it.id == cm.id }
                                    if (idx >= 0) currentList[idx] = cm else currentList.add(cm)
                                }
                                customModelsList = currentList
                                onSaveCustomModels(currentList)
                            }

                            currentSelectedModel = selectedCentralModelId
                            onSaveModel(selectedCentralModelId)

                            android.widget.Toast.makeText(
                                context,
                                "API Key & Model Pusat ($selectedCentralModelId) Berhasil Diterapkan ke Penyimpanan Lokal!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()

                            showCentralModelDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChatGptEmerald),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Terapkan Model & API Key Pusat",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = ChatGptTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun PersonaSelectionCard(
    persona: ChatPersona,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDeleteCustom: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) ChatGptSelectedCardBg else ChatGptCardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) ChatGptEmerald else ChatGptBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) ChatGptEmerald else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (isSelected) ChatGptEmerald else ChatGptTextMuted,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = persona.sampleEmoji,
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 6.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = persona.name,
                        color = ChatGptTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (persona.isCustom) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ChatGptEmerald.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Kustom",
                                color = ChatGptEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = persona.description,
                    color = ChatGptTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (persona.isCustom) {
                IconButton(
                    onClick = onDeleteCustom,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Persona",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModelRadioOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ModelSelectionCard(
        selected = selected,
        title = title,
        subtitle = subtitle,
        onClick = onClick
    )
}

@Composable
private fun ModelSelectionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) ChatGptSelectedCardBg else ChatGptCardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) ChatGptEmerald else ChatGptBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = ChatGptEmerald,
                    unselectedColor = ChatGptTextMuted
                ),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = ChatGptTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = ChatGptTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = ChatGptTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ApiKeyInputField(
    label: String,
    placeholder: String,
    value: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = ChatGptTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ChatGptInputBg)
                .border(0.5.dp, ChatGptBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = ChatGptTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(ChatGptEmerald),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = ChatGptTextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
