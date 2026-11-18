package com.example.elearningportal

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    private lateinit var notificationSwitch: Switch
    private lateinit var darkModeCheckbox: CheckBox
    private lateinit var preferredUsername: EditText
    private lateinit var saveSettingsButton: Button
    private lateinit var resetSettingsButton: Button

    private lateinit var prefs: SharedPreferences
    private lateinit var session: SessionManager

    companion object {
        const val PREFS_NAME = "elearning_settings"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_PREFERRED_USERNAME = "preferred_username"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize SharedPreferences & SessionManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        session = SessionManager(this)

        // Bind views from activity_settings.xml
        notificationSwitch = findViewById(R.id.notificationSwitch)
        darkModeCheckbox = findViewById(R.id.darkModeCheckbox)
        preferredUsername = findViewById(R.id.preferredUsername)
        saveSettingsButton = findViewById(R.id.saveSettingsButton)
        resetSettingsButton = findViewById(R.id.resetSettingsButton)

        // Load saved preferences into the UI
        loadSettings()

        // Save button
        saveSettingsButton.setOnClickListener {
            saveSettings()
        }

        // Reset to Default button
        resetSettingsButton.setOnClickListener {
            confirmResetSettings()
        }
    }

    private fun loadSettings() {
        val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        val darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false)
        // Fall back to SessionManager username if no custom preferred username was saved yet
        val savedUsername = prefs.getString(KEY_PREFERRED_USERNAME, session.userName) ?: ""

        notificationSwitch.isChecked = notificationsEnabled
        darkModeCheckbox.isChecked = darkModeEnabled
        preferredUsername.setText(savedUsername)
    }

    private fun saveSettings() {
        val usernameInput = preferredUsername.text.toString().trim()

        if (usernameInput.isEmpty()) {
            preferredUsername.error = "Enter a preferred username"
            preferredUsername.requestFocus()
            return
        }

        val notificationsEnabled = notificationSwitch.isChecked
        val darkModeEnabled = darkModeCheckbox.isChecked

        // Persist values in SharedPreferences
        prefs.edit().apply {
            putBoolean(KEY_NOTIFICATIONS, notificationsEnabled)
            putBoolean(KEY_DARK_MODE, darkModeEnabled)
            putString(KEY_PREFERRED_USERNAME, usernameInput)
            apply()
        }

        // Apply Dark Mode immediately across the app
        applyTheme(darkModeEnabled)

        Toast.makeText(
            this,
            "Settings saved successfully",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun confirmResetSettings() {
        AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("Reset all preferences back to their default values?")
            .setPositiveButton("Reset") { _, _ ->
                resetToDefaults()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetToDefaults() {
        val defaultUsername = session.userName

        prefs.edit().apply {
            putBoolean(KEY_NOTIFICATIONS, true)
            putBoolean(KEY_DARK_MODE, false)
            putString(KEY_PREFERRED_USERNAME, defaultUsername)
            apply()
        }

        // Update UI controls
        notificationSwitch.isChecked = true
        darkModeCheckbox.isChecked = false
        preferredUsername.setText(defaultUsername)
        preferredUsername.error = null

        // Revert to light mode
        applyTheme(false)

        Toast.makeText(
            this,
            "Settings reset to default",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun applyTheme(darkMode: Boolean) {
        val mode = if (darkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}