package com.example.data.model

data class CustomAiModel(
    val id: String,
    val name: String,
    val apiKey: String = "",
    val providerType: String = "Groq", // "Groq", "OpenRouter", "Mistral", "Gemini", "OpenAI"
    val baseUrl: String = "" // Custom API endpoint base URL (optional)
)

