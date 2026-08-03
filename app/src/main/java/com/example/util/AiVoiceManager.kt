package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AiVoiceOption(
    val id: String,
    val name: String,
    val gender: String, // "Male", "Female", "Neutral"
    val description: String,
    val provider: String // "gemini", "openai", "elevenlabs", "google_cloud"
)

object AiVoiceCatalog {
    val GEMINI_VOICES = listOf(
        AiVoiceOption("Puck", "Puck (Gemini Male)", "Male", "Suara Pria Enerjik & Natural Pusat", "gemini"),
        AiVoiceOption("Charon", "Charon (Gemini Deep Male)", "Male", "Suara Pria Dalam & Karismatik Pusat", "gemini"),
        AiVoiceOption("Kore", "Kore (Gemini Female)", "Female", "Suara Wanita Lembut & Ramah Pusat", "gemini"),
        AiVoiceOption("Fenrir", "Fenrir (Gemini Firm Male)", "Male", "Suara Pria Maskulin & Tegas Pusat", "gemini"),
        AiVoiceOption("Aoede", "Aoede (Gemini Warm Female)", "Female", "Suara Wanita Anggun & Warm Pusat", "gemini")
    )

    val OPENAI_VOICES = listOf(
        AiVoiceOption("alloy", "Alloy (OpenAI Balanced)", "Neutral", "Suara Netral, Seimbang & Luwes Pusat", "openai"),
        AiVoiceOption("echo", "Echo (OpenAI Warm Male)", "Male", "Suara Pria Hangat & Ramah Pusat", "openai"),
        AiVoiceOption("fable", "Fable (OpenAI British Male)", "Male", "Suara Pria Ekspresif Naratif Pusat", "openai"),
        AiVoiceOption("onyx", "Onyx (OpenAI Deep Male)", "Male", "Suara Pria Dalam & Wibawa Pusat", "openai"),
        AiVoiceOption("nova", "Nova (OpenAI Energetic Female)", "Female", "Suara Wanita Ceria & Jernih Pusat", "openai"),
        AiVoiceOption("shimmer", "Shimmer (OpenAI Clear Female)", "Female", "Suara Wanita Profesional & Smooth Pusat", "openai")
    )

    val GOOGLE_CLOUD_VOICES = listOf(
        AiVoiceOption("id-ID-Neural2-A", "Indonesia Neural2 Female", "Female", "Suara AI Cloud Indonesia Neural2 Wanita", "google_cloud"),
        AiVoiceOption("id-ID-Neural2-C", "Indonesia Neural2 Male", "Male", "Suara AI Cloud Indonesia Neural2 Pria", "google_cloud"),
        AiVoiceOption("id-ID-Wavenet-A", "Indonesia Wavenet Female", "Female", "Suara AI Cloud Indonesia Wavenet Wanita", "google_cloud"),
        AiVoiceOption("id-ID-Wavenet-D", "Indonesia Wavenet Male", "Male", "Suara AI Cloud Indonesia Wavenet Pria", "google_cloud")
    )

