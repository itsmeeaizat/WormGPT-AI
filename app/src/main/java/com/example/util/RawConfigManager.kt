package com.example.util

import android.content.Context
import com.example.DEFAULT_CONFIG
import com.example.BuildConfig
import com.example.R
import com.example.data.model.CustomAiModel
import org.json.JSONObject

data class CentralModelItem(
    val id: String,           // Model ID Endpoint
    val name: String,         // Judul Model (Model Display Name)
    val baseUrl: String,      // URL Endpoint (Custom Base URL)
    val providerType: String, // Provider Type (Custom, Groq, OpenRouter, Mistral, Gemini, OpenAI, dll)
    val apiKey: String,       // API Key khusus / default pusat
    val description: String   // Deskripsi
)

data class CentralConfig(
    val centralGeminiApiKey: String,
    val centralGroqApiKey: String,
    val centralOpenRouterApiKey: String,
    val centralMistralApiKey: String,
    val defaultModel: String,
    val availableCentralModels: List<CentralModelItem>,
    val ownerNote: String
) {
    fun toCustomAiModels(): List<CustomAiModel> {
        return availableCentralModels.map { item ->
            CustomAiModel(
                id = item.id,
                name = item.name,
                apiKey = item.apiKey,
                providerType = item.providerType.ifBlank { "Custom" },
                baseUrl = item.baseUrl
            )
        }
    }
}

object RawConfigManager {

    fun loadCentralConfig(context: Context): CentralConfig {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.developer_config)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)

            val rawGemini = json.optString("central_gemini_api_key", "").ifBlank {
                DEFAULT_CONFIG.DEFAULT_GEMINI_API_KEY.ifBlank {
                    if (BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") {
                        BuildConfig.GEMINI_API_KEY
                    } else ""
                }
            }

            val rawGroq = json.optString("central_groq_api_key", "").ifBlank { DEFAULT_CONFIG.DEFAULT_GROQ_API_KEY }
            val rawOpenRouter = json.optString("central_openrouter_api_key", "").ifBlank { DEFAULT_CONFIG.DEFAULT_OPENROUTER_API_KEY }
            val rawMistral = json.optString("central_mistral_api_key", "").ifBlank { DEFAULT_CONFIG.DEFAULT_MISTRAL_API_KEY }
            val defaultModel = json.optString("default_model", DEFAULT_CONFIG.DEFAULT_MODEL)
            val note = json.optString("owner_note", "Konfigurasi API Key & Model Default dari Pusat Server Developer")

            val modelsList = mutableListOf<CentralModelItem>()
            val modelsArray = json.optJSONArray("central_custom_models")
                ?: json.optJSONArray("available_central_models")

            if (modelsArray != null) {
                for (i in 0 until modelsArray.length()) {
                    val item = modelsArray.getJSONObject(i)
                    modelsList.add(
                        CentralModelItem(
                            id = item.optString("id").ifBlank { item.optString("model_id") },
                            name = item.optString("name").ifBlank { item.optString("title") },
                            baseUrl = item.optString("baseUrl").ifBlank { item.optString("endpoint_url") },
                            providerType = item.optString("providerType").ifBlank { item.optString("provider_type", "Custom") },
                            apiKey = item.optString("apiKey").ifBlank { item.optString("api_key") },
                            description = item.optString("description")
                        )
                    )
                }
            }

            if (modelsList.isEmpty()) {
                modelsList.add(
                    CentralModelItem(
                        id = "gemini-3.5-flash-lite",
                        name = "Gemini 3.5 Flash Lite (Pusat Standard)",
                        baseUrl = "",
                        providerType = "Gemini",
                        apiKey = rawGemini,
                        description = "Respons super cepat & hemat kuota"
                    )
                )
            }

            CentralConfig(
                centralGeminiApiKey = rawGemini,
                centralGroqApiKey = rawGroq,
                centralOpenRouterApiKey = rawOpenRouter,
                centralMistralApiKey = rawMistral,
                defaultModel = defaultModel,
                availableCentralModels = modelsList,
                ownerNote = note
            )
        } catch (e: Exception) {
            CentralConfig(
                centralGeminiApiKey = if (BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") BuildConfig.GEMINI_API_KEY else DEFAULT_CONFIG.DEFAULT_GEMINI_API_KEY,
                centralGroqApiKey = DEFAULT_CONFIG.DEFAULT_GROQ_API_KEY,
                centralOpenRouterApiKey = DEFAULT_CONFIG.DEFAULT_OPENROUTER_API_KEY,
                centralMistralApiKey = DEFAULT_CONFIG.DEFAULT_MISTRAL_API_KEY,
                defaultModel = DEFAULT_CONFIG.DEFAULT_MODEL,
                availableCentralModels = listOf(
                    CentralModelItem(
                        id = "gemini-3.5-flash-lite",
                        name = "Gemini 3.5 Flash Lite (Pusat Standard)",
                        baseUrl = "",
                        providerType = "Gemini",
                        apiKey = "",
                        description = "Respons super cepat & hemat kuota"
                    )
                ),
                ownerNote = "Fallback Default Config"
            )
        }
    }
}
