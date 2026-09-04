package com.ismael.daybyday.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.ui.theme.DayByDayTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applySecureFlag()

        setContent {
            DayByDayTheme {
                val app = dayByDayApp
                // null tant qu'on ne sait pas encore si la base est vide.
                var offerRestore by remember { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(Unit) {
                    offerRestore = !app.prefs.firstRunRestoreChecked &&
                        app.repository.allDays().isEmpty()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when {
                        app.lock.isLocked -> LockScreen(
                            prefs = app.prefs,
                            onUnlocked = { app.lock.unlock() },
                        )

                        offerRestore == null -> Box(Modifier.fillMaxSize())

                        offerRestore == true -> WelcomeRestoreScreen(
                            onFinished = { offerRestore = false },
                        )

                        else -> AppNavigation()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        applySecureFlag()
        dayByDayApp.lock.onEnterForeground()
    }

    override fun onStop() {
        super.onStop()
        dayByDayApp.lock.onEnterBackground()
    }

    /** Empeche les captures d'ecran et masque l'app dans la liste des recentes. */
    fun applySecureFlag() {
        if (dayByDayApp.prefs.blockScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
