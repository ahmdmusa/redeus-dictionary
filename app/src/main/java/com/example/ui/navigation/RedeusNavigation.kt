package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.applePressScale

val LocalWindowWidthSizeClass = staticCompositionLocalOf { WindowWidthSizeClass.Compact }

enum class RedeusDestination(
    val index: Int,
    val label: String,
    val pageTitle: String,
    val contentDescription: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    SEARCH(
        index = 0,
        label = "Search",
        pageTitle = "Dictionary",
        contentDescription = "Word Lookup",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    LIBRARY(
        index = 1,
        label = "Library",
        pageTitle = "Library",
        contentDescription = "Courses & Deck",
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder
    ),
    READING(
        index = 2,
        label = "Read",
        pageTitle = "Reading",
        contentDescription = "Daily Reading",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook
    ),
    CARDS(
        index = 3,
        label = "Cards",
        pageTitle = "Practice",
        contentDescription = "Flashcards",
        selectedIcon = Icons.Filled.Style,
        unselectedIcon = Icons.Outlined.Style
    ),
    REVIEW(
        index = 4,
        label = "Review",
        pageTitle = "Review",
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
    val currentDest = destinations.getOrElse(selectedTab) { RedeusDestination.SEARCH }
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val hairlineColor = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.08f)
    val tabBarBackground = if (isDark) Color(0xFF121214) else Color(0xFFF9F9FB)
    val inactiveTabColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
            // Apple HIG Clean Navigation Header: large editorial title + single quiet settings button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 14.dp, top = 10.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentDest.pageTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val settingsInteraction = remember { MutableInteractionSource() }
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .applePressScale(settingsInteraction, 0.92f)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = settingsInteraction,
                                    indication = null,
                                    onClick = onOpenSettings
                                )
                                .testTag("settings_button"),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = hairlineColor)
                }
            }
        },
        bottomBar = {
            if (!useNavigationRail) {
                // Apple HIG Clean Tab Bar — zero chunky M3 pill, crisp tint & hairline separator
                Surface(
                    color = tabBarBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        HorizontalDivider(thickness = 0.5.dp, color = hairlineColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            destinations.forEach { destination ->
                                val isSelected = selectedTab == destination.index
                                val itemColor = if (isSelected) MaterialTheme.colorScheme.primary else inactiveTabColor
                                val interactionSource = remember { MutableInteractionSource() }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .applePressScale(interactionSource, 0.94f)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = { onSelectTab(destination.index) }
                                        )
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
                                ) {
                                    DestinationIconWithBadge(
                                        destination = destination,
                                        isSelected = isSelected,
                                        tint = itemColor,
                                        cardsDueTodayCount = cardsDueTodayCount
                                    )
                                    Text(
                                        text = destination.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            letterSpacing = 0.sp
                                        ),
                                        color = itemColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible
                                    )
                                }
                            }
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
                    containerColor = tabBarBackground,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    destinations.forEach { destination ->
                        val isSelected = selectedTab == destination.index
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { onSelectTab(destination.index) },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = inactiveTabColor,
                                unselectedTextColor = inactiveTabColor
                            ),
                            icon = {
                                DestinationIconWithBadge(
                                    destination = destination,
                                    isSelected = isSelected,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else inactiveTabColor,
                                    cardsDueTodayCount = cardsDueTodayCount
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                VerticalDivider(thickness = 0.5.dp, color = hairlineColor)
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

        // Apple HIG Top-Floating Toast Banner — never covers bottom FSRS rating buttons or tab bar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                .testTag("app_snackbar_host")
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFF1C1C1E),
                contentColor = Color.White,
                actionColor = Color(0xFF0A84FF)
            )
        }
    }
}

@Composable
private fun DestinationIconWithBadge(
    destination: RedeusDestination,
    isSelected: Boolean,
    tint: Color,
    cardsDueTodayCount: Int
) {
    val icon = if (isSelected) destination.selectedIcon else destination.unselectedIcon
    if (destination == RedeusDestination.REVIEW && cardsDueTodayCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = Color(0xFFFF3B30),
                    contentColor = Color.White,
                    modifier = Modifier.semantics {
                        contentDescription = "$cardsDueTodayCount cards due for review"
                    }
                ) {
                    Text(
                        text = if (cardsDueTodayCount > 99) "99+" else cardsDueTodayCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = destination.contentDescription,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = destination.contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
