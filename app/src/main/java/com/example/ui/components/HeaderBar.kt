package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WormMode
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptRedAccent
import com.example.ui.theme.WormGptRedDark
import com.example.ui.theme.WormGptSurface

import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff

@Composable
fun HeaderBar(
    currentMode: WormMode,
    ttsEnabled: Boolean,
    onToggleTts: () -> Unit,
    onOpenModeSelector: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenModeSelector() }
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = WormGptRedDark.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = WormGptRedAccent.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "W",
                    color = WormGptRedAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WormGPT",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = WormGptSurface,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = WormGptBorderRed,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentMode.tag,
                            color = WormGptRedAccent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .alpha(alphaAnim)
                            .background(WormGptRedAccent, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentMode.name.uppercase(),
                        color = WormGptRedAccent.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenModeSelector,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(38.dp)
                    .background(Color(0xFF18181B), CircleShape)
                    .border(0.5.dp, WormGptBorderRed.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Switch Mode",
                    tint = WormGptRedAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(38.dp)
                    .background(Color(0xFF18181B), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Chat History",
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenPermissions,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(38.dp)
                    .background(Color(0xFF18181B), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "App Permissions",
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onToggleTts,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(38.dp)
                    .background(Color(0xFF18181B), CircleShape)
            ) {
                Icon(
                    imageVector = if (ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle TTS Voice",
                    tint = if (ttsEnabled) WormGptRedAccent else Color(0xFFA1A1AA),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
