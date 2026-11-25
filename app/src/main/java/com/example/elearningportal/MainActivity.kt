package com.example.elearningportal

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var btnNavChevron: ImageButton
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No valid session? Send the user to the login screen.
        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            redirectToLogin()
            return
        }

        val isDark = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(SettingsActivity.KEY_DARK_MODE, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        setContentView(R.layout.activity_main)

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
        supportActionBar?.title = "ELearning Portal"

        // Initialize bottom navigation and far-left chevron button
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnNavChevron = findViewById(R.id.btnNavChevron)

        showMainNavigation()

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    fun showMainNavigation() {
        if (!::btnNavChevron.isInitialized || !::bottomNavigation.isInitialized) return

        btnNavChevron.visibility = View.GONE
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_navigation)

        btnNavChevron.setOnClickListener { view ->
            showNavigationPopupMenu(view)
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_register -> {
                    loadFragment(RegisterFragment())
                    true
                }
                R.id.nav_courses -> {
                    loadFragment(CoursesFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun showDetailNavigation(onEditRunnable: Runnable, onDeleteRunnable: Runnable) {
        if (!::btnNavChevron.isInitialized || !::bottomNavigation.isInitialized) return

        btnNavChevron.visibility = View.VISIBLE
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.detail_actions_menu)

        btnNavChevron.setOnClickListener {
            showMainNavigation()
            supportFragmentManager.popBackStack()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_edit_course -> {
                    onEditRunnable.run()
                    true
                }
                R.id.action_delete_course -> {
                    onDeleteRunnable.run()
                    true
                }
                else -> false
            }
        }
    }

    private fun showNavigationPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "🏠  Home Dashboard")
        popup.menu.add(0, 2, 1, "📝  Register Student")
        popup.menu.add(0, 3, 2, "📚  Explore Courses")
        popup.menu.add(0, 4, 3, "👥  Registered Students")
        popup.menu.add(0, 5, 4, "⚙️  Portal Settings")
        popup.menu.add(0, 6, 5, "🚪  Logout")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showMainNavigation()
                    bottomNavigation.selectedItemId = R.id.nav_home
                    loadFragment(HomeFragment())
                    true
                }
                2 -> {
                    showMainNavigation()
                    bottomNavigation.selectedItemId = R.id.nav_register
                    loadFragment(RegisterFragment())
                    true
                }
                3 -> {
                    showMainNavigation()
                    bottomNavigation.selectedItemId = R.id.nav_courses
                    loadFragment(CoursesFragment())
                    true
                }
                4 -> {
                    startActivity(Intent(this, StudentsActivity::class.java))
                    true
                }
                5 -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                6 -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

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
                logout()
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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun logout() {
        session.clear()
        redirectToLogin()
    }
}