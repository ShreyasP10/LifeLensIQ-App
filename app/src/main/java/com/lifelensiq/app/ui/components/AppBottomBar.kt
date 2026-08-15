package com.lifelensiq.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.lifelensiq.app.ui.navigation.Routes

data class BottomTabItem(val route: String, val label: String, val icon: ImageVector)

/** Top-level destinations shown in the bottom navigation bar. */
val BOTTOM_TABS = listOf(
    BottomTabItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomTabItem(Routes.ACTIVITY, "Activity", Icons.Filled.List),
    BottomTabItem(Routes.SESSIONS, "Sessions", Icons.Filled.PlayArrow),
    BottomTabItem(Routes.TRENDS, "Trends", Icons.Filled.DateRange),
    BottomTabItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

/** Material 3 bottom navigation bar with state-preserving tab switching. */
@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        BOTTOM_TABS.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}