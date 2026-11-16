package com.example.elearningportal

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    // Demo credentials
    private val correctName = "Devin"
    private val correctEmail = "devin@gmail.com"
    private val correctPassword = "123456"

    private lateinit var studentName: EditText
    private lateinit var studentEmail: EditText
    private lateinit var accessPassword: EditText
    private lateinit var rememberMe: CheckBox
    private lateinit var loginProgress: ProgressBar
    private lateinit var statusMessage: TextView
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Connect XML components
        studentName = findViewById(R.id.studentName)
        studentEmail = findViewById(R.id.studentEmail)
        accessPassword = findViewById(R.id.accessPassword)
        rememberMe = findViewById(R.id.rememberMe)
        loginProgress = findViewById(R.id.loginProgress)
        statusMessage = findViewById(R.id.statusMessage)
        submitButton = findViewById(R.id.submitButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Restore remembered details
        val preferences = getSharedPreferences(
            "LoginPreferences",
            MODE_PRIVATE
        )

        studentName.setText(preferences.getString("name", ""))
        studentEmail.setText(preferences.getString("email", ""))
        rememberMe.isChecked =
            preferences.getBoolean("remember", false)

        // Submit button
        submitButton.setOnClickListener {
            validateLogin()
        }

        // Cancel button
        cancelButton.setOnClickListener {
            studentName.text.clear()
            studentEmail.text.clear()
            accessPassword.text.clear()
            statusMessage.text = ""
            studentName.error = null
            studentEmail.error = null
            accessPassword.error = null
        }

        // Forgot password
        findViewById<TextView>(R.id.forgotPassword)
            .setOnClickListener {
                statusMessage.text =
                    "Please contact the system administrator for password assistance."
            }
    }

    private fun validateLogin() {

        val name = studentName.text.toString().trim()
        val email = studentEmail.text.toString().trim()
        val password = accessPassword.text.toString()

        // Check empty fields
        if (name.isEmpty()) {
            studentName.error = "Please enter your name"
            studentName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            studentEmail.error = "Please enter your email"
            studentEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            accessPassword.error = "Please enter your password"
            accessPassword.requestFocus()
            return
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            studentEmail.error = "Enter a valid email address"
            studentEmail.requestFocus()
            return
        }

        // Validate password length
        if (password.length < 6) {
            accessPassword.error = "Password must be at least 6 characters"
            accessPassword.requestFocus()
            return
        }

        // Show progress
        loginProgress.visibility = View.VISIBLE
        submitButton.isEnabled = false
        statusMessage.text = "Verifying login details..."

        handler.postDelayed({

            loginProgress.visibility = View.GONE
            submitButton.isEnabled = true

            // Check demo credentials
            if (
                name.equals(correctName, ignoreCase = true) &&
                email.equals(correctEmail, ignoreCase = true) &&
                password == correctPassword
            ) {

                // Save remembered details
                val preferences = getSharedPreferences(
                    "LoginPreferences",
                    MODE_PRIVATE
                )

                if (rememberMe.isChecked) {
                    preferences.edit()
                        .putString("name", name)
                        .putString("email", email)
                        .putBoolean("remember", true)
                        .apply()
                } else {
                    preferences.edit().clear().apply()
                }

                Toast.makeText(
                    this,
                    "Login successful!",
                    Toast.LENGTH_SHORT
                ).show()

                // Open MainActivity and pass student name
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("STUDENT_NAME", name)
                startActivity(intent)

                finish()

            } else {
                statusMessage.text =
                    "Invalid name, email or password. Please try again."
            }

        }, 600)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
