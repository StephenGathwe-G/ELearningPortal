package com.example.elearningportal

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No valid session? Send the user to the login screen.
        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            redirectToLogin()
            return // don't run the rest of onCreate
        }

        setContentView(R.layout.activity_main)

        val studentName = intent.getStringExtra("student_name")
            ?: session.userName.ifEmpty { "Student" }
        // use studentName (e.g. display in a greeting TextView)

        // Handle system window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Configure toolbar
        val toolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Elearnig portal"

        // Initialize bottom navigation
        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    // Display HomeFragment
                    true
                }

                R.id.nav_register -> {
                    // Display RegisterFragment
                    true
                }

                R.id.nav_courses -> {
                    // Display CoursesFragment
                    true
                }

                else -> false
            }
        }

        // Load the default fragment only on first creation
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
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
