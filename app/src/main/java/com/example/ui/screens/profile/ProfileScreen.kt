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
    val selectedAppIconId by viewModel.selectedAppIcon.collectAsStateWithLifecycle()
    val selectedThemeId by viewModel.selectedThemeColor.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEnglishLevel by viewModel.userEnglishLevel.collectAsStateWithLifecycle()
    val ieltsTargetBand by viewModel.ieltsTargetBand.collectAsStateWithLifecycle()
    val newCardsPerDay by viewModel.newCardsPerDay.collectAsStateWithLifecycle()
    val weeklyTarget by viewModel.weeklyNewWordsTarget.collectAsStateWithLifecycle()
    val weeklyStudied by viewModel.weeklyWordsStudiedCount.collectAsStateWithLifecycle()
    val activeIconMeta = remember(selectedAppIconId) { com.example.ui.SadaAppIcon.fromId(selectedAppIconId) }
    val activeThemeMeta = remember(selectedThemeId) {
        com.example.ui.theme.SadaBrandThemePresets.firstOrNull { it.id.equals(selectedThemeId, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
        viewModel.refreshLocalImageStats()
    }

    androidx.activity.compose.BackHandler(enabled = subPage != ProfileSubPage.HOME) {
        subPage = ProfileSubPage.HOME
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
                    text = if (subPage == ProfileSubPage.DAILY_READING) "IELTS Reading Corner" else "Sentence Mining",
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
            // Personalized Learner Identity Card (Name + Avatar + CEFR / IELTS Band + Weekly Goal)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_identity_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val initial = userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = userName.ifBlank { "Sada Learner" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("profile_user_name_text")
                            )
                            Text(
                                text = "CEFR $userEnglishLevel · Target IELTS Band $ieltsTargetBand",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onOpenSettingsTab(1) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("profile_edit_name_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit profile & targets",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Daily & Weekly Habit Target Summary Bar
                    val weekFrac = (weeklyStudied.toFloat() / weeklyTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weekly Habit Target",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$weeklyStudied / $weeklyTarget words · $newCardsPerDay/day",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { weekFrac },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
            }

            // Top KPI Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMetricCard(value = "${bookmarkedWords.size}", label = "In Deck", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                ProfileMetricCard(value = "$masteredCount", label = "Mastered", color = Color(0xFF30D158), modifier = Modifier.weight(1f))
                ProfileMetricCard(
                    value = "${(detailedStats.last30DaysRetentionRate * 100f).toInt()}%",
                    label = "Retention",
                    color = Color(0xFFFF9F0A),
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricCard(value = "🔥$currentStreak", label = "Streak", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
            }

            // Tools & Assets (Apple Inset Grouped)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppleSectionHeader(title = "Sada · Tools & Appearance")
                AppleInsetGroupedCard(cornerRadius = 18.dp) {
                    ProfileToolRow(
                        icon = Icons.Default.Palette,
                        tint = MaterialTheme.colorScheme.primary,
                        title = "App Icon & Color Theme",
                        subtitle = "${activeIconMeta.title} · ${activeThemeMeta?.label ?: "Sada Teal"}",
                        badgeText = "Customize",
                        onClick = { onOpenSettingsTab(1) }
                    )
                    AppleInsetDivider(startIndent = 60.dp)
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
                        tint = MaterialTheme.colorScheme.secondary,
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
                        onClick = { onOpenSettingsTab(2) }
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
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.5.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = color,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
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
