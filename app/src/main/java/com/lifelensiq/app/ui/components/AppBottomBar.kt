package com.lifelensiq.app.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifelensiq.app.ui.navigation.Routes

data class BottomTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isWebsite: Boolean = false
)

/**
 * Top-level destinations: the mobile section (Home, Activity, Sessions,
 * Trends, Settings) plus a separate Website section (Website stats) —
 * visually split by a divider.
 */
val BOTTOM_TABS = listOf(
    BottomTabItem(Routes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    BottomTabItem(Routes.WEBSITE, "Website", Icons.Rounded.Refresh, Icons.Outlined.Refresh, isWebsite = true),
    BottomTabItem(Routes.ACTIVITY, "Activity", Icons.Rounded.List, Icons.Outlined.List),
    BottomTabItem(Routes.SESSIONS, "Sessions", Icons.Rounded.PlayArrow, Icons.Outlined.PlayArrow),
    BottomTabItem(Routes.TRENDS, "Trends", Icons.Rounded.DateRange, Icons.Outlined.DateRange),
    BottomTabItem(Routes.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings)
)

/** Material 3 bottom navigation bar with state-preserving tab switching. */
@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        BOTTOM_TABS.forEachIndexed { index, tab ->
            if (index == 1) {
                HorizontalDivider(
                    modifier = Modifier.width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab.route) },
                icon = {
                    Icon(
                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (tab.isWebsite) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    }
                )
            )
        }
    }
}