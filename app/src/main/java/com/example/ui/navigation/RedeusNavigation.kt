package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R

val LocalWindowWidthSizeClass = staticCompositionLocalOf { WindowWidthSizeClass.Compact }

enum class RedeusDestination(
    val index: Int,
    val label: String,
    val contentDescription: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    SEARCH(
        index = 0,
        label = "Search",
        contentDescription = "Word Lookup",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    LIBRARY(
        index = 1,
        label = "Library",
        contentDescription = "Personal Deck",
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder
    ),
    READING(
        index = 2,
        label = "Read",
        contentDescription = "Daily Reading",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook
    ),
    CARDS(
        index = 3,
        label = "Cards",
        contentDescription = "Flashcards",
        selectedIcon = Icons.Filled.Style,
        unselectedIcon = Icons.Outlined.Style
    ),
    REVIEW(
        index = 4,
        label = "Review",
        contentDescription = "Smart Review",
        selectedIcon = Icons.Filled.RateReview,
        unselectedIcon = Icons.Outlined.RateReview
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeusAppShell(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    cardsDueTodayCount: Int,
    snackbarHostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val widthSizeClass = LocalWindowWidthSizeClass.current
    val useNavigationRail = widthSizeClass != WindowWidthSizeClass.Compact
    val destinations = RedeusDestination.entries

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("app_snackbar_host")
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary
                )
            }
        },
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                        contentDescription = "Redeus Logo",
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Redeus Dictionary",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = destinations.getOrElse(selectedTab) { RedeusDestination.SEARCH }.contentDescription,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            }
        },
        bottomBar = {
            if (!useNavigationRail) {
                Column {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp
                    ) {
                        destinations.forEach { destination ->
                            val isSelected = selectedTab == destination.index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { onSelectTab(destination.index) },
                                icon = {
                                    DestinationIconWithBadge(
                                        destination = destination,
                                        isSelected = isSelected,
                                        cardsDueTodayCount = cardsDueTodayCount
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible
                                    )
                                },
                                alwaysShowLabel = true
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (useNavigationRail) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    destinations.forEach { destination ->
                        val isSelected = selectedTab == destination.index
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { onSelectTab(destination.index) },
                            icon = {
                                DestinationIconWithBadge(
                                    destination = destination,
                                    isSelected = isSelected,
                                    cardsDueTodayCount = cardsDueTodayCount
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    content(PaddingValues(0.dp))
                }
            }
        } else {
            content(innerPadding)
        }
    }
}

@Composable
private fun DestinationIconWithBadge(
    destination: RedeusDestination,
    isSelected: Boolean,
    cardsDueTodayCount: Int
) {
    val icon = if (isSelected) destination.selectedIcon else destination.unselectedIcon
    if (destination == RedeusDestination.REVIEW && cardsDueTodayCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.semantics {
                        contentDescription = "$cardsDueTodayCount cards due for review"
                    }
                ) {
                    Text(
                        text = if (cardsDueTodayCount > 99) "99+" else cardsDueTodayCount.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = destination.contentDescription
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = destination.contentDescription
        )
    }
}
