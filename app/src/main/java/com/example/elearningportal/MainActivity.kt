package com.example.elearningportal

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
// Add these imports at the top of MainActivity.kt
import android.view.Menu
import android.view.MenuItem
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

        val isDark = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(SettingsActivity.KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

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

        supportActionBar?.title = "Elearning portal"

        // Initialize bottom navigation
        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    // Display HomeFragment
                    loadFragment(HomeFragment())
                    true
                }

                R.id.nav_register -> {
                    // Display RegisterFragment
                    loadFragment(RegisterFragment())
                    true
                }

                R.id.nav_courses -> {
                    // Display CoursesFragment
                    loadFragment(CoursesFragment())
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_logout -> {
                logout() // Uses your existing logout() method
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun redirectToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
    // Load selected fragment
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Hook this up to a logout button or menu item
    private fun logout() {
        session.clear()
        redirectToLogin()
    }
}
