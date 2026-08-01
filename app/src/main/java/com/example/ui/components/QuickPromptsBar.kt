package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WormGptBorderRed
import com.example.ui.theme.WormGptRedAccent

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
            label = "Scan Repo MrZXN777/WormGPT-",
            promptText = "Identify vulnerabilities in the repository structure for MrZXN777/WormGPT- and highlight security risk factors.",
            icon = Icons.Default.Code
        ),
        QuickPromptItem(
            label = "OWASP Top 10 Check",
            promptText = "Provide an OWASP Top 10 vulnerability inspection protocol for web and API backends.",
            icon = Icons.Default.BugReport
        ),
        QuickPromptItem(
            label = "Generate YARA Rule",
            promptText = "Write a production YARA rule to detect obfuscated web shell scripts and suspicious memory injections.",
            icon = Icons.Default.Shield
        ),
        QuickPromptItem(
            label = "Reverse Shell Syntax",
            promptText = "Give clean terminal syntax examples for Bash, Python, and PowerShell reverse shells for authorized lab penetration testing.",
            icon = Icons.Default.Terminal
        ),
        QuickPromptItem(
            label = "SQL Injection Audit",
            promptText = "Explain blind SQL injection detection techniques and show how to write parameterized query remediations.",
            icon = Icons.Default.BugReport
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF18181B))
                    .border(
                        width = 0.5.dp,
                        color = WormGptBorderRed.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectPrompt(item.promptText) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = WormGptRedAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item.label,
                        color = Color(0xFFD4D4D8),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
