package com.example.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.R
import com.example.data.WordEntity
import com.example.ui.*
import com.example.ui.navigation.LocalWindowWidthSizeClass
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusExpressiveType
import com.example.ui.theme.RedeusMotion

@Composable
fun SearchTabContent(
    viewModel: DictionaryViewModel,
    searchQuery: String,
    uiState: SearchUiState,
    innerPadding: PaddingValues
) {
    val widthSizeClass = LocalWindowWidthSizeClass.current
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded

    if (isExpanded) {
        ExpandedSearchLayout(
            viewModel = viewModel,
            searchQuery = searchQuery,
            uiState = uiState,
            innerPadding = innerPadding
        )
    } else {
        CompactMediumSearchLayout(
            viewModel = viewModel,
            searchQuery = searchQuery,
            uiState = uiState,
            innerPadding = innerPadding
        )
    }
}

@Composable
private fun CompactMediumSearchLayout(
    viewModel: DictionaryViewModel,
    searchQuery: String,
    uiState: SearchUiState,
    innerPadding: PaddingValues
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroBannerSection()
            SearchInputSection(
                searchQuery = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClear = { viewModel.clearSearchState() },
                onSubmit = { viewModel.performSearch(searchQuery) }
            )
            SearchResultSection(
                viewModel = viewModel,
                searchQuery = searchQuery,
                uiState = uiState
            )
            SecurityWarningCard()
        }
    }
}

@Composable
private fun ExpandedSearchLayout(
    viewModel: DictionaryViewModel,
    searchQuery: String,
    uiState: SearchUiState,
    innerPadding: PaddingValues
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Pane: Hero, Search Input, Discovery Suggestions & Notice
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroBannerSection()
            SearchInputSection(
                searchQuery = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClear = { viewModel.clearSearchState() },
                onSubmit = { viewModel.performSearch(searchQuery) }
            )
            DiscoverySuggestions { selectedWord ->
                viewModel.performSearch(selectedWord)
            }
            SecurityWarningCard()
        }

        // Right Pane: Active Result / Loading / Error / Prompt
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState is SearchUiState.Idle) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Look up any English word",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Search a word or select a suggestion on the left to view definitions, phonetics, examples, and memory aids.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                SearchResultSection(
                    viewModel = viewModel,
                    searchQuery = searchQuery,
                    uiState = uiState
                )
            }
        }
    }
}

@Composable
private fun SearchInputSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_word_input"),
            label = { Text("Word to define") },
            placeholder = {
                Text(
                    text = "e.g. resilient, serendipity...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSubmit()
                    focusManager.clearFocus()
                }
            ),
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        Button(
            onClick = {
                onSubmit()
                focusManager.clearFocus()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("search_word_button"),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Define word with AI",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SearchResultSection(
    viewModel: DictionaryViewModel,
    searchQuery: String,
    uiState: SearchUiState
) {
    val reducedMotion = LocalReducedMotionEnabled.current

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationMedium)) togetherWith
                fadeOut(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationShort))
        },
        label = "searchState"
    ) { state ->
        when (state) {
            is SearchUiState.Idle -> {
                DiscoverySuggestions { selectedWord ->
                    viewModel.performSearch(selectedWord)
                }
            }
            is SearchUiState.Loading -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Analyzing word with Gemini AI...",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Fetching definition, phonetics, examples, and memory aid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is SearchUiState.Success -> {
                WordDetailsCard(
                    wordEntity = state.word,
                    onSpeak = { viewModel.speak(state.word.word) },
                    onBookmarkToggle = { viewModel.toggleBookmark(state.word) },
                    viewModel = viewModel
                )
            }
            is SearchUiState.Error -> {
                ErrorStateCard(
                    message = state.message,
                    onRetry = { viewModel.performSearch(searchQuery) }
                )
            }
        }
    }
}

@Composable
fun HeroBannerSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Redeus Dictionary",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "AI-powered definitions, pronunciation & spaced repetition",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverySuggestions(onSelectWord: (String) -> Unit) {
    val list = listOf("serendipity", "ephemeral", "benevolent", "resilient", "pragmatic", "aesthetic")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Explore & try a word",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            list.forEach { word ->
                SuggestionChip(
                    onClick = { onSelectWord(word) },
                    label = {
                        Text(
                            text = word.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("seed_word_$word")
                )
            }
        }
    }
}

