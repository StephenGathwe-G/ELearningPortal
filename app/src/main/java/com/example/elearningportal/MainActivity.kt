package com.example.elearningportal

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No valid session? Send the user to the login screen.
        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            redirectToLogin()
            return // don't run the rest of onCreate
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val studentName = intent.getStringExtra("student_name")
            ?: session.userName.ifEmpty { "Student" }
        // use studentName (e.g. display in a greeting TextView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Catches sessions that expire while the app is in the background
    override fun onResume() {
        super.onResume()
        if (::session.isInitialized && !session.isLoggedIn()) redirectToLogin()
    }

    private fun redirectToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    // Hook this up to a logout button or menu item
    private fun logout() {
        session.clear()
        redirectToLogin()
    }
}