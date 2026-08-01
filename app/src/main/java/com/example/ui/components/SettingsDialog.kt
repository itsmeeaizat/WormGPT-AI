package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptRedAccent

@Composable
fun SettingsDialog(
    customApiKey: String,
    groqApiKey: String = "",
    openRouterApiKey: String = "",
    mistralApiKey: String = "",
    selectedModel: String,
    onSaveApiKey: (String) -> Unit,
    onSaveGroqApiKey: (String) -> Unit = {},
    onSaveOpenRouterApiKey: (String) -> Unit = {},
    onSaveMistralApiKey: (String) -> Unit = {},
    onSaveModel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var geminiKeyText by remember { mutableStateOf(customApiKey) }
    var groqKeyText by remember { mutableStateOf(groqApiKey) }
    var openRouterKeyText by remember { mutableStateOf(openRouterApiKey) }
    var mistralKeyText by remember { mutableStateOf(mistralApiKey) }
    var currentSelectedModel by remember { mutableStateOf(selectedModel) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF18181B))
                .border(1.dp, WormGptBorderRed, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = WormGptRedAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "WormGPT Multi-AI System Config",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Default secret status
                val isSecretInjected = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF09090B))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isSecretInjected || geminiKeyText.isNotBlank()) Color(0xFF10B981) else Color(0xFFE11D48),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSecretInjected || geminiKeyText.isNotBlank()) "Global Gemini Key Active" else "Global Gemini Key Missing (Optional if using Free/Other AI)",
                        color = if (isSecretInjected || geminiKeyText.isNotBlank()) Color(0xFF10B981) else Color(0xFFF43F5E),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Model Selection
                Text(
                    text = "TARGET MODEL ENGINE",
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF09090B))
                        .padding(8.dp)
                ) {
                    ModelRadioOption(
                        selected = currentSelectedModel == "gemini-3.5-flash",
                        title = "gemini-3.5-flash",
                        subtitle = "Google Fast AI (Global/Custom Key)",
                        onClick = { currentSelectedModel = "gemini-3.5-flash" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "gemini-3.1-pro-preview",
                        title = "gemini-3.1-pro-preview",
                        subtitle = "Google Deep Reasoning AI",
                        onClick = { currentSelectedModel = "gemini-3.1-pro-preview" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "groq/llama-3.3-70b-versatile",
                        title = "groq/llama-3.3-70b-versatile",
                        subtitle = "Groq Llama 3.3 (Requires Groq API Key)",
                        onClick = { currentSelectedModel = "groq/llama-3.3-70b-versatile" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "openrouter/anthropic/claude-3.5-sonnet",
                        title = "openrouter/claude-3.5-sonnet",
                        subtitle = "OpenRouter Claude 3.5 (Requires OpenRouter Key)",
                        onClick = { currentSelectedModel = "openrouter/anthropic/claude-3.5-sonnet" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "mistral/mistral-large-latest",
                        title = "mistral/mistral-large-latest",
                        subtitle = "Mistral Large (Requires Mistral Key)",
                        onClick = { currentSelectedModel = "mistral/mistral-large-latest" }
                    )
                    ModelRadioOption(
                        selected = currentSelectedModel == "pollinations/openai",
                        title = "pollinations/openai (Free Global)",
                        subtitle = "Free fallback LLM engine (No API Key needed)",
                        onClick = { currentSelectedModel = "pollinations/openai" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Key Inputs Section
                Text(
                    text = "API KEYS CONFIGURATION",
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyInputField(
                    label = "CUSTOM GEMINI API KEY",
                    placeholder = "AIzaSy...",
                    value = geminiKeyText,
                    onValueChange = { geminiKeyText = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyInputField(
                    label = "GROQ API KEY (gsk_...)",
                    placeholder = "gsk_...",
                    value = groqKeyText,
                    onValueChange = { groqKeyText = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyInputField(
                    label = "OPENROUTER API KEY (sk-or-...)",
                    placeholder = "sk-or-...",
                    value = openRouterKeyText,
                    onValueChange = { openRouterKeyText = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyInputField(
                    label = "MISTRAL API KEY",
                    placeholder = "Mistral API key...",
                    value = mistralKeyText,
                    onValueChange = { mistralKeyText = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", color = Color(0xFFA1A1AA))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveApiKey(geminiKeyText)
                            onSaveGroqApiKey(groqKeyText)
                            onSaveOpenRouterApiKey(openRouterKeyText)
                            onSaveMistralApiKey(mistralKeyText)
                            onSaveModel(currentSelectedModel)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WormGptRedAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Save Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Watermark & Feedback
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF09090B))
                        .border(0.5.dp, Color(0xFF27272A), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aizat",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "tiktok: itsmee_aizat | github: itsmeeaizat",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:aizatalamudinindonesia.plus@gmail.com")
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "WormGPT AI Feedback & Support")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Gagal membuka email: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Email,
                            contentDescription = null,
                            tint = WormGptRedAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Feedback / Email",
                            color = Color.White,
                            fontSize = 12.sp,
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
fun ModelRadioOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = WormGptRedAccent)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = Color(0xFF71717A),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ApiKeyInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            color = Color(0xFFA1A1AA),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF09090B))
                .border(0.5.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(WormGptRedAccent),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF52525B),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
