package com.example.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.domain.BookProgressSummary
import com.example.domain.CourseProgressCalculator
import com.example.domain.UnitProgressStatus
import com.example.ui.DictionaryViewModel
import com.example.ui.components.*
import com.example.ui.navigation.LocalWindowWidthSizeClass
import com.example.ui.screens.today.bookAccentColor

private enum class CourseCatalogFilter(val label: String) {
    ALL("All"),
    IELTS("IELTS"),
    ESSENTIAL("6 Books"),
    TOPICS("Topics")
}

@Composable
fun CoursesExplorerContent(
    viewModel: DictionaryViewModel,
    bookmarkedWords: List<WordEntity>,
    onNavigateToSearch: () -> Unit,
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val books by viewModel.offlineBooks.collectAsStateWithLifecycle()
    val topics by viewModel.offlineTopics.collectAsStateWithLifecycle()
    val bookWordLowersMap by viewModel.bookWordLowersMap.collectAsStateWithLifecycle()
    val topicWordLowersMap by viewModel.topicWordLowersMap.collectAsStateWithLifecycle()
    val selectedBook by viewModel.selectedCourseBook.collectAsStateWithLifecycle()
    val courseUnits by viewModel.courseUnits.collectAsStateWithLifecycle()
    val unitWordLowersMap by viewModel.unitWordLowersMap.collectAsStateWithLifecycle()
    val selectedUnit by viewModel.selectedCourseUnit.collectAsStateWithLifecycle()
    val selectedTopic by viewModel.selectedCourseTopic.collectAsStateWithLifecycle()
    val courseWords by viewModel.courseWords.collectAsStateWithLifecycle()
    val courseSearchQuery by viewModel.courseSearchQuery.collectAsStateWithLifecycle()
    val courseSearchResults by viewModel.courseSearchResults.collectAsStateWithLifecycle()
    val isAdding by viewModel.isAddingCourseWords.collectAsStateWithLifecycle()
    val activeCourseBookNum by viewModel.activeCourseBookNum.collectAsStateWithLifecycle()
    val activeCourseUnitWordMap by viewModel.activeCourseUnitWordMap.collectAsStateWithLifecycle()
    val newCardsPerDay by viewModel.newCardsPerDay.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val widthSizeClass = LocalWindowWidthSizeClass.current

    var catalogFilter by rememberSaveable { mutableStateOf(CourseCatalogFilter.ALL) }
    var aiHelperWordKey by remember { mutableStateOf<String?>(null) }
    var aiHelperFallbackEntity by remember { mutableStateOf<WordEntity?>(null) }

    // System Back pops Words -> Units -> Books cleanly instead of exiting the app
    BackHandler(enabled = selectedTopic != null || selectedUnit != null || selectedBook != null) {
        when {
            selectedTopic != null -> viewModel.selectCourseTopic(null)
            selectedUnit != null -> viewModel.selectCourseUnit(null)
            selectedBook != null -> viewModel.selectCourseBook(null)
        }
    }

    val deckWordMap = remember(bookmarkedWords) {
        bookmarkedWords.associateBy { it.word.trim().lowercase() }
    }
    val masteredWordSet = remember(bookmarkedWords) {
        bookmarkedWords
            .filter { CourseProgressCalculator.isMastered(it) }
            .map { it.word.trim().lowercase() }
            .toSet()
    }

    val activeBook = remember(books, activeCourseBookNum) {
        books.firstOrNull { it.bookNum == activeCourseBookNum }
    }
    val activeBookProgress = remember(activeBook, bookWordLowersMap, deckWordMap, activeCourseUnitWordMap) {
        activeBook?.let { b ->
            CourseProgressCalculator.calculateBookProgress(
                book = b,
                bookWordLowers = bookWordLowersMap[b.bookNum].orEmpty(),
                deckWordMap = deckWordMap,
                unitWordLowersMap = activeCourseUnitWordMap
            )
        }
    }

    LaunchedEffect(Unit) {
        if (books.isEmpty()) {
            viewModel.loadOfflineCatalog()
        }
    }
    LaunchedEffect(activeCourseBookNum) {
        if (activeCourseBookNum > 0) {
            viewModel.loadActiveCourseUnitMap(activeCourseBookNum)
        }
    }

    fun openAiHelperForCourseWord(item: OfflineCurriculumWord) {
        val key = item.word.trim().lowercase()
        aiHelperWordKey = key
        aiHelperFallbackEntity = deckWordMap[key] ?: WordEntity(
            word = item.word,
            phonetic = item.ipa,
            partOfSpeech = item.pos,
            definition = item.definitionEn,
            arabicMeaning = item.translationAr,
            example = item.exampleEn,
            tags = item.bookShortTag
        )
    }

    val activeAiHelperWord = aiHelperWordKey?.let { key ->
        deckWordMap[key] ?: aiHelperFallbackEntity
    }
    if (activeAiHelperWord != null) {
        WordAiHelperSheet(
            wordEntity = activeAiHelperWord,
            viewModel = viewModel,
            onDismiss = {
                aiHelperWordKey = null
                aiHelperFallbackEntity = null
            },
            onSelectRelatedWord = { related ->
                aiHelperWordKey = null
                aiHelperFallbackEntity = null
                viewModel.openWordFromSearch(related)
                onNavigateToSearch()
            }
        )
    }

    val gridColumns = when (widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 4
        WindowWidthSizeClass.Medium -> 3
        else -> 2
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            selectedTopic != null -> {
                val topic = selectedTopic!!
                val extraTags = listOf("Topic: ${topic.nameEn}")
                CourseWordsListPane(
                    title = "${topic.nameEn} · ${topic.nameAr}",
                    subtitle = "${courseWords.size} words · Essential 6 Books",
                    words = courseWords,
                    deckWordMap = deckWordMap,
                    masteredWordSet = masteredWordSet,
                    isAdding = isAdding,
                    primaryStudyLabel = "Study Topic Now",
                    onBack = { viewModel.selectCourseTopic(null) },
                    onStudyUnitNow = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.studyTopicNow(topic = topic, topicWords = courseWords)
                    },
                    onAddBatch = { batchSize ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.addNextBatchToDeck(courseWords, batchSize, extraTags, topic.nameEn)
                    },
                    onAddAll = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.addAllWordsToDeck(courseWords, extraTags, topic.nameEn)
                    },
                    onReviewDeck = {
                        viewModel.startUnifiedStudySession(
                            deckFilter = "Topic: ${topic.nameEn}",
                            deckTitle = topic.nameEn
                        )
                    },
                    onWordClick = { item ->
                        viewModel.openCourseWord(item, extraTags) { onNavigateToSearch() }
                    },
                    onWordSpeak = { item -> viewModel.speak(item.word) },
                    onWordAiHelper = { item -> openAiHelperForCourseWord(item) },
                    onWordToggleDeck = { item, inDeck ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleCourseWordInDeck(item, inDeck, extraTags)
                    }
                )
            }

            selectedBook != null && selectedUnit != null -> {
                val book = selectedBook!!
                val unit = selectedUnit!!
                val unitDeckTag = OfflineDictionaryDatabase.buildDeckTagForUnit(book.shortTag, unit.unitNum)
                CourseWordsListPane(
                    title = unit.title,
                    subtitle = "${book.shortTag} (${book.cefr}) · ${courseWords.size} words",
                    words = courseWords,
                    deckWordMap = deckWordMap,
                    masteredWordSet = masteredWordSet,
                    isAdding = isAdding,
                    primaryStudyLabel = "Study Unit Now",
                    onBack = { viewModel.selectCourseUnit(null) },
                    onStudyUnitNow = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.studyUnitNow(book = book, unit = unit, unitWords = courseWords)
                    },
                    onAddBatch = { batchSize ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.addNextBatchToDeck(courseWords, batchSize, label = "${book.shortTag} U${unit.unitNum}")
                    },
                    onAddAll = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.addAllWordsToDeck(courseWords, label = "${book.shortTag} U${unit.unitNum}")
                    },
                    onReviewDeck = {
                        viewModel.startUnifiedStudySession(
                            deckFilter = unitDeckTag,
                            deckTitle = "${book.shortTag} · ${unit.title}"
                        )
                    },
                    onWordClick = { item ->
                        viewModel.openCourseWord(item) { onNavigateToSearch() }
                    },
                    onWordSpeak = { item -> viewModel.speak(item.word) },
                    onWordAiHelper = { item -> openAiHelperForCourseWord(item) },
                    onWordToggleDeck = { item, inDeck ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleCourseWordInDeck(item, inDeck)
                    }
                )
            }

            selectedBook != null -> {
                val book = selectedBook!!
                val bookWordsSet = bookWordLowersMap[book.bookNum].orEmpty()
                val inDeckCount = bookWordsSet.count { it in deckWordMap }
                val masteredCount = bookWordsSet.count { it in masteredWordSet }
                val isActiveCourse = activeCourseBookNum == book.bookNum

                CourseBookUnitsPane(
                    book = book,
                    units = courseUnits,
                    unitWordLowersMap = unitWordLowersMap,
                    deckWordMap = deckWordMap,
                    bookInDeckCount = inDeckCount,
                    bookMasteredCount = masteredCount,
                    isActiveCourse = isActiveCourse,
                    onBack = { viewModel.selectCourseBook(null) },
                    onSelectUnit = { viewModel.selectCourseUnit(it) },
                    onSetActiveCourse = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setActiveCourseBookNum(book.bookNum)
                        viewModel.showSnackbar("${book.shortTag} set as Active Course for daily Auto-Feed")
                    },
                    onReviewBook = {
                        viewModel.startUnifiedStudySession(
                            deckFilter = book.shortTag,
                            deckTitle = book.title
                        )
                    }
                )
            }

            else -> {
                // Spotlight-style inline search field (48dp comfortable touch height)
                AppleInlineSearchField(
                    query = courseSearchQuery,
                    placeholder = "Search 9,591 Book & IELTS words...",
                    onQueryChange = { q ->
                        val cat = when (catalogFilter) {
                            CourseCatalogFilter.ESSENTIAL -> "essential"
                            CourseCatalogFilter.IELTS -> "ielts"
                            else -> null
                        }
                        viewModel.setCourseSearchQuery(q, cat)
                    },
                    onClear = { viewModel.setCourseSearchQuery("") },
                    testTag = "courses_search_input"
                )

                // Apple segmented filter pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CourseCatalogFilter.entries.forEach { filter ->
                        val selected = catalogFilter == filter
                        val interaction = remember { MutableInteractionSource() }
                        Surface(
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .applePressScale(interaction, 0.96f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null
                                ) {
                                    catalogFilter = filter
                                    if (courseSearchQuery.trim().length >= 2) {
                                        val cat = when (filter) {
                                            CourseCatalogFilter.ESSENTIAL -> "essential"
                                            CourseCatalogFilter.IELTS -> "ielts"
                                            else -> null
                                        }
                                        viewModel.setCourseSearchQuery(courseSearchQuery, cat)
                                    }
                                }
                                .testTag("course_filter_${filter.name.lowercase()}"),
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                if (courseSearchQuery.trim().length >= 2) {
                    if (courseSearchResults.isEmpty()) {
                        AppleInsetGroupedCard {
                            Text(
                                text = "No matches for \"$courseSearchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            item(key = "search_results_group") {
                                AppleInsetGroupedCard {
                                    courseSearchResults.forEachIndexed { idx, item ->
                                        val inDeck = deckWordMap.containsKey(item.word.lowercase())
                                        val isMastered = item.word.lowercase() in masteredWordSet
                                        CurriculumWordInsetRow(
                                            item = item,
                                            inDeck = inDeck,
                                            isMastered = isMastered,
                                            showSourceBadge = true,
                                            onClick = { viewModel.openCourseWord(item) { onNavigateToSearch() } },
                                            onSpeak = { viewModel.speak(item.word) },
                                            onAiHelper = { openAiHelperForCourseWord(item) },
                                            onToggleDeck = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.toggleCourseWordInDeck(item, inDeck)
                                            }
                                        )
                                        if (idx < courseSearchResults.lastIndex) {
                                            AppleInsetDivider(startIndent = 16.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (catalogFilter == CourseCatalogFilter.TOPICS) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item(key = "topics_header") {
                            AppleSectionHeader(title = "26 Life Topics · Essential 6 Books")
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        item(key = "topics_group") {
                            AppleInsetGroupedCard {
                                topics.forEachIndexed { idx, topic ->
                                    val wordsSet = topicWordLowersMap[topic.id].orEmpty()
                                    val inDeck = wordsSet.count { it in deckWordMap }
                                    val mastered = wordsSet.count { it in masteredWordSet }
                                    TopicInsetRow(
                                        topic = topic,
                                        inDeckCount = inDeck,
                                        masteredCount = mastered,
                                        onClick = { viewModel.selectCourseTopic(topic) }
                                    )
                                    if (idx < topics.lastIndex) {
                                        AppleInsetDivider(startIndent = 60.dp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val ieltsBooks = books.filter { it.category == "ielts" }
                    val essentialBooks = books.filter { it.category == "essential" }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Active Course Hero Banner
                        if (activeBook != null && activeBookProgress != null) {
                            item(key = "active_course_hero") {
                                ActiveCourseHeroBanner(
                                    summary = activeBookProgress,
                                    isBusy = isAdding,
                                    newCardsPerDay = newCardsPerDay,
                                    onStudyNextBatch = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.autoFeedActiveCourseWords(count = newCardsPerDay, launchSession = true)
                                    },
                                    onOpenBook = { viewModel.selectCourseBook(activeBook) }
                                )
                            }
                        }

                        if (catalogFilter == CourseCatalogFilter.ALL || catalogFilter == CourseCatalogFilter.IELTS) {
                            item(key = "ielts_grid_section") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    AppleSectionHeader(
                                        title = "IELTS Preparation · ${ieltsBooks.sumOf { it.wordCount }} Words"
                                    )
                                    BookCoverGrid(
                                        books = ieltsBooks,
                                        columns = gridColumns,
                                        activeCourseBookNum = activeCourseBookNum,
                                        bookWordLowersMap = bookWordLowersMap,
                                        deckWordMap = deckWordMap,
                                        masteredWordSet = masteredWordSet,
                                        onBookClick = { viewModel.selectCourseBook(it) }
                                    )
                                }
                            }
                        }

                        if (catalogFilter == CourseCatalogFilter.ALL || catalogFilter == CourseCatalogFilter.ESSENTIAL) {
                            item(key = "essential_grid_section") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    AppleSectionHeader(
                                        title = "6 Essential Books · 3,600 Words · Local Photos"
                                    )
                                    BookCoverGrid(
                                        books = essentialBooks,
                                        columns = gridColumns,
                                        activeCourseBookNum = activeCourseBookNum,
                                        bookWordLowersMap = bookWordLowersMap,
                                        deckWordMap = deckWordMap,
                                        masteredWordSet = masteredWordSet,
                                        onBookClick = { viewModel.selectCourseBook(it) }
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

@Composable
private fun ActiveCourseHeroBanner(
    summary: BookProgressSummary,
    isBusy: Boolean,
    newCardsPerDay: Int,
    onStudyNextBatch: () -> Unit,
    onOpenBook: () -> Unit
) {
    val accent = bookAccentColor(summary.book.bookNum)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_course_hero_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF30D158))
                        )
                        Text(
                            text = "ACTIVE COURSE · AUTO-FEED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = accent
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accent.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = summary.book.cefr,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = summary.book.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Next: Unit ${summary.currentUnitNum} · ${summary.inDeckCount}/${summary.totalWords} in Deck · ${summary.masteredCount} mastered",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { (summary.inDeckPercent / 100f).coerceIn(0f, 1f) },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.18f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val studyInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = onStudyNextBatch,
                        enabled = !isBusy,
                        interactionSource = studyInteraction,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .applePressScale(studyInteraction, 0.97f)
                            .testTag("active_course_study_cta")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Study Unit ${summary.currentUnitNum}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenBook,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(0.75f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Units",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCoverGrid(
    books: List<OfflineBook>,
    columns: Int,
    activeCourseBookNum: Int,
    bookWordLowersMap: Map<Int, Set<String>>,
    deckWordMap: Map<String, WordEntity>,
    masteredWordSet: Set<String>,
    onBookClick: (OfflineBook) -> Unit
) {
    val rows = remember(books, columns) { books.chunked(columns.coerceAtLeast(1)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowBooks.forEach { book ->
                    val wordsSet = bookWordLowersMap[book.bookNum].orEmpty()
                    val inDeck = wordsSet.count { it in deckWordMap }
                    val mastered = wordsSet.count { it in masteredWordSet }
                    BookCoverGridCard(
                        book = book,
                        inDeckCount = inDeck,
                        masteredCount = mastered,
                        isActive = book.bookNum == activeCourseBookNum,
                        onClick = { onBookClick(book) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat((columns - rowBooks.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BookCoverGridCard(
    book: OfflineBook,
    inDeckCount: Int,
    masteredCount: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = bookAccentColor(book.bookNum)
    val icon = when (book.bookNum) {
        7 -> Icons.Default.WorkspacePremium
        8 -> Icons.Default.Headphones
        9 -> Icons.AutoMirrored.Filled.LibraryBooks
        else -> Icons.AutoMirrored.Filled.MenuBook
    }
    val progress = if (book.wordCount > 0) (inDeckCount.toFloat() / book.wordCount.toFloat()).coerceIn(0f, 1f) else 0f
    val pct = (progress * 100f).toInt()
    val interaction = remember { MutableInteractionSource() }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 0.75.dp,
            color = if (isActive) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .applePressScale(interaction, 0.97f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .testTag("course_book_card_${book.bookNum}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Gradient visual book cover header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(accent.copy(alpha = 0.90f), accent.copy(alpha = 0.55f))
                        )
                    )
                    .padding(12.dp)
            ) {
                // Top-left CEFR chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.28f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = book.cefr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Top-right circular progress ring
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        strokeWidth = 3.dp,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = Color.White
                    )
                }

                // Bottom-left icon + optional Active pill
                Row(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(18.dp)
                    )
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF30D158)
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Card details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${book.unitCount} units · ${book.wordCount} words",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (inDeckCount > 0) "$inDeckCount in Deck · $masteredCount ✓" else "Tap to start",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (inDeckCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (inDeckCount > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AppleInlineSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicInsetRow(
    topic: OfflineTopic,
    inDeckCount: Int,
    masteredCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("course_topic_card_${topic.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppleIconBadge(
            icon = Icons.Default.Category,
            tintColor = Color(0xFF30D158),
            size = 36.dp,
            iconSize = 18.dp
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = topic.nameEn,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${topic.nameAr} · ${topic.wordCount} words" +
                    if (inDeckCount > 0) " · $inDeckCount in Deck ($masteredCount mastered)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open topic",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CourseBookUnitsPane(
    book: OfflineBook,
    units: List<OfflineUnit>,
    unitWordLowersMap: Map<Int, Set<String>>,
    deckWordMap: Map<String, WordEntity>,
    bookInDeckCount: Int,
    bookMasteredCount: Int,
    isActiveCourse: Boolean,
    onBack: () -> Unit,
    onSelectUnit: (OfflineUnit) -> Unit,
    onSetActiveCourse: () -> Unit,
    onReviewBook: () -> Unit
) {
    val accent = bookAccentColor(book.bookNum)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("courses_back_to_books")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to books",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${book.cefr} · $bookInDeckCount/${book.wordCount} in Deck · $bookMasteredCount mastered",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Active Course + Study Book action row (48dp touch targets)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onSetActiveCourse,
                shape = RoundedCornerShape(14.dp),
                color = if (isActiveCourse) Color(0xFF30D158).copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    1.dp,
                    if (isActiveCourse) Color(0xFF30D158).copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("set_active_course_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isActiveCourse) Icons.Default.CheckCircle else Icons.Default.StarOutline,
                        contentDescription = null,
                        tint = if (isActiveCourse) Color(0xFF30D158) else accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActiveCourse) "Active Course ✓" else "Set Active Course",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isActiveCourse) Color(0xFF30D158) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (bookInDeckCount > 0) {
                Button(
                    onClick = onReviewBook,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("study_book_now_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Study ($bookInDeckCount)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item(key = "units_group") {
                AppleInsetGroupedCard {
                    units.forEachIndexed { idx, unit ->
                        val unitWords = unitWordLowersMap[unit.unitNum].orEmpty()
                        val unitSummary = CourseProgressCalculator.calculateUnitProgress(
                            unit = unit,
                            unitWordLowers = unitWords,
                            deckWordMap = deckWordMap
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .clickable { onSelectUnit(unit) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("course_unit_card_${unit.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Status indicator: ✓ Completed / ◐ In Progress / ○ Not Started
                            when (unitSummary.status) {
                                UnitProgressStatus.COMPLETED -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF30D158),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Unit completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                                UnitProgressStatus.IN_PROGRESS -> {
                                    Box(
                                        modifier = Modifier.size(34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { unitSummary.progressFraction },
                                            strokeWidth = 3.dp,
                                            color = accent,
                                            trackColor = accent.copy(alpha = 0.2f),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Text(
                                            text = "${unit.unitNum}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = accent
                                        )
                                    }
                                }
                                UnitProgressStatus.NOT_STARTED -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${unit.unitNum}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFeatureSettings = "tnum"
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = unit.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${unit.wordCount} words" +
                                        if (unitSummary.inDeckCount > 0) " · ${unitSummary.inDeckCount} in Deck · ${unitSummary.masteredCount} mastered" else "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (unitSummary.inDeckCount > 0) {
                                    LinearProgressIndicator(
                                        progress = { unitSummary.progressFraction },
                                        color = if (unitSummary.status == UnitProgressStatus.COMPLETED) Color(0xFF30D158) else accent,
                                        trackColor = accent.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open unit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (idx < units.lastIndex) {
                            AppleInsetDivider(startIndent = 60.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseWordsListPane(
    title: String,
    subtitle: String,
    words: List<OfflineCurriculumWord>,
    deckWordMap: Map<String, WordEntity>,
    masteredWordSet: Set<String>,
    isAdding: Boolean,
    primaryStudyLabel: String,
    onBack: () -> Unit,
    onStudyUnitNow: () -> Unit,
    onAddBatch: (Int) -> Unit,
    onAddAll: () -> Unit,
    onReviewDeck: () -> Unit,
    onWordClick: (OfflineCurriculumWord) -> Unit,
    onWordSpeak: (OfflineCurriculumWord) -> Unit,
    onWordAiHelper: (OfflineCurriculumWord) -> Unit,
    onWordToggleDeck: (OfflineCurriculumWord, Boolean) -> Unit
) {
    val inDeckCount = remember(words, deckWordMap) {
        words.count { deckWordMap.containsKey(it.word.lowercase()) }
    }
    val remainingCount = (words.size - inDeckCount).coerceAtLeast(0)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("courses_back_to_units")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$subtitle · $inDeckCount/${words.size} in Deck",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Primary 1-tap "Study Unit Now" CTA (52dp full-width hero action)
        if (words.isNotEmpty()) {
            val studyNowInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onStudyUnitNow,
                enabled = !isAdding,
                interactionSource = studyNowInteraction,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .applePressScale(studyNowInteraction, 0.98f)
                    .testTag("study_unit_now_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = primaryStudyLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Secondary batch add / deck review pills (all 48dp height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (remainingCount > 0) {
                val add10Interaction = remember { MutableInteractionSource() }
                FilledTonalButton(
                    onClick = { onAddBatch(10) },
                    enabled = !isAdding,
                    interactionSource = add10Interaction,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .applePressScale(add10Interaction, 0.96f)
                        .testTag("add_next_10_cards_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+10 to Deck", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                }
                if (remainingCount > 10) {
                    FilledTonalButton(
                        onClick = { onAddBatch(20) },
                        enabled = !isAdding,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("+20", style = MaterialTheme.typography.labelLarge)
                    }
                }
                OutlinedButton(
                    onClick = onAddAll,
                    enabled = !isAdding,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_all_unit_cards_button")
                ) {
                    Text("Add All ($remainingCount)", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (inDeckCount > 0) {
                val reviewInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = onReviewDeck,
                    interactionSource = reviewInteraction,
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF30D158).copy(alpha = 0.16f),
                    contentColor = Color(0xFF30D158),
                    border = BorderStroke(0.75.dp, Color(0xFF30D158).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .height(48.dp)
                        .applePressScale(reviewInteraction, 0.96f)
                        .testTag("review_unit_deck_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text(
                            text = "Review Deck ($inDeckCount)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item(key = "words_group") {
                AppleInsetGroupedCard {
                    words.forEachIndexed { idx, item ->
                        val inDeck = deckWordMap.containsKey(item.word.lowercase())
                        val isMastered = item.word.lowercase() in masteredWordSet
                        CurriculumWordInsetRow(
                            item = item,
                            inDeck = inDeck,
                            isMastered = isMastered,
                            showSourceBadge = false,
                            onClick = { onWordClick(item) },
                            onSpeak = { onWordSpeak(item) },
                            onAiHelper = { onWordAiHelper(item) },
                            onToggleDeck = { onWordToggleDeck(item, inDeck) }
                        )
                        if (idx < words.lastIndex) {
                            AppleInsetDivider(startIndent = 16.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurriculumWordInsetRow(
    item: OfflineCurriculumWord,
    inDeck: Boolean,
    isMastered: Boolean,
    showSourceBadge: Boolean,
    onClick: () -> Unit,
    onSpeak: () -> Unit,
    onAiHelper: () -> Unit,
    onToggleDeck: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 12.dp)
            .testTag("course_word_row_${item.word.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: Headword + metadata + 48dp Speak & Bookmark buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.word.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.pos.isNotBlank()) {
                    Text(
                        text = item.pos,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (isMastered) {
                    Text(
                        text = "· ✓",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF30D158)
                    )
                } else if (showSourceBadge && item.bookShortTag.isNotBlank()) {
                    Text(
                        text = "· ${item.bookShortTag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            // 48dp Pronounce button
            IconButton(
                onClick = onSpeak,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("course_word_speak_${item.word.lowercase()}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Pronounce ${item.word}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 48dp Bookmark / Add to Deck toggle
            IconButton(
                onClick = onToggleDeck,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("course_word_bookmark_${item.word.lowercase()}")
            ) {
                Icon(
                    imageVector = if (inDeck) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (inDeck) "Remove from Deck" else "Add to FSRS Deck",
                    tint = if (inDeck) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        // Row 2: Full-width Arabic meaning + English definition + compact AI Helper pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (item.translationAr.isNotBlank()) {
                    Text(
                        text = item.translationAr,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (item.definitionEn.isNotBlank()) {
                    Text(
                        text = item.definitionEn,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                onClick = onAiHelper,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f)),
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag("course_word_ai_helper_${item.word.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Helper for ${item.word}",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}
