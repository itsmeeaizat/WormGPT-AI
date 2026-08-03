package com.example.data.model

data class AttachedFile(
    val name: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val mimeType: String,
    val extension: String,
    val isText: Boolean,
    val contentPayload: String,
    val lineCount: Int = 0,
    val functionsCount: Int = 0,
    val variablesCount: Int = 0,
    val routesCount: Int = 0,
    val inspectionSummary: String? = null,
    val detectedFunctions: List<String> = emptyList(),
    val detectedVariables: List<String> = emptyList(),
    val detectedRoutesAndMenus: List<String> = emptyList(),
    val base64Data: String? = null,
    val isTruncated: Boolean = false,
    val isLargeFile: Boolean = false,
    val fullRawContent: String? = null
)

