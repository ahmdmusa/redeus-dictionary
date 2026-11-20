package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.WordEntity
import com.example.ui.DetailedSrsStats
import com.example.ui.EaseFactorDistribution
import com.example.ui.ForecastDayStat
import com.example.ui.SrsRatingDistribution
import com.example.ui.components.LeechBadge
import com.example.ui.theme.RedeusExpressiveType

@Composable
fun DetailedRetentionRateCard(stats: DetailedSrsStats) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("retention_rate_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Retention rate",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Last 30 days",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${stats.totalReviewsLast30Days} reviews",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", stats.last30DaysRetentionRate)}%",
                        style = RedeusExpressiveType.metricDisplay
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSubTile(
                    label = "Last 30 days",
                    value = "${String.format(java.util.Locale.US, "%.1f", stats.last30DaysRetentionRate)}%",
                    subtitle = "${stats.totalReviewsLast30Days} reviews",
                    modifier = Modifier.weight(1f)
                )
                MetricSubTile(
                    label = "All-time",
                    value = "${String.format(java.util.Locale.US, "%.1f", stats.overallRetentionRate)}%",
                    subtitle = "${stats.totalReviewsAllTime} reviews",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Percentage of flashcard reviews recalled successfully (Hard, Good, or Easy).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricSubTile(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReviewsPerDayChartCard(stats: DetailedSrsStats) {
    val dailyList = stats.reviewsPerDayLast30Days
    val maxCount = dailyList.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reviews_per_day_chart_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reviews per day (30 days)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Peak: $maxCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Total 30-day reviews: ${stats.totalReviewsLast30Days}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyList.forEachIndexed { index, item ->
                        val isToday = index == dailyList.lastIndex
                        val barHeight = ((item.count.toFloat() / maxCount) * 56f).dp
                            .coerceAtLeast(if (item.count > 0) 6.dp else 2.dp)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.height(92.dp)
                        ) {
                            if (item.count > 0) {
                                Text(
                                    text = "${item.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        when {
                                            isToday -> MaterialTheme.colorScheme.tertiary
                                            item.count > 0 -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDistributionCard(dist: SrsRatingDistribution) {
    val total = dist.totalCount.coerceAtLeast(1)
    val againRatio = dist.againCount.toFloat() / total
    val hardRatio = dist.hardCount.toFloat() / total
    val goodRatio = dist.goodCount.toFloat() / total
    val easyRatio = dist.easyCount.toFloat() / total

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rating_distribution_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Rating breakdown (all-time)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (dist.totalCount > 0) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (againRatio > 0) Box(modifier = Modifier.fillMaxHeight().weight(againRatio).background(MaterialTheme.colorScheme.error))
                        if (hardRatio > 0) Box(modifier = Modifier.fillMaxHeight().weight(hardRatio).background(MaterialTheme.colorScheme.tertiary))
                        if (goodRatio > 0) Box(modifier = Modifier.fillMaxHeight().weight(goodRatio).background(MaterialTheme.colorScheme.primary))
                        if (easyRatio > 0) Box(modifier = Modifier.fillMaxHeight().weight(easyRatio).background(MaterialTheme.colorScheme.secondary))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingStatChip("Again", dist.againCount, dist.againPct, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    RatingStatChip("Hard", dist.hardCount, dist.hardPct, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingStatChip("Good", dist.goodCount, dist.goodPct, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    RatingStatChip("Easy", dist.easyCount, dist.easyPct, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RatingStatChip(
    label: String,
    count: Int,
    pct: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(color)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$count (${String.format(java.util.Locale.US, "%.1f", pct)}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EaseFactorDistributionCard(dist: EaseFactorDistribution) {
    val total = dist.totalWords.coerceAtLeast(1)
    val buckets = listOf(
        Triple("< 1.8 (Very hard)", dist.bucketUnder1_8, MaterialTheme.colorScheme.error),
        Triple("1.8 – 2.2 (Hard)", dist.bucket1_8to2_2, MaterialTheme.colorScheme.tertiary),
        Triple("2.2 – 2.6 (Moderate)", dist.bucket2_2to2_6, MaterialTheme.colorScheme.secondary),
        Triple("2.6 – 3.0 (Easy)", dist.bucket2_6to3_0, MaterialTheme.colorScheme.primary),
        Triple("> 3.0 (Very easy)", dist.bucketOver3_0, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ease_factor_dist_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ease factor distribution",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Card difficulty breakdown across ${dist.totalWords} words.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            buckets.forEach { (label, count, color) ->
                val ratio = count.toFloat() / total
                val pct = ratio * 100f
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "$count (${String.format(java.util.Locale.US, "%.1f", pct)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { ratio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.extraSmall),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastUpcomingReviewsCard(forecast: List<ForecastDayStat>) {
    val maxCount = forecast.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val totalUpcoming = forecast.sumOf { it.count }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("forecast_chart_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "14-day review forecast",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Upcoming review load for the next 14 days ($totalUpcoming total due).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    forecast.forEach { item ->
                        val isToday = item.dayOffset == 0
                        val barHeight = ((item.count.toFloat() / maxCount) * 56f).dp
                            .coerceAtLeast(if (item.count > 0) 6.dp else 2.dp)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.height(104.dp)
                        ) {
                            if (item.count > 0) {
                                Text(
                                    text = "${item.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        when {
                                            isToday -> MaterialTheme.colorScheme.tertiary
                                            item.count > 0 -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.dayOfWeek,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreakHeatmapCard(stats: DetailedSrsStats) {
    val streakDays = stats.streakHistoryLast30Days

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streak_heatmap_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "30-day streak heatmap",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: ${stats.currentStreak} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Longest: ${stats.longestStreak} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    streakDays.chunked(6).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowDays.forEach { day ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    shape = MaterialTheme.shapes.small,
                                    color = if (day.hasReview) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLowest
                                    },
                                    contentColor = if (day.hasReview) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    border = if (day.isToday) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = day.dateLabel.substringAfter('/'),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeechManagementCard(
    leechWords: List<WordEntity>,
    onClearLeechFlag: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leech_management_card"),
        shape = MaterialTheme.shapes.large,
        color = if (leechWords.isNotEmpty()) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (leechWords.isNotEmpty()) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (leechWords.isNotEmpty()) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Leech detection",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (leechWords.isNotEmpty()) {
                                "${leechWords.size} words repeatedly failing review"
                            } else {
                                "No leech words detected"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (leechWords.isNotEmpty()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (leechWords.isNotEmpty()) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                ) {
                    Text(
                        text = "${leechWords.size} leeches",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (leechWords.isEmpty()) {
                Text(
                    text = "Your retention is solid. Cards that exceed the lapse threshold will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Leech cards benefit from stronger mnemonics, simpler definitions, or context sentences.",
                    style = MaterialTheme.typography.bodySmall
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    leechWords.forEach { word ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leech_item_${word.word}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = word.word.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        LeechBadge(lapses = word.lapses)
                                    }
                                    Text(
                                        text = "${word.lapses} lapses • Ease ${String.format(java.util.Locale.US, "%.2f", word.easeFactor)} • Interval ${word.interval}d",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { onClearLeechFlag(word.word) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("clear_leech_${word.word}")
                                ) {
                                    Text("Clear flag")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