@Composable
fun TryAnotherPhotoButton(
    word: WordEntity,
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    val imageSource by viewModel.imageSourcePreference.collectAsStateWithLifecycle()
    if (imageSource != "web") return

    val cyclingWord by viewModel.cyclingPhotoWord.collectAsStateWithLifecycle()
    val isCycling = cyclingWord?.equals(word.word, ignoreCase = true) == true

    FilledTonalButton(
        onClick = { viewModel.cycleNextPhoto(word) },
        enabled = !isCycling,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag("try_another_photo_button_${word.word}")
    ) {
        if (isCycling) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .testTag("try_another_photo_spinner"),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fetching...",
                style = MaterialTheme.typography.labelMedium
            )
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Try another photo",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordDetailsCard(
    wordEntity: WordEntity,
    onSpeak: () -> Unit,
    onBookmarkToggle: () -> Unit,
    viewModel: DictionaryViewModel
) {
    var showCardInfoDialog by remember { mutableStateOf(false) }
    val masteryPercentage = if (wordEntity.isBookmarked) 85 else 35
    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
    val displayPhonetic = getPhoneticForAccent(wordEntity.phonetic, selectedAccent)

    if (showCardInfoDialog) {
        WordCardInfoDialog(
            word = wordEntity,
            viewModel = viewModel,
            onDismiss = { showCardInfoDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_details_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Expressive Headword Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            text = wordEntity.word.replaceFirstChar { it.uppercase() },
                            style = RedeusExpressiveType.headword,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (wordEntity.partOfSpeech.isNotBlank()) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(
                                        text = wordEntity.partOfSpeech,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            CardStatusBadge(wordEntity = wordEntity)
                            if (wordEntity.isLeech) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Text(
                                        text = if (wordEntity.lapses > 0) "Leech • ${wordEntity.lapses}" else "Leech",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showCardInfoDialog = true },
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("word_card_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Card review history"
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onBookmarkToggle,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (wordEntity.isBookmarked) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                },
                                contentColor = if (wordEntity.isBookmarked) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("bookmark_toggle")
                        ) {
                            Icon(
                                imageVector = if (wordEntity.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (wordEntity.isBookmarked) "Remove bookmark" else "Bookmark word"
                            )
                        }
                    }
                }

                // Pronunciation Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onSpeak() }
                        .padding(vertical = 2.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onSpeak,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("speak_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Pronounce word"
                        )
                    }
                    Column {
                        Text(
                            text = displayPhonetic.ifBlank { "/${wordEntity.word}/" },
                            style = RedeusExpressiveType.phonetic,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$selectedAccent pronunciation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }

                NeoWordTagsRow(word = wordEntity, viewModel = viewModel)
            }
        }

        // 2. Definition Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Definition",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = wordEntity.definition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 3. Usage Example + Sentence Mining Actions
        if (wordEntity.example.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Usage example",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "\u201c${wordEntity.example}\u201d",
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ExampleSentenceMiningActionsRow(
                        wordEntity = wordEntity,
                        viewModel = viewModel
                    )
                }
            }
        }

        // 4. Visual Illustration / Photo Card
        if (wordEntity.imagePrompt.isNotEmpty()) {
            val context = LocalContext.current
            val imageLoader = rememberAppImageLoader(context)
            val isWebPhoto = wordEntity.imagePrompt.startsWith("http")
            val captionText = if (isWebPhoto) {
                val derived = viewModel.getDerivedQuery(wordEntity.word)
                if (!derived.isNullOrBlank()) "Photo matching \"$derived\"" else "Photo from Pexels for ${wordEntity.word}"
            } else {
                "\"${wordEntity.imagePrompt}\""
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(getAIImageUrl(wordEntity.word, wordEntity.imagePrompt))
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = "Visual illustration of ${wordEntity.word}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    Text(
                                        text = "Loading illustration...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ImageNotSupported,
                                        contentDescription = "Image unavailable",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Image unavailable",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        success = {
                            SubcomposeAsyncImageContent(
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    )

                    // Scrim gradient for readable overlay caption
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f)
                                    ),
                                    startY = 120f
                                )
                            )
                    )

                    TryAnotherPhotoButton(
                        word = wordEntity,
                        viewModel = viewModel,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = if (isWebPhoto) "Pexels photo" else "AI visualization",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = captionText,
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Synonyms & Antonyms
        if (wordEntity.synonyms.isNotBlank() || wordEntity.antonyms.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (wordEntity.synonyms.isNotBlank()) {
                        Text(
                            text = "Synonyms",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wordEntity.synonyms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { syn ->
                                AssistChip(
                                    onClick = { viewModel.performSearch(syn) },
                                    label = { Text(syn, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                    }

                    if (wordEntity.antonyms.isNotBlank()) {
                        if (wordEntity.synonyms.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        Text(
                            text = "Antonyms",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wordEntity.antonyms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { ant ->
                                AssistChip(
                                    onClick = { viewModel.performSearch(ant) },
                                    label = { Text(ant, style = MaterialTheme.typography.labelMedium) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Memory Aid (Mnemonic)
        if (wordEntity.mnemonic.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Memory aid",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = wordEntity.mnemonic,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 7. Mastery Summary Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$masteryPercentage%",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Mastery status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (wordEntity.isBookmarked) "Saved in your vocabulary deck" else "Searched word (not bookmarked yet)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LinearProgressIndicator(
                        progress = { masteryPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraSmall)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    onRetry: () -> Unit
) {
    val isOffline = remember(message) {
        val lower = message.lowercase()
        lower.contains("offline") ||
            lower.contains("network") ||
            lower.contains("internet") ||
            lower.contains("unable to resolve host") ||
            lower.contains("timeout")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_state_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = if (isOffline) "You appear to be offline" else "Couldn't complete request",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try again")
            }
        }
    }
}

@Composable
fun SecurityWarningCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("security_warning_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Security notice",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Debug builds may include local API keys from .env. Do not distribute APKs containing real API keys.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
