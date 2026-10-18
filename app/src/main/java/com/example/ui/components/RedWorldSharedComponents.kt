package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CanonStatus
import com.example.ui.theme.AmberGoldSecondary
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.BurgundyCardBorder
import com.example.ui.theme.BurgundySurfaceContainer
import com.example.ui.theme.BurgundySurfaceElevated
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.StatusAssumptionAmber
import com.example.ui.theme.StatusCanonGreen
import com.example.ui.theme.StatusDraftIndigo
import com.example.ui.theme.StatusReviewRed
import com.example.ui.theme.TextIvoryPrimary
import com.example.ui.theme.TextMutedRose

/**
 * Unified Theme Selector Card supporting all 4 required themes:
 * Светлая (LIGHT), Тёмная (DARK), Системная (SYSTEM), RedWorld (REDWORLD).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioThemeSelectorCard(
    selectedThemeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_theme_selector_card"),
        colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
        border = BorderStroke(1.dp, BurgundyCardBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Тема оформления",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Визуальный стиль и тема оформления",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextIvoryPrimary
                    )
                    Text(
                        text = "Активна тема: ${selectedThemeMode.titleRu} — ${selectedThemeMode.subtitleRu}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedRose
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = mode == selectedThemeMode
                    val modeIcon = when (mode) {
                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                        AppThemeMode.DARK -> Icons.Default.DarkMode
                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        AppThemeMode.REDWORLD -> Icons.Default.Palette
                    }
                    val previewBg = when (mode) {
                        AppThemeMode.LIGHT -> Color(0xFFF4F6FA)
                        AppThemeMode.DARK -> Color(0xFF111827)
                        AppThemeMode.SYSTEM -> Color(0xFF1E293B)
                        AppThemeMode.REDWORLD -> Color(0xFF211016)
                    }
                    val previewAccent = when (mode) {
                        AppThemeMode.LIGHT -> Color(0xFFBE123C)
                        AppThemeMode.DARK -> Color(0xFF6366F1)
                        AppThemeMode.SYSTEM -> Color(0xFF14B8A6)
                        AppThemeMode.REDWORLD -> Color(0xFFE11D48)
                    }

                    Surface(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectThemeMode(mode) }
                            .testTag("theme_option_${mode.id.lowercase()}"),
                        color = if (isSelected) CrimsonPrimary.copy(alpha = 0.16f) else BurgundySurfaceElevated,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) CrimsonPrimary else BurgundyCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(previewBg)
                                    .border(2.dp, previewAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(previewAccent)
                                )
                            }
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = mode.titleRu,
                                tint = if (isSelected) CrimsonPrimary else TextMutedRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = mode.titleRu,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) CrimsonPrimary else TextIvoryPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact 4-Theme Quick Switcher row for the Navigation Drawer or Header.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioThemeQuickRow(
    selectedThemeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Тема оформления:",
            style = MaterialTheme.typography.labelMedium,
            color = TextMutedRose
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppThemeMode.entries.forEach { mode ->
                val selected = mode == selectedThemeMode
                Surface(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectThemeMode(mode) }
                        .testTag("quick_theme_${mode.id.lowercase()}"),
                    color = if (selected) CrimsonPrimary else BurgundySurfaceElevated,
                    border = BorderStroke(1.dp, if (selected) CrimsonPrimary else BurgundyCardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.titleRu,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else TextIvoryPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified Primary Button with accessible >= 48.dp touch target, optional icon, and loading spinner.
 */
@Composable
fun StudioPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CrimsonPrimary,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Unified Secondary Outlined Button with accessible >= 48.dp touch target.
 */
@Composable
fun StudioSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BurgundyCardBorder),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CrimsonPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = TextIvoryPrimary
        )
    }
}

/**
 * Unified TextField with consistent rounded corners, clear labels, and optional error message.
 */
@Composable
fun StudioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    errorMessage: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = singleLine,
            minLines = minLines,
            isError = !errorMessage.isNullOrBlank(),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelMedium,
                color = StatusReviewRed
            )
        }
    }
}

/**
 * Unified Loading State component.
 */
@Composable
fun StudioLoadingState(
    title: String,
    subtitle: String = "Пожалуйста, подождите...",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_loading_state"),
        colors = CardDefaults.cardColors(containerColor = BurgundySurfaceElevated),
        border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = CrimsonPrimary,
                strokeWidth = 3.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextIvoryPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedRose
                )
            }
        }
    }
}

/**
 * Unified Empty State component for empty lists, search results, or uninitialized sections.
 */
@Composable
fun StudioEmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Inbox,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_empty_state"),
        colors = CardDefaults.cardColors(containerColor = BurgundySurfaceContainer),
        border = BorderStroke(1.dp, BurgundyCardBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = CrimsonPrimary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextIvoryPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedRose,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                StudioPrimaryButton(
                    text = actionLabel,
                    onClick = onActionClick
                )
            }
        }
    }
}

/**
 * Unified Error Banner component with retry and dismiss actions.
 */
@Composable
fun StudioErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = message.isNotBlank()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("studio_error_banner"),
            colors = CardDefaults.cardColors(
                containerColor = StatusReviewRed.copy(alpha = 0.14f)
            ),
            border = BorderStroke(1.dp, StatusReviewRed),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Ошибка",
                    tint = StatusReviewRed,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextIvoryPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (onRetry != null) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Повторить",
                            tint = AmberGoldSecondary
                        )
                    }
                }
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть сообщение об ошибке",
                            tint = TextMutedRose
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified Confirmation Dialog for destructive or important operations.
 */
@Composable
fun StudioConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Подтвердить",
    dismissLabel: String = "Отмена",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BurgundySurfaceContainer,
        titleContentColor = TextIvoryPrimary,
        textContentColor = TextMutedRose,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) StatusReviewRed else CrimsonPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = TextMutedRose)
            }
        }
    )
}

/**
 * Atmospheric Hero Banner rendered via Jetpack Compose Canvas,
 * adapting its colors to Light, Dark, System, and RedWorld themes.
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
    val palette = LocalStudioPalette.current
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
                val gradientColors = if (palette.isDark) {
                    listOf(
                        palette.surfaceContainer,
                        palette.background,
                        palette.primaryContainer
                    )
                } else {
                    listOf(
                        Color(0xFFFFF1F2),
                        Color(0xFFF8FAFC),
                        Color(0xFFFFE4E6)
                    )
                }
                drawRect(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
                // Glowing core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.primary.copy(alpha = 0.28f), Color.Transparent),
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
                        color = palette.primary.copy(alpha = 0.3f),
                        start = nodes[i],
                        end = nodes[i + 1],
                        strokeWidth = 2f
                    )
                }
                nodes.forEach { pt ->
                    drawCircle(color = palette.primary.copy(alpha = 0.75f), radius = 4f, center = pt)
                }
                // Stylized architecture silhouette on bottom right
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
                drawPath(
                    path = mansionPath,
                    color = palette.primary.copy(alpha = if (palette.isDark) 0.18f else 0.10f)
                )
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
        color = if (isPositive) StatusCanonGreen.copy(alpha = 0.16f) else StatusAssumptionAmber.copy(alpha = 0.16f),
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
                            color = TextIvoryPrimary
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
    clipboard?.setPrimaryClip(ClipData.newPlainText("World AI Studio", text))
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
