package com.example.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DictionaryViewModel
import com.example.ui.navigation.LocalWindowWidthSizeClass
import com.example.ui.theme.RedeusExpressiveType
import com.example.ui.theme.getDynamicColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DictionaryViewModel,
    onNavigateBack: () -> Unit,
    initialTab: Int = 0
) {
    BackHandler(onBack = onNavigateBack)

    val autoPronounce by viewModel.autoPronounceEnabled.collectAsStateWithLifecycle()
    val pronunciationSource by viewModel.pronunciationSource.collectAsStateWithLifecycle()
    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
    val selectedTtsVoiceName by viewModel.selectedTtsVoiceName.collectAsStateWithLifecycle()
    val availableOfflineVoices by viewModel.availableOfflineVoices.collectAsStateWithLifecycle()
    val activeOfflineVoiceLabel by viewModel.activeOfflineVoiceLabel.collectAsStateWithLifecycle()
    val speechSpeed by viewModel.speechSpeed.collectAsStateWithLifecycle()
    val geminiApiKeys by viewModel.geminiApiKeys.collectAsStateWithLifecycle()
    val activeKeyIndex by viewModel.activeKeyIndex.collectAsStateWithLifecycle()
    val lastPronunciationSource by viewModel.lastPronunciationSource.collectAsStateWithLifecycle()

    val geminiMeaningApiKeys by viewModel.geminiMeaningApiKeys.collectAsStateWithLifecycle()
    val geminiMeaningActiveKeyIndex by viewModel.geminiMeaningActiveKeyIndex.collectAsStateWithLifecycle()
    val geminiMeaningModelChoice by viewModel.geminiMeaningModelChoice.collectAsStateWithLifecycle()
    val geminiMeaningCustomModel by viewModel.geminiMeaningCustomModel.collectAsStateWithLifecycle()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsStateWithLifecycle()
    val pexelsApiKey by viewModel.pexelsApiKey.collectAsStateWithLifecycle()
    val useSeparateTtsKeys by viewModel.useSeparateTtsKeys.collectAsStateWithLifecycle()
    val isTestingGeminiKey by viewModel.isTestingGeminiKey.collectAsStateWithLifecycle()
    val geminiKeyTestStatus by viewModel.geminiKeyTestStatus.collectAsStateWithLifecycle()
    val ttsCooldownSeconds by viewModel.ttsCooldownSeconds.collectAsStateWithLifecycle()
    val wordApiCooldownSeconds by viewModel.wordApiCooldownSeconds.collectAsStateWithLifecycle()

    val aiVisualizationsEnabled by viewModel.aiVisualizationsEnabled.collectAsStateWithLifecycle()
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val alwaysNotifyEnabled by viewModel.alwaysNotifyEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
    val selectedThemeColor by viewModel.selectedThemeColor.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val arabicTranslationEnabled by viewModel.arabicTranslationEnabled.collectAsStateWithLifecycle()
    val isBackupBusy by viewModel.isBackupBusy.collectAsStateWithLifecycle()
    val pexelsQuerySource by viewModel.pexelsQuerySource.collectAsStateWithLifecycle()

    val easyBonus by viewModel.easyBonus.collectAsStateWithLifecycle()
    val startingEase by viewModel.startingEase.collectAsStateWithLifecycle()
    val intervalModifier by viewModel.intervalModifier.collectAsStateWithLifecycle()
    val hardIntervalMultiplier by viewModel.hardIntervalMultiplier.collectAsStateWithLifecycle()
    val leechThreshold by viewModel.leechThreshold.collectAsStateWithLifecycle()
    val leechWords by viewModel.leechWordsList.collectAsStateWithLifecycle()

    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val longestStreak by viewModel.longestStreak.collectAsStateWithLifecycle()
    val successRate by viewModel.successRate.collectAsStateWithLifecycle()
    val reviewsToday by viewModel.reviewsToday.collectAsStateWithLifecycle()
    val reviewsThisWeek by viewModel.reviewsThisWeek.collectAsStateWithLifecycle()

    val totalWordsCount by viewModel.totalWordsCount.collectAsStateWithLifecycle()
    val wordsMasteredCount by viewModel.wordsMasteredCount.collectAsStateWithLifecycle()
    val wordsLearningCount by viewModel.wordsLearningCount.collectAsStateWithLifecycle()
    val wordsNewCount by viewModel.wordsNewCount.collectAsStateWithLifecycle()
    val detailedSrsStats by viewModel.detailedSrsStats.collectAsStateWithLifecycle()

    val userEnglishLevel by viewModel.userEnglishLevel.collectAsStateWithLifecycle()
    val readingSessions by viewModel.readingSessionsState.collectAsStateWithLifecycle()
    val currentReadingLevel by viewModel.readingLevel.collectAsStateWithLifecycle()
    val aiUsageStats by viewModel.aiUsageStats.collectAsStateWithLifecycle()
    val geminiLocked by viewModel.geminiLocked.collectAsStateWithLifecycle()
    val srsAlgorithm by viewModel.srsAlgorithm.collectAsStateWithLifecycle()
    val targetRetention by viewModel.targetRetention.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val widthSizeClass = LocalWindowWidthSizeClass.current
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    var permissionState by remember { mutableStateOf("default") }
    var settingsTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) } // 0: Stats, 1: Preferences, 2: API keys, 3: SRS Settings
    var showAdvancedSrsSliders by rememberSaveable { mutableStateOf(false) }
    var showResetConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderTimePicker by rememberSaveable { mutableStateOf(false) }
    var showVoicePickerDialog by rememberSaveable { mutableStateOf(false) }

    fun updatePermissionState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            permissionState = if (granted) {
                "granted"
            } else {
                val sharedPrefs = context.getSharedPreferences("vocab_ai_prefs", Context.MODE_PRIVATE)
                val hasAsked = sharedPrefs.getBoolean("has_asked_notification_permission", false)
                if (hasAsked) "denied" else "default"
            }
        } else {
            permissionState = "granted"
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatePermissionState()
                viewModel.refreshStats()
                viewModel.refreshAvailableTtsVoices()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
        viewModel.refreshAvailableTtsVoices()
    }

    if (showVoicePickerDialog) {
        AlertDialog(
            onDismissRequest = { showVoicePickerDialog = false },
            title = { Text("Offline Neural Voice ($selectedAccent)") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            viewModel.setSelectedTtsVoiceName("")
                            viewModel.previewTtsVoice("Brilliant pronunciation with automatic offline neural voice.")
                            showVoicePickerDialog = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTtsVoiceName.isBlank()) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Auto (Best Offline $selectedAccent Neural)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatically prefers Google Offline Neural / Sonia / HD local voices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (availableOfflineVoices.isEmpty()) {
                        Text(
                            text = "No offline English voices detected yet. Tap \"Download UK/US Offline Voices\" to install Google Speech Services British or US offline voice packs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    } else {
                        availableOfflineVoices.take(24).forEach { voice ->
                            val isChosen = selectedTtsVoiceName == voice.name
                            Surface(
                                onClick = {
                                    viewModel.setSelectedTtsVoiceName(voice.name)
                                    viewModel.previewTtsVoice("Hello! This is how your flashcards will sound offline.")
                                    showVoicePickerDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChosen) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = voice.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChosen) FontWeight.SemiBold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${voice.localeTag} • ${if (voice.isLocal) "100% Offline" else "Requires Network"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoicePickerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val sharedPrefs = context.getSharedPreferences("vocab_ai_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("has_asked_notification_permission", true).apply()
        updatePermissionState()
        viewModel.setRemindersEnabled(isGranted)
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackupJson(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackupJson(uri)
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.exportAnkiCsv(uri)
    }

    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            title = { Text("Daily reminder time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setReminderTime(timePickerState.hour, timePickerState.minute)
                        showReminderTimePicker = false
                    },
                    modifier = Modifier.testTag("confirm_reminder_time_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset all progress?") },
            text = {
                Text(
                    "This will reset spaced-repetition progress for all $totalWordsCount words in your deck. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllWordsProgress()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_reset_all_button")
                ) {
                    Text("Reset all")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_reset_all_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val tabs = listOf(
        Triple("Stats", Icons.Default.Analytics, "settings_tab_stats"),
        Triple("Preferences", Icons.Default.Tune, "settings_tab_prefs"),
        Triple("API keys", Icons.Default.Key, "settings_tab_api"),
        Triple("SRS settings", Icons.Default.Psychology, "settings_tab_srs")
    )

    val tabContent: @Composable (Modifier) -> Unit = { contentModifier ->
        Column(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (settingsTab) {
                    0 -> {
                        // STREAK SUMMARY
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(52.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(30.dp))
                                    }
                                }
                                Column {
                                    Text(
                                        text = "$currentStreak day streak",
                                        style = RedeusExpressiveType.headword
                                    )
                                    Text(
                                        text = "Longest streak: $longestStreak days",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // REVIEW HISTORY
                        SettingsSectionCard(title = "Review history") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SettingsStatBox("Today", "$reviewsToday reviews", Modifier.weight(1f))
                                SettingsStatBox("This week", "$reviewsThisWeek reviews", Modifier.weight(1f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "Success rate: ${String.format(java.util.Locale.US, "%.1f", successRate)}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Percentage of flashcard reviews rated Good or Easy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // DECK COMPOSITION
                        SettingsSectionCard(title = "Deck composition ($totalWordsCount words)") {
                            val total = totalWordsCount.coerceAtLeast(1)
                            val masteredPct = wordsMasteredCount.toFloat() / total
                            val learningPct = wordsLearningCount.toFloat() / total
                            val newPct = wordsNewCount.toFloat() / total

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    if (masteredPct > 0) Box(modifier = Modifier.fillMaxHeight().weight(masteredPct).background(MaterialTheme.colorScheme.primary))
                                    if (learningPct > 0) Box(modifier = Modifier.fillMaxHeight().weight(learningPct).background(MaterialTheme.colorScheme.tertiary))
                                    if (newPct > 0) Box(modifier = Modifier.fillMaxHeight().weight(newPct).background(MaterialTheme.colorScheme.secondary))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DeckLegendDot("Mastered: $wordsMasteredCount", MaterialTheme.colorScheme.primary)
                                DeckLegendDot("Learning: $wordsLearningCount", MaterialTheme.colorScheme.tertiary)
                                DeckLegendDot("New: $wordsNewCount", MaterialTheme.colorScheme.secondary)
                            }
                        }

                        // READING PROGRESS
                        SettingsSectionCard(
                            title = "Reading progress",
                            modifier = Modifier.testTag("reading_progress_card")
                        ) {
                            val currentCefr = viewModel.getCefrLabel(currentReadingLevel)
                            if (readingSessions.isEmpty()) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "Complete your first Daily Reading to start tracking progress.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Current level: $currentCefr",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                val totalScore = readingSessions.sumOf { it.score }
                                val totalQuestions = readingSessions.sumOf { it.total }
                                val avgPct = if (totalQuestions > 0) (totalScore.toFloat() / totalQuestions.toFloat()) * 100f else 0f

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SettingsStatBox(
                                        label = "Current level",
                                        value = currentCefr,
                                        subtitle = "${readingSessions.size} sessions",
                                        modifier = Modifier.weight(1f)
                                    )
                                    SettingsStatBox(
                                        label = "Average score",
                                        value = "${avgPct.toInt()}%",
                                        subtitle = "$totalScore/$totalQuestions correct",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                val lastSessions = readingSessions.takeLast(10)
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(68.dp)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        lastSessions.forEach { session ->
                                            val ratio = if (session.total > 0) session.score.toFloat() / session.total.toFloat() else 0f
                                            val barHeight = (ratio * 34f).dp.coerceAtLeast(8.dp)
                                            val barColor = when {
                                                ratio >= 0.8f -> MaterialTheme.colorScheme.primary
                                                ratio >= 0.5f -> MaterialTheme.colorScheme.tertiary
                                                else -> MaterialTheme.colorScheme.error
                                            }
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Bottom,
                                                modifier = Modifier.fillMaxHeight()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(16.dp)
                                                        .height(barHeight)
                                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                        .background(barColor)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${session.score}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // AI USAGE TODAY
                        SettingsSectionCard(
                            title = "AI usage today",
                            modifier = Modifier.testTag("ai_usage_card")
                        ) {
                            if (aiUsageStats.geminiCallsToday == 0 && aiUsageStats.openRouterCallsToday == 0) {
                                Text(
                                    text = "No AI requests yet today",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("ai_usage_empty_text")
                                )
                            } else {
                                Text(
                                    text = "Gemini: ${aiUsageStats.geminiCallsToday} requests",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("ai_usage_gemini_row")
                                )
                                Text(
                                    text = "Backup (OpenRouter): ${aiUsageStats.openRouterCallsToday} requests",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("ai_usage_backup_row")
                                )
                                val lastProviderLabel = if (aiUsageStats.lastProviderUsed.equals("openrouter", ignoreCase = true)) {
                                    "Backup"
                                } else {
                                    "Gemini"
                                }
                                Text(
                                    text = "Last response from: $lastProviderLabel",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("ai_usage_last_provider")
                                )
                            }
                        }

                        // LOCK GEMINI
                        SettingsSectionCard(
                            title = "Gemini lock",
                            modifier = Modifier.testTag("lock_gemini_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setGeminiLocked(!geminiLocked) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Lock Gemini for today",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (geminiLocked) {
                                            "Gemini locked — using backup (OpenRouter)"
                                        } else {
                                            "Gemini active"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (geminiLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("gemini_lock_status_text")
                                    )
                                }
                                Switch(
                                    checked = geminiLocked,
                                    onCheckedChange = { viewModel.setGeminiLocked(it) },
                                    modifier = Modifier.testTag("lock_gemini_switch")
                                )
                            }
                        }
                    }

                    1 -> {
                        // ENGLISH LEVEL
                        SettingsSectionCard(title = "My English level") {
                            Text(
                                text = "Choose your CEFR level to calibrate Daily Reading passage difficulty.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("A1", "A2", "B1", "B2", "C1").forEach { levelOption ->
                                    val isSelected = userEnglishLevel.equals(levelOption, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setUserEnglishLevel(levelOption) },
                                        label = { Text(levelOption) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier.testTag("english_level_chip_$levelOption")
                                    )
                                }
                            }
                        }

                        // AUDIO & PRONUNCIATION
                        SettingsSectionCard(title = "Audio & pronunciation") {
                            SettingsToggleRow(
                                title = "Auto-pronounce words",
                                subtitle = "Speak pronunciation automatically when cards appear",
                                checked = autoPronounce,
                                onCheckedChange = { viewModel.setAutoPronounceEnabled(it) },
                                switchTag = "auto_pronounce_switch"
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Text(
                                text = "Pronunciation voice source",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (pronunciationSource) {
                                    "online" -> "Online (Gemini AI voice)"
                                    "device" -> "Device voice (built-in Android TTS)"
                                    else -> "Auto (Gemini AI first, fallback to device TTS)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Auto" to "auto", "Online" to "online", "Device" to "device").forEach { (label, value) ->
                                    val isSelected = pronunciationSource == value
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setPronunciationSource(value) },
                                        label = { Text(label) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pronunciation_source_chip_$value")
                                    )
                                }
                            }

                            val displaySource = lastPronunciationSource.ifBlank { "Not used yet" }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("last_pronunciation_source_audio_row")
                            ) {
                                Text(
                                    text = "Last pronunciation source: $displaySource",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("last_pronunciation_source_text")
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Text(
                                text = "Offline Neural Accent & Voice",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Active: $activeOfflineVoiceLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("UK British" to "UK", "US American" to "US").forEach { (label, value) ->
                                    val isSelected = selectedAccent.equals(value, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setSelectedAccent(value) },
                                        label = { Text(label) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("accent_chip_$value")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.refreshAvailableTtsVoices()
                                        showVoicePickerDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("choose_offline_voice_button")
                                ) {
                                    Text("Choose Voice (${availableOfflineVoices.size})")
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.previewTtsVoice() },
                                    modifier = Modifier.testTag("test_offline_voice_button")
                                ) {
                                    Text("Test")
                                }
                            }

                            TextButton(
                                onClick = {
                                    try {
                                        val installIntent = android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                                        context.startActivity(installIntent)
                                    } catch (_: Exception) {
                                        try {
                                            val settingsIntent = android.content.Intent("com.android.settings.TTS_SETTINGS")
                                            context.startActivity(settingsIntent)
                                        } catch (_: Exception) {
                                            viewModel.showSnackbar("Open Android Settings → Text-to-Speech to download Google UK/US offline voices.")
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("install_offline_tts_data_button")
                            ) {
                                Text("Download Google UK/US Offline Voice Packs")
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Speech speed", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Playback rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Normal", "Slow").forEach { value ->
                                        val isSelected = speechSpeed == value
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setSpeechSpeed(value) },
                                            label = { Text(value) },
                                            modifier = Modifier.testTag("speech_speed_chip_$value")
                                        )
                                    }
                                }
                            }
                        }

                        // APPEARANCE
                        SettingsSectionCard(title = "Appearance") {
                            Text(
                                text = "Theme mode",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("System" to "system", "Light" to "light", "Dark" to "dark").forEach { (label, value) ->
                                    val isSelected = themeMode == value
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setThemeMode(value) },
                                        label = { Text(label) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("theme_mode_chip_$value")
                                    )
                                }
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                SettingsToggleRow(
                                    title = "Material You dynamic color",
                                    subtitle = "Match app colors to your wallpaper when Default palette is selected",
                                    checked = dynamicColorEnabled,
                                    onCheckedChange = { viewModel.setDynamicColorEnabled(it) },
                                    switchTag = "dynamic_color_switch"
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Text(
                                text = "Color palette",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose a preset color palette for the app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val presets = listOf("yellow", "blue", "green", "orange", "pink", "teal")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                presets.forEach { name ->
                                    val presetScheme = getDynamicColorScheme(name, darkTheme)
                                    val isSelected = selectedThemeColor.equals(name, ignoreCase = true)
                                    Surface(
                                        onClick = { viewModel.setSelectedThemeColor(name) },
                                        shape = CircleShape,
                                        color = presetScheme.primary,
                                        contentColor = presetScheme.onPrimary,
                                        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .semantics { contentDescription = "Theme palette $name" }
                                            .testTag("color_theme_$name")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(22.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            SettingsToggleRow(
                                title = "Show word photos",
                                subtitle = "Show real photos on word cards",
                                checked = aiVisualizationsEnabled,
                                onCheckedChange = { viewModel.setAiVisualizationsEnabled(it) },
                                switchTag = "ai_visualizations_switch"
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            SettingsToggleRow(
                                title = "Arabic meanings (Offline + AI)",
                                subtitle = "Show Arabic meanings automatically on word cards, flashcards & FSRS review",
                                checked = arabicTranslationEnabled,
                                onCheckedChange = { viewModel.setArabicTranslationEnabled(it) },
                                switchTag = "arabic_translation_switch"
                            )
                        }

                        // REAL PHOTO SEARCH SOURCE
                        SettingsSectionCard(
                            title = "Real photo search source",
                            modifier = Modifier.testTag("pexels_query_source_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Word" to "word", "Definition" to "definition", "AI desc" to "image_prompt").forEach { (label, value) ->
                                    val isSelected = pexelsQuerySource == value
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setPexelsQuerySource(value) },
                                        label = { Text(label) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pexels_query_source_chip_$value")
                                    )
                                }
                            }
                        }

                        // STUDY REMINDERS
                        SettingsSectionCard(title = "Study reminders") {
                            val toggleReminders: (Boolean) -> Unit = { nextState ->
                                if (nextState) {
                                    if (permissionState == "granted" || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        viewModel.setRemindersEnabled(true)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    viewModel.setRemindersEnabled(false)
                                }
                            }
                            SettingsToggleRow(
                                title = "Daily reminder notifications",
                                subtitle = "Receive alerts when vocabulary cards are due",
                                checked = remindersEnabled && permissionState == "granted",
                                onCheckedChange = toggleReminders,
                                switchTag = "study_reminders_switch"
                            )

                            if (remindersEnabled) {
                                when (permissionState) {
                                    "granted" -> {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Text("Daily study reminders are active", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                        SettingsToggleRow(
                                            title = "Always notify daily",
                                            subtitle = "Send a reminder even if no cards are due",
                                            checked = alwaysNotifyEnabled,
                                            onCheckedChange = { viewModel.setAlwaysNotifyEnabled(it) },
                                            switchTag = "always_notify_switch"
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        val formattedTime = remember(reminderHour, reminderMinute) {
                                            val h12 = when {
                                                reminderHour == 0 -> 12
                                                reminderHour > 12 -> reminderHour - 12
                                                else -> reminderHour
                                            }
                                            val amPm = if (reminderHour < 12) "AM" else "PM"
                                            String.format(java.util.Locale.US, "%d:%02d %s", h12, reminderMinute, amPm)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Reminder time",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Daily at $formattedTime",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.testTag("reminder_time_text")
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = { showReminderTimePicker = true },
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("change_reminder_time_button")
                                            ) {
                                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Change")
                                            }
                                        }
                                    }
                                    "denied" -> {
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("Notifications are blocked", style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    "Enable notifications in Android Settings > Apps > Redeus Dictionary.",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                                data = Uri.fromParts("package", context.packageName, null)
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Log.e("SettingsScreen", "Failed to open settings", e)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                        contentColor = MaterialTheme.colorScheme.onError
                                                    ),
                                                    modifier = Modifier
                                                        .heightIn(min = 48.dp)
                                                        .testTag("open_settings_btn")
                                                ) {
                                                    Text("Open system settings")
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("enable_notifications_btn")
                                        ) {
                                            Text("Enable notifications")
                                        }
                                    }
                                }
                            }
                        }

                        // BACKUP & EXPORT
                        SettingsSectionCard(
                            title = "Backup & export",
                            modifier = Modifier.testTag("backup_export_card")
                        ) {
                            Text(
                                text = "Back up your saved words, mined sentences, reading history, and SRS progress, or export your deck to Anki.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { exportBackupLauncher.launch("redeus_backup.json") },
                                    enabled = !isBackupBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("export_backup_json_button")
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export JSON")
                                }
                                OutlinedButton(
                                    onClick = { importBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
                                    enabled = !isBackupBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("import_backup_json_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore JSON")
                                }
                            }
                            OutlinedButton(
                                onClick = { exportCsvLauncher.launch("redeus_anki_deck.csv") },
                                enabled = !isBackupBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("export_anki_csv_button")
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export deck to Anki (CSV)")
                            }
                        }
                    }

                    2 -> {
                        // UNIFIED GEMINI API KEYS (PRIMARY)
                        SettingsSectionCard(
                            title = if (useSeparateTtsKeys) "Gemini API keys (Meaning & Reading)" else "Gemini API keys",
                            modifier = Modifier.testTag("gemini_meaning_keys_card")
                        ) {
                            Text(
                                text = if (useSeparateTtsKeys) {
                                    "Store Gemini API keys for word definitions, examples, and Daily Reading generation."
                                } else {
                                    "One Gemini API key powers word definitions, Daily Reading, and AI pronunciation. Multiple keys automatically fail over on rate limits."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ApiKeyDirectLinkBanner(
                                title = "Get Gemini API key (Free)",
                                subtitle = "aistudio.google.com/app/apikey",
                                url = "https://aistudio.google.com/app/apikey",
                                testTag = "gemini_meaning_direct_link_banner"
                            )
                            if (geminiMeaningApiKeys.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    geminiMeaningApiKeys.forEachIndexed { idx, _ ->
                                        FilterChip(
                                            selected = geminiMeaningActiveKeyIndex == idx,
                                            onClick = { viewModel.setGeminiMeaningActiveKeyIndex(idx) },
                                            label = { Text(if (geminiMeaningActiveKeyIndex == idx) "Active: Key ${idx + 1}" else "Key ${idx + 1}") },
                                            modifier = Modifier.testTag("active_meaning_key_${idx + 1}_chip")
                                        )
                                    }
                                }
                            }
                            if (geminiMeaningApiKeys.isEmpty()) {
                                TextButton(
                                    onClick = { openExternalUrl(uriHandler, context, "https://aistudio.google.com/app/apikey") },
                                    modifier = Modifier.testTag("gemini_meaning_empty_get_key_link")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Get your free Gemini API key")
                                }
                            } else {
                                geminiMeaningApiKeys.forEachIndexed { index, apiKey ->
                                    GeminiKeyRowItem(
                                        index = index,
                                        apiKey = apiKey,
                                        isActive = geminiMeaningActiveKeyIndex == index,
                                        onKeyChange = { viewModel.updateGeminiMeaningApiKey(index, it) },
                                        onSelectActive = { viewModel.setGeminiMeaningActiveKeyIndex(index) },
                                        onDelete = { viewModel.removeGeminiMeaningApiKey(index) },
                                        labelPrefix = "Gemini Key",
                                        testTagPrefix = "gemini_meaning_key",
                                        directLinkUrl = "https://aistudio.google.com/app/apikey",
                                        directLinkLabel = "Get key from Google AI Studio"
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { viewModel.addGeminiMeaningApiKey("") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("add_gemini_meaning_key_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Gemini key")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.testActiveGeminiKey() },
                                    enabled = !isTestingGeminiKey && geminiMeaningApiKeys.any { it.trim().isNotBlank() },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("test_gemini_key_button")
                                ) {
                                    if (isTestingGeminiKey) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isTestingGeminiKey) "Testing..." else "Test key")
                                }
                            }
                            val testStatus = geminiKeyTestStatus
                            if (testStatus != null) {
                                val (ok, statusMsg) = testStatus
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    contentColor = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = statusMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("gemini_key_test_status")
                                    )
                                }
                            }
                            if (geminiMeaningApiKeys.none { it.trim().isNotBlank() }) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No key set — this feature is disabled until you add one.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("gemini_meaning_disabled_hint")
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Text("Gemini model", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            var modelMenuExpanded by remember { mutableStateOf(false) }
                            val commonModels = listOf(
                                "gemini-3.5-flash-lite",
                                "gemini-4-hydrogen",
                                "gemini-3.5-flash",
                                "gemini-3.5-pro",
                                "gemini-2.5-flash",
                                "gemini-2.5-pro",
                                "gemini-2.0-flash",
                                "gemini-2.0-flash-lite",
                                "gemini-1.5-flash",
                                "gemini-1.5-pro",
                                "Custom..."
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { modelMenuExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("gemini_model_selector_dropdown")
                                ) {
                                    Text(
                                        text = if (geminiMeaningModelChoice == "Custom..." || geminiMeaningModelChoice.equals("custom", ignoreCase = true)) {
                                            "Custom (${geminiMeaningCustomModel.ifBlank { "None" }})"
                                        } else {
                                            geminiMeaningModelChoice
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Gemini model")
                                }
                                DropdownMenu(
                                    expanded = modelMenuExpanded,
                                    onDismissRequest = { modelMenuExpanded = false }
                                ) {
                                    commonModels.forEach { modelOption ->
                                        val isSelected = geminiMeaningModelChoice.equals(modelOption, ignoreCase = true)
                                        DropdownMenuItem(
                                            text = { Text(modelOption) },
                                            trailingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                            } else null,
                                            onClick = {
                                                viewModel.setGeminiMeaningModelChoice(modelOption)
                                                modelMenuExpanded = false
                                            },
                                            modifier = Modifier.testTag("model_option_$modelOption")
                                        )
                                    }
                                }
                            }

                            if (geminiMeaningModelChoice == "Custom..." || geminiMeaningModelChoice.equals("custom", ignoreCase = true)) {
                                OutlinedTextField(
                                    value = geminiMeaningCustomModel,
                                    onValueChange = { viewModel.setGeminiMeaningCustomModel(it) },
                                    label = { Text("Custom model ID") },
                                    placeholder = { Text("e.g. gemini-3.5-flash-lite") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_gemini_model_input")
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            SettingsToggleRow(
                                title = "Use separate keys for TTS pronunciation",
                                subtitle = "Optional — keep separate quota for online AI voice",
                                checked = useSeparateTtsKeys,
                                onCheckedChange = { viewModel.setUseSeparateTtsKeys(it) },
                                switchTag = "use_separate_tts_keys_switch"
                            )
                        }

                        if (useSeparateTtsKeys) {
                            // GEMINI TTS API KEYS (OPTIONAL SEPARATE QUOTA)
                            SettingsSectionCard(title = "Gemini TTS API keys") {
                                Text(
                                    text = "Store dedicated Gemini API keys for online pronunciation. If the active key hits rate limits (HTTP 429), remaining keys are tried automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                ApiKeyDirectLinkBanner(
                                    title = "Get Gemini API key (Free)",
                                    subtitle = "aistudio.google.com/app/apikey",
                                    url = "https://aistudio.google.com/app/apikey",
                                    testTag = "gemini_tts_direct_link_banner"
                                )
                                Text(
                                    text = if (geminiApiKeys.isEmpty()) "No keys added (device TTS fallback)" else "Active: Key ${activeKeyIndex + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (geminiApiKeys.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        geminiApiKeys.forEachIndexed { idx, _ ->
                                            FilterChip(
                                                selected = activeKeyIndex == idx,
                                                onClick = { viewModel.setActiveKeyIndex(idx) },
                                                label = { Text(if (activeKeyIndex == idx) "Active: Key ${idx + 1}" else "Key ${idx + 1}") },
                                                modifier = Modifier.testTag("active_key_${idx + 1}_chip")
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Last pronunciation source: ${lastPronunciationSource.ifBlank { "Not used yet" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("last_pronunciation_source_keys_row")
                                )
                                if (geminiApiKeys.isEmpty()) {
                                    TextButton(
                                        onClick = { openExternalUrl(uriHandler, context, "https://aistudio.google.com/app/apikey") },
                                        modifier = Modifier.testTag("gemini_tts_empty_get_key_link")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Get your free Gemini API key")
                                    }
                                } else {
                                    geminiApiKeys.forEachIndexed { index, apiKey ->
                                        GeminiKeyRowItem(
                                            index = index,
                                            apiKey = apiKey,
                                            isActive = activeKeyIndex == index,
                                            onKeyChange = { viewModel.updateGeminiApiKey(index, it) },
                                            onSelectActive = { viewModel.setActiveKeyIndex(index) },
                                            onDelete = { viewModel.removeGeminiApiKey(index) },
                                            directLinkUrl = "https://aistudio.google.com/app/apikey",
                                            directLinkLabel = "Get key from Google AI Studio"
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.addGeminiApiKey("") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("add_gemini_key_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Gemini key")
                                }
                            }
                        }

                        // REQUEST COOLDOWN (RATE-LIMIT PROTECTION)
                        SettingsSectionCard(
                            title = "Request cooldown",
                            modifier = Modifier.testTag("api_cooldown_card")
                        ) {
                            Text(
                                text = "Minimum wait time between consecutive network API requests to avoid rate limits (HTTP 429). Cached words and audio play instantly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Word & AI API cooldown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Word & AI API cooldown",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (wordApiCooldownSeconds == 0) "Off" else "${wordApiCooldownSeconds}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("word_api_cooldown_value")
                                )
                            }
                            Slider(
                                value = wordApiCooldownSeconds.coerceIn(0, 60).toFloat(),
                                onValueChange = { viewModel.setWordApiCooldownSeconds(it.toInt()) },
                                valueRange = 0f..60f,
                                steps = 11,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("word_api_cooldown_slider")
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0, 5, 15, 30, 60).forEach { sec ->
                                    FilterChip(
                                        selected = wordApiCooldownSeconds == sec,
                                        onClick = { viewModel.setWordApiCooldownSeconds(sec) },
                                        label = { Text(if (sec == 0) "Off" else "${sec}s") },
                                        modifier = Modifier.testTag("word_api_cooldown_chip_$sec")
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // TTS pronunciation cooldown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TTS pronunciation cooldown",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (ttsCooldownSeconds == 0) "Off" else "${ttsCooldownSeconds}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("tts_cooldown_value")
                                )
                            }
                            Slider(
                                value = ttsCooldownSeconds.coerceIn(0, 60).toFloat(),
                                onValueChange = { viewModel.setTtsCooldownSeconds(it.toInt()) },
                                valueRange = 0f..60f,
                                steps = 11,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tts_cooldown_slider")
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0, 5, 15, 30, 60).forEach { sec ->
                                    FilterChip(
                                        selected = ttsCooldownSeconds == sec,
                                        onClick = { viewModel.setTtsCooldownSeconds(sec) },
                                        label = { Text(if (sec == 0) "Off" else "${sec}s") },
                                        modifier = Modifier.testTag("tts_cooldown_chip_$sec")
                                    )
                                }
                            }
                        }

                        // OPENROUTER & PEXELS KEYS
                        SingleApiKeyCard(
                            heading = "OpenRouter API key",
                            description = "Fallback provider when Gemini hits rate limits or is locked.",
                            currentKey = openRouterApiKey,
                            placeholder = "Enter OpenRouter API key",
                            testTagPrefix = "openrouter_key",
                            directLinkUrl = "https://openrouter.ai/keys",
                            directLinkLabel = "Get OpenRouter API key (Free)",
                            directLinkSubtitle = "openrouter.ai/keys",
                            onSaveKey = { viewModel.setOpenRouterApiKey(it) },
                            onClearKey = { viewModel.setOpenRouterApiKey("") }
                        )

                        SingleApiKeyCard(
                            heading = "Pexels API key",
                            description = "Used to fetch real photos for words.",
                            currentKey = pexelsApiKey,
                            placeholder = "Enter Pexels API key",
                            testTagPrefix = "pexels_key",
                            directLinkUrl = "https://www.pexels.com/api/",
                            directLinkLabel = "Get Pexels API key (Free)",
                            directLinkSubtitle = "pexels.com/api",
                            onSaveKey = { viewModel.setPexelsApiKey(it) },
                            onClearKey = { viewModel.setPexelsApiKey("") }
                        )
                    }

                    3 -> {
                        // DETAILED SRS STATS HEADER
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("detailed_stats_header_card"),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(32.dp))
                                Column {
                                    Text("Detailed SRS statistics", style = MaterialTheme.typography.titleMedium)
                                    Text("Spaced-repetition memory performance and controls", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        DetailedRetentionRateCard(stats = detailedSrsStats)
                        ReviewsPerDayChartCard(stats = detailedSrsStats)
                        RatingDistributionCard(dist = detailedSrsStats.ratingDistribution)
                        EaseFactorDistributionCard(dist = detailedSrsStats.easeFactorDistribution)
                        ForecastUpcomingReviewsCard(forecast = detailedSrsStats.forecastNext14Days)
                        StreakHeatmapCard(stats = detailedSrsStats)
                        LeechManagementCard(
                            leechWords = leechWords,
                            onClearLeechFlag = { viewModel.clearLeechFlag(it) }
                        )

                        // SCHEDULING ALGORITHM
                        SettingsSectionCard(title = "Scheduling algorithm") {
                            Text(
                                text = "Select the algorithm used for spaced-repetition reviews.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isSm2 = srsAlgorithm == "sm2"
                                val isFsrs = srsAlgorithm == "fsrs"
                                AlgorithmChoiceTile(
                                    title = "SM-2 (Classic)",
                                    subtitle = "Battle-tested standard",
                                    selected = isSm2,
                                    onClick = { viewModel.setSrsAlgorithm("sm2") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("srs_algorithm_sm2_button")
                                )
                                AlgorithmChoiceTile(
                                    title = "FSRS",
                                    subtitle = "Modern DSR model",
                                    selected = isFsrs,
                                    onClick = { viewModel.setSrsAlgorithm("fsrs") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("srs_algorithm_fsrs_button")
                                )
                            }
                            Text(
                                text = "You can switch between algorithms anytime.",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (srsAlgorithm == "fsrs") {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Target retention", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text("$targetRetention%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = targetRetention.toFloat(),
                                    onValueChange = { viewModel.setTargetRetention(it.toInt()) },
                                    valueRange = 80f..97f,
                                    steps = 16,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("srs_target_retention_slider")
                                )
                            }
                        }

                        // ADVANCED SRS PARAMETERS
                        SettingsSectionCard(title = "Advanced SRS parameters") {
                            SettingsToggleRow(
                                title = "Show SM-2 parameter sliders",
                                subtitle = "Fine-tune repetition scheduling multipliers",
                                checked = showAdvancedSrsSliders,
                                onCheckedChange = { showAdvancedSrsSliders = it },
                                switchTag = "advanced_srs_sliders_switch"
                            )

                            if (showAdvancedSrsSliders) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                SrsSliderRow(
                                    label = "Easy bonus",
                                    valueText = String.format(java.util.Locale.US, "%.2f", easyBonus),
                                    valueTag = "easy_bonus_value",
                                    value = easyBonus,
                                    range = 1.0f..3.0f,
                                    sliderTag = "easy_bonus_slider",
                                    onValueChange = { viewModel.setEasyBonus(it) }
                                )
                                SrsSliderRow(
                                    label = "Starting ease",
                                    valueText = String.format(java.util.Locale.US, "%.2f", startingEase),
                                    valueTag = "starting_ease_value",
                                    value = startingEase,
                                    range = 1.3f..4.0f,
                                    sliderTag = "starting_ease_slider",
                                    onValueChange = { viewModel.setStartingEase(it) }
                                )
                                SrsSliderRow(
                                    label = "Interval modifier",
                                    valueText = String.format(java.util.Locale.US, "%.2f", intervalModifier),
                                    valueTag = "interval_modifier_value",
                                    value = intervalModifier,
                                    range = 0.2f..2.0f,
                                    sliderTag = "interval_modifier_slider",
                                    onValueChange = { viewModel.setIntervalModifier(it) }
                                )
                                SrsSliderRow(
                                    label = "Hard interval multiplier",
                                    valueText = String.format(java.util.Locale.US, "%.2f", hardIntervalMultiplier),
                                    valueTag = "hard_interval_multiplier_value",
                                    value = hardIntervalMultiplier,
                                    range = 1.0f..2.0f,
                                    sliderTag = "hard_interval_multiplier_slider",
                                    onValueChange = { viewModel.setHardIntervalMultiplier(it) }
                                )
                                SrsSliderRow(
                                    label = "Leech threshold",
                                    valueText = "$leechThreshold lapses",
                                    valueTag = "leech_threshold_value",
                                    value = leechThreshold.toFloat(),
                                    range = 1f..20f,
                                    steps = 18,
                                    sliderTag = "leech_threshold_slider",
                                    onValueChange = { viewModel.setLeechThreshold(it.toInt()) }
                                )
                                OutlinedButton(
                                    onClick = { viewModel.resetSrsSettingsToDefault() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("reset_srs_defaults_button")
                                ) {
                                    Text("Reset SRS settings to defaults")
                                }
                            }
                        }

                        // DANGER ZONE
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Danger zone", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Resetting progress will return all vocabulary cards to their initial learning stage.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { showResetConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("reset_all_progress_button")
                                ) {
                                    Text("Reset all progress")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tabs.forEachIndexed { index, (title, icon, tag) ->
                        NavigationDrawerItem(
                            label = { Text(title) },
                            selected = settingsTab == index,
                            onClick = { settingsTab = index },
                            icon = { Icon(icon, contentDescription = null) },
                            modifier = Modifier.testTag(tag)
                        )
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                tabContent(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = settingsTab,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, (title, _, tag) ->
                        Tab(
                            selected = settingsTab == index,
                            onClick = { settingsTab = index },
                            text = { Text(title, maxLines = 1) },
                            modifier = Modifier.testTag(tag)
                        )
                    }
                }
                tabContent(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun SettingsDialog(
    viewModel: DictionaryViewModel,
    onDismiss: () -> Unit
) {
    SettingsScreen(viewModel = viewModel, onNavigateBack = onDismiss)
}

@Composable
private fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(switchTag)
        )
    }
}

@Composable
private fun SettingsStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DeckLegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AlgorithmChoiceTile(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.heightIn(min = 64.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SrsSliderRow(
    label: String,
    valueText: String,
    valueTag: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    sliderTag: String,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag(valueTag)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(sliderTag)
        )
    }
}
