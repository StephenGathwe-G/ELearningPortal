package com.example.elearningportal

import android.content.Context

/**
 * Two kinds of session:
 *  - Persistent ("Remember me" checked): saved in SharedPreferences, survives app
 *    close and restart until it expires or the user logs out.
 *  - Temporary ("Remember me" unchecked): kept only in memory, so it ends when the
 *    app process is closed.
 */
class SessionManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Call after the credentials have been verified. */
    fun createSession(name: String, email: String, remember: Boolean) {
        clear()
        if (remember) {
            prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                .apply()
        } else {
            memLoggedIn = true
            memName = name
            memEmail = email
        }
    }

    /** True if a temporary or a non-expired persistent session exists. */
    fun isLoggedIn(): Boolean {
        if (memLoggedIn) return true
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) return false

        val age = System.currentTimeMillis() - prefs.getLong(KEY_LOGIN_TIME, 0L)
        if (age < 0 || age > SESSION_DURATION_MS) {
            clear()
            return false
        }
        return true
    }

    val userName: String
        get() = if (memLoggedIn) memName else prefs.getString(KEY_NAME, "") ?: ""

    val userEmail: String
        get() = if (memLoggedIn) memEmail else prefs.getString(KEY_EMAIL, "") ?: ""

    /** Call on logout. Clears both kinds of session. */
    fun clear() {
        memLoggedIn = false
        memName = ""
        memEmail = ""
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_LOGIN_TIME = "login_time"

        // Persistent session lifetime: 7 days. Change as needed.
        private const val SESSION_DURATION_MS = 7L * 24 * 60 * 60 * 1000

        // In-memory session: lives only as long as the app process
        @Volatile private var memLoggedIn = false
        @Volatile private var memName = ""
        @Volatile private var memEmail = ""
    }
}