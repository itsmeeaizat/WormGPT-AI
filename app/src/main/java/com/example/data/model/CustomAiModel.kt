package com.example.data.model

data class CustomAiModel(
    val id: String,
    val name: String,
    val apiKey: String,
    val providerType: String // "Groq", "OpenRouter", "Mistral", "Gemini", "OpenAI"
)
