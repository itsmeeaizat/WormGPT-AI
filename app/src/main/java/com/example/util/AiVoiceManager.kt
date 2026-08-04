package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class AiVoiceOption(
    val id: String,
    val name: String,
    val gender: String, // "Male", "Female", "Neutral"
    val description: String,
    val provider: String // "gemini", "openai", "elevenlabs", "google_cloud"
)

object AiVoiceCatalog {
    val GEMINI_VOICES = listOf(
        AiVoiceOption("Puck", "Puck (Gemini Live Male)", "Male", "Suara Pria Enerjik & Natural Real-time Live", "gemini"),
        AiVoiceOption("Charon", "Charon (Gemini Live Deep)", "Male", "Suara Pria Dalam & Karismatik Real-time Live", "gemini"),
        AiVoiceOption("Kore", "Kore (Gemini Live Female)", "Female", "Suara Wanita Lembut & Ramah Real-time Live", "gemini"),
        AiVoiceOption("Fenrir", "Fenrir (Gemini Live Firm)", "Male", "Suara Pria Maskulin & Tegas Real-time Live", "gemini"),
        AiVoiceOption("Aoede", "Aoede (Gemini Live Warm)", "Female", "Suara Wanita Anggun & Warm Real-time Live", "gemini")
    )

    val OPENAI_VOICES = listOf(
        AiVoiceOption("alloy", "Alloy (OpenAI Balanced)", "Neutral", "Suara Netral, Seimbang & Luwes Live", "openai"),
        AiVoiceOption("echo", "Echo (OpenAI Warm Male)", "Male", "Suara Pria Hangat & Ramah Live", "openai"),
        AiVoiceOption("fable", "Fable (OpenAI British Male)", "Male", "Suara Pria Ekspresif Naratif Live", "openai"),
        AiVoiceOption("onyx", "Onyx (OpenAI Deep Male)", "Male", "Suara Pria Dalam & Wibawa Live", "openai"),
        AiVoiceOption("nova", "Nova (OpenAI Energetic Female)", "Female", "Suara Wanita Ceria & Jernih Live", "openai"),
        AiVoiceOption("shimmer", "Shimmer (OpenAI Clear Female)", "Female", "Suara Wanita Profesional Live", "openai")
    )

    val GOOGLE_CLOUD_VOICES = listOf(
        AiVoiceOption("id-ID-Neural2-A", "Indonesia Neural2 Female", "Female", "Suara AI Cloud Indonesia Neural2 Wanita Live", "google_cloud"),
        AiVoiceOption("id-ID-Neural2-C", "Indonesia Neural2 Male", "Male", "Suara AI Cloud Indonesia Neural2 Pria Live", "google_cloud"),
        AiVoiceOption("id-ID-Wavenet-A", "Indonesia Wavenet Female", "Female", "Suara AI Cloud Indonesia Wavenet Wanita Live", "google_cloud"),
        AiVoiceOption("id-ID-Wavenet-D", "Indonesia Wavenet Male", "Male", "Suara AI Cloud Indonesia Wavenet Pria Live", "google_cloud")
    )

    val ELEVENLABS_VOICES = listOf(
        AiVoiceOption("21m00Tcm4TlvDq8ikWAM", "Rachel (ElevenLabs Natural)", "Female", "Suara AI ElevenLabs Natural & Calming Live", "elevenlabs"),
        AiVoiceOption("pNInz6obpgDQGcFmaJgB", "Adam (ElevenLabs Deep Male)", "Male", "Suara AI ElevenLabs Pria Karismatik Live", "elevenlabs"),
        AiVoiceOption("EXAVITQu4vr4xnSDxMaL", "Bella (ElevenLabs Expressive)", "Female", "Suara AI ElevenLabs Narasi Jernih Live", "elevenlabs"),
        AiVoiceOption("ErXwobaYiN019PkySvjV", "Antoni (ElevenLabs Elegant Male)", "Male", "Suara AI ElevenLabs Elegan & Luwes Live", "elevenlabs"),
        AiVoiceOption("TxGEqnHWrfWFTfGW9XjX", "Josh (ElevenLabs Conversational)", "Male", "Suara AI ElevenLabs Conversational Live", "elevenlabs")
    )

