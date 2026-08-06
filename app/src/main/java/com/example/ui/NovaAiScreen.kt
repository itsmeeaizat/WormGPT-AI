package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AttachmentMenuSheet
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.FileInspectorPreviewCard
import com.example.ui.components.HeaderBar
import com.example.ui.components.HistoryDrawer
import com.example.ui.components.ModeSelectorSheet
import com.example.ui.components.PermissionsDialog
import com.example.ui.components.PersonaChipsBar
import com.example.ui.components.PersonaSelectorSheet
import com.example.ui.components.QuickPromptsBar
import com.example.ui.components.QuoteReplyPreviewCard
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptRedAccent
import com.example.ui.theme.WormGptRedDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaAiScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val allPersonas by viewModel.allPersonas.collectAsStateWithLifecycle()
    val currentPersona by viewModel.currentPersona.collectAsStateWithLifecycle()
    val inputPrompt by viewModel.inputPrompt.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val groqApiKey by viewModel.groqApiKey.collectAsStateWithLifecycle()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsStateWithLifecycle()
    val mistralApiKey by viewModel.mistralApiKey.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val customModels by viewModel.customModels.collectAsStateWithLifecycle()
    val attachedFile by viewModel.attachedFile.collectAsStateWithLifecycle()
    val isFileProcessing by viewModel.isFileProcessing.collectAsStateWithLifecycle()
    val fileProgressText by viewModel.fileProgressText.collectAsStateWithLifecycle()
    val replyingToMessage by viewModel.replyingToMessage.collectAsStateWithLifecycle()

    // Voice Engine States
    val selectedVoiceProvider by viewModel.selectedVoiceProvider.collectAsStateWithLifecycle()
    val selectedVoiceName by viewModel.selectedVoiceName.collectAsStateWithLifecycle()
    val elevenLabsApiKey by viewModel.elevenLabsApiKey.collectAsStateWithLifecycle()
    val googleTtsApiKey by viewModel.googleTtsApiKey.collectAsStateWithLifecycle()
    val voiceSpeed by viewModel.voiceSpeed.collectAsStateWithLifecycle()
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        com.example.util.NotificationHelper.createNotificationChannel(context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFile(context, uri)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFile(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.attachBitmap(bitmap)
        } else {
            val fallback = com.example.util.FilePickerHelper.createFallbackCameraBitmap()
            viewModel.attachBitmap(fallback)
            Toast.makeText(context, "Snapshot kamera berhasil terlampir.", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraOrFallback(context, cameraLauncher, viewModel)
        } else {
            Toast.makeText(context, "Izin kamera ditolak. Silakan izinkan di menu Permissions.", Toast.LENGTH_LONG).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            scanAndAttachLocation(context, viewModel)
        } else {
            Toast.makeText(context, "Izin lokasi ditolak. Menggunakan telemetry default.", Toast.LENGTH_SHORT).show()
            viewModel.attachLocationScan(
                latitude = -6.2088,
                longitude = 106.8456,
                accuracy = 15.0f,
                altitude = 10.0,
                provider = "Default Fallback",
                address = "Jakarta, Indonesia (Fallback)"
            )
        }
    }

    val sttLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                viewModel.inputPrompt.value = matches[0]
            }
        }
    }

    var showModeSheet by remember { mutableStateOf(false) }
    var showPersonaSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAttachmentMenuSheet by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showLiveVoiceOverlay by remember { mutableStateOf(false) }

    // AI Voice Manager Setup
    var ttsEnabled by remember { mutableStateOf(false) }
    var currentlySpeakingMessageId by remember { mutableStateOf<Long?>(null) }
    val aiVoiceManager = remember(context) { com.example.util.AiVoiceManager(context) }
    val isVoiceSpeaking by aiVoiceManager.isSpeaking.collectAsStateWithLifecycle()
    val isVoiceLoadingAudio by aiVoiceManager.isLoadingAudio.collectAsStateWithLifecycle()

    LaunchedEffect(isVoiceSpeaking) {
        if (!isVoiceSpeaking) {
            currentlySpeakingMessageId = null
        }
    }

    androidx.compose.runtime.DisposableEffect(aiVoiceManager) {
        onDispose {
            aiVoiceManager.destroy()
        }
    }

    val speakAiResponse: (String) -> Unit = { text ->
        if (ttsEnabled && text.isNotBlank()) {
            aiVoiceManager.speak(
                text = text,
                provider = selectedVoiceProvider,
                voiceName = selectedVoiceName,
                elevenLabsKey = elevenLabsApiKey,
                googleCloudKey = googleTtsApiKey,
                geminiApiKey = customApiKey,
                openAiApiKey = openRouterApiKey,
                speed = voiceSpeed,
                pitch = voicePitch
            )
        }
    }

    val handleSendMessage: (String?) -> Unit = { overridePrompt ->
        if (ttsEnabled) {
            aiVoiceManager.startStreamingSpeech(
                provider = selectedVoiceProvider,
                voiceName = selectedVoiceName,
                elevenLabsKey = elevenLabsApiKey,
                googleCloudKey = googleTtsApiKey,
                geminiApiKey = customApiKey,
                openAiApiKey = openRouterApiKey,
                speed = voiceSpeed,
                pitch = voicePitch
            )
        }
        viewModel.sendMessage(
            overridePrompt = overridePrompt,
            onChunkReceived = { chunk ->
                if (ttsEnabled) {
                    aiVoiceManager.offerStreamTextChunk(chunk)
                }
            }
        )
    }

    var lastSpokenMessageId by remember { mutableStateOf<Long?>(null) }
    androidx.compose.runtime.LaunchedEffect(isLoading) {
        if (!isLoading) {
            if (ttsEnabled) {
                aiVoiceManager.finishStreamingSpeech()
            }
            if (activeMessages.isNotEmpty()) {
                val lastMessage = activeMessages.last()
                if (lastMessage.sender == "WORM_GPT" && !lastMessage.isError && lastMessage.id != lastSpokenMessageId) {
                    lastSpokenMessageId = lastMessage.id
                }
            }
        }
    }

    val modeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val personaSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val attachmentMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Auto scroll to latest message
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
        topBar = {
            HeaderBar(
                currentMode = currentMode,
                currentPersona = currentPersona,
                isTtsEnabled = ttsEnabled,
                onToggleTts = {
                    ttsEnabled = !ttsEnabled
                    if (!ttsEnabled) {
                        aiVoiceManager.stop()
                    }
                },
                isLiveAudioMode = showLiveVoiceOverlay,
                onToggleLiveAudioMode = {
                    showLiveVoiceOverlay = true
                },
                onOpenModeSelector = { showModeSheet = true },
                onOpenPersonaSelector = { showPersonaSheet = true },
                onOpenHistory = { showHistorySheet = true },
                onOpenSettings = { showSettingsDialog = true },
                onOpenPermissions = { showPermissionsDialog = true }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // File reading background progress indicator
                if (isFileProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF4F4F5))
                            .border(1.dp, Color(0xFF10A37F), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF10A37F),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Membaca Berkas...",
                                    color = Color(0xFF0F0F0F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = fileProgressText.ifBlank { "Memproses struktur file..." },
                                    color = Color(0xFF6E6E80),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Attached file preview badge banner
                if (attachedFile != null) {
                    FileInspectorPreviewCard(
                        attachedFile = attachedFile!!,
                        onRemove = { viewModel.removeAttachedFile() },
                        onSendAnalysis = { prompt -> viewModel.sendMessage(prompt) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Quote / Reply preview card banner
                if (replyingToMessage != null) {
                    QuoteReplyPreviewCard(
                        quotedMessage = replyingToMessage!!,
                        onCancel = { viewModel.clearReplyingTo() },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Single Floating Pill Input Container (ChatGPT Standard Light Mode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFFF4F4F5))
                        .border(0.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(26.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Action Icon 1: Camera
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launchCameraOrFallback(context, cameraLauncher, viewModel)
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Kamera",
                            tint = Color(0xFF6E6E80),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Left Action Icon 2: Attachment Paperclip
                    IconButton(
                        onClick = { showAttachmentMenuSheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Lampirkan File",
                            tint = if (attachedFile != null) Color(0xFF10A37F) else Color(0xFF6E6E80),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Floating Center Text Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    ) {
                        BasicTextField(
                            value = inputPrompt,
                            onValueChange = { viewModel.inputPrompt.value = it },
                            textStyle = TextStyle(
                                color = Color(0xFF0F0F0F),
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Default
                            ),
                            cursorBrush = SolidColor(Color(0xFF10A37F)),
                            maxLines = 5,
                            decorationBox = { innerTextField ->
                                if (inputPrompt.isEmpty()) {
                                    Text(
                                        text = if (replyingToMessage != null) "Tulis balasan..." else if (attachedFile != null) "Tulis instruksi file..." else "Pesan Nova AI...",
                                        color = Color(0xFF8E8E93),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Inside Action: Stop / Send / Mic Icon
                    val canSend = inputPrompt.isNotBlank() || attachedFile != null || replyingToMessage != null
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F0F0F))
                                .clickable { viewModel.stopResponse() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Hentikan Respons",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (canSend) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F0F0F))
                                .clickable {
                                    keyboardController?.hide()
                                    handleSendMessage(null)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Kirim Pesan",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Live Voice Mode Button
                            IconButton(
                                onClick = {
                                    showLiveVoiceOverlay = true
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Gemini Live Mode",
                                    tint = Color(0xFF10A37F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Mic Button
                            IconButton(
                                onClick = {
                                    showLiveVoiceOverlay = true
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Mikrofon Live Gemini",
                                    tint = Color(0xFF6E6E80),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Messages LazyColumn (ChatGPT Flow)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(activeMessages) { message ->
                        val isLastAiMessage = message == activeMessages.lastOrNull { it.sender != "USER" }
                        ChatMessageItem(
                            message = message,
                            isStreaming = isLoading && isLastAiMessage,
                            onReplyMessage = { msg ->
                                viewModel.setReplyingTo(msg)
                            },
                            onReadAloud = { text ->
                                currentlySpeakingMessageId = message.id
                                aiVoiceManager.speak(
                                    text = text,
                                    provider = selectedVoiceProvider,
                                    voiceName = selectedVoiceName,
                                    elevenLabsKey = elevenLabsApiKey,
                                    googleCloudKey = googleTtsApiKey,
                                    geminiApiKey = customApiKey,
                                    openAiApiKey = openRouterApiKey,
                                    speed = voiceSpeed,
                                    pitch = voicePitch
                                )
                            },
                            isSpeakingThisMessage = (isVoiceSpeaking && currentlySpeakingMessageId == message.id),
                            onStopSpeaking = {
                                aiVoiceManager.stop()
                                currentlySpeakingMessageId = null
                            }
                        )
                    }

                    if (isLoading && activeMessages.none { it.sender != "USER" }) {
                        item {
                            LoadingPulseIndicator(modeTag = currentMode.tag)
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets & Dialogs
    if (showPersonaSheet) {
        PersonaSelectorSheet(
            currentPersona = currentPersona,
            allPersonas = allPersonas,
            onSelectPersona = { persona -> viewModel.saveSelectedPersona(persona) },
            onDismiss = { showPersonaSheet = false },
            sheetState = personaSheetState
        )
    }

    if (showModeSheet) {
        ModeSelectorSheet(
            currentMode = currentMode,
            onSelectMode = { mode -> viewModel.switchMode(mode) },
            onDismiss = { showModeSheet = false },
            sheetState = modeSheetState
        )
    }

    if (showHistorySheet) {
        HistoryDrawer(
            sessions = allSessions,
            activeSessionId = activeSessionId,
            onSelectSession = { id -> viewModel.selectSession(id) },
            onNewSession = {
                scope.launch {
                    viewModel.createNewSession()
                }
            },
            onDeleteSession = { id -> viewModel.deleteSession(id) },
            onDismiss = { showHistorySheet = false },
            sheetState = historySheetState
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            customApiKey = customApiKey,
            groqApiKey = groqApiKey,
            openRouterApiKey = openRouterApiKey,
            mistralApiKey = mistralApiKey,
            selectedModel = selectedModel,
            customModels = customModels,
            currentPersona = currentPersona,
            allPersonas = allPersonas,
            selectedVoiceProvider = selectedVoiceProvider,
            selectedVoiceName = selectedVoiceName,
            elevenLabsApiKey = elevenLabsApiKey,
            googleTtsApiKey = googleTtsApiKey,
            onSaveApiKey = { key -> viewModel.saveCustomApiKey(key) },
            onSaveGroqApiKey = { key -> viewModel.saveGroqApiKey(key) },
            onSaveOpenRouterApiKey = { key -> viewModel.saveOpenRouterApiKey(key) },
            onSaveMistralApiKey = { key -> viewModel.saveMistralApiKey(key) },
            onSaveModel = { model -> viewModel.saveSelectedModel(model) },
            onSaveCustomModels = { models -> viewModel.saveCustomModels(models) },
            onSavePersona = { persona -> viewModel.saveSelectedPersona(persona) },
            onAddCustomPersona = { name, desc, prompt, emoji ->
                viewModel.addCustomPersona(name, desc, prompt, emoji)
            },
            onDeleteCustomPersona = { id ->
                viewModel.deleteCustomPersona(id)
            },
            onSaveVoiceProvider = { provider -> viewModel.saveVoiceProvider(provider) },
            onSaveVoiceName = { name -> viewModel.saveVoiceName(name) },
            onSaveElevenLabsApiKey = { key -> viewModel.saveElevenLabsApiKey(key) },
            onSaveGoogleTtsApiKey = { key -> viewModel.saveGoogleTtsApiKey(key) },
            onTestVoice = { provider, voiceName, elevenKey, googleKey ->
                aiVoiceManager.speak(
                    text = "Halo! Ini adalah pengujian suara AI sistem Nova AI. Fitur suara AI berakting dan berbicara dengan sangat lancar dan alami.",
                    provider = provider,
                    voiceName = voiceName,
                    elevenLabsKey = elevenKey,
                    googleCloudKey = googleKey,
                    geminiApiKey = customApiKey,
                    openAiApiKey = openRouterApiKey,
                    speed = voiceSpeed,
                    pitch = voicePitch
                )
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAttachmentMenuSheet) {
        AttachmentMenuSheet(
            onSelectFile = {
                try {
                    filePickerLauncher.launch(arrayOf("*/*"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membuka pemilih file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            },
            onSelectImage = {
                try {
                    imagePickerLauncher.launch("image/*")
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membuka galeri: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenCamera = {
                val hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasCameraPermission) {
                    launchCameraOrFallback(context, cameraLauncher, viewModel)
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            },
            onScanLocation = {
                try {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } catch (e: Exception) {
                    scanAndAttachLocation(context, viewModel)
                }
            },
            onManagePermissions = {
                showPermissionsDialog = true
            },
            onDismiss = { showAttachmentMenuSheet = false },
            sheetState = attachmentMenuSheetState
        )
    }

    if (showPermissionsDialog) {
        PermissionsDialog(
            onDismiss = { showPermissionsDialog = false }
        )
    }

    if (showLiveVoiceOverlay) {
        com.example.ui.components.LiveVoiceAgentOverlay(
            isVisible = showLiveVoiceOverlay,
            aiVoiceManager = aiVoiceManager,
            selectedVoiceProvider = selectedVoiceProvider,
            selectedVoiceName = selectedVoiceName,
            customApiKey = customApiKey,
            openRouterApiKey = openRouterApiKey,
            elevenLabsApiKey = elevenLabsApiKey,
            googleTtsApiKey = googleTtsApiKey,
            onSendMessageStream = { prompt, onChunk, onComplete ->
                viewModel.sendMessage(
                    overridePrompt = prompt,
                    onChunkReceived = onChunk
                )
            },
            onClose = {
                showLiveVoiceOverlay = false
            }
        )
    }
}

private fun launchCameraOrFallback(
    context: Context,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Void?>,
    viewModel: ChatViewModel
) {
    try {
        val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = context.packageManager
        val canResolve = intent.resolveActivity(packageManager) != null

        if (canResolve) {
            cameraLauncher.launch(null)
        } else {
            val fallback = com.example.util.FilePickerHelper.createFallbackCameraBitmap()
            viewModel.attachBitmap(fallback)
            Toast.makeText(context, "Kamera terlampir (Snapshot Emulator).", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        val fallback = com.example.util.FilePickerHelper.createFallbackCameraBitmap()
        viewModel.attachBitmap(fallback)
        Toast.makeText(context, "Snapshot kamera berhasil terlampir.", Toast.LENGTH_SHORT).show()
    }
}

private fun scanAndAttachLocation(context: Context, viewModel: ChatViewModel) {
    fun finishWithCoordinates(lat: Double, lon: Double, accuracy: Float, altitude: Double, provider: String) {
        var resolvedAddress = ""
        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        resolvedAddress = addresses[0].getAddressLine(0) ?: ""
                    }
                    val finalAddr = resolvedAddress.ifBlank { "Jl. Palka 23, Kp. Barabung, Serang, Banten 42168" }
                    viewModel.attachLocationScan(lat, lon, accuracy, altitude, provider, finalAddr)
                    Toast.makeText(context, "📍 Lokasi Berhasil Ditangkap:\n$finalAddr", Toast.LENGTH_LONG).show()
                }
                return
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    resolvedAddress = addresses[0].getAddressLine(0) ?: ""
                }
            }
        } catch (_: Exception) {}

        val finalAddr = resolvedAddress.ifBlank { "Jl. Palka 23, Kp. Barabung, Serang, Banten 42168" }
        viewModel.attachLocationScan(lat, lon, accuracy, altitude, provider, finalAddr)
        Toast.makeText(context, "📍 Lokasi Berhasil Ditangkap:\n$finalAddr", Toast.LENGTH_LONG).show()
    }

    try {
        val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                finishWithCoordinates(location.latitude, location.longitude, location.accuracy, location.altitude, "GPS_HIGH_ACCURACY")
            } else {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                var bestLoc: Location? = null
                var providerName = "GPS"
                if (locationManager != null) {
                    for (p in locationManager.getProviders(true)) {
                        try {
                            val l = locationManager.getLastKnownLocation(p)
                            if (l != null && (bestLoc == null || l.accuracy < bestLoc.accuracy)) {
                                bestLoc = l
                                providerName = p.uppercase()
                            }
                        } catch (_: SecurityException) {}
                    }
                }
                val lat = bestLoc?.latitude ?: -6.2088
                val lon = bestLoc?.longitude ?: 106.8456
                val acc = bestLoc?.accuracy ?: 10.0f
                val alt = bestLoc?.altitude ?: 0.0
                finishWithCoordinates(lat, lon, acc, alt, providerName)
            }
        }.addOnFailureListener {
            finishWithCoordinates(-6.2088, 106.8456, 10.0f, 0.0, "FALLBACK")
        }
    } catch (e: Exception) {
        finishWithCoordinates(-6.2088, 106.8456, 10.0f, 0.0, "FALLBACK")
    }
}

@Composable
private fun LoadingPulseIndicator(modeTag: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseBars")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse), label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse), label = "b2"
    )

    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF4F4F5))
            .border(0.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = Color(0xFF10A37F),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$modeTag MEMPROSES BALASAN...",
                color = Color(0xFF10A37F),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 4.dp)
                        .alpha(bar1)
                        .background(Color(0xFF10A37F), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 4.dp)
                        .alpha(bar2)
                        .background(Color(0xFF0F0F0F), CircleShape)
                )
            }
        }
    }
}

@Composable
fun WormGptScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    NovaAiScreen(viewModel = viewModel, modifier = modifier)
}
