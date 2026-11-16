package com.example.elearningportal

import android.os.Bundle
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private var studentName: String = "Student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Receive student name from LoginActivity
        studentName = intent.getStringExtra("STUDENT_NAME") ?: "Student"

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

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Elearning Portal"

        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->

            val selectedFragment: Fragment? = when (item.itemId) {

                R.id.nav_home -> HomeFragment()

                R.id.nav_register -> RegisterFragment()

                R.id.nav_courses -> CoursesFragment()

                else -> null
            }

            if (selectedFragment != null) {

                // Pass student name to HomeFragment
                if (selectedFragment is HomeFragment) {
                    selectedFragment.arguments = Bundle().apply {
                        putString("STUDENT_NAME", studentName)
                    }
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, selectedFragment)
                    .commit()

                true
            } else {
                false
            }
        }

        // Load default fragment
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    // Display Options Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    // Handle Options Menu clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {

            R.id.action_settings -> {
                startActivity(
                    Intent(this, SettingsActivity::class.java)
                )
                true
            }

            R.id.action_logout -> {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