    val ELEVENLABS_VOICES = listOf(
        AiVoiceOption("21m00Tcm4TlvDq8ikWAM", "Rachel (ElevenLabs Natural)", "Female", "Suara AI ElevenLabs Natural & Calming", "elevenlabs"),
        AiVoiceOption("pNInz6obpgDQGcFmaJgB", "Adam (ElevenLabs Deep Male)", "Male", "Suara AI ElevenLabs Pria Karismatik", "elevenlabs"),
        AiVoiceOption("EXAVITQu4vr4xnSDxMaL", "Bella (ElevenLabs Expressive)", "Female", "Suara AI ElevenLabs Narasi Jernih", "elevenlabs"),
        AiVoiceOption("ErXwobaYiN019PkySvjV", "Antoni (ElevenLabs Elegant Male)", "Male", "Suara AI ElevenLabs Elegan & Luwes", "elevenlabs"),
        AiVoiceOption("TxGEqnHWrfWFTfGW9XjX", "Josh (ElevenLabs Conversational)", "Male", "Suara AI ElevenLabs Conversational", "elevenlabs")
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

class AiVoiceManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    private val scope = CoroutineScope(Dispatchers.Main)
    private var speakJob: Job? = null

    private var speechChannel: Channel<String>? = null
    private val sentenceBuffer = StringBuilder()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isLoadingAudio = MutableStateFlow(false)
    val isLoadingAudio: StateFlow<Boolean> = _isLoadingAudio

    fun stop() {
        speakJob?.cancel()
        speakJob = null
        speechChannel?.close()
        speechChannel = null
        sentenceBuffer.clear()

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        _isSpeaking.value = false
        _isLoadingAudio.value = false
    }

    /**
     * Inisialisasi Real-time Streaming Voice langsung dari API Server Pusat.
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
        val channel = Channel<String>(Channel.UNLIMITED)
        speechChannel = channel

        speakJob = scope.launch(Dispatchers.IO) {
            _isSpeaking.value = true
            for (sentence in channel) {
                val cleanSentence = cleanTextForSpeech(sentence)
                if (cleanSentence.isBlank()) continue

                _isLoadingAudio.value = true
                val audioFile = try {
                    fetchAudioForSentence(cleanSentence, provider, voiceName, elevenLabsKey, googleCloudKey, speed, pitch)
                } catch (e: Exception) {
                    Log.e("AiVoiceManager", "Gagal mengambil streaming audio API: ${e.localizedMessage}")
                    null
                }
                _isLoadingAudio.value = false

                if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                    playAudioFileAndWait(audioFile)
                }
            }
            _isSpeaking.value = false
        }
    }

    /**
     * Menerima potongan token teks AI secara real-time dari stream model.
     */
    fun offerStreamTextChunk(chunk: String) {
        if (speechChannel == null) return
        sentenceBuffer.append(chunk)

        var boundaryIdx = findSentenceBoundary(sentenceBuffer)
        while (boundaryIdx != -1) {
            val sentence = sentenceBuffer.substring(0, boundaryIdx + 1)
            sentenceBuffer.delete(0, boundaryIdx + 1)
            if (sentence.isNotBlank()) {
                speechChannel?.trySend(sentence)
            }
            boundaryIdx = findSentenceBoundary(sentenceBuffer)
        }
    }

    /**
     * Menandai akhir dari streaming jawaban AI.
     */
    fun finishStreamingSpeech() {
        val remaining = sentenceBuffer.toString()
        sentenceBuffer.clear()
        if (remaining.isNotBlank()) {
            speechChannel?.trySend(remaining)
        }
        speechChannel?.close()
    }

    private fun findSentenceBoundary(sb: StringBuilder): Int {
        for (i in 0 until sb.length) {
            val ch = sb[i]
            if (ch == '.' || ch == '!' || ch == '?' || ch == '\n' || ch == '\r') {
                return i
            }
            if (i >= 45 && (ch == ',' || ch == ':' || ch == ';')) {
                return i
            }
        }
        return -1
    }

    fun speak(
        text: String,
        provider: String,
        voiceName: String,
        elevenLabsKey: String = "",
        googleCloudKey: String = "",
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        startStreamingSpeech(provider, voiceName, elevenLabsKey, googleCloudKey, speed, pitch)
        offerStreamTextChunk(text)
        finishStreamingSpeech()
    }

    private suspend fun fetchAudioForSentence(
        cleanText: String,
        provider: String,
        voiceName: String,
        elevenLabsKey: String,
        googleCloudKey: String,
        speed: Float,
        pitch: Float
    ): File? {
        return when (provider.lowercase()) {
            "elevenlabs" -> {
                if (elevenLabsKey.isNotBlank()) fetchElevenLabsAudio(cleanText, voiceName, elevenLabsKey) else null
            }
            "google_cloud" -> {
                if (googleCloudKey.isNotBlank()) fetchGoogleCloudTtsAudio(cleanText, voiceName, googleCloudKey, speed, pitch) else null
            }
            "openai" -> fetchPollinationsTtsAudio(cleanText, voiceName)
            "gemini" -> {
                val mappedVoice = when (voiceName.lowercase()) {
                    "puck", "charon", "fenrir" -> "echo"
                    "kore", "aoede" -> "nova"
                    else -> voiceName.lowercase()
                }
                fetchPollinationsTtsAudio(cleanText, mappedVoice)
            }
            else -> fetchPollinationsTtsAudio(cleanText, "echo")
        }
    }

    private suspend fun fetchPollinationsTtsAudio(text: String, voiceName: String): File? = withContext(Dispatchers.IO) {
        try {
            val encodedText = URLEncoder.encode(text.take(600), "UTF-8")
            val voiceParam = URLEncoder.encode(voiceName.lowercase(), "UTF-8")
            val urlString = "https://text.pollinations.ai/prompt/$encodedText?voice=$voiceParam&model=openai"

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val tempFile = File(context.cacheDir, "stream_voice_${System.currentTimeMillis()}.mp3")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext tempFile
            }
        } catch (e: Exception) {
            Log.e("AiVoiceManager", "Pollinations Voice API error: ${e.localizedMessage}")
        }
        return@withContext null
    }

    private suspend fun fetchElevenLabsAudio(text: String, voiceId: String, apiKey: String): File? = withContext(Dispatchers.IO) {
        try {
            val realVoiceId = if (voiceId.isBlank()) "21m00Tcm4TlvDq8ikWAM" else voiceId
            val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$realVoiceId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("xi-api-key", apiKey)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 6000
            conn.readTimeout = 12000
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("text", text.take(600))
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
                val tempFile = File(context.cacheDir, "elevenlabs_${System.currentTimeMillis()}.mp3")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext tempFile
            }
        } catch (e: Exception) {
            Log.e("AiVoiceManager", "ElevenLabs API error: ${e.localizedMessage}")
        }
        return@withContext null
    }

    private suspend fun fetchGoogleCloudTtsAudio(
        text: String,
        voiceName: String,
        apiKey: String,
        speed: Float,
        pitch: Float
    ): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 6000
            conn.readTimeout = 12000
            conn.doOutput = true

            val langCode = if (voiceName.startsWith("id-")) "id-ID" else "en-US"
            val jsonBody = JSONObject().apply {
                put("input", JSONObject().put("text", text.take(600)))
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
                    val audioBytes = android.util.Base64.decode(audioContentBase64, android.util.Base64.DEFAULT)
                    val tempFile = File(context.cacheDir, "google_tts_${System.currentTimeMillis()}.mp3")
                    FileOutputStream(tempFile).use { it.write(audioBytes) }
                    return@withContext tempFile
                }
            }
        } catch (e: Exception) {
            Log.e("AiVoiceManager", "Google Cloud Voice API error: ${e.localizedMessage}")
        }
        return@withContext null
    }

    private suspend fun playAudioFileAndWait(file: File) = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Unit>()
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    deferred.complete(Unit)
                    file.delete()
                }
                setOnErrorListener { _, _, _ ->
                    deferred.complete(Unit)
                    file.delete()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            file.delete()
            deferred.complete(Unit)
        }
        deferred.await()
    }

    fun destroy() {
        stop()
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("```[\\s\\S]*?```"), " kode program ")
            .replace(Regex("`[^`]*`"), "")
            .replace(Regex("https?://\\S+"), " tautan web ")
            .replace(Regex("[#*_~\\[\\]()<>{}\\\\/'\"]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
