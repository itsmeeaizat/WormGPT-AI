package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatPersona
import com.example.data.model.NovaMode

@Composable
fun HeaderBar(
    currentMode: NovaMode,
    currentPersona: ChatPersona = ChatPersona.ALL_PERSONAS[0],
    isTtsEnabled: Boolean = false,
    onToggleTts: () -> Unit = {},
    isLiveAudioMode: Boolean = false,
    onToggleLiveAudioMode: () -> Unit = {},
    onOpenCanvas: () -> Unit = {},
    onOpenModeSelector: () -> Unit,
    onOpenPersonaSelector: () -> Unit = {},
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Hamburger Menu Icon to open chat history
        IconButton(
            onClick = onOpenHistory,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Riwayat Chat",
                tint = Color(0xFF2D2D2D),
                modifier = Modifier.size(24.dp)
            )
        }

        // Center: Clickable Model/Persona Title Selector with Dropdown Arrow (ChatGPT style)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenPersonaSelector() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nova_ai_logo),
                contentDescription = "Nova AI Logo",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Nova AI",
                color = Color(0xFF0F0F0F),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE6F4F1))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = currentPersona.tag,
                    color = Color(0xFF10A37F),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Pilih Persona",
                tint = Color(0xFF6E6E80),
                modifier = Modifier.size(18.dp)
            )
        }

        // Right Action Buttons (Canvas, Live Voice, Message Audio TTS & Settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // AI Canvas Workspace Button
            IconButton(
                onClick = onOpenCanvas,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFE6F4F1), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Canvas Workspace Nova AI",
                    tint = Color(0xFF10A37F),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Live Voice Agent Mode Button (Gemini Live Overlay)
            IconButton(
                onClick = onToggleLiveAudioMode,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isLiveAudioMode) Color(0xFFE6F4EA) else Color(0xFFF4F4F5),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Mode Suara Live Gemini",
                    tint = if (isLiveAudioMode) Color(0xFF10A37F) else Color(0xFF6E6E80),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Read AI Messages Voice TTS Toggle Button
            IconButton(
                onClick = onToggleTts,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isTtsEnabled) Color(0xFFE6F4EA) else Color(0xFFF4F4F5),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = if (isTtsEnabled) "Pembacaan Suara Pesan Aktif" else "Pembacaan Suara Pesan Nonaktif",
                    tint = if (isTtsEnabled) Color(0xFF10A37F) else Color(0xFF6E6E80),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings Icon
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    tint = Color(0xFF6E6E80),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
