package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CanonStatus
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.StatusAssumptionAmber
import com.example.ui.theme.StatusCanonGreen
import com.example.ui.theme.StatusDraftIndigo
import com.example.ui.theme.StatusReviewRed
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose

/**
 * Atmospheric RedWorld Hero Banner rendered via Jetpack Compose Canvas
 * (depicts the Red Mansion silhouette, crimson neural constellation nodes, and status pills).
 */
@Composable
fun RedWorldHeroArtBanner(
    title: String,
    subtitle: String,
    isGeminiReady: Boolean,
    isDriveConnected: Boolean,
    onOpenConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("redworld_hero_banner"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BurgundyCardBorder),
        colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF280710),
                            Color(0xFF140509),
                            Color(0xFF420A19)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
                // Glowing crimson moon / neural core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x55E11D48), Color.Transparent),
                        center = Offset(w * 0.82f, h * 0.35f),
                        radius = h * 0.65f
                    ),
                    center = Offset(w * 0.82f, h * 0.35f),
                    radius = h * 0.65f
                )
                // Neural constellation lines
                val nodes = listOf(
                    Offset(w * 0.68f, h * 0.25f),
                    Offset(w * 0.78f, h * 0.18f),
                    Offset(w * 0.88f, h * 0.32f),
                    Offset(w * 0.74f, h * 0.48f),
                    Offset(w * 0.92f, h * 0.56f)
                )
                for (i in 0 until nodes.lastIndex) {
                    drawLine(
                        color = Color(0x44FB7185),
                        start = nodes[i],
                        end = nodes[i + 1],
                        strokeWidth = 2f
                    )
                }
                nodes.forEach { pt ->
                    drawCircle(color = Color(0xFFFB7185), radius = 4f, center = pt)
                }
                // Stylized Red Mansion gable silhouette on bottom right
                val mansionPath = Path().apply {
                    moveTo(w * 0.60f, h)
                    lineTo(w * 0.60f, h * 0.68f)
                    lineTo(w * 0.66f, h * 0.52f)
                    lineTo(w * 0.72f, h * 0.68f)
                    lineTo(w * 0.78f, h * 0.68f)
                    lineTo(w * 0.84f, h * 0.44f)
                    lineTo(w * 0.90f, h * 0.68f)
                    lineTo(w * 0.96f, h * 0.68f)
                    lineTo(w * 0.96f, h)
                    close()
                }
                drawPath(path = mansionPath, color = Color(0x550D0608))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextIvoryPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedRose,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadgePill(
                        label = if (isGeminiReady) "Gemini API: Активен" else "Gemini API: Настроить ключ",
                        isPositive = isGeminiReady,
                        onClick = onOpenConnections
                    )
                    StatusBadgePill(
                        label = if (isDriveConnected) "Google Drive: Синхр." else "Локальный + SAF",
                        isPositive = true,
                        onClick = onOpenConnections
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadgePill(
    label: String,
    isPositive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        color = if (isPositive) Color(0x3310B981) else Color(0x33F59E0B),
        border = BorderStroke(
            1.dp,
            if (isPositive) StatusCanonGreen.copy(alpha = 0.6f) else StatusAssumptionAmber.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Default.CheckCircle else Icons.Default.Key,
                contentDescription = label,
                tint = if (isPositive) StatusCanonGreen else StatusAssumptionAmber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextIvoryPrimary
            )
        }
    }
}

@Composable
fun CanonStatusBadge(statusName: String, modifier: Modifier = Modifier) {
    val status = CanonStatus.entries.firstOrNull { it.name == statusName } ?: CanonStatus.CANON
    val color = when (status) {
        CanonStatus.CANON -> StatusCanonGreen
        CanonStatus.ASSUMPTION -> StatusAssumptionAmber
        CanonStatus.DRAFT -> StatusDraftIndigo
        CanonStatus.NEEDS_REVIEW -> StatusReviewRed
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Renders Markdown text including headers, bullet lists, and syntax-styled code blocks
 * with a one-tap copy button.
 */
@Composable
fun MarkdownMessageRenderer(
    markdownText: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val segments = splitMarkdownCodeBlocks(markdownText)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        segments.forEach { segment ->
            if (segment.isCodeBlock) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ObsidianBackground,
                    border = BorderStroke(1.dp, BurgundyCardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = segment.language.ifEmpty { "code" }.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGoldSecondary,
                                fontFamily = JetBrainsMonoFont
                            )
                            IconButton(
                                onClick = { copyTextToClipboard(context, segment.content) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Скопировать код",
                                    tint = TextMutedRose,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = segment.content,
                            fontFamily = JetBrainsMonoFont,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            } else {
                val lines = segment.content.lines()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        when {
                            trimmed.startsWith("### ") -> Text(
                                text = trimmed.removePrefix("### ").replace("**", ""),
                                style = MaterialTheme.typography.titleSmall,
                                color = AmberGoldSecondary
                            )
                            trimmed.startsWith("## ") -> Text(
                                text = trimmed.removePrefix("## ").replace("**", ""),
                                style = MaterialTheme.typography.titleMedium,
                                color = CrimsonPrimary
                            )
                            trimmed.startsWith("# ") -> Text(
                                text = trimmed.removePrefix("# ").replace("**", ""),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextIvoryPrimary
                            )
                            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                                Row(modifier = Modifier.padding(start = 4.dp)) {
                                    Text(
                                        text = "• ",
                                        color = CrimsonPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = trimmed.drop(2).replace("**", ""),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextIvoryPrimary
                                    )
                                }
                            }
                            trimmed.isNotEmpty() -> Text(
                                text = trimmed.replace("**", ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextIvoryPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MarkdownSegment(
    val isCodeBlock: Boolean,
    val language: String,
    val content: String
)

private fun splitMarkdownCodeBlocks(raw: String): List<MarkdownSegment> {
    val result = mutableListOf<MarkdownSegment>()
    val regex = Regex("""```([a-zA-Z0-9_-]*)\n?(.*?)```""", RegexOption.DOT_MATCHES_ALL)
    var lastIndex = 0
    for (match in regex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val textPart = raw.substring(lastIndex, match.range.first).trim()
            if (textPart.isNotEmpty()) {
                result.add(MarkdownSegment(false, "", textPart))
            }
        }
        result.add(
            MarkdownSegment(
                isCodeBlock = true,
                language = match.groupValues[1].trim(),
                content = match.groupValues[2].trim()
            )
        )
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        val tail = raw.substring(lastIndex).trim()
        if (tail.isNotEmpty()) {
            result.add(MarkdownSegment(false, "", tail))
        }
    }
    return result.ifEmpty { listOf(MarkdownSegment(false, "", raw)) }
}

fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("RedWorld Studio", text))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectionHeaderRow(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextIvoryPrimary
        )
        if (actionLabel != null && onActionClick != null) {
            AssistChip(
                onClick = onActionClick,
                label = { Text(actionLabel, style = MaterialTheme.typography.labelMedium) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = BurgundySurfaceElevated,
                    labelColor = CrimsonPrimary
                ),
                border = BorderStroke(1.dp, BurgundyCardBorder)
            )
        }
    }
}
