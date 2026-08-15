package com.lifelensiq.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lifelensiq.app.di.ServiceLocator
import com.lifelensiq.app.domain.repository.AuthState
import com.lifelensiq.app.ui.activity.ActivityScreen
import com.lifelensiq.app.ui.activity.ActivityViewModel
import com.lifelensiq.app.ui.activity.CategoryDetailScreen
import com.lifelensiq.app.ui.activity.CategoryDetailViewModel
import com.lifelensiq.app.ui.auth.AuthViewModel
import com.lifelensiq.app.ui.auth.LoginScreen
import com.lifelensiq.app.ui.category.CategoryOverrideScreen
import com.lifelensiq.app.ui.category.CategoryOverrideViewModel
import com.lifelensiq.app.ui.components.AppBottomBar
import com.lifelensiq.app.ui.components.AppTopBar
import com.lifelensiq.app.ui.components.CategoryTitle
import com.lifelensiq.app.ui.export.ExportScreen
import com.lifelensiq.app.ui.export.ExportViewModel
import com.lifelensiq.app.ui.home.HomeScreen
import com.lifelensiq.app.ui.home.HomeViewModel
import com.lifelensiq.app.ui.onboarding.OnboardingScreen
import com.lifelensiq.app.ui.sessions.SessionsScreen
import com.lifelensiq.app.ui.sessions.SessionsViewModel
import com.lifelensiq.app.ui.settings.SettingsScreen
import com.lifelensiq.app.ui.settings.SettingsViewModel
import com.lifelensiq.app.ui.trends.TrendsScreen
import com.lifelensiq.app.ui.trends.TrendsViewModel
import com.lifelensiq.app.util.SettingsStore
import kotlinx.coroutines.delay

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val CATEGORY = "category/{category}"
    const val SESSIONS = "sessions"
    const val TRENDS = "trends"
    const val EXPORT = "export"
    const val SETTINGS = "settings"
    const val CATEGORY_OVERRIDES = "category-overrides"

    fun category(name: String): String = "category/${Uri.encode(name)}"
}

private val TAB_ROUTES = listOf(Routes.HOME, Routes.ACTIVITY, Routes.SESSIONS, Routes.TRENDS, Routes.SETTINGS)

/** Screens with a back arrow instead of a selected bottom tab. */
private val DETAIL_ROUTES = listOf(Routes.CATEGORY, Routes.EXPORT, Routes.CATEGORY_OVERRIDES)

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    initialRoute: String? = null
) {

    val authRepo = ServiceLocator.authRepository()
    val authState by authRepo.state.collectAsState()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    var onboarded by remember { mutableStateOf(SettingsStore.onboardingDone) }

    // First launch: walk through the onboarding pages before anything else.
    if (!onboarded) {
        OnboardingScreen(
            onDone = {
                SettingsStore.onboardingDone = true
                onboarded = true
            }
        )
        return
    }

    // Don't mount the NavHost until auth resolves: otherwise the login
    // screen flashes for a frame on cold start with a restored session.
    if (authState is AuthState.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when (authState) {
        is AuthState.LoggedIn -> initialRoute?.takeIf { it in TAB_ROUTES } ?: Routes.HOME
        else -> Routes.LOGIN
    }

    LaunchedEffect(authState, initialRoute) {
        val target = when (authState) {
            is AuthState.LoggedIn -> initialRoute?.takeIf { it in TAB_ROUTES } ?: Routes.HOME
            is AuthState.LoggedOut -> Routes.LOGIN
            else -> return@LaunchedEffect
        }
        // Cold start: the NavHost graph may not be attached yet — wait for it.
        while (navController.graph.findNode(target) == null) {
            delay(50)
        }
        if (navController.currentBackStackEntry?.destination?.route != target) {
            navController.navigate(target) { popUpTo(0) }
        }
    }

    val categoryName = remember(currentEntry) {
        currentEntry?.arguments?.getString("category")?.let(Uri::decode)
    }
    val showBars = currentRoute != null && currentRoute != Routes.LOGIN
    val showBack = currentRoute in DETAIL_ROUTES

    Scaffold(
        topBar = {
            if (showBars) {
                AppTopBar(
                    title = {
                        if (currentRoute == Routes.CATEGORY) {
                            CategoryTitle(categoryName ?: "Category")
                        } else {
                            Text(topBarTitleFor(currentRoute))
                        }
                    },
                    onBack = if (showBack) ({ navController.popBackStack() }) else null
                )
            }
        },
        bottomBar = {
            if (showBars) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {

            composable(Routes.LOGIN) {
                val vm: AuthViewModel = viewModel { AuthViewModel(authRepo) }
                LoginScreen(vm, onLoggedIn = { navController.navigate(Routes.HOME) { popUpTo(0) } })
            }

            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel {
                    HomeViewModel(ServiceLocator.eventRepository())
                }
                HomeScreen(vm, navController)
            }

            composable(Routes.ACTIVITY) {
                val vm: ActivityViewModel = viewModel {
                    ActivityViewModel(
                        ServiceLocator.eventRepository(),
                        com.lifelensiq.app.util.DeviceIdProvider.get(ServiceLocator.context())
                    )
                }
                ActivityScreen(vm, onCategoryClick = { category ->
                    navController.navigate(Routes.category(category))
                })
            }

            composable(
                route = Routes.CATEGORY,
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val category = Uri.decode(backStackEntry.arguments?.getString("category") ?: "")
                val vm: CategoryDetailViewModel = viewModel(key = "category-$category") {
                    CategoryDetailViewModel(ServiceLocator.eventRepository(), category)
                }
                CategoryDetailScreen(vm, category)
            }

            composable(Routes.SESSIONS) {
                val vm: SessionsViewModel = viewModel {
                    SessionsViewModel(ServiceLocator.eventRepository())
                }
                SessionsScreen(vm)
            }

            composable(Routes.TRENDS) {
                val vm: TrendsViewModel = viewModel {
                    TrendsViewModel(ServiceLocator.eventRepository())
                }
                TrendsScreen(vm)
            }

            composable(Routes.EXPORT) {
                val vm: ExportViewModel = viewModel {
                    ExportViewModel(ServiceLocator.exportUseCase())
                }
                ExportScreen(vm)
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel {
                    SettingsViewModel(
                        auth = ServiceLocator.authRepository(),
                        events = ServiceLocator.eventRepository()
                    )
                }
                SettingsScreen(vm, navController)
            }

            composable(Routes.CATEGORY_OVERRIDES) {
                val vm: CategoryOverrideViewModel = viewModel {
                    CategoryOverrideViewModel(ServiceLocator.context() as android.app.Application)
                }
                CategoryOverrideScreen(vm)
            }
        }
    }
}

private fun topBarTitleFor(route: String?): String = when (route) {
    Routes.HOME -> "LifeLens IQ"
    Routes.ACTIVITY -> "Today's Activity"
    Routes.SESSIONS -> "Study Sessions"
    Routes.TRENDS -> "Trends"
    Routes.SETTINGS -> "Settings"
    Routes.EXPORT -> "Export Data"
    Routes.CATEGORY_OVERRIDES -> "App Categories"
    else -> ""
}
