package com.ismael.daybyday.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.LocalDate
import java.time.YearMonth

private fun YearMonth.toIndex(): Int = year * 12 + (monthValue - 1)

private fun indexToMonth(index: Int): YearMonth = YearMonth.of(index / 12, index % 12 + 1)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var monthIndex by rememberSaveable { mutableIntStateOf(YearMonth.now().toIndex()) }

    NavHost(navController = navController, startDestination = "calendar") {

        composable("calendar") {
            CalendarScreen(
                month = indexToMonth(monthIndex),
                onMonthChange = { monthIndex = it.toIndex() },
                onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                onOpenYear = { navController.navigate("year/${indexToMonth(monthIndex).year}") },
                onOpenStats = { navController.navigate("stats") },
                onOpenSettings = { navController.navigate("settings") },
            )
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

        composable(
            route = "year/{year}",
            arguments = listOf(navArgument("year") { type = NavType.IntType }),
        ) { entry ->
            val year = entry.arguments?.getInt("year") ?: LocalDate.now().year
            YearScreen(
                initialYear = year,
                onBack = { navController.popBackStack() },
                onMonthClick = { month ->
                    monthIndex = month.toIndex()
                    navController.popBackStack()
                },
                onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
            )
        }

        composable("stats") {
            StatsScreen(onBack = { navController.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
