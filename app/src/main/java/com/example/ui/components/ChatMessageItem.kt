package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ChatMessageEntity
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptCodeBg
import com.example.ui.theme.WormGptGreenTerminal
import com.example.ui.theme.WormGptRedAccent
import com.example.ui.theme.WormGptRedDark
import com.example.ui.theme.WormGptSurface
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

                        // Copy Action
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    copyToClipboard(context, message.content)
                                }
                                .background(Color(0xFF27272A).copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Text",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COPY",
                                color = Color(0xFFA1A1AA),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
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
                val lang = if (lines.isNotEmpty() && lines[0].length < 15 && !lines[0].contains(" ")) {
                    lines[0].uppercase()
                } else {
                    "CODE"
                }
                val codeText = if (lang != "CODE" && lines.size > 1) {
                    lines.drop(1).joinToString("\n")
                } else {
                    part.trim()
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
                                text = "[$lang]",
                                color = WormGptGreenTerminal,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "COPY BLOCK",
                                color = Color(0xFFA1A1AA),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { copyToClipboard(context, codeText) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
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
                // Regular markdown text
                if (part.trim().isNotEmpty()) {
                    Text(
                        text = part.trim(),
                        color = Color(0xFFE0E0E0),
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
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
