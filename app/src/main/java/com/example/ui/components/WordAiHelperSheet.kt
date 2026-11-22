package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.data.LocalImageOrigin
import com.example.data.WordEntity
import com.example.data.WordImageRequest
import com.example.ui.DictionaryViewModel
import com.example.ui.rememberAppImageLoader

/**
 * Highlights occurrences of [targetWord] (and close stem prefixes) inside an example sentence.
 */
@Composable
fun HighlightedExampleText(
    example: String,
    targetWord: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    val accent = MaterialTheme.colorScheme.primary
    val baseColor = MaterialTheme.colorScheme.onSurface
    val cleanTarget = targetWord.trim()

    val annotated = remember(example, cleanTarget, accent, baseColor) {
        buildAnnotatedString {
            append("\u201c")
            if (cleanTarget.length >= 3) {
                val stem = if (cleanTarget.length >= 5) cleanTarget.dropLast(1) else cleanTarget
                val regex = Regex("\\b${Regex.escape(stem)}[a-zA-Z']*\\b", RegexOption.IGNORE_CASE)
                var lastIdx = 0
                for (match in regex.findAll(example)) {
                    if (match.range.first > lastIdx) {
                        append(example.substring(lastIdx, match.range.first))
                    }
                    withStyle(
                        SpanStyle(
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal
                        )
                    ) {
                        append(match.value)
                    }
                    lastIdx = match.range.last + 1
                }
                if (lastIdx < example.length) {
                    append(example.substring(lastIdx))
                }
            } else {
                append(example)
            }
            append("\u201d")
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontStyle = FontStyle.Italic,
            fontSize = 15.5.sp,
            lineHeight = 23.sp,
            textDirection = TextDirection.Ltr
        ),
        color = baseColor,
        textAlign = textAlign,
        modifier = modifier
    )
}

/**
 * Unified 3-tier permanent local image banner used across Study Cards & Dictionary Detail.
 * - Tier 1: Permanently saved file (filesDir/word_images/<word>.webp) -> badge "Saved Offline ✓"
 * - Tier 2: Bundled 3,586 Essential Book WebP DB (word_images.db) -> badge "Offline Photo"
 * - Tier 3: Optional remote URL -> auto-saved to Tier 1 on first load; collapses cleanly to 0dp offline!
 */
