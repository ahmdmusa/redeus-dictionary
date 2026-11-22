package com.example.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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

/**
 * Study-first 5-tab Information Architecture (Apple HIG tab-bars.md):
 *   0 = Today (Daily Habit Home)
 *   1 = Study (Unified FSRS + Extra Practice, with achievable daily due badge)
 *   2 = Courses (9 Offline Books + 26 Topics as visual cover cards)
 *   3 = Dictionary (Smart EN <-> AR Search + Deck + History)
 *   4 = Profile (Analytics + Local Assets + Daily Reading + Mining + Settings)
 */
enum class RedeusDestination(
    val index: Int,
    val label: String,
    val pageTitle: String,
    val contentDescription: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY(
        index = 0,
        label = "Today",
        pageTitle = "Today",
        contentDescription = "Daily Study Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    STUDY(
        index = 1,
        label = "Study",
        pageTitle = "Study",
        contentDescription = "Spaced Repetition Study",
        selectedIcon = Icons.Filled.Style,
        unselectedIcon = Icons.Outlined.Style
    ),
    COURSES(
        index = 2,
        label = "Courses",
        pageTitle = "Courses",
        contentDescription = "Offline Books & IELTS",
        selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
        unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks
    ),
    DICTIONARY(
        index = 3,
        label = "Search",
        pageTitle = "Dictionary",
        contentDescription = "Offline Bilingual Dictionary",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    PROFILE(
        index = 4,
        label = "Profile",
        pageTitle = "Profile",
        contentDescription = "Progress, Tools & Settings",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
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
    showTopHeader: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val widthSizeClass = LocalWindowWidthSizeClass.current
    val useNavigationRail = widthSizeClass != WindowWidthSizeClass.Compact
    val destinations = RedeusDestination.entries
    val currentDest = destinations.getOrElse(selectedTab) { RedeusDestination.TODAY }
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val hairlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.55f else 0.7f)
    val tabBarBackground = MaterialTheme.colorScheme.surfaceContainerLow
    val inactiveTabColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                // Today renders its own editorial date + greeting + streak header; other tabs show clean Apple title
                if (showTopHeader && currentDest != RedeusDestination.TODAY) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.statusBarsPadding()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
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
                                        .size(48.dp)
                                        .applePressScale(settingsInteraction, 0.92f)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = settingsInteraction,
                                            indication = null,
                                            onClick = onOpenSettings
                                        )
                                        .testTag("settings_button"),
                                    shape = CircleShape,
                                    color = Color.Transparent
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            modifier = Modifier.size(38.dp)
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
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = hairlineColor)
                        }
                    }
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    Surface(
                        color = tabBarBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.navigationBarsPadding()) {
                            HorizontalDivider(thickness = 0.5.dp, color = hairlineColor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
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
                                            .testTag("nav_tab_${destination.name.lowercase()}")
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
                                            overflow = TextOverflow.Ellipsis
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

        // Apple HIG Top-Floating Toast Banner
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
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = MaterialTheme.colorScheme.inversePrimary
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
    if (destination == RedeusDestination.STUDY && cardsDueTodayCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.semantics {
                        contentDescription = "$cardsDueTodayCount cards ready to study today"
                    }
                ) {
                    Text(
                        text = if (cardsDueTodayCount > 99) "99+" else cardsDueTodayCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
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
