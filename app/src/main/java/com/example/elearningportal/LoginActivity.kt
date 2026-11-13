package com.example.elearningportal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRemember: CheckBox
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button
    private lateinit var tvForgot: TextView
    private lateinit var progressBar: ProgressBar

    private val prefs by lazy { getSharedPreferences("login_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etName     = findViewById(R.id.etName)
        etEmail    = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRemember = findViewById(R.id.cbRemember)
        btnSubmit  = findViewById(R.id.btnSubmit)
        btnCancel  = findViewById(R.id.btnCancel)
        tvForgot   = findViewById(R.id.tvForgot)
        progressBar = findViewById(R.id.progressBar)

        // Restore saved credentials
        val savedName = prefs.getString("name", "")
        val savedEmail = prefs.getString("email", "")
        if (prefs.getBoolean("remember", false)) {
            etName.setText(savedName)
            etEmail.setText(savedEmail)
            cbRemember.isChecked = true
        }

        btnSubmit.setOnClickListener { onLogin() }

        btnCancel.setOnClickListener {
            etName.text.clear()
            etEmail.text.clear()
            etPassword.text.clear()
            cbRemember.isChecked = false
        }

        tvForgot.setOnClickListener {
            Toast.makeText(this, "Password reset link sent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onLogin() {
        if (!validate()) return

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val remember = cbRemember.isChecked

        // Save preferences
        prefs.edit().apply {
            putBoolean("remember", remember)
            putString("name", name)
            putString("email", email)
            apply()
        }

        // Simulate login on background thread
        progressBar.visibility = View.VISIBLE
        btnSubmit.isEnabled = false

        Thread {
            Thread.sleep(2000) // simulate network call
            runOnUiThread {
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled = true
                navigateToMain(name)
            }
        }.start()
    }

    private fun validate(): Boolean {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (name.isEmpty()) {
            etName.error = "Name is required"
            return false
        }
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return false
        } else if (!Pattern.matches("""^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$""", email)) {
            etEmail.error = "Invalid email format"
            return false
        }
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return false
        } else if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    private fun navigateToMain(name: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("student_name", name)
        startActivity(intent)
        finish()
    }
}