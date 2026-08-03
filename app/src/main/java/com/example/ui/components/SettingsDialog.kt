package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.model.ChatPersona
import com.example.data.model.CustomAiModel
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptRedAccent

@Composable
fun SettingsDialog(
    customApiKey: String,
    groqApiKey: String = "",
    openRouterApiKey: String = "",
    mistralApiKey: String = "",
    selectedModel: String,
    customModels: List<CustomAiModel> = emptyList(),
    currentPersona: ChatPersona = ChatPersona.ALL_PERSONAS[0],
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
    onSaveVoiceProvider: (String) -> Unit = {},
    onSaveVoiceName: (String) -> Unit = {},
    onSaveElevenLabsApiKey: (String) -> Unit = {},
    onSaveGoogleTtsApiKey: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var geminiKeyText by remember { mutableStateOf(customApiKey) }
    var groqKeyText by remember { mutableStateOf(groqApiKey) }
    var openRouterKeyText by remember { mutableStateOf(openRouterApiKey) }
    var mistralKeyText by remember { mutableStateOf(mistralApiKey) }
    var currentSelectedModel by remember { mutableStateOf(selectedModel) }
    var customModelsList by remember { mutableStateOf(customModels) }
    var selectedPersona by remember { mutableStateOf(currentPersona) }

    // Voice Settings States
    var voiceProvider by remember { mutableStateOf(selectedVoiceProvider) }
    var voiceName by remember { mutableStateOf(selectedVoiceName) }
    var elevenKeyText by remember { mutableStateOf(elevenLabsApiKey) }
    var googleTtsKeyText by remember { mutableStateOf(googleTtsApiKey) }

    var showOwnerLogin by remember { mutableStateOf(false) }
    var showOwnerSettings by remember { mutableStateOf(false) }
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

    val scrollState = rememberScrollState()

    fun openAddCustomModelForm(modelToEdit: CustomAiModel? = null) {
        editingModelItem = modelToEdit
        if (modelToEdit != null) {
            newModelId = modelToEdit.id
            newModelName = modelToEdit.name
            newModelKey = modelToEdit.apiKey
            newModelBaseUrl = modelToEdit.baseUrl
            newModelProvider = modelToEdit.providerType
        } else {
            newModelId = ""
            newModelName = ""
            newModelKey = ""
            newModelBaseUrl = ""
            newModelProvider = "Groq"
        }
        showAddCustomModelDialog = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF18181B))
                .border(1.dp, WormGptBorderRed, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = WormGptRedAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "WormGPT Multi-AI System Config",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Owner Settings Protected Access Button
                Button(
                    onClick = { showOwnerLogin = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = WormGptRedAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔒 Owner Settings",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Default secret status
                val isSecretInjected = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF09090B))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isSecretInjected || geminiKeyText.isNotBlank()) Color(0xFF10B981) else Color(0xFFE11D48),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSecretInjected || geminiKeyText.isNotBlank()) "Global Gemini Key Active" else "Global Gemini Key Missing (Optional if using Free/Other AI)",
                        color = if (isSecretInjected || geminiKeyText.isNotBlank()) Color(0xFF10B981) else Color(0xFFF43F5E),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chat Persona / Style Selector Section
                Text(
                    text = "GAYA OBROLAN & PERSONA AI",
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF09090B))
                        .padding(8.dp)
                ) {
                    ChatPersona.ALL_PERSONAS.forEach { persona ->
                        val isSelected = persona.id == selectedPersona.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF27272A) else Color.Transparent)
                                .clickable {
                                    selectedPersona = persona
                                    onSavePersona(persona)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedPersona = persona
                                    onSavePersona(persona)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = WormGptRedAccent,
                                    unselectedColor = Color(0xFF71717A)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = persona.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = persona.description,
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = persona.sampleEmoji,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Model Selection Section
                Text(
                    text = "TARGET MODEL ENGINE",
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF09090B))
                        .padding(8.dp)
                ) {
                    ModelRadioOption(
                        selected = currentSelectedModel == "gemini-3.5-flash",
                        title = "gemini-3.5-flash",
                        subtitle = "Google Fast AI (Global/Custom Key)",
                        onClick = { currentSelectedModel = "gemini-3.5-flash" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "gemini-3.1-pro-preview",
                        title = "gemini-3.1-pro-preview",
                        subtitle = "Google Deep Reasoning AI",
                        onClick = { currentSelectedModel = "gemini-3.1-pro-preview" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "groq/llama-3.3-70b-versatile",
                        title = "groq/llama-3.3-70b-versatile",
                        subtitle = "Groq Llama 3.3 (Requires Groq API Key)",
                        onClick = { currentSelectedModel = "groq/llama-3.3-70b-versatile" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "openrouter/anthropic/claude-3.5-sonnet",
                        title = "openrouter/claude-3.5-sonnet",
                        subtitle = "OpenRouter Claude 3.5 (Requires OpenRouter Key)",
                        onClick = { currentSelectedModel = "openrouter/anthropic/claude-3.5-sonnet" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "mistral/mistral-large-latest",
                        title = "mistral/mistral-large-latest",
                        subtitle = "Mistral Large (Requires Mistral Key)",
                        onClick = { currentSelectedModel = "mistral/mistral-large-latest" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "pollinations/openai",
                        title = "pollinations/openai (Free Global)",
                        subtitle = "Free fallback LLM engine (No API Key needed)",
                        onClick = { currentSelectedModel = "pollinations/openai" }
                    )

                    // Render custom models in selection list
                    if (customModelsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF27272A))
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "CUSTOM ADDED MODELS",
                            color = WormGptRedAccent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )

                        customModelsList.forEach { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (currentSelectedModel == model.id) Color(0xFF18181B) else Color.Transparent)
                                    .padding(end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModelRadioOption(
                                        selected = currentSelectedModel == model.id,
                                        title = model.name,
                                        subtitle = "Custom ${model.providerType} (${model.id})${if (model.apiKey.isNotBlank()) " • Key Set" else ""}",
                                        onClick = { currentSelectedModel = model.id }
                                    )
                                }
                                IconButton(
                                    onClick = { openAddCustomModelForm(model) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Model",
                                        tint = Color(0xFFA1A1AA),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        customModelsList = customModelsList.filter { it.id != model.id }
                                        onSaveCustomModels(customModelsList)
                                        if (currentSelectedModel == model.id) {
                                            currentSelectedModel = "gemini-3.5-flash"
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Model",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // "Tambah Model Baru" Button
                    Button(
                        onClick = { openAddCustomModelForm(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = WormGptRedAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "➕ Tambah Model AI Baru",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Engine & Voice Variant Selection
                Text(
                    text = "🔊 ENGINE SUARA AI & VARIASI SUARA NATURAL",
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF09090B))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "PILIH LAYANAN SUARA AI:",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "gemini" to "Gemini",
                            "openai" to "OpenAI",
                            "elevenlabs" to "ElevenLabs",
                            "google_cloud" to "G-Cloud",
                            "system" to "System HP"
                        ).forEach { (pKey, pLabel) ->
                            val isSel = voiceProvider.equals(pKey, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) WormGptRedAccent else Color(0xFF18181B))
                                    .clickable {
                                        voiceProvider = pKey
                                        val firstVoice = com.example.util.AiVoiceCatalog.getVoicesForProvider(pKey).firstOrNull()?.id ?: ""
                                        voiceName = firstVoice
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pLabel,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "VARIASI SUARA AI (${voiceProvider.uppercase()}):",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val availableVoices = com.example.util.AiVoiceCatalog.getVoicesForProvider(voiceProvider)
                    availableVoices.forEach { voice ->
                        val isSelectedVoice = voiceName.equals(voice.id, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelectedVoice) Color(0xFF27272A) else Color.Transparent)
                                .clickable { voiceName = voice.id }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelectedVoice,
                                onClick = { voiceName = voice.id },
                                colors = RadioButtonDefaults.colors(selectedColor = WormGptRedAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = voice.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (voice.gender == "Male") Color(0xFF1E3A8A) else if (voice.gender == "Female") Color(0xFF831843) else Color(0xFF3F3F46))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (voice.gender == "Male") "👨 Pria" else if (voice.gender == "Female") "👩 Wanita" else "🧑 Netral",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Text(
                                    text = voice.description,
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    if (voiceProvider == "elevenlabs") {
                        Spacer(modifier = Modifier.height(8.dp))
                        ApiKeyInputField(
                            label = "ELEVENLABS API KEY",
                            placeholder = "xi-api-key...",
                            value = elevenKeyText,
                            isPassword = true,
                            onValueChange = { elevenKeyText = it }
                        )
                    } else if (voiceProvider == "google_cloud") {
                        Spacer(modifier = Modifier.height(8.dp))
                        ApiKeyInputField(
                            label = "GOOGLE CLOUD TTS API KEY",
                            placeholder = "Google TTS API key...",
                            value = googleTtsKeyText,
                            isPassword = true,
                            onValueChange = { googleTtsKeyText = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Key Inputs Section
                Text(
                    text = "API KEYS CONFIGURATION",
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyInputField(
                    label = "CUSTOM GEMINI API KEY",
                    placeholder = "AIzaSy...",
                    value = geminiKeyText,
                    onValueChange = { geminiKeyText = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyInputField(
                    label = "GROQ API KEY (gsk_...)",
                    placeholder = "gsk_...",
                    value = groqKeyText,
                    onValueChange = { groqKeyText = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyInputField(
                    label = "OPENROUTER API KEY (sk-or-...)",
                    placeholder = "sk-or-...",
                    value = openRouterKeyText,
                    onValueChange = { openRouterKeyText = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyInputField(
                    label = "MISTRAL API KEY",
                    placeholder = "Mistral API key...",
                    value = mistralKeyText,
                    onValueChange = { mistralKeyText = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", color = Color(0xFFA1A1AA))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                        colors = ButtonDefaults.buttonColors(containerColor = WormGptRedAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Save Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Watermark & Feedback
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF09090B))
                        .border(0.5.dp, Color(0xFF27272A), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aizat",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "tiktok: itsmee_aizat | github: itsmeeaizat",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:aizatalamudinindonesia.plus@gmail.com")
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "WormGPT AI Feedback & Support")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Gagal membuka email: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = WormGptRedAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Feedback / Email",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Dynamic Add / Edit Custom Model Dialog
    if (showAddCustomModelDialog) {
        Dialog(onDismissRequest = { showAddCustomModelDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF18181B))
                    .border(1.dp, WormGptBorderRed, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = if (editingModelItem == null) "➕ Tambah Model AI Baru" else "✏️ Edit Model AI",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Konfigurasi model AI kustom yang akan ditambahkan ke daftar pilihan utama.",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ApiKeyInputField(
                        label = "NAMA MODEL / DISPLAY NAME *",
                        placeholder = "Contoh: Custom Llama 3.3, Private Model",
                        value = newModelName,
                        onValueChange = { newModelName = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApiKeyInputField(
                        label = "MODEL ID / ENDPOINT *",
                        placeholder = "Contoh: groq/llama-3.3-70b, deepseek-chat",
                        value = newModelId,
                        onValueChange = { newModelId = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "PROVIDER TYPE *",
                        color = Color(0xFFA1A1AA),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Groq", "OpenRouter", "Mistral", "Gemini", "OpenAI / Custom").forEach { prov ->
                            val selected = newModelProvider.equals(prov, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) WormGptRedAccent else Color(0xFF09090B))
                                    .border(0.5.dp, if (selected) WormGptRedAccent else Color(0xFF3F3F46), RoundedCornerShape(6.dp))
                                    .clickable { newModelProvider = prov }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(text = prov, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ApiKeyInputField(
                        label = "CUSTOM API KEY (OPSIONAL)",
                        placeholder = "Biarkan kosong jika memakai API Key utama",
                        value = newModelKey,
                        onValueChange = { newModelKey = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApiKeyInputField(
                        label = "CUSTOM BASE URL / ENDPOINT (OPSIONAL)",
                        placeholder = "Contoh: https://api.openai.com/v1/chat/completions",
                        value = newModelBaseUrl,
                        onValueChange = { newModelBaseUrl = it }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddCustomModelDialog = false }) {
                            Text(text = "Batal", color = Color(0xFFA1A1AA))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newModelId.isNotBlank() && newModelName.isNotBlank()) {
                                    val updatedModel = CustomAiModel(
                                        id = newModelId.trim(),
                                        name = newModelName.trim(),
                                        apiKey = newModelKey.trim(),
                                        providerType = newModelProvider,
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
                            colors = ButtonDefaults.buttonColors(containerColor = WormGptRedAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Simpan Model", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Owner Login Dialog (Protected by password AizatDev123)
    if (showOwnerLogin) {
        Dialog(onDismissRequest = { showOwnerLogin = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF18181B))
                    .border(1.dp, WormGptBorderRed, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "🔒 Owner Password Verification",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter owner password to access Owner Settings & Dynamic Multi-Model AI.",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ApiKeyInputField(
                        label = "OWNER PASSWORD",
                        placeholder = "Enter password...",
                        value = ownerPasswordInput,
                        isPassword = true,
                        onValueChange = { ownerPasswordInput = it; ownerLoginError = false }
                    )
                    if (ownerLoginError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "❌ Password salah!",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showOwnerLogin = false }) {
                            Text(text = "Cancel", color = Color(0xFFA1A1AA))
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
                            colors = ButtonDefaults.buttonColors(containerColor = WormGptRedAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Login", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Owner Settings Dialog (Synchronized API Keys & Dynamic Multi-Model AI)
    if (showOwnerSettings) {
        Dialog(onDismissRequest = { showOwnerSettings = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF18181B))
                    .border(1.dp, WormGptBorderRed, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "🛠 Owner Settings & Dynamic Multi-Model AI",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Protected Owner Mode — Synchronized API Keys & Custom Models",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SYNCHRONIZED API KEYS",
                        color = Color(0xFFA1A1AA),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("GEMINI API KEY", "AIzaSy...", geminiKeyText) { geminiKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("GROQ API KEY", "gsk_...", groqKeyText) { groqKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("OPENROUTER API KEY", "sk-or-...", openRouterKeyText) { openRouterKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("MISTRAL API KEY", "Mistral key...", mistralKeyText) { mistralKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("ELEVENLABS API KEY", "xi-api-key...", elevenKeyText, isPassword = true) { elevenKeyText = it }
                    Spacer(modifier = Modifier.height(6.dp))
                    ApiKeyInputField("GOOGLE CLOUD TTS API KEY", "Google TTS key...", googleTtsKeyText, isPassword = true) { googleTtsKeyText = it }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "DYNAMIC MULTI-MODEL AI CONFIGURATION",
                        color = Color(0xFFA1A1AA),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Add Custom Model Form in Owner Settings
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF09090B))
                            .border(0.5.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Add New Custom AI Model",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ApiKeyInputField("MODEL ID / ENDPOINT (e.g. groq/llama-3.1-8b)", "Model ID...", newModelId) { newModelId = it }
                        Spacer(modifier = Modifier.height(6.dp))
                        ApiKeyInputField("DISPLAY NAME (e.g. Custom Llama)", "Display name...", newModelName) { newModelName = it }
                        Spacer(modifier = Modifier.height(6.dp))
                        ApiKeyInputField("MODEL API KEY (OPTIONAL)", "API Key for this model...", newModelKey) { newModelKey = it }
                        Spacer(modifier = Modifier.height(6.dp))
                        ApiKeyInputField("BASE URL / ENDPOINT (OPTIONAL)", "https://...", newModelBaseUrl) { newModelBaseUrl = it }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "PROVIDER TYPE", color = Color(0xFFA1A1AA), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Groq", "OpenRouter", "Mistral", "Gemini", "OpenAI").forEach { prov ->
                                val selected = newModelProvider == prov
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) WormGptRedAccent else Color(0xFF18181B))
                                        .border(0.5.dp, if (selected) WormGptRedAccent else Color(0xFF3F3F46), RoundedCornerShape(6.dp))
                                        .clickable { newModelProvider = prov }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(text = prov, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (newModelId.isNotBlank() && newModelName.isNotBlank()) {
                                    val newModel = CustomAiModel(
                                        id = newModelId.trim(),
                                        name = newModelName.trim(),
                                        apiKey = newModelKey.trim(),
                                        providerType = newModelProvider,
                                        baseUrl = newModelBaseUrl.trim()
                                    )
                                    customModelsList = customModelsList + newModel
                                    newModelId = ""
                                    newModelName = ""
                                    newModelKey = ""
                                    newModelBaseUrl = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WormGptRedAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "➕ Add Custom AI Model", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "ADDED CUSTOM MODELS (${customModelsList.size})",
                        color = Color(0xFFA1A1AA),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (customModelsList.isEmpty()) {
                        Text(
                            text = "No custom models added yet. Use the form above to add models dynamically.",
                            color = Color(0xFF52525B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        customModelsList.forEach { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF09090B))
                                    .border(0.5.dp, Color(0xFF27272A), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = model.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(text = "ID: ${model.id} | Provider: ${model.providerType}", color = Color(0xFFA1A1AA), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                TextButton(
                                    onClick = {
                                        customModelsList = customModelsList.filter { it.id != model.id }
                                    }
                                ) {
                                    Text(text = "🗑 Delete", color = Color(0xFFEF4444), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
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
                            colors = ButtonDefaults.buttonColors(containerColor = WormGptRedAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Apply Owner Settings", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = WormGptRedAccent)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = Color(0xFF71717A),
                fontSize = 11.sp
            )
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
                color = Color(0xFFA1A1AA),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF09090B))
                .border(0.5.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(WormGptRedAccent),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF52525B),
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
