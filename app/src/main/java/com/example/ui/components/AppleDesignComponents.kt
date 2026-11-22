package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalReducedMotionEnabled

/**
 * Apple Fluid Interfaces (§1 Response & §4 Springs):
 * Instant tactile scale feedback on pointer-DOWN (not release) using a critically damped spring.
 */
fun Modifier.applePressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f
): Modifier = composed {
    val reducedMotion = LocalReducedMotionEnabled.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 650f
        ),
        label = "applePressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Apple HIG Inset Grouped Section Header (uppercase subtle 12sp label with generous top breathing room).
 */
@Composable
fun AppleSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trailing?.invoke()
    }
}

/**
 * Apple HIG Inset Grouped Container:
 * Elevated surface (#1C1C1E dark / #FFFFFF light) with 16dp continuous corners and hairline border.
 */
@Composable
fun AppleInsetGroupedCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Apple HIG Inset Divider (0.5dp hairline indented to align with row text).
 */
@Composable
fun AppleInsetDivider(
    startIndent: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    HorizontalDivider(
        modifier = modifier.padding(start = startIndent),
        thickness = 0.5.dp,
        color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    )
}

/**
 * Apple HIG Settings/Books style rounded-square colored icon tile (32dp, 8dp radius).
 */
@Composable
fun AppleIconBadge(
    icon: ImageVector,
    tintColor: Color,
    size: Dp = 32.dp,
    iconSize: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(tintColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Apple HIG Segmented Control (iOS UISegmentedControl style) replacing heavy underlined tab rows.
 */
@Composable
fun AppleSegmentedControl(
    items: List<Pair<String, String>>, // Pair(label, testTag)
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val trackColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)
    val selectedThumbColor = if (isDark) Color(0xFF3A3A3C) else Color.White

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = trackColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (label, tag) ->
                val isSelected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .applePressScale(interactionSource, 0.97f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(index) }
                        )
                        .testTag(tag),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) selectedThumbColor else Color.Transparent,
                    shadowElevation = if (isSelected && !isDark) 1.dp else 0.dp,
                    border = if (isSelected && isDark) BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
