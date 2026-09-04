package com.ismael.daybyday.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.LocalDate
import java.time.YearMonth

private fun YearMonth.toIndex(): Int = year * 12 + (monthValue - 1)

private fun indexToMonth(index: Int): YearMonth = YearMonth.of(index / 12, index % 12 + 1)

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("calendar", "Mois", Icons.Default.DateRange),
    Tab("year", "Année", Icons.AutoMirrored.Filled.List),
    Tab("stats", "Bilan", Icons.Default.Star),
    Tab("money", "Argent", Icons.Default.ShoppingCart),
    Tab("settings", "Réglages", Icons.Default.Settings),
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var monthIndex by rememberSaveable { mutableIntStateOf(YearMonth.now().toIndex()) }
    var yearShown by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = scaffoldPadding.calculateBottomPadding()),
        ) {
            NavHost(navController = navController, startDestination = "calendar") {

                composable("calendar") {
                    CalendarScreen(
                        month = indexToMonth(monthIndex),
                        onMonthChange = { monthIndex = it.toIndex() },
                        onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                        onOpenSearch = { navController.navigate("search") },
                    )
                }

                composable("year") {
                    YearScreen(
                        year = yearShown,
                        onYearChange = { yearShown = it },
                        onMonthClick = { month ->
                            monthIndex = month.toIndex()
                            navController.switchTab("calendar")
                        },
                        onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                    )
                }

                composable("stats") {
                    StatsScreen()
                }

                composable("money") {
                    MoneyScreen(
                        onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                    )
                }

                composable("settings") {
                    SettingsScreen()
                }

                composable(
                    route = "day/{epochDay}",
                    arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
                ) { entry ->
                    val epochDay = entry.arguments?.getLong("epochDay") ?: LocalDate.now().toEpochDay()
                    DayScreen(
                        initialDate = LocalDate.ofEpochDay(epochDay),
                        onBack = { navController.popBackStack() },
                    )
                }

                composable("search") {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                    )
                }
            }
        }
    }
}

/** Change d'onglet sans empiler les destinations les unes sur les autres. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
