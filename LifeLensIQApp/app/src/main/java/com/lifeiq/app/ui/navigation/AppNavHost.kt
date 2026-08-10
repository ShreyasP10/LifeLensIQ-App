package com.lifeiq.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeiq.app.di.ServiceLocator
import com.lifeiq.app.domain.repository.AuthState
import com.lifeiq.app.ui.attendance.AttendanceScreen
import com.lifeiq.app.ui.attendance.AttendanceViewModel
import com.lifeiq.app.ui.auth.AuthViewModel
import com.lifeiq.app.ui.auth.LoginScreen
import com.lifeiq.app.ui.export.ExportScreen
import com.lifeiq.app.ui.export.ExportViewModel
import com.lifeiq.app.ui.home.HomeScreen
import com.lifeiq.app.ui.home.HomeViewModel
import com.lifeiq.app.ui.sessions.SessionsScreen
import com.lifeiq.app.ui.sessions.SessionsViewModel
import com.lifeiq.app.ui.settings.SettingsScreen
import com.lifeiq.app.ui.settings.SettingsViewModel
import com.lifeiq.app.ui.timetable.TimetableScreen
import com.lifeiq.app.ui.timetable.TimetableViewModel

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val TIMETABLE = "timetable"
    const val SESSIONS = "sessions"
    const val ATTENDANCE = "attendance"
    const val EXPORT = "export"
    const val SETTINGS = "settings"
}

private val TAB_ROUTES = listOf(Routes.HOME, Routes.TIMETABLE, Routes.SESSIONS, Routes.ATTENDANCE, Routes.SETTINGS)

private data class TabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    TabItem(Routes.HOME, "Home", Icons.Filled.Home),
    TabItem(Routes.TIMETABLE, "Timetable", Icons.Filled.DateRange),
    TabItem(Routes.SESSIONS, "Sessions", Icons.Filled.PlayArrow),
    TabItem(Routes.ATTENDANCE, "Attendance", Icons.Filled.CheckCircle),
    TabItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    val authRepo = ServiceLocator.authRepository()
    val authState by authRepo.state.collectAsState()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn && navController.currentBackStackEntry?.destination?.route != Routes.HOME) {
            navController.navigate(Routes.HOME) { popUpTo(0) }
        } else if (authState is AuthState.LoggedOut && navController.currentBackStackEntry?.destination?.route != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in TAB_ROUTES) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = androidx.compose.ui.Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {

            composable(Routes.LOGIN) {
                val vm: AuthViewModel = viewModel { AuthViewModel(authRepo) }
                LoginScreen(vm, onLoggedIn = { navController.navigate(Routes.HOME) { popUpTo(0) } })
            }

            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel {
                    HomeViewModel(ServiceLocator.eventRepository(), ServiceLocator.timetableRepository())
                }
                HomeScreen(vm, navController)
            }

            composable(Routes.TIMETABLE) {
                val vm: TimetableViewModel = viewModel {
                    TimetableViewModel(ServiceLocator.timetableRepository())
                }
                TimetableScreen(vm, onBack = { navController.popBackStack() })
            }

            composable(Routes.SESSIONS) {
                val vm: SessionsViewModel = viewModel {
                    SessionsViewModel(ServiceLocator.eventRepository(), ServiceLocator.timetableRepository())
                }
                SessionsScreen(vm, onBack = { navController.popBackStack() })
            }

            composable(Routes.ATTENDANCE) {
                val vm: AttendanceViewModel = viewModel {
                    AttendanceViewModel(ServiceLocator.eventRepository(), ServiceLocator.timetableRepository())
                }
                AttendanceScreen(vm, onBack = { navController.popBackStack() })
            }

            composable(Routes.EXPORT) {
                val vm: ExportViewModel = viewModel {
                    ExportViewModel(ServiceLocator.exportUseCase())
                }
                ExportScreen(vm, onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel {
                    SettingsViewModel(
                        auth = ServiceLocator.authRepository(),
                        events = ServiceLocator.eventRepository(),
                        timetable = ServiceLocator.timetableRepository()
                    )
                }
                SettingsScreen(vm, onBack = { navController.popBackStack() })
            }
        }
    }
}
