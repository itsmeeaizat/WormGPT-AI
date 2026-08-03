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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttachedFile
import com.example.ui.theme.WormGptCodeBg
import com.example.ui.theme.WormGptGreenTerminal
import com.example.ui.theme.WormGptRedAccent
import com.example.ui.theme.WormGptRedDark

@Composable
fun FileInspectorPreviewCard(
    attachedFile: AttachedFile,
    onRemove: () -> Unit,
    onSendAnalysis: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Summary, 1 = Structure, 2 = Code

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121214))
            .border(1.dp, WormGptRedAccent.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WormGptRedDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "JS Code Inspector",
                            tint = WormGptRedAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = attachedFile.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(WormGptRedAccent)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = attachedFile.extension.uppercase().ifBlank { "FILE" },
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = "${attachedFile.formattedSize} • ${attachedFile.lineCount} lines",
                            color = Color(0xFFA1A1AA),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attached file",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stat Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatChip(
                    label = "Functions",
                    value = attachedFile.functionsCount.toString(),
                    color = WormGptGreenTerminal
                )
                StatChip(
                    label = "Variables",
                    value = attachedFile.variablesCount.toString(),
                    color = Color(0xFF60A5FA)
                )
                StatChip(
                    label = "Routes/Menus",
                    value = attachedFile.routesCount.toString(),
                    color = Color(0xFFF59E0B)
                )
            }

            // Large File / Token Chunking Warning & Confirmation Dialog
            var showFullReadDialog by remember { mutableStateOf(false) }
            var isFullContentActive by remember { mutableStateOf(false) }

            if (attachedFile.isTruncated || attachedFile.isLargeFile) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF271A0C))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFullContentActive) "⚠️ UNLIMITED MODE ACTIVE (HIGH TOKEN CONSUMPTION)" else "⚡ TOKEN OPTIMIZATION ACTIVE",
                                color = if (isFullContentActive) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isFullContentActive)
                                    "Membaca seluruh isi file (${attachedFile.fullRawContent?.length ?: 0} char). Hati-hati kuota token API!"
                                else
                                    "File dipotong (8000 char) agar tidak memicu error rate limit HTTP 429 & lag RAM HP.",
                                color = Color(0xFFD4D4D8),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isFullContentActive) Color(0xFF3F3F46) else Color(0xFFB45309))
                                .clickable { showFullReadDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isFullContentActive) "REVERT CHUNK" else "BACA FULL",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (showFullReadDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showFullReadDialog = false },
                    title = {
                        Text(
                            text = "⚡ Opsi Pembacaan Token File",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "File '${attachedFile.name}' berukuran ${attachedFile.formattedSize} (${attachedFile.lineCount} baris).\n\n" +
                                    "• Hemat Token (Rekomendasi): Membaca ringkasan AST, daftar fungsi, dan 8.000 karakter pertama file.\n" +
                                    "• Baca Seluruh File: Mengunggah seluruh isi mentah file. Menggunakan token yang jauh lebih besar dan dapat memicu error HTTP 429 Rate Limit.",
                            fontSize = 12.sp,
                            color = Color(0xFFD4D4D8)
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                isFullContentActive = true
                                showFullReadDialog = false
                            }
                        ) {
                            Text("Baca Seluruhnya (Banyak Token)", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                isFullContentActive = false
                                showFullReadDialog = false
                            }
                        ) {
                            Text("Hemat Token (Rekomendasi)", color = WormGptGreenTerminal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFF18181B),
                    titleContentColor = Color.White,
                    textContentColor = Color(0xFFD4D4D8)
                )
            }


            Spacer(modifier = Modifier.height(10.dp))

            // Inspection View Switcher Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF18181B))
                    .padding(3.dp)
            ) {
                InspectorTabButton(
                    text = "Summary",
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                InspectorTabButton(
                    text = "Structure (${attachedFile.functionsCount})",
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
                InspectorTabButton(
                    text = "Preview",
                    isSelected = selectedTab == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 2 }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Content Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WormGptCodeBg)
                    .border(0.5.dp, Color(0xFF27272A), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Summary Text
                        Text(
                            text = attachedFile.inspectionSummary ?: attachedFile.contentPayload.take(500),
                            color = Color(0xFFD4D4D8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                    1 -> {
                        // Structure List
                        Column {
                            if (attachedFile.detectedRoutesAndMenus.isNotEmpty()) {
                                Text(
                                    text = "🧭 ROUTES / MENUS / COMMANDS:",
                                    color = Color(0xFFF59E0B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                attachedFile.detectedRoutesAndMenus.take(3).forEach {
                                    Text(
                                        text = "  • $it",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            if (attachedFile.detectedFunctions.isNotEmpty()) {
                                Text(
                                    text = "⚡ DETECTED FUNCTIONS:",
                                    color = WormGptGreenTerminal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                attachedFile.detectedFunctions.take(4).forEach {
                                    Text(
                                        text = "  • $it",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "No top-level functions explicitly detected.",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    2 -> {
                        // Code Raw Content Preview
                        Text(
                            text = attachedFile.contentPayload.take(1200),
                            color = WormGptGreenTerminal,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy Inspection Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF27272A))
                        .clickable {
                            val report = attachedFile.inspectionSummary ?: attachedFile.contentPayload
                            copyToClipboard(context, report)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Analysis",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "COPY REPORT",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Send Analysis to AI Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WormGptRedDark)
                        .border(1.dp, WormGptRedAccent, RoundedCornerShape(8.dp))
                        .clickable {
                            val prompt = "Tolong periksa dan audit file '${attachedFile.name}':\n\n" +
                                    (attachedFile.inspectionSummary ?: attachedFile.contentPayload.take(2000))
                            onSendAnalysis(prompt)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Audit via AI",
                        tint = WormGptRedAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AUDIT CODE VIA AI",
                        color = WormGptRedAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            color = Color(0xFFA1A1AA),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun InspectorTabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) WormGptRedDark else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) WormGptRedAccent else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color(0xFFA1A1AA),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("File Inspection Report", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Laporan inspeksi file tersalin ke clipboard", Toast.LENGTH_SHORT).show()
}
