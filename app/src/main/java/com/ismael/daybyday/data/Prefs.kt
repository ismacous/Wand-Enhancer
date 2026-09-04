package com.ismael.daybyday.data

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Preferences locales (verrouillage, confidentialite). Le code PIN n'est jamais
 * stocke en clair : seul un hash PBKDF2 sale est conserve.
 */
class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("daybyday_prefs", Context.MODE_PRIVATE)

    var lockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    var blockScreenshots: Boolean
        get() = prefs.getBoolean(KEY_SECURE_SCREEN, true)
        set(value) = prefs.edit().putBoolean(KEY_SECURE_SCREEN, value).apply()

    var startOnToday: Boolean
        get() = prefs.getBoolean(KEY_START_TODAY, true)
        set(value) = prefs.edit().putBoolean(KEY_START_TODAY, value).apply()

    val hasPin: Boolean get() = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_SALT).remove(KEY_PIN_HASH).apply()
    }

    fun checkPin(pin: String): Boolean {
        val saltEncoded = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashEncoded = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltEncoded, Base64.NO_WRAP)
        val expected = Base64.decode(hashEncoded, Base64.NO_WRAP)
        val actual = hash(pin, salt)
        return constantTimeEquals(expected, actual)
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    private companion object {
        const val KEY_LOCK_ENABLED = "lock_enabled"
        const val KEY_BIOMETRIC = "biometric_enabled"
        const val KEY_SECURE_SCREEN = "secure_screen"
        const val KEY_START_TODAY = "start_on_today"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val ITERATIONS = 120_000
    }
}
