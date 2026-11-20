package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DayStreakStatus
import com.example.ui.DictionaryViewModel
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusMotion

@Composable
fun DailyStreakAndHeatmapBanner(
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val longestStreak by viewModel.longestStreak.collectAsStateWithLifecycle()
    val heatmap60Days by viewModel.activityHeatmap60Days.collectAsStateWithLifecycle()
    val todayActions by viewModel.todayHabitActionsCount.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyGoalTarget.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotionEnabled.current

    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    var selectedDay by remember { mutableStateOf<DayStreakStatus?>(null) }

    val progress = if (dailyGoal > 0) (todayActions.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) else 0f
    val goalReached = todayActions >= dailyGoal

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_streak_heatmap_banner"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Streak callout + Best streak + Expand toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (currentStreak > 0) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (currentStreak > 0) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$currentStreak day streak",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = "Best: ${longestStreak}d",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("toggle_heatmap_button")
                ) {
                    Text(
                        text = if (isExpanded) "Hide map" else "60-day map",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse heatmap" else "Expand heatmap",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Daily Goal Progress
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (goalReached) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (goalReached) "Daily habit goal complete" else "Today's habit goal",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (goalReached) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Text(
                        text = "$todayActions / $dailyGoal actions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    color = if (goalReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            // Expandable 60-Day Contribution Heatmap
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(RedeusMotion.spec(reducedMotion)) + expandVertically(RedeusMotion.spec(reducedMotion)),
                exit = fadeOut(RedeusMotion.spec(reducedMotion)) + shrinkVertically(RedeusMotion.spec(reducedMotion))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "60-day activity",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val activeDaysCount = heatmap60Days.count { it.count > 0 }
                        Text(
                            text = "$activeDaysCount / 60 active days",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val daysList = remember(heatmap60Days) {
                        if (heatmap60Days.size >= 60) {
                            heatmap60Days
                        } else {
                            val today = java.time.LocalDate.now()
                            (59 downTo 0).map { offset ->
                                val d = today.minusDays(offset.toLong())
                                DayStreakStatus(
                                    epochDay = d.toEpochDay(),
                                    dateLabel = "${d.monthValue}/${d.dayOfMonth}",
                                    hasReview = false,
                                    count = 0,
                                    isToday = offset == 0
                                )
                            }
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            daysList.chunked(10).forEach { rowDays ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    rowDays.forEach { day ->
                                        val (cellBg, cellFg) = when {
                                            day.count == 0 -> MaterialTheme.colorScheme.surfaceContainerLowest to MaterialTheme.colorScheme.onSurfaceVariant
                                            day.count in 1..3 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                                            day.count in 4..8 -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
                                        }
                                        val isSelected = selectedDay?.epochDay == day.epochDay

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(MaterialTheme.shapes.extraSmall)
                                                .background(cellBg)
                                                .then(
                                                    if (isSelected || day.isToday) {
                                                        Modifier.border(
                                                            width = if (isSelected) 2.dp else 1.5.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                                            shape = MaterialTheme.shapes.extraSmall
                                                        )
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clickable { selectedDay = day }
                                                .semantics {
                                                    contentDescription = "${day.dateLabel}: ${day.count} learning actions"
                                                }
                                                .testTag("heatmap_cell_${day.dateLabel}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.dateLabel.substringAfter('/'),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cellFg
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val activeDayInfo = selectedDay ?: heatmap60Days.lastOrNull()
                    if (activeDayInfo != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (activeDayInfo.isToday) "Today (${activeDayInfo.dateLabel})" else "Date: ${activeDayInfo.dateLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${activeDayInfo.count} learning actions",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (activeDayInfo.count > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intensity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeatmapLegendItem(color = MaterialTheme.colorScheme.surfaceContainerLowest, label = "0")
                            HeatmapLegendItem(color = MaterialTheme.colorScheme.primaryContainer, label = "1–3")
                            HeatmapLegendItem(color = MaterialTheme.colorScheme.primary, label = "4–8")
                            HeatmapLegendItem(color = MaterialTheme.colorScheme.tertiary, label = "9+")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = MaterialTheme.shapes.extraSmall,
            color = color,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
