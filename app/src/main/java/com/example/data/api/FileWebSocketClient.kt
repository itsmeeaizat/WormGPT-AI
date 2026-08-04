package com.example.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class FileWsEvent {
    data class Connected(val clientId: String) : FileWsEvent()
    data class FileQueued(val fileId: String, val message: String) : FileWsEvent()
    data class FileProcessing(val fileId: String, val message: String) : FileWsEvent()
    data class FileParsed(val fileId: String, val previewText: String, val message: String) : FileWsEvent()
    data class AiStreamStart(val fileId: String, val message: String) : FileWsEvent()
    data class AiStreamChunk(val fileId: String, val chunk: String) : FileWsEvent()
    data class AiStreamComplete(val fileId: String, val fullText: String) : FileWsEvent()
    data class FileError(val fileId: String, val errorMessage: String) : FileWsEvent()
}

class FileWebSocketClient(
    private val baseUrl: String = "ws://10.0.2.2:3000"
) {
    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    var eventListener: ((FileWsEvent) -> Unit)? = null

    fun connect(clientId: String) {
        disconnect()
        val wsUrl = "$baseUrl?clientId=$clientId"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("FileWsClient", "WebSocket Connection Opened")
                eventListener?.invoke(FileWsEvent.Connected(clientId))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("FileWsClient", "WebSocket Received: $text")
                parseAndDispatchEvent(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("FileWsClient", "WebSocket Connection Failure: ${t.localizedMessage}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("FileWsClient", "WebSocket Closed: $reason")
            }
        })
    }

    private fun parseAndDispatchEvent(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val event = json.optString("event", "")
            val fileId = json.optString("fileId", "")

            when (event) {
                "connected" -> {
                    val clientId = json.optString("clientId", "")
                    eventListener?.invoke(FileWsEvent.Connected(clientId))
                }
                "file_queued" -> {
                    val msg = json.optString("message", "File dimasukkan ke antrean")
                    eventListener?.invoke(FileWsEvent.FileQueued(fileId, msg))
                }
                "file_processing" -> {
                    val msg = json.optString("message", "File sedang diproses...")
                    eventListener?.invoke(FileWsEvent.FileProcessing(fileId, msg))
                }
                "file_parsed" -> {
                    val msg = json.optString("message", "Teks file berhasil diekstrak")
                    val preview = json.optString("textPreview", "")
                    eventListener?.invoke(FileWsEvent.FileParsed(fileId, preview, msg))
                }
                "ai_stream_start" -> {
                    val msg = json.optString("message", "AI mulai menjawab...")
                    eventListener?.invoke(FileWsEvent.AiStreamStart(fileId, msg))
                }
                "ai_stream_chunk" -> {
                    val chunk = json.optString("chunk", "")
                    eventListener?.invoke(FileWsEvent.AiStreamChunk(fileId, chunk))
                }
                "ai_stream_complete" -> {
                    val fullText = json.optString("fullText", "")
                    eventListener?.invoke(FileWsEvent.AiStreamComplete(fileId, fullText))
                }
                "file_error" -> {
                    val errMsg = json.optString("message", "Terjadi kesalahan pada file")
                    eventListener?.invoke(FileWsEvent.FileError(fileId, errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("FileWsClient", "Error parsing WebSocket event JSON: ${e.localizedMessage}")
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User disconnected")
            webSocket = null
        } catch (_: Exception) {}
    }
}
