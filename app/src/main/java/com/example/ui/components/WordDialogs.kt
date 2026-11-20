package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ReviewLogEntity
import com.example.data.WordEntity
import com.example.ui.DictionaryViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditorDialog(
    word: WordEntity,
    viewModel: DictionaryViewModel,
    onDismiss: () -> Unit
) {
    var newTagInput by remember { mutableStateOf("") }
    val currentTags = remember(word.tags) {
        word.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    val suggestedTags = listOf("IELTS", "TOEFL", "Business", "Academic", "Daily", "Idioms")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .testTag("tag_editor_dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Decks & tags",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Organize '${word.word}' into study decks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("close_tag_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                // Current Tags
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Assigned tags (${currentTags.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentTags.isEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No tags assigned yet. Tap a suggested deck or type a custom tag below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentTags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.removeTagFromWord(word.word, tag) },
                                    label = { Text("#$tag") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove $tag",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Custom Tag Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tag_input_field"),
                        label = { Text("Add custom tag") },
                        placeholder = { Text("e.g. GRE, Travel, Medical") },
                        singleLine = true
                    )
                    FilledTonalButton(
                        onClick = {
                            if (newTagInput.isNotBlank()) {
                                viewModel.addTagToWord(word.word, newTagInput.trim())
                                newTagInput = ""
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("add_tag_button")
                    ) {
                        Text("Add")
                    }
                }

                // Suggested Preset Decks
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Suggested study decks",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedTags.forEach { suggested ->
                            val isAlreadyAssigned = currentTags.any { it.equals(suggested, ignoreCase = true) }
                            FilterChip(
                                selected = isAlreadyAssigned,
                                onClick = {
                                    if (isAlreadyAssigned) {
                                        viewModel.removeTagFromWord(word.word, suggested)
                                    } else {
                                        viewModel.addTagToWord(word.word, suggested)
                                    }
                                },
                                label = { Text("#$suggested") },
                                leadingIcon = if (isAlreadyAssigned) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("suggested_tag_${suggested.lowercase()}")
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("tag_dialog_done_button")
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordTagsRow(
    word: WordEntity,
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    var showTagDialog by remember { mutableStateOf(false) }
    val tags = remember(word.tags) {
        word.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    if (showTagDialog) {
        TagEditorDialog(
            word = word,
            viewModel = viewModel,
            onDismiss = { showTagDialog = false }
        )
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.take(3).forEach { tag ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = "#$tag",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        if (tags.size > 3) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = "+${tags.size - 3}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        AssistChip(
            onClick = { showTagDialog = true },
            label = {
                Text(
                    text = if (tags.isEmpty()) "Add tag" else "Edit tags",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = "Edit tags",
                    modifier = Modifier.size(14.dp)
                )
            },
            modifier = Modifier.testTag("open_tag_editor_${word.word}")
        )
    }
}

@Composable
fun WordCardInfoDialog(
    word: WordEntity,
    viewModel: DictionaryViewModel,
    onDismiss: () -> Unit
) {
    val reviewLogs by viewModel.getReviewLogsForWord(word.word)
        .collectAsStateWithLifecycle(initialValue = emptyList<ReviewLogEntity>())
    val srsAlgorithm by viewModel.srsAlgorithm.collectAsStateWithLifecycle()
    val isFsrs = srsAlgorithm.equals("fsrs", ignoreCase = true) || word.fsrsStability != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
                .heightIn(max = 680.dp)
                .testTag("word_card_info_dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = word.word.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CardStatusBadge(wordEntity = word)
                            if (word.isLeech) {
                                LeechBadge(lapses = word.lapses)
                            }
                        }
                        Text(
                            text = "Card info & review history",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("close_card_info_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close card info")
                    }
                }

                // Stats Summary Grid
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPair("Repetitions", "${word.repetitions}")
                            StatPair("Distinct days", "${word.distinctReviewDays} d")
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPair("Interval", "${word.interval}d")
                            StatPair(
                                label = "Lapses",
                                value = "${word.lapses}",
                                highlightError = word.lapses > 0
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isFsrs && word.fsrsStability != null) {
                                StatPair(
                                    "FSRS S / D",
                                    "${String.format(java.util.Locale.US, "%.1fd", word.fsrsStability ?: 0.0)} / ${String.format(java.util.Locale.US, "%.1f", word.fsrsDifficulty ?: 0.0)}"
                                )
                                StatPair("FSRS state", word.fsrsState ?: "New")
                            } else {
                                StatPair("Ease factor", String.format(java.util.Locale.US, "%.2f", word.easeFactor))
                                StatPair("Algorithm", "SM-2 (Classic)")
                            }
                        }
                    }
                }

                // Timeline Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Review timeline (${reviewLogs.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Most recent first",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Timeline List
                if (reviewLogs.isEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No reviews recorded yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Review this card in the Review or Cards tab to track your progress.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val dateFormatter = remember {
                        java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm", java.util.Locale.US)
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reviewLogs) { log ->
                            val formattedDate = remember(log.timestamp) {
                                try {
                                    java.time.Instant.ofEpochMilli(log.timestamp)
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .format(dateFormatter)
                                } catch (_: Exception) {
                                    "Date: ${log.timestamp}"
                                }
                            }
                            val (badgeBg, badgeFg, badgeLabel) = when (log.rating.lowercase()) {
                                "again" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Again")
                                "hard" -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "Hard")
                                "good" -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "Good")
                                "easy" -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "Easy")
                                else -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface, log.rating)
                            }

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = badgeBg,
                                        contentColor = badgeFg
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
private fun StatPair(
    label: String,
    value: String,
    highlightError: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlightError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
