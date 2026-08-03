package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ChatMessageEntity
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptCodeBg
import com.example.ui.theme.WormGptGreenTerminal
import com.example.ui.theme.WormGptRedAccent
import com.example.ui.theme.WormGptRedDark
import com.example.ui.theme.WormGptSurface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == "USER"
    val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User Message
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .background(
                        color = Color(0xFF27272A),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 0.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            Text(
                text = "SENT • $timeFormatted",
                color = Color(0xFF71717A),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, end = 2.dp)
            )
        } else {
            // WormGPT Message
            val isError = message.isError
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isError) {
                                listOf(Color(0xFF2D0A0A), Color(0xFF180505))
                            } else {
                                listOf(Color(0xFF18181B), Color(0xFF09090B))
                            }
                        ),
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 20.dp,
                            bottomEnd = 20.dp,
                            bottomStart = 20.dp
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isError) Color(0xFFEF4444) else WormGptBorderRed.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 20.dp,
                            bottomEnd = 20.dp,
                            bottomStart = 20.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Column {
                    // Header Tag
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isError) Icons.Default.Warning else Icons.Default.Terminal,
                                contentDescription = null,
                                tint = if (isError) Color(0xFFEF4444) else WormGptRedAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isError) "[SYS_EXEC_ERROR]" else "[SYS_OVERRIDE_ACTIVE]",
                                color = if (isError) Color(0xFFEF4444) else WormGptRedAccent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Actions Row (Copy & Download)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Copy Action
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        copyToClipboard(context, message.content)
                                    }
                                    .background(Color(0xFF27272A).copy(alpha = 0.5f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Text",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "SALIN",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Download Action
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val ext = detectDocumentExtension(message.content)
                                        val savedPath = saveTextToDownloads(context, message.content, ext)
                                        if (savedPath != null) {
                                            Toast.makeText(context, "✅ File berhasil diunduh ke $savedPath", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "❌ Gagal mengunduh file.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .background(Color(0xFF1E293B))
                                    .border(0.5.dp, WormGptGreenTerminal.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Unduh File",
                                    tint = WormGptGreenTerminal,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "UNDUH FILE",
                                    color = WormGptGreenTerminal,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Content Rendering (Supports Markdown Code Blocks)
                    FormattedMessageText(content = message.content, context = context)

                    // Indicator Line / Mode Tag Footer
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(28.dp)
                                    .background(
                                        if (isError) Color(0xFFEF4444) else WormGptRedAccent,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(8.dp)
                                    .background(WormGptRedDark, RoundedCornerShape(2.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(8.dp)
                                    .background(WormGptRedDark, RoundedCornerShape(2.dp))
                            )
                        }

                        Text(
                            text = message.modeTag + " • " + timeFormatted,
                            color = WormGptBorderRed,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattedMessageText(
    content: String,
    context: Context
) {
    // Split content by ``` code blocks
    val parts = content.split("```")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block content
                val lines = part.trim().lines()
                val langTag = if (lines.isNotEmpty() && lines[0].length < 15 && !lines[0].contains(" ")) {
                    lines[0].uppercase()
                } else {
                    "CODE"
                }
                val codeText = if (langTag != "CODE" && lines.size > 1) {
                    lines.drop(1).joinToString("\n")
                } else {
                    part.trim()
                }

                val ext = when (langTag.lowercase()) {
                    "js", "javascript" -> "js"
                    "py", "python" -> "py"
                    "kt", "kotlin" -> "kt"
                    "java" -> "java"
                    "html" -> "html"
                    "css" -> "css"
                    "json" -> "json"
                    "md", "markdown" -> "md"
                    "sql" -> "sql"
                    "sh", "bash" -> "sh"
                    else -> "txt"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WormGptCodeBg)
                        .border(0.5.dp, Color(0xFF27272A), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[$langTag]",
                                color = WormGptGreenTerminal,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Copy Code Block Button
                                Text(
                                    text = "SALIN",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF27272A))
                                        .clickable { copyToClipboard(context, codeText) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )

                                // Download Code Block File Button
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF064E3B))
                                        .border(0.5.dp, WormGptGreenTerminal.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .clickable {
                                            val savedPath = saveTextToDownloads(context, codeText, ext)
                                            if (savedPath != null) {
                                                Toast.makeText(context, "✅ File .$ext disimpan ke $savedPath", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "❌ Gagal mengunduh file .$ext", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Unduh Kode",
                                        tint = WormGptGreenTerminal,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "UNDUH (.${ext.uppercase()})",
                                        color = WormGptGreenTerminal,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = codeText,
                            color = Color(0xFF34D399),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Regular markdown text with structured headers, bullet points, quotes, and inline code formatting
                if (part.trim().isNotEmpty()) {
                    FormattedMarkdownParagraph(text = part)
                }
            }
        }
    }
}

@Composable
private fun FormattedMarkdownParagraph(text: String) {
    val lines = text.trim().lines()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmedLine.removePrefix("# ").trim()),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmedLine.removePrefix("## ").trim()),
                        color = WormGptGreenTerminal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmedLine.removePrefix("### ").trim()),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)
                    )
                }
                trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") -> {
                    val bulletText = trimmedLine.removePrefix("- ").removePrefix("* ").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            color = WormGptGreenTerminal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineMarkdown(bulletText),
                            color = Color(0xFFE4E4E7),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
                trimmedLine.matches(Regex("""^\d+\.\s+.*""")) -> {
                    val numPrefix = trimmedLine.substringBefore(" ")
                    val body = trimmedLine.substringAfter(" ")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$numPrefix ",
                            color = WormGptRedAccent,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineMarkdown(body),
                            color = Color(0xFFE4E4E7),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
                trimmedLine.startsWith("> ") -> {
                    val quoteText = trimmedLine.removePrefix("> ").trim()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF18181B))
                            .border(0.5.dp, WormGptRedAccent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = parseInlineMarkdown(quoteText),
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp
                        )
                    }
                }
                trimmedLine.isNotBlank() -> {
                    Text(
                        text = parseInlineMarkdown(trimmedLine),
                        color = Color(0xFFE4E4E7),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val codeIndex = text.indexOf("`", cursor)
            val boldIndex = text.indexOf("**", cursor)
            val italicIndex = if (boldIndex == -1) text.indexOf("*", cursor) else -1

            val nextSpecial = listOfNotNull(
                if (codeIndex != -1) codeIndex else null,
                if (boldIndex != -1) boldIndex else null,
                if (italicIndex != -1 && (boldIndex == -1 || italicIndex < boldIndex)) italicIndex else null
            ).minOrNull()

            if (nextSpecial == null) {
                append(text.substring(cursor))
                break
            }

            if (nextSpecial > cursor) {
                append(text.substring(cursor, nextSpecial))
            }

            when (nextSpecial) {
                codeIndex -> {
                    val endCode = text.indexOf("`", codeIndex + 1)
                    if (endCode != -1) {
                        val codeVal = text.substring(codeIndex + 1, endCode)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0xFF27272A),
                                color = WormGptGreenTerminal,
                                fontSize = 12.sp
                            )
                        ) {
                            append(" $codeVal ")
                        }
                        cursor = endCode + 1
                    } else {
                        append("`")
                        cursor = codeIndex + 1
                    }
                }
                boldIndex -> {
                    val endBold = text.indexOf("**", boldIndex + 2)
                    if (endBold != -1) {
                        val boldVal = text.substring(boldIndex + 2, endBold)
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        ) {
                            append(boldVal)
                        }
                        cursor = endBold + 2
                    } else {
                        append("**")
                        cursor = boldIndex + 2
                    }
                }
                italicIndex -> {
                    val endItalic = text.indexOf("*", italicIndex + 1)
                    if (endItalic != -1) {
                        val italicVal = text.substring(italicIndex + 1, endItalic)
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFFD4D4D8)
                            )
                        ) {
                            append(italicVal)
                        }
                        cursor = endItalic + 1
                    } else {
                        append("*")
                        cursor = italicIndex + 1
                    }
                }
                else -> {
                    cursor++
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("WormGPT Code", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun saveTextToDownloads(context: Context, textContent: String, defaultExtension: String = "txt"): String? {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val ext = defaultExtension.lowercase().removePrefix(".")
    val filename = "WormGPT_Export_${timestamp}.$ext"

    val mimeType = when (ext) {
        "js" -> "application/javascript"
        "py" -> "text/x-python"
        "json" -> "application/json"
        "html" -> "text/html"
        "css" -> "text/css"
        "kt" -> "text/x-kotlin"
        "java" -> "text/x-java-source"
        "md" -> "text/markdown"
        "doc", "docx" -> "text/plain"
        else -> "text/plain"
    }

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(textContent.toByteArray(Charsets.UTF_8))
                }
                "Download/$filename"
            } else null
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val targetFile = File(downloadsDir, filename)
            targetFile.writeText(textContent, Charsets.UTF_8)
            "Download/$filename"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun detectDocumentExtension(content: String): String {
    val lower = content.lowercase()
    return when {
        lower.contains("```js") || lower.contains("```javascript") || lower.contains("function") || lower.contains("const ") -> "js"
        lower.contains("```python") || lower.contains("```py") || lower.contains("def ") -> "py"
        lower.contains("```html") -> "html"
        lower.contains("```json") -> "json"
        lower.contains("curriculum vitae") || lower.contains("portofolio") || lower.contains("resume") || lower.contains("surat lamaran") -> "doc"
        lower.contains("# ") || lower.contains("## ") -> "md"
        else -> "txt"
    }
}
