package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    isStreaming: Boolean = false,
    onReplyMessage: (ChatMessageEntity) -> Unit = {},
    onReadAloud: ((String) -> Unit)? = null,
    isSpeakingThisMessage: Boolean = false,
    onStopSpeaking: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == "USER"
    val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User Message - ChatGPT Style Pill Bubble (Light Mode)
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomEnd = 4.dp,
                                bottomStart = 20.dp
                            )
                        )
                        .background(Color(0xFFF4F4F5))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { showContextMenu = !showContextMenu },
                                onTap = { showContextMenu = !showContextMenu }
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = Color(0xFF0F0F0F),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }

                // Floating Action Strip on Long-Press / Tap
                AnimatedVisibility(visible = showContextMenu) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(0.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reply
                        Row(
                            modifier = Modifier
                                .clickable {
                                    showContextMenu = false
                                    onReplyMessage(message)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Balas",
                                tint = Color(0xFF5D5D6D),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BALAS", color = Color(0xFF5D5D6D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Copy
                        Row(
                            modifier = Modifier
                                .clickable {
                                    showContextMenu = false
                                    copyToClipboard(context, message.content)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin",
                                tint = Color(0xFF5D5D6D),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SALIN", color = Color(0xFF5D5D6D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // AI Response - ChatGPT Style Natural Text Stream Flow (Light Mode)
            val isError = message.isError

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showContextMenu = !showContextMenu }
                        )
                    },
                verticalAlignment = Alignment.Top
            ) {
                // OpenAI / AI Avatar Icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isError) Color(0xFFDC2626) else Color(0xFF10A37F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Warning else Icons.Default.AutoAwesome,
                        contentDescription = "WormGPT AI",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Message Header label
                    Text(
                        text = if (isError) "System Error" else "WormGPT",
                        color = Color(0xFF0F0F0F),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Message content rendering with streaming cursor if active
                    FormattedMessageText(
                        content = message.content,
                        isStreaming = isStreaming,
                        context = context
                    )

                    // Floating Context Menu Bar (SALIN, BALAS, BACAKAN, UNDUH)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { copyToClipboard(context, message.content) }
                                .background(Color(0xFFF4F4F5))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin",
                                tint = Color(0xFF5D5D6D),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SALIN", color = Color(0xFF5D5D6D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Reply Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onReplyMessage(message) }
                                .background(Color(0xFFF4F4F5))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Balas",
                                tint = Color(0xFF5D5D6D),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BALAS", color = Color(0xFF5D5D6D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Read Aloud Button
                        if (!isError) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (isSpeakingThisMessage) {
                                            onStopSpeaking?.invoke()
                                        } else {
                                            onReadAloud?.invoke(message.content)
                                        }
                                    }
                                    .background(if (isSpeakingThisMessage) Color(0xFFE6F4F1) else Color(0xFFF4F4F5))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingThisMessage) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Bacakan",
                                    tint = if (isSpeakingThisMessage) Color(0xFF10A37F) else Color(0xFF5D5D6D),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSpeakingThisMessage) "STOP" else "BACAKAN",
                                    color = if (isSpeakingThisMessage) Color(0xFF10A37F) else Color(0xFF5D5D6D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Download Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val ext = detectDocumentExtension(message.content)
                                    val savedPath = saveTextToDownloads(context, message.content, ext)
                                    if (savedPath != null) {
                                        Toast.makeText(context, "✅ Disimpan ke $savedPath", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "❌ Gagal mengunduh file.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .background(Color(0xFFE6F4F1))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Unduh",
                                tint = Color(0xFF10A37F),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("UNDUH", color = Color(0xFF10A37F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattedMessageText(
    content: String,
    isStreaming: Boolean = false,
    context: Context
) {
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(0.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(10.dp))
                        .padding(12.dp)
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
                                text = langTag,
                                color = Color(0xFFD4D4D8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Copy Code
                                Text(
                                    text = "Salin",
                                    color = Color(0xFFE4E4E7),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF2D2D2D))
                                        .clickable { copyToClipboard(context, codeText) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )

                                // Download Code File
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10A37F).copy(alpha = 0.25f))
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
                                        tint = Color(0xFF10A37F),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Unduh .$ext",
                                        color = Color(0xFF10A37F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = codeText,
                            color = Color(0xFFF4F4F5),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                // Regular Markdown Text with Blinking Cursor if Streaming
                if (part.trim().isNotEmpty() || (isStreaming && index == parts.lastIndex)) {
                    FormattedMarkdownParagraph(
                        text = part,
                        showCursor = isStreaming && index == parts.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun FormattedMarkdownParagraph(
    text: String,
    showCursor: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val lines = text.trim().lines()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEachIndexed { lineIdx, line ->
            val isLastLine = lineIdx == lines.lastIndex
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmedLine.removePrefix("# ").trim()),
                        color = Color(0xFF0F0F0F),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmedLine.removePrefix("## ").trim()),
                        color = Color(0xFF10A37F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                trimmedLine.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmedLine.removePrefix("### ").trim()),
                        color = Color(0xFF0F0F0F),
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
                            color = Color(0xFF10A37F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineMarkdown(bulletText),
                            color = Color(0xFF0F0F0F),
                            fontSize = 14.sp,
                            lineHeight = 21.sp
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
                            color = Color(0xFF10A37F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineMarkdown(body),
                            color = Color(0xFF0F0F0F),
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                }
                trimmedLine.isNotBlank() -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = parseInlineMarkdown(trimmedLine),
                            color = Color(0xFF0F0F0F),
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                        if (showCursor && isLastLine) {
                            Text(
                                text = " ▌",
                                color = Color(0xFF10A37F),
                                fontSize = 14.sp,
                                modifier = Modifier.alpha(alphaAnim)
                            )
                        }
                    }
                }
            }
        }
        if (showCursor && lines.isEmpty()) {
            Text(
                text = "▌",
                color = Color(0xFF10A37F),
                fontSize = 14.sp,
                modifier = Modifier.alpha(alphaAnim)
            )
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
                                background = Color(0xFFF0F0F2),
                                color = Color(0xFF10A37F),
                                fontSize = 13.sp
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
                                color = Color(0xFF0F0F0F)
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
                                color = Color(0xFF5D5D6D)
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
    val clip = ClipData.newPlainText("WormGPT Message", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Teks berhasil disalin", Toast.LENGTH_SHORT).show()
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
