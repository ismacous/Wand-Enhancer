package com.ismael.daybyday

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismael.daybyday.ui.Dates
import com.ismael.daybyday.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class CalendarUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun leCalendrierDuMoisEnCoursEstAffiche() {
        composeRule.onNodeWithText("DayByDay").assertIsDisplayed()
        composeRule.onNodeWithText(Dates.monthTitle(YearMonth.now())).assertIsDisplayed()
        composeRule.onNodeWithText("Bilan du mois").assertIsDisplayed()
    }

    @Test
    fun colorierUnJourEtEcrireUnTitreEstConserve() {
        val today = LocalDate.now()
        val dayTag = "day-${today.toEpochDay()}"
        val title = "Test ${System.currentTimeMillis()}"

        composeRule.onNodeWithTag(dayTag).performClick()
        composeRule.onNodeWithText("Couleur du jour").assertIsDisplayed()

        composeRule.onNodeWithTag("color-GREEN").performClick()
        composeRule.onNodeWithTag("day-title-field").performTextInput(title)

        composeRule.onNodeWithContentDescription("Retour").performClick()
        composeRule.onNodeWithTag(dayTag).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Bonne journée").assertIsDisplayed()
    }
}
