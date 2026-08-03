package com.example.util

import android.util.Base64
import com.example.data.model.AttachedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object JsFileInspector {

    private const val MAX_CHUNK_CHARS = 8000 // Safe preview chunk size to avoid memory & token issues
    private const val LARGE_FILE_CHAR_THRESHOLD = 15000

    private val FUNCTION_REGEX = Regex(
        """(?:async\s+)?function\s+([a-zA-Z0-9_${'$'}]+)\s*\(([^)]*)\)|(?:const|let|var)\s+([a-zA-Z0-9_${'$'}]+)\s*=\s*(?:async\s*)?\(([^)]*)\)\s*=>|(?:class)\s+([a-zA-Z0-9_${'$'}]+)""",
        RegexOption.MULTILINE
    )

    private val VARIABLE_REGEX = Regex(
        """(?:const|let|var)\s+([a-zA-Z0-9_${'$'}]+)\s*=""",
        RegexOption.MULTILINE
    )

    private val IMPORT_REGEX = Regex(
        """(?:import\s+.*?from\s+['"][^'"]+['"]|require\(['"][^'"]+['"]\))""",
        RegexOption.MULTILINE
    )

    private val ROUTE_MENU_REGEX = Regex(
        """(?:app|router|bot)\.(?:get|post|put|delete|use|on|command)\s*\(\s*['"]([^'"]+)['"]|(?:case\s+['"]([^'"]+)['"]\s*:)|(?:menu|command|endpoint|route)\s*:\s*['"]([^'"]+)['"]""",
        RegexOption.IGNORE_CASE
    )

    suspend fun inspectContent(
        fileName: String,
        fileSize: Long,
        formattedSize: String,
        mimeType: String,
        extension: String,
        rawContent: String
    ): AttachedFile = withContext(Dispatchers.IO) {
        val lines = rawContent.lines()
        val lineCount = lines.size
        val charCount = rawContent.length

        val isLargeFile = charCount > LARGE_FILE_CHAR_THRESHOLD || fileSize > 25 * 1024
        val isTruncated = charCount > MAX_CHUNK_CHARS

        val displayContent = if (isTruncated) {
            rawContent.take(MAX_CHUNK_CHARS) + "\n\n... [DIPOTONG: ${charCount - MAX_CHUNK_CHARS} karakter sisanya disembunyikan untuk menghemat token API & RAM HP]"
        } else {
            rawContent
        }

        val base64Encoded = try {
            Base64.encodeToString(rawContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }

        val detectedFunctions = mutableListOf<String>()
        val detectedVariables = mutableListOf<String>()
        val detectedImports = mutableListOf<String>()
        val detectedRoutesAndMenus = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            val lineNum = index + 1
            val trimmed = line.trim()

            // Extract Functions
            FUNCTION_REGEX.findAll(trimmed).forEach { match ->
                val funcName = match.groupValues.firstOrNull { it.isNotBlank() && it != match.value } ?: "anonymous"
                if (funcName !in detectedFunctions && funcName.length > 1) {
                    detectedFunctions.add("Line $lineNum: $funcName()")
                }
            }

            // Extract Variables
            VARIABLE_REGEX.findAll(trimmed).forEach { match ->
                val varName = match.groupValues.getOrNull(1)
                if (!varName.isNullOrBlank() && varName !in detectedVariables && varName.length > 1) {
                    detectedVariables.add("Line $lineNum: $varName")
                }
            }

            // Extract Imports
            IMPORT_REGEX.findAll(trimmed).forEach { match ->
                val imp = match.value
                if (imp !in detectedImports) {
                    detectedImports.add("Line $lineNum: $imp")
                }
            }

            // Extract Routes / Menus
            ROUTE_MENU_REGEX.findAll(trimmed).forEach { match ->
                val route = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
                if (!route.isNullOrBlank() && route !in detectedRoutesAndMenus) {
                    detectedRoutesAndMenus.add("Line $lineNum: $route")
                }
            }
        }

        val summaryReport = buildString {
            appendLine("🔍 FILE CONTENT INSPECTION REPORT")
            appendLine("----------------------------------------")
            appendLine("File Name   : $fileName")
            appendLine("File Size   : $formattedSize ($fileSize bytes)")
            appendLine("Total Lines : $lineCount lines ($charCount characters)")
            appendLine("Extension   : .${extension.uppercase()}")
            appendLine("MIME Type   : $mimeType")
            appendLine("Optimization: ${if (isTruncated) "TOKEN CHUNKING ACTIVE (${MAX_CHUNK_CHARS} char preview)" else "FULL CONTENT LOADED"}")
            appendLine()
            appendLine("📊 CODE STRUCTURE ANALYSIS:")
            appendLine(" • Detected Functions : ${detectedFunctions.size}")
            appendLine(" • Detected Variables : ${detectedVariables.size}")
            appendLine(" • Detected Imports   : ${detectedImports.size}")
            appendLine(" • Detected Routes/Menus: ${detectedRoutesAndMenus.size}")
            
            if (detectedRoutesAndMenus.isNotEmpty()) {
                appendLine()
                appendLine("🧭 DETECTED ROUTES / MENUS / COMMANDS:")
                detectedRoutesAndMenus.take(15).forEach { appendLine("   $it") }
            }

            if (detectedFunctions.isNotEmpty()) {
                appendLine()
                appendLine("⚡ KEY FUNCTIONS DETECTED:")
                detectedFunctions.take(20).forEach { appendLine("   $it") }
            }

            if (detectedImports.isNotEmpty()) {
                appendLine()
                appendLine("📦 DEPENDENCIES & IMPORTS:")
                detectedImports.take(10).forEach { appendLine("   $it") }
            }
        }

        AttachedFile(
            name = fileName,
            sizeBytes = fileSize,
            formattedSize = formattedSize,
            mimeType = mimeType,
            extension = extension,
            isText = true,
            contentPayload = displayContent,
            lineCount = lineCount,
            functionsCount = detectedFunctions.size,
            variablesCount = detectedVariables.size,
            routesCount = detectedRoutesAndMenus.size,
            inspectionSummary = summaryReport,
            detectedFunctions = detectedFunctions,
            detectedVariables = detectedVariables,
            detectedRoutesAndMenus = detectedRoutesAndMenus,
            base64Data = base64Encoded,
            isTruncated = isTruncated,
            isLargeFile = isLargeFile,
            fullRawContent = rawContent
        )
    }
}

