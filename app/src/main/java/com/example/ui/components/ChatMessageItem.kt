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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material.icons.filled.LocationOn
import android.content.Intent
import android.net.Uri
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ZoomIn
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.db.ChatMessageEntity

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    isStreaming: Boolean = false,
    onReplyMessage: (ChatMessageEntity) -> Unit = {},
    onReadAloud: ((String) -> Unit)? = null,
    isSpeakingThisMessage: Boolean = false,
    onStopSpeaking: (() -> Unit)? = null,
    onOpenCanvas: ((imageUrl: String?, textContent: String?) -> Unit)? = null,
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
                    Column {
                        if (!message.repliedText.isNullOrEmpty()) {
                            QuotedMessageView(
                                repliedText = message.repliedText,
                                senderName = message.repliedSender ?: "USER"
                            )
                        }
                        val locData = parseLocationData(message.content)
                        SelectionContainer {
                            if (locData != null) {
                                Column {
                                    LocationMapCard(
                                        location = locData,
                                        context = context
                                    )
                                    if (locData.remainingText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = locData.remainingText,
                                            color = Color(0xFF0F0F0F),
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = message.content,
                                    color = Color(0xFF0F0F0F),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
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
                // AI Avatar Icon (Matches App Icon)
                if (isError) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Nova AI",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nova_ai_logo),
                        contentDescription = "Nova AI Avatar",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Message Header label
                    Text(
                        text = if (isError) "System Error" else "Nova AI",
                        color = Color(0xFF0F0F0F),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    if (!message.repliedText.isNullOrEmpty()) {
                        QuotedMessageView(
                            repliedText = message.repliedText,
                            senderName = message.repliedSender ?: "Nova AI"
                        )
                    }

                    // Message content rendering with streaming cursor if active
                    FormattedMessageText(
                        content = message.content.trimEnd(),
                        isStreaming = isStreaming,
                        context = context
                    )

                    // Floating Context Menu Bar (SALIN, BALAS, BACAKAN, UNDUH)
                    val isLongOrDocument = message.content.contains("```") ||
                            message.content.contains("# ") ||
                            message.content.length > 150 ||
                            detectDocumentExtension(message.content) != "txt"

                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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

                        if (isLongOrDocument) {
                            // Download File Button (.doc, .js, .txt, etc.)
                            val docExt = detectDocumentExtension(message.content)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val savedPath = saveTextToDownloads(context, message.content, docExt)
                                        if (savedPath != null) {
                                            Toast.makeText(context, "✅ File .$docExt disimpan ke $savedPath", Toast.LENGTH_LONG).show()
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
                                    contentDescription = "Unduh File",
                                    tint = Color(0xFF10A37F),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("UNDUH .${docExt.uppercase()}", color = Color(0xFF10A37F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Download PDF Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val savedPath = saveAsPdfToDownloads(context, "Dokumen_NovaAI", message.content)
                                        if (savedPath != null) {
                                            Toast.makeText(context, "📄 PDF disimpan ke $savedPath", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "❌ Gagal membuat PDF.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .background(Color(0xFFFEF2F2))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Unduh PDF",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("UNDUH PDF", color = Color(0xFFDC2626), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
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
    SelectionContainer {
        val locData = parseLocationData(content)
        if (locData != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LocationMapCard(
                    location = locData,
                    context = context
                )
                if (locData.remainingText.isNotBlank()) {
                    Text(
                        text = locData.remainingText,
                        color = Color(0xFF0F0F0F),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            val parts = content.trimEnd().split("```")

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    val clip = ClipData.newPlainText("Nova AI Message", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Teks berhasil disalin", Toast.LENGTH_SHORT).show()
}

private fun saveTextToDownloads(context: Context, textContent: String, defaultExtension: String = "txt"): String? {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val ext = defaultExtension.lowercase().removePrefix(".")
    val filename = "NovaAI_Export_${timestamp}.$ext"

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

private fun saveAsPdfToDownloads(context: Context, docTitle: String, textContent: String): String? {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val safeTitle = docTitle.replace("[^a-zA-Z0-9_]".toRegex(), "_")
    val filename = "NovaAI_${safeTitle}_${timestamp}.pdf"

    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions: 595x842 pt

    val bodyPaint = Paint().apply {
        textSize = 11f
        color = android.graphics.Color.BLACK
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    val headerPaint = Paint().apply {
        textSize = 16f
        color = android.graphics.Color.rgb(16, 163, 127)
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val lines = textContent.split("\n")
    var pageNumber = 1
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    var y = 50f

    // Draw Title Header
    canvas.drawText("Nova AI Document - $docTitle", 40f, y, headerPaint)
    y += 30f

    for (rawLine in lines) {
        val line = rawLine.replace("```", "").replace("**", "")
        val words = line.split(" ")
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (bodyPaint.measureText(testLine) > 515f) {
                canvas.drawText(currentLine, 40f, y, bodyPaint)
                y += 16f
                currentLine = word
                if (y > 790f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(nextPageInfo)
                    canvas = page.canvas
                    y = 50f
                }
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, 40f, y, bodyPaint)
            y += 16f
            if (y > 790f) {
                pdfDocument.finishPage(page)
                pageNumber++
                val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(nextPageInfo)
                canvas = page.canvas
                y = 50f
            }
        }
    }

    pdfDocument.finishPage(page)

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                "Download/$filename"
            } else {
                pdfDocument.close()
                null
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val targetFile = File(downloadsDir, filename)
            FileOutputStream(targetFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            "Download/$filename"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        try { pdfDocument.close() } catch (_: Exception) {}
        null
    }
}

@Composable
fun QuotedMessageView(
    repliedText: String,
    senderName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        color = Color(0xFFE4E4E7).copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .background(Color(0xFF10A37F), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (senderName == "USER") "Anda" else senderName,
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10A37F)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = repliedText,
                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF3F3F46)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: String = "10.0m",
    val address: String,
    val remainingText: String = ""
)

fun parseLocationData(content: String): LocationData? {
    // 1. Tag format: [LOCATION_CARD|lat=...|lon=...|acc=...|addr=...]
    if (content.contains("[LOCATION_CARD|")) {
        try {
            val tagStart = content.indexOf("[LOCATION_CARD|")
            val tagEnd = content.indexOf("]", tagStart)
            if (tagEnd > tagStart) {
                val tagStr = content.substring(tagStart + 15, tagEnd)
                var lat = -6.2088
                var lon = 106.8456
                var acc = "10m"
                var addr = ""
                tagStr.split("|").forEach { part ->
                    val keyValue = part.split("=", limit = 2)
                    if (keyValue.size == 2) {
                        when (keyValue[0]) {
                            "lat" -> lat = keyValue[1].toDoubleOrNull() ?: lat
                            "lon" -> lon = keyValue[1].toDoubleOrNull() ?: lon
                            "acc" -> acc = keyValue[1]
                            "addr" -> addr = keyValue[1]
                        }
                    }
                }
                val remaining = (content.substring(0, tagStart) + content.substring(tagEnd + 1)).trim()
                return LocationData(lat, lon, acc, addr.ifBlank { "Lat: $lat, Lon: $lon" }, remaining)
            }
        } catch (_: Exception) {}
    }

    // 2. Legacy Telemetry Scan format
    if (content.contains("LOCATION TELEMETRY SCAN") || (content.contains("Latitude:") && content.contains("Longitude:"))) {
        try {
            val latMatch = Regex("Latitude:\\s*([\\d.-]+)").find(content)
            val lonMatch = Regex("Longitude:\\s*([\\d.-]+)").find(content)
            val addrMatch = Regex("Resolved Address:\\s*([^\\n]+)").find(content)

            val lat = latMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: -6.2088
            val lon = lonMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 106.8456
            val addr = addrMatch?.groupValues?.get(1)?.trim() ?: "Jl. Palka 23, Kp. Barabung, Serang, Banten 42168"

            return LocationData(lat, lon, "10m", addr, "")
        } catch (_: Exception) {}
    }

    return null
}

@Composable
fun LocationMapCard(
    location: LocationData,
    context: Context,
    modifier: Modifier = Modifier
) {
    val mapsIntent = remember(location.latitude, location.longitude, location.address) {
        val uriStr = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${Uri.encode(location.address)})"
        Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
            setPackage("com.google.android.apps.maps")
        }
    }

    val fallbackIntent = remember(location.latitude, location.longitude) {
        val webUriStr = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
        Intent(Intent.ACTION_VIEW, Uri.parse(webUriStr))
    }

    fun openMaps() {
        try {
            context.startActivity(mapsIntent)
        } catch (_: Exception) {
            try {
                context.startActivity(fallbackIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Tidak dapat membuka Google Maps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .clickable { openMaps() }
    ) {
        // 1. Google Maps Snapshot Graphic Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFFE5ECE9))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Water and park shape accents
                drawCircle(color = Color(0xFFD4E4DD), center = Offset(w * 0.2f, h * 0.3f), radius = 60.dp.toPx())
                drawCircle(color = Color(0xFFCBE3F0), center = Offset(w * 0.85f, h * 0.7f), radius = 90.dp.toPx())

                // Main streets
                val path1 = Path().apply {
                    moveTo(0f, h * 0.45f)
                    lineTo(w, h * 0.55f)
                }
                drawPath(path1, color = Color.White, style = Stroke(width = 16.dp.toPx()))

                val path2 = Path().apply {
                    moveTo(w * 0.48f, 0f)
                    lineTo(w * 0.52f, h)
                }
                drawPath(path2, color = Color.White, style = Stroke(width = 14.dp.toPx()))

                // Secondary roads
                val path3 = Path().apply {
                    moveTo(0f, h * 0.8f)
                    lineTo(w, h * 0.2f)
                }
                drawPath(path3, color = Color(0xFFFAF8F5), style = Stroke(width = 8.dp.toPx()))

                // GPS Accuracy radius
                drawCircle(
                    color = Color(0x334285F4),
                    center = Offset(w / 2f, h / 2f),
                    radius = 32.dp.toPx()
                )
            }

            // Red Google Maps Location Marker Pin
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp, 5.dp)
                        .align(Alignment.BottomCenter)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f))
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pin Lokasi",
                    tint = Color(0xFFEA4335),
                    modifier = Modifier.size(40.dp)
                )
            }

            // Google Maps Badge Top-Left
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Google Maps",
                    color = Color(0xFF202124),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // "Buka Peta ↗" Pill Button Bottom-Right
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF10A37F))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Buka Peta ↗",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Address & Subtitle Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFEA4335),
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = location.address,
                        color = Color(0xFF18181B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.6f", location.latitude)}, ${String.format(Locale.getDefault(), "%.6f", location.longitude)} • GPS Presisi",
                        color = Color(0xFF71717A),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
