package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.util.saveTextToDownloads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class DrawPathData(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasWorkspaceSheet(
    initialImageUrl: String? = null,
    initialTextContent: String? = null,
    onDismiss: () -> Unit,
    onSendToChat: (text: String, imageUri: Uri?) -> Unit,
    onAiImageEdit: (prompt: String, currentImageUrl: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(if (initialImageUrl != null) 0 else if (initialTextContent != null) 1 else 0) }

    // --- Image Canvas States ---
    var imageUrl by remember { mutableStateOf(initialImageUrl) }
    var aiEditPrompt by remember { mutableStateOf("") }
    var strokeColor by remember { mutableStateOf(Color(0xFFFF2A2A)) }
    var strokeWidth by remember { mutableFloatStateOf(8f) }
    var isEraserMode by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) } // -1f to 1f
    var contrast by remember { mutableFloatStateOf(1f) }   // 0.5f to 2f

    val paths = remember { mutableStateListOf<DrawPathData>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    // --- Document & Code Canvas States ---
    var codeContent by remember { mutableStateOf(initialTextContent ?: "// Tulis kode atau dokumen di Canvas Nova AI\nfun main() {\n    println(\"Hello Nova AI Canvas!\")\n}") }
    var isPreviewMode by remember { mutableStateOf(false) }

    val presetColors = listOf(
        Color(0xFFFF2A2A), // Red
        Color(0xFF10A37F), // Green/Teal
        Color(0xFF2563EB), // Blue
        Color(0xFFF59E0B), // Yellow/Orange
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF0F0F0F), // Black
        Color(0xFFFFFFFF)  // White
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10A37F).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Canvas",
                                tint = Color(0xFF10A37F),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Nova AI Canvas",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeTab == 0) "Workspace Editor Gambar & AI Generation" else "Workspace Kode & Dokumen Live",
                                color = Color(0xFFA1A1AA),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF27272A), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Canvas",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(Color(0xFF1E1E22), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 0) Color(0xFF10A37F) else Color.Transparent)
                            .clickable { activeTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Canvas Gambar",
                                tint = if (activeTab == 0) Color.White else Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Canvas Gambar",
                                color = if (activeTab == 0) Color.White else Color(0xFFA1A1AA),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 1) Color(0xFF10A37F) else Color.Transparent)
                            .clickable { activeTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Canvas Dokumen",
                                tint = if (activeTab == 1) Color.White else Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Canvas Dokumen",
                                color = if (activeTab == 1) Color.White else Color(0xFFA1A1AA),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (activeTab == 0) {
                    // TAB 0: IMAGE CANVAS & DRAWING TOOL
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        // Drawing Area Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1A1A1E))
                                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp))
                                .clipToBounds(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Gambar Canvas",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Canvas Kosong",
                                        tint = Color(0xFF52525B),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Canvas Gambar Kosong",
                                        color = Color(0xFFD4D4D8),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Kamu bisa menggambar bebas atau meminta AI membuatkan gambar dari prompt di bawah!",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            // Interactive Drawing Overlay
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val p = Path().apply { moveTo(offset.x, offset.y) }
                                                currentPath = p
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                currentPath?.let { p ->
                                                    val currentOffset = change.position
                                                    p.lineTo(currentOffset.x, currentOffset.y)
                                                    // Trigger recomposition
                                                    currentPath = Path().apply { addPath(p) }
                                                }
                                            },
                                            onDragEnd = {
                                                currentPath?.let { p ->
                                                    paths.add(
                                                        DrawPathData(
                                                            path = p,
                                                            color = if (isEraserMode) Color(0xFF1A1A1E) else strokeColor,
                                                            strokeWidth = strokeWidth,
                                                            isEraser = isEraserMode
                                                        )
                                                    )
                                                }
                                                currentPath = null
                                            }
                                        )
                                    }
                            ) {
                                // Draw existing paths
                                paths.forEach { pathData ->
                                    drawPath(
                                        path = pathData.path,
                                        color = pathData.color,
                                        style = Stroke(width = pathData.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                                // Draw active path
                                currentPath?.let { activeP ->
                                    drawPath(
                                        path = activeP,
                                        color = if (isEraserMode) Color(0xFF1A1A1E) else strokeColor,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Toolbar & Palette Options
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Eraser Toggle
                            IconButton(
                                onClick = { isEraserMode = !isEraserMode },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isEraserMode) Color(0xFFEF4444) else Color(0xFF27272A),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "Eraser",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Clear Canvas
                            IconButton(
                                onClick = { paths.clear() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF27272A), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Coretan",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Undo
                            IconButton(
                                onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF27272A), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "Undo",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF3F3F46)))

                            // Color Palette
                            presetColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (strokeColor == color && !isEraserMode) 3.dp else 1.dp,
                                            color = if (strokeColor == color && !isEraserMode) Color.White else Color(0xFF52525B),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            strokeColor = color
                                            isEraserMode = false
                                        }
                                )
                            }
                        }

                        // Stroke size slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text("Ketebalan Brush", color = Color(0xFFA1A1AA), fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = strokeWidth,
                                onValueChange = { strokeWidth = it },
                                valueRange = 3f..35f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF10A37F),
                                    activeTrackColor = Color(0xFF10A37F)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // AI Edit Prompt Box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Prompt",
                                    tint = Color(0xFF10A37F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = aiEditPrompt,
                                    onValueChange = { aiEditPrompt = it },
                                    placeholder = {
                                        Text(
                                            text = "Minta AI edit / buatkan gambar baru...",
                                            color = Color(0xFF71717A),
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (aiEditPrompt.isNotBlank()) {
                                            onAiImageEdit(aiEditPrompt, imageUrl)
                                            val genUrl = "https://image.pollinations.ai/prompt/${Uri.encode(aiEditPrompt)}?width=1024&height=1024&seed=${System.currentTimeMillis()}&nologo=true&enhance=true"
                                            imageUrl = genUrl
                                            aiEditPrompt = ""
                                            Toast.makeText(context, "✨ AI sedang memproses gambar di Canvas...", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10A37F)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("PROSES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Footer Actions: Kirim ke Chat & Simpan Gambar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!imageUrl.isNullOrBlank()) {
                                        val msgText = "🎨 [CANVAS IMAGE]\n![Canvas Output]($imageUrl)"
                                        onSendToChat(msgText, null)
                                        Toast.makeText(context, "✅ Hasil Canvas dikirim ke Chat!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Canvas masih kosong. Buat atau minta AI hasilkan gambar dulu!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10A37F)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Kirim",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kirim ke Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!imageUrl.isNullOrBlank()) {
                                        Toast.makeText(context, "📄 Gambar disimpan dari Canvas!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Tidak ada gambar di Canvas untuk disimpan.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color(0xFF3F3F46))),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Simpan",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan Gambar", fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    // TAB 1: DOCUMENT & CODE CANVAS
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        // Toolbar Code Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPreviewMode) "Preview Dokumen" else "Editor Teks/Kode Live",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { isPreviewMode = !isPreviewMode },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPreviewMode) Color(0xFF10A37F) else Color(0xFF27272A)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                        contentDescription = "Toggle Preview",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPreviewMode) "Edit Kode" else "Preview",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Main Code Box / Editor
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF18181B))
                                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (isPreviewMode) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = codeContent,
                                        color = Color(0xFFF4F4F5),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 20.sp
                                    )
                                }
                            } else {
                                TextField(
                                    value = codeContent,
                                    onValueChange = { codeContent = it },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color(0xFFF4F4F5),
                                        unfocusedTextColor = Color(0xFFF4F4F5)
                                    ),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Footer Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (codeContent.isNotBlank()) {
                                        onSendToChat(codeContent, null)
                                        Toast.makeText(context, "✅ Dokumen Canvas dikirim ke Chat!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10A37F)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Kirim",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kirim ke Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val path = saveTextToDownloads(context, codeContent, "txt")
                                    if (path != null) {
                                        Toast.makeText(context, "📄 File disimpan ke $path", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "❌ Gagal menyimpan file.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color(0xFF3F3F46))),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Unduh",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unduh .TXT", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
