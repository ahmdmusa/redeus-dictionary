package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WordEntity
import com.example.domain.CourseProgressCalculator
import com.example.ui.DailyReadingUiState
import com.example.ui.DictionaryViewModel
import com.example.ui.components.*
import com.example.ui.screens.mining.SentenceMiningHubView
import com.example.ui.screens.reading.DailyReadingTabContent
import com.example.ui.screens.settings.*

private enum class ProfileSubPage { HOME, DAILY_READING, SENTENCE_MINING }

/**
 * Tab 4: Profile, Progress Analytics & Tools.
 * Surfaces the 723-line SRS analytics suite out of Settings, displays permanent local photo assets,
 * and cleanly houses optional tools (Daily Reading with Online AI badge, Sentence Mining, Voice/TTS, Settings).
 */
@Composable
fun ProfileScreen(
    viewModel: DictionaryViewModel,
    bookmarkedWords: List<WordEntity>,
    dailyReadingState: DailyReadingUiState,
    innerPadding: PaddingValues,
    onOpenSettingsTab: (Int) -> Unit
) {
    var subPage by rememberSaveable { mutableStateOf(ProfileSubPage.HOME) }
    val detailedStats by viewModel.detailedSrsStats.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val bundledImages by viewModel.bundledLocalImagesCount.collectAsStateWithLifecycle()
    val savedImages by viewModel.savedLocalImagesCount.collectAsStateWithLifecycle()
    val activeVoiceLabel by viewModel.activeOfflineVoiceLabel.collectAsStateWithLifecycle()
    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
        viewModel.refreshLocalImageStats()
    }

    if (subPage != ProfileSubPage.HOME) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { subPage = ProfileSubPage.HOME },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Profile")
                }
                Text(
                    text = if (subPage == ProfileSubPage.DAILY_READING) "Daily Reading (Online AI)" else "Sentence Mining",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            when (subPage) {
                ProfileSubPage.DAILY_READING -> DailyReadingTabContent(
                    viewModel = viewModel,
                    readingState = dailyReadingState,
                    innerPadding = PaddingValues(0.dp)
                )
                ProfileSubPage.SENTENCE_MINING -> SentenceMiningHubView(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
                ProfileSubPage.HOME -> Unit
            }
        }
        return
    }

    val masteredCount = remember(bookmarkedWords) {
        bookmarkedWords.count { CourseProgressCalculator.isMastered(it) }
    }
    val leechWords = remember(bookmarkedWords) {
        bookmarkedWords.filter { it.isLeech }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("profile_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top KPI Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileMetricCard(value = "${bookmarkedWords.size}", label = "In Deck", color = Color(0xFF0A84FF), modifier = Modifier.weight(1f))
                ProfileMetricCard(value = "$masteredCount", label = "Mastered", color = Color(0xFF30D158), modifier = Modifier.weight(1f))
                ProfileMetricCard(
                    value = "${(detailedStats.last30DaysRetentionRate * 100f).toInt()}%",
                    label = "Retention",
                    color = Color(0xFFFF9F0A),
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricCard(value = "🔥$currentStreak", label = "Streak", color = Color(0xFFFF453A), modifier = Modifier.weight(1f))
            }

            // Tools & Assets (Apple Inset Grouped)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppleSectionHeader(title = "Tools & Offline Assets")
                AppleInsetGroupedCard(cornerRadius = 18.dp) {
                    ProfileToolRow(
                        icon = Icons.Default.Image,
                        tint = Color(0xFF30D158),
                        title = "Local Image Assets",
                        subtitle = "$bundledImages bundled offline + $savedImages saved permanently",
                        badgeText = "Offline",
                        onClick = { viewModel.refreshLocalImageStats() }
                    )
                    AppleInsetDivider(startIndent = 60.dp)
                    ProfileToolRow(
                        icon = Icons.Default.RecordVoiceOver,
                        tint = Color(0xFF0A84FF),
                        title = "Voice & Offline TTS ($selectedAccent)",
                        subtitle = activeVoiceLabel.ifBlank { "Priority: Natural Offline → Online → AI" },
                        onClick = { onOpenSettingsTab(1) }
                    )
                    AppleInsetDivider(startIndent = 60.dp)
                    ProfileToolRow(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        tint = Color(0xFFFF9F0A),
                        title = "Daily Reading",
                        subtitle = "Adaptive AI passages & comprehension",
                        badgeText = "Online AI",
                        onClick = { subPage = ProfileSubPage.DAILY_READING }
                    )
                    AppleInsetDivider(startIndent = 60.dp)
                    ProfileToolRow(
                        icon = Icons.Default.FormatQuote,
                        tint = Color(0xFF5E5CE6),
                        title = "Sentence Mining",
                        subtitle = "Practice sentences saved from cards & dictionary",
                        onClick = { subPage = ProfileSubPage.SENTENCE_MINING }
                    )
                    AppleInsetDivider(startIndent = 60.dp)
                    ProfileToolRow(
                        icon = Icons.Default.Tune,
                        tint = Color(0xFF8E8E93),
                        title = "FSRS, API Keys & Backup",
                        subtitle = "Algorithm, Gemini/Pexels keys, JSON & Anki CSV",
                        onClick = { onOpenSettingsTab(1) }
                    )
                }
            }

            // Analytics elevated directly out of Settings!
            AppleSectionHeader(title = "30-Day Activity & Forecast")
            StreakHeatmapCard(stats = detailedStats)
            ForecastUpcomingReviewsCard(forecast = detailedStats.forecastNext14Days)
            DetailedRetentionRateCard(stats = detailedStats)
            ReviewsPerDayChartCard(stats = detailedStats)

            if (leechWords.isNotEmpty()) {
                LeechManagementCard(
                    leechWords = leechWords,
                    onClearLeechFlag = { viewModel.clearLeechFlag(it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProfileMetricCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = color,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProfileToolRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppleIconBadge(icon = icon, tintColor = tint, size = 36.dp, iconSize = 19.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}
