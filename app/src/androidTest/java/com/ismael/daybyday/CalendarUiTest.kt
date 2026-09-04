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

    @Test
    fun colorierAujourdHuiDepuisLAccueil() {
        // Une couleur differente de l'autre test pour rester independant de
        // l'ordre d'execution (un clic sur la couleur deja choisie l'enleve).
        composeRule.onNodeWithTag("today-ORANGE").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Journée mitigée").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun lesOngletsDuBasFonctionnent() {
        val year = LocalDate.now().year

        composeRule.onNodeWithText("Bilan").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Mon bilan").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Année").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Année $year").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Réglages").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Rappel quotidien").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Mois").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Bilan du mois").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun laRechercheTrouveUneJourneeEcrite() {
        val today = LocalDate.now()
        val marker = "Ruisseau${System.currentTimeMillis() % 100000}"

        composeRule.onNodeWithTag("day-${today.toEpochDay()}").performClick()
        composeRule.onNodeWithTag("day-note-field").performTextInput(marker)
        composeRule.onNodeWithContentDescription("Retour").performClick()

        composeRule.onNodeWithContentDescription("Rechercher").performClick()
        composeRule.onNodeWithTag("search-field").performTextInput(marker)

        // Le compteur de resultats n'apparait que si la journee est retrouvee
        // en base (le champ de recherche contient deja le texte tape).
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("1 journée(s) trouvée(s)")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