    fun getVoicesForProvider(provider: String): List<AiVoiceOption> {
        return when (provider.lowercase()) {
            "gemini" -> GEMINI_VOICES
            "openai" -> OPENAI_VOICES
            "google_cloud" -> GOOGLE_CLOUD_VOICES
            "elevenlabs" -> ELEVENLABS_VOICES
            else -> GEMINI_VOICES + OPENAI_VOICES
        }
    }
}

/**
 * Real-Time Bidirectional Live Audio Stream Engine with MediaPlayer, Cloud TTS & System TextToSpeech Fallback.
 */
class AiVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.Main)
    
    private var nativeTts: TextToSpeech? = null
    private var isNativeTtsReady = false
    private var nativeTtsCallback: (() -> Unit)? = null

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    
    private var pcmAudioChannel: Channel<ByteArray>? = null
    private var textStreamChannel: Channel<String>? = null
    
    private var audioPlayerJob: Job? = null
    private var synthesisJob: Job? = null
    private var liveWebSocket: WebSocket? = null

    private val textBuffer = StringBuilder()
    private var processedTextLength = 0

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isLiveStreaming = MutableStateFlow(false)
    val isLiveStreaming: StateFlow<Boolean> = _isLiveStreaming

    private val _isLoadingAudio = MutableStateFlow(false)
    val isLoadingAudio: StateFlow<Boolean> = _isLoadingAudio

    private val sampleRate = 24000

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        try {
            nativeTts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("AiVoiceManager", "Native TTS init exception: ${e.localizedMessage}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                val localeIndo = Locale("id", "ID")
                val langResult = nativeTts?.setLanguage(localeIndo)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    nativeTts?.setLanguage(Locale.US)
                }
                isNativeTtsReady = true

                nativeTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            _isSpeaking.value = true
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            _isSpeaking.value = false
                            nativeTtsCallback?.invoke()
                            nativeTtsCallback = null
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            _isSpeaking.value = false
                            nativeTtsCallback?.invoke()
                            nativeTtsCallback = null
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("AiVoiceManager", "Error setting up Native TTS listener: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Memutar Byte Audio (MP3/WAV/AAC) menggunakan MediaPlayer Native Android.
     */
    private fun playAudioBytes(
        bytes: ByteArray,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        onComplete: (() -> Unit)? = null,
        onError: (() -> Unit)? = null
    ) {
        stopAudioPlaybackOnly()
        try {
            val tempFile = File.createTempFile("voice_tts_", ".mp3", context.cacheDir)
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use { fos ->
                fos.write(bytes)
            }

            releaseMediaPlayer()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && speed != 1.0f) {
                    try {
                        playbackParams = playbackParams.setSpeed(speed).setPitch(pitch)
                    } catch (_: Exception) {}
                }
                prepare()
                start()
                _isSpeaking.value = true
                setOnCompletionListener {
                    _isSpeaking.value = false
                    try { tempFile.delete() } catch (_: Exception) {}
                    releaseMediaPlayer()
                    onComplete?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    _isSpeaking.value = false
                    try { tempFile.delete() } catch (_: Exception) {}
                    releaseMediaPlayer()
                    onError?.invoke()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AiVoiceManager", "MediaPlayer playback error: ${e.localizedMessage}")
            _isSpeaking.value = false
            onError?.invoke()
        }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    private fun stopAudioPlaybackOnly() {
        releaseMediaPlayer()
        releaseAudioTrack()
        try { nativeTts?.stop() } catch (_: Exception) {}
    }

    private fun initAudioTrack() {
        releaseAudioTrack()
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufSize * 4, 16384)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("AiVoiceManager", "AudioTrack init error: ${e.localizedMessage}")
        }
    }

    private fun releaseAudioTrack() {
        try {
            audioTrack?.let {
                if (it.state == AudioTrack.STATE_INITIALIZED) {
                    it.pause()
                    it.flush()
                    it.stop()
                }
                it.release()
            }
            audioTrack = null
        } catch (_: Exception) {}
    }

    /**
     * Memulai Live Real-Time Streaming Audio Speech Session.
     */
    fun startStreamingSpeech(
        provider: String,
        voiceName: String,
        elevenLabsKey: String = "",
        googleCloudKey: String = "",
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        stop()

        val textChannel = Channel<String>(Channel.UNLIMITED)
        textStreamChannel = textChannel
        
        textBuffer.clear()
        processedTextLength = 0

        _isSpeaking.value = true
        _isLiveStreaming.value = true

        // Dedicated Text to Audio Synthesizer & Sequential Player Loop
        synthesisJob = scope.launch(Dispatchers.IO) {
            try {
                for (phrase in textChannel) {
                    val cleanPhrase = cleanTextForLiveSpeech(phrase)
                    if (cleanPhrase.isBlank()) continue

                    _isLoadingAudio.value = true
                    val audioBytes = fetchLiveAudioBytes(
                        text = cleanPhrase,
                        provider = provider,
                        voiceName = voiceName,
                        elevenLabsKey = elevenLabsKey,
                        googleCloudKey = googleCloudKey,
                        speed = speed,
                        pitch = pitch
                    )
                    _isLoadingAudio.value = false

                    val deferred = CompletableDeferred<Unit>()
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            playAudioBytes(
                                bytes = audioBytes,
                                speed = speed,
                                pitch = pitch,
                                onComplete = { deferred.complete(Unit) },
                                onError = {
                                    speakNativeTts(cleanPhrase, speed, pitch, onComplete = { deferred.complete(Unit) })
                                }
                            )
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            speakNativeTts(cleanPhrase, speed, pitch, onComplete = { deferred.complete(Unit) })
                        }
                    }
                    deferred.await()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isSpeaking.value = false
                    _isLiveStreaming.value = false
                }
            }
        }
    }

    fun offerStreamTextChunk(chunk: String) {
        if (textStreamChannel == null) return
        textBuffer.append(chunk)

        val fullText = textBuffer.toString()
        val unprocessed = fullText.substring(processedTextLength)

        var boundaryIdx = findBoundary(unprocessed)
        var offset = 0
        while (boundaryIdx != -1) {
            val clause = unprocessed.substring(offset, boundaryIdx + 1).trim()
            if (clause.isNotBlank()) {
                textStreamChannel?.trySend(clause)
            }
            offset = boundaryIdx + 1
            boundaryIdx = findNextBoundary(unprocessed, offset)
        }
        processedTextLength += offset
    }

    fun finishStreamingSpeech() {
        val fullText = textBuffer.toString()
        if (processedTextLength < fullText.length) {
            val remaining = fullText.substring(processedTextLength).trim()
            if (remaining.isNotBlank()) {
                textStreamChannel?.trySend(remaining)
            }
        }
        processedTextLength = fullText.length
        textStreamChannel?.close()
    }

    /**
     * Membacakan teks secara instan (sekali panggil / tombol 'BACAKAN').
     */
    fun speak(
        text: String,
        provider: String = "gemini",
        voiceName: String = "Puck",
        elevenLabsKey: String = "",
        googleCloudKey: String = "",
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        stop()
        val cleanText = cleanTextForLiveSpeech(text)
        if (cleanText.isBlank()) return

        _isSpeaking.value = true
        _isLoadingAudio.value = true

        scope.launch(Dispatchers.IO) {
            val audioBytes = fetchLiveAudioBytes(
                text = cleanText,
                provider = provider,
                voiceName = voiceName,
                elevenLabsKey = elevenLabsKey,
                googleCloudKey = googleCloudKey,
                speed = speed,
                pitch = pitch
            )
            _isLoadingAudio.value = false

            withContext(Dispatchers.Main) {
                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    playAudioBytes(
                        bytes = audioBytes,
                        speed = speed,
                        pitch = pitch,
                        onComplete = { _isSpeaking.value = false },
                        onError = {
                            speakNativeTts(cleanText, speed, pitch)
                        }
                    )
                } else {
                    speakNativeTts(cleanText, speed, pitch)
                }
            }
        }
    }

    /**
     * Speak menggunakan Native Android System Speech TextToSpeech.
     */
    fun speakNativeTts(
        text: String,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        onComplete: (() -> Unit)? = null
    ) {
        if (nativeTts == null) {
            initNativeTts()
        }
        nativeTtsCallback = onComplete
        _isSpeaking.value = true
        val cleanText = cleanTextForLiveSpeech(text)
        try {
            nativeTts?.setSpeechRate(speed)
            nativeTts?.setPitch(pitch)
            val uttId = "WormGpt_TTS_${System.currentTimeMillis()}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, uttId)
                nativeTts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, uttId)
            } else {
                val map = HashMap<String, String>()
                map[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = uttId
                @Suppress("DEPRECATION")
                nativeTts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, map)
            }
        } catch (e: Exception) {
            Log.e("AiVoiceManager", "Native TTS speak error: ${e.localizedMessage}")
            _isSpeaking.value = false
            nativeTtsCallback?.invoke()
            nativeTtsCallback = null
        }
    }

    fun writeLiveAudioPcmChunk(pcmBytes: ByteArray) {
        if (audioTrack == null) {
            initAudioTrack()
        }
        _isSpeaking.value = true
        _isLiveStreaming.value = true
        try {
            audioTrack?.write(pcmBytes, 0, pcmBytes.size)
        } catch (e: Exception) {
            Log.e("AiVoiceManager", "Error writing PCM chunk: ${e.localizedMessage}")
        }
    }

    fun connectLiveAudioWebSocket(wsUrl: String = "ws://10.0.2.2:3000/api/live-audio") {
        disconnectLiveAudioWebSocket()
        val client = OkHttpClient()
        val request = Request.Builder().url(wsUrl).build()

        liveWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("AiVoiceManager", "Live Audio WebSocket Connected")
                _isLiveStreaming.value = true
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val pcmData = bytes.toByteArray()
                writeLiveAudioPcmChunk(pcmData)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val base64Pcm = json.optString("audioPcm", "")
                    if (base64Pcm.isNotBlank()) {
                        val pcmBytes = android.util.Base64.decode(base64Pcm, android.util.Base64.DEFAULT)
                        writeLiveAudioPcmChunk(pcmBytes)
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("AiVoiceManager", "Live Audio WebSocket Failure: ${t.localizedMessage}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isLiveStreaming.value = false
            }
        })
    }

    fun disconnectLiveAudioWebSocket() {
        try {
            liveWebSocket?.close(1000, "User disconnected live stream")
            liveWebSocket = null
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            audioPlayerJob?.cancel()
            audioPlayerJob = null

            synthesisJob?.cancel()
            synthesisJob = null

            textStreamChannel?.close()
            textStreamChannel = null

            pcmAudioChannel?.close()
            pcmAudioChannel = null

            disconnectLiveAudioWebSocket()
            stopAudioPlaybackOnly()

            textBuffer.clear()
            processedTextLength = 0
        } catch (_: Exception) {
        } finally {
            _isSpeaking.value = false
            _isLiveStreaming.value = false
            _isLoadingAudio.value = false
        }
    }

    fun destroy() {
        stop()
        try {
            nativeTts?.shutdown()
            nativeTts = null
        } catch (_: Exception) {}
    }

    private fun findBoundary(str: String): Int {
        return findNextBoundary(str, 0)
    }

    private fun findNextBoundary(str: String, startIdx: Int): Int {
        for (i in startIdx until str.length) {
            val ch = str[i]
            if (ch == '.' || ch == '!' || ch == '?' || ch == '\n' || ch == '\r') {
                return i
            }
            if (i - startIdx >= 40 && (ch == ',' || ch == ':' || ch == ';')) {
                return i
            }
        }
        return -1
    }

    private suspend fun fetchLiveAudioBytes(
        text: String,
        provider: String,
        voiceName: String,
        elevenLabsKey: String,
        googleCloudKey: String,
        speed: Float,
        pitch: Float
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            return@withContext when (provider.lowercase()) {
                "elevenlabs" -> {
                    if (elevenLabsKey.isNotBlank()) {
                        fetchElevenLabsLiveStream(text, voiceName, elevenLabsKey)
                    } else {
                        fetchCloudTtsStreamBytes(text, "id")
                    }
                }
                "google_cloud" -> {
                    if (googleCloudKey.isNotBlank()) {
                        fetchGoogleCloudTtsBytes(text, voiceName, googleCloudKey, speed, pitch)
                    } else {
                        val lang = if (voiceName.startsWith("id-")) "id" else "en"
                        fetchCloudTtsStreamBytes(text, lang)
                    }
                }
                else -> {
                    val isEnglish = text.contains(Regex("[a-zA-Z]")) && !text.contains(Regex("(yang|dengan|untuk|bisa|ini|itu|adalah|tidak|kamu|saya|apa|bagaimana)"))
                    val lang = if (isEnglish) "en" else "id"
                    fetchCloudTtsStreamBytes(text, lang)
                }
            }
        } catch (e: Exception) {
            Log.e("AiVoiceManager", "Live audio fetch error: ${e.localizedMessage}")
        }
        return@withContext null
    }

    private suspend fun fetchCloudTtsStreamBytes(text: String, lang: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val encodedText = URLEncoder.encode(text.take(300), "UTF-8")
            val urlString = "https://translate.google.com/translate_tts?ie=UTF-8&q=$encodedText&tl=$lang&client=tw-ob"

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

            if (conn.responseCode == 200) {
                val bos = ByteArrayOutputStream()
                conn.inputStream.use { input -> input.copyTo(bos) }
                val bytes = bos.toByteArray()
                if (bytes.isNotEmpty()) {
                    return@withContext bytes
                }
            }
        } catch (_: Exception) {}
        return@withContext null
    }

    private suspend fun fetchElevenLabsLiveStream(text: String, voiceId: String, apiKey: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val realVoiceId = if (voiceId.isBlank()) "21m00Tcm4TlvDq8ikWAM" else voiceId
            val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$realVoiceId/stream?output_format=mp3_44100_128")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("xi-api-key", apiKey)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("text", text.take(400))
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.75)
                })
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val bos = ByteArrayOutputStream()
                conn.inputStream.use { input -> input.copyTo(bos) }
                return@withContext bos.toByteArray()
            }
        } catch (_: Exception) {}
        return@withContext null
    }

    private suspend fun fetchGoogleCloudTtsBytes(
        text: String,
        voiceName: String,
        apiKey: String,
        speed: Float,
        pitch: Float
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.doOutput = true

            val langCode = if (voiceName.startsWith("id-")) "id-ID" else "en-US"
            val jsonBody = JSONObject().apply {
                put("input", JSONObject().put("text", text.take(400)))
                put("voice", JSONObject().apply {
                    put("languageCode", langCode)
                    put("name", voiceName)
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "MP3")
                    put("speakingRate", speed)
                    put("pitch", pitch)
                })
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseStr)
                val audioContentBase64 = json.optString("audioContent", "")
                if (audioContentBase64.isNotBlank()) {
                    return@withContext android.util.Base64.decode(audioContentBase64, android.util.Base64.DEFAULT)
                }
            }
        } catch (_: Exception) {}
        return@withContext null
    }

    private fun cleanTextForLiveSpeech(text: String): String {
        return text
            .replace(Regex("```[\\s\\S]*?```"), " kode program ")
            .replace(Regex("`[^`]*`"), "")
            .replace(Regex("https?://\\S+"), " tautan web ")
            .replace(Regex("[#*_~\\[\\]()<>{}\\\\/'\"]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
