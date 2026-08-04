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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatPersona
import com.example.data.model.WormMode

@Composable
fun HeaderBar(
    currentMode: WormMode,
    currentPersona: ChatPersona = ChatPersona.ALL_PERSONAS[0],
    isLiveAudioMode: Boolean = false,
    onToggleLiveAudioMode: () -> Unit = {},
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
            Text(
                text = "WormGPT",
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

        // Right Action Buttons (Live Audio Mic & Settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Live Audio / Speech Mode Toggle Button
            IconButton(
                onClick = onToggleLiveAudioMode,
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (isLiveAudioMode) Color(0xFF10A37F).copy(alpha = 0.15f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isLiveAudioMode) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = if (isLiveAudioMode) "Mode Suara AI Aktif" else "Mode Suara AI Nonaktif",
                    tint = if (isLiveAudioMode) Color(0xFF10A37F) else Color(0xFF6E6E80),
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
