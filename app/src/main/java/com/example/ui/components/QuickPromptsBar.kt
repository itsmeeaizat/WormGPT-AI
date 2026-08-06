package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickPromptItem(
    val label: String,
    val promptText: String,
    val icon: ImageVector
)

@Composable
fun QuickPromptsBar(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        QuickPromptItem(
            label = "🌐 Riset Web Mendalam",
            promptText = "Tolong cari dan analisis informasi terbaru dan mendalam dari internet tentang ",
            icon = Icons.Default.Language
        ),
        QuickPromptItem(
            label = "📄 Buat CV & Lamaran",
            promptText = "Tolong buatkan draf CV profesional dan Surat Lamaran Kerja siap unduh untuk posisi ",
            icon = Icons.Default.Description
        ),
        QuickPromptItem(
            label = "💻 Buat Kode JS / Python",
            promptText = "Buatkan skrip kode JavaScript & HTML/Python lengkap beserta opsi file unduhan untuk ",
            icon = Icons.Default.Code
        ),
        QuickPromptItem(
            label = "📝 Bantu Tugas Sekolah/Kuliah",
            promptText = "Bantu saya menyelesaikan tugas ini secara lengkap, terstruktur, dan siap dijadikan dokumen PDF: ",
            icon = Icons.Default.Assignment
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF4F4F5))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE4E4E7),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelectPrompt(item.promptText) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color(0xFF10A37F),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        color = Color(0xFF27272A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
