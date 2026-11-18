package com.example.elearningportal

import android.content.Context

/**
 * Stores the logged-in user's session in SharedPreferences.
 * Kept separate from the "remember me" prefs in LoginActivity, which only
 * pre-fill the form and do NOT count as a session.
 */
class SessionManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Call after the credentials have been verified. */
    fun createSession(name: String, email: String) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            .apply()
    }

    /** True if a session exists and has not expired. Expired sessions are cleared. */
    fun isLoggedIn(): Boolean {
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) return false

        val age = System.currentTimeMillis() - prefs.getLong(KEY_LOGIN_TIME, 0L)
        if (age < 0 || age > SESSION_DURATION_MS) {
            clear()
            return false
        }
        return true
    }

    val userName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""

    val userEmail: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""

    /** Call on logout. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_LOGIN_TIME = "login_time"

        // Session lifetime: 7 days. Change as needed.
        private const val SESSION_DURATION_MS = 7L * 24 * 60 * 60 * 1000
    }
}