package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.AiVoiceCatalog
import com.example.util.AiVoiceManager
import kotlinx.coroutines.launch
import java.util.Locale

enum class LiveAgentState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun LiveVoiceAgentOverlay(
    isVisible: Boolean,
    aiVoiceManager: AiVoiceManager,
    selectedVoiceProvider: String,
    selectedVoiceName: String,
    customApiKey: String,
    openRouterApiKey: String,
    elevenLabsApiKey: String,
    googleTtsApiKey: String,
    onSendMessageStream: (String, (String) -> Unit, () -> Unit) -> Unit,
    onClose: () -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var liveState by remember { mutableStateOf(LiveAgentState.IDLE) }
    var userTranscript by remember { mutableStateOf("") }
    var aiLiveResponse by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Tekan & Tahan untuk Bicara, atau Ketuk 1x") }
    var isMuted by remember { mutableStateOf(false) }

    // Speech Recognizer
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    var isListeningSTT by remember { mutableStateOf(false) }

    fun startListening() {
        if (isMuted) return
        aiVoiceManager.stop()
        liveState = LiveAgentState.LISTENING
        statusText = "Sedang Mendengarkan... (Bicara sekarang)"
        userTranscript = ""
        aiLiveResponse = ""

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara sekarang...")
        }

        try {
            speechRecognizer.startListening(intent)
            isListeningSTT = true
        } catch (e: Exception) {
            liveState = LiveAgentState.IDLE
            statusText = "Ketuk mikrofon atau pilih pertanyaan cepat"
        }
    }

    fun stopListening() {
        if (isListeningSTT) {
            try {
                speechRecognizer.stopListening()
            } catch (_: Exception) {}
            isListeningSTT = false
        }
    }

    fun processUserSpeech(spokenText: String) {
        if (spokenText.isBlank()) {
            liveState = LiveAgentState.IDLE
            statusText = "Suara tidak terdeteksi. Tekan/tahan untuk bicara."
            return
        }

        userTranscript = spokenText
        liveState = LiveAgentState.THINKING
        statusText = "Nova AI sedang memproses..."
        aiLiveResponse = ""

        val fullResponseBuilder = StringBuilder()
        var streamStarted = false

        val promptForVoice = if (spokenText.contains("[Jawablah")) spokenText else "$spokenText (Jawablah secara singkat, percakapan langsung, luwes, dan sangat alami tanpa format markdown)"

        onSendMessageStream(
            promptForVoice,
            { chunk ->
                if (!streamStarted) {
                    streamStarted = true
                    liveState = LiveAgentState.SPEAKING
                    statusText = "Nova AI menjawab..."
                    aiVoiceManager.startStreamingSpeech(
                        provider = selectedVoiceProvider,
                        voiceName = selectedVoiceName,
                        elevenLabsKey = elevenLabsApiKey,
                        googleCloudKey = googleTtsApiKey,
                        geminiApiKey = customApiKey,
                        openAiApiKey = openRouterApiKey
                    )
                }
                fullResponseBuilder.append(chunk)
                aiLiveResponse = fullResponseBuilder.toString()
                aiVoiceManager.offerStreamTextChunk(chunk)
            },
            {
                aiVoiceManager.finishStreamingSpeech()
            }
        )
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListeningSTT = true
                liveState = LiveAgentState.LISTENING
                statusText = "Mendengarkan... (Bicara sekarang)"
            }

            override fun onBeginningOfSpeech() {
                statusText = "Mendengar suara Anda..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListeningSTT = false
                if (liveState == LiveAgentState.LISTENING) {
                    liveState = LiveAgentState.THINKING
                    statusText = "Memproses ucapan Anda..."
                }
            }

            override fun onError(error: Int) {
                isListeningSTT = false
                if (liveState == LiveAgentState.LISTENING) {
                    liveState = LiveAgentState.IDLE
                    statusText = "Tekan & tahan untuk bicara"
                }
            }

            override fun onResults(results: Bundle?) {
                isListeningSTT = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                processUserSpeech(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    userTranscript = matches.first()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        onDispose {
            try {
                speechRecognizer.destroy()
            } catch (_: Exception) {}
        }
    }

    // Monitor speaking state to return to IDLE when done
    val isVoiceSpeaking by aiVoiceManager.isSpeaking.collectAsStateWithLifecycle()
    LaunchedEffect(isVoiceSpeaking) {
        if (!isVoiceSpeaking && liveState == LiveAgentState.SPEAKING) {
            liveState = LiveAgentState.IDLE
            statusText = "Tekan & Tahan untuk Bicara, atau Ketuk 1x"
        }
    }

    // Auto start listening on open
    LaunchedEffect(isVisible) {
        if (isVisible) {
            startListening()
        }
    }

    // Infinite Animation Transitions
    val infiniteTransition = rememberInfiniteTransition(label = "GeminiLivePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val orbColorGradient = when (liveState) {
        LiveAgentState.LISTENING -> listOf(Color(0xFF10A37F), Color(0xFF00C853), Color(0xFF00E5FF))
        LiveAgentState.THINKING -> listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899))
        LiveAgentState.SPEAKING -> listOf(Color(0xFF10A37F), Color(0xFF0284C7), Color(0xFF059669))
        LiveAgentState.IDLE -> listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF64748B))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFFFFF)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Soft Radiant Glow Effect
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2.3f)
                val radius = size.width * 0.48f * (if (liveState != LiveAgentState.IDLE) pulseScale else 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbColorGradient[0].copy(alpha = 0.15f), Color.Transparent),
                        center = center,
                        radius = radius * 1.5f
                    ),
                    center = center,
                    radius = radius * 1.5f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE6F4EA))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF10A37F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini Live Voice",
                            color = Color(0xFF0D654D),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            aiVoiceManager.stop()
                            stopListening()
                            onClose()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF4F4F5), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Mode Live Suara",
                            tint = Color(0xFF3F3F46),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Center: Animated Glowing Pulsing Orb (Supports Hold to Speak & Tap to Speak)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(230.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    if (liveState == LiveAgentState.SPEAKING) {
                                        aiVoiceManager.stop()
                                    }
                                    startListening()
                                    val released = tryAwaitRelease()
                                    if (released) {
                                        stopListening()
                                    }
                                },
                                onTap = {
                                    if (liveState == LiveAgentState.SPEAKING) {
                                        aiVoiceManager.stop()
                                        startListening()
                                    } else if (liveState == LiveAgentState.LISTENING) {
                                        stopListening()
                                    } else {
                                        startListening()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(if (liveState != LiveAgentState.IDLE) pulseScale else 1f)
                    ) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = orbColorGradient,
                                center = center
                            ),
                            radius = size.width / 2.2f
                        )
                    }

                    // Inner White Glass Circle
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFFFFF))
                            .border(3.dp, orbColorGradient[0].copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (liveState) {
                                LiveAgentState.LISTENING -> Icons.Default.GraphicEq
                                LiveAgentState.THINKING -> Icons.Default.AutoAwesome
                                LiveAgentState.SPEAKING -> Icons.Default.VolumeUp
                                LiveAgentState.IDLE -> Icons.Default.Mic
                            },
                            contentDescription = "Status Suara",
                            tint = if (liveState == LiveAgentState.IDLE) Color(0xFF10A37F) else orbColorGradient[0],
                            modifier = Modifier.size(58.dp)
                        )
                    }
                }

                // Live Transcripts & Status Text Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        color = Color(0xFF0D654D),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Voice Prompts Chips Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickPrompts = listOf(
                            "Halo Gemini! Ceritakan lelucon lucu",
                            "Siapa kamu dan apa kelebihanmu?",
                            "Beri saya 3 tips produktivitas hari ini",
                            "Bagaimana cara kerja kecerdasan buatan?"
                        )
                        items(quickPrompts) { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(20.dp))
                                    .clickable {
                                        processUserSpeech(prompt)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    color = Color(0xFF27272A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (userTranscript.isNotBlank()) {
                            Text(
                                text = "Anda: $userTranscript",
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (aiLiveResponse.isNotBlank()) {
                            Text(
                                text = "Nova AI: $aiLiveResponse",
                                color = Color(0xFF0D654D),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (userTranscript.isBlank() && aiLiveResponse.isBlank()) {
                            Text(
                                text = "Tahan tombol mikrofon di bawah atau ketuk untuk perbincangan suara Gemini Live...",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bottom Action Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Mic Button
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            if (isMuted) {
                                stopListening()
                                liveState = LiveAgentState.IDLE
                                statusText = "Mikrofon dibisukan"
                            } else {
                                startListening()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (isMuted) Color(0xFFFEE2E2) else Color(0xFFF4F4F5),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isMuted) Color(0xFFFCA5A5) else Color(0xFFE4E4E7),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Bisu Mikrofon",
                            tint = if (isMuted) Color(0xFFDC2626) else Color(0xFF27272A),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Large Center Action (Hold / Tap to Speak)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (liveState == LiveAgentState.SPEAKING) Color(0xFFEF4444)
                                else Color(0xFF10A37F)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        if (liveState == LiveAgentState.SPEAKING) {
                                            aiVoiceManager.stop()
                                        }
                                        startListening()
                                        val released = tryAwaitRelease()
                                        if (released) {
                                            stopListening()
                                        }
                                    },
                                    onTap = {
                                        if (liveState == LiveAgentState.SPEAKING) {
                                            aiVoiceManager.stop()
                                            startListening()
                                        } else if (liveState == LiveAgentState.LISTENING) {
                                            stopListening()
                                        } else {
                                            startListening()
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (liveState == LiveAgentState.SPEAKING) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Aksi Utama Live Voice",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Restart / Re-trigger Voice Button
                    IconButton(
                        onClick = {
                            aiVoiceManager.stop()
                            startListening()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFF4F4F5), CircleShape)
                            .border(1.dp, Color(0xFFE4E4E7), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Ulangi Suara",
                            tint = Color(0xFF10A37F),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

