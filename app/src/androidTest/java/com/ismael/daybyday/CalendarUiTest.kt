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
import androidx.test.platform.app.InstrumentationRegistry
import com.ismael.daybyday.data.Prefs
import com.ismael.daybyday.ui.Dates
import com.ismael.daybyday.ui.MainActivity
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class CalendarUiTest {

    companion object {
        /**
         * L'ecran de reprise de sauvegarde s'affiche quand la base est vide.
         * On le desactive avant que la regle ne lance l'activite, sinon il
         * masque le calendrier pendant les tests.
         */
        @JvmStatic
        @BeforeClass
        fun desactiverLEcranDeReprise() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            Prefs(context).firstRunRestoreChecked = true
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun leCalendrierDuMoisEnCoursEstAffiche() {
        composeRule.onNodeWithText("Aujourd'hui", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(Dates.monthTitle(YearMonth.now())).assertIsDisplayed()
        // Le bilan est en bas de la page : il existe sans forcement etre visible.
        composeRule.onNodeWithText("Bilan du mois").assertExists()
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

        composeRule.onNodeWithText("Argent").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Ce qu'il te reste").fetchSemanticsNodes().isNotEmpty()
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

    @Test
    fun noterUnMomentDeLaJourneeColoreLaJournee() {
        // Un jour que les autres tests ne touchent pas, pour que la couleur du
        // jour soit encore en mode automatique.
        val day = LocalDate.now().minusDays(3)

        composeRule.onNodeWithTag("day-${day.toEpochDay()}").performClick()
        composeRule.onNodeWithText("Les moments de la journée").assertIsDisplayed()

        // Un matin vert et une nuit noire donnent une journée orange en moyenne.
        composeRule.onNodeWithTag("part-MORNING-GREEN").performClick()
        composeRule.onNodeWithTag("part-NIGHT-BLACK").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Calculée à partir de tes moments de la journée.")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
