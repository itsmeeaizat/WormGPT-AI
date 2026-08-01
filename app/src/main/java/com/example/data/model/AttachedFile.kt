package com.example.data.model

data class AttachedFile(
    val name: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val mimeType: String,
    val extension: String,
    val isText: Boolean,
    val contentPayload: String
)
