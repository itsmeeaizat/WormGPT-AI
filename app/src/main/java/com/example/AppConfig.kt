package com.example

import com.example.data.model.CustomAiModel

/**
 * ============================================================================
 * KONFIGURASI DEFAULT APLIKASI (DEFAULT_CONFIG)
 * 
 * Tempat Pengaturan Model Bawaan & API Key Utama (Default) di dalam Kode.
 * 
 * Anda dapat memasukkan API Key utama, menetapkan Model bawaan, dan menambah
 * daftar Model Kustom bawaan di sini.
 * Saat aplikasi di-export, nilai di DEFAULT_CONFIG ini akan otomatis aktif 
 * dan tersinkronisasi secara langsung.
 * ============================================================================
 */
object DEFAULT_CONFIG {
    // ------------------------------------------------------------------------
    // 1. MODEL BAWAAN DEFAULT
    // Contoh Pilihan Model:
    // - "gemini-3.5-flash"
    // - "gemini-1.5-pro"
    // - "groq/llama-3.3-70b-versatile"
    // - "openrouter/anthropic/claude-3.5-sonnet"
    // - "mistral/mistral-large-latest"
    // ------------------------------------------------------------------------
    var DEFAULT_MODEL: String = "gemini-3.5-flash"

    // ------------------------------------------------------------------------
    // 2. API KEY BAWAAN DEFAULT (UTAMA)
    // Masukkan API Key utama Anda di dalam tanda petik pada variabel di bawah:
    // ------------------------------------------------------------------------
    var DEFAULT_GEMINI_API_KEY: String = ""     // Contoh: "AIzaSy..."
    var DEFAULT_GROQ_API_KEY: String = ""       // Contoh: "gsk_..."
    var DEFAULT_OPENROUTER_API_KEY: String = "" // Contoh: "sk-or-..."
    var DEFAULT_MISTRAL_API_KEY: String = ""    // Contoh: "..."

    // ------------------------------------------------------------------------
    // 3. SUARA AI BAWAAN DEFAULT (VOICE ENGINE & VOICE OPTIONS)
    // ------------------------------------------------------------------------
    var DEFAULT_VOICE_PROVIDER: String = "gemini" // "gemini", "openai", "elevenlabs", "google_cloud", "system"
    var DEFAULT_VOICE_NAME: String = "Puck"       // Contoh Gemini Voices: "Puck", "Kore", "Charon", "Fenrir", "Aoede"
    var DEFAULT_ELEVENLABS_API_KEY: String = ""   // Contoh Key ElevenLabs
    var DEFAULT_GOOGLE_TTS_API_KEY: String = ""   // Contoh Key Google Cloud TTS

    // ------------------------------------------------------------------------
    // 4. DAFTAR MODEL KUSTOM BAWAAN (DEFAULT CUSTOM MODELS)
    // Anda dapat menambah atau mengonfigurasi model kustom bawaan di sini
    // agar otomatis tersinkronisasi saat aplikasi di-build menjadi APK.
    // ------------------------------------------------------------------------
    var DEFAULT_CUSTOM_MODELS: List<CustomAiModel> = listOf(
        CustomAiModel(
            id = "groq/llama-3.3-70b-versatile",
            name = "Groq Llama 3.3 70B",
            apiKey = "",
            providerType = "Groq"
        ),
        CustomAiModel(
            id = "openrouter/deepseek/deepseek-r1",
            name = "DeepSeek R1 (OpenRouter)",
            apiKey = "",
            providerType = "OpenRouter"
        )
    )
}