@Composable
fun WordLocalImageBanner(
    wordEntity: WordEntity,
    viewModel: DictionaryViewModel,
    height: Dp = 164.dp,
    showRefreshButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = rememberAppImageLoader(context)
    val refreshVersion by viewModel.localImageRefreshVersion.collectAsStateWithLifecycle()
    val cyclingWord by viewModel.cyclingPhotoWord.collectAsStateWithLifecycle()

    val origin = remember(wordEntity.word, refreshVersion) {
        viewModel.getLocalImageOrigin(wordEntity.word)
    }
    val remoteUrl = remember(wordEntity.imagePrompt) {
        if (wordEntity.imagePrompt.startsWith("http://") || wordEntity.imagePrompt.startsWith("https://")) {
            wordEntity.imagePrompt
        } else {
            ""
        }
    }

    // Do not attempt rendering if there is neither a local image nor an online URL
    if (origin == LocalImageOrigin.NONE && remoteUrl.isBlank()) {
        return
    }

    var failed by remember(wordEntity.word, remoteUrl, refreshVersion) { mutableStateOf(false) }
    if (failed) return

    val requestModel = remember(wordEntity.word, remoteUrl, refreshVersion) {
        WordImageRequest(
            word = wordEntity.word,
            remoteUrl = remoteUrl,
            version = refreshVersion
        )
    }

    val isCycling = cyclingWord.equals(wordEntity.word.trim(), ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .testTag("word_local_image_banner")
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(requestModel)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "Illustration for ${wordEntity.word}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onError = { failed = true },
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            },
            error = {},
            success = {
                SubcomposeAsyncImageContent(
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        )

        // Subtle bottom scrim for badge legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.52f)),
                        startY = 120f
                    )
                )
        )

        val badgeLabel = when (origin) {
            LocalImageOrigin.SAVED_PERMANENT -> "Saved Offline Asset ✓"
            LocalImageOrigin.BUNDLED_OFFLINE -> "Local Photo ✓"
            LocalImageOrigin.NONE -> "Saving Offline..."
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.52f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFFA6F5B9),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        if (showRefreshButton) {
            Surface(
                onClick = { viewModel.cycleNextPhoto(wordEntity) },
                enabled = !isCycling,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(48.dp)
                    .testTag("cycle_local_photo_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCycling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Download another photo and save offline",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Per-Word AI Assistant Modal Bottom Sheet.
 * Gives every word an on-demand helper for:
 *   1) Extra AI notes (Mnemonic, Synonyms, Antonyms, Usage) -> saved offline in Room forever
 *   2) AI Voice (Gemini TTS) for the word and example -> cached locally
 *   3) Download/Change photo -> saved permanently to filesDir/word_images/
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordAiHelperSheet(
    wordEntity: WordEntity,
    viewModel: DictionaryViewModel,
    onDismiss: () -> Unit,
    onSelectRelatedWord: ((String) -> Unit)? = null
) {
    val enrichingWord by viewModel.enrichingWord.collectAsStateWithLifecycle()
    val cyclingWord by viewModel.cyclingPhotoWord.collectAsStateWithLifecycle()
    val refreshVersion by viewModel.localImageRefreshVersion.collectAsStateWithLifecycle()
    val lastAudioSource by viewModel.lastPronunciationSource.collectAsStateWithLifecycle()
    val activeOfflineVoice by viewModel.activeOfflineVoiceLabel.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    val wordKey = wordEntity.word.trim().lowercase()
    val isEnriching = enrichingWord == wordKey
    val isCyclingPhoto = cyclingWord == wordKey
    val imageOrigin = remember(wordKey, refreshVersion) { viewModel.getLocalImageOrigin(wordKey) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("word_ai_helper_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFBF5AF2),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Word AI Assistant",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFBF5AF2)
                        )
                    }
                    Text(
                        text = wordEntity.word.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Ltr
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (wordEntity.arabicMeaning.isNotBlank()) {
                        Text(
                            text = wordEntity.arabicMeaning,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                textDirection = TextDirection.Rtl
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close assistant"
                    )
                }
            }

            // Saved Offline Notes (Mnemonic + Synonyms + Antonyms) if already enriched
            if (wordEntity.mnemonic.isNotBlank() || wordEntity.synonyms.isNotBlank() || wordEntity.antonyms.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF30D158),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Saved Offline Forever",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF30D158)
                            )
                        }

                        if (wordEntity.mnemonic.isNotBlank()) {
                            Text(
                                text = "Memory Aid: ${wordEntity.mnemonic}",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (wordEntity.synonyms.isNotBlank()) {
                            Text(
                                text = "Synonyms",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                wordEntity.synonyms.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { syn ->
                                    Surface(
                                        onClick = { onSelectRelatedWord?.invoke(syn) },
                                        enabled = onSelectRelatedWord != null,
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = syn,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (wordEntity.antonyms.isNotBlank()) {
                            Text(
                                text = "Antonyms",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                wordEntity.antonyms.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { ant ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = ant,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Action: Fetch / Refresh Extra AI Info & Save Offline
            val aiInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.enrichWordWithAi(wordEntity)
                },
                enabled = !isEnriching,
                interactionSource = aiInteraction,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E5CE6),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .applePressScale(aiInteraction, 0.97f)
                    .testTag("ai_helper_enrich_button")
            ) {
                if (isEnriching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Fetching & saving offline...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (wordEntity.mnemonic.isBlank()) {
                            "Get AI Memory Aid, Synonyms & Notes"
                        } else {
                            "Refresh AI Notes & Examples"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Audio Actions: Natural Offline First + Optional AI Voice (Gemini TTS)
            AppleInsetGroupedCard(cornerRadius = 16.dp) {
                HelperActionRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    tint = MaterialTheme.colorScheme.primary,
                    title = "Natural Offline Pronunciation",
                    subtitle = activeOfflineVoice.ifBlank { "Priority 1: Offline Neural UK/US" },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.speak(wordEntity.word)
                    },
                    testTag = "ai_helper_offline_voice"
                )
                if (wordEntity.example.isNotBlank()) {
                    AppleInsetDivider(startIndent = 58.dp)
                    HelperActionRow(
                        icon = Icons.Default.RecordVoiceOver,
                        tint = Color(0xFF30D158),
                        title = "Pronounce Example Sentence",
                        subtitle = wordEntity.example,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.speakSentence(wordEntity.example)
                        },
                        testTag = "ai_helper_example_voice"
                    )
                }
                AppleInsetDivider(startIndent = 58.dp)
                HelperActionRow(
                    icon = Icons.Default.GraphicEq,
                    tint = Color(0xFFBF5AF2),
                    title = "Play with AI Voice (Gemini TTS)",
                    subtitle = "Saves locally after playing · Last: $lastAudioSource",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.speakWithAiVoice(wordEntity.word, wordEntity.word)
                    },
                    testTag = "ai_helper_ai_voice"
                )
                AppleInsetDivider(startIndent = 58.dp)
                HelperActionRow(
                    icon = Icons.Default.Image,
                    tint = Color(0xFFFF9F0A),
                    title = when (imageOrigin) {
                        LocalImageOrigin.SAVED_PERMANENT -> "Change Saved Offline Photo"
                        LocalImageOrigin.BUNDLED_OFFLINE -> "Download Alternative Photo (Saves Forever)"
                        LocalImageOrigin.NONE -> "Download Photo & Save as Local Asset"
                    },
                    subtitle = if (isCyclingPhoto) "Downloading & saving permanently..." else "Stored locally once fetched — never re-downloads",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.cycleNextPhoto(wordEntity)
                    },
                    enabled = !isCyclingPhoto,
                    testTag = "ai_helper_fetch_photo"
                )
            }

            Text(
                text = "Voice Priority: 1) Natural Offline Neural → 2) Online Dictionary → 3) AI Voice",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HelperActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppleIconBadge(icon = icon, tintColor = tint, size = 34.dp, iconSize = 18.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
