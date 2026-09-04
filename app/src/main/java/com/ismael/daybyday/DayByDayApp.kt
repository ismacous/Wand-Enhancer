package com.ismael.daybyday

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ismael.daybyday.data.DayRepository
import com.ismael.daybyday.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DayByDayApp : Application() {

    val repository: DayRepository by lazy { DayRepository(this) }
    val prefs: Prefs by lazy { Prefs(this) }
    val lock: LockController by lazy { LockController(prefs) }

    /** Portee de coroutine liee au process, pour les sauvegardes de fin d'ecran. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

/** Etat de verrouillage de l'application, partage entre l'activite et l'UI. */
class LockController(private val prefs: Prefs) {

    var isLocked by mutableStateOf(prefs.lockEnabled && prefs.hasPin)
        private set

    private var backgroundedAt: Long = 0L

    fun unlock() {
        isLocked = false
    }

    fun lockNow() {
        if (prefs.lockEnabled && prefs.hasPin) isLocked = true
    }

    fun onEnterBackground() {
        backgroundedAt = System.currentTimeMillis()
    }

    fun onEnterForeground() {
        if (!prefs.lockEnabled || !prefs.hasPin) {
            isLocked = false
            return
        }
        if (backgroundedAt != 0L && System.currentTimeMillis() - backgroundedAt > GRACE_MS) {
            isLocked = true
        }
    }

    /** Appele quand le verrouillage vient d'etre active ou desactive. */
    fun refresh() {
        if (!prefs.lockEnabled || !prefs.hasPin) isLocked = false
    }

    private companion object {
        const val GRACE_MS = 15_000L
    }
}

val Context.dayByDayApp: DayByDayApp
    get() = applicationContext as DayByDayApp
