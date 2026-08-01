package com.example.ui.components

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptRedAccent

import androidx.compose.material.icons.filled.Security

data class AttachmentOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentMenuSheet(
    onSelectFile: () -> Unit,
    onSelectImage: () -> Unit,
    onOpenCamera: () -> Unit,
    onScanLocation: () -> Unit,
    onManagePermissions: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    val options = listOf(
        AttachmentOption(
            title = "Upload File / Code Document",
            subtitle = "Unlimited file size: .apk, .zip, .rar, .js, .py, .kt, .json, etc.",
            icon = Icons.Default.InsertDriveFile,
            onClick = {
                onSelectFile()
                onDismiss()
            }
        ),
        AttachmentOption(
            title = "Upload Photo / Image Gallery",
            subtitle = "Screenshots, architecture diagrams, visual bug logs",
            icon = Icons.Default.Image,
            onClick = {
                onSelectImage()
                onDismiss()
            }
        ),
        AttachmentOption(
            title = "Open Camera & Capture Photo",
            subtitle = "Take live photo of screen, code, or hardware setup",
            icon = Icons.Default.CameraAlt,
            onClick = {
                onOpenCamera()
                onDismiss()
            }
        ),
        AttachmentOption(
            title = "Scan GPS Location Telemetry",
            subtitle = "Capture device geolocation coordinates for security audit",
            icon = Icons.Default.LocationOn,
            onClick = {
                onScanLocation()
                onDismiss()
            }
        ),
        AttachmentOption(
            title = "Manage Access Permissions",
            subtitle = "Check and grant Camera, Storage Reading, and GPS Location permissions",
            icon = Icons.Default.Security,
            onClick = {
                onManagePermissions()
                onDismiss()
            }
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF18181B),
        scrimColor = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = WormGptRedAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "WormGPT Attachment Hub",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Attach payloads, files, media, or telemetry to active prompt",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                options.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF09090B))
                            .border(
                                width = 0.5.dp,
                                color = WormGptBorderRed.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { item.onClick() }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF27272A), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = WormGptRedAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.subtitle,
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
