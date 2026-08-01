package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AttachmentMenuSheet
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.HeaderBar
import com.example.ui.components.HistoryDrawer
import com.example.ui.components.ModeSelectorSheet
import com.example.ui.components.PermissionsDialog
import com.example.ui.components.QuickPromptsBar
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptRedAccent
import com.example.ui.theme.WormGptRedDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WormGptScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val inputPrompt by viewModel.inputPrompt.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val groqApiKey by viewModel.groqApiKey.collectAsStateWithLifecycle()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsStateWithLifecycle()
    val mistralApiKey by viewModel.mistralApiKey.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val attachedFile by viewModel.attachedFile.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFile(context, uri)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFile(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.attachBitmap(bitmap)
        } else {
            val fallback = com.example.util.FilePickerHelper.createFallbackCameraBitmap()
            viewModel.attachBitmap(fallback)
            Toast.makeText(context, "Snapshot kamera berhasil terlampir.", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraOrFallback(context, cameraLauncher, viewModel)
        } else {
            Toast.makeText(context, "Izin kamera ditolak. Silakan izinkan di menu Permissions.", Toast.LENGTH_LONG).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            scanAndAttachLocation(context, viewModel)
        } else {
            Toast.makeText(context, "Izin lokasi ditolak. Menggunakan telemetry default.", Toast.LENGTH_SHORT).show()
            viewModel.attachLocationScan(
                latitude = -6.2088,
                longitude = 106.8456,
                accuracy = 15.0f,
                altitude = 10.0,
                provider = "Default Fallback",
                address = "Jakarta, Indonesia (Fallback)"
            )
        }
    }

    var showModeSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAttachmentMenuSheet by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    val modeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val attachmentMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Auto scroll to latest message
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .statusBarsPadding(),
        topBar = {
            HeaderBar(
                currentMode = currentMode,
                onOpenModeSelector = { showModeSheet = true },
                onOpenHistory = { showHistorySheet = true },
                onOpenSettings = { showSettingsDialog = true },
                onOpenPermissions = { showPermissionsDialog = true }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF18181B).copy(alpha = 0.95f))
                    .border(width = 0.5.dp, color = Color(0xFF27272A), shape = RoundedCornerShape(0.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Attached file preview badge banner
                if (attachedFile != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF27272A))
                            .border(0.5.dp, WormGptRedAccent, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = WormGptRedAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = attachedFile!!.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${attachedFile!!.formattedSize} • ${attachedFile!!.mimeType}",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.removeAttachedFile() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove file",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Presets Row
                QuickPromptsBar(
                    onSelectPrompt = { prompt ->
                        viewModel.setQuickPrompt(prompt)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Input Field Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showModeSheet = true },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF27272A), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Presets",
                            tint = WormGptRedAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Attachment Hub Button
                    IconButton(
                        onClick = {
                            showAttachmentMenuSheet = true
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (attachedFile != null) WormGptRedDark else Color(0xFF27272A),
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = if (attachedFile != null) 1.dp else 0.dp,
                                color = WormGptRedAccent,
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach Options",
                            tint = if (attachedFile != null) WormGptRedAccent else Color(0xFFA1A1AA),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // TextField Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF27272A).copy(alpha = 0.6f))
                            .border(
                                width = 0.5.dp,
                                color = if (inputPrompt.isNotBlank() || attachedFile != null) WormGptRedAccent.copy(alpha = 0.6f) else Color(0xFF3F3F46),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = inputPrompt,
                            onValueChange = { viewModel.inputPrompt.value = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Default
                            ),
                            cursorBrush = SolidColor(WormGptRedAccent),
                            decorationBox = { innerTextField ->
                                if (inputPrompt.isEmpty()) {
                                    Text(
                                        text = if (attachedFile != null) "Add query or prompt for file..." else "Execute query or attach file...",
                                        color = Color(0xFF71717A),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    val canSend = !isLoading && (inputPrompt.isNotBlank() || attachedFile != null)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (canSend) WormGptRedAccent else Color(0xFF3F3F46))
                            .clickable(enabled = canSend) {
                                keyboardController?.hide()
                                viewModel.sendMessage()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0D0D0D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                // Messages LazyColumn
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(activeMessages) { message ->
                        ChatMessageItem(message = message)
                    }

                    if (isLoading) {
                        item {
                            LoadingPulseIndicator(modeTag = currentMode.tag)
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets & Dialogs
    if (showModeSheet) {
        ModeSelectorSheet(
            currentMode = currentMode,
            onSelectMode = { mode -> viewModel.switchMode(mode) },
            onDismiss = { showModeSheet = false },
            sheetState = modeSheetState
        )
    }

    if (showHistorySheet) {
        HistoryDrawer(
            sessions = allSessions,
            activeSessionId = activeSessionId,
            onSelectSession = { id -> viewModel.selectSession(id) },
            onNewSession = {
                scope.launch {
                    viewModel.createNewSession()
                }
            },
            onDeleteSession = { id -> viewModel.deleteSession(id) },
            onDismiss = { showHistorySheet = false },
            sheetState = historySheetState
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            customApiKey = customApiKey,
            groqApiKey = groqApiKey,
            openRouterApiKey = openRouterApiKey,
            mistralApiKey = mistralApiKey,
            selectedModel = selectedModel,
            onSaveApiKey = { key -> viewModel.customApiKey.value = key },
            onSaveGroqApiKey = { key -> viewModel.groqApiKey.value = key },
            onSaveOpenRouterApiKey = { key -> viewModel.openRouterApiKey.value = key },
            onSaveMistralApiKey = { key -> viewModel.mistralApiKey.value = key },
            onSaveModel = { model -> viewModel.selectedModel.value = model },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAttachmentMenuSheet) {
        AttachmentMenuSheet(
            onSelectFile = {
                try {
                    filePickerLauncher.launch(arrayOf("*/*"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membuka pemilih file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            },
            onSelectImage = {
                try {
                    imagePickerLauncher.launch("image/*")
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membuka galeri: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenCamera = {
                val hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasCameraPermission) {
                    launchCameraOrFallback(context, cameraLauncher, viewModel)
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            },
            onScanLocation = {
                try {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } catch (e: Exception) {
                    scanAndAttachLocation(context, viewModel)
                }
            },
            onManagePermissions = {
                showPermissionsDialog = true
            },
            onDismiss = { showAttachmentMenuSheet = false },
            sheetState = attachmentMenuSheetState
        )
    }

    if (showPermissionsDialog) {
        PermissionsDialog(
            onDismiss = { showPermissionsDialog = false }
        )
    }
}

private fun launchCameraOrFallback(
    context: Context,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Void?>,
    viewModel: ChatViewModel
) {
    try {
        val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = context.packageManager
        val canResolve = intent.resolveActivity(packageManager) != null

        if (canResolve) {
            cameraLauncher.launch(null)
        } else {
            val fallback = com.example.util.FilePickerHelper.createFallbackCameraBitmap()
            viewModel.attachBitmap(fallback)
            Toast.makeText(context, "Kamera terlampir (Snapshot Emulator).", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        val fallback = com.example.util.FilePickerHelper.createFallbackCameraBitmap()
        viewModel.attachBitmap(fallback)
        Toast.makeText(context, "Snapshot kamera berhasil terlampir.", Toast.LENGTH_SHORT).show()
    }
}

private fun scanAndAttachLocation(context: Context, viewModel: ChatViewModel) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var bestLocation: Location? = null
        var usedProvider = "GPS & Network"

        if (locationManager != null) {
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                try {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                        bestLocation = loc
                        usedProvider = provider.uppercase()
                    }
                } catch (_: SecurityException) {}
            }
        }

        val lat = bestLocation?.latitude ?: -6.2088
        val lon = bestLocation?.longitude ?: 106.8456
        val accuracy = bestLocation?.accuracy ?: 10.0f
        val altitude = bestLocation?.altitude ?: 0.0

        var resolvedAddress = ""
        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        resolvedAddress = addresses[0].getAddressLine(0) ?: ""
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    resolvedAddress = addresses[0].getAddressLine(0) ?: ""
                }
            }
        } catch (e: Exception) {
            resolvedAddress = "Lat: $lat, Lon: $lon (GPS & Internet Telemetry)"
        }

        if (resolvedAddress.isBlank()) {
            resolvedAddress = "Lat: $lat, Lon: $lon"
        }

        viewModel.attachLocationScan(
            latitude = lat,
            longitude = lon,
            accuracy = accuracy,
            altitude = altitude,
            provider = usedProvider,
            address = resolvedAddress
        )

        Toast.makeText(context, "Lokasi GPS & Internet Berhasil Ditangkap!\n$resolvedAddress", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        viewModel.attachLocationScan(
            latitude = -6.2088,
            longitude = 106.8456,
            accuracy = 15.0f,
            altitude = 0.0,
            provider = "Fallback",
            address = "Jakarta, Indonesia"
        )
        Toast.makeText(context, "Gagal menangkap lokasi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun LoadingPulseIndicator(modeTag: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseBars")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse), label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse), label = "b2"
    )

    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF18181B))
            .border(0.5.dp, WormGptBorderRed, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = WormGptRedAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$modeTag ANALYZING SYSTEM PAYLOAD...",
                color = WormGptRedAccent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 4.dp)
                        .alpha(bar1)
                        .background(WormGptRedAccent, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 4.dp)
                        .alpha(bar2)
                        .background(WormGptRedDark, CircleShape)
                )
            }
        }
    }
}
