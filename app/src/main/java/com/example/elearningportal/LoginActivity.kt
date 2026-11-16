package com.example.elearningportal

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.elearningportal.database.AppDatabase

class LoginActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRemember: CheckBox
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button
    private lateinit var tvForgot: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var loginMenuButton: ImageButton

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    // Background operations (same pattern as RegisterActivity)
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = AppDatabase.getInstance(this)

        // Seed on the background thread. The executor is single-threaded, so this
        // always finishes before any login attempt queued after it.
        executor.execute {
            try {
                db.seedInitialUser()
            } catch (e: Exception) {
                // Seeding failed; login will simply reject all credentials
            }
        }

        // Initialize views
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRemember = findViewById(R.id.cbRemember)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnCancel = findViewById(R.id.btnCancel)
        tvForgot = findViewById(R.id.tvForgot)
        progressBar = findViewById(R.id.progressBar)
        //loginMenuButton = findViewById(R.id.loginMenuButton)

        restoreSavedCredentials()

        // Login button
        btnSubmit.setOnClickListener { loginUser() }

        // Cancel button
        btnCancel.setOnClickListener { clearForm() }

        // Forgot password
        tvForgot.setOnClickListener { handleForgotPassword() }

        // Popup menu
        setupPopupMenu()
    }

    // Restore saved credentials
    private fun restoreSavedCredentials() {
        if (prefs.getBoolean(KEY_REMEMBER, false)) {
            etName.setText(prefs.getString(KEY_NAME, ""))
            etEmail.setText(prefs.getString(KEY_EMAIL, ""))
            cbRemember.isChecked = true
        }
    }

    private fun loginUser() {

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val remember = cbRemember.isChecked

        // Validate name
        if (name.isEmpty()) {
            etName.error = "Enter your full name"
            etName.requestFocus()
            return
        }

        // Validate email
        if (email.isEmpty()) {
            etEmail.error = "Enter your email"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email address"
            etEmail.requestFocus()
            return
        }

        // Validate password
        if (password.isEmpty()) {
            etPassword.error = "Enter your password"
            etPassword.requestFocus()
            return
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            etPassword.error =
                "Password must be at least $MIN_PASSWORD_LENGTH characters"
            etPassword.requestFocus()
            return
        }

        // Prevent multiple submissions
        setLoading(true)

        // Authenticate on background thread
        executor.execute {

            val userName = try {
                authenticate(email, password)
            } catch (e: Exception) {
                null
            }

            runOnUiThread {

                if (isFinishing || isDestroyed) return@runOnUiThread

                setLoading(false)

                if (userName != null) {
                    saveCredentials(remember, userName, email)

                    Toast.makeText(this, "Welcome, $userName!", Toast.LENGTH_SHORT).show()
                    navigateToMain(userName)

                } else {
                    etPassword.text?.clear()
                    Toast.makeText(
                        this,
                        "Invalid email or password",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun authenticate(email: String, password: String): String? =
        db.authenticate(email, password)

    private fun saveCredentials(remember: Boolean, name: String, email: String) {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER, remember)
            if (remember) {
                putString(KEY_NAME, name)
                putString(KEY_EMAIL, email)
            } else {
                remove(KEY_NAME)
                remove(KEY_EMAIL)
            }
            apply()
        }
    }

    private fun handleForgotPassword() {
        val email = etEmail.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter your email first"
            etEmail.requestFocus()
            return
        }

        Toast.makeText(
            this,
            "Password reset is not implemented yet",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSubmit.isEnabled = !loading
    }

    // Clear login form
    private fun clearForm() {
        etName.text.clear()
        etEmail.text.clear()
        etPassword.text.clear()
        cbRemember.isChecked = false
        etName.error = null
        etEmail.error = null
        etPassword.error = null
    }

    // Popup menu implementation
    private fun setupPopupMenu() {

        loginMenuButton.setOnClickListener { view ->

            val popup = PopupMenu(this, view)

            popup.menu.add("Clear Form")
            popup.menu.add("Load Sample Data")

            popup.setOnMenuItemClickListener { item ->

                when (item.title.toString()) {

                    "Clear Form" -> {
                        clearForm()
                        Toast.makeText(
                            this,
                            "Form cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    "Load Sample Data" -> {
                        etName.setText(AppDatabase.DEFAULT_USER_NAME)
                        etEmail.setText(AppDatabase.DEFAULT_USER_EMAIL)
                        etPassword.setText(AppDatabase.DEFAULT_USER_PASSWORD)
                        Toast.makeText(this, "Sample data loaded", Toast.LENGTH_SHORT).show()
                    }
                }

                true
            }

            popup.show()
        }
    }

    private fun navigateToMain(name: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("student_name", name)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    companion object {
        private const val PREFS_NAME = "login_prefs"
        private const val KEY_REMEMBER = "remember"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val MIN_PASSWORD_LENGTH = 6
    }
}